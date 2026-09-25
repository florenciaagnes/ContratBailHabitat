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
4. **Archivage** : génère le PDF final, l'enregistre sur disque (chemin
   configurable via `app.documents.storage-path`) avec son hash SHA-256, et
   fige le statut du contrat à `ARCHIVE`. Le contenu des articles déjà ajoutés
   à un contrat (`contenu_final`) n'est **jamais** recalculé si le modèle
   d'article est modifié ensuite.
5. **`/contrats/archives`** — recherche (numéro, nom, prénom, CIN, société,
   adresse) insensible à la casse, filtrable par statut et par dates.
6. **`/articles`** — administration de la bibliothèque de modèles d'articles
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
- La modification des variables d'un article existant (bibliothèque) ne gère
  que l'ajout de nouvelles variables, pas la mise à jour des variables déjà
  créées (à compléter si besoin : endpoint `PUT /api/articles/{id}/variables/{varId}`).
- Pas de pagination sur les listes de contrats / archives.
- Pas de notifications d'expiration (30/15/7 jours) — non demandées dans ce
  cahier des charges, mais mentionnées dans un échange précédent sur le même
  projet ; à ajouter si nécessaire.
