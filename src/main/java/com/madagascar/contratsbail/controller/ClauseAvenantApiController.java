package com.madagascar.contratsbail.controller;

import com.madagascar.contratsbail.entity.ModeleClauseAvenant;
import com.madagascar.contratsbail.entity.VariableClauseAvenant;
import com.madagascar.contratsbail.service.ClauseAvenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clauses-avenant")
public class ClauseAvenantApiController {

    private final ClauseAvenantService clauseAvenantService;

    @GetMapping
    public List<ModeleClauseAvenant> rechercher(@RequestParam(required = false) String recherche) {
        return clauseAvenantService.rechercher(recherche);
    }

    @GetMapping("/{id}")
    public ModeleClauseAvenant obtenir(@PathVariable Long id) {
        return clauseAvenantService.obtenir(id);
    }

    @PostMapping
    public ResponseEntity<ModeleClauseAvenant> creer(@RequestBody ModeleClauseAvenant modele) {
        return ResponseEntity.ok(clauseAvenantService.creer(modele));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModeleClauseAvenant> modifier(@PathVariable Long id, @RequestBody ModeleClauseAvenant donnees) {
        return ResponseEntity.ok(clauseAvenantService.modifier(id, donnees));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        clauseAvenantService.desactiver(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/variables")
    public ResponseEntity<VariableClauseAvenant> ajouterVariable(@PathVariable Long id, @RequestBody VariableClauseAvenant variable) {
        return ResponseEntity.ok(clauseAvenantService.ajouterVariable(id, variable));
    }

    @PutMapping("/{id}/variables/{idVariable}")
    public ResponseEntity<VariableClauseAvenant> modifierVariable(@PathVariable Long id, @PathVariable Long idVariable,
                                                                  @RequestBody VariableClauseAvenant variable) {
        return ResponseEntity.ok(clauseAvenantService.modifierVariable(id, idVariable, variable));
    }
}
