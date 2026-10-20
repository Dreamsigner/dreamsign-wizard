# Duolingo KMP Learning App

Application d’apprentissage en Kotlin Multiplatform et Compose Multiplatform pour Windows, Android et iOS. L’interface reprend un parcours ludique et utilise la charte du cabinet : rouge signature `#E3262E`, rouge d’action `#C91E28`, encre `#141414`, graphite `#4A4A4A`, ivoire `#F7F5F0` et béton `#D9D7D2`. Le logo du cabinet n’est pas utilisé dans cette application.

## Parcours et contenus

Le cours **MIT 18.01SC — Calcul à une variable, Fall 2010** contient les **102 séances sources et 236 exercices** : 93 cours, 4 révisions et 5 entraînements adaptés aux examens. Chaque séance dispose de notes originales en français, d’exercices corrigés, d’indices et de liens précis vers les documents MIT consultés. Cette pratique guidée ne remplace pas tous les problèmes ouverts du cours d’origine.

Le cours **The Missing Semester, édition 2026**, contient les neuf conférences et 51 exercices sur les outils du développeur. Le **TCF Canada** dispose de trois ateliers originaux d’expression écrite, fondés sur ses trois formats FEI : saisie réelle, brouillon conservé localement, exemple et grille de relecture. L’auto-évaluation ne fournit aucune correction automatique de langue ni aucun score officiel. Les autres cours OSSU et les autres compétences TCF restent à compléter : voir `docs/curriculum/STATUS.md`.

**MIT 18.06SC — Algèbre linéaire, Fall 2011** ajoute 40 séances et 131 exercices : 32 cours, quatre révisions et quatre entraînements adaptés aux examens. Le catalogue actuel compte donc 154 leçons/ateliers et 421 exercices ; sa couverture de l’ensemble OSSU/TCF reste partielle.

Les cours rédigés sont dans `curriculum/courses/`. `python tools/validate_curriculum.py --generate` vérifie leur correspondance intégrale au manifeste source puis génère le code Kotlin dans `data/generated/`. Une unité correspond à un cours ; chaque séance est une leçon. L’atelier TCF est explicitement une séquence de préparation originale, pas un programme officiel. Les identifiants publiés restent stables pour conserver la progression. `SampleData.kt` fournit les catégories et les anciens exemples des catégories sans cours intégré.

L’application n’utilise pas de génération IA et ne prétend pas analyser des PDF ou vidéos. Le contenu pédagogique est préparé à partir des ressources de cours citées. Les ressources MIT/OSSU peuvent avoir leurs propres conditions de licence; les liens mènent au matériel d’origine au lieu de le recopier.

## Ressources de référence

- [OSSU Computer Science](https://github.com/ossu/computer-science)
- [OSSU Math](https://github.com/ossu/math)
- [MIT OpenCourseWare — Mathematics for Computer Science](https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-spring-2015/)
- [MIT OpenCourseWare — Linear Algebra](https://ocw.mit.edu/courses/18-06-linear-algebra-spring-2010/)
- [MIT OpenCourseWare — Probability and Statistics](https://ocw.mit.edu/courses/18-05-introduction-to-probability-and-statistics-spring-2022/)

## Lancer l’application Windows

Depuis Android Studio ou IntelliJ IDEA, ouvrir le dossier du projet puis lancer la tâche Gradle `composeApp > Tasks > compose desktop > desktopRun`.

Le dépôt ne contient pas de wrapper Gradle. Si Gradle est installé localement, lancer depuis la racine :

```powershell
gradle :composeApp:desktopRun
```

La progression est enregistrée localement : fichier atomique dans `%LOCALAPPDATA%/AtelierApprentissage/progress.json` sur Windows, SharedPreferences sur Android, NSUserDefaults sur iOS. Les XP sont attribués une seule fois par leçon. Une sauvegarde illisible est préservée et signalée ; les erreurs d’écriture sont visibles. La synchronisation entre appareils reste absente. Voir `docs/platforms.md` pour les builds et les limites de signature iOS.

## Cibles

- Windows Desktop : `composeApp/src/desktopMain/kotlin/main.kt`
- Android : `composeApp/src/androidMain/kotlin/com/duolingo/app/MainActivity.kt`
- UI et logique partagées : `composeApp/src/commonMain/kotlin/com/duolingo/app`
