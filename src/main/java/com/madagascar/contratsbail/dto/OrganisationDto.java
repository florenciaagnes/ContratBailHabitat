package com.madagascar.contratsbail.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrganisationDto {
    private Long id;
    private String nom;
    private String formeJuridique;
    private String siegeSocial;
    private String adresse;
    private String telephone;
    private String email;
    private String numeroIdentification;
    private List<RepresentantDto> representants = new ArrayList<>();
}
