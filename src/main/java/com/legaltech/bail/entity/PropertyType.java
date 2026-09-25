package com.legaltech.bail.entity;

public enum PropertyType {
    APPARTEMENT("Appartement"),
    MAISON("Maison Individuelle"),
    VILLA("Villa / Résidence"),
    STUDIO("Studio / T1"),
    COMMERCIAL("Local Commercial / Bureau"),
    IMMEUBLE("Immeuble Entier");

    private final String label;

    PropertyType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
