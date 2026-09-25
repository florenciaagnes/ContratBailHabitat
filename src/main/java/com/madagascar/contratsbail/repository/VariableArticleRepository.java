package com.madagascar.contratsbail.repository;

import com.madagascar.contratsbail.entity.VariableArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariableArticleRepository extends JpaRepository<VariableArticle, Long> {
    List<VariableArticle> findByModeleArticleIdOrderByOrdreAsc(Long modeleArticleId);
}
