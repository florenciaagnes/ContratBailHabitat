package com.legaltech.bail.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 30)
    private String telephone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "cin_numero", length = 30)
    private String cinNumero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(name = "date_naissance")
    private java.time.LocalDate dateNaissance;

    @Enumerated(EnumType.STRING)
    @Column(name = "genre", length = 20)
    private Gender genre;

    @Enumerated(EnumType.STRING)
    @Column(name = "situation_matrimoniale", length = 30)
    private MaritalStatus situationMatrimoniale;

    public String getNomComplet() {
        return prenom + " " + nom;
    }
}
