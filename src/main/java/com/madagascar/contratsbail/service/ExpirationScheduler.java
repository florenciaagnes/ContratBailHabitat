package com.madagascar.contratsbail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Met a jour le statut EXPIRE des contrats au demarrage puis chaque nuit (00h05, heure de Madagascar). */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpirationScheduler {

    private final StatutContratService statutContratService;

    @EventListener(ApplicationReadyEvent.class)
    public void auDemarrage() {
        executer();
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Indian/Antananarivo")
    public void chaqueNuit() {
        executer();
    }

    private void executer() {
        try {
            int n = statutContratService.synchroniserTous();
            log.info("Synchronisation des statuts d'expiration : {} contrat(s) mis a jour", n);
        } catch (Exception e) {
            log.warn("Echec de la synchronisation des statuts d'expiration", e);
        }
    }
}
