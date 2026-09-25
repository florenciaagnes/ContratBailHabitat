package com.legaltech.bail.repository;

import com.legaltech.bail.entity.Property;
import com.legaltech.bail.entity.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {
    List<Property> findByProprietaireId(Long proprietaireId);

    @Query("SELECT p FROM Property p WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(p.adresse) LIKE LOWER(CONCAT('%', :query, '%')) " +
           " OR LOWER(p.quartier) LIKE LOWER(CONCAT('%', :query, '%')) " +
           " OR LOWER(p.refCadastre) LIKE LOWER(CONCAT('%', :query, '%')) " +
           " OR LOWER(p.proprietaire.nom) LIKE LOWER(CONCAT('%', :query, '%')) " +
           " OR LOWER(p.proprietaire.prenom) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:typeBien IS NULL OR p.typeBien = :typeBien) " +
           "AND (:ville IS NULL OR :ville = '' OR LOWER(p.ville) LIKE LOWER(CONCAT('%', :ville, '%'))) " +
           "AND (:superficieMin IS NULL OR p.superficie >= :superficieMin) " +
           "AND (:superficieMax IS NULL OR p.superficie <= :superficieMax) " +
           "AND (:nombreChambresMin IS NULL OR p.nombreChambres >= :nombreChambresMin) " +
           "AND (:meuble IS NULL OR p.meuble = :meuble) " +
           "AND (:parking IS NULL OR p.parking = :parking) " +
           "AND (:compteurJirama IS NULL OR p.compteurJirama = :compteurJirama) " +
           "ORDER BY p.id DESC")
    List<Property> searchMultiCriteria(
            @Param("query") String query,
            @Param("typeBien") PropertyType typeBien,
            @Param("ville") String ville,
            @Param("superficieMin") Double superficieMin,
            @Param("superficieMax") Double superficieMax,
            @Param("nombreChambresMin") Integer nombreChambresMin,
            @Param("meuble") Boolean meuble,
            @Param("parking") Boolean parking,
            @Param("compteurJirama") Boolean compteurJirama);
}
