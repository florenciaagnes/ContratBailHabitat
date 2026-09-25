package com.legaltech.bail.entity;

import lombok.Getter;

@Getter
public enum Gender {
    HOMME("Homme"),
    FEMME("Femme");

    private final String label;

    Gender(String label) {
        this.label = label;
    }
}
