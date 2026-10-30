# Atelier IA : trajectoire vers LearnHarness

État examiné le 30 octobre 2026. La release Windows 1.12.0 fournit des objectifs, des séances persistantes et un dialogue avec une API compatible Chat Completions. Elle ne fournit pas la connexion par abonnement ChatGPT, le plan pédagogique structuré, la pratique adaptative ni le graphe de connaissances. Cette liste est un suivi de livraison, pas une déclaration de parité.

## 0. Accès au tuteur depuis la release

- Proposer deux modes distincts : **Continuer avec ChatGPT** via l'autorisation officielle pour applications open source, et **autre fournisseur avec clé API**. Ne jamais réutiliser les identifiants Codex ou convertir un abonnement en clé API.
- Vérifier l'identité signée, le consentement au forfait, le modèle réellement disponible, le renouvellement des jetons, la déconnexion et la fin complète des réponses en flux. Conserver les jetons dans un fichier local protégé et séparé des séances ; rendre leur usage visible et donner accès aux limites ChatGPT.
- Vérification de sortie : connexion dans le navigateur, sélection d'un modèle, première réponse, reprise après redémarrage, erreur de limite compréhensible, déconnexion locale et distante. Les tests déterministes ne nécessitent aucun compte ; un essai manuel avec un compte volontaire reste indispensable avant publication.

## 1. Parcours pédagogique vérifiable

- Le tuteur commence par l'objectif et un diagnostic des prérequis. Il enregistre les questions, réponses et incertitudes de l'élève, sans inférer une maîtrise à partir d'un seul message.
- Il produit un plan **structuré** de quelques étapes avec liens de prérequis, preuves attendues et sources de diagnostic. L'élève peut l'accepter, le corriger ou demander un autre diagnostic avant l'enseignement.
- Vérification de sortie : rouvrir la séance retrouve le plan et sa décision ; changer le plan ne détruit pas les échanges ; une étape ne devient pas acquise sans preuve consultable.

## 2. Enseignement et pratique

- Enseigner une étape à la fois à partir des connaissances constatées ; faire formuler une prédiction, examiner le raisonnement, corriger les confusions puis vérifier la compréhension.
- Ouvrir des séances de pratique distinctes avec des problèmes autonomes, indices gradués et variantes selon les erreurs observées. Chaque énoncé comprend toutes les données nécessaires pour le résoudre.
- Vérification de sortie : un parcours de test couvre un apprenant débutant, une réponse partiellement juste, une erreur persistante et une reprise après fermeture.

## 3. Mémoire entre séances

- Enregistrer des propositions et leurs liens de raisonnement avec référence à la séance et au passage source. Permettre inspection, correction et suppression par l'élève.
- Réutiliser uniquement les éléments validés ou explicitement signaler les hypothèses quand une nouvelle séance démarre.
- Vérification de sortie : une correction dans le graphe change le contexte des séances suivantes ; aucune proposition sans provenance n'est présentée comme certaine.

## 4. Ressources et outils

- Ajouter l'envoi volontaire de cours, PDF et images compatibles avec le modèle, avec limites de taille, aperçu et indication des données transmises.
- Ajouter ensuite recherche sourcée, démonstrations interactives et dictée locale, chacune avec un contrôle de fonctionnement dans la distribution Windows. Examiner Android et iOS par adaptateur avant de promettre ces capacités hors Windows.

## Décision technique actuelle

Conserver Kotlin Multiplatform / Compose pour ces étapes. Le langage ne détermine ni la qualité du tutorat ni l'accès à l'abonnement ; la lacune est dans les fonctions et adaptateurs manquants. Réévaluer la pile après une tranche complète, à partir du coût mesuré du développement Windows et de la portée réellement voulue pour Android/iOS.

Sources de référence : [fonctionnement LearnHarness](https://github.com/amosblomqvist/learnharness/blob/main/README.md), [connexion open source ChatGPT](https://developers.openai.com/siwc/token-sharing-open-source), [contrat Responses du forfait](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference), [compatibilité Compose Multiplatform](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html).
