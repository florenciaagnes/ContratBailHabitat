package com.madagascar.contratsbail.dto;

import lombok.Data;

@Data
public class SignatureRequest {
    private Long idPersonne;
    /** Data URL base64 issue du canvas de signature (image/png;base64,...) */
    private String signatureData;
}
