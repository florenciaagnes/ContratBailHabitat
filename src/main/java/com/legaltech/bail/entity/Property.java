package com.legaltech.bail.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id")
    private User proprietaire;

    @Column(nullable = false, length = 255)
    private String adresse;

    @Column(length = 100)
    private String quartier;

    @Column(nullable = false, length = 100)
    private String ville;

    @Column(name = "ref_cadastre", length = 100)
    private String refCadastre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_bien", length = 50)
    @Builder.Default
    private PropertyType typeBien = PropertyType.APPARTEMENT;

    @Column(name = "superficie")
    private Double superficie;

    @Column(name = "nombre_pieces")
    private Integer nombrePieces;

    @Column(name = "nombre_chambres")
    private Integer nombreChambres;

    @Column(name = "etage")
    private Integer etage;

    @Column(name = "meuble")
    @Builder.Default
    private Boolean meuble = false;

    @Column(name = "parking")
    @Builder.Default
    private Boolean parking = false;

    @Column(name = "compteur_jirama")
    @Builder.Default
    private Boolean compteurJirama = true;
}
