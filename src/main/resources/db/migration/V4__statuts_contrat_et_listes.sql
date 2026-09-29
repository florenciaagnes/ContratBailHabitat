-- ============================================================
-- 1) Variables de type LISTE (liste deroulante)
-- ============================================================
ALTER TABLE variable_article ADD COLUMN options TEXT;

DO $$
DECLARE c TEXT;
BEGIN
    SELECT conname INTO c FROM pg_constraint
    WHERE conrelid = 'variable_article'::regclass AND contype = 'c'
      AND pg_get_constraintdef(oid) LIKE '%TEXTAREA%';
    IF c IS NOT NULL THEN
        EXECUTE format('ALTER TABLE variable_article DROP CONSTRAINT %I', c);
    END IF;
END $$;

ALTER TABLE variable_article ADD CONSTRAINT variable_article_type_check
    CHECK (type IN ('TEXT', 'NUMBER', 'MONEY', 'DATE', 'BOOLEAN', 'TEXTAREA', 'LISTE'));

-- Mode de paiement : liste deroulante (options modifiables dans la bibliotheque d'articles)
UPDATE variable_article
SET type = 'LISTE',
    options = E'Espèces\nMVola\nOrange Money\nAirtel Money\nVirement bancaire\nChèque'
WHERE nom = 'mode_paiement'
  AND id_modele_article = (SELECT id FROM modele_article WHERE code = 'MODALITE_PAIEMENT');

-- ============================================================
-- 2) Table des statuts d'un contrat (un contrat peut cumuler plusieurs statuts :
--    ex. SIGNE + ARCHIVE + EXPIRE)
-- ============================================================
CREATE TABLE statut_contrat (
    id          BIGSERIAL PRIMARY KEY,
    id_contrat  BIGINT NOT NULL REFERENCES contrat(id) ON DELETE CASCADE,
    statut      VARCHAR(30) NOT NULL
                    CHECK (statut IN ('BROUILLON','EN_ATTENTE_SIGNATURE','SIGNE','ARCHIVE','RESILIE','EXPIRE')),
    date_statut TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_statut_par_contrat UNIQUE (id_contrat, statut)
);
CREATE INDEX idx_statut_contrat_contrat ON statut_contrat (id_contrat);

-- Contrats deja signes par toutes les parties (personnes ou representants de societe)
CREATE TEMP TABLE contrats_signes AS
WITH attendus AS (
    SELECT pc.id_contrat, pc.id_personne
    FROM partie_contrat pc
    WHERE pc.type_partie = 'PERSONNE'
    UNION
    SELECT pc.id_contrat, ro.id_personne
    FROM partie_contrat pc
    JOIN representant_organisation ro ON ro.id_organisation = pc.id_organisation
    WHERE pc.type_partie = 'ORGANISATION'
)
SELECT c.id
FROM contrat c
WHERE EXISTS (SELECT 1 FROM attendus a WHERE a.id_contrat = c.id)
  AND NOT EXISTS (
        SELECT 1 FROM attendus a
        WHERE a.id_contrat = c.id
          AND NOT EXISTS (SELECT 1 FROM signature s
                          WHERE s.id_contrat = c.id AND s.id_personne = a.id_personne));

UPDATE contrat
SET statut = 'SIGNE', date_signature = COALESCE(date_signature, now())
WHERE id IN (SELECT id FROM contrats_signes)
  AND statut IN ('BROUILLON', 'EN_ATTENTE_SIGNATURE');

-- Reprise du statut courant de chaque contrat existant
INSERT INTO statut_contrat (id_contrat, statut, date_statut)
SELECT id, statut, COALESCE(date_archivage, date_signature, date_creation) FROM contrat;

-- Statut SIGNE pour les contrats deja signes (les contrats archives l'ont forcement ete)
INSERT INTO statut_contrat (id_contrat, statut, date_statut)
SELECT c.id, 'SIGNE', COALESCE(c.date_signature, c.date_archivage, c.date_creation)
FROM contrat c
WHERE c.id IN (SELECT id FROM contrats_signes) OR c.statut = 'ARCHIVE'
ON CONFLICT (id_contrat, statut) DO NOTHING;

-- Un statut de workflow remplace les precedents
DELETE FROM statut_contrat
WHERE statut IN ('BROUILLON', 'EN_ATTENTE_SIGNATURE')
  AND id_contrat IN (SELECT id_contrat FROM statut_contrat WHERE statut IN ('SIGNE', 'ARCHIVE'));

DELETE FROM statut_contrat s
WHERE s.statut = 'BROUILLON'
  AND EXISTS (SELECT 1 FROM statut_contrat e
              WHERE e.id_contrat = s.id_contrat AND e.statut = 'EN_ATTENTE_SIGNATURE');

DROP TABLE contrats_signes;

-- Le statut EXPIRE est calcule et ajoute par l'application au demarrage puis chaque nuit.
