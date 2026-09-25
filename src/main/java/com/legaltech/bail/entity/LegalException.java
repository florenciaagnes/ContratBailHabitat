package com.legaltech.bail.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "legal_exceptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "clause_index", nullable = false)
    private Integer clauseIndex;

    @Column(name = "source_document", nullable = false, length = 255)
    private String sourceDocument;

    @Column(name = "page_reference", nullable = false, length = 50)
    private String pageReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "extrait_loi", nullable = false, columnDefinition = "TEXT")
    private String extraitLoi;

    @Column(nullable = false)
    @Builder.Default
    private Boolean resolu = false;
}
