package com.madagascar.contratsbail.dto;

import lombok.Data;

@Data
public class RepresentantDto {
    private Long id;
    private PersonneDto personne;
    private String fonction;
    private Integer ordre;
}
