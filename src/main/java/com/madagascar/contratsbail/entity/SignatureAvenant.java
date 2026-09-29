package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "signature_avenant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignatureAvenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_avenant", nullable = false)
    @JsonIgnore
    private Avenant avenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_personne", nullable = false)
    private Personne personne;

    @Column(name = "signature_data", nullable = false, columnDefinition = "TEXT")
    private String signatureData;

    @Column(name = "date_signature", nullable = false)
    private LocalDateTime dateSignature;

    @Column(nullable = false)
    @Builder.Default
    private Integer ordre = 0;

    @PrePersist
    void prePersist() {
        if (dateSignature == null) {
            dateSignature = LocalDateTime.now();
        }
    }
}
