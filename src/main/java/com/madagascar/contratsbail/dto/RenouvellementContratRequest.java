package com.madagascar.contratsbail.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RenouvellementContratRequest {
    private LocalDate nouvelleDateFin;
}
