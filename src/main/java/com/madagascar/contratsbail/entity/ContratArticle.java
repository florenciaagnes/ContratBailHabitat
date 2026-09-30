package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

// Ignore les proprietes internes generees par Hibernate sur les proxys
// (HibernateProxy/ByteBuddy) : sans ca, serialiser une association LAZY
// pas encore chargee fait planter Jackson (voir README).
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "contrat_article")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrat", nullable = false)
    @JsonIgnore
    private Contrat contrat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_modele_article", nullable = false)
    private ModeleArticle modeleArticle;

    @Column(nullable = false)
    private Integer ordre;

    /**
     * Texte final de l'article au moment de la creation du contrat.
     * Ne doit JAMAIS etre recalcule automatiquement si le modele change ensuite.
     */
    @Column(name = "contenu_final", nullable = false, columnDefinition = "TEXT")
    private String contenuFinal;

    @OneToMany(mappedBy = "contratArticle", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<ContratVariable> variables = new ArrayList<>();
}
