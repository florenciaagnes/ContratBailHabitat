-- ==============================================================================
-- SCHÉMA POSTGRESQL COMPLETE - LEGALTECH MADAGASCAR
-- ==============================================================================
-- Database creation instruction (to be run in psql CLI):
-- CREATE DATABASE legaltech_bail OWNER postgres;
-- \c legaltech_bail

-- 1. Table users
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    telephone VARCHAR(30),
    password_hash VARCHAR(255) NOT NULL,
    cin_numero VARCHAR(30),
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    date_naissance DATE,
    genre VARCHAR(20),
    situation_matrimoniale VARCHAR(30)
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS date_naissance DATE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS genre VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS situation_matrimoniale VARCHAR(30);

-- 2. Table properties
CREATE TABLE IF NOT EXISTS properties (
    id BIGSERIAL PRIMARY KEY,
    proprietaire_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    adresse VARCHAR(255) NOT NULL,
    quartier VARCHAR(100),
    ville VARCHAR(100) NOT NULL,
    ref_cadastre VARCHAR(100),
    type_bien VARCHAR(50) DEFAULT 'APPARTEMENT',
    superficie NUMERIC(8, 2),
    nombre_pieces INT DEFAULT 1,
    nombre_chambres INT DEFAULT 1,
    etage INT DEFAULT 0,
    meuble BOOLEAN DEFAULT FALSE,
    parking BOOLEAN DEFAULT FALSE,
    compteur_jirama BOOLEAN DEFAULT TRUE
);

ALTER TABLE properties ADD COLUMN IF NOT EXISTS type_bien VARCHAR(50) DEFAULT 'APPARTEMENT';
ALTER TABLE properties ADD COLUMN IF NOT EXISTS superficie NUMERIC(8, 2);
ALTER TABLE properties ADD COLUMN IF NOT EXISTS nombre_pieces INT DEFAULT 1;
ALTER TABLE properties ADD COLUMN IF NOT EXISTS nombre_chambres INT DEFAULT 1;
ALTER TABLE properties ADD COLUMN IF NOT EXISTS etage INT DEFAULT 0;
ALTER TABLE properties ADD COLUMN IF NOT EXISTS meuble BOOLEAN DEFAULT FALSE;
ALTER TABLE properties ADD COLUMN IF NOT EXISTS parking BOOLEAN DEFAULT FALSE;
ALTER TABLE properties ADD COLUMN IF NOT EXISTS compteur_jirama BOOLEAN DEFAULT TRUE;


-- 3. Table contracts
CREATE TABLE IF NOT EXISTS contracts (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(50) UNIQUE NOT NULL,
    bailleur_id BIGINT REFERENCES users(id),
    locataire_id BIGINT REFERENCES users(id),
    bien_id BIGINT REFERENCES properties(id),
    loyer NUMERIC(12, 2) NOT NULL,
    charges NUMERIC(12, 2) DEFAULT 0.00,
    depot NUMERIC(12, 2) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    tacite_reconduction BOOLEAN DEFAULT TRUE,
    statut VARCHAR(40) NOT NULL DEFAULT 'PENDING_AUDIT',
    search_vector TSVECTOR
);

-- 4. Table contract_clauses
CREATE TABLE IF NOT EXISTS contract_clauses (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    ordre INT NOT NULL,
    titre VARCHAR(150) NOT NULL,
    texte TEXT NOT NULL
);

-- 5. Table legal_exceptions
CREATE TABLE IF NOT EXISTS legal_exceptions (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    clause_index INT NOT NULL,
    source_document VARCHAR(255) NOT NULL,
    page_reference VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    extrait_loi TEXT NOT NULL,
    resolu BOOLEAN DEFAULT FALSE
);

-- 6. Table signatures
CREATE TABLE IF NOT EXISTS signatures (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    signataire_id BIGINT NOT NULL REFERENCES users(id),
    hash_sha256 VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    otp_valide BOOLEAN DEFAULT FALSE
);

-- 7. Table amendments
CREATE TABLE IF NOT EXISTS amendments (
    id BIGSERIAL PRIMARY KEY,
    parent_contract_id BIGINT NOT NULL REFERENCES contracts(id),
    new_contract_id BIGINT NOT NULL REFERENCES contracts(id),
    motif TEXT NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Triggers & Full-Text Search Index (TSVECTOR)
CREATE OR REPLACE FUNCTION contracts_tsvector_trigger() RETURNS trigger AS $$
BEGIN
  new.search_vector :=
     setweight(to_tsvector('pg_catalog.french', coalesce(new.reference,'')), 'A') ||
     setweight(to_tsvector('pg_catalog.french', coalesce((SELECT ville FROM properties WHERE id = new.bien_id),'')), 'B') ||
     setweight(to_tsvector('pg_catalog.french', coalesce((SELECT adresse FROM properties WHERE id = new.bien_id),'')), 'B');
  RETURN new;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_contracts_tsvector ON contracts;
CREATE TRIGGER trg_contracts_tsvector
  BEFORE INSERT OR UPDATE ON contracts
  FOR EACH ROW EXECUTE FUNCTION contracts_tsvector_trigger();

CREATE INDEX IF NOT EXISTS idx_contracts_search_vector ON contracts USING GIN(search_vector);
