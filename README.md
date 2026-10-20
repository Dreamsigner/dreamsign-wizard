<p align="center">
  <img src="docs/images/atelier-banner.svg" alt="Atelier d’apprentissage — des livres et des marches de blocs rouges" width="100%">
</p>

# Atelier d’apprentissage

Une application native **Windows**, en français, pour comprendre un cours, pratiquer et reprendre ses erreurs. Développée avec Kotlin et Compose Multiplatform dans le dépôt **Duolingo KMP Learning App**.

[![Build Windows](https://github.com/Dreamsigner/duolingo-kmp-learning-app/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/Dreamsigner/duolingo-kmp-learning-app/actions/workflows/build.yml)

[Tester sur Windows](docs/windows-testing.md) · [Lire les cours et exercices](docs/courses/README.md) · [Couverture vérifiable](docs/curriculum/AUTHORED-COVERAGE.md) · [Sources et supports](docs/curriculum/course-resources-notes.md)

## Apprendre, puis pratiquer

| Dans l’application | Fonctionnement |
| --- | --- |
| **Cours et supports** | Toutes les notes françaises d’un cours réunies dans un lecteur, avec sommaire et exercices associés. Les PDF officiels se lisent dans l’application avec pages et zoom. Les notes restent lisibles avant de débloquer les exercices. |
| **Questions et corrections** | Objectif au-dessus de l’énoncé, réponses juste après la question, indice et bouton « Pourquoi cette réponse ? ». |
| **Reprise des erreurs** | Une erreur permet d’avancer; les questions ratées reviennent en fin de séance jusqu’à leur correction. |
| **Mathématiques composées** | Rendu LaTeX local et reconnaissance prudente des matrices, fractions et puissances dans les champs mathématiques. Les formes non reconnues restent visibles sous leur forme source. |
| **Progression locale** | Cours choisi, leçons terminées, XP, série et brouillons conservés sur cet appareil. Les XP d’une leçon sont attribués une seule fois. |
| **Ateliers TCF** | Lecture, écoute de documents originaux, saisie et production orale avec réécoute et auto-évaluation; aucun score officiel ni correction automatique de langue. |

L’interface PC utilise une navigation latérale et un espace de lecture adapté à la largeur. La charte rouge, encre et ivoire accompagne des illustrations pixel/voxel originales; aucune texture ni aucun personnage Minecraft n’est inclus.

![Catalogue natif Windows, rendu Compose du véritable contenu publié](docs/images/windows-catalogue.png)

<details>
<summary>Voir un exercice d’algèbre linéaire avec matrice composée</summary>

![Exercice avec objectif au-dessus, matrice composée et réponses directement sous la question](docs/images/windows-mathematics.png)

Captures produites par les tests Compose hors écran avec le catalogue publié et une progression de test isolée.

</details>

## Catalogue actuel

**15 unités, 391 leçons et ateliers, 1 190 exercices.** Une unité correspond à un cours identifié; les séances suivent son ordre source. Le classement des unités suit les sections OSSU, avec les adaptations locales documentées. Les notes et exercices sont rédigés pour ce projet et appariés aux références de leur séance.

| Parcours | Cours publiés |
| --- | --- |
| **Mathématiques** | Calcul à une variable · Mathématiques discrètes · Algèbre linéaire · Logique formelle · Méthodes numériques |
| **Informatique** | Python · Outils du développeur · Architecture des ordinateurs · Prolog · Théorie du calcul · Théorie algorithmique des jeux |
| **TCF Canada** | Ateliers originaux de compréhension orale, compréhension écrite, expression orale et expression écrite |

Le cursus OSSU complet et la préparation TCF complète **restent à terminer**. Les adaptations guidées ne remplacent pas toutes les banques de problèmes, les projets et les examens ouverts des cours originaux. Un support PDF de séance n’est pas présenté comme un manuel complet. [Détails, provenance et limites](docs/curriculum/STATUS.md).

## Tester sans installer Gradle

1. Ouvrir un [run Windows réussi](https://github.com/Dreamsigner/duolingo-kmp-learning-app/actions/workflows/build.yml).
2. Télécharger **DuolingoKmpApp-Windows**, extraire les archives et conserver ensemble l’exécutable, `app` et `runtime`.
3. Lancer **DuolingoKmpApp.exe**. Java est inclus dans le paquet.

[Guide Windows et diagnostic sans fenêtre](docs/windows-testing.md). Les notes et documents audio sont intégrés; le lecteur PDF télécharge les supports officiels avec Internet et les vidéos restent des liens. La capture physique du microphone et la lecture d’écran native ne sont pas prouvées par les tests hors écran.

## Contribuer au projet

- [Architecture et plateformes](docs/platforms.md) — le périmètre actif est **Windows uniquement**; le code Android/iOS est conservé, leurs développements et builds sont arrêtés.
- [Tests automatisés Compose, audio et persistance](docs/automated-testing.md) — scénarios sans Computer Use, rapports et captures.
- [Rendu mathématique](docs/math-typesetting.md) · [Glossaire Prolog français/anglais](docs/curriculum/learn-prolog-now-fr-glossary-order-notes.md).
- [Registre de progression](docs/EXECUTION-LOG.md) — distingue implémentation, tests, inspection visuelle et vérifications manquantes.

Les contenus canoniques sont dans `curriculum/courses/`. `python tools/validate_curriculum.py --generate` valide les correspondances sources et génère Kotlin. Garder les identifiants publiés stables pour préserver la progression. Le dépôt n’a pas de wrapper Gradle : utiliser Gradle 8.11.1 et JDK 17 pour `gradle :composeApp:desktopTest :composeApp:createDistributable`.

La progression Windows est enregistrée dans `%LOCALAPPDATA%/AtelierApprentissage/progress.json`; une sauvegarde illisible est préservée et signalée. Aucune génération IA dans l’application, aucun import automatique de PDF, aucune synchronisation cloud.

## Références

[OSSU Computer Science](https://github.com/ossu/computer-science) · [OSSU Math](https://github.com/ossu/math) · [MIT OpenCourseWare](https://ocw.mit.edu/) · [France Éducation international — TCF Canada](https://www.france-education-international.fr/test/tcf-canada)

Les supports originaux restent sur leurs sites officiels. Leurs licences et exceptions ne sont pas étendues aux notes de ce projet. Les dépendances distribuées sont documentées dans [les notices tierces](docs/licences/THIRD-PARTY-NOTICES.txt).
