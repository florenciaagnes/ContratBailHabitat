package com.legaltech.bail.service;

import com.legaltech.bail.client.LegalAuditClient;
import com.legaltech.bail.dto.AuditDtos.*;
import com.legaltech.bail.entity.*;
import com.legaltech.bail.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private final ContractRepository contractRepository;
    private final ContractClauseRepository clauseRepository;
    private final LegalExceptionRepository exceptionRepository;
    private final LegalAuditClient auditClient;
    private final SimulatedDateService simulatedDateService;

    @Transactional
    public void evaluateExpirations(java.time.LocalDate currentDate) {
        if (currentDate == null) return;
        List<Contract> contracts = contractRepository.findAll();
        for (Contract c : contracts) {
            if (c.getDateFin() != null) {
                if (currentDate.isAfter(c.getDateFin())) {
                    if (c.getStatut() != ContractStatus.EXPIRED && c.getStatut() != ContractStatus.AMENDED) {
                        c.setStatut(ContractStatus.EXPIRED);
                        contractRepository.save(c);
                    }
                } else if (!currentDate.isAfter(c.getDateFin()) && c.getStatut() == ContractStatus.EXPIRED) {
                    if (c.getSignatures() != null && !c.getSignatures().isEmpty()) {
                        c.setStatut(ContractStatus.SIGNED);
                    } else {
                        c.setStatut(ContractStatus.READY_TO_SIGN);
                    }
                    contractRepository.save(c);
                }
            }
        }
    }

    @Transactional
    public Contract createContract(Contract contract, List<ContractClause> clauseInputs) {
        if (contract.getReference() == null || contract.getReference().isBlank()) {
            contract.setReference("BAIL-MADA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        contract.setStatut(ContractStatus.PENDING_AUDIT);
        
        Contract savedContract = contractRepository.save(contract);

        List<ClauseDto> clauseDtos = new ArrayList<>();
        if (clauseInputs != null && !clauseInputs.isEmpty()) {
            for (int i = 0; i < clauseInputs.size(); i++) {
                ContractClause c = clauseInputs.get(i);
                c.setContract(savedContract);
                c.setOrdre(i + 1);
                ContractClause savedClause = clauseRepository.save(c);
                savedContract.addClause(savedClause);

                clauseDtos.add(ClauseDto.builder()
                        .ordre(savedClause.getOrdre())
                        .titre(savedClause.getTitre())
                        .texte(savedClause.getTexte())
                        .build());
            }
        }

        // Run dynamic legal audit via Python Flask
        runAuditOnContract(savedContract, clauseDtos);

        return contractRepository.save(savedContract);
    }

    @Transactional
    public Contract reAuditContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable : " + contractId));

        // Delete old exceptions
        exceptionRepository.deleteByContractId(contractId);
        contract.getExceptions().clear();

        List<ClauseDto> clauseDtos = new ArrayList<>();
        for (ContractClause c : contract.getClauses()) {
            clauseDtos.add(ClauseDto.builder()
                    .ordre(c.getOrdre())
                    .titre(c.getTitre())
                    .texte(c.getTexte())
                    .build());
        }

        runAuditOnContract(contract, clauseDtos);

        return contractRepository.save(contract);
    }

    @Transactional
    public Contract updateClause(Long contractId, Long clauseId, String newTitre, String newTexte) {
        ContractClause clause = clauseRepository.findById(clauseId)
                .orElseThrow(() -> new IllegalArgumentException("Clause introuvable : " + clauseId));

        clause.setTitre(newTitre);
        clause.setTexte(newTexte);
        clauseRepository.save(clause);

        return reAuditContract(contractId);
    }

    @Transactional
    public Contract updateContractDetails(Long contractId, java.math.BigDecimal loyer, java.math.BigDecimal charges, java.math.BigDecimal depot, java.time.LocalDate dateDebut, java.time.LocalDate dateFin, Boolean taciteReconduction, List<ContractClause> newClauses) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable : " + contractId));

        if (contract.getStatut() == ContractStatus.SIGNED || contract.getStatut() == ContractStatus.AMENDED) {
            throw new IllegalStateException("Ce contrat est signé et scellé. Son contenu est légalement immuable.");
        }

        if (loyer != null) contract.setLoyer(loyer);
        if (charges != null) contract.setCharges(charges);
        if (depot != null) contract.setDepot(depot);
        if (dateDebut != null) contract.setDateDebut(dateDebut);
        if (dateFin != null) contract.setDateFin(dateFin);
        if (taciteReconduction != null) contract.setTaciteReconduction(taciteReconduction);

        if (newClauses != null) {
            clauseRepository.deleteByContractId(contractId);
            contract.getClauses().clear();
            for (int i = 0; i < newClauses.size(); i++) {
                ContractClause c = newClauses.get(i);
                c.setContract(contract);
                c.setOrdre(i + 1);
                ContractClause savedClause = clauseRepository.save(c);
                contract.addClause(savedClause);
            }
        }

        return reAuditContract(contractId);
    }

    private void runAuditOnContract(Contract contract, List<ClauseDto> clauseDtos) {
        log.info("Lancement de l'audit juridique sur le contrat ref {}", contract.getReference());
        AuditResponseDto auditResult = auditClient.auditContract(contract.getLoyer(), contract.getDepot(), clauseDtos);

        boolean hasCritical = false;
        if (auditResult != null && auditResult.getExceptions() != null) {
            for (ExceptionDto dto : auditResult.getExceptions()) {
                Severity sev = "CRITICAL".equalsIgnoreCase(dto.getSeverity()) ? Severity.CRITICAL : Severity.WARNING;
                if (sev == Severity.CRITICAL) {
                    hasCritical = true;
                }

                LegalException ex = LegalException.builder()
                        .contract(contract)
                        .clauseIndex(dto.getClause_index() != null ? dto.getClause_index() : 0)
                        .sourceDocument(dto.getSource_document() != null ? dto.getSource_document() : "Document_Inconnu.pdf")
                        .pageReference(dto.getPage_reference() != null ? dto.getPage_reference() : "Page 1")
                        .severity(sev)
                        .message(dto.getMessage())
                        .extraitLoi(dto.getExtracted_law_text())
                        .resolu(false)
                        .build();

                LegalException savedEx = exceptionRepository.save(ex);
                contract.addException(savedEx);
            }
        }

        if (hasCritical) {
            log.warn("Anomalies critiques détectées. Statut passé à ACTION_REQUIRED pour {}", contract.getReference());
            contract.setStatut(ContractStatus.ACTION_REQUIRED);
        } else {
            log.info("Aucune anomalie critique. Statut passé à READY_TO_SIGN pour {}", contract.getReference());
            contract.setStatut(ContractStatus.READY_TO_SIGN);
        }
    }

    @Transactional
    public Page<Contract> searchContracts(
            String query,
            ContractStatus statut,
            String ville,
            com.legaltech.bail.entity.PropertyType typeBien,
            Double superficieMin,
            Double superficieMax,
            Integer nombreChambresMin,
            Boolean meuble,
            Long bailleurId,
            Long locataireId,
            java.math.BigDecimal loyerMin,
            java.math.BigDecimal loyerMax,
            Boolean taciteReconduction,
            Pageable pageable) {
        
        evaluateExpirations(simulatedDateService.getSimulatedDate());

        String trimmedQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        String trimmedVille = (ville != null && !ville.isBlank()) ? ville.trim() : null;

        return contractRepository.searchMultiCriteria(
                trimmedQuery, statut, trimmedVille, typeBien, superficieMin, superficieMax, nombreChambresMin, meuble,
                bailleurId, locataireId, loyerMin, loyerMax, taciteReconduction, pageable);
    }

    @Transactional(readOnly = true)
    public Contract getContractById(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable : " + id));
    }
}
