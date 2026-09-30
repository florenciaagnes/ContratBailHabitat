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
@Table(name = "representant_organisation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepresentantOrganisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_organisation", nullable = false)
    @JsonIgnore
    private Organisation organisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_personne", nullable = false)
    private Personne personne;

    @Column(length = 150)
    private String fonction;

    @Column(nullable = false)
    private Integer ordre;
}
