package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.madagascar.contratsbail.entity.enums.RolePartie;
import com.madagascar.contratsbail.entity.enums.TypePartie;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "partie_contrat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartieContrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrat", nullable = false)
    @JsonIgnore
    private Contrat contrat;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_partie", nullable = false, length = 20)
    private TypePartie typePartie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_personne")
    private Personne personne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_organisation")
    private Organisation organisation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolePartie role;

    @Column(nullable = false)
    private Integer ordre;

    /** Nom d'affichage, quelle que soit la nature de la partie (personne ou organisation). */
    public String getNomAffichage() {
        if (typePartie == TypePartie.PERSONNE && personne != null) {
            return personne.getNomComplet();
        }
        if (typePartie == TypePartie.ORGANISATION && organisation != null) {
            return organisation.getNom();
        }
        return "";
    }
}
