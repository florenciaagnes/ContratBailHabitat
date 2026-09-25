package com.madagascar.contratsbail.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "modele_article")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeleArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(name = "contenu_modele", nullable = false, columnDefinition = "TEXT")
    private String contenuModele;

    @Column(name = "ordre_defaut", nullable = false)
    @Builder.Default
    private Integer ordreDefaut = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean obligatoire = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    // Pas de @JsonIgnore ici : le frontend a besoin de cette liste pour afficher
    // dynamiquement les champs de saisie (ex. montant_loyer) dans la popup
    // "+ Ajouter un article". Seule la reference retour (VariableArticle.modeleArticle)
    // est ignorée, pour eviter la boucle infinie de serialisation.
    @OneToMany(mappedBy = "modeleArticle", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Builder.Default
    private List<VariableArticle> variables = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        dateModification = LocalDateTime.now();
    }
}
