package com.madagascar.contratsbail.dto;

import com.madagascar.contratsbail.entity.enums.RolePartie;
import com.madagascar.contratsbail.entity.enums.TypePartie;
import lombok.Data;

@Data
public class PartieDto {
    private Long id;
    private TypePartie typePartie;
    private RolePartie role;
    private Integer ordre;
    private PersonneDto personne;
    private OrganisationDto organisation;
}
