package com.madagascar.contratsbail.repository;

import com.madagascar.contratsbail.entity.Signature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignatureRepository extends JpaRepository<Signature, Long> {
    List<Signature> findByContratIdOrderByOrdreAsc(Long contratId);
}
