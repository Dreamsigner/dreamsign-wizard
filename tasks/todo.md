# Studio IA — suivi

## Première tranche

- [x] Examiner LearnHarness et le dépôt existant.
- [x] Confirmer le maintien de Kotlin/Compose avec l'utilisateur.
- [x] Décrire le premier parcours et les limites de parité.
- [x] Implémenter séances, contrôleur et interfaces de tutorat/stockage.
- [x] Brancher persistance et HTTP Windows.
- [x] Intégrer la navigation et les écrans Compose.
- [x] Valider tests déterministes, régressions et captures.
- [x] Construire l'application et pousser le code.

## Étapes ultérieures

- [ ] Examiner et prioriser les [pistes issues des PR LearnHarness](../docs/product/2026-10-30-learnharness-pr-features.md).

- [x] Découverte à la demande des modèles d'un service compatible Chat Completions, recherche et choix par séance, avec conservation du fournisseur et du niveau de réflexion.
- [x] Modèles principal et secondaire, exceptions par tâche et niveau de réflexion utilisés par les appels du Studio IA.
- [x] Étendre le rendu des réponses du Studio IA aux délimiteurs LaTeX usuels, aux formules multilignes et aux listes numérotées.
- [ ] Ajouter des métadonnées de capacités, contexte et coût uniquement depuis des sources fiables par fournisseur ; filtrer les niveaux de réflexion pris en charge.
- [x] Ajouter les QCM sélectionnables du Studio IA, le raisonnement et la confiance avant validation, avec correction révélée ensuite.
- [x] Distinguer les QCM de prédiction et de vérification de compréhension, en conservant les anciens QCM.
- [ ] Revoir l'ergonomie et la direction visuelle du Studio IA après stabilisation des réglages et du rendu.

Validation du 1er novembre 2026 : 231 tests Desktop réussis, compilation Kotlin Desktop réussie, captures de réponse scientifique à 360 et 1000 pixels inspectées. La distribution locale n'a pas été créée : WiX et la dépendance de vérification du runtime manquent dans le cache Gradle hors ligne.

- [ ] Plan structuré et validation des prérequis.
  - [x] Conclusion de diagnostic structurée avec acquis, limites, confusions et sources de QCM répondus.
  - [x] Édition, acceptation et sauvegarde locale d’un plan avec sources et preuves attendues ; plan accepté transmis au tuteur.
  - [x] Proposition structurée issue du diagnostic, sourcée et soumise à l'acceptation de l'apprenant.
  - [ ] Validation effective des prérequis avant chaque étape.
- [x] Graphe de connaissances avec sources, acceptation explicite et mécanisme de correction.
- [ ] Pratique adaptative basée sur les raisonnements observés.
  - [x] Problème autonome à solution engagée avant présentation, indices gradués, réponse et raisonnement enregistrés, verdict et reprise.
  - [ ] Choix du problème suivant et des reprises fondé sur des preuves de maîtrise vérifiées.
- [ ] Intégration documentée des cours et documents choisis.
- [ ] Recherche, démonstrations et dictée locale.

## Validation de la première tranche — 27 octobre 2026

- Suite desktop complète : 209 tests réussis, aucun échec/erreur/ignoré.
- Après correction de la création compacte : 6 tests UI réussis et distribution reconstruite (210 cas uniques couverts avec la suite précédente).
- Captures natives des conversations et écrans complets à 360×480 et 1000×800 inspectées.
- Contrôle du paquet Windows réussi, module Java HTTP inclus, sans requête réseau ni accès à la progression de l'utilisateur.
- Protocole testé sur serveur local; aucune réponse de modèle externe réel revendiquée.
- Publication de code uniquement; aucun nouvel installateur publié par cette tranche.
