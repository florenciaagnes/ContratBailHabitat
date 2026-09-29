# Contrats de bail d'habitation

Application Spring Boot (Java 17) de génération et gestion de contrats de bail
d'habitation, avec base PostgreSQL, génération de PDF, signature électronique
dessinée (canvas), recherche et archivage.

## ⚠️ À savoir avant de démarrer

- **Aucune image de modèle n'a été transmise** dans la conversation qui a servi
  à générer ce projet (le texte du cahier des charges y faisait référence, mais
  aucun fichier image n'était joint). La mise en page du PDF (`PdfGenerationService`)
  suit donc la structure **textuelle** décrite dans le cahier des charges
  (titre centré, bloc "Entre", articles numérotés, signatures). Si vous avez le
  modèle visuel réel, il faudra ajuster ce service (polices, marges, tableaux,
  positionnement exact) pour coller au document papier.
- Le **contenu juridique exact** de plusieurs articles (durée, dépôt de garantie,
  droit du locataire, entretien et réparation) n'a pas été fourni. Les textes
  correspondants dans `V2__seed_articles.sql` sont marqués **`[A COMPLETER]`** :
  ce sont des placeholders à valider par un juriste avant toute utilisation réelle.
  Ne les considérez pas comme des références légales.
- Spring Security n'est **pas** activé (non demandé dans le MVP) : il n'y a donc
  aucune authentification. À ajouter avant toute mise en production multi-utilisateur.
- La signature est présentée comme une **signature dessinée / électronique simple**,
  sans valeur juridique équivalente à une signature électronique qualifiée.

## Prérequis

- Java 17+
- Maven 3.9+
- PostgreSQL 14+

## Mise en route

```bash
# 1. Créer la base de données
createdb contrats_bail

# 2. Adapter si besoin les identifiants dans
#    src/main/resources/application.properties
#    (spring.datasource.url / username / password)

# 3. Lancer l'application (Flyway crée le schéma et insère les articles de base)
mvn spring-boot:run
```

L'application est ensuite disponible sur http://localhost:8080 et redirige
vers `/contrats`.

## Fonctionnement général

1. **`/contrats/nouveau`** — formulaire de création en deux volets (saisie à
   gauche, aperçu du contrat en temps réel à droite). Propriétaire (personne
   physique), locataire (personne physique **ou** société avec un nombre
   illimité de représentants), bien loué, ajout d'articles depuis la
   bibliothèque via une popup de recherche, avec remplacement des variables
   `{{nom_variable}}` en direct.
2. À la création, le contrat est enregistré en base avec un **numéro unique**
   `BAIL-<année>-<séquence>` et le statut `BROUILLON`.
3. **`/contrats/{id}`** — consultation du contrat : ajout d'articles
   supplémentaires, signature au canvas (souris/tactile) pour chaque personne
   concernée (propriétaire, locataire ou représentants), aperçu PDF en ligne,
   téléchargement, archivage.
4. **Signatures et statuts automatiques** : dès que toutes les parties
   (propriétaire, locataire ou représentants) ont signé, le contrat passe
   automatiquement au statut **SIGNÉ**. **Archivage** : génère le PDF final,
   l'enregistre sur disque (chemin configurable via `app.documents.storage-path`)
   avec son hash SHA-256, et ajoute le statut `ARCHIVÉ`. Le contenu des
   articles déjà ajoutés à un contrat (`contenu_final`) n'est **jamais**
   recalculé si le modèle d'article est modifié ensuite. Un contrat **cumule**
   ses statuts (ex. `SIGNÉ` + `ARCHIVÉ` + `EXPIRÉ` en même temps) : ils sont
   stockés dans une table dédiée (`statut_contrat`) et tous affichés, sous
   forme de badges, dans les tableaux (liste des contrats, archives).
5. **Expiration automatique** : un contrat dont la date de fin effective
   (celle du bail, ou la nouvelle date de fin du dernier avenant archivé) est
   dépassée reçoit automatiquement le statut **EXPIRÉ**, y compris si on crée
   un nouveau contrat déjà expiré. Cette synchronisation tourne au démarrage
   de l'application puis chaque nuit à 00h05 (heure de Madagascar).
   Les variables marquées « valeur automatique » (ex. `date_debut` et
   `date_fin` de l'article DUREE) ne sont plus saisies : elles sont reprises
   des dates de la section « Durée & bien loué ».
6. **`/contrats/archives`** — recherche (numéro, nom, prénom, CIN, société,
   adresse) insensible à la casse, filtrable par statut et par dates.
6. **Avenants** — depuis la page d'un contrat **signé**, bouton « + Nouvel
   avenant » : objet, date d'effet, une ou plusieurs clauses **choisies dans
   une bibliothèque de modèles** (exactement comme les articles d'un contrat :
   on sélectionne un titre puis on ne remplit que ses variables, y compris des
   listes déroulantes) et/ou une nouvelle date de fin. L'avenant a son propre
   numéro (`BAIL-2026-001-AV1`), son PDF, ses signatures (propriétaire,
   locataire ou représentants) et son archivage. Le contrat archivé n'est
   **jamais modifié** : la date de fin effective est affichée sur la page du
   contrat à partir des avenants archivés.
7. **`/clauses-avenant`** — bibliothèque des modèles de clauses d'avenant
   (même principe que `/articles`) : révision du loyer, changement du mode de
   paiement, clause libre... avec variables typées, y compris des listes
   déroulantes (ex. mode de paiement : Espèces / MVola / Orange Money /
   Airtel Money / Virement bancaire / Chèque).
8. **`/articles`** — administration de la bibliothèque de modèles d'articles
   (créer, modifier, désactiver, gérer les variables et leur type :
   TEXT / NUMBER / MONEY / DATE / BOOLEAN / TEXTAREA).

## Structure du projet

```
entity/       Entités JPA (Personne, Organisation, Bien, Contrat, PartieContrat,
              RepresentantOrganisation, ModeleArticle, VariableArticle,
              ContratArticle, ContratVariable, Signature, DocumentContrat)
repository/   Interfaces Spring Data JPA
service/      Logique métier (ContratService, ArticleService,
              PdfGenerationService, DocumentStorageService)
controller/   Contrôleurs MVC (pages Thymeleaf) et REST (API JSON utilisée par
              le JavaScript des pages)
dto/          Objets de transfert pour la création/modification de contrats
exception/    Exceptions métier + gestionnaire global
util/         Substitution des variables {{...}} dans les modèles d'articles
templates/    Vues Thymeleaf
static/       CSS et JavaScript (aucun framework, JS natif)
db/migration/ Scripts Flyway (schéma + données initiales)
```

## Limites connues du MVP

- `spring.jpa.open-in-view` est activé (`true`) pour permettre aux templates
  Thymeleaf de lire les associations lazy (parties, articles, signatures,
  représentants) après le retour du contrôleur. C'est un compromis pratique
  pour ce MVP ; pour une version plus avancée, mieux vaut charger
  explicitement ces associations dans les services (`JOIN FETCH` ou DTOs
  dédiés) et repasser `open-in-view` à `false`.
- Pas de gestion des doublons de personnes/organisations (chaque saisie crée
  un nouvel enregistrement, sauf si un `id` existant est transmis).
- Bibliothèque d'articles / de clauses d'avenant : les variables existantes
  peuvent être modifiées (libellé, type, options, valeur automatique) mais
  pas supprimées ni renommées.
- Le statut « workflow » d'un contrat (colonne `contrat.statut`, utilisée
  pour les autorisations : modifier/signer/archiver) reste une valeur unique
  (BROUILLON → EN_ATTENTE_SIGNATURE → SIGNE → ARCHIVE). Seul l'**affichage**
  (table `statut_contrat`) cumule plusieurs statuts, dont EXPIRE qui peut
  s'ajouter à n'importe lequel des précédents.
- Avenants : une fois créé, un avenant n'est pas modifiable (ses clauses ne
  peuvent pas être éditées) ; en cas d'erreur, en créer un nouveau. La phrase
  « Toutes les autres clauses ... demeurent inchangées » ajoutée dans le PDF
  est une formule courante à faire valider par un juriste.
- Pas de pagination sur les listes de contrats / archives.
- Pas de notifications d'expiration (30/15/7 jours) — non demandées dans ce
  cahier des charges, mais mentionnées dans un échange précédent sur le même
  projet ; à ajouter si nécessaire.
