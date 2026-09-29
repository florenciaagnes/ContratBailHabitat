package com.madagascar.contratsbail.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AjoutClauseAvenantRequest {
    private Long idModeleClause;
    private List<VariableValeurDto> variables = new ArrayList<>();
}
