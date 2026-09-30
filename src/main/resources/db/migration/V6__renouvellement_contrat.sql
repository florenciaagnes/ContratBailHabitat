-- ============================================================
-- Renouvellement d'un contrat expire par tacite reconduction :
-- cree un NOUVEAU contrat (memes bien/parties/articles) avec une
-- nouvelle date de fin, trace vers le contrat d'origine.
-- ============================================================
ALTER TABLE contrat
    ADD COLUMN id_contrat_origine BIGINT REFERENCES contrat(id) ON DELETE SET NULL;

CREATE INDEX idx_contrat_origine ON contrat (id_contrat_origine);
