package com.madagascar.contratsbail.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Requete complete de creation d'un contrat (formulaire cote gauche de l'ecran). */
@Data
public class CreationContratRequest {
    private LocalDate dateDebut;
    private LocalDate dateFin;

    private BienDto bien;

    private PartieDto proprietaire;
    private PartieDto locataire;

    private List<AjoutArticleRequest> articles = new ArrayList<>();
}
