package com.madagascar.contratsbail.repository;

import com.madagascar.contratsbail.entity.*;
import com.madagascar.contratsbail.entity.enums.StatutContrat;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Construit dynamiquement les criteres de recherche des contrats.
 * Chaque predicat n'est ajoute a la requete que si le critere correspondant
 * est reellement fourni : aucun parametre "null" n'est jamais lie a la
 * requete SQL, ce qui evite les erreurs PostgreSQL de type
 * "n'a pas pu determiner le type de donnees du parametre $N".
 */
public final class ContratSpecifications {

    private ContratSpecifications() {
    }

    public static Specification<Contrat> avecTerme(String terme) {
        return (root, query, cb) -> {
            if (terme == null || terme.isBlank()) {
                return cb.conjunction();
            }
            query.distinct(true);
            String motif = "%" + terme.trim().toLowerCase() + "%";

            Join<Contrat, PartieContrat> parties = root.join("parties", JoinType.LEFT);
            Join<PartieContrat, Personne> personne = parties.join("personne", JoinType.LEFT);
            Join<PartieContrat, Organisation> organisation = parties.join("organisation", JoinType.LEFT);
            Join<Contrat, Bien> bien = root.join("bien", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("numero")), motif),
                    cb.like(cb.lower(cb.coalesce(personne.get("nom"), "")), motif),
                    cb.like(cb.lower(cb.coalesce(personne.get("prenom"), "")), motif),
                    cb.like(cb.lower(cb.coalesce(personne.get("cin"), "")), motif),
                    cb.like(cb.lower(cb.coalesce(organisation.get("nom"), "")), motif),
                    cb.like(cb.lower(cb.coalesce(bien.get("adresse"), "")), motif)
            );
        };
    }

    public static Specification<Contrat> avecStatut(StatutContrat statut) {
        return (root, query, cb) -> statut == null
                ? cb.conjunction()
                : cb.equal(root.get("statut"), statut);
    }

    public static Specification<Contrat> dateCreationApres(LocalDateTime debut) {
        return (root, query, cb) -> debut == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("dateCreation"), debut);
    }

    public static Specification<Contrat> dateCreationAvant(LocalDateTime fin) {
        return (root, query, cb) -> fin == null
                ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("dateCreation"), fin);
    }
}
