<p align="center">
  <img src="docs/images/atelier-banner.svg" alt="Atelier d’apprentissage" width="100%">
</p>

# Atelier d’apprentissage

Application d’apprentissage en français pour Windows, développée en Kotlin avec Compose Multiplatform. Elle réunit des cours de mathématiques et d’informatique ainsi que des ateliers de préparation au TCF Canada, avec exercices interactifs et suivi de progression.

## Nouvelle direction : Atelier IA

L'application évolue vers un apprentissage piloté par les objectifs personnels, inspiré du fonctionnement de [LearnHarness](https://github.com/amosblomqvist/learnharness). La première tranche ajoute un tuteur conversationnel en français et des séances persistantes, en conservant Kotlin/Compose et les cours existants.

Dans **Atelier IA**, créer un objectif, décrire ses connaissances et son raisonnement, puis échanger avec le tuteur. Retrouver une séance permet de reprendre son historique. **Paramètres → Connexion IA**, ou le raccourci **Connexion IA** de l'atelier, configure l'URL de base compatible Chat Completions et le nom du modèle. La clé reste en mémoire jusqu'à la fermeture; elle n'est pas enregistrée avec les séances.

Les échanges sont stockés localement dans le dossier de données Windows de l'application, séparément de la progression des cours. Lors d'un envoi, l'objectif et un contexte borné de la séance sont transmis au fournisseur configuré. Un serveur HTTP local peut fonctionner sans clé; les fournisseurs distants nécessitent HTTPS et une clé. Les tests utilisent un serveur local et des réponses déterministes, sans appeler un fournisseur payant.

Cette tranche ne comprend pas encore de graphe de connaissances ni de plan pédagogique structuré et validé : le diagnostic et le plan sont conversationnels. La [conception du pivot](docs/product/2026-10-26-learning-studio.md) et le [plan de réalisation](tasks/plan.md) précisent les étapes suivantes. Le nouveau parcours nécessite une compilation de cette révision; ce changement ne publie pas d'installateur.

