package com.madagascar.contratsbail.repository;

import com.madagascar.contratsbail.entity.RepresentantOrganisation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepresentantOrganisationRepository extends JpaRepository<RepresentantOrganisation, Long> {
    List<RepresentantOrganisation> findByOrganisationIdOrderByOrdreAsc(Long organisationId);
}
