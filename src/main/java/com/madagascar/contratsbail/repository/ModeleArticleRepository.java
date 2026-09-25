package com.madagascar.contratsbail.repository;

import com.madagascar.contratsbail.entity.ModeleArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModeleArticleRepository extends JpaRepository<ModeleArticle, Long> {
    List<ModeleArticle> findByActifTrueOrderByOrdreDefautAsc();
    List<ModeleArticle> findAllByOrderByOrdreDefautAsc();
    Optional<ModeleArticle> findByCode(String code);
    List<ModeleArticle> findByActifTrueAndTitreContainingIgnoreCaseOrderByOrdreDefautAsc(String titre);
}
