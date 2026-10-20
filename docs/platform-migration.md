# Migration coordonnée vers une chaîne Apple Xcode 26

Recherche du 20 octobre 2026, sources officielles consultées en direct. **Migration appliquée dans les fichiers de travail après validation de la référence `c3439cc`** ; aucun build de la nouvelle chaîne n'est encore rapporté ici. Aucun commit ni push de cette migration n'a été effectué par ce lot.

## Référence exécutée avant migration

Le [run `c0c82e9`, 39353730474](https://github.com/Dreamsigner/duolingo-kmp-learning-app/actions/runs/39353730474) a terminé en échec le 20 octobre. Windows, Android et les tests Kotlin `iosX64Test` réussissaient. Sur Xcode 15.3, les deux lancements XCTest échouaient chacun sur les assertions `Cours` et `Profil`. L'archive appareil était sautée. L'artefact de diagnostic `iOS-checks` porte l'identifiant `12235255237`.

Le diff vérifié entre `c0c82e99674b9f9fdbd81c04814823164f2432ea` et `c3439cca75d23b430a41b0c5bd2c19bfa1349135` ajoute dans `LaunchTests.swift` l'environnement `COMPOSE_UI_TESTING=1` **avant** `app.launch()`, ainsi que la conservation/affichage de la hiérarchie et de la capture en fin de test. L'ancienne implémentation `MainViewController` ne forçait `AccessibilitySyncOptions.Always` que lorsque cette variable était présente. Elle ne l'était donc pas dans le test commité précédent.

Le [run corrigé `c3439cc`, 39379770520](https://github.com/Dreamsigner/duolingo-kmp-learning-app/actions/runs/39379770520) est **entièrement réussi**, terminé à **06:38:43 UTC** :

- Jobs Windows et shared/Android : réussite.
- Job iOS `118485965528`, runner `macos-14` ; journal : **Xcode 15.3, build 15E204a**, Gradle 8.2.1.
- Tests Kotlin `iosX64Test` : réussite.
- Script historique : iPhone 15 puis iPad (10e génération), runtime configuré iOS 17.4. XCTest : **1 test, 0 échec** pour chaque appareil, en 8,315 s puis 11,330 s. Deux `TEST SUCCEEDED` dans le journal.
- Archive Release appareil : `ARCHIVE SUCCEEDED` à 06:38:29 UTC. Elle reste non signée.
- Artefacts publiés : `iOS-checks` **12245990512** (1 224 552 octets), `DuolingoKmpApp-iOS-builds` **12245776238** (26 039 985 octets), APK Android **12244901504**, Windows **12244797881**, Windows-checks **12244693336**, Android-checks **12244652526**. Rétention CI : 14 jours ; les identifiants ne garantissent pas une disponibilité permanente.

Ces faits proviennent des métadonnées REST et du journal du job iOS lus directement. Les journaux ont été traités en mémoire ; aucun secret ni URL signée de redirection n'est archivé. Les PNG et `.xcresult` n'ont pas été inspectés visuellement dans ce lot. Le correctif isolé rétablit les assertions de lancement sur l'ancienne chaîne ; cela confirme le diagnostic d'un faux négatif d'accessibilité, sans prouver toutes les interactions de l'application.


## Versions appliquées, validation en attente

| Élément | État constaté dans le dépôt | Version retenue |
|---|---|---|
| Kotlin Multiplatform | 2.0.0 | **2.2.21** |
| Plugin Compose Compiler | 2.0.0 via catalogue | **2.2.21**, même version que Kotlin |
| Plugin Kotlin Serialization | `2.0.0` explicitement dans `composeApp/build.gradle.kts` | **2.2.21**, via le catalogue commun |
| Compose Multiplatform | 1.6.11 | **1.9.3** |
| Android Gradle Plugin | 8.2.2 | **8.10.1** |
| Gradle, configuration CI sans wrapper versionné | 8.2.1 | **8.11.1**, explicitement sur les trois jobs |
| JDK / cible bytecode | 17 | **17** |
| Android compileSdk | 34 | **35** |
| Android minSdk | 24 | 24 pour ce lot |
| Android targetSdk | 34 | Décision séparée de publication Android ; ne pas confondre avec compileSdk |
| Xcode | 15.3 | **26.0.1**, chemin explicite |
| Runner principal | macos-14 Intel | **macos-15 ARM64** |
| Tests Kotlin simulateur | iosX64Test | **iosSimulatorArm64Test** sur ce runner |
| Interface | Material 2, `compose.material` | Material 2 conservé ; ajouter explicitement les icônes |

Cette combinaison est une **déduction des contraintes publiées**, pas un build validé de l'application. Elle évite les versions alpha, bêta et RC. Kotlin 2.2.21 est le premier palier de la matrice actuellement publiée qui indique Xcode 26.0 ; Kotlin 2.2.20 y indique encore Xcode 16.4. La même ligne autorise Gradle 7.6.3–8.14 et AGP 7.3.1–8.11.1. [Matrice Kotlin](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html#version-compatibility), [release stable 2.2.21](https://github.com/JetBrains/kotlin/releases/tag/v2.2.21).

Compose 1.9.3 est une release stable qui utilise Runtime, UI, Foundation et Material 1.9.4. Elle corrige notamment un crash d'accessibilité sur iOS antérieur à 17. Le plugin Compose Compiler doit suivre la version de Kotlin ; les recommandations iOS demandent au moins Kotlin 2.2.20. La présence de Material 2 dans cette release permet de conserver les composants actuels. [Release 1.9.3](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.9.3), [compatibilité Compose](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html).

### Pourquoi Android doit également évoluer

Les trois AAR officiels Google suivants ont été lus **en mémoire**, sans extraction sur disque ni installation. Leur fichier `META-INF/com/android/build/gradle/aar-metadata.properties` donne dans chaque cas `minCompileSdk=35` et `minAndroidGradlePluginVersion=8.6.0` : [UI 1.9.4](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.9.4/ui-android-1.9.4.aar), [Foundation 1.9.4](https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation-android/1.9.4/foundation-android-1.9.4.aar), [Material 1.9.4](https://dl.google.com/dl/android/maven2/androidx/compose/material/material-android/1.9.4/material-android-1.9.4.aar).

Ces minima AAR ne suffisent pas à choisir AGP : la [matrice officielle Kotlin/D8/R8 de Google](https://developer.android.com/build/kotlin-support) indique **R8 8.10.21 pour Kotlin 2.2**. Retenir la famille **AGP 8.10**, avec son correctif stable **8.10.1**, évite de surcharger manuellement son R8 intégré. Une proposition AGP 8.6.1 fondée uniquement sur les AAR négligerait cette contrainte du compilateur Android. La page Google affiche une plage AGP dans sa colonne correspondante ; l'exigence R8 doit être vérifiée séparément du simple chargement du plugin Kotlin.

AGP 8.10 documente Gradle **8.11.1**, JDK **17**, Build Tools **35.0.0** et la prise en charge de l'API 36. `compileSdk=35` reste le minimum constaté pour nos AAR ; le support maximal API 36 n'impose pas de l'activer dans ce lot. Installer `platforms;android-35` et `build-tools;35.0.0` dans les jobs concernés. Ce palier reste inférieur à AGP 9 et sa réorganisation des modules Android/KMP. Confirmer le R8 effectivement résolu et l'ensemble des métadonnées transitives lors du build. [Notes officielles AGP 8.10 et correctif 8.10.1](https://developer.android.com/build/releases/agp-8-10-0-release-notes).

## Runner, Xcode et architectures

GitHub annonce le retrait de macOS 14 au **2 novembre 2026**. Le maintien de ce runner ne constitue donc pas une chaîne durable. [Annonce GitHub](https://github.blog/changelog/2026-10-01-github-actions-macos-14-runner-image-retirement/).

Le label **`macos-15` correspond actuellement à ARM64** ; `macos-15-intel` désigne explicitement x86_64. Le fichier `macos-15-Readme.md` décrit l'image Intel : il faut lire `macos-15-arm64-Readme.md` pour le label principal. [Table des labels officielle](https://github.com/actions/runner-images/blob/main/README.md).

L'[image ARM64 macOS 15](https://github.com/actions/runner-images/blob/main/images/macos/macos-15-arm64-Readme.md) consultée annonce Xcode 26.0.1, build 17A400, dans `/Applications/Xcode_26.0.1.app`, les SDK iOS/appareil et simulateur 26.0 et un runtime installé affiché iOS 26.0, version 26.0.1. Son Xcode par défaut reste 16.4 : fixer `DEVELOPER_DIR=/Applications/Xcode_26.0.1.app/Contents/Developer`, puis vérifier `xcodebuild -version`. L'image consultée porte la version `20260907.0337.1` ; cette observation ne fige pas l'image d'un futur run.

Choisir un iPhone et un iPad présents dans le runtime sélectionné. Lire `xcrun simctl list runtimes -j` et `xcrun simctl list devicetypes -j`, puis utiliser leurs identifiants réels. Ne pas recopier l'ancien runtime `iOS-17-4` ni inventer le suffixe du runtime 26.0.1 à partir de son nom d'affichage. Conserver les deux résultats XCTest et captures.

Sur ARM64, exécuter `:composeApp:iosSimulatorArm64Test` ; les cibles `iosArm64` et `iosSimulatorArm64` existent déjà. Sur `macos-15-intel`, la variante de transition serait `iosX64Test`, avec la même migration Kotlin/Compose/Xcode. ARM64 est préférable pour la suite : la documentation Kotlin/Native classe le simulateur ARM64 au premier niveau de support et impose un hôte natif pour l'exécution des tests correspondants. [Cibles et hôtes Kotlin/Native](https://kotlinlang.org/docs/native-target-support.html).

**Option macOS 26** : les images [ARM64](https://github.com/actions/runner-images/blob/main/images/macos/macos-26-arm64-Readme.md) et [Intel](https://github.com/actions/runner-images/blob/main/images/macos/macos-26-Readme.md) publient également Xcode 26.0.1. Cependant, la liste des simulateurs installés commence à iOS 26.2 dans les images consultées. La présence du SDK simulateur 26.0 ne prouve pas qu'un runtime 26.0 est prêt à démarrer. Pour le premier changement minimal, préférer macOS 15 ; passer à macOS 26 après sélection et preuve d'un runtime compatible, ou après une nouvelle combinaison Kotlin/Xcode vérifiée. Ne pas laisser le Xcode par défaut 26.6 remplacer silencieusement le 26.0 prévu par la matrice Kotlin retenue.

### Précision de la chaîne Kotlin/Native 2.2.21

Le fichier [`konan.properties` de la release Kotlin 2.2.21](https://github.com/JetBrains/kotlin/blob/v2.2.21/kotlin-native/konan/konan.properties) cible explicitement `xcode_26.0.1_17A400` et le SDK iOS 26.0. Il indique un minimum iOS 12.0 et un minimum 14.0 pour `iosSimulatorArm64`. Le minimum 14.0 du projet Xcode est donc conservé à ce stade ; vérifier aussi les exigences des binaires Compose résolus avant de conclure sur le minimum effectivement distribuable.

## Adaptations minimales du code

### Accessibilité iOS

Le `MainViewController.kt` actuel utilise `AccessibilitySyncOptions.Always(debugLogger = null)` lorsque `COMPOSE_UI_TESTING=1`. **Il n'existe pas de remplacement un pour un à introduire en Compose 1.9.3** : l'option et son type ont été retirés depuis 1.8 ; l'arbre est synchronisé à la demande. La documentation générale conserve encore du texte historique autour de cette option : privilégier la note explicite de retrait et les sources de la version ciblée. [Changelog 1.8](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.8.0), [configuration UIKit 1.9.3](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.9.3/compose/ui/ui/src/uikitMain/kotlin/androidx/compose/ui/uikit/ComposeUIViewControllerConfiguration.uikit.kt).

L'adaptation proposée est de retirer le bloc de configuration, ses imports devenus inutiles et l'opt-in s'il n'est plus nécessaire :

```kotlin
import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App() }
```

L'environnement XCTest dédié ne force alors plus cette ancienne API. Confirmer avec de vrais tests que les contrôles sont exposés ; les propriétés Compose `testTag` se mappent vers `accessibilityIdentifier`. Préférer des identifiants stables aux hypothèses sur le type UIKit d'un texte fusionné dans un bouton. [Accessibilité et XCTest](https://kotlinlang.org/docs/multiplatform/compose-ios-accessibility.html).

### Material 2, icônes et autres points d'intégration

Depuis Compose 1.8, Material n'apporte plus automatiquement `material-icons-core`. Les imports `Icons.Default.Home`, `Person`, `Star`, `Close`, etc., actuellement présents nécessitent une dépendance explicite. La migration officielle donne `implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")` dans `commonMain`. Vérifier la compilation des icônes employées ; aucune conversion globale vers Material 3 n'est requise par le choix proposé. [Note de migration des icônes](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.8.0).

Aligner les plugins Kotlin, Compose Compiler et Serialization ; adapter le DSL de compilation si une dépréciation devient bloquante. Ne pas mettre toutes les bibliothèques au dernier numéro par automatisme : inspecter le graphe résolu et traiter les incompatibilités natives constatées. Les changements de sérialisation doivent préserver `progress.json` et les clés de stockage natives ; cette migration de chaîne ne justifie aucune remise à zéro de progression.

## Ordre d'application et preuves attendues

1. Référence archivée ci-dessus : `c0c82e9` échoue sur XCTest, `c3439cc` réussit les trois jobs. Ne pas attribuer cette réussite aux nouvelles versions.
2. Effectué dans les fichiers de travail : catalogue, plugins alignés, cibles JVM 17, compileSdk 35, icônes explicites, retrait de l’ancienne API, Gradle/SDK des jobs et runner ARM64/Xcode 26.0.1. Material 2, schéma JSON de progression et minimum Xcode iOS 14.0 conservés. Les étapes Python du parent, dont les contrôles introductifs/discrets et le diff de couverture, sont conservées.
3. Sur Windows : compilation, tests de progression, application autonome et contrôle visuel. Sur Android : métadonnées AAR, lint, APK et lancement réel en émulateur.
4. Sur Apple : consigner architecture, Xcode, SDK et runtime réels ; lancer les tests Kotlin, les XCTest iPhone/iPad et inspecter les captures. Vérifier notes, réponses, aides, navigation et restauration de la progression.
5. Construire séparément le framework appareil et l'archive Release non signée. Contrôler le contenu, les architectures et le minimum iOS réel ; un SDK de compilation 26 n'implique pas un minimum de déploiement iOS 26. Ne pas relever le minimum iOS sans vérifier la contrainte des binaires résolus.
6. Présenter les preuves par plateforme et par commit. Si un runtime, une cible ou une dépendance échoue, enregistrer l'échec exact avant d'élargir la migration.

Aucun build de la **nouvelle** chaîne n’a été exécuté par ce lot. Le script iOS lit désormais les inventaires JSON réels, choisit un runtime iOS 26.0/26.0.x disponible et deux appareils existants iPhone/iPad, puis conserve leur sélection avec les résultats. Il échoue explicitement si le runtime ou une famille manque ; il n’installe aucun runtime. La CI consigne et impose l’architecture ARM64, puis archive versions Xcode/SDK et inventaires avec les `.xcresult`. La disponibilité effective de ces éléments sur le prochain runner reste à démontrer.

## Revue indépendante après application

Contrôle de sources du 20 octobre 2026, sans build concurrent :

- **Runner et Xcode** : la table actuelle `runner-images/README.md` donne bien `macos-15` ARM64 et `macos-15-intel` x64. Le README ARM64 annonce toujours Xcode 26.0.1 / 17A400, SDK iOS 26.0 et runtime installé 26.0.1 ; le workflow impose l’architecture et la version Xcode avant les tests. Ce sont les caractéristiques publiées de l’image, pas une preuve du prochain runner alloué.
- **Minimum Compose** : la documentation générale est mobile dans le temps. La [note officielle Compose 1.11.1](https://kotlinlang.org/docs/multiplatform/whats-new-compose-111.html#changes-to-ios-target-support) décrit le relèvement du minimum iOS de 13.0 à 14.0 et le retrait des cibles Apple x86_64. Ces changements ultérieurs ne doivent pas être attribués automatiquement à Compose 1.9.3. Le minimum projet 14.0 et les cibles conservées restent cohérents avec le palier retenu et les propriétés Kotlin 2.2.21. Le catalogue [Compose core 1.9.3](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.9.3/gradle/libs.versions.toml) fixe Skiko 0.9.22.2 ; les [options de compilation natives de cette version Skiko](https://github.com/JetBrains/skiko/blob/v0.9.22.2/skiko/buildSrc/src/main/kotlin/tasks/configuration/NativeTasksConfiguration.kt) fixent `-mios-version-min=12.0` / `-mios-simulator-version-min=12.0`. Ces minima de compilation ne relèvent donc pas le minimum projet 14.0. Le minimum effectif de chaque binaire distribué devra être lu dans l’archive finale ; cette revue n’a pas inspecté ces binaires.
- **Imports natifs audio** : les définitions [`AVFAudio.def`](https://github.com/JetBrains/kotlin/blob/v2.2.21/kotlin-native/platformLibs/src/platform/ios/AVFAudio.def) et [`Foundation.def`](https://github.com/JetBrains/kotlin/blob/v2.2.21/kotlin-native/platformLibs/src/platform/ios/Foundation.def) exposent les modules sous `platform.AVFAudio` et `platform.Foundation`. Les observateurs `AVAudioSessionInterruptionNotification`, `NSNotificationCenter` et `NSOperationQueue.mainQueue` présents dans les backends suivent ces modules.
- **Permission microphone** : [`AVAudioSession.requestRecordPermission`](https://developer.apple.com/documentation/avfaudio/avaudiosession/requestrecordpermission(_:)) est dépréciée depuis iOS 17, au profit d’AVAudioApplication ; les métadonnées Apple ne la déclarent pas indisponible. Ce seul fait ne prouve donc pas une erreur de compilation sous Xcode 26. Une modernisation future devra garder un chemin pour iOS 14–16.
- **Interruptions** : les anciens callbacks [`audioPlayerBeginInterruption`](https://developer.apple.com/documentation/avfaudio/avaudioplayerdelegate/audioplayerbegininterruption(_:)) et [`audioRecorderBeginInterruption`](https://developer.apple.com/documentation/avfaudio/avaudiorecorderdelegate/audiorecorderbegininterruption(_:)) sont dépréciés depuis iOS 8. Apple recommande les notifications de session ; les deux backends possèdent désormais ces observateurs, retirés lors de la libération. Les callbacks historiques subsistants sont redondants, sans suppression documentée. La génération et l’identité de l’objet actif protègent contre une deuxième livraison.
- **Android** : le callback `MainActivity.onRequestPermissionsResult` délègue le code de requête et les résultats au backend après `super`. `AudioRecord.read(..., READ_BLOCKING)` existe depuis API 23 et reste compatible avec minSdk 24 ; [référence officielle](https://developer.android.com/reference/android/media/AudioRecord#read(byte[],%20int,%20int,%20int)). Aucun problème de signature/import concret n’a été identifié par lecture seule.
- **Smoke CI** : les inventaires sont archivés avant même les tests Kotlin ; le script sélectionne deux UDID disponibles dans le runtime demandé, conserve les résultats des deux XCTest et propage leur échec. Il ne prouve pas encore la lecture/enregistrement audio, le comportement après interruption, ni l’octroi/refus de permission sur appareil. Ces contrôles restent à effectuer avec le prochain build et des essais natifs.

## Échec de compilation natif constaté au commit 0607e5a

Le journal CI `ios-0607e5a.log` fourni au diagnostic rapporte à **07:06:08 UTC le 20 octobre 2026**, dans `:composeApp:compileKotlinIosSimulatorArm64`, quatre références non résolues : `AudioPlayer.ios.kt` lignes 42 et 97 (`setActive`) et `AudioRecorder.ios.kt` lignes 6 et 43 (`kAudioFormatLinearPCM`). Ces erreurs précèdent les tests et ne constituent pas un échec des assertions XCTest.

La revue par lecture précédente vérifiait les modules et la disponibilité des API Apple ; elle n’avait pas vérifié la résolution de chaque symbole dans les bindings Kotlin/Native générés. Son absence de défaut identifié ne prouvait donc pas la compilabilité. Le compilateur fournit ici une preuve plus forte et révèle deux erreurs d’import concrètes manquées par cette revue :

- `AudioPlayer.ios.kt` importe explicitement les classes AVFAudio, mais pas l’extension de catégorie `setActive`. Ajout ciblé de **`import platform.AVFAudio.setActive`**. Le recorder importe déjà `platform.AVFAudio.*` et ne reçoit pas cette erreur. Kotlin explique que les [membres de catégories Objective-C sont importés comme extensions Kotlin](https://kotlinlang.org/docs/native-objc-interop.html#extensions-and-category-members) ; importer la classe seule ne met pas ces extensions en portée.
- `AudioRecorder.ios.kt` cherchait le format PCM dans AudioToolbox. Apple classe ce symbole dans [Core Audio Types](https://developer.apple.com/documentation/coreaudiotypes/kaudioformatlinearpcm), et la [définition de plateforme Kotlin 2.2.21](https://github.com/JetBrains/kotlin/blob/v2.2.21/kotlin-native/platformLibs/src/platform/ios/CoreAudioTypes.def) expose ce module sous `platform.CoreAudioTypes`. Import corrigé en **`platform.CoreAudioTypes.kAudioFormatLinearPCM`**.

Seuls ces deux imports ont été modifiés pour la correction. Aucun build concurrent ni commit n’a été lancé par ce lot. La compilation iOS suivante doit confirmer la résolution des bindings ; tant que ce résultat n’existe pas, ces corrections ne sont pas présentées comme un build réussi.

## Distribution Apple

Apple impose Xcode 26 ou ultérieur et les SDK 26 aux envois App Store Connect depuis le 28 avril 2026. Le palier proposé répond à cette contrainte de chaîne, sous réserve de validation réelle du build. [Exigences Apple](https://developer.apple.com/news/upcoming-requirements/).

L'équipe Apple Developer, le certificat et le profil de provisionnement restent des prérequis manquants pour signer et exporter une application appareil. Une archive non signée et une application simulateur ne sont pas une IPA installable ; la migration de versions ne fournit pas ces éléments de signature.
