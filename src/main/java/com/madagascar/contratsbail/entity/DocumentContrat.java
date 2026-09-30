package com.madagascar.contratsbail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Ignore les proprietes internes generees par Hibernate sur les proxys
// (HibernateProxy/ByteBuddy) : sans ca, serialiser une association LAZY
// pas encore chargee fait planter Jackson (voir README).
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "document_contrat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentContrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrat", nullable = false)
    @JsonIgnore
    private Contrat contrat;

    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;

    @Column(name = "chemin_fichier", nullable = false, length = 1000)
    private String cheminFichier;

    @Column(name = "type_document", nullable = false, length = 50)
    @Builder.Default
    private String typeDocument = "PDF_CONTRAT";

    @Column(name = "date_generation", nullable = false)
    private LocalDateTime dateGeneration;

    @Column(name = "date_archivage")
    private LocalDateTime dateArchivage;

    @Column(name = "hash_document", length = 128)
    private String hashDocument;

    @PrePersist
    void prePersist() {
        if (dateGeneration == null) {
            dateGeneration = LocalDateTime.now();
        }
    }
}
