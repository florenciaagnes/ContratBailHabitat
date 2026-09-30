package com.madagascar.contratsbail.controller;

import com.madagascar.contratsbail.entity.ModeleArticle;
import com.madagascar.contratsbail.entity.VariableArticle;
import com.madagascar.contratsbail.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
public class ArticleApiController {

    private final ArticleService articleService;

    @GetMapping
    public List<ModeleArticle> rechercher(@RequestParam(required = false) String recherche) {
        return articleService.rechercher(recherche);
    }

    @GetMapping("/{id}")
    public ModeleArticle obtenir(@PathVariable Long id) {
        return articleService.obtenir(id);
    }

    @PostMapping
    public ResponseEntity<ModeleArticle> creer(@RequestBody ModeleArticle modeleArticle) {
        return ResponseEntity.ok(articleService.creer(modeleArticle));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModeleArticle> modifier(@PathVariable Long id, @RequestBody ModeleArticle donnees) {
        return ResponseEntity.ok(articleService.modifier(id, donnees));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        articleService.desactiver(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/variables/{idVariable}")
    public ResponseEntity<VariableArticle> modifierVariable(@PathVariable Long id, @PathVariable Long idVariable,
                                                            @RequestBody VariableArticle variable) {
        return ResponseEntity.ok(articleService.modifierVariable(id, idVariable, variable));
    }

    @PostMapping("/{id}/variables")
    public ResponseEntity<VariableArticle> ajouterVariable(@PathVariable Long id, @RequestBody VariableArticle variable) {
        return ResponseEntity.ok(articleService.ajouterVariable(id, variable));
    }
}
