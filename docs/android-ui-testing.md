# Tests Compose instrumentés Android

## État du lot

`LearningAndroidUiTest.kt` est rédigé ; **aucune compilation ni exécution Android n’est revendiquée par ce lot**. Le parent applique les dépendances et exécute la CI. Les tests desktop antérieurs et ces tests Android sont deux preuves distinctes.

Trois tests instrumentés montent les vrais `DuolingoApp` et `LessonQuizScreen`, avec `LearningAppTheme`, dans l’activité Android hôte de Compose :

1. Notes avant la question, validation désactivée sans choix, mauvaise réponse, absence d’avance, retry, correction, fin et XP persistés dans le stockage mémoire.
2. Navigation cours/profil, ouverture des notes et de l’indice, retour à la question avec choix conservé et aucune récompense indue.
3. Présence des contrôles dans le viewport Android et capture native en mémoire, sans fichier screenshot ni entrée pilotée par image.

Les données sont un catalogue original d’une seule leçon et un `ProgressStore` contenant uniquement une chaîne JSON volatile. Le test n’appelle ni `App()` ni `MainActivity` et n’initialise pas les SharedPreferences. Le provider `PracticeMedia` factice échoue avant tout accès accidentel à l’audio, aux enregistrements ou au microphone. Le gestionnaire d’URI factice refuse les ouvertures externes. Aucune permission physique n’est demandée, aucune donnée utilisateur n’est effacée et aucun `GrantPermissionRule`, `pm clear` ou orchestrateur avec effacement de données n’est ajouté.

## Versions et configuration à appliquer par le parent

Référence du projet : Kotlin 2.2.21, Compose Multiplatform **1.9.3**, AGP **8.10.1**, Gradle 8.11.1, JDK 17, compileSdk 35, minSdk 24. Compose Multiplatform 1.9.3 se fonde sur Android Compose UI **1.9.4** : utiliser cette version pour ses artefacts Android de test, sans prendre automatiquement la version actuelle d’une BOM.

Dans `composeApp/build.gradle.kts`, conserver le plugin Android application/KMP actuel, puis ajouter au bloc `kotlin.sourceSets` :

```kotlin
val androidInstrumentedTest by getting {
    dependencies {
        implementation("androidx.compose.ui:ui-test-junit4:1.9.4")
        implementation("androidx.test.ext:junit:1.2.1")
        implementation("androidx.test:runner:1.6.2")
    }
}
```

Le nom KMP est `androidInstrumentedTest` et le fichier est placé dans `src/androidInstrumentedTest/kotlin/`, pas dans un dossier `androidTest` supposé équivalent. Avec le layout Android KMP actuel, vérifier que le task de compilation instrumentée découvre réellement la classe.

Dans le `android.defaultConfig` existant :

```kotlin
testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
```

Au niveau module, hors du bloc KMP, ajouter l’activité hôte uniquement à Debug :

```kotlin
dependencies {
    add("debugImplementation", "androidx.compose.ui:ui-test-manifest:1.9.4")
}
```

Le modèle officiel est `@get:Rule val compose = createComposeRule()`. La règle lance une `androidx.activity.ComponentActivity` déclarée par `ui-test-manifest` ; il ne faut pas remplacer celle-ci par `createAndroidComposeRule<MainActivity>()`, qui lancerait l’initialisation réelle de l’application. Aucun fichier Manifest de test manuel ni dépendance Android instrumentée aux tests desktop n’est nécessaire pour ce lot.

Les versions AndroidX Test proposées sont stables et explicites. Le POM Google de `ui-test-junit4-android:1.9.4` a été lu directement : il publie notamment des minima antérieurs (`androidx.test:core:1.5.0`, `monitor:1.6.1`, `ext:junit:1.1.5`, JUnit 4.13.2) ; les versions proposées 1.2.1/1.6.2 ne sont donc pas prétendues celles littéralement imposées par ce POM. Le graphe effectivement résolu et la compilation restent à vérifier en CI.

## Commandes proposées

Construire les APK d’application et de test pour détecter les erreurs de configuration sans émulateur :

```sh
gradle :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest --stacktrace
```

Dans l’émulateur dédié déjà démarré par `android-emulator-runner` (API 34 compatible avec minSdk 24), exécuter :

```sh
gradle :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.duolingo.app.ui.LearningAndroidUiTest \
  --stacktrace
```

L’API de compilation 35 n’oblige pas à changer le runtime de l’émulateur API 34. Ne pas utiliser `testDebugUnitTest` comme preuve d’exécution instrumentée. Si plusieurs appareils sont présents, sélectionner explicitement le numéro de série réel de l’émulateur via l’environnement `ANDROID_SERIAL` ; ne pas lancer ce lot contre un appareil contenant des données personnelles.

Dans le step existant `reactivecircus/android-emulator-runner`, la propriété `script` peut contenir le smoke de lancement puis la commande Gradle ci-dessus. La validation pertinente exige un rapport avec **trois tests de cette classe effectivement exécutés**, pas seulement une tâche Gradle verte avec zéro test.

Conserver les rapports `composeApp/build/reports/androidTests/` et les XML `composeApp/build/outputs/androidTest-results/` dans les artefacts, même si l’instrumentation échoue. La capture en mémoire ne produit pas de PNG à archiver. Les captures desktop restent disponibles séparément.

## Sources officielles consultées le 20 octobre 2026

- [Configuration officielle Compose UI testing](https://developer.android.com/develop/ui/compose/testing#setup) : `ui-test-junit4`, `debugImplementation(ui-test-manifest)`, `createComposeRule()` et contenu sous thème.
- [Compose Multiplatform 1.9.3, dépendances Android](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.9.3) : UI 1.9.4.
- [Source versionnée de la règle Android côté Compose 1.9.3](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.9.3/compose/ui/ui-test-junit4/src/androidMain/kotlin/androidx/compose/ui/test/junit4/AndroidComposeTestRule.android.kt).
- [POM Google ui-test-junit4 Android 1.9.4](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-test-junit4-android/1.9.4/ui-test-junit4-android-1.9.4.pom).
- [POM Google ui-test-manifest 1.9.4](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-test-manifest/1.9.4/ui-test-manifest-1.9.4.pom).
- [Layout des sources Android KMP](https://kotlinlang.org/docs/multiplatform/multiplatform-android-layout.html).

Ces tests contrôlent la couche Compose sur Android avec données injectées ; ils ne vérifient pas les permissions microphone, les pilotes audio, SharedPreferences réelles, l’Activity de production ou le parcours de publication. Le smoke natif de lancement de l’application demeure une vérification complémentaire.
