package com.legaltech.bail.service;

import com.legaltech.bail.entity.*;
import com.legaltech.bail.repository.AmendmentRepository;
import com.legaltech.bail.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AmendmentService {

    private static final Logger log = LoggerFactory.getLogger(AmendmentService.class);

    private final ContractRepository contractRepository;
    private final AmendmentRepository amendmentRepository;
    private final ContractService contractService;

    @Transactional
    public Amendment createAmendment(Long parentContractId, String motif, Contract newContractData, List<ContractClause> newClauses) {
        Contract parent = contractRepository.findById(parentContractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrat d'origine introuvable : " + parentContractId));

        if (parent.getStatut() != ContractStatus.SIGNED && parent.getStatut() != ContractStatus.EXPIRED) {
            throw new IllegalStateException("Seul un contrat signé ou arrivé à échéance (expiré) peut faire l'objet d'un avenant modificatif.");
        }

        // Mark parent contract as AMENDED to maintain strict legal immutability history
        parent.setStatut(ContractStatus.AMENDED);
        contractRepository.save(parent);

        // Prepare new contract (child amendment)
        Contract child = Contract.builder()
                .reference("AVENANT-" + parent.getReference() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .bailleur(parent.getBailleur())
                .locataire(parent.getLocataire())
                .bien(parent.getBien())
                .loyer(newContractData.getLoyer() != null ? newContractData.getLoyer() : parent.getLoyer())
                .charges(newContractData.getCharges() != null ? newContractData.getCharges() : parent.getCharges())
                .depot(newContractData.getDepot() != null ? newContractData.getDepot() : parent.getDepot())
                .dateDebut(newContractData.getDateDebut() != null ? newContractData.getDateDebut() : parent.getDateDebut())
                .dateFin(newContractData.getDateFin() != null ? newContractData.getDateFin() : parent.getDateFin())
                .taciteReconduction(newContractData.getTaciteReconduction() != null ? newContractData.getTaciteReconduction() : parent.getTaciteReconduction())
                .statut(ContractStatus.PENDING_AUDIT)
                .build();

        // If new clauses provided, use them; otherwise clone parent clauses
        List<ContractClause> clausesToUse = new ArrayList<>();
        if (newClauses != null && !newClauses.isEmpty()) {
            clausesToUse = newClauses;
        } else {
            for (ContractClause pc : parent.getClauses()) {
                clausesToUse.add(ContractClause.builder()
                        .ordre(pc.getOrdre())
                        .titre(pc.getTitre())
                        .texte(pc.getTexte())
                        .build());
            }
        }

        Contract savedChild = contractService.createContract(child, clausesToUse);

        // Save Amendment history record
        Amendment amendment = Amendment.builder()
                .parentContract(parent)
                .newContract(savedChild)
                .motif(motif)
                .date(LocalDateTime.now())
                .build();

        Amendment savedAmendment = amendmentRepository.save(amendment);
        log.info("Avenant créé avec succès. Parent ID: {}, Nouveau Contrat ID: {}, Motif: {}", parentContractId, savedChild.getId(), motif);

        return savedAmendment;
    }

    @Transactional(readOnly = true)
    public List<Amendment> getHistoryByContractId(Long contractId) {
        return amendmentRepository.findByParentContractId(contractId);
    }
}
