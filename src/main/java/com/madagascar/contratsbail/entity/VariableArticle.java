package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.madagascar.contratsbail.entity.enums.TypeVariable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "variable_article")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariableArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_modele_article", nullable = false)
    @JsonIgnore
    private ModeleArticle modeleArticle;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 200)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeVariable type;

    @Column(nullable = false)
    @Builder.Default
    private Boolean obligatoire = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer ordre = 0;
}
