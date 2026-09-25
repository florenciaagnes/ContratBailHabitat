package com.madagascar.contratsbail.repository;

import com.madagascar.contratsbail.entity.ContratVariable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContratVariableRepository extends JpaRepository<ContratVariable, Long> {
    List<ContratVariable> findByContratArticleId(Long contratArticleId);
}
