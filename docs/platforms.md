# Construire et tester les applications

Les trois applications utilisent les mêmes écrans Compose et la même logique Kotlin.
Android démarre dans `MainActivity`, Windows dans `main.kt`, iOS dans une application
SwiftUI qui appelle `MainViewController()` via le framework statique `ComposeApp`.

## Versions configurées

Kotlin **2.2.21**, Compose Multiplatform **1.9.3**, AGP **8.10.1**, Java **17** et Gradle **8.11.1**.
Les plugins Kotlin, Compose Compiler et Serialization suivent la même version Kotlin.
Le dépôt ne contient pas de wrapper Gradle : utiliser Gradle 8.11.1,
ou `gradle/actions/setup-gradle`, comme dans le workflow.
La [matrice officielle Kotlin](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html#version-compatibility)
et les [propriétés de Kotlin/Native 2.2.21](https://github.com/JetBrains/kotlin/blob/v2.2.21/kotlin-native/konan/konan.properties)
justifient **Xcode 26.0.1**. Voir [le dossier de migration](platform-migration.md) pour les contraintes,
les sources et la séparation entre résultats de l'ancienne chaîne et validation à venir.
**Cette nouvelle configuration attend son premier build CI.** Le run de référence `c3439cc`
a réussi Windows, Android, les tests Kotlin/XCTest iOS et l'archive avec les versions antérieures.

## Windows

```powershell
gradle :composeApp:desktopTest :composeApp:createDistributable
```

La CI produit `DuolingoKmpApp-Windows.zip`. Extraire tout le ZIP et lancer
`DuolingoKmpApp.exe`. Le dossier inclut le runtime Java ; conserver ses fichiers.
Ce ZIP est une application portable et ne contient pas d'installateur MSI/EXE.
Les formats d'installateur restent configurés dans Gradle pour une distribution future.

## Android

Installer Android SDK 35 et Build Tools 35.0.0 ; définir `ANDROID_HOME` ou `sdk.dir`
dans le fichier local non versionné `local.properties`.

```sh
gradle :composeApp:lintDebug :composeApp:assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

Le workflow fournit un APK de débogage signé avec la clé de débogage du runner.
Il est installable pour les essais, pas destiné au Play Store. La CI lance aussi
l'APK dans un émulateur API 34, recherche la navigation affichée et conserve
la capture de l'accueil, la hiérarchie UI et les logs Android.

## iPhone et iPad

Sur un Mac Apple Silicon avec Xcode **26.0.1** et Gradle 8.11.1 sur le PATH, ouvrir
`iosApp/iosApp.xcodeproj`, sélectionner le scheme `iosApp` et un simulateur disponible.
Le projet Xcode est versionné ; aucun générateur Xcode supplémentaire n'est requis.
La phase Xcode exécute `:composeApp:embedAndSignAppleFrameworkForXcode` suivant
l'[intégration directe officielle](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html).

```sh
export DEVELOPER_DIR=/Applications/Xcode_26.0.1.app/Contents/Developer
gradle :composeApp:iosSimulatorArm64Test
bash tools/smoke-ios.sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Release -destination 'generic/platform=iOS' \
  -archivePath build/ios/DuolingoKmpApp-unsigned.xcarchive \
  CODE_SIGNING_ALLOWED=NO archive
```

La CI sélectionne `macos-15` ARM64 et vérifie `uname -m`. Le script lit les runtimes
et appareils réellement présents ; par défaut, il demande une version iOS 26.0 ou 26.0.x
(`IOS_SIMULATOR_RUNTIME_VERSION_PREFIX=26.0`), puis choisit un iPhone et un iPad existants.
Il échoue si ces ressources manquent et ne télécharge aucun runtime.
Les inventaires JSON et `selected-simulators.json` donnent les noms/identifiants réels ;
les fichiers de version Xcode/SDK, les résultats `.xcresult` et les PNG sont archivés.
Le ZIP simulateur produit sur ce runner contient une application ARM64 pour Mac Apple Silicon.
La cible Kotlin `iosX64` reste disponible pour les hôtes Intel ; y lancer `iosX64Test`
et sélectionner explicitement un runner Intel compatible.

Compose 1.9.3 synchronise l'accessibilité à la demande ; l'ancienne API
`AccessibilitySyncOptions` a été retirée depuis 1.8. Le drapeau historique
`COMPOSE_UI_TESTING` des XCTest n'active plus cette API. Les assertions et captures
du prochain run doivent vérifier l'exposition réelle des contrôles avec la nouvelle version.
Le minimum iOS du projet reste 14.0 ; contrôler également les contraintes et architectures
des binaires résolus. Un SDK 26 ne signifie pas un minimum de déploiement 26.

L'archive appareil `.xcarchive` est **non signée**. Elle ne peut pas être installée
sur un iPhone ou iPad. Pour une installation sur appareil ou TestFlight, fournir une
équipe Apple Developer, un certificat et un profil de provisionnement adaptés,
puis signer/exporter dans Xcode. Aucune IPA installable n'est produite par ce workflow.

## Validation et distribution Apple

La référence [c3439cc](https://github.com/Dreamsigner/duolingo-kmp-learning-app/actions/runs/39379770520)
a réussi sur macos-14 / Xcode 15.3, y compris les deux XCTest et l'archive non signée.
GitHub retire macos-14 le **2 novembre 2026** : la configuration nouvelle utilise macos-15.
[Apple demande Xcode 26 et les SDK 26 depuis le 28 avril 2026](https://developer.apple.com/news/upcoming-requirements/).
La migration a été compilée et testée sur les trois plateformes pour
[`c3af563`, run 39416233195](https://github.com/Dreamsigner/duolingo-kmp-learning-app/actions/runs/39416233195) :
les trois jobs sont terminés avec succès, état revérifié via l'API GitHub le
20 octobre 2026. Cela ne valide pas les modifications ultérieures non comprises
dans ce commit, ni la capture microphone physique ou une signature Apple.

Une configuration CI ajoutée au dépôt ne prouve pas que ses builds ont réussi :
consulter les trois jobs du workflow `Build` et leurs artefacts pour le commit livré.
