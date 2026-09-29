package com.madagascar.contratsbail.service;

import com.madagascar.contratsbail.dto.*;
import com.madagascar.contratsbail.entity.*;
import com.madagascar.contratsbail.entity.enums.RolePartie;
import com.madagascar.contratsbail.entity.enums.SourceValeurAuto;
import com.madagascar.contratsbail.entity.enums.StatutContrat;
import com.madagascar.contratsbail.entity.enums.TypePartie;
import com.madagascar.contratsbail.entity.enums.TypeVariable;
import com.madagascar.contratsbail.exception.RessourceIntrouvableException;
import com.madagascar.contratsbail.exception.ValidationMetierException;
import com.madagascar.contratsbail.repository.*;
import com.madagascar.contratsbail.util.CinValidator;
import com.madagascar.contratsbail.util.TypesBien;
import com.madagascar.contratsbail.util.VariableSubstitutionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ContratService {

    private final ContratRepository contratRepository;
    private final PersonneRepository personneRepository;
    private final OrganisationRepository organisationRepository;
    private final RepresentantOrganisationRepository representantRepository;
    private final BienRepository bienRepository;
    private final PartieContratRepository partieContratRepository;
    private final ModeleArticleRepository modeleArticleRepository;
    private final VariableArticleRepository variableArticleRepository;
    private final ContratArticleRepository contratArticleRepository;
    private final ContratVariableRepository contratVariableRepository;
    private final SignatureRepository signatureRepository;

    private final PdfGenerationService pdfGenerationService;
    private final DocumentStorageService documentStorageService;
    private final StatutContratService statutContratService;

    /** Si vrai, la signature de toutes les parties est obligatoire avant archivage. */
    private final boolean signatureObligatoireAvantArchivage = true;

    // ------------------------------------------------------------------
    // Creation
    // ------------------------------------------------------------------

    public Contrat creerContrat(CreationContratRequest requete) {
        validerCreation(requete);

        Bien bien = bienRepository.save(mapBien(requete.getBien()));

        Contrat contrat = Contrat.builder()
                .numero(genererNumero())
                .dateDebut(requete.getDateDebut())
                .dateFin(requete.getDateFin())
                .bien(bien)
                .statut(StatutContrat.BROUILLON)
                .build();
        contrat = contratRepository.save(contrat);
        statutContratService.ajouter(contrat, StatutContrat.BROUILLON);

        PartieContrat proprietaire = construirePartie(requete.getProprietaire(), RolePartie.PROPRIETAIRE, 0, contrat);
        PartieContrat locataire = construirePartie(requete.getLocataire(), RolePartie.LOCATAIRE, 1, contrat);
        partieContratRepository.save(proprietaire);
        partieContratRepository.save(locataire);

        if (requete.getArticles() != null) {
            int ordre = 1;
            for (AjoutArticleRequest articleReq : requete.getArticles()) {
                ajouterArticleInterne(contrat, articleReq, ordre++);
            }
        }

        statutContratService.synchroniserExpiration(contrat);
        return obtenir(contrat.getId());
    }

    private void validerCreation(CreationContratRequest requete) {
        if (requete.getDateDebut() == null) {
            throw new ValidationMetierException("La date de début du bail est obligatoire.");
        }
        if (requete.getDateFin() != null && requete.getDateFin().isBefore(requete.getDateDebut())) {
            throw new ValidationMetierException("La date de fin doit être postérieure ou égale à la date de début.");
        }
        if (requete.getBien() == null || requete.getBien().getAdresse() == null || requete.getBien().getAdresse().isBlank()) {
            throw new ValidationMetierException("L'adresse du bien loué est obligatoire.");
        }
        if (!TypesBien.estValide(requete.getBien().getTypeBien())) {
            throw new ValidationMetierException("Type de bien invalide : choisissez une valeur de la liste ("
                    + String.join(", ", TypesBien.VALEURS) + ").");
        }
        validerPartie(requete.getProprietaire(), "propriétaire");
        validerPartie(requete.getLocataire(), "locataire");
    }

    private void validerPartie(PartieDto partie, String role) {
        if (partie == null || partie.getTypePartie() == null) {
            throw new ValidationMetierException("Les informations du " + role + " sont obligatoires.");
        }
        if (partie.getTypePartie() == TypePartie.PERSONNE) {
            if (partie.getPersonne() == null || partie.getPersonne().getNom() == null || partie.getPersonne().getNom().isBlank()) {
                throw new ValidationMetierException("Le nom du " + role + " est obligatoire.");
            }
            CinValidator.valider(partie.getPersonne().getCin(), "du " + role);
        } else {
            if (partie.getOrganisation() == null || partie.getOrganisation().getNom() == null || partie.getOrganisation().getNom().isBlank()) {
                throw new ValidationMetierException("Le nom de la société (" + role + ") est obligatoire.");
            }
            if (partie.getOrganisation().getRepresentants() == null || partie.getOrganisation().getRepresentants().isEmpty()) {
                throw new ValidationMetierException("Au moins un représentant est obligatoire pour une société (" + role + ").");
            }
            for (RepresentantDto r : partie.getOrganisation().getRepresentants()) {
                if (r.getPersonne() == null || r.getPersonne().getNom() == null || r.getPersonne().getNom().isBlank()) {
                    throw new ValidationMetierException("Le nom de chaque représentant est obligatoire.");
                }
                CinValidator.valider(r.getPersonne().getCin(),
                        "du représentant " + r.getPersonne().getNom());
            }
        }
    }

    private PartieContrat construirePartie(PartieDto dto, RolePartie role, int ordre, Contrat contrat) {
        PartieContrat.PartieContratBuilder builder = PartieContrat.builder()
                .contrat(contrat)
                .typePartie(dto.getTypePartie())
                .role(role)
                .ordre(ordre);

        if (dto.getTypePartie() == TypePartie.PERSONNE) {
            Personne personne = personneRepository.save(mapPersonne(dto.getPersonne()));
            builder.personne(personne);
        } else {
            Organisation organisation = organisationRepository.save(mapOrganisation(dto.getOrganisation()));
            int ordreRep = 0;
            for (RepresentantDto r : dto.getOrganisation().getRepresentants()) {
                Personne personneRep = personneRepository.save(mapPersonne(r.getPersonne()));
                RepresentantOrganisation representant = RepresentantOrganisation.builder()
                        .organisation(organisation)
                        .personne(personneRep)
                        .fonction(r.getFonction())
                        .ordre(ordreRep++)
                        .build();
                representantRepository.save(representant);
            }
            builder.organisation(organisation);
        }
        return builder.build();
    }

    private Personne mapPersonne(PersonneDto dto) {
        Personne personne = dto.getId() != null
                ? personneRepository.findById(dto.getId()).orElse(new Personne())
                : new Personne();
        personne.setNom(dto.getNom());
        personne.setPrenom(dto.getPrenom());
        personne.setCin(CinValidator.normaliser(dto.getCin()));
        personne.setDateDelivranceCin(dto.getDateDelivranceCin());
        personne.setLieuDelivranceCin(dto.getLieuDelivranceCin());
        personne.setAdresse(dto.getAdresse());
        personne.setTelephone(dto.getTelephone());
        personne.setEmail(dto.getEmail());
        return personne;
    }

    private Organisation mapOrganisation(OrganisationDto dto) {
        Organisation organisation = dto.getId() != null
                ? organisationRepository.findById(dto.getId()).orElse(new Organisation())
                : new Organisation();
        organisation.setNom(dto.getNom());
        organisation.setFormeJuridique(dto.getFormeJuridique());
        organisation.setSiegeSocial(dto.getSiegeSocial());
        organisation.setAdresse(dto.getAdresse());
        organisation.setTelephone(dto.getTelephone());
        organisation.setEmail(dto.getEmail());
        organisation.setNumeroIdentification(dto.getNumeroIdentification());
        return organisation;
    }

    private Bien mapBien(BienDto dto) {
        Bien bien = dto.getId() != null
                ? bienRepository.findById(dto.getId()).orElse(new Bien())
                : new Bien();
        bien.setAdresse(dto.getAdresse());
        bien.setDescription(dto.getDescription());
        bien.setTypeBien(dto.getTypeBien());
        bien.setSurface(dto.getSurface());
        bien.setReference(dto.getReference());
        return bien;
    }

    private String genererNumero() {
        Long sequence = contratRepository.prochainNumeroSequence();
        return "BAIL-" + Year.now().getValue() + "-" + String.format("%03d", sequence);
    }

    // ------------------------------------------------------------------
    // Articles
    // ------------------------------------------------------------------

    public ContratArticle ajouterArticle(Long idContrat, AjoutArticleRequest requete) {
        Contrat contrat = obtenir(idContrat);
        verifierModifiable(contrat);
        int ordreSuivant = contrat.getArticles().stream()
                .mapToInt(ContratArticle::getOrdre).max().orElse(0) + 1;
        ContratArticle article = ajouterArticleInterne(contrat, requete, ordreSuivant);
        return article;
    }

    private ContratArticle ajouterArticleInterne(Contrat contrat, AjoutArticleRequest requete, int ordre) {
        ModeleArticle modele = modeleArticleRepository.findById(requete.getIdModeleArticle())
                .orElseThrow(() -> new RessourceIntrouvableException("Modele d'article introuvable"));

        List<VariableArticle> variablesDefinies = variableArticleRepository
                .findByModeleArticleIdOrderByOrdreAsc(modele.getId());

        Map<String, String> valeursSaisies = new HashMap<>();
        if (requete.getVariables() != null) {
            for (VariableValeurDto vv : requete.getVariables()) {
                if (vv.getNom() != null) {
                    valeursSaisies.put(vv.getNom(), vv.getValeur() == null ? "" : vv.getValeur());
                }
            }
        }

        // Variables a valeur automatique : reprises du contrat (section Duree & bien loue)
        for (VariableArticle v : variablesDefinies) {
            if (v.getValeurAuto() != null) {
                String auto = valeurAutomatique(v.getValeurAuto(), contrat);
                if (auto != null) {
                    valeursSaisies.put(v.getNom(), auto);
                } else if (Boolean.TRUE.equals(v.getObligatoire())) {
                    String quoi = v.getValeurAuto() == SourceValeurAuto.DATE_DEBUT ? "date de début" : "date de fin";
                    throw new ValidationMetierException("L'article « " + modele.getTitre() + " » utilise la "
                            + quoi + " du bail : renseignez-la dans la section Durée avant d'ajouter cet article.");
                }
            }
        }

        for (VariableArticle v : variablesDefinies) {
            if (Boolean.TRUE.equals(v.getObligatoire())) {
                String val = valeursSaisies.get(v.getNom());
                if (val == null || val.isBlank()) {
                    throw new ValidationMetierException("La variable '" + v.getLibelle() + "' est obligatoire pour l'article " + modele.getTitre() + ".");
                }
            }
        }

        for (VariableArticle v : variablesDefinies) {
            String val = valeursSaisies.get(v.getNom());
            if (v.getType() == TypeVariable.LISTE && val != null && !val.isBlank()
                    && !v.getOptionsListe().contains(val)) {
                throw new ValidationMetierException("La valeur choisie pour '" + v.getLibelle()
                        + "' ne fait pas partie de la liste autorisée.");
            }
        }

        String contenuFinal = VariableSubstitutionUtil.substituer(modele.getContenuModele(), valeursSaisies);

        ContratArticle contratArticle = ContratArticle.builder()
                .contrat(contrat)
                .modeleArticle(modele)
                .ordre(ordre)
                .contenuFinal(contenuFinal)
                .build();
        contratArticle = contratArticleRepository.save(contratArticle);

        for (VariableArticle v : variablesDefinies) {
            String valeur = valeursSaisies.get(v.getNom());
            if (valeur != null) {
                ContratVariable cv = ContratVariable.builder()
                        .contratArticle(contratArticle)
                        .variable(v)
                        .valeur(valeur)
                        .build();
                contratVariableRepository.save(cv);
            }
        }

        return contratArticle;
    }

    private void verifierModifiable(Contrat contrat) {
        if (contrat.getStatut() == StatutContrat.SIGNE || contrat.getStatut() == StatutContrat.ARCHIVE) {
            throw new ValidationMetierException(
                    "Ce contrat est signé ou archivé : il ne peut plus être modifié. Utilisez un avenant pour le compléter.");
        }
    }

    private String valeurAutomatique(SourceValeurAuto source, Contrat contrat) {
        LocalDate date = source == SourceValeurAuto.DATE_DEBUT ? contrat.getDateDebut() : contrat.getDateFin();
        return date == null ? null : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public void supprimerArticle(Long idContrat, Long idContratArticle) {
        ContratArticle article = contratArticleRepository.findById(idContratArticle)
                .orElseThrow(() -> new RessourceIntrouvableException("Article introuvable"));
        verifierModifiable(article.getContrat());
        if (!article.getContrat().getId().equals(idContrat)) {
            throw new ValidationMetierException("Cet article n'appartient pas à ce contrat.");
        }
        contratArticleRepository.delete(article);
    }

    public void reordonnerArticles(Long idContrat, List<Long> idsEnOrdre) {
        Contrat contrat = obtenir(idContrat);
        verifierModifiable(contrat);
        Map<Long, ContratArticle> parId = contrat.getArticles().stream()
                .collect(Collectors.toMap(ContratArticle::getId, a -> a));
        int ordre = 1;
        for (Long id : idsEnOrdre) {
            ContratArticle a = parId.get(id);
            if (a != null) {
                a.setOrdre(ordre++);
                contratArticleRepository.save(a);
            }
        }
    }

    // ------------------------------------------------------------------
    // Signatures
    // ------------------------------------------------------------------

    public Signature signer(Long idContrat, SignatureRequest requete) {
        Contrat contrat = obtenir(idContrat);
        if (requete.getSignatureData() == null || requete.getSignatureData().isBlank()) {
            throw new ValidationMetierException("La signature est vide.");
        }
        if (requete.getIdPersonne() == null) {
            throw new ValidationMetierException("Sélectionnez un signataire.");
        }
        if (contrat.getStatut() == StatutContrat.ARCHIVE) {
            throw new ValidationMetierException("Ce contrat est archivé : il ne peut plus être signé.");
        }
        Personne personne = personneRepository.findById(requete.getIdPersonne())
                .orElseThrow(() -> new RessourceIntrouvableException("Personne introuvable"));

        List<Personne> attendus = signatairesAttendus(contrat);
        if (attendus.stream().noneMatch(p -> p.getId().equals(personne.getId()))) {
            throw new ValidationMetierException("Cette personne n'est pas partie au contrat.");
        }
        Set<Long> idsSignes = contrat.getSignatures().stream()
                .map(sg -> sg.getPersonne().getId()).collect(Collectors.toSet());
        if (idsSignes.contains(personne.getId())) {
            throw new ValidationMetierException(personne.getNomComplet() + " a déjà signé ce contrat.");
        }

        Signature signature = Signature.builder()
                .contrat(contrat)
                .personne(personne)
                .signatureData(requete.getSignatureData())
                .typeSignature(com.madagascar.contratsbail.entity.enums.TypeSignature.DESSINEE)
                .ordre(contrat.getSignatures().size())
                .build();
        signature = signatureRepository.save(signature);
        contrat.getSignatures().add(signature);
        idsSignes.add(personne.getId());

        if (contrat.getStatut() == StatutContrat.BROUILLON) {
            contrat.setStatut(StatutContrat.EN_ATTENTE_SIGNATURE);
            statutContratService.ajouter(contrat, StatutContrat.EN_ATTENTE_SIGNATURE);
        }
        // Toutes les parties (personnes / representants) ont signe : le contrat est SIGNE
        boolean tousOntSigne = attendus.stream().allMatch(p -> idsSignes.contains(p.getId()));
        if (tousOntSigne && contrat.getStatut() != StatutContrat.SIGNE) {
            contrat.setStatut(StatutContrat.SIGNE);
            contrat.setDateSignature(LocalDateTime.now());
            statutContratService.ajouter(contrat, StatutContrat.SIGNE);
        }
        contratRepository.save(contrat);
        return signature;
    }

    // ------------------------------------------------------------------
    // PDF et archivage
    // ------------------------------------------------------------------

    public byte[] genererPdf(Long idContrat) {
        Contrat contrat = obtenir(idContrat);
        return pdfGenerationService.genererPdf(contrat);
    }

    public DocumentContrat archiver(Long idContrat) {
        Contrat contrat = obtenir(idContrat);
        if (contrat.getStatut() == StatutContrat.ARCHIVE) {
            throw new ValidationMetierException("Ce contrat est déjà archivé.");
        }

        if (signatureObligatoireAvantArchivage) {
            List<Personne> signatairesAttendus = signatairesAttendus(contrat);
            Set<Long> idsSignes = contrat.getSignatures().stream()
                    .map(s -> s.getPersonne().getId()).collect(Collectors.toSet());
            for (Personne p : signatairesAttendus) {
                if (!idsSignes.contains(p.getId())) {
                    throw new ValidationMetierException(
                            "Toutes les parties doivent signer avant l'archivage (manquant : " + p.getNomComplet() + ").");
                }
            }
        }

        byte[] pdf = pdfGenerationService.genererPdf(contrat);
        DocumentContrat document = documentStorageService.enregistrer(contrat, pdf, true);

        contrat.setStatut(StatutContrat.ARCHIVE);
        contrat.setDateArchivage(LocalDateTime.now());
        if (contrat.getDateSignature() == null) {
            contrat.setDateSignature(LocalDateTime.now());
        }
        statutContratService.ajouter(contrat, StatutContrat.SIGNE);
        statutContratService.ajouter(contrat, StatutContrat.ARCHIVE);
        contratRepository.save(contrat);
        statutContratService.synchroniserExpiration(contrat);

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

    // ------------------------------------------------------------------
    // Consultation / recherche
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Contrat obtenir(Long id) {
        Contrat contrat = contratRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Contrat introuvable : id=" + id));
        return contrat;
    }

    @Transactional(readOnly = true)
    public Contrat obtenirParNumero(String numero) {
        return contratRepository.findByNumero(numero)
                .orElseThrow(() -> new RessourceIntrouvableException("Contrat introuvable : numero=" + numero));
    }

    @Transactional(readOnly = true)
    public List<Contrat> listerTous() {
        return contratRepository.findAllByOrderByDateCreationDesc();
    }

    @Transactional(readOnly = true)
    public List<Contrat> listerArchives() {
        return contratRepository.findByStatutOrderByDateCreationDesc(StatutContrat.ARCHIVE);
    }

    @Transactional(readOnly = true)
    public List<Contrat> rechercher(String terme, StatutContrat statut, LocalDate dateDebutMin, LocalDate dateFinMax) {
        LocalDateTime debut = dateDebutMin != null ? dateDebutMin.atStartOfDay() : null;
        LocalDateTime fin = dateFinMax != null ? dateFinMax.atTime(23, 59, 59) : null;

        Specification<Contrat> specification = Specification
                .where(ContratSpecifications.avecTerme(terme))
                .and(ContratSpecifications.avecStatut(statut))
                .and(ContratSpecifications.dateCreationApres(debut))
                .and(ContratSpecifications.dateCreationAvant(fin));

        return contratRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "dateCreation"));
    }
}
