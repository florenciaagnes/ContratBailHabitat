package com.madagascar.contratsbail.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Remplace les variables de la forme {{nom_variable}} dans le contenu d'un modele d'article
 * par les valeurs saisies par l'utilisateur.
 */
public final class VariableSubstitutionUtil {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*\\}\\}");

    private VariableSubstitutionUtil() {
    }

    public static String substituer(String contenuModele, Map<String, String> valeurs) {
        if (contenuModele == null) {
            return "";
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(contenuModele);
        StringBuilder resultat = new StringBuilder();
        while (matcher.find()) {
            String nomVariable = matcher.group(1);
            String valeur = valeurs.getOrDefault(nomVariable, "[" + nomVariable + "]");
            matcher.appendReplacement(resultat, Matcher.quoteReplacement(valeur == null ? "" : valeur));
        }
        matcher.appendTail(resultat);
        return resultat.toString();
    }
}
