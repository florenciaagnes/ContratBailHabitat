package com.madagascar.contratsbail.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "personne")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Personne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 150)
    private String prenom;

    @Column(length = 50)
    private String cin;

    @Column(name = "date_delivrance_cin")
    private LocalDate dateDelivranceCin;

    @Column(name = "lieu_delivrance_cin", length = 150)
    private String lieuDelivranceCin;

    @Column(length = 500)
    private String adresse;

    @Column(length = 50)
    private String telephone;

    @Column(length = 150)
    private String email;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

    public String getNomComplet() {
        StringBuilder sb = new StringBuilder(nom == null ? "" : nom);
        if (prenom != null && !prenom.isBlank()) {
            sb.append(" ").append(prenom);
        }
        return sb.toString().trim();
    }
}
