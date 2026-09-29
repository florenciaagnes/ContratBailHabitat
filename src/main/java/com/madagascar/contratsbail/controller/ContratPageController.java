package com.madagascar.contratsbail.controller;

import com.madagascar.contratsbail.entity.Contrat;
import com.madagascar.contratsbail.entity.enums.StatutContrat;
import com.madagascar.contratsbail.service.ArticleService;
import com.madagascar.contratsbail.service.AvenantService;
import com.madagascar.contratsbail.service.ContratService;
import com.madagascar.contratsbail.util.TypesBien;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/contrats")
public class ContratPageController {

    private final ContratService contratService;
    private final ArticleService articleService;
    private final AvenantService avenantService;

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("contrats", contratService.listerTous());
        return "contrats/liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("articlesDisponibles", articleService.listerActifs());
        model.addAttribute("typesBien", TypesBien.VALEURS);
        return "contrats/nouveau";
    }

    @GetMapping("/{id}")
    public String voir(@PathVariable Long id, Model model) {
        Contrat contrat = contratService.obtenir(id);
        model.addAttribute("contrat", contrat);
        model.addAttribute("articlesDisponibles", articleService.listerActifs());
        model.addAttribute("avenants", avenantService.listerParContrat(id));
        model.addAttribute("dateFinEffective", avenantService.dateFinEffective(id));
        return "contrats/voir";
    }

    @GetMapping("/archives")
    public String archives(@RequestParam(required = false) String terme,
                            @RequestParam(required = false) StatutContrat statut,
                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateDebut,
                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateFin,
                            Model model) {
        List<Contrat> resultats = (terme != null && !terme.isBlank()) || statut != null || dateDebut != null || dateFin != null
                ? contratService.rechercher(terme, statut, dateDebut, dateFin)
                : contratService.listerArchives();
        model.addAttribute("contrats", resultats);
        model.addAttribute("terme", terme);
        model.addAttribute("statut", statut);
        model.addAttribute("dateDebut", dateDebut);
        model.addAttribute("dateFin", dateFin);
        model.addAttribute("statuts", StatutContrat.values());
        return "contrats/archives";
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> voirPdf(@PathVariable Long id) {
        Contrat contrat = contratService.obtenir(id);
        byte[] pdf = contratService.genererPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + contrat.getNumero() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = "/{id}/pdf/telecharger", produces = MediaType.APPLICATION_PDF_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> telechargerPdf(@PathVariable Long id) {
        Contrat contrat = contratService.obtenir(id);
        byte[] pdf = contratService.genererPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + contrat.getNumero() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/archiver")
    public String archiver(@PathVariable Long id) {
        contratService.archiver(id);
        return "redirect:/contrats/" + id;
    }
}
