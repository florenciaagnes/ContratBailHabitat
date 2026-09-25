package com.madagascar.contratsbail.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BienDto {
    private Long id;
    private String adresse;
    private String description;
    private String typeBien;
    private BigDecimal surface;
    private String reference;
}
