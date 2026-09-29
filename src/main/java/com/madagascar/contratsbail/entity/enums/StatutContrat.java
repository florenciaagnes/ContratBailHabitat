package com.madagascar.contratsbail.entity.enums;

/** L'ordre de declaration est l'ordre d'affichage des statuts d'un contrat. */
public enum StatutContrat {
    BROUILLON("Brouillon"),
    EN_ATTENTE_SIGNATURE("En attente de signature"),
    SIGNE("Signé"),
    ARCHIVE("Archivé"),
    RESILIE("Résilié"),
    EXPIRE("Expiré");

    private final String libelle;

    StatutContrat(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
