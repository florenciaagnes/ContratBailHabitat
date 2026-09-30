package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.madagascar.contratsbail.entity.enums.StatutContrat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Ignore les proprietes internes generees par Hibernate sur les proxys
// (HibernateProxy/ByteBuddy) : sans ca, serialiser une association LAZY
// pas encore chargee fait planter Jackson (voir README).
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "contrat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String numero;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_bien", nullable = false)
    private Bien bien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutContrat statut = StatutContrat.BROUILLON;

    @Column(name = "date_signature")
    private LocalDateTime dateSignature;

    @Column(name = "date_archivage")
    private LocalDateTime dateArchivage;

    /** Renseigne quand ce contrat est un renouvellement (tacite reconduction) d'un contrat expire. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrat_origine")
    @JsonIgnore
    private Contrat contratOrigine;

    @OneToMany(mappedBy = "contrat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @JsonIgnore
    @Builder.Default
    private List<PartieContrat> parties = new ArrayList<>();

    @OneToMany(mappedBy = "contrat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @JsonIgnore
    @Builder.Default
    private List<ContratArticle> articles = new ArrayList<>();

    @OneToMany(mappedBy = "contrat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @JsonIgnore
    @Builder.Default
    private List<Signature> signatures = new ArrayList<>();

    @OneToMany(mappedBy = "contrat", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<DocumentContrat> documents = new ArrayList<>();

    @OneToMany(mappedBy = "contrat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateStatut ASC, id ASC")
    @JsonIgnore
    @Builder.Default
    private List<ContratStatut> statuts = new ArrayList<>();

    /** Tous les statuts detenus par le contrat, dans l'ordre d'affichage (BROUILLON ... EXPIRE). */
    public List<StatutContrat> getStatutsAffiches() {
        return statuts.stream().map(ContratStatut::getStatut).distinct().sorted().toList();
    }

    /** Vrai si le contrat porte actuellement le statut EXPIRE (utilisable en Thymeleaf : contrat.expire). */
    public boolean isExpire() {
        return statuts.stream().anyMatch(cs -> cs.getStatut() == StatutContrat.EXPIRE);
    }

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
