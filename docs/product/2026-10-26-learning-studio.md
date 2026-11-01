# Nouvelle direction : un Dreamsign Wizard personnel

Décision utilisateur du 26 octobre 2026 : faire évoluer l'application actuelle, en conservant Kotlin Multiplatform / Compose. Référence produit : [LearnHarness](https://github.com/amosblomqvist/learnharness), consultée le 26 octobre 2026.

## Intention

Partir de ce que la personne veut comprendre, découvrir ses prérequis, construire un parcours puis enseigner par étapes. L'élève explique son raisonnement et ses incertitudes; l'assistant adapte ses questions. À terme, les connaissances et leurs liens sont réutilisés entre séances. Un catalogue de cours reste une ressource, et ne définit plus à lui seul le produit.

LearnHarness décrit une séquence diagnostic, plan, enseignement, pratique, et un graphe persistant de propositions et de raisonnements. Son [architecture](https://github.com/amosblomqvist/learnharness/blob/main/docs/architecture.md) sépare domaine, agents, stockage et présentation. Notre implémentation reprend ces responsabilités avec les technologies du projet; le code de ce dépôt externe n'est pas intégré.

## Première version utilisable

- Entrée **Studio IA** : créer un objectif libre, ouvrir une séance, retrouver ses séances.
- Conversation avec un modèle réellement configuré par l'utilisateur; aucune réponse simulée présentée comme une réponse IA.
- Instructions de tutorat en français : diagnostic préalable, raisonnement explicite, prérequis et plan, puis explications et vérifications progressives. La première version exprime ce plan dans la conversation; il n'est pas encore un arbre de dépendances validé.
- Connexion compatible Chat Completions : URL de base, nom du modèle, clé facultative pour un serveur local sans authentification. Choix du fournisseur explicite, pas de modèle imposé.
- Séances et préférences non secrètes enregistrées localement, séparément de la progression des cours. Reprise après redémarrage; la clé reste seulement en mémoire et doit être ressaisie.
- L'objectif et le contexte de la séance sont envoyés au fournisseur uniquement après une action d'envoi. Ni le catalogue entier, ni toutes les anciennes séances ne sont transmis automatiquement.
- Échec réseau, réponse invalide, annulation ou erreur de sauvegarde visibles; pas de faux succès ni d'écrasement silencieux d'un fichier illisible.
- Cours, exercices existants, médias et progression conservés. Windows reste la distribution maintenue.

## Limites assumées de cette première tranche

Un tuteur conversationnel guidé par des instructions n'est pas encore un moteur agentique complet. Le graphe de connaissances, l'extraction auditée des propositions, les exercices adaptatifs structurés, la recherche documentaire et les démonstrations interactives sont des étapes suivantes. Aucune note de maîtrise ou connaissance acquise n'est inventée à partir d'un simple échange.

État au 1er novembre 2026 : le graphe persistant, l'extraction sourcée soumise à acceptation et la correction manuelle sont disponibles sur Windows. La validation des prérequis et l'adaptation des exercices à une maîtrise vérifiée restent à construire.

Le protocole est testé avec un serveur local déterministe. Sans identifiants et choix de modèle fournis pour un essai réel, les tests ne prouvent ni la compatibilité de tous les fournisseurs, ni la qualité pédagogique de leurs réponses. Les changements de modèle, clés et endpoint de cette app sont distincts de la configuration des agents de développement Codex.

## Organisation cible

```text
Interface Studio IA -> contrôleur de séances -> port de tutorat
                              |                      |
                              v                      v
                     stockage local séparé    adaptateur HTTP Windows
                                                     |
                                                     v
                                            fournisseur choisi
```

Les dépendances externes restent derrière des interfaces injectables. Les cours existants ne sont pas migrés dans un nouveau format. La configuration des appels IA ne se mélange pas à la sauvegarde de progression.

Le produit porte désormais le nom **Dreamsign Wizard**. Voir aussi les [pistes issues des PR LearnHarness](2026-10-30-learnharness-pr-features.md).

## Suite du produit

1. Plan structuré et vérifiable : objectifs, prérequis, étapes et preuves de compréhension.
2. Mémoire explicite : propositions, relations et liens vers les échanges sources; correction et inspection par l'utilisateur.
3. Pratique adaptative : exercices autonomes, indices gradués, reprise des difficultés, sans annoncer une compétence sur une seule bonne réponse.
4. Ressources : exploiter les cours existants et les documents choisis avec provenance visible.
5. Outils spécialisés : recherche et visuels; dictée locale selon les règles de l'environnement.

Chaque étape doit livrer un parcours complet vérifiable avant d'élargir le nombre d'agents ou de fournisseurs.
