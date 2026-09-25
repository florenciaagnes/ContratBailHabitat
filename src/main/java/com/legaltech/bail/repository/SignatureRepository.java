package com.legaltech.bail.repository;

import com.legaltech.bail.entity.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long> {
    List<Signature> findByContractId(Long contractId);
    Optional<Signature> findByContractIdAndSignataireId(Long contractId, Long signataireId);
}
