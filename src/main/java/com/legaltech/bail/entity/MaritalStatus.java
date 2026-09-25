package com.legaltech.bail.entity;

import lombok.Getter;

@Getter
public enum MaritalStatus {
    CELIBATAIRE("Célibataire"),
    MARIE("Marié(e)"),
    DIVORCE("Divorcé(e)"),
    VEUF("Veuf / Veuve");

    private final String label;

    MaritalStatus(String label) {
        this.label = label;
    }
}
