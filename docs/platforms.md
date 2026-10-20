# Construire et tester les applications

Les trois applications utilisent les mêmes écrans Compose et la même logique Kotlin.
Android démarre dans `MainActivity`, Windows dans `main.kt`, iOS dans une application
SwiftUI qui appelle `MainViewController()` via le framework statique `ComposeApp`.

## Versions conservées

Kotlin 2.0.0, Compose Multiplatform 1.6.11, AGP 8.2.2, Java 17 et Gradle 8.2.1.
Le dépôt ne contient pas de wrapper Gradle : installer cette version de Gradle,
ou utiliser `gradle/actions/setup-gradle`, comme dans le workflow.
La [matrice officielle Kotlin](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html#version-compatibility)
indique Xcode 15.3 pour Kotlin 2.0.0. La CI sélectionne explicitement cette version.

## Windows

```powershell
gradle :composeApp:desktopTest :composeApp:createDistributable
```

La CI produit `DuolingoKmpApp-Windows.zip`. Extraire tout le ZIP et lancer
`DuolingoKmpApp.exe`. Le dossier inclut le runtime Java ; conserver ses fichiers.
Ce ZIP est une application portable et ne contient pas d'installateur MSI/EXE.
Les formats d'installateur restent configurés dans Gradle pour une distribution future.

## Android

Installer Android SDK 34 et Build Tools 34.0.0 ; définir `ANDROID_HOME` ou `sdk.dir`
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

Sur un Mac avec Xcode 15.3 et Gradle sur le PATH, ouvrir
`iosApp/iosApp.xcodeproj`, sélectionner le scheme `iosApp` et un simulateur iOS 17.4.
Le projet Xcode est versionné ; aucun générateur Xcode supplémentaire n'est requis.
La phase de compilation Xcode exécute `:composeApp:embedAndSignAppleFrameworkForXcode`.
Cette [intégration directe officielle](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html)
compile et expose le framework Kotlin à SwiftUI.

```sh
export DEVELOPER_DIR=/Applications/Xcode_15.3.app/Contents/Developer
gradle :composeApp:iosX64Test
bash tools/smoke-ios.sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Release -destination 'generic/platform=iOS' \
  -archivePath build/ios/DuolingoKmpApp-unsigned.xcarchive \
  CODE_SIGNING_ALLOWED=NO archive
```

La CI exécute les tests Kotlin iOS, puis le test de lancement SwiftUI sur iPhone 15
et iPad (10e génération). Les fichiers `.xcresult` contiennent les assertions et
captures XCTest ; les PNG de l'accueil sont aussi fournis séparément.
Le ZIP simulateur contient une application x86_64 pour le simulateur Mac Intel.
Pour les lancements XCTest, `COMPOSE_UI_TESTING=1` active la synchronisation
permanente de l'arbre d'accessibilité Compose 1.6.11. Les lancements normaux gardent
le comportement par défaut. Cette configuration suit l'[API de synchronisation
documentée par JetBrains](https://kotlinlang.org/docs/multiplatform/compose-ios-accessibility.html#choose-the-tree-synchronization-option)
et le [code de la version 1.6.11](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.6.11/compose/ui/ui/src/uikitMain/kotlin/androidx/compose/ui/uikit/ComposeUIViewControllerConfiguration.uikit.kt).
La CI conserve la hiérarchie accessible et les captures même lorsqu'une assertion
échoue, et signale alors l'échec du job.
Sur un Mac Apple Silicon, compiler le même projet pour générer la version arm64.
Installer une application simulateur avec `xcrun simctl install booted <chemin.app>`.

L'archive appareil `.xcarchive` est **non signée**. Elle ne peut pas être installée
sur un iPhone ou iPad. Pour une installation sur appareil ou TestFlight, fournir une
équipe Apple Developer, un certificat et un profil de provisionnement adaptés,
puis signer/exporter dans Xcode. Aucune IPA installable n'est produite par ce workflow.

## Limite de la chaîne Apple actuelle

L'[image officielle macos-14](https://github.com/actions/runner-images/blob/main/images/macos/macos-14-Readme.md)
contient Xcode 15.3 et iOS 17.4. GitHub annonce son retrait le **2 novembre 2026**.
La CI Apple actuelle devra ensuite utiliser un runner Mac conservant Xcode 15.3,
ou faire l'objet d'une mise à niveau coordonnée Kotlin/Compose/Xcode. Les SDK anciens
de cette configuration ne suffisent pas à une soumission actuelle sur l'App Store :
[Apple demande Xcode 26 et les SDK 26 depuis le 28 avril 2026](https://developer.apple.com/news/upcoming-requirements/).

Une configuration CI ajoutée au dépôt ne prouve pas que ses builds ont réussi :
consulter les trois jobs du workflow `Build` et leurs artefacts pour le commit livré.
