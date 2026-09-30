package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Ignore les proprietes internes generees par Hibernate sur les proxys
// (HibernateProxy/ByteBuddy) : sans ca, serialiser une association LAZY
// pas encore chargee fait planter Jackson (voir README).
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "organisation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(name = "forme_juridique", length = 100)
    private String formeJuridique;

    @Column(name = "siege_social", length = 500)
    private String siegeSocial;

    @Column(length = 500)
    private String adresse;

    @Column(length = 50)
    private String telephone;

    @Column(length = 150)
    private String email;

    @Column(name = "numero_identification", length = 100)
    private String numeroIdentification;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @OneToMany(mappedBy = "organisation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @JsonIgnore
    @Builder.Default
    private List<RepresentantOrganisation> representants = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
