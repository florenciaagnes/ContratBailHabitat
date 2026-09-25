package com.legaltech.bail.client;

import com.legaltech.bail.dto.AuditDtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class LegalAuditClient {

    private static final Logger log = LoggerFactory.getLogger(LegalAuditClient.class);

    @Value("${flask.audit.url:http://localhost:5000/api/v1/audit-contract}")
    private String flaskAuditUrl;

    private final RestTemplate restTemplate;

    public LegalAuditClient() {
        this.restTemplate = new RestTemplate();
    }

    public AuditResponseDto auditContract(BigDecimal rentAmount, BigDecimal depositAmount, List<ClauseDto> clauses) {
        AuditRequestDto request = AuditRequestDto.builder()
                .rent_amount(rentAmount)
                .deposit_amount(depositAmount)
                .clauses(clauses)
                .build();

        try {
            log.info("Sending audit request to Flask Microservice at {}: {} clauses", flaskAuditUrl, clauses.size());
            
            // Try RestClient / RestTemplate call to Flask API
            AuditResponseDto response = restTemplate.postForObject(flaskAuditUrl, request, AuditResponseDto.class);
            if (response != null && response.getExceptions() != null) {
                log.info("Audit response received with {} exceptions", response.getExceptions().size());
                return response;
            }
        } catch (Exception e) {
            log.warn("Flask Microservice unreachable or returned error ({}). Running embedded Java legal auditor rules.", e.getMessage());
        }

        // Embedded fallback auditor engine if Flask service is unreachable during offline boot
        return runEmbeddedAuditRules(rentAmount, depositAmount, clauses);
    }

    private AuditResponseDto runEmbeddedAuditRules(BigDecimal rentAmount, BigDecimal depositAmount, List<ClauseDto> clauses) {
        List<ExceptionDto> exceptions = new ArrayList<>();

        // Deposit amount check (> 2 months rent)
        if (rentAmount != null && depositAmount != null && rentAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxAllowed = rentAmount.multiply(BigDecimal.valueOf(2));
            if (depositAmount.compareTo(maxAllowed) > 0) {
                exceptions.add(ExceptionDto.builder()
                        .clause_index(0)
                        .source_document("Loi_2015_037_Bail_Habitation.pdf")
                        .page_reference("Page 2")
                        .extracted_law_text("Article 12 : Le dépôt de garantie versé par le locataire à la signature du contrat ne peut en aucun cas excéder deux (2) mois de loyer principal hors charges.")
                        .severity("CRITICAL")
                        .message("Le dépôt de garantie demandé (" + depositAmount + " Ar) dépasse la limite légale de 2 mois de loyer (" + maxAllowed + " Ar max).")
                        .build());
            }
        }

        // Clause content checks
        if (clauses != null) {
            for (int i = 0; i < clauses.size(); i++) {
                ClauseDto c = clauses.get(i);
                String txt = c.getTexte() != null ? c.getTexte().toLowerCase() : "";
                String title = c.getTitre() != null ? c.getTitre() : "Clause " + (i + 1);

                if (txt.contains("coupure") || txt.contains("serrures") || txt.contains("sans tribunal") || txt.contains("expulsion directe") || txt.contains("voie de fait")) {
                    exceptions.add(ExceptionDto.builder()
                            .clause_index(i)
                            .source_document("Guide_Pratique_Tribunal_Expulsion_Voies_De_Fait.pdf")
                            .page_reference("Page 1")
                            .extracted_law_text("Article 5 : L'expulsion d'un locataire ou l'interruption des services essentiels (coupure d'eau, d'électricité, changement des serrures) exige obligatoirement une décision de justice exécutoire du TPI.")
                            .severity("CRITICAL")
                            .message("Violation grave dans '" + title + "' : Interdiction formelle de coupure d'eau/électricité et d'expulsion sans décision de justice exécutoire du TPI.")
                            .build());
                }

                if (txt.contains("gros travaux") || txt.contains("toiture") || txt.contains("étanchéité") || txt.contains("gros œuvre") || txt.contains("charge exclusive du locataire")) {
                    exceptions.add(ExceptionDto.builder()
                            .clause_index(i)
                            .source_document("LTGO_Loi_66_003_Code_Civil_Mada.pdf")
                            .page_reference("Page 2")
                            .extracted_law_text("Article 1720 : Le bailleur est tenu d'exécuter à ses frais toutes les réparations majeures et de structure. Est réputée léonine et abusive toute clause déchargeant le bailleur de son obligation d'entretien des gros ouvrages.")
                            .severity("WARNING")
                            .message("Clause abusive (Léonine) dans '" + title + "' : Le bailleur ne peut pas transférer l'obligation des gros travaux de structure sur le locataire.")
                            .build());
                }
            }
        }

        return AuditResponseDto.builder()
                .status("COMPLETED")
                .exceptions(exceptions)
                .build();
    }
}
