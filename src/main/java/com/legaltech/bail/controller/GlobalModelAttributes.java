package com.legaltech.bail.controller;

import com.legaltech.bail.service.SimulatedDateService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final SimulatedDateService simulatedDateService;

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        LocalDate currentSimulated = simulatedDateService.getSimulatedDate();
        model.addAttribute("simulatedDate", currentSimulated);
    }
}
