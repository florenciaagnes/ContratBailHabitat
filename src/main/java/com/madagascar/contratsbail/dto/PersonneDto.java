package com.madagascar.contratsbail.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PersonneDto {
    private Long id;
    private String nom;
    private String prenom;
    private String cin;
    private LocalDate dateDelivranceCin;
    private String lieuDelivranceCin;
    private String adresse;
    private String telephone;
    private String email;
}
