# Migration coordonnÃ©e vers une chaÃ®ne Apple Xcode 26

Recherche du 20 octobre 2026, sources officielles consultÃ©es en direct. **Migration appliquÃ©e dans les fichiers de travail aprÃ¨s validation de la rÃ©fÃ©rence `c3439cc`** ; aucun build de la nouvelle chaÃ®ne n'est encore rapportÃ© ici. Aucun commit ni push de cette migration n'a Ã©tÃ© effectuÃ© par ce lot.

## RÃ©fÃ©rence exÃ©cutÃ©e avant migration

Le [run `c0c82e9`, 39353730474](https://github.com/Dreamsigner/duolingo-kmp-learning-app/actions/runs/39353730474) a terminÃ© en Ã©chec le 20 octobre. Windows, Android et les tests Kotlin `iosX64Test` rÃ©ussissaient. Sur Xcode 15.3, les deux lancements XCTest Ã©chouaient chacun sur les assertions `Cours` et `Profil`. L'archive appareil Ã©tait sautÃ©e. L'artefact de diagnostic `iOS-checks` porte l'identifiant `12235255237`.

Le diff vÃ©rifiÃ© entre `c0c82e99674b9f9fdbd81c04814823164f2432ea` et `c3439cca75d23b430a41b0c5bd2c19bfa1349135` ajoute dans `LaunchTests.swift` l'environnement `COMPOSE_UI_TESTING=1` **avant** `app.launch()`, ainsi que la conservation/affichage de la hiÃ©rarchie et de la capture en fin de test. L'ancienne implÃ©mentation `MainViewController` ne forÃ§ait `AccessibilitySyncOptions.Always` que lorsque cette variable Ã©tait prÃ©sente. Elle ne l'Ã©tait donc pas dans le test commitÃ© prÃ©cÃ©dent.

Le [run corrigÃ© `c3439cc`, 39379770520](https://github.com/Dreamsigner/duolingo-kmp-learning-app/actions/runs/39379770520) est **entiÃ¨rement rÃ©ussi**, terminÃ© Ã  **06:38:43 UTC** :

- Jobs Windows et shared/Android : rÃ©ussite.
- Job iOS `118485965528`, runner `macos-14` ; journal : **Xcode 15.3, build 15E204a**, Gradle 8.2.1.
- Tests Kotlin `iosX64Test` : rÃ©ussite.
- Script historique : iPhone 15 puis iPad (10e gÃ©nÃ©ration), runtime configurÃ© iOS 17.4. XCTest : **1 test, 0 Ã©chec** pour chaque appareil, en 8,315 s puis 11,330 s. Deux `TEST SUCCEEDED` dans le journal.
- Archive Release appareil : `ARCHIVE SUCCEEDED` Ã  06:38:29 UTC. Elle reste non signÃ©e.
- Artefacts publiÃ©s : `iOS-checks` **12245990512** (1 224 552 octets), `DuolingoKmpApp-iOS-builds` **12245776238** (26 039 985 octets), APK Android **12244901504**, Windows **12244797881**, Windows-checks **12244693336**, Android-checks **12244652526**. RÃ©tention CI : 14 jours ; les identifiants ne garantissent pas une disponibilitÃ© permanente.

Ces faits proviennent des mÃ©tadonnÃ©es REST et du journal du job iOS lus directement. Les journaux ont Ã©tÃ© traitÃ©s en mÃ©moire ; aucun secret ni URL signÃ©e de redirection n'est archivÃ©. Les PNG et `.xcresult` n'ont pas Ã©tÃ© inspectÃ©s visuellement dans ce lot. Le correctif isolÃ© rÃ©tablit les assertions de lancement sur l'ancienne chaÃ®ne ; cela confirme le diagnostic d'un faux nÃ©gatif d'accessibilitÃ©, sans prouver toutes les interactions de l'application.


## Versions appliquÃ©es, validation en attente

| Ã‰lÃ©ment | Ã‰tat constatÃ© dans le dÃ©pÃ´t | Version retenue |
|---|---|---|
| Kotlin Multiplatform | 2.0.0 | **2.2.21** |
| Plugin Compose Compiler | 2.0.0 via catalogue | **2.2.21**, mÃªme version que Kotlin |
| Plugin Kotlin Serialization | `2.0.0` explicitement dans `composeApp/build.gradle.kts` | **2.2.21**, via le catalogue commun |
| Compose Multiplatform | 1.6.11 | **1.9.3** |
| Android Gradle Plugin | 8.2.2 | **8.10.1** |
| Gradle, configuration CI sans wrapper versionnÃ© | 8.2.1 | **8.11.1**, explicitement sur les trois jobs |
| JDK / cible bytecode | 17 | **17** |
| Android compileSdk | 34 | **35** |
| Android minSdk | 24 | 24 pour ce lot |
| Android targetSdk | 34 | DÃ©cision sÃ©parÃ©e de publication Android ; ne pas confondre avec compileSdk |
| Xcode | 15.3 | **26.0.1**, chemin explicite |
| Runner principal | macos-14 Intel | **macos-15 ARM64** |
| Tests Kotlin simulateur | iosX64Test | **iosSimulatorArm64Test** sur ce runner |
| Interface | Material 2, `compose.material` | Material 2 conservÃ© ; ajouter explicitement les icÃ´nes |

Cette combinaison est une **dÃ©duction des contraintes publiÃ©es**, pas un build validÃ© de l'application. Elle Ã©vite les versions alpha, bÃªta et RC. Kotlin 2.2.21 est le premier palier de la matrice actuellement publiÃ©e qui indique Xcode 26.0 ; Kotlin 2.2.20 y indique encore Xcode 16.4. La mÃªme ligne autorise Gradle 7.6.3â€“8.14 et AGP 7.3.1â€“8.11.1. [Matrice Kotlin](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html#version-compatibility), [release stable 2.2.21](https://github.com/JetBrains/kotlin/releases/tag/v2.2.21).

Compose 1.9.3 est une release stable qui utilise Runtime, UI, Foundation et Material 1.9.4. Elle corrige notamment un crash d'accessibilitÃ© sur iOS antÃ©rieur Ã  17. Le plugin Compose Compiler doit suivre la version de Kotlin ; les recommandations iOS demandent au moins Kotlin 2.2.20. La prÃ©sence de Material 2 dans cette release permet de conserver les composants actuels. [Release 1.9.3](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.9.3), [compatibilitÃ© Compose](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html).

### Pourquoi Android doit Ã©galement Ã©voluer

Les trois AAR officiels Google suivants ont Ã©tÃ© lus **en mÃ©moire**, sans extraction sur disque ni installation. Leur fichier `META-INF/com/android/build/gradle/aar-metadata.properties` donne dans chaque cas `minCompileSdk=35` et `minAndroidGradlePluginVersion=8.6.0` : [UI 1.9.4](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.9.4/ui-android-1.9.4.aar), [Foundation 1.9.4](https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation-android/1.9.4/foundation-android-1.9.4.aar), [Material 1.9.4](https://dl.google.com/dl/android/maven2/androidx/compose/material/material-android/1.9.4/material-android-1.9.4.aar).

Ces minima AAR ne suffisent pas Ã  choisir AGP : la [matrice officielle Kotlin/D8/R8 de Google](https://developer.android.com/build/kotlin-support) indique **R8 8.10.21 pour Kotlin 2.2**. Retenir la famille **AGP 8.10**, avec son correctif stable **8.10.1**, Ã©vite de surcharger manuellement son R8 intÃ©grÃ©. Une proposition AGP 8.6.1 fondÃ©e uniquement sur les AAR nÃ©gligerait cette contrainte du compilateur Android. La page Google affiche une plage AGP dans sa colonne correspondante ; l'exigence R8 doit Ãªtre vÃ©rifiÃ©e sÃ©parÃ©ment du simple chargement du plugin Kotlin.

AGP 8.10 documente Gradle **8.11.1**, JDK **17**, Build Tools **35.0.0** et la prise en charge de l'API 36. `compileSdk=35` reste le minimum constatÃ© pour nos AAR ; le support maximal API 36 n'impose pas de l'activer dans ce lot. Installer `platforms;android-35` et `build-tools;35.0.0` dans les jobs concernÃ©s. Ce palier reste infÃ©rieur Ã  AGP 9 et sa rÃ©organisation des modules Android/KMP. Confirmer le R8 effectivement rÃ©solu et l'ensemble des mÃ©tadonnÃ©es transitives lors du build. [Notes officielles AGP 8.10 et correctif 8.10.1](https://developer.android.com/build/releases/agp-8-10-0-release-notes).

## Runner, Xcode et architectures

GitHub annonce le retrait de macOS 14 au **2 novembre 2026**. Le maintien de ce runner ne constitue donc pas une chaÃ®ne durable. [Annonce GitHub](https://github.blog/changelog/2026-10-01-github-actions-macos-14-runner-image-retirement/).

Le label **`macos-15` correspond actuellement Ã  ARM64** ; `macos-15-intel` dÃ©signe explicitement x86_64. Le fichier `macos-15-Readme.md` dÃ©crit l'image Intel : il faut lire `macos-15-arm64-Readme.md` pour le label principal. [Table des labels officielle](https://github.com/actions/runner-images/blob/main/README.md).

L'[image ARM64 macOS 15](https://github.com/actions/runner-images/blob/main/images/macos/macos-15-arm64-Readme.md) consultÃ©e annonce Xcode 26.0.1, build 17A400, dans `/Applications/Xcode_26.0.1.app`, les SDK iOS/appareil et simulateur 26.0 et un runtime installÃ© affichÃ© iOS 26.0, version 26.0.1. Son Xcode par dÃ©faut reste 16.4 : fixer `DEVELOPER_DIR=/Applications/Xcode_26.0.1.app/Contents/Developer`, puis vÃ©rifier `xcodebuild -version`. L'image consultÃ©e porte la version `20260907.0337.1` ; cette observation ne fige pas l'image d'un futur run.

Choisir un iPhone et un iPad prÃ©sents dans le runtime sÃ©lectionnÃ©. Lire `xcrun simctl list runtimes -j` et `xcrun simctl list devicetypes -j`, puis utiliser leurs identifiants rÃ©els. Ne pas recopier l'ancien runtime `iOS-17-4` ni inventer le suffixe du runtime 26.0.1 Ã  partir de son nom d'affichage. Conserver les deux rÃ©sultats XCTest et captures.

Sur ARM64, exÃ©cuter `:composeApp:iosSimulatorArm64Test` ; les cibles `iosArm64` et `iosSimulatorArm64` existent dÃ©jÃ . Sur `macos-15-intel`, la variante de transition serait `iosX64Test`, avec la mÃªme migration Kotlin/Compose/Xcode. ARM64 est prÃ©fÃ©rable pour la suite : la documentation Kotlin/Native classe le simulateur ARM64 au premier niveau de support et impose un hÃ´te natif pour l'exÃ©cution des tests correspondants. [Cibles et hÃ´tes Kotlin/Native](https://kotlinlang.org/docs/native-target-support.html).

**Option macOS 26** : les images [ARM64](https://github.com/actions/runner-images/blob/main/images/macos/macos-26-arm64-Readme.md) et [Intel](https://github.com/actions/runner-images/blob/main/images/macos/macos-26-Readme.md) publient Ã©galement Xcode 26.0.1. Cependant, la liste des simulateurs installÃ©s commence Ã  iOS 26.2 dans les images consultÃ©es. La prÃ©sence du SDK simulateur 26.0 ne prouve pas qu'un runtime 26.0 est prÃªt Ã  dÃ©marrer. Pour le premier changement minimal, prÃ©fÃ©rer macOS 15 ; passer Ã  macOS 26 aprÃ¨s sÃ©lection et preuve d'un runtime compatible, ou aprÃ¨s une nouvelle combinaison Kotlin/Xcode vÃ©rifiÃ©e. Ne pas laisser le Xcode par dÃ©faut 26.6 remplacer silencieusement le 26.0 prÃ©vu par la matrice Kotlin retenue.

### PrÃ©cision de la chaÃ®ne Kotlin/Native 2.2.21

Le fichier [`konan.properties` de la release Kotlin 2.2.21](https://github.com/JetBrains/kotlin/blob/v2.2.21/kotlin-native/konan/konan.properties) cible explicitement `xcode_26.0.1_17A400` et le SDK iOS 26.0. Il indique un minimum iOS 12.0 et un minimum 14.0 pour `iosSimulatorArm64`. Le minimum 14.0 du projet Xcode est donc conservÃ© Ã  ce stade ; vÃ©rifier aussi les exigences des binaires Compose rÃ©solus avant de conclure sur le minimum effectivement distribuable.

## Adaptations minimales du code

### AccessibilitÃ© iOS

Le `MainViewController.kt` actuel utilise `AccessibilitySyncOptions.Always(debugLogger = null)` lorsque `COMPOSE_UI_TESTING=1`. **Il n'existe pas de remplacement un pour un Ã  introduire en Compose 1.9.3** : l'option et son type ont Ã©tÃ© retirÃ©s depuis 1.8 ; l'arbre est synchronisÃ© Ã  la demande. La documentation gÃ©nÃ©rale conserve encore du texte historique autour de cette option : privilÃ©gier la note explicite de retrait et les sources de la version ciblÃ©e. [Changelog 1.8](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.8.0), [configuration UIKit 1.9.3](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.9.3/compose/ui/ui/src/uikitMain/kotlin/androidx/compose/ui/uikit/ComposeUIViewControllerConfiguration.uikit.kt).

L'adaptation proposÃ©e est de retirer le bloc de configuration, ses imports devenus inutiles et l'opt-in s'il n'est plus nÃ©cessaire :

```kotlin
import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App() }
```

L'environnement XCTest dÃ©diÃ© ne force alors plus cette ancienne API. Confirmer avec de vrais tests que les contrÃ´les sont exposÃ©s ; les propriÃ©tÃ©s Compose `testTag` se mappent vers `accessibilityIdentifier`. PrÃ©fÃ©rer des identifiants stables aux hypothÃ¨ses sur le type UIKit d'un texte fusionnÃ© dans un bouton. [AccessibilitÃ© et XCTest](https://kotlinlang.org/docs/multiplatform/compose-ios-accessibility.html).

### Material 2, icÃ´nes et autres points d'intÃ©gration

Depuis Compose 1.8, Material n'apporte plus automatiquement `material-icons-core`. Les imports `Icons.Default.Home`, `Person`, `Star`, `Close`, etc., actuellement prÃ©sents nÃ©cessitent une dÃ©pendance explicite. La migration officielle donne `implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")` dans `commonMain`. VÃ©rifier la compilation des icÃ´nes employÃ©es ; aucune conversion globale vers Material 3 n'est requise par le choix proposÃ©. [Note de migration des icÃ´nes](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.8.0).

Aligner les plugins Kotlin, Compose Compiler et Serialization ; adapter le DSL de compilation si une dÃ©prÃ©ciation devient bloquante. Ne pas mettre toutes les bibliothÃ¨ques au dernier numÃ©ro par automatisme : inspecter le graphe rÃ©solu et traiter les incompatibilitÃ©s natives constatÃ©es. Les changements de sÃ©rialisation doivent prÃ©server `progress.json` et les clÃ©s de stockage natives ; cette migration de chaÃ®ne ne justifie aucune remise Ã  zÃ©ro de progression.

## Ordre d'application et preuves attendues

1. RÃ©fÃ©rence archivÃ©e ci-dessus : `c0c82e9` Ã©choue sur XCTest, `c3439cc` rÃ©ussit les trois jobs. Ne pas attribuer cette rÃ©ussite aux nouvelles versions.
2. EffectuÃ© dans les fichiers de travail : catalogue, plugins alignÃ©s, cibles JVM 17, compileSdk 35, icÃ´nes explicites, retrait de lâ€™ancienne API, Gradle/SDK des jobs et runner ARM64/Xcode 26.0.1. Material 2, schÃ©ma JSON de progression et minimum Xcode iOS 14.0 conservÃ©s. Les Ã©tapes Python du parent, dont les contrÃ´les introductifs/discrets et le diff de couverture, sont conservÃ©es.
3. Sur Windows : compilation, tests de progression, application autonome et contrÃ´le visuel. Sur Android : mÃ©tadonnÃ©es AAR, lint, APK et lancement rÃ©el en Ã©mulateur.
4. Sur Apple : consigner architecture, Xcode, SDK et runtime rÃ©els ; lancer les tests Kotlin, les XCTest iPhone/iPad et inspecter les captures. VÃ©rifier notes, rÃ©ponses, aides, navigation et restauration de la progression.
5. Construire sÃ©parÃ©ment le framework appareil et l'archive Release non signÃ©e. ContrÃ´ler le contenu, les architectures et le minimum iOS rÃ©el ; un SDK de compilation 26 n'implique pas un minimum de dÃ©ploiement iOS 26. Ne pas relever le minimum iOS sans vÃ©rifier la contrainte des binaires rÃ©solus.
6. PrÃ©senter les preuves par plateforme et par commit. Si un runtime, une cible ou une dÃ©pendance Ã©choue, enregistrer l'Ã©chec exact avant d'Ã©largir la migration.

Aucun build de la **nouvelle** chaÃ®ne nâ€™a Ã©tÃ© exÃ©cutÃ© par ce lot. Le script iOS lit dÃ©sormais les inventaires JSON rÃ©els, choisit un runtime iOS 26.0/26.0.x disponible et deux appareils existants iPhone/iPad, puis conserve leur sÃ©lection avec les rÃ©sultats. Il Ã©choue explicitement si le runtime ou une famille manque ; il nâ€™installe aucun runtime. La CI consigne et impose lâ€™architecture ARM64, puis archive versions Xcode/SDK et inventaires avec les `.xcresult`. La disponibilitÃ© effective de ces Ã©lÃ©ments sur le prochain runner reste Ã  dÃ©montrer.

## Distribution Apple

Apple impose Xcode 26 ou ultÃ©rieur et les SDK 26 aux envois App Store Connect depuis le 28 avril 2026. Le palier proposÃ© rÃ©pond Ã  cette contrainte de chaÃ®ne, sous rÃ©serve de validation rÃ©elle du build. [Exigences Apple](https://developer.apple.com/news/upcoming-requirements/).

L'Ã©quipe Apple Developer, le certificat et le profil de provisionnement restent des prÃ©requis manquants pour signer et exporter une application appareil. Une archive non signÃ©e et une application simulateur ne sont pas une IPA installable ; la migration de versions ne fournit pas ces Ã©lÃ©ments de signature.
