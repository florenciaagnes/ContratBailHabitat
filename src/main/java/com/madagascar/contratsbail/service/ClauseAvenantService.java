package com.madagascar.contratsbail.service;

import com.madagascar.contratsbail.entity.ModeleClauseAvenant;
import com.madagascar.contratsbail.entity.VariableClauseAvenant;
import com.madagascar.contratsbail.entity.enums.TypeVariable;
import com.madagascar.contratsbail.exception.RessourceIntrouvableException;
import com.madagascar.contratsbail.exception.ValidationMetierException;
import com.madagascar.contratsbail.repository.ModeleClauseAvenantRepository;
import com.madagascar.contratsbail.repository.VariableClauseAvenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Bibliotheque de clauses d'avenant : meme principe que ArticleService pour les articles de contrat. */
@Service
@RequiredArgsConstructor
@Transactional
public class ClauseAvenantService {

    private final ModeleClauseAvenantRepository modeleClauseAvenantRepository;
    private final VariableClauseAvenantRepository variableClauseAvenantRepository;

    @Transactional(readOnly = true)
    public List<ModeleClauseAvenant> listerActifs() {
        return modeleClauseAvenantRepository.findByActifTrueOrderByOrdreDefautAsc();
    }

    @Transactional(readOnly = true)
    public List<ModeleClauseAvenant> listerTous() {
        return modeleClauseAvenantRepository.findAllByOrderByOrdreDefautAsc();
    }

    @Transactional(readOnly = true)
    public List<ModeleClauseAvenant> rechercher(String terme) {
        if (terme == null || terme.isBlank()) {
            return listerActifs();
        }
        return modeleClauseAvenantRepository.findByActifTrueAndTitreContainingIgnoreCaseOrderByOrdreDefautAsc(terme.trim());
    }

    @Transactional(readOnly = true)
    public ModeleClauseAvenant obtenir(Long id) {
        return modeleClauseAvenantRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Modele de clause introuvable : id=" + id));
    }

    public ModeleClauseAvenant creer(ModeleClauseAvenant modele) {
        modele.setId(null);
        ModeleClauseAvenant enregistre = modeleClauseAvenantRepository.save(modele);
        if (modele.getVariables() != null) {
            for (VariableClauseAvenant v : modele.getVariables()) {
                validerVariable(v);
                v.setId(null);
                v.setModeleClause(enregistre);
                variableClauseAvenantRepository.save(v);
            }
        }
        return enregistre;
    }

    public ModeleClauseAvenant modifier(Long id, ModeleClauseAvenant donnees) {
        ModeleClauseAvenant existant = obtenir(id);
        existant.setTitre(donnees.getTitre());
        existant.setContenuModele(donnees.getContenuModele());
        existant.setOrdreDefaut(donnees.getOrdreDefaut());
        if (donnees.getActif() != null) {
            existant.setActif(donnees.getActif());
        }
        return modeleClauseAvenantRepository.save(existant);
    }

    public void desactiver(Long id) {
        ModeleClauseAvenant existant = obtenir(id);
        existant.setActif(false);
        modeleClauseAvenantRepository.save(existant);
    }

    public VariableClauseAvenant ajouterVariable(Long idModeleClause, VariableClauseAvenant variable) {
        ModeleClauseAvenant modele = obtenir(idModeleClause);
        validerVariable(variable);
        variable.setId(null);
        variable.setModeleClause(modele);
        return variableClauseAvenantRepository.save(variable);
    }

    public VariableClauseAvenant modifierVariable(Long idModeleClause, Long idVariable, VariableClauseAvenant donnees) {
        VariableClauseAvenant v = variableClauseAvenantRepository.findById(idVariable)
                .orElseThrow(() -> new RessourceIntrouvableException("Variable introuvable : id=" + idVariable));
        if (!v.getModeleClause().getId().equals(idModeleClause)) {
            throw new ValidationMetierException("Cette variable n'appartient pas à cette clause.");
        }
        v.setLibelle(donnees.getLibelle());
        v.setType(donnees.getType());
        v.setObligatoire(donnees.getObligatoire() == null ? Boolean.TRUE : donnees.getObligatoire());
        v.setOrdre(donnees.getOrdre() == null ? v.getOrdre() : donnees.getOrdre());
        v.setOptions(donnees.getOptions());
        validerVariable(v);
        return variableClauseAvenantRepository.save(v);
    }

    @Transactional(readOnly = true)
    public List<VariableClauseAvenant> listerVariables(Long idModeleClause) {
        return variableClauseAvenantRepository.findByModeleClauseIdOrderByOrdreAsc(idModeleClause);
    }

    private void validerVariable(VariableClauseAvenant v) {
        if (v.getType() == TypeVariable.LISTE && v.getOptionsListe().isEmpty()) {
            throw new ValidationMetierException("La variable '" + v.getNom()
                    + "' est de type LISTE : renseignez au moins une option.");
        }
    }
}
