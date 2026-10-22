# Vérification sans Computer Use

Les tests composent les vrais écrans Kotlin/Compose et agissent sur leurs nœuds
sémantiques. Ils ne déplacent pas la souris, n'ouvrent pas de fenêtre et ne
dépendent pas de captures pour choisir leurs actions. Le rendu desktop est
explicitement hors écran (`java.awt.headless=true`).

## Commandes reproductibles

Prérequis : JDK 17 et Gradle 8.11.1. Depuis la racine du dépôt :

```sh
gradle :composeApp:desktopTest --no-daemon --console=plain
python -m unittest discover -s tools -p 'test_*.py'
python tools/validate_curriculum.py
```

Pour forcer une nouvelle exécution de tous les tests desktop malgré le cache :

```sh
gradle :composeApp:desktopTest --rerun-tasks --no-daemon --console=plain
```

Les rapports HTML sont dans `composeApp/build/reports/tests/desktopTest/`, les
résultats XML dans `composeApp/build/test-results/desktopTest/`. Les tests
`LearningUiTest` et `AudioPracticeUiTest` produisent également des PNG dans
`composeApp/build/reports/ui-snapshots/`. La CI archive ces rendus dans
`Windows-checks` et `Android-checks`.

## Ce que les tests exercent

- Navigation cours/profil, notes de cours, aide et retour avec choix conservé.
- Mauvaise réponse, nouvelle tentative, correction, fin de leçon et XP une seule
  fois, y compris après reconstruction du modèle à partir du stockage injecté.
- Compréhension écrite et production écrite avec limites de mots et relecture.
- Positions des contrôles en viewport compact (400 × 900) et large (1400 × 900),
  et rendu de la vraie consigne de production orale à largeur compacte.
- Ouverture indépendante de la première leçon de chaque cours du catalogue
  publié, capture de notes réelles, et choix du cours conservé après redémarrage.
- Activation au clavier de la navigation PC et affichage des numéros/type de
  séance, en distinguant un entraînement adapté d'un examen officiel.
- Écoute complète nécessaire, arrêt sans validation, ressource manquante,
  transcription accessible distincte de l'écoute.
- Capture simulée, erreur de sauvegarde puis reprise, réécoute, bilan et
  annulation lors du passage en arrière-plan.
- Travailleur JavaSound avec ligne audio factice, erreurs, annulations,
  remplacement et durée maximale ; format WAV et stockage atomique isolé.

`PracticeMedia` remplace ressources, lecture, capture, stockage et cycle de vie
pour les tests média. La progression est injectée séparément via `ProgressStore`.
Aucune fixture ne lit les enregistrements ou la progression de l'utilisateur.
Les règles de format WAV et de progression restent celles de l'application.

## Android et iOS

Les trois tests Compose Android instrumentés ont été exécutés dans l'émulateur
du run historique 39488554876 : voir `android-ui-testing.md`. Leurs sources sont
conservées, ainsi que celles des tests Kotlin iOS et des XCTest. Depuis la
décision Windows uniquement du 20 octobre 2026, ces jobs mobiles ne sont plus
exécutés par le workflow `Build`. Aucun succès iOS n'est déduit de leur présence.

## Contrôle du lanceur Windows distribué

Le lanceur accepte un mode de diagnostic séparé de l'interface :

```powershell
.\DuolingoKmpApp.exe --self-check --report "D:\chemin\nouveau-rapport.json"
```

Le fichier de sortie doit être nouveau et son dossier doit exister. Un fichier
existant n'est jamais remplacé. Ce mode charge les vrais objets du catalogue,
contrôle leurs identifiants et contenus requis, puis lit les vrais fichiers WAV
via le lecteur de ressources de l'application et calcule leurs SHA-256.
Il ne crée ni fenêtre, lecteur audio, microphone, modèle de progression ou
stockage utilisateur. Un échec renvoie un code processus non nul.

