package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

// Ignore les proprietes internes generees par Hibernate sur les proxys
// (HibernateProxy/ByteBuddy) : sans ca, serialiser une association LAZY
// pas encore chargee fait planter Jackson (voir README).
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "avenant_clause")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvenantClause {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_avenant", nullable = false)
    @JsonIgnore
    private Avenant avenant;

    @Column(nullable = false)
    private Integer ordre;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;
}
