package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Modele reutilisable de clause d'avenant : meme principe qu'un ModeleArticle. */
@Entity
@Table(name = "modele_clause_avenant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeleClauseAvenant {

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
    private Boolean actif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @OneToMany(mappedBy = "modeleClause", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Builder.Default
    private List<VariableClauseAvenant> variables = new ArrayList<>();

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
