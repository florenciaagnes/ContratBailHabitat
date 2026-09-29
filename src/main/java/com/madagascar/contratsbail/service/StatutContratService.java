package com.madagascar.contratsbail.service;

import com.madagascar.contratsbail.entity.Avenant;
import com.madagascar.contratsbail.entity.Contrat;
import com.madagascar.contratsbail.entity.ContratStatut;
import com.madagascar.contratsbail.entity.enums.StatutAvenant;
import com.madagascar.contratsbail.entity.enums.StatutContrat;
import com.madagascar.contratsbail.repository.AvenantRepository;
import com.madagascar.contratsbail.repository.ContratRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

/**
 * Gere les statuts (table statut_contrat) d'un contrat. Un contrat peut cumuler
 * plusieurs statuts, par exemple SIGNE + ARCHIVE + EXPIRE.
 * Cette classe ne depend que des repositories (pas d'autre service) pour eviter
 * toute dependance circulaire avec ContratService et AvenantService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StatutContratService {

    private static final ZoneId FUSEAU = ZoneId.of("Indian/Antananarivo");

    private final ContratRepository contratRepository;
    private final AvenantRepository avenantRepository;

    /** Ajoute un statut au contrat (sans doublon) et retire ceux qu'il remplace. */
    public void ajouter(Contrat contrat, StatutContrat statut) {
        if (statut == StatutContrat.EN_ATTENTE_SIGNATURE) {
            retirer(contrat, StatutContrat.BROUILLON);
        } else if (statut == StatutContrat.SIGNE || statut == StatutContrat.ARCHIVE) {
            retirer(contrat, StatutContrat.BROUILLON, StatutContrat.EN_ATTENTE_SIGNATURE);
        }
        if (!possede(contrat, statut)) {
            contrat.getStatuts().add(ContratStatut.builder().contrat(contrat).statut(statut).build());
        }
    }

    public void retirer(Contrat contrat, StatutContrat... statuts) {
        List<StatutContrat> aRetirer = Arrays.asList(statuts);
        contrat.getStatuts().removeIf(cs -> aRetirer.contains(cs.getStatut()));
    }

    public boolean possede(Contrat contrat, StatutContrat statut) {
        return contrat.getStatuts().stream().anyMatch(cs -> cs.getStatut() == statut);
    }

    /**
     * Date de fin effective : date de fin du contrat, remplacee par la nouvelle date de fin
     * du dernier avenant archive qui la modifie (prolongation ou raccourcissement).
     */
    @Transactional(readOnly = true)
    public LocalDate dateFinEffective(Contrat contrat) {
        LocalDate fin = contrat.getDateFin();
        for (Avenant a : avenantRepository.findByContratIdOrderByDateCreationAsc(contrat.getId())) {
            if (a.getStatut() == StatutAvenant.ARCHIVE && a.getNouvelleDateFin() != null) {
                fin = a.getNouvelleDateFin();
            }
        }
        return fin;
    }

    /**
     * Ajoute EXPIRE si la date de fin effective est depassee, le retire si un avenant
     * l'a repoussee. Un contrat sans date de fin n'expire jamais. Le statut de workflow
     * (SIGNE, ARCHIVE...) est conserve : EXPIRE s'y ajoute.
     * @return true si les statuts ont ete modifies
     */
    public boolean synchroniserExpiration(Contrat contrat) {
        LocalDate fin = dateFinEffective(contrat);
        boolean doitExpirer = fin != null && fin.isBefore(LocalDate.now(FUSEAU));
        boolean expire = possede(contrat, StatutContrat.EXPIRE);

        if (doitExpirer && !expire) {
            ajouter(contrat, StatutContrat.EXPIRE);
            contratRepository.save(contrat);
            return true;
        }
        if (!doitExpirer && expire) {
            retirer(contrat, StatutContrat.EXPIRE);
            contratRepository.save(contrat);
            return true;
        }
        return false;
    }

    /** @return le nombre de contrats dont les statuts ont change */
    public int synchroniserTous() {
        int modifies = 0;
        for (Contrat contrat : contratRepository.findAll()) {
            if (synchroniserExpiration(contrat)) {
                modifies++;
            }
        }
        return modifies;
    }
}
