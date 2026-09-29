package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.madagascar.contratsbail.entity.enums.StatutContrat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Une ligne de la table statut_contrat : un statut detenu par un contrat. */
@Entity
@Table(name = "statut_contrat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratStatut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrat", nullable = false)
    @JsonIgnore
    private Contrat contrat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutContrat statut;

    @Column(name = "date_statut", nullable = false)
    private LocalDateTime dateStatut;

    @PrePersist
    void prePersist() {
        if (dateStatut == null) {
            dateStatut = LocalDateTime.now();
        }
    }
}
