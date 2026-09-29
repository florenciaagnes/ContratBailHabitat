package com.madagascar.contratsbail.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class CreationAvenantRequest {
    private LocalDate dateEffet;
    private String objet;
    /** Optionnelle : renseignee si l'avenant proroge ou modifie la date de fin du bail. */
    private LocalDate nouvelleDateFin;
    private List<AjoutClauseAvenantRequest> clauses = new ArrayList<>();
}
