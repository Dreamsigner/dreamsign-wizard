# Parité Studio IA — référence vérifiable

Référence fonctionnelle : LearnHarness `e69e85269c064c6c9c49a8b06ae78c06e79c4a63` (main public du 31 octobre 2026, version 0.4.1). Cette référence fige les critères de sortie ; les évolutions ultérieures de LearnHarness nécessitent un nouveau relevé. Dreamsign Wizard reste une application Kotlin Multiplatform / Compose, avec Windows comme cible de livraison actuelle.

| Parcours | Comportement de référence | État Dreamsign 2.2.0 | Vérification requise |
| --- | --- | --- | --- |
| Questions du diagnostic et du cours | QCM sélectionnable, choix multiple éventuel, « Je ne sais pas », raisonnement et confiance indépendants ; réponse engagée et explication révélée après soumission | Messages libres uniquement | Réponse choisie + raisonnement enregistrés et repris sans correction prématurée ; test compact et bureau |
| Diagnostic | Questions successives et conclusion explicite des prérequis avec preuves et incertitudes | Consigne de tutorat dans le prompt | État de diagnostic persistant ; aucune maîtrise déduite d'un seul message |
| Plan pédagogique | Carte de dépendances proposée, acceptée ou corrigée par l'apprenant, révisable en cours de route | Plan manuel éditable et accepté | Génération structurée validée, provenance des constats, reprise après redémarrage |
| Enseignement | Étape ciblée, prédiction avant explication, vérification après, progression fondée sur les réponses | Conversation guidée par un prompt | Questions associées à une étape ; preuves et corrections consultables |
| Pratique | Problème autonome, solution engagée cachée, indices gradués, verdict et nouvelle tentative adaptée | Exercices du catalogue hors Studio | Séance de pratique persistante, erreur et raisonnement conservés |
| Graphe de connaissances | Propositions et liens de raisonnement, source et séance, inspection et correction par l'apprenant | Absent | Révision d'une proposition modifie le contexte d'une séance suivante |
| Documents | Import volontaire de notes texte/PDF avec file, suivi et provenance | Lecteur PDF de cours sans import Studio | Sélection, limites et origine visibles ; aucune lecture globale des fichiers |
| Outils | Recherche sourcée, démonstrations/visuels, dictée et historique de séance | Absents du Studio | Fonctionnement dans la distribution Windows, permissions et sources visibles |
| Modèles | Fournisseurs configurés, capacités vérifiées, choix par rôle et séance | Choix API/ChatGPT par séance ; `/models` sans métadonnées fiables | N'afficher capacités/coûts que lorsqu'une source fournisseur les justifie ; couvrir les rôles réellement utilisés |

Ordre d'intégration : questions structurées ; diagnostic et plan ; cycle d'enseignement et pratique ; graphe et documents ; outils et réglages spécialisés ; revue de l'interface et validation de bout en bout. Chaque étape doit conserver les anciennes séances lisibles, séparer les secrets des données d'étude, avoir des tests déterministes et aboutir à une release Windows vérifiée selon `AGENTS.md`.

État prévu pour 2.3.0 : QCM avec choix, raisonnement, confiance et révélation après validation ; proposition de plan structurée et sourcée, conservée séparément du plan accepté. Les critères « diagnostic persistant », « validation des prérequis », « enseignement », « pratique adaptative », « graphe utilisable », « documents », « outils » et « rôles de modèles » restent ouverts. Le noyau du graphe et son stockage ne sont pas encore reliés au Studio.

Références de code LearnHarness : `src/core/question-core.ts`, `components/question-dock.tsx`, `src/agent/tools/probe.ts`, `src/agent/tools/plan.ts`, `src/core/plan-tree.ts`, `src/agent/tools/practice.ts`, `src/core/contracts/graph.ts`, `components/notes-import.tsx`, `src/core/contracts/models.ts`. Les composants et contrats servent de référence de comportement ; leur code n'est pas incorporé tel quel.
