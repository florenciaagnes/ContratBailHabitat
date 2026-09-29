package com.madagascar.contratsbail.repository;

import com.madagascar.contratsbail.entity.ModeleClauseAvenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModeleClauseAvenantRepository extends JpaRepository<ModeleClauseAvenant, Long> {
    List<ModeleClauseAvenant> findByActifTrueOrderByOrdreDefautAsc();
    List<ModeleClauseAvenant> findAllByOrderByOrdreDefautAsc();
    Optional<ModeleClauseAvenant> findByCode(String code);
    List<ModeleClauseAvenant> findByActifTrueAndTitreContainingIgnoreCaseOrderByOrdreDefautAsc(String titre);
}
