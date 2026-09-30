package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.madagascar.contratsbail.entity.enums.SourceValeurAuto;
import com.madagascar.contratsbail.entity.enums.TypeVariable;
import jakarta.persistence.*;
import java.util.Arrays;
import java.util.List;
import lombok.*;

// Ignore les proprietes internes generees par Hibernate sur les proxys
// (HibernateProxy/ByteBuddy) : sans ca, serialiser une association LAZY
// pas encore chargee fait planter Jackson (voir README).
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    /**
     * Si renseigne, la variable n'est pas saisie par l'utilisateur : sa valeur
     * est reprise automatiquement du contrat (date de debut / date de fin).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "valeur_auto", length = 20)
    private SourceValeurAuto valeurAuto;

    /** Options d'une variable de type LISTE, une par ligne. */
    @Column(columnDefinition = "TEXT")
    private String options;

    /** Options nettoyees (sans lignes vides), utilisees par la liste deroulante. */
    public List<String> getOptionsListe() {
        if (options == null || options.isBlank()) {
            return List.of();
        }
        return Arrays.stream(options.split("\\R")).map(String::trim).filter(o -> !o.isEmpty()).toList();
    }
}