La CI Windows exécute ce mode sur le `.exe` produit avec son runtime embarqué,
avec délai maximal de 60 secondes. Elle archive `installation-check.json` dans
`Windows-checks`, en complément du contrôle Python des fichiers du paquet.
Cela vérifie le démarrage du lanceur et la résolution des ressources; les vrais
écrans sont vérifiés séparément par les tests Compose hors écran.

## Retours d’usage : cours et documents

Les régressions ajoutées couvrent la file de questions ratées, la rotation des
erreurs répétées, le retrait après une correction immédiate, l’écoute conservée
durant la reprise, l’ordre OSSU et les références de chaque exercice. Les tests
Compose vérifient l’objectif au-dessus de la question, le bouton d’explication,
la lecture de sections verrouillées sans XP et le rendu du véritable catalogue.
Le lecteur PDF utilise des documents originaux générés en mémoire dans les tests :
chargement, erreur/réessai, défilement vertical continu, zoom et fermeture des
sessions sont contrôlés. Une liste paresseuse ne rasterise que les pages
composées autour de la fenêtre visible. Le test d’un document de 200 pages
rejoint directement la page 100 puis défile vers la suivante sans pré-rendre
l’ensemble du manuel. Les dimensions de chaque page, rotation comprise, sont
connues avant le rendu pour conserver une hauteur stable pendant le chargement.
Les captures du lecteur utilisent des pages PDF réellement rendues par PDFBox.

Un test réseau distinct s’exécute uniquement sur demande :

```powershell
gradle :composeApp:sourcePdfSmokeTest
```

Ce test télécharge un support officiel MIT 18.06SC et l’ouvre avec le lecteur
de production, puis rend une page. Il est exclu de `desktopTest` pour que la
suite ordinaire ne dépende pas de la disponibilité d’un serveur tiers. Le run
local du 21 octobre 2026 a ouvert un PDF de quatre pages et rendu la première
en 1000 × 1294 pixels. Cela ne vérifie pas tous les PDF de l’index.

Le diagnostic du véritable `.exe` distribué teste aussi les moteurs LaTeX et
PDF sur du matériel original en mémoire : il expose `mathRenderingVerified`
et `pdfRenderingVerified`, exigés par la CI, sans ouvrir de fenêtre ni stockage
de progression. Les artefacts incluent maintenant les XML JUnit en plus du HTML.

## Limites des preuves

Un test média simulé ne prouve pas la qualité sonore, l'autorisation système,
l'interruption réelle ni le fonctionnement d'un microphone physique. Les captures
des tests prouvent un rendu donné ; elles ne couvrent pas toutes les tailles,
la lecture d'écran native ou chaque écran du catalogue. Android et iOS sont hors
du périmètre actif; leurs résultats historiques ne valident pas la version Windows
livrée. Une archive iOS non signée n'est pas installable sur un iPhone réel.

## Aperçus vidéo (mathématiques, Windows)

`VideoPreviewTest` vérifie les URL YouTube autorisées, le cache borné, les images
invalides/trop grandes, les cartes étroites/larges, le repli MP4, l’ouverture du
lien exact et l’activation clavier avec erreur navigateur. Ces tests utilisent
un chargeur injectable sans réseau. Les captures `video-preview-*.png` sont
produites dans `composeApp/build/reports/ui-snapshots` et archivées en CI.
L’image chargée dans le test est une fixture : le test ne certifie pas la
disponibilité en ligne d’une vidéo ni sa lecture dans le navigateur.

## Recherche dans le sommaire des mathématiques

`CourseContentsSearchTest` contrôle les accents français et la casse, les requêtes
à plusieurs mots, l’ordre des séances et le saut vers l’index d’origine d’une leçon
verrouillée. Les captures à 500/1100 px vérifient recherche, absence de résultats
et effacement. L’affichage historique hors maths ne reçoit pas de recherche.

