package com.madagascar.contratsbail.controller;

import com.madagascar.contratsbail.dto.CreationAvenantRequest;
import com.madagascar.contratsbail.dto.SignatureRequest;
import com.madagascar.contratsbail.entity.Avenant;
import com.madagascar.contratsbail.entity.SignatureAvenant;
import com.madagascar.contratsbail.service.AvenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AvenantApiController {

    private final AvenantService avenantService;

    @PostMapping("/api/contrats/{id}/avenants")
    public ResponseEntity<Map<String, Object>> creer(@PathVariable Long id,
                                                     @RequestBody CreationAvenantRequest requete) {
        Avenant avenant = avenantService.creer(id, requete);
        return ResponseEntity.ok(Map.of("id", avenant.getId(), "numero", avenant.getNumero()));
    }

    @PostMapping("/api/avenants/{id}/signatures")
    public ResponseEntity<Map<String, Object>> signer(@PathVariable Long id,
                                                      @RequestBody SignatureRequest requete) {
        SignatureAvenant signature = avenantService.signer(id, requete);
        return ResponseEntity.ok(Map.of("id", signature.getId()));
    }
}
