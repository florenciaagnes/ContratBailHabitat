package com.madagascar.contratsbail.repository;

import com.madagascar.contratsbail.entity.Avenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvenantRepository extends JpaRepository<Avenant, Long> {
    List<Avenant> findByContratIdOrderByDateCreationAsc(Long contratId);
    long countByContratId(Long contratId);
}
