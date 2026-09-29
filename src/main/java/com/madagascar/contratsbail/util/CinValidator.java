package com.madagascar.contratsbail.util;

import com.madagascar.contratsbail.exception.ValidationMetierException;

/**
 * Validation du numero de CIN malgache : exactement 12 chiffres.
 * - chiffres 1 a 5 : code administratif / geographique de la commune
 * - chiffre 6      : 1 = homme, 2 = femme (aucune autre valeur acceptee)
 * - chiffres 7 a 12 : numero sequentiel
 *
 * Le code commune n'est verifie que sur son format (5 chiffres) : aucune
 * liste officielle des communes n'est embarquee dans l'application.
 */
public final class CinValidator {

    public static final int LONGUEUR = 12;

    private CinValidator() {
    }

    /** Retire les separateurs courants (espaces, points, tirets) saisis par l'utilisateur. */
    public static String normaliser(String cin) {
        return cin == null ? null : cin.replaceAll("[\\s.\\-]", "");
    }

    /** @throws ValidationMetierException si le CIN est invalide. */
    public static void valider(String cin, String libelle) {
        String n = normaliser(cin);
        if (n == null || n.isEmpty()) {
            throw new ValidationMetierException("Le CIN " + libelle + " est obligatoire.");
        }
        if (!n.matches("[0-9]+")) {
            throw new ValidationMetierException("Le CIN " + libelle + " ne doit contenir que des chiffres.");
        }
        if (n.length() != LONGUEUR) {
            throw new ValidationMetierException("Le CIN " + libelle + " doit contenir exactement "
                    + LONGUEUR + " chiffres (" + n.length() + " saisis).");
        }
        char sexe = n.charAt(5);
        if (sexe != '1' && sexe != '2') {
            throw new ValidationMetierException("Le CIN " + libelle
                    + " est invalide : le 6e chiffre doit être 1 (homme) ou 2 (femme).");
        }
    }
}
