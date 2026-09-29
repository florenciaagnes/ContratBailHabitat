package com.madagascar.contratsbail.util;

import java.util.List;

/** Types de bien proposes dans la liste deroulante du formulaire de contrat. */
public final class TypesBien {

    public static final List<String> VALEURS =
            List.of("Appartement", "Studio", "Maison", "Villa", "Duplex", "Chambre", "Autre");

    private TypesBien() {
    }

    /** Vrai si vide (champ facultatif) ou si la valeur fait partie de la liste. */
    public static boolean estValide(String valeur) {
        return valeur == null || valeur.isBlank() || VALEURS.contains(valeur);
    }
}
