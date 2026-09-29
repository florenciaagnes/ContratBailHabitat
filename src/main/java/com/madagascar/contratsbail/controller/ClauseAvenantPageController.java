package com.madagascar.contratsbail.controller;

import com.madagascar.contratsbail.entity.ModeleClauseAvenant;
import com.madagascar.contratsbail.entity.enums.TypeVariable;
import com.madagascar.contratsbail.service.ClauseAvenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/clauses-avenant")
public class ClauseAvenantPageController {

    private final ClauseAvenantService clauseAvenantService;

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("clauses", clauseAvenantService.listerTous());
        return "clauses-avenant/liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("clause", new ModeleClauseAvenant());
        model.addAttribute("typesVariable", TypeVariable.values());
        return "clauses-avenant/form";
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id, Model model) {
        model.addAttribute("clause", clauseAvenantService.obtenir(id));
        model.addAttribute("typesVariable", TypeVariable.values());
        return "clauses-avenant/form";
    }
}
