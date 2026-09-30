package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

// Ignore les proprietes internes generees par Hibernate sur les proxys
// (HibernateProxy/ByteBuddy) : sans ca, serialiser une association LAZY
// pas encore chargee fait planter Jackson (voir README).
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "bien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String adresse;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "type_bien", length = 100)
    private String typeBien;

    private BigDecimal surface;

    @Column(length = 100)
    private String reference;
}
