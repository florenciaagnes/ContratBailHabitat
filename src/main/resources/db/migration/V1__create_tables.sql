-- ============================================================
-- Contrats de bail d'habitation - schema initial
-- ============================================================

CREATE TABLE personne (
    id                      BIGSERIAL PRIMARY KEY,
    nom                     VARCHAR(150) NOT NULL,
    prenom                  VARCHAR(150),
    cin                     VARCHAR(50),
    date_delivrance_cin     DATE,
    lieu_delivrance_cin     VARCHAR(150),
    adresse                 VARCHAR(500),
    telephone               VARCHAR(50),
    email                   VARCHAR(150),
    date_creation           TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_personne_nom ON personne (lower(nom));
CREATE INDEX idx_personne_cin ON personne (cin);

CREATE TABLE organisation (
    id                      BIGSERIAL PRIMARY KEY,
    nom                     VARCHAR(200) NOT NULL,
    forme_juridique         VARCHAR(100),
    siege_social            VARCHAR(500),
    adresse                 VARCHAR(500),
    telephone               VARCHAR(50),
    email                   VARCHAR(150),
    numero_identification   VARCHAR(100),
    date_creation           TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_organisation_nom ON organisation (lower(nom));

CREATE TABLE bien (
    id              BIGSERIAL PRIMARY KEY,
    adresse         VARCHAR(500) NOT NULL,
    description     TEXT,
    type_bien       VARCHAR(100),
    surface         NUMERIC(10,2),
    reference       VARCHAR(100)
);

CREATE TABLE contrat (
    id                  BIGSERIAL PRIMARY KEY,
    numero              VARCHAR(50) NOT NULL UNIQUE,
    date_creation       TIMESTAMP NOT NULL DEFAULT now(),
    date_debut          DATE,
    date_fin            DATE,
    id_bien             BIGINT NOT NULL REFERENCES bien(id),
    statut              VARCHAR(30) NOT NULL DEFAULT 'BROUILLON'
                            CHECK (statut IN ('BROUILLON','EN_ATTENTE_SIGNATURE','SIGNE','ARCHIVE','RESILIE','EXPIRE')),
    date_signature      TIMESTAMP,
    date_archivage      TIMESTAMP,
    CONSTRAINT chk_contrat_dates CHECK (date_fin IS NULL OR date_debut IS NULL OR date_fin >= date_debut)
);
CREATE INDEX idx_contrat_statut ON contrat (statut);
CREATE INDEX idx_contrat_dates ON contrat (date_debut, date_fin);

-- Sequence utilisee pour generer le numero de contrat (BAIL-<annee>-<seq>)
CREATE SEQUENCE contrat_numero_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE partie_contrat (
    id              BIGSERIAL PRIMARY KEY,
    id_contrat      BIGINT NOT NULL REFERENCES contrat(id) ON DELETE CASCADE,
    type_partie     VARCHAR(20) NOT NULL CHECK (type_partie IN ('PERSONNE','ORGANISATION')),
    id_personne     BIGINT REFERENCES personne(id),
    id_organisation BIGINT REFERENCES organisation(id),
    role            VARCHAR(20) NOT NULL CHECK (role IN ('PROPRIETAIRE','LOCATAIRE')),
    ordre           INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_partie_reference CHECK (
        (type_partie = 'PERSONNE' AND id_personne IS NOT NULL AND id_organisation IS NULL) OR
        (type_partie = 'ORGANISATION' AND id_organisation IS NOT NULL AND id_personne IS NULL)
    )
);
CREATE INDEX idx_partie_contrat ON partie_contrat (id_contrat);

CREATE TABLE representant_organisation (
    id              BIGSERIAL PRIMARY KEY,
    id_organisation BIGINT NOT NULL REFERENCES organisation(id) ON DELETE CASCADE,
    id_personne     BIGINT NOT NULL REFERENCES personne(id),
    fonction        VARCHAR(150),
    ordre           INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_representant_organisation ON representant_organisation (id_organisation);

CREATE TABLE modele_article (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(80) NOT NULL UNIQUE,
    titre               VARCHAR(200) NOT NULL,
    contenu_modele      TEXT NOT NULL,
    ordre_defaut        INTEGER NOT NULL DEFAULT 0,
    obligatoire         BOOLEAN NOT NULL DEFAULT FALSE,
    actif               BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation       TIMESTAMP NOT NULL DEFAULT now(),
    date_modification   TIMESTAMP
);

CREATE TABLE variable_article (
    id                  BIGSERIAL PRIMARY KEY,
    id_modele_article   BIGINT NOT NULL REFERENCES modele_article(id) ON DELETE CASCADE,
    nom                 VARCHAR(100) NOT NULL,
    libelle             VARCHAR(200) NOT NULL,
    type                VARCHAR(20) NOT NULL CHECK (type IN ('TEXT','NUMBER','MONEY','DATE','BOOLEAN','TEXTAREA')),
    obligatoire         BOOLEAN NOT NULL DEFAULT TRUE,
    ordre               INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_variable_par_article UNIQUE (id_modele_article, nom)
);

CREATE TABLE contrat_article (
    id                  BIGSERIAL PRIMARY KEY,
    id_contrat          BIGINT NOT NULL REFERENCES contrat(id) ON DELETE CASCADE,
    id_modele_article   BIGINT NOT NULL REFERENCES modele_article(id),
    ordre               INTEGER NOT NULL DEFAULT 0,
    contenu_final       TEXT NOT NULL
);
CREATE INDEX idx_contrat_article_contrat ON contrat_article (id_contrat);

CREATE TABLE contrat_variable (
    id                  BIGSERIAL PRIMARY KEY,
    id_contrat_article  BIGINT NOT NULL REFERENCES contrat_article(id) ON DELETE CASCADE,
    id_variable         BIGINT NOT NULL REFERENCES variable_article(id),
    valeur              TEXT
);
CREATE INDEX idx_contrat_variable_article ON contrat_variable (id_contrat_article);

CREATE TABLE signature (
    id                  BIGSERIAL PRIMARY KEY,
    id_contrat          BIGINT NOT NULL REFERENCES contrat(id) ON DELETE CASCADE,
    id_personne         BIGINT NOT NULL REFERENCES personne(id),
    signature_data      TEXT NOT NULL,
    date_signature      TIMESTAMP NOT NULL DEFAULT now(),
    type_signature      VARCHAR(20) NOT NULL DEFAULT 'DESSINEE' CHECK (type_signature IN ('DESSINEE')),
    ordre               INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_signature_contrat ON signature (id_contrat);

CREATE TABLE document_contrat (
    id                  BIGSERIAL PRIMARY KEY,
    id_contrat          BIGINT NOT NULL REFERENCES contrat(id) ON DELETE CASCADE,
    nom_fichier         VARCHAR(255) NOT NULL,
    chemin_fichier      VARCHAR(1000) NOT NULL,
    type_document       VARCHAR(50) NOT NULL DEFAULT 'PDF_CONTRAT',
    date_generation     TIMESTAMP NOT NULL DEFAULT now(),
    date_archivage      TIMESTAMP,
    hash_document       VARCHAR(128)
);
CREATE INDEX idx_document_contrat ON document_contrat (id_contrat);

CREATE TABLE mode_payement(
        id                  BIGSERIAL PRIMARY KEY,
        libelle             VARCHAR(12)
)
