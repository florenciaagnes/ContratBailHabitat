package com.legaltech.bail.repository;

import com.legaltech.bail.entity.Gender;
import com.legaltech.bail.entity.MaritalStatus;
import com.legaltech.bail.entity.Role;
import com.legaltech.bail.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);

    @Query("SELECT u FROM User u WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :query, '%')) " +
           " OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :query, '%')) " +
           " OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
           " OR LOWER(u.cinNumero) LIKE LOWER(CONCAT('%', :query, '%')) " +
           " OR LOWER(u.telephone) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:role IS NULL OR u.role = :role) " +
           "AND (:genre IS NULL OR u.genre = :genre) " +
           "AND (:situationMatrimoniale IS NULL OR u.situationMatrimoniale = :situationMatrimoniale) " +
           "ORDER BY u.nom ASC, u.prenom ASC")
    List<User> searchMultiCriteria(
            @Param("query") String query,
            @Param("role") Role role,
            @Param("genre") Gender genre,
            @Param("situationMatrimoniale") MaritalStatus situationMatrimoniale);
}
