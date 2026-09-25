package com.madagascar.contratsbail.repository;

import com.madagascar.contratsbail.entity.ContratArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContratArticleRepository extends JpaRepository<ContratArticle, Long> {
    List<ContratArticle> findByContratIdOrderByOrdreAsc(Long contratId);
}
