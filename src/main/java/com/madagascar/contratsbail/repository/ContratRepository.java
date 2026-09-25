package com.madagascar.contratsbail.repository;

import com.madagascar.contratsbail.entity.Contrat;
import com.madagascar.contratsbail.entity.enums.StatutContrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat, Long>, JpaSpecificationExecutor<Contrat> {

    Optional<Contrat> findByNumero(String numero);

    List<Contrat> findByStatutOrderByDateCreationDesc(StatutContrat statut);

    List<Contrat> findAllByOrderByDateCreationDesc();

    // La recherche (terme / statut / dates) est desormais implementee via
    // ContratSpecifications + JpaSpecificationExecutor : voir ContratService.rechercher().
    // Cela evite de lier des parametres JDBC a null dans un motif ":param IS NULL",
    // motif que PostgreSQL ne sait pas toujours typer (erreur "parametre $N").

    List<Contrat> findByStatutAndDateFinBefore(StatutContrat statut, LocalDate date);

    @Query(value = "select nextval('contrat_numero_seq')", nativeQuery = true)
    Long prochainNumeroSequence();
}

