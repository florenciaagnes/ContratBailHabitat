-- ============================================================
-- Bibliotheque de clauses d'avenant : meme principe que la
-- bibliotheque d'articles (modele + variables + valeurs saisies),
-- pour que l'ajout d'une clause a un avenant se fasse en choisissant
-- un titre puis en remplissant uniquement ses variables.
-- ============================================================

CREATE TABLE modele_clause_avenant (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(80) NOT NULL UNIQUE,
    titre               VARCHAR(200) NOT NULL,
    contenu_modele      TEXT NOT NULL,
    ordre_defaut        INTEGER NOT NULL DEFAULT 0,
    actif               BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation       TIMESTAMP NOT NULL DEFAULT now(),
    date_modification   TIMESTAMP
);

CREATE TABLE variable_clause_avenant (
    id                      BIGSERIAL PRIMARY KEY,
    id_modele_clause        BIGINT NOT NULL REFERENCES modele_clause_avenant(id) ON DELETE CASCADE,
    nom                     VARCHAR(100) NOT NULL,
    libelle                 VARCHAR(200) NOT NULL,
    type                    VARCHAR(20) NOT NULL CHECK (type IN ('TEXT','NUMBER','MONEY','DATE','BOOLEAN','TEXTAREA','LISTE')),
    obligatoire             BOOLEAN NOT NULL DEFAULT TRUE,
    ordre                   INTEGER NOT NULL DEFAULT 0,
    options                 TEXT,
    CONSTRAINT uq_variable_par_clause UNIQUE (id_modele_clause, nom)
);

-- Quelques modeles de clauses courants (contenu a valider par un juriste)
INSERT INTO modele_clause_avenant (code, titre, contenu_modele, ordre_defaut) VALUES
('REVISION_LOYER', 'RÉVISION DU LOYER',
 'À compter du {{date_effet}}, le montant du loyer mensuel est porté à {{nouveau_montant_loyer}} Francs malagasy, en remplacement du montant initialement prévu au contrat.',
 1),
('CHANGEMENT_MODE_PAIEMENT', 'CHANGEMENT DU MODE DE PAIEMENT',
 'Les parties conviennent que le loyer sera désormais réglé par {{nouveau_mode_paiement}}, en remplacement du mode de paiement initialement prévu au contrat.',
 2),
('AUTRE_CLAUSE', 'AUTRE DISPOSITION',
 '{{texte_libre}}',
 3);

INSERT INTO variable_clause_avenant (id_modele_clause, nom, libelle, type, obligatoire, ordre)
SELECT id, 'date_effet', 'Date d''effet (rappel)', 'DATE', TRUE, 1 FROM modele_clause_avenant WHERE code = 'REVISION_LOYER';
INSERT INTO variable_clause_avenant (id_modele_clause, nom, libelle, type, obligatoire, ordre)
SELECT id, 'nouveau_montant_loyer', 'Nouveau montant du loyer', 'MONEY', TRUE, 2 FROM modele_clause_avenant WHERE code = 'REVISION_LOYER';

INSERT INTO variable_clause_avenant (id_modele_clause, nom, libelle, type, obligatoire, ordre, options)
SELECT id, 'nouveau_mode_paiement', 'Nouveau mode de paiement', 'LISTE', TRUE, 1,
       E'Espèces\nMVola\nOrange Money\nAirtel Money\nVirement bancaire\nChèque'
FROM modele_clause_avenant WHERE code = 'CHANGEMENT_MODE_PAIEMENT';

INSERT INTO variable_clause_avenant (id_modele_clause, nom, libelle, type, obligatoire, ordre)
SELECT id, 'texte_libre', 'Texte de la clause', 'TEXTAREA', TRUE, 1 FROM modele_clause_avenant WHERE code = 'AUTRE_CLAUSE';
