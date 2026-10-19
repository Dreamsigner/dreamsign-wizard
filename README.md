# Duolingo KMP Learning App

Application d’apprentissage en Kotlin Multiplatform et Compose Multiplatform pour Windows, Android et iOS. L’interface reprend un parcours ludique, des objectifs courts, des XP, des cœurs et le vert déjà utilisé dans le projet.

## Parcours et contenus

Le catalogue jouable contient actuellement 8 unités de mathématiques, 8 unités d’informatique et une unité de français. Les parcours s’inspirent de l’organisation OSSU (raisonnement, calcul, maths discrètes, algèbre linéaire, probabilités, systèmes, algorithmes, sécurité, bases de données et éthique). Les explications et exercices de l’application sont rédigés pour ce projet; les pages de référence et cours MIT OpenCourseWare sont liés dans l’aide de chaque leçon.

Les catalogues sont définis dans `composeApp/src/commonMain/kotlin/com/duolingo/app/data/SampleData.kt`. Une leçon contient un objectif, une note de cours, un ou plusieurs exercices avec correction et indice, des ressources de référence et des identifiants de prérequis. Pour ajouter une leçon, attribuer un identifiant stable unique et ajouter son contenu à une unité; l’écran d’aide et le parcours l’affichent automatiquement. Ne pas renommer les identifiants déjà publiés : la progression s’y rattache. `curriculumVersion` et `contentVersion` documentent les évolutions de structure et de contenu.

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

La progression est actuellement gardée en mémoire pendant la session. La persistance locale et la synchronisation entre appareils ne sont pas encore implémentées.

## Cibles

- Windows Desktop : `composeApp/src/desktopMain/kotlin/main.kt`
- Android : `composeApp/src/androidMain/kotlin/com/duolingo/app/MainActivity.kt`
- UI et logique partagées : `composeApp/src/commonMain/kotlin/com/duolingo/app`
