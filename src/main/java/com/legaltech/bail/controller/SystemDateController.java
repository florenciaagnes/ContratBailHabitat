package com.legaltech.bail.controller;

import com.legaltech.bail.service.ContractService;
import com.legaltech.bail.service.SimulatedDateService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemDateController {

    private final SimulatedDateService simulatedDateService;
    private final ContractService contractService;

    @PostMapping("/date")
    public String updateSimulatedDate(
            @RequestParam("simulatedDate") String dateStr,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            LocalDate newDate = LocalDate.parse(dateStr);
            simulatedDateService.setSimulatedDate(newDate);
            contractService.evaluateExpirations(newDate);
            redirectAttributes.addFlashAttribute("infoMessage", "Date système mise à jour : " + newDate + ". Vérification des échéances effectuée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Format de date invalide : " + e.getMessage());
        }

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/contracts");
    }
}
