package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contrat_variable")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrat_article", nullable = false)
    @JsonIgnore
    private ContratArticle contratArticle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_variable", nullable = false)
    private VariableArticle variable;

    @Column(columnDefinition = "TEXT")
    private String valeur;
}
