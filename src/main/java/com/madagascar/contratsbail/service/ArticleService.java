package com.madagascar.contratsbail.service;

import com.madagascar.contratsbail.entity.ModeleArticle;
import com.madagascar.contratsbail.entity.VariableArticle;
import com.madagascar.contratsbail.exception.RessourceIntrouvableException;
import com.madagascar.contratsbail.repository.ModeleArticleRepository;
import com.madagascar.contratsbail.repository.VariableArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleService {

    private final ModeleArticleRepository modeleArticleRepository;
    private final VariableArticleRepository variableArticleRepository;

    @Transactional(readOnly = true)
    public List<ModeleArticle> listerActifs() {
        return modeleArticleRepository.findByActifTrueOrderByOrdreDefautAsc();
    }

    @Transactional(readOnly = true)
    public List<ModeleArticle> listerTous() {
        return modeleArticleRepository.findAllByOrderByOrdreDefautAsc();
    }

    @Transactional(readOnly = true)
    public List<ModeleArticle> rechercher(String terme) {
        if (terme == null || terme.isBlank()) {
            return listerActifs();
        }
        return modeleArticleRepository.findByActifTrueAndTitreContainingIgnoreCaseOrderByOrdreDefautAsc(terme.trim());
    }

    @Transactional(readOnly = true)
    public ModeleArticle obtenir(Long id) {
        return modeleArticleRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Modele d'article introuvable : id=" + id));
    }

    public ModeleArticle creer(ModeleArticle modeleArticle) {
        modeleArticle.setId(null);
        ModeleArticle enregistre = modeleArticleRepository.save(modeleArticle);
        if (modeleArticle.getVariables() != null) {
            for (VariableArticle v : modeleArticle.getVariables()) {
                v.setId(null);
                v.setModeleArticle(enregistre);
                variableArticleRepository.save(v);
            }
        }
        return enregistre;
    }

    public ModeleArticle modifier(Long id, ModeleArticle donnees) {
        ModeleArticle existant = obtenir(id);
        existant.setTitre(donnees.getTitre());
        existant.setContenuModele(donnees.getContenuModele());
        existant.setOrdreDefaut(donnees.getOrdreDefaut());
        existant.setObligatoire(donnees.getObligatoire());
        if (donnees.getActif() != null) {
            existant.setActif(donnees.getActif());
        }
        return modeleArticleRepository.save(existant);
    }

    public void desactiver(Long id) {
        ModeleArticle existant = obtenir(id);
        existant.setActif(false);
        modeleArticleRepository.save(existant);
    }

    public VariableArticle ajouterVariable(Long idModeleArticle, VariableArticle variable) {
        ModeleArticle modele = obtenir(idModeleArticle);
        variable.setId(null);
        variable.setModeleArticle(modele);
        return variableArticleRepository.save(variable);
    }

    @Transactional(readOnly = true)
    public List<VariableArticle> listerVariables(Long idModeleArticle) {
        return variableArticleRepository.findByModeleArticleIdOrderByOrdreAsc(idModeleArticle);
    }
}
