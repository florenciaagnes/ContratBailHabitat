package com.madagascar.contratsbail.controller;

import com.madagascar.contratsbail.entity.ModeleArticle;
import com.madagascar.contratsbail.entity.enums.TypeVariable;
import com.madagascar.contratsbail.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/articles")
public class ArticlePageController {

    private final ArticleService articleService;

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("articles", articleService.listerTous());
        return "articles/liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("article", new ModeleArticle());
        model.addAttribute("typesVariable", TypeVariable.values());
        return "articles/form";
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id, Model model) {
        model.addAttribute("article", articleService.obtenir(id));
        model.addAttribute("typesVariable", TypeVariable.values());
        return "articles/form";
    }
}
