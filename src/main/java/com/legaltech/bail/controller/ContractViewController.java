package com.legaltech.bail.controller;

import com.legaltech.bail.entity.*;
import com.legaltech.bail.repository.PropertyRepository;
import com.legaltech.bail.repository.UserRepository;
import com.legaltech.bail.service.AmendmentService;
import com.legaltech.bail.service.ContractService;
import com.legaltech.bail.service.SignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class ContractViewController {

    private final ContractService contractService;
    private final SignatureService signatureService;
    private final AmendmentService amendmentService;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final com.legaltech.bail.repository.ContractRepository contractRepository;

    @GetMapping
    public String listContracts(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "statut", required = false) ContractStatus statut,
            @RequestParam(name = "ville", required = false) String ville,
            @RequestParam(name = "typeBien", required = false) com.legaltech.bail.entity.PropertyType typeBien,
            @RequestParam(name = "superficieMin", required = false) Double superficieMin,
            @RequestParam(name = "superficieMax", required = false) Double superficieMax,
            @RequestParam(name = "nombreChambresMin", required = false) Integer nombreChambresMin,
            @RequestParam(name = "meuble", required = false) Boolean meuble,
            @RequestParam(name = "bailleurId", required = false) Long bailleurId,
            @RequestParam(name = "locataireId", required = false) Long locataireId,
            @RequestParam(name = "loyerMin", required = false) BigDecimal loyerMin,
            @RequestParam(name = "loyerMax", required = false) BigDecimal loyerMax,
            @RequestParam(name = "taciteReconduction", required = false) Boolean taciteReconduction,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        Page<Contract> contractPage = contractService.searchContracts(
                query, statut, ville, typeBien, superficieMin, superficieMax, nombreChambresMin, meuble,
                bailleurId, locataireId, loyerMin, loyerMax, taciteReconduction,
                PageRequest.of(page, size, Sort.by("id").descending()));

        model.addAttribute("contracts", contractPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", contractPage.getTotalPages());
        model.addAttribute("totalItems", contractPage.getTotalElements());

        // Search parameters for sticky inputs in view
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("selectedStatut", statut);
        model.addAttribute("selectedVille", ville != null ? ville : "");
        model.addAttribute("selectedTypeBien", typeBien);
        model.addAttribute("superficieMin", superficieMin);
        model.addAttribute("superficieMax", superficieMax);
        model.addAttribute("nombreChambresMin", nombreChambresMin);
        model.addAttribute("selectedMeuble", meuble);
        model.addAttribute("selectedBailleurId", bailleurId);
        model.addAttribute("selectedLocataireId", locataireId);
        model.addAttribute("loyerMin", loyerMin);
        model.addAttribute("loyerMax", loyerMax);
        model.addAttribute("selectedTacite", taciteReconduction);

        // Filter dropdown options
        model.addAttribute("allStatuts", ContractStatus.values());
        model.addAttribute("allLandlords", userRepository.findByRole(Role.LANDLORD));
        model.addAttribute("allTenants", userRepository.findByRole(Role.TENANT));
        model.addAttribute("villes", contractRepository.findDistinctVilles());
        model.addAttribute("propertyTypes", com.legaltech.bail.entity.PropertyType.values());

        return "contracts/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("landlords", userRepository.findByRole(Role.LANDLORD));
        model.addAttribute("tenants", userRepository.findByRole(Role.TENANT));
        model.addAttribute("allUsers", userRepository.findAll());
        model.addAttribute("properties", propertyRepository.findAll());
        return "contracts/create";
    }

    @PostMapping("/create")
    public String processCreateContract(
            @RequestParam("bailleurId") Long bailleurId,
            @RequestParam("locataireId") Long locataireId,
            @RequestParam("bienId") Long bienId,
            @RequestParam("loyer") BigDecimal loyer,
            @RequestParam(value = "charges", defaultValue = "0") BigDecimal charges,
            @RequestParam("depot") BigDecimal depot,
            @RequestParam("dateDebut") String dateDebut,
            @RequestParam("dateFin") String dateFin,
            @RequestParam(value = "taciteReconduction", defaultValue = "false") Boolean taciteReconduction,
            @RequestParam(value = "clauseTitres", required = false) List<String> clauseTitres,
            @RequestParam(value = "clauseTextes", required = false) List<String> clauseTextes,
            RedirectAttributes redirectAttributes) {

        User bailleur = userRepository.findById(bailleurId).orElseThrow();
        User locataire = userRepository.findById(locataireId).orElseThrow();
        Property bien = propertyRepository.findById(bienId).orElseThrow();

        Contract contract = Contract.builder()
                .bailleur(bailleur)
                .locataire(locataire)
                .bien(bien)
                .loyer(loyer)
                .charges(charges)
                .depot(depot)
                .dateDebut(LocalDate.parse(dateDebut))
                .dateFin(LocalDate.parse(dateFin))
                .taciteReconduction(taciteReconduction)
                .build();

        List<ContractClause> clauses = new ArrayList<>();
        if (clauseTitres != null && clauseTextes != null) {
            for (int i = 0; i < Math.min(clauseTitres.size(), clauseTextes.size()); i++) {
                if (!clauseTitres.get(i).isBlank() && !clauseTextes.get(i).isBlank()) {
                    clauses.add(ContractClause.builder()
                            .ordre(i + 1)
                            .titre(clauseTitres.get(i))
                            .texte(clauseTextes.get(i))
                            .build());
                }
            }
        }

        Contract created = contractService.createContract(contract, clauses);
        redirectAttributes.addFlashAttribute("successMessage", "Contrat de bail créé et soumis à l'audit juridique avec succès.");
        return "redirect:/contracts/" + created.getId() + "/audit";
    }

    @GetMapping("/{id}")
    public String showContractDetail(@PathVariable("id") Long id, Model model) {
        Contract contract = contractService.getContractById(id);
        model.addAttribute("contract", contract);
        boolean isSigned = (contract.getStatut() == ContractStatus.SIGNED || contract.getStatut() == ContractStatus.AMENDED);
        model.addAttribute("isSigned", isSigned);
        model.addAttribute("history", amendmentService.getHistoryByContractId(id));
        return "contracts/detail";
    }

    @PostMapping("/{id}/edit")
    public String processEditContract(
            @PathVariable("id") Long id,
            @RequestParam("loyer") BigDecimal loyer,
            @RequestParam(value = "charges", defaultValue = "0") BigDecimal charges,
            @RequestParam("depot") BigDecimal depot,
            @RequestParam("dateDebut") String dateDebut,
            @RequestParam("dateFin") String dateFin,
            @RequestParam(value = "taciteReconduction", defaultValue = "false") Boolean taciteReconduction,
            @RequestParam(value = "clauseTitres", required = false) List<String> clauseTitres,
            @RequestParam(value = "clauseTextes", required = false) List<String> clauseTextes,
            RedirectAttributes redirectAttributes) {

        Contract contract = contractService.getContractById(id);
        if (contract.getStatut() == ContractStatus.SIGNED || contract.getStatut() == ContractStatus.AMENDED) {
            redirectAttributes.addFlashAttribute("errorMessage", "Impossible de modifier ce contrat : Il est signé et légalement immuable. Créez un avenant modificatif.");
            return "redirect:/contracts/" + id;
        }

        List<ContractClause> clauses = new ArrayList<>();
        if (clauseTitres != null && clauseTextes != null) {
            for (int i = 0; i < Math.min(clauseTitres.size(), clauseTextes.size()); i++) {
                if (!clauseTitres.get(i).isBlank() && !clauseTextes.get(i).isBlank()) {
                    clauses.add(ContractClause.builder()
                            .ordre(i + 1)
                            .titre(clauseTitres.get(i))
                            .texte(clauseTextes.get(i))
                            .build());
                }
            }
        }

        try {
            contractService.updateContractDetails(id, loyer, charges, depot, LocalDate.parse(dateDebut), LocalDate.parse(dateFin), taciteReconduction, clauses);
            redirectAttributes.addFlashAttribute("successMessage", "Contrat mis à jour et ré-audité avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la modification : " + e.getMessage());
        }

        return "redirect:/contracts/" + id;
    }

    @GetMapping("/{id}/audit")
    public String showAuditResult(@PathVariable("id") Long id, Model model) {
        Contract contract = contractService.getContractById(id);
        model.addAttribute("contract", contract);
        return "contracts/audit-result";
    }

    @PostMapping("/{id}/re-audit")
    public String reAuditContract(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        contractService.reAuditContract(id);
        redirectAttributes.addFlashAttribute("successMessage", "L'audit juridique a été relancé avec succès.");
        return "redirect:/contracts/" + id + "/audit";
    }

    @PostMapping("/{id}/update-clause")
    public String updateClause(
            @PathVariable("id") Long id,
            @RequestParam("clauseId") Long clauseId,
            @RequestParam("titre") String titre,
            @RequestParam("texte") String texte,
            RedirectAttributes redirectAttributes) {

        contractService.updateClause(id, clauseId, titre, texte);
        redirectAttributes.addFlashAttribute("successMessage", "La clause #" + clauseId + " a été mise à jour. Nouvel audit effectué.");
        return "redirect:/contracts/" + id + "/audit";
    }

    @GetMapping("/{id}/sign")
    public String showSignPage(@PathVariable("id") Long id, Model model) {
        Contract contract = contractService.getContractById(id);
        model.addAttribute("contract", contract);
        model.addAttribute("signers", List.of(contract.getBailleur(), contract.getLocataire()));
        return "contracts/sign";
    }

    @PostMapping("/{id}/send-otp")
    public String sendOtp(
            @PathVariable("id") Long id,
            @RequestParam("userId") Long userId,
            RedirectAttributes redirectAttributes) {

        String otp = signatureService.generateOtp(id, userId);
        redirectAttributes.addFlashAttribute("infoMessage", "Code OTP généré pour la signature : " + otp + " (Pour démo, '123456' est aussi accepté).");
        return "redirect:/contracts/" + id + "/sign";
    }

    @PostMapping("/{id}/sign")
    public String processSignature(
            @PathVariable("id") Long id,
            @RequestParam("userId") Long userId,
            @RequestParam("otp") String otp,
            RedirectAttributes redirectAttributes) {

        try {
            signatureService.signContract(id, userId, otp, "127.0.0.1");
            redirectAttributes.addFlashAttribute("successMessage", "Signature numérique scellée avec succès avec empreinte SHA-256.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Échec de la signature : " + e.getMessage());
        }
        return "redirect:/contracts/" + id + "/sign";
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadSealedPdf(@PathVariable("id") Long id) {
        Contract contract = contractService.getContractById(id);
        byte[] pdfBytes = signatureService.generateSealedPdf(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + contract.getReference() + "_scelle.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/{id}/amend")
    public String createAmendment(
            @PathVariable("id") Long id,
            @RequestParam("motif") String motif,
            @RequestParam("loyer") BigDecimal loyer,
            RedirectAttributes redirectAttributes) {

        Contract newContractData = Contract.builder()
                .loyer(loyer)
                .build();

        try {
            Amendment amendment = amendmentService.createAmendment(id, motif, newContractData, null);
            redirectAttributes.addFlashAttribute("successMessage", "Avenant créé avec succès (Nouvelle réf: " + amendment.getNewContract().getReference() + ").");
            return "redirect:/contracts/" + amendment.getNewContract().getId() + "/audit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la création de l'avenant : " + e.getMessage());
            return "redirect:/contracts/" + id + "/sign";
        }
    }
}
