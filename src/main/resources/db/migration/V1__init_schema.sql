CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    telephone VARCHAR(30),
    password_hash VARCHAR(255) NOT NULL,
    cin_numero VARCHAR(30),
    role VARCHAR(30) NOT NULL DEFAULT 'USER'
);

CREATE TABLE IF NOT EXISTS properties (
    id BIGSERIAL PRIMARY KEY,
    proprietaire_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    adresse VARCHAR(255) NOT NULL,
    quartier VARCHAR(100),
    ville VARCHAR(100) NOT NULL,
    ref_cadastre VARCHAR(100)
);

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

CREATE TABLE IF NOT EXISTS contract_clauses (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    ordre INT NOT NULL,
    titre VARCHAR(150) NOT NULL,
    texte TEXT NOT NULL
);

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

CREATE TABLE IF NOT EXISTS signatures (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    signataire_id BIGINT NOT NULL REFERENCES users(id),
    hash_sha256 VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    otp_valide BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS amendments (
    id BIGSERIAL PRIMARY KEY,
    parent_contract_id BIGINT NOT NULL REFERENCES contracts(id),
    new_contract_id BIGINT NOT NULL REFERENCES contracts(id),
    motif TEXT NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
