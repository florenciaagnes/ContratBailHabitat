package com.madagascar.contratsbail.controller;

import com.madagascar.contratsbail.dto.AjoutArticleRequest;
import com.madagascar.contratsbail.dto.CreationContratRequest;
import com.madagascar.contratsbail.dto.SignatureRequest;
import com.madagascar.contratsbail.entity.Contrat;
import com.madagascar.contratsbail.entity.ContratArticle;
import com.madagascar.contratsbail.entity.DocumentContrat;
import com.madagascar.contratsbail.entity.Signature;
import com.madagascar.contratsbail.entity.enums.StatutContrat;
import com.madagascar.contratsbail.service.ContratService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contrats")
public class ContratApiController {

    private final ContratService contratService;

    @PostMapping
    public ResponseEntity<Contrat> creer(@RequestBody CreationContratRequest requete) {
        Contrat contrat = contratService.creerContrat(requete);
        return ResponseEntity.ok(contrat);
    }

    @GetMapping("/{id}")
    public Contrat obtenir(@PathVariable Long id) {
        return contratService.obtenir(id);
    }

    @GetMapping("/recherche")
    public List<Contrat> rechercher(@RequestParam(required = false) String terme,
                                     @RequestParam(required = false) StatutContrat statut,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return contratService.rechercher(terme, statut, dateDebut, dateFin);
    }

    @PostMapping("/{id}/articles")
    public ResponseEntity<ContratArticle> ajouterArticle(@PathVariable Long id, @RequestBody AjoutArticleRequest requete) {
        return ResponseEntity.ok(contratService.ajouterArticle(id, requete));
    }

    @DeleteMapping("/{id}/articles/{articleId}")
    public ResponseEntity<Void> supprimerArticle(@PathVariable Long id, @PathVariable Long articleId) {
        contratService.supprimerArticle(id, articleId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/articles/ordre")
    public ResponseEntity<Void> reordonner(@PathVariable Long id, @RequestBody List<Long> idsEnOrdre) {
        contratService.reordonnerArticles(id, idsEnOrdre);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/signatures")
    public ResponseEntity<Signature> signer(@PathVariable Long id, @RequestBody SignatureRequest requete) {
        return ResponseEntity.ok(contratService.signer(id, requete));
    }

    @PostMapping("/{id}/archiver")
    public ResponseEntity<DocumentContrat> archiver(@PathVariable Long id) {
        return ResponseEntity.ok(contratService.archiver(id));
    }
}
