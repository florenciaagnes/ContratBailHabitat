package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.madagascar.contratsbail.entity.enums.StatutAvenant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "avenant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Avenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrat", nullable = false)
    @JsonIgnore
    private Contrat contrat;

    /** Ex. BAIL-2026-001-AV1 */
    @Column(nullable = false, unique = true, length = 60)
    private String numero;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    @Column(nullable = false, length = 300)
    private String objet;

    /** Renseignee uniquement si l'avenant modifie la date de fin du bail. */
    @Column(name = "nouvelle_date_fin")
    private LocalDate nouvelleDateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutAvenant statut = StatutAvenant.BROUILLON;

    @Column(name = "date_archivage")
    private LocalDateTime dateArchivage;

    @OneToMany(mappedBy = "avenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @JsonIgnore
    @Builder.Default
    private List<AvenantClause> clauses = new ArrayList<>();

    @OneToMany(mappedBy = "avenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @JsonIgnore
    @Builder.Default
    private List<SignatureAvenant> signatures = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
