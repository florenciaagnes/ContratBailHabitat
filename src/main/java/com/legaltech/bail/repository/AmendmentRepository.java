package com.legaltech.bail.repository;

import com.legaltech.bail.entity.Amendment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmendmentRepository extends JpaRepository<Amendment, Long> {
    List<Amendment> findByParentContractId(Long parentContractId);
    Optional<Amendment> findByNewContractId(Long newContractId);
}
