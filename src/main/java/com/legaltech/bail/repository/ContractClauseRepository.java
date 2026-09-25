package com.legaltech.bail.repository;

import com.legaltech.bail.entity.ContractClause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractClauseRepository extends JpaRepository<ContractClause, Long> {
    List<ContractClause> findByContractIdOrderByOrdreAsc(Long contractId);
    void deleteByContractId(Long contractId);
}
