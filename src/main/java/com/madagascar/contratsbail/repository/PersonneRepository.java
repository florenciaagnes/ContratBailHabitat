package com.madagascar.contratsbail.repository;

import com.madagascar.contratsbail.entity.Personne;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonneRepository extends JpaRepository<Personne, Long> {
}
