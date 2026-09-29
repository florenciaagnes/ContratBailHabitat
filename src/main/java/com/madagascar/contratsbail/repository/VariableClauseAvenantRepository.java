package com.madagascar.contratsbail.repository;

import com.madagascar.contratsbail.entity.VariableClauseAvenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariableClauseAvenantRepository extends JpaRepository<VariableClauseAvenant, Long> {
    List<VariableClauseAvenant> findByModeleClauseIdOrderByOrdreAsc(Long idModeleClause);
}
