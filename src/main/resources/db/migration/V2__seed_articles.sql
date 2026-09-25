-- ============================================================
-- Articles initiaux du contrat de bail
-- NOTE : le contenu juridique exact de certains articles n'a pas ete fourni.
-- Les articles marques [A COMPLETER] sont des placeholders a valider par un
-- administrateur / juriste avant utilisation en production.
-- ============================================================

INSERT INTO modele_article (code, titre, contenu_modele, ordre_defaut, obligatoire, actif) VALUES
('OBJET_CONTRAT', 'OBJET DU CONTRAT',
 'Le présent contrat a pour objet la location du bien désigné ci-dessus, à usage exclusif d''habitation, sis {{adresse_bien}}.',
 1, TRUE, TRUE),

('MONTANT_LOYER', 'MONTANT DU LOYER',
 'Le montant du loyer mensuel convenu entre les parties est fixé à {{montant_loyer}} Francs malagasy (Ar {{montant_loyer}}).',
 2, TRUE, TRUE),

('MODALITE_PAIEMENT', 'MODALITE DE PAIEMENTS',
 'Le loyer est payable mensuellement, d''avance, au plus tard le {{jour_paiement}} de chaque mois, par {{mode_paiement}}.',
 3, TRUE, TRUE),

('DUREE', 'DUREE DU BAIL',
 '[A COMPLETER] Le présent bail est conclu pour une durée de {{duree_mois}} mois, à compter du {{date_debut}} jusqu''au {{date_fin}}, renouvelable par tacite reconduction sauf préavis contraire conforme à la réglementation en vigueur.',
 4, TRUE, TRUE),

('DEPOT_GARANTIE', 'DEPOT DE GARANTIE',
 '[A COMPLETER] A la signature du présent contrat, le locataire verse au propriétaire un dépôt de garantie d''un montant de {{montant_depot}} Francs malagasy, restituable en fin de bail sous déduction des sommes dues et des dégradations constatées.',
 5, FALSE, TRUE),

('DROIT_LOCATAIRE', 'DROIT DU LOCATAIRE',
 '[A COMPLETER] Le locataire a le droit de jouir paisiblement des lieux loués pendant toute la durée du bail, sous réserve du respect des clauses du présent contrat et de la réglementation applicable.',
 6, TRUE, TRUE),

('ENTRETIEN_REPARATION', 'ENTRETIEN ET REPARATION',
 '[A COMPLETER] Le locataire s''engage à entretenir le bien loué en bon état et à en assurer les réparations locatives. Les grosses réparations restent à la charge du propriétaire, conformément à la réglementation en vigueur.',
 7, TRUE, TRUE);

-- Variables associees a chaque article

INSERT INTO variable_article (id_modele_article, nom, libelle, type, obligatoire, ordre)
SELECT id, 'adresse_bien', 'Adresse du bien loué', 'TEXT', TRUE, 1 FROM modele_article WHERE code = 'OBJET_CONTRAT';

INSERT INTO variable_article (id_modele_article, nom, libelle, type, obligatoire, ordre)
SELECT id, 'montant_loyer', 'Montant du loyer mensuel', 'MONEY', TRUE, 1 FROM modele_article WHERE code = 'MONTANT_LOYER';

INSERT INTO variable_article (id_modele_article, nom, libelle, type, obligatoire, ordre)
SELECT id, 'jour_paiement', 'Jour de paiement (1-31)', 'NUMBER', TRUE, 1 FROM modele_article WHERE code = 'MODALITE_PAIEMENT';
INSERT INTO variable_article (id_modele_article, nom, libelle, type, obligatoire, ordre)
SELECT id, 'mode_paiement', 'Mode de paiement', 'TEXT', TRUE, 2 FROM modele_article WHERE code = 'MODALITE_PAIEMENT';

INSERT INTO variable_article (id_modele_article, nom, libelle, type, obligatoire, ordre)
SELECT id, 'duree_mois', 'Durée du bail (en mois)', 'NUMBER', TRUE, 1 FROM modele_article WHERE code = 'DUREE';
INSERT INTO variable_article (id_modele_article, nom, libelle, type, obligatoire, ordre)
SELECT id, 'date_debut', 'Date de début du bail', 'DATE', TRUE, 2 FROM modele_article WHERE code = 'DUREE';
INSERT INTO variable_article (id_modele_article, nom, libelle, type, obligatoire, ordre)
SELECT id, 'date_fin', 'Date de fin du bail', 'DATE', TRUE, 3 FROM modele_article WHERE code = 'DUREE';

INSERT INTO variable_article (id_modele_article, nom, libelle, type, obligatoire, ordre)
SELECT id, 'montant_depot', 'Montant du dépôt de garantie', 'MONEY', TRUE, 1 FROM modele_article WHERE code = 'DEPOT_GARANTIE';
