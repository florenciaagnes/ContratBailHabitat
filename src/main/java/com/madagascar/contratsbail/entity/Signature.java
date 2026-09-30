package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.madagascar.contratsbail.entity.enums.TypeSignature;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Ignore les proprietes internes generees par Hibernate sur les proxys
// (HibernateProxy/ByteBuddy) : sans ca, serialiser une association LAZY
// pas encore chargee fait planter Jackson (voir README).
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "signature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrat", nullable = false)
    @JsonIgnore
    private Contrat contrat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_personne", nullable = false)
    private Personne personne;

    /** Image de la signature encodee en base64 (data URL issue du canvas HTML). */
    @Column(name = "signature_data", nullable = false, columnDefinition = "TEXT")
    private String signatureData;

    @Column(name = "date_signature", nullable = false)
    private LocalDateTime dateSignature;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_signature", nullable = false, length = 20)
    @Builder.Default
    private TypeSignature typeSignature = TypeSignature.DESSINEE;

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
