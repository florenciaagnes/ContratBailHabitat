package com.madagascar.contratsbail.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class AjoutArticleRequest {
    private Long idModeleArticle;
    private List<VariableValeurDto> variables = new ArrayList<>();
}
