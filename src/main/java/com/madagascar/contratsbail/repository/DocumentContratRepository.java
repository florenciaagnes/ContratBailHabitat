package com.madagascar.contratsbail.repository;

import com.madagascar.contratsbail.entity.DocumentContrat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentContratRepository extends JpaRepository<DocumentContrat, Long> {
    List<DocumentContrat> findByContratIdOrderByDateGenerationDesc(Long contratId);
}
