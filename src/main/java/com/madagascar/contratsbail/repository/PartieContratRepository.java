package com.madagascar.contratsbail.repository;

import com.madagascar.contratsbail.entity.PartieContrat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartieContratRepository extends JpaRepository<PartieContrat, Long> {
    List<PartieContrat> findByContratIdOrderByOrdreAsc(Long contratId);
}
