package com.legaltech.bail.repository;

import com.legaltech.bail.entity.LegalException;
import com.legaltech.bail.entity.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LegalExceptionRepository extends JpaRepository<LegalException, Long> {
    List<LegalException> findByContractId(Long contractId);
    List<LegalException> findByContractIdAndSeverity(Long contractId, Severity severity);
    List<LegalException> findByContractIdAndResoluFalse(Long contractId);
    void deleteByContractId(Long contractId);
}
