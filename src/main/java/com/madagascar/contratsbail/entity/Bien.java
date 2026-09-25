package com.madagascar.contratsbail.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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
