package com.legaltech.bail.service;

import com.legaltech.bail.entity.Contract;
import com.legaltech.bail.entity.ContractStatus;
import com.legaltech.bail.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class TaciteReconductionScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaciteReconductionScheduler.class);

    private final ContractRepository contractRepository;

    /**
     * Tâche planifiée quotidienne (tous les jours à 02:00 du matin)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void processTaciteReconduction() {
        log.info("[CRON SCHEDULER] Vérification quotidienne des préavis (J-90) et de la tacite reconduction (J-0)...");

        LocalDate today = LocalDate.now();
        List<Contract> activeContracts = contractRepository.findByStatutAndTaciteReconductionTrue(ContractStatus.SIGNED);

        for (Contract contract : activeContracts) {
            LocalDate dateFin = contract.getDateFin();
            long daysRemaining = ChronoUnit.DAYS.between(today, dateFin);

            // Alerte Préavis J-90 (entre 80 et 90 jours restant)
            if (daysRemaining <= 90 && daysRemaining > 0) {
                log.info("[ALERTE PRÉAVIS J-90] Le contrat ref {} arrive à échéance dans {} jours (Date fin: {}). Préavis de résiliation ouvert.",
                        contract.getReference(), daysRemaining, dateFin);
            }

            // Tacite reconduction automatique à J-0 (Date de fin atteinte ou dépassée)
            if (daysRemaining <= 0) {
                long durationInMonths = ChronoUnit.MONTHS.between(contract.getDateDebut(), contract.getDateFin());
                if (durationInMonths <= 0) durationInMonths = 12; // Par défaut 1 an

                LocalDate newDateDebut = dateFin.plusDays(1);
                LocalDate newDateFin = newDateDebut.plusMonths(durationInMonths);

                log.info("[TACITE RECONDUCTION J-0] Reconduction automatique du contrat ref {} du {} au {}",
                        contract.getReference(), newDateDebut, newDateFin);

                contract.setDateDebut(newDateDebut);
                contract.setDateFin(newDateFin);
                contractRepository.save(contract);
            }
        }
    }
}
