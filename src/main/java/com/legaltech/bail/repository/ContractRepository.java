package com.legaltech.bail.repository;

import com.legaltech.bail.entity.Contract;
import com.legaltech.bail.entity.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findByReference(String reference);

    Page<Contract> findByStatut(ContractStatus statut, Pageable pageable);

    List<Contract> findByStatutAndTaciteReconductionTrue(ContractStatus statut);

    List<Contract> findByDateFinBetweenAndStatut(LocalDate from, LocalDate to, ContractStatus statut);

    /**
     * Recherche plein texte native via websearch_to_tsquery sur search_vector
     */
    @Query(value = "SELECT * FROM contracts c WHERE c.search_vector @@ websearch_to_tsquery('french', :query)",
           countQuery = "SELECT count(*) FROM contracts c WHERE c.search_vector @@ websearch_to_tsquery('french', :query)",
           nativeQuery = true)
    Page<Contract> fullTextSearchNative(@Param("query") String query, Pageable pageable);

    /**
     * Recherche flexible par mot-clé (fallback insensible à la casse sur référence, ville, adresse ou noms des parties)
     */
    @Query("SELECT c FROM Contract c WHERE " +
           "LOWER(c.reference) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.bien.ville) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.bien.adresse) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.bailleur.nom) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.locataire.nom) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Contract> searchByKeyword(@Param("query") String query, Pageable pageable);

    @Query("SELECT DISTINCT c.bien.ville FROM Contract c WHERE c.bien.ville IS NOT NULL AND c.bien.ville != '' ORDER BY c.bien.ville ASC")
    List<String> findDistinctVilles();

    @Query("SELECT c FROM Contract c WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(c.reference) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(c.bien.ville) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(c.bien.adresse) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(c.bailleur.nom) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(c.locataire.nom) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:statut IS NULL OR c.statut = :statut) AND " +
           "(:ville IS NULL OR :ville = '' OR LOWER(c.bien.ville) LIKE LOWER(CONCAT('%', :ville, '%'))) AND " +
           "(:typeBien IS NULL OR c.bien.typeBien = :typeBien) AND " +
           "(:superficieMin IS NULL OR c.bien.superficie >= :superficieMin) AND " +
           "(:superficieMax IS NULL OR c.bien.superficie <= :superficieMax) AND " +
           "(:nombreChambresMin IS NULL OR c.bien.nombreChambres >= :nombreChambresMin) AND " +
           "(:meuble IS NULL OR c.bien.meuble = :meuble) AND " +
           "(:bailleurId IS NULL OR c.bailleur.id = :bailleurId) AND " +
           "(:locataireId IS NULL OR c.locataire.id = :locataireId) AND " +
           "(:loyerMin IS NULL OR c.loyer >= :loyerMin) AND " +
           "(:loyerMax IS NULL OR c.loyer <= :loyerMax) AND " +
           "(:taciteReconduction IS NULL OR c.taciteReconduction = :taciteReconduction)")
    Page<Contract> searchMultiCriteria(
            @Param("query") String query,
            @Param("statut") ContractStatus statut,
            @Param("ville") String ville,
            @Param("typeBien") com.legaltech.bail.entity.PropertyType typeBien,
            @Param("superficieMin") Double superficieMin,
            @Param("superficieMax") Double superficieMax,
            @Param("nombreChambresMin") Integer nombreChambresMin,
            @Param("meuble") Boolean meuble,
            @Param("bailleurId") Long bailleurId,
            @Param("locataireId") Long locataireId,
            @Param("loyerMin") java.math.BigDecimal loyerMin,
            @Param("loyerMax") java.math.BigDecimal loyerMax,
            @Param("taciteReconduction") Boolean taciteReconduction,
            Pageable pageable);
}