[![Build Windows](https://github.com/Dreamsigner/duolingo-kmp-learning-app/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/Dreamsigner/duolingo-kmp-learning-app/actions/workflows/build.yml)

[Installation](#installation) · [Développement](#développement) · [Catalogue des cours](docs/courses/README.md) · [Documentation](#documentation)

## Fonctionnalités

- Lecture des notes de cours avec sommaire, ressources de référence et exercices associés.
- Consultation des supports PDF dans l’application, avec défilement vertical continu et zoom.
- Exercices avec indices, corrections expliquées et reprise des questions incorrectes en fin de séance.
- Partage d’un défi : copier la question et ses choix, ou enregistrer une carte PNG sur Windows, sans dévoiler la correction.
- Affichage des expressions mathématiques avec un moteur de rendu LaTeX local.
- Ateliers TCF de compréhension écrite et orale, rédaction et production orale avec réécoute et auto-évaluation.
- Sauvegarde locale de la progression, des XP, du cours sélectionné et des brouillons.
- Interface de bureau avec navigation latérale et disposition adaptée au redimensionnement.
- Espace de cours avec progression, prochaine leçon, recherche et filtres de disponibilité.
- Transitions douces, survols et appuis animés, avec une identité visuelle rouge, encre et papier.
- Sommaire latéral permanent dans le lecteur sur grand écran, et ouverture en fenêtre superposée sur petit écran.

![Catalogue des cours sur Windows](docs/images/windows-catalogue.png)

<details>
<summary>Exemple d’exercice de mathématiques</summary>

![Exercice d’algèbre linéaire](docs/images/windows-mathematics.png)

</details>

## Catalogue pédagogique

Le catalogue comprend **15 unités, 391 leçons et ateliers et 1 190 exercices**.

| Catégorie | Domaines |
| --- | --- |
| Mathématiques | Calcul à une variable, mathématiques discrètes, algèbre linéaire, logique formelle et méthodes numériques |
| Informatique | Python, outils du développeur, architecture des ordinateurs, Prolog, théorie du calcul et théorie algorithmique des jeux |
| TCF Canada | Compréhension orale et écrite, expression orale et écrite |

Les cours s’appuient notamment sur OSSU, MIT OpenCourseWare et les références de France Éducation international. Chaque unité identifie son cours source; les notes et exercices sont des adaptations rédigées pour l’application.

La couverture du cursus OSSU et de la préparation TCF est partielle. Les exercices adaptés ne remplacent pas l’ensemble des devoirs, projets et évaluations des cours sources. Les ateliers TCF ne délivrent pas de score officiel ni de correction automatique des productions.

Consulter le [catalogue détaillé](docs/courses/README.md) et la [couverture pédagogique](docs/curriculum/AUTHORED-COVERAGE.md).

## Installation

1. Ouvrir la [release Windows 1.12.0](https://github.com/Dreamsigner/duolingo-kmp-learning-app/releases/tag/v1.12.0).
2. Télécharger l’installateur **AtelierApprentissage-1.12.0-Setup.exe**.
3. Lancer **AtelierApprentissage-1.12.0-Setup.exe** et suivre l’assistant. L’installation est propre à l’utilisateur Windows, avec raccourcis sur le Bureau et dans le menu Démarrer.

Java est inclus : aucun JDK ni Gradle n’est nécessaire. La désinstallation se fait depuis les applications installées des paramètres Windows. L’installateur n’est pas signé numériquement; Windows peut afficher un avertissement d’éditeur inconnu. `SHA256SUMS.txt` accompagne l’exécutable pour vérifier l’intégrité du téléchargement.

L’artefact **DuolingoKmpApp-Windows** reste disponible pour une utilisation portable : extraire le ZIP et lancer `DuolingoKmpApp.exe`, avec les dossiers `app` et `runtime` à ses côtés.

Dans **Cours**, le panneau du cours réunit sa progression et l’action **Continuer**. La loupe ouvre une recherche par titre ou objectif. Les filtres **Toutes**, **Disponibles** et **Terminées** permettent de retrouver une séance, sans modifier son ordre ni ses prérequis. **Réinitialiser** efface les filtres; changer de cours réinitialise la recherche.

Dans le sommaire des cours de mathématiques, une recherche par titre ou objectif permet aussi d’atteindre une leçon.

Le bouton **Ajouter aux favoris** marque une leçon de mathématiques à revoir. Dans **Sommaire**, activer **Favoris uniquement** pour retrouver les leçons marquées du cours; la recherche reste disponible. Les favoris sont enregistrés localement avec la progression et peuvent être retirés depuis la leçon.

Dans les supports de mathématiques, les vidéos disposent de cartes avec titre, vignette YouTube lorsqu’elle est disponible et ouverture directe dans le navigateur. Les fichiers MP4 conservent une carte sans vignette. Le bouton **Lire dans l’app** ouvre le lecteur source dans une fenêtre adaptative : YouTube conserve ses commandes officielles et les MP4 utilisent celles du moteur vidéo Edge WebView2. En plein écran, l’en-tête de l’application disparaît; **Échap** restaure la fenêtre précédente. Vitesse, sous-titres et qualité dépendent de la source. Le runtime Microsoft Edge WebView2 doit être présent; le bouton navigateur reste disponible.

La lecture des vidéos YouTube et MP4 reprend à la dernière position enregistrée, même après redémarrage de l’application. Les positions sont propres à chaque vidéo; une vidéo arrivée à sa fin repart du début. La reprise ne lance pas volontairement la lecture automatique. Les commandes officielles permettent toujours de changer de position.

Le bouton **Télécharger avec la piste audio française**, sous une vidéo YouTube, recherche une piste française déjà publiée par YouTube, y compris un doublage automatique. Si une piste est accessible, choisir le fichier MP4 de destination. L’application télécharge la vidéo et cette piste, les assemble, puis vérifie le fichier final. Elle ne remplace jamais le français par de l’anglais. Une piste inaccessible et un échec de vérification sont signalés séparément.

Le téléchargement requiert **yt-dlp**, **FFmpeg** (avec `ffprobe`) et un moteur JavaScript pris en charge par yt-dlp, par exemple **Deno**, installés sur le poste. Ces outils ne sont pas inclus dans l’installateur et l’application ne les installe pas automatiquement. Elle détecte `yt-dlp`, `ffmpeg` et `ffprobe` dans `PATH`; voir le guide Windows pour des chemins explicites.

Une piste française doit être réellement accessible sur la vidéo choisie. L’absence de cette piste est distinguée d’un échec réseau, d’un blocage YouTube ou d’un moteur JavaScript manquant. L’application signale aussi les avertissements d’obsolescence de yt-dlp, sans masquer l’erreur principale. Elle ne met pas ces outils à jour automatiquement et n’importe pas les cookies du navigateur.

Les notes et documents audio sont intégrés; les supports PDF externes et les vidéos nécessitent une connexion Internet.

La section **Paramètres**, sous **Cours** et **Profil** dans la navigation, regroupe la version, les mises à jour et les informations de sauvegarde locale. Une pastille chiffrée sur son icône signale les actions en attente : mise à jour prête à installer ou erreur à traiter. Elle disparaît lorsque l’action n’est plus nécessaire; un téléchargement en cours ne crée pas d’alerte supplémentaire.

La version installée à l’emplacement Windows standard vérifie automatiquement les releases GitHub stables. Les mises à jour sont téléchargées et vérifiées, puis appliquées à la fermeture de l’application. Dans **Paramètres**, le réglage **Mises à jour auto** permet de désactiver ce comportement; **Installer et redémarrer** applique immédiatement une version déjà prête. Les versions portables et installations dans un autre dossier conservent une mise à jour manuelle.

Une mise à niveau remplace la version installée et préserve les données personnelles. Le cache de mise à jour nettoie les anciens installateurs devenus inutiles et les fichiers temporaires abandonnés depuis plus de 24 heures. L’installateur vérifié en attente reste disponible pour réessayer un échec; après réussite, sa copie en cache et le fichier de lancement temporaire sont supprimés. Les préférences et le dernier diagnostic sont conservés. Les installateurs téléchargés manuellement et le cache de maintenance géré par Windows restent distincts de ce nettoyage.

La progression est enregistrée dans `%LOCALAPPDATA%/AtelierApprentissage/progress.json`. Elle n’est pas synchronisée entre appareils.

Voir le [guide Windows](docs/windows-testing.md) pour le diagnostic et la gestion des données locales.

## Développement

### Prérequis

- Windows et JDK 17.
- Gradle 8.11.1, disponible dans le `PATH`. Le dépôt ne contient pas de wrapper Gradle.
- Python 3.12 pour les outils de contenu pédagogique; SymPy 1.12 pour les contrôleurs mathématiques.

Versions du projet : Kotlin **2.2.21** et Compose Multiplatform **1.9.3**. Les dépendances sont déclarées dans `gradle/libs.versions.toml` et `composeApp/build.gradle.kts`.

### Commandes

À exécuter depuis la racine du dépôt :

| Commande | Usage |
| --- | --- |
| `gradle :composeApp:compileKotlinDesktop` | Compiler l’application Windows |
| `gradle :composeApp:desktopTest` | Exécuter les tests Kotlin et Compose desktop |
| `gradle :composeApp:createDistributable` | Construire l’application portable avec son runtime |
| `gradle :composeApp:packageExe` | Construire l’installateur Windows par utilisateur |
| `gradle :composeApp:sourcePdfSmokeTest` | Vérifier le lecteur avec un PDF officiel; nécessite Internet |
| `python tools/validate_curriculum.py --generate` | Valider le catalogue et régénérer les données Kotlin |
| `python tools/validate_course_resources.py` | Vérifier l’index des supports pédagogiques |
| `python tools/export_course_notes.py --check` | Vérifier les exports Markdown des cours |

Après construction, le lanceur se trouve dans `composeApp/build/compose/binaries/main/app/DuolingoKmpApp/`.

### Structure du projet

| Dossier | Rôle |
| --- | --- |
| `composeApp/src/commonMain/` | Interface Compose, modèles et logique d’apprentissage partagés |
| `composeApp/src/desktopMain/` | Lanceur Windows, stockage local, audio et lecteurs de documents |
| `composeApp/src/commonTest/`, `composeApp/src/desktopTest/` | Tests de logique, de persistance et d’interface |
| `curriculum/courses/` | Contenus pédagogiques canoniques au format JSON |
| `tools/` | Validation, contrôles pédagogiques et génération des contenus |
| `docs/courses/` | Notes de cours et exercices consultables sur GitHub |
| `.github/workflows/` | Vérifications et création du paquet Windows |

Windows est la plateforme actuellement distribuée. Les cibles Android et iOS restent présentes dans le code Kotlin Multiplatform, sans distribution maintenue.

### Contributions

Pour modifier le contenu pédagogique, travailler dans `curriculum/courses/`, conserver les identifiants publiés et préciser les sources. Régénérer les fichiers dérivés avec les outils du dépôt plutôt que de modifier directement le catalogue Kotlin généré.

Pour une modification applicative, ajouter ou adapter les tests concernés et décrire son comportement dans la pull request. Le workflow `Build` vérifie les contenus, exécute les tests desktop et construit le paquet Windows.

## Documentation

- [Utilisation et diagnostic Windows](docs/windows-testing.md)
- [Tests automatisés](docs/automated-testing.md)
- [Bibliothèque des cours](docs/courses/README.md)
- [Couverture pédagogique](docs/curriculum/AUTHORED-COVERAGE.md)
- [Sources et supports](docs/curriculum/course-resources-notes.md)
- [Rendu mathématique](docs/math-typesetting.md)

## Références et licences tierces

[OSSU Computer Science](https://github.com/ossu/computer-science) · [MIT OpenCourseWare](https://ocw.mit.edu/) · [France Éducation international — TCF Canada](https://www.france-education-international.fr/test/tcf-canada)

Les supports externes restent soumis aux licences de leurs éditeurs. Les licences et attributions des bibliothèques distribuées sont regroupées dans [`docs/licences/`](docs/licences/).
