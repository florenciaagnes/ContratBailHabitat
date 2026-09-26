package com.madagascar.contratsbail.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mode_payement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModePayement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "libelle", length = 100)
    private String libelle;   
}
