package com.legaltech.bail.controller;

import com.legaltech.bail.entity.*;
import com.legaltech.bail.exception.InvalidCinException;
import com.legaltech.bail.exception.InvalidPhoneException;
import com.legaltech.bail.exception.MinorUserException;
import com.legaltech.bail.repository.PropertyRepository;
import com.legaltech.bail.repository.UserRepository;
import com.legaltech.bail.service.UserValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final UserValidationService userValidationService;

    @GetMapping("/users")
    public String listUsersAndProperties(
            // Person filter parameters
            @RequestParam(value = "userQuery", required = false) String userQuery,
            @RequestParam(value = "userRole", required = false) Role userRole,
            @RequestParam(value = "userGenre", required = false) Gender userGenre,
            @RequestParam(value = "userMatrimoniale", required = false) MaritalStatus userMatrimoniale,
            // Property filter parameters
            @RequestParam(value = "propQuery", required = false) String propQuery,
            @RequestParam(value = "propType", required = false) PropertyType propType,
            @RequestParam(value = "propVille", required = false) String propVille,
            @RequestParam(value = "propSuperficieMin", required = false) Double propSuperficieMin,
            @RequestParam(value = "propSuperficieMax", required = false) Double propSuperficieMax,
            @RequestParam(value = "propChambresMin", required = false) Integer propChambresMin,
            @RequestParam(value = "propMeuble", required = false) Boolean propMeuble,
            @RequestParam(value = "propParking", required = false) Boolean propParking,
            @RequestParam(value = "propJirama", required = false) Boolean propJirama,
            Model model) {

        model.addAttribute("users", userRepository.searchMultiCriteria(userQuery, userRole, userGenre, userMatrimoniale));
        model.addAttribute("landlords", userRepository.findByRole(Role.LANDLORD));
        model.addAttribute("properties", propertyRepository.searchMultiCriteria(
                propQuery, propType, propVille, propSuperficieMin, propSuperficieMax, propChambresMin, propMeuble, propParking, propJirama));

        model.addAttribute("roles", Role.values());
        model.addAttribute("genders", Gender.values());
        model.addAttribute("maritalStatuses", MaritalStatus.values());
        model.addAttribute("propertyTypes", PropertyType.values());

        // Echo search params back to view
        model.addAttribute("userQuery", userQuery);
        model.addAttribute("userRole", userRole);
        model.addAttribute("userGenre", userGenre);
        model.addAttribute("userMatrimoniale", userMatrimoniale);

        model.addAttribute("propQuery", propQuery);
        model.addAttribute("propType", propType);
        model.addAttribute("propVille", propVille);
        model.addAttribute("propSuperficieMin", propSuperficieMin);
        model.addAttribute("propSuperficieMax", propSuperficieMax);
        model.addAttribute("propChambresMin", propChambresMin);
        model.addAttribute("propMeuble", propMeuble);
        model.addAttribute("propParking", propParking);
        model.addAttribute("propJirama", propJirama);

        return "users/list";
    }

    @PostMapping("/users/create")
    public String createUser(
            @RequestParam("nom") String nom,
            @RequestParam("prenom") String prenom,
            @RequestParam("email") String email,
            @RequestParam(value = "telephone", required = false) String telephone,
            @RequestParam(value = "cinNumero", required = false) String cinNumero,
            @RequestParam("role") Role role,
            @RequestParam("dateNaissance") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateNaissance,
            @RequestParam(value = "genre", required = false) Gender genre,
            @RequestParam(value = "situationMatrimoniale", required = false) MaritalStatus situationMatrimoniale,
            RedirectAttributes redirectAttributes) {

        try {
            // Validation (Age >= 18, Phone operator check, CIN 12 digits + 6th digit gender match)
            userValidationService.validateUser(dateNaissance, genre, cinNumero, telephone);

            User user = User.builder()
                    .nom(nom.trim())
                    .prenom(prenom.trim())
                    .email(email.trim().toLowerCase())
                    .telephone(telephone != null ? telephone.trim() : "")
                    .cinNumero(cinNumero != null ? cinNumero.trim() : "")
                    .passwordHash("hash_pass_123")
                    .role(role)
                    .dateNaissance(dateNaissance)
                    .genre(genre)
                    .situationMatrimoniale(situationMatrimoniale)
                    .build();

            userRepository.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "Nouvelle personne (" + user.getNomComplet() + ") enregistrée avec succès.");
        } catch (MinorUserException | InvalidCinException | InvalidPhoneException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Exception de Validation : " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de l'enregistrement de la personne : " + e.getMessage());
        }

        return "redirect:/users";
    }

    @PostMapping("/users/update")
    public String updateUser(
            @RequestParam("id") Long id,
            @RequestParam("nom") String nom,
            @RequestParam("prenom") String prenom,
            @RequestParam("email") String email,
            @RequestParam(value = "telephone", required = false) String telephone,
            @RequestParam(value = "cinNumero", required = false) String cinNumero,
            @RequestParam("role") Role role,
            @RequestParam("dateNaissance") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateNaissance,
            @RequestParam(value = "genre", required = false) Gender genre,
            @RequestParam(value = "situationMatrimoniale", required = false) MaritalStatus situationMatrimoniale,
            RedirectAttributes redirectAttributes) {

        try {
            // Validation
            userValidationService.validateUser(dateNaissance, genre, cinNumero, telephone);

            User user = userRepository.findById(id).orElseThrow();
            user.setNom(nom.trim());
            user.setPrenom(prenom.trim());
            user.setEmail(email.trim().toLowerCase());
            user.setTelephone(telephone != null ? telephone.trim() : "");
            user.setCinNumero(cinNumero != null ? cinNumero.trim() : "");
            user.setRole(role);
            user.setDateNaissance(dateNaissance);
            user.setGenre(genre);
            user.setSituationMatrimoniale(situationMatrimoniale);

            userRepository.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "Personne (" + user.getNomComplet() + ") mise à jour avec succès.");
        } catch (MinorUserException | InvalidCinException | InvalidPhoneException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Exception de Validation : " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la mise à jour de la personne : " + e.getMessage());
        }

        return "redirect:/users";
    }

    @PostMapping("/properties/create")
    public String createProperty(
            @RequestParam("proprietaireId") Long proprietaireId,
            @RequestParam("adresse") String adresse,
            @RequestParam(value = "quartier", required = false) String quartier,
            @RequestParam("ville") String ville,
            @RequestParam(value = "refCadastre", required = false) String refCadastre,
            @RequestParam(value = "typeBien", defaultValue = "APPARTEMENT") PropertyType typeBien,
            @RequestParam(value = "superficie", required = false) Double superficie,
            @RequestParam(value = "nombrePieces", defaultValue = "1") Integer nombrePieces,
            @RequestParam(value = "nombreChambres", defaultValue = "1") Integer nombreChambres,
            @RequestParam(value = "etage", defaultValue = "0") Integer etage,
            @RequestParam(value = "meuble", defaultValue = "false") Boolean meuble,
            @RequestParam(value = "parking", defaultValue = "false") Boolean parking,
            @RequestParam(value = "compteurJirama", defaultValue = "true") Boolean compteurJirama,
            RedirectAttributes redirectAttributes) {

        try {
            User proprietaire = userRepository.findById(proprietaireId).orElseThrow();
            Property prop = Property.builder()
                    .proprietaire(proprietaire)
                    .adresse(adresse.trim())
                    .quartier(quartier != null ? quartier.trim() : "")
                    .ville(ville.trim())
                    .refCadastre(refCadastre != null ? refCadastre.trim() : "")
                    .typeBien(typeBien)
                    .superficie(superficie)
                    .nombrePieces(nombrePieces)
                    .nombreChambres(nombreChambres)
                    .etage(etage)
                    .meuble(meuble)
                    .parking(parking)
                    .compteurJirama(compteurJirama)
                    .build();

            propertyRepository.save(prop);
            redirectAttributes.addFlashAttribute("successMessage", "Nouveau bien immobilier (" + typeBien.getLabel() + ") enregistré avec succès pour " + proprietaire.getNomComplet() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de l'enregistrement du bien : " + e.getMessage());
        }

        return "redirect:/users";
    }

    @PostMapping("/properties/update")
    public String updateProperty(
            @RequestParam("id") Long id,
            @RequestParam("proprietaireId") Long proprietaireId,
            @RequestParam("adresse") String adresse,
            @RequestParam(value = "quartier", required = false) String quartier,
            @RequestParam("ville") String ville,
            @RequestParam(value = "refCadastre", required = false) String refCadastre,
            @RequestParam(value = "typeBien", defaultValue = "APPARTEMENT") PropertyType typeBien,
            @RequestParam(value = "superficie", required = false) Double superficie,
            @RequestParam(value = "nombrePieces", defaultValue = "1") Integer nombrePieces,
            @RequestParam(value = "nombreChambres", defaultValue = "1") Integer nombreChambres,
            @RequestParam(value = "etage", defaultValue = "0") Integer etage,
            @RequestParam(value = "meuble", defaultValue = "false") Boolean meuble,
            @RequestParam(value = "parking", defaultValue = "false") Boolean parking,
            @RequestParam(value = "compteurJirama", defaultValue = "true") Boolean compteurJirama,
            RedirectAttributes redirectAttributes) {

        try {
            Property prop = propertyRepository.findById(id).orElseThrow();
            User proprietaire = userRepository.findById(proprietaireId).orElseThrow();

            prop.setProprietaire(proprietaire);
            prop.setAdresse(adresse.trim());
            prop.setQuartier(quartier != null ? quartier.trim() : "");
            prop.setVille(ville.trim());
            prop.setRefCadastre(refCadastre != null ? refCadastre.trim() : "");
            prop.setTypeBien(typeBien);
            prop.setSuperficie(superficie);
            prop.setNombrePieces(nombrePieces);
            prop.setNombreChambres(nombreChambres);
            prop.setEtage(etage);
            prop.setMeuble(meuble);
            prop.setParking(parking);
            prop.setCompteurJirama(compteurJirama);

            propertyRepository.save(prop);
            redirectAttributes.addFlashAttribute("successMessage", "Bien immobilier (" + typeBien.getLabel() + ") mis à jour avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la mise à jour du bien : " + e.getMessage());
        }

        return "redirect:/users";
    }
}
