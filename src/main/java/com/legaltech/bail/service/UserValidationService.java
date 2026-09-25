package com.legaltech.bail.service;

import com.legaltech.bail.entity.Gender;
import com.legaltech.bail.exception.InvalidCinException;
import com.legaltech.bail.exception.InvalidPhoneException;
import com.legaltech.bail.exception.MinorUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserValidationService {

    private final SimulatedDateService simulatedDateService;

    // Pattern for Malagasy phone numbers: 032, 033, 034, 038 or +261 32/33/34/38 followed by 7 digits
    private static final Pattern MALAGASY_PHONE_PATTERN = Pattern.compile("^(\\+261|0)(32|33|34|38)\\d{7}$");

    // Pattern for Malagasy CIN: 12 digits
    private static final Pattern CIN_PATTERN = Pattern.compile("^\\d{12}$");

    public void validateUser(LocalDate dateNaissance, Gender genre, String cinNumero, String telephone) {
        validateAge(dateNaissance);
        validatePhone(telephone);
        validateCin(cinNumero, genre);
    }

    public void validateAge(LocalDate dateNaissance) {
        if (dateNaissance == null) {
            throw new MinorUserException("La date de naissance est obligatoire.");
        }

        LocalDate currentDate = simulatedDateService.getSimulatedDate();
        int age = Period.between(dateNaissance, currentDate).getYears();

        if (age < 18) {
            throw new MinorUserException("Seule une personne majeure (18 ans révolus) peut être enregistrée. Âge actuel calculé : " + age + " ans (Date système : " + currentDate + ").");
        }
    }

    public void validatePhone(String telephone) {
        if (telephone == null || telephone.isBlank()) {
            return; // Optionnel ou non renseigné
        }

        String cleanedPhone = telephone.replaceAll("\\s+", "").replaceAll("-", "");
        if (!MALAGASY_PHONE_PATTERN.matcher(cleanedPhone).matches()) {
            throw new InvalidPhoneException("Le numéro de téléphone '" + telephone + "' ne respecte pas les normes d'un opérateur malgache (Telma: 034/038, Orange: 032, Airtel: 033 sur 10 chiffres).");
        }
    }

    public void validateCin(String cinNumero, Gender genre) {
        if (cinNumero == null || cinNumero.isBlank()) {
            return; // Optionnel si non renseigné
        }

        String cleanedCin = cinNumero.replaceAll("\\s+", "").replaceAll("-", "");
        if (!CIN_PATTERN.matcher(cleanedCin).matches()) {
            throw new InvalidCinException("Le numéro de CIN malgache (" + cinNumero + ") doit comporter exactement 12 chiffres.");
        }

        if (genre != null) {
            // Position 6 (0-indexed position 5)
            char genderDigit = cleanedCin.charAt(5);
            if (genre == Gender.HOMME && genderDigit != '1') {
                throw new InvalidCinException("Erreur de conformité CIN/Genre : Pour un Homme, le 6ème chiffre du numéro CIN malgache doit être '1' (chiffre trouvé à la 6ème position : '" + genderDigit + "').");
            } else if (genre == Gender.FEMME && genderDigit != '2') {
                throw new InvalidCinException("Erreur de conformité CIN/Genre : Pour une Femme, le 6ème chiffre du numéro CIN malgache doit être '2' (chiffre trouvé à la 6ème position : '" + genderDigit + "').");
            }
        }
    }
}
