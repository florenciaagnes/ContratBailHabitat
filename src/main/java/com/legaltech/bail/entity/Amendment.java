package com.legaltech.bail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "amendments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amendment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_contract_id", nullable = false)
    private Contract parentContract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_contract_id", nullable = false)
    private Contract newContract;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motif;

    @Column(nullable = false)
    private LocalDateTime date;
}
