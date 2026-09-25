package com.legaltech.bail.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

public class AuditDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClauseDto {
        private Integer ordre;
        private String titre;
        private String texte;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditRequestDto {
        private BigDecimal rent_amount;
        private BigDecimal deposit_amount;
        private List<ClauseDto> clauses;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExceptionDto {
        private Integer clause_index;
        private String source_document;
        private String page_reference;
        private String extracted_law_text;
        private String severity;
        private String message;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditResponseDto {
        private String status;
        private List<ExceptionDto> exceptions;
    }
}
