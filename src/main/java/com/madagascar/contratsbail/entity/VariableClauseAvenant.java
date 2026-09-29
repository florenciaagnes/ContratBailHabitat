package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.madagascar.contratsbail.entity.enums.TypeVariable;
import jakarta.persistence.*;
import lombok.*;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "variable_clause_avenant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariableClauseAvenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_modele_clause", nullable = false)
    @JsonIgnore
    private ModeleClauseAvenant modeleClause;

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

    /** Options d'une variable de type LISTE, une par ligne. */
    @Column(columnDefinition = "TEXT")
    private String options;

    public List<String> getOptionsListe() {
        if (options == null || options.isBlank()) {
            return List.of();
        }
        return Arrays.stream(options.split("\\R")).map(String::trim).filter(o -> !o.isEmpty()).toList();
    }
}
