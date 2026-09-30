package com.madagascar.contratsbail.service;

import com.madagascar.contratsbail.dto.AjoutClauseAvenantRequest;
import com.madagascar.contratsbail.dto.CreationAvenantRequest;
import com.madagascar.contratsbail.dto.VariableValeurDto;
import com.madagascar.contratsbail.dto.SignatureRequest;
import com.madagascar.contratsbail.entity.*;
import com.madagascar.contratsbail.entity.enums.StatutAvenant;
import com.madagascar.contratsbail.entity.enums.StatutContrat;
import com.madagascar.contratsbail.entity.enums.TypePartie;
import com.madagascar.contratsbail.exception.RessourceIntrouvableException;
import com.madagascar.contratsbail.exception.ValidationMetierException;
import com.madagascar.contratsbail.repository.AvenantRepository;
import com.madagascar.contratsbail.repository.ContratRepository;
import com.madagascar.contratsbail.repository.ModeleClauseAvenantRepository;
import com.madagascar.contratsbail.repository.PersonneRepository;
import com.madagascar.contratsbail.repository.SignatureAvenantRepository;
import com.madagascar.contratsbail.repository.VariableClauseAvenantRepository;
import com.madagascar.contratsbail.util.VariableSubstitutionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gestion des avenants : un avenant complete un contrat deja signe et archive
 * SANS jamais modifier le contrat archive (ni son PDF, ni ses articles).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AvenantService {

    private final AvenantRepository avenantRepository;
    private final SignatureAvenantRepository signatureAvenantRepository;
    private final ContratRepository contratRepository;
    private final PersonneRepository personneRepository;
    private final PdfGenerationService pdfGenerationService;
    private final DocumentStorageService documentStorageService;
    private final ModeleClauseAvenantRepository modeleClauseAvenantRepository;
    private final VariableClauseAvenantRepository variableClauseAvenantRepository;
    private final StatutContratService statutContratService;

    public Avenant creer(Long idContrat, CreationAvenantRequest requete) {
        Contrat contrat = contratRepository.findById(idContrat)
                .orElseThrow(() -> new RessourceIntrouvableException("Contrat introuvable : id=" + idContrat));

        if (contrat.getStatut() != StatutContrat.ARCHIVE && contrat.getStatut() != StatutContrat.SIGNE) {
            throw new ValidationMetierException(
                    "Un avenant ne peut être créé que pour un contrat signé et archivé.");
        }
        valider(requete);

        long rang = avenantRepository.countByContratId(idContrat) + 1;
        Avenant avenant = Avenant.builder()
                .contrat(contrat)
                .numero(contrat.getNumero() + "-AV" + rang)
                .dateEffet(requete.getDateEffet())
                .objet(requete.getObjet().trim())
                .nouvelleDateFin(requete.getNouvelleDateFin())
                .statut(StatutAvenant.BROUILLON)
                .build();

        int ordre = 1;
        if (requete.getClauses() != null) {
            for (AjoutClauseAvenantRequest c : requete.getClauses()) {
                avenant.getClauses().add(construireClause(avenant, c, ordre++));
            }
        }
        return avenantRepository.save(avenant);
    }

    private void valider(CreationAvenantRequest r) {
        if (r.getDateEffet() == null) {
            throw new ValidationMetierException("La date d'effet de l'avenant est obligatoire.");
        }
        if (r.getObjet() == null || r.getObjet().isBlank()) {
            throw new ValidationMetierException("L'objet de l'avenant est obligatoire.");
        }
        boolean aDesClauses = r.getClauses() != null && !r.getClauses().isEmpty();
        if (!aDesClauses && r.getNouvelleDateFin() == null) {
            throw new ValidationMetierException(
                    "Un avenant doit contenir au moins une clause ou une nouvelle date de fin.");
        }
        if (aDesClauses) {
            for (AjoutClauseAvenantRequest c : r.getClauses()) {
                if (c.getIdModeleClause() == null) {
                    throw new ValidationMetierException("Chaque clause doit référencer un modèle de clause.");
                }
            }
        }
        if (r.getNouvelleDateFin() != null && r.getNouvelleDateFin().isBefore(r.getDateEffet())) {
            throw new ValidationMetierException(
                    "La nouvelle date de fin doit être postérieure ou égale à la date d'effet.");
        }
    }

    @Transactional(readOnly = true)
    public Avenant obtenir(Long id) {
        return avenantRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Avenant introuvable : id=" + id));
    }

    @Transactional(readOnly = true)
    public List<Avenant> listerParContrat(Long idContrat) {
        return avenantRepository.findByContratIdOrderByDateCreationAsc(idContrat);
    }

    /** Date de fin du bail apres prise en compte des avenants archives (null si aucun avenant ne la modifie). */
    @Transactional(readOnly = true)
    public LocalDate dateFinEffective(Long idContrat) {
        LocalDate resultat = null;
        for (Avenant a : listerParContrat(idContrat)) {
            if (a.getStatut() == StatutAvenant.ARCHIVE && a.getNouvelleDateFin() != null) {
                resultat = a.getNouvelleDateFin();
            }
        }
        return resultat;
    }

    public SignatureAvenant signer(Long idAvenant, SignatureRequest requete) {
        Avenant avenant = obtenir(idAvenant);
        if (avenant.getStatut() == StatutAvenant.ARCHIVE) {
            throw new ValidationMetierException("Cet avenant est archivé : il ne peut plus être signé.");
        }
        if (requete.getSignatureData() == null || requete.getSignatureData().isBlank()) {
            throw new ValidationMetierException("La signature est vide.");
        }
        if (requete.getIdPersonne() == null) {
            throw new ValidationMetierException("Sélectionnez un signataire.");
        }
        Personne personne = personneRepository.findById(requete.getIdPersonne())
                .orElseThrow(() -> new RessourceIntrouvableException("Personne introuvable"));

        Set<Long> attendus = signatairesAttendus(avenant.getContrat()).stream()
                .map(Personne::getId).collect(Collectors.toSet());
        if (!attendus.contains(personne.getId())) {
            throw new ValidationMetierException("Cette personne n'est pas partie au contrat.");
        }
        boolean dejaSigne = avenant.getSignatures().stream()
                .anyMatch(s -> s.getPersonne().getId().equals(personne.getId()));
        if (dejaSigne) {
            throw new ValidationMetierException(personne.getNomComplet() + " a déjà signé cet avenant.");
        }

        SignatureAvenant signature = SignatureAvenant.builder()
                .avenant(avenant)
                .personne(personne)
                .signatureData(requete.getSignatureData())
                .ordre(avenant.getSignatures().size())
                .build();
        signature = signatureAvenantRepository.save(signature);
        avenant.getSignatures().add(signature);

        if (avenant.getStatut() == StatutAvenant.BROUILLON) {
            avenant.setStatut(StatutAvenant.EN_ATTENTE_SIGNATURE);
            avenantRepository.save(avenant);
        }
        return signature;
    }

    @Transactional(readOnly = true)
    public byte[] genererPdf(Long idAvenant) {
        return pdfGenerationService.genererPdfAvenant(obtenir(idAvenant));
    }

    public DocumentContrat archiver(Long idAvenant) {
        Avenant avenant = obtenir(idAvenant);
        if (avenant.getStatut() == StatutAvenant.ARCHIVE) {
            throw new ValidationMetierException("Cet avenant est déjà archivé.");
        }

        Set<Long> idsSignes = avenant.getSignatures().stream()
                .map(s -> s.getPersonne().getId()).collect(Collectors.toSet());
        for (Personne p : signatairesAttendus(avenant.getContrat())) {
            if (!idsSignes.contains(p.getId())) {
                throw new ValidationMetierException(
                        "Toutes les parties doivent signer l'avenant avant l'archivage (manquant : "
                                + p.getNomComplet() + ").");
            }
        }

        byte[] pdf = pdfGenerationService.genererPdfAvenant(avenant);
        DocumentContrat document = documentStorageService.enregistrer(
                avenant.getContrat(), avenant.getNumero() + ".pdf", "PDF_AVENANT", pdf, true);

        avenant.setStatut(StatutAvenant.ARCHIVE);
        avenant.setDateArchivage(LocalDateTime.now());
        avenantRepository.save(avenant);
        // la nouvelle date de fin (prolongation) peut lever ou creer le statut EXPIRE du contrat
        statutContratService.synchroniserExpiration(avenant.getContrat());
        return document;
    }

    private List<Personne> signatairesAttendus(Contrat contrat) {
        List<Personne> resultat = new ArrayList<>();
        for (PartieContrat partie : contrat.getParties()) {
            if (partie.getTypePartie() == TypePartie.PERSONNE) {
                resultat.add(partie.getPersonne());
            } else {
                partie.getOrganisation().getRepresentants()
                        .forEach(r -> resultat.add(r.getPersonne()));
            }
        }
        return resultat;
    }

    /** Construit une clause d'avenant a partir d'un modele de la bibliotheque + valeurs saisies. */
    private AvenantClause construireClause(Avenant avenant, AjoutClauseAvenantRequest requete, int ordre) {
        ModeleClauseAvenant modele = modeleClauseAvenantRepository.findById(requete.getIdModeleClause())
                .orElseThrow(() -> new RessourceIntrouvableException("Modèle de clause introuvable"));

        List<VariableClauseAvenant> variablesDefinies =
                variableClauseAvenantRepository.findByModeleClauseIdOrderByOrdreAsc(modele.getId());

        Map<String, String> valeurs = new HashMap<>();
        if (requete.getVariables() != null) {
            for (VariableValeurDto vv : requete.getVariables()) {
                if (vv.getNom() != null) {
                    valeurs.put(vv.getNom(), vv.getValeur() == null ? "" : vv.getValeur());
                }
            }
        }

        for (VariableClauseAvenant v : variablesDefinies) {
            String val = valeurs.get(v.getNom());
            if (Boolean.TRUE.equals(v.getObligatoire()) && (val == null || val.isBlank())) {
                throw new ValidationMetierException("La variable '" + v.getLibelle()
                        + "' est obligatoire pour la clause " + modele.getTitre() + ".");
            }
            if (v.getType() == com.madagascar.contratsbail.entity.enums.TypeVariable.LISTE
                    && val != null && !val.isBlank() && !v.getOptionsListe().contains(val)) {
                throw new ValidationMetierException("La valeur choisie pour '" + v.getLibelle()
                        + "' ne fait pas partie de la liste autorisée.");
            }
        }

        String contenuFinal = VariableSubstitutionUtil.substituer(modele.getContenuModele(), valeurs);

        return AvenantClause.builder()
                .avenant(avenant)
                .ordre(ordre)
                .titre(modele.getTitre())
                .contenu(contenuFinal)
                .build();
    }
}
