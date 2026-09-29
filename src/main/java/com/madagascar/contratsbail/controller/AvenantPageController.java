package com.madagascar.contratsbail.controller;

import com.madagascar.contratsbail.entity.Avenant;
import com.madagascar.contratsbail.service.AvenantService;
import com.madagascar.contratsbail.service.ContratService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AvenantPageController {

    private final AvenantService avenantService;
    private final ContratService contratService;

    @GetMapping("/contrats/{id}/avenants/nouveau")
    public String nouveau(@PathVariable Long id, Model model) {
        model.addAttribute("contrat", contratService.obtenir(id));
        return "avenants/nouveau";
    }

    @GetMapping("/avenants/{id}")
    public String voir(@PathVariable Long id, Model model) {
        Avenant avenant = avenantService.obtenir(id);
        model.addAttribute("avenant", avenant);
        model.addAttribute("contrat", avenant.getContrat());
        return "avenants/voir";
    }

    @GetMapping(value = "/avenants/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> voirPdf(@PathVariable Long id) {
        Avenant avenant = avenantService.obtenir(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + avenant.getNumero() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(avenantService.genererPdf(id));
    }

    @GetMapping(value = "/avenants/{id}/pdf/telecharger", produces = MediaType.APPLICATION_PDF_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> telechargerPdf(@PathVariable Long id) {
        Avenant avenant = avenantService.obtenir(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + avenant.getNumero() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(avenantService.genererPdf(id));
    }

    @PostMapping("/avenants/{id}/archiver")
    public String archiver(@PathVariable Long id) {
        avenantService.archiver(id);
        return "redirect:/avenants/" + id;
    }
}
