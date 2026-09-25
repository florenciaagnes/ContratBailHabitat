package com.legaltech.bail.service;

import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class SimulatedDateService {

    private LocalDate simulatedDate = LocalDate.now();

    public LocalDate getSimulatedDate() {
        return simulatedDate;
    }

    public void setSimulatedDate(LocalDate date) {
        if (date != null) {
            this.simulatedDate = date;
        }
    }

    public void resetDate() {
        this.simulatedDate = LocalDate.now();
    }
}
