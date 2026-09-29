-- ============================================================
-- 1) Variables a valeur automatique (dates reprises du contrat)
-- ============================================================
ALTER TABLE variable_article
    ADD COLUMN valeur_auto VARCHAR(20) CHECK (valeur_auto IN ('DATE_DEBUT', 'DATE_FIN'));

UPDATE variable_article SET valeur_auto = 'DATE_DEBUT'
WHERE nom = 'date_debut'
  AND id_modele_article = (SELECT id FROM modele_article WHERE code = 'DUREE');

UPDATE variable_article SET valeur_auto = 'DATE_FIN'
WHERE nom = 'date_fin'
  AND id_modele_article = (SELECT id FROM modele_article WHERE code = 'DUREE');

-- ============================================================
-- 2) Avenants aux contrats
-- ============================================================
CREATE TABLE avenant (
    id                  BIGSERIAL PRIMARY KEY,
    id_contrat          BIGINT NOT NULL REFERENCES contrat(id) ON DELETE CASCADE,
    numero              VARCHAR(60) NOT NULL UNIQUE,
    date_creation       TIMESTAMP NOT NULL DEFAULT now(),
    date_effet          DATE NOT NULL,
    objet               VARCHAR(300) NOT NULL,
    nouvelle_date_fin   DATE,
    statut              VARCHAR(30) NOT NULL DEFAULT 'BROUILLON'
                            CHECK (statut IN ('BROUILLON', 'EN_ATTENTE_SIGNATURE', 'ARCHIVE')),
    date_archivage      TIMESTAMP,
    CONSTRAINT chk_avenant_dates CHECK (nouvelle_date_fin IS NULL OR nouvelle_date_fin >= date_effet)
);
CREATE INDEX idx_avenant_contrat ON avenant (id_contrat);

CREATE TABLE avenant_clause (
    id          BIGSERIAL PRIMARY KEY,
    id_avenant  BIGINT NOT NULL REFERENCES avenant(id) ON DELETE CASCADE,
    ordre       INTEGER NOT NULL DEFAULT 0,
    titre       VARCHAR(200) NOT NULL,
    contenu     TEXT NOT NULL
);
CREATE INDEX idx_avenant_clause_avenant ON avenant_clause (id_avenant);

CREATE TABLE signature_avenant (
    id              BIGSERIAL PRIMARY KEY,
    id_avenant      BIGINT NOT NULL REFERENCES avenant(id) ON DELETE CASCADE,
    id_personne     BIGINT NOT NULL REFERENCES personne(id),
    signature_data  TEXT NOT NULL,
    date_signature  TIMESTAMP NOT NULL DEFAULT now(),
    ordre           INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_signature_avenant ON signature_avenant (id_avenant);
