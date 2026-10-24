# Tester le paquet Windows

## Installer l’application

1. Dans une exécution GitHub Actions réussie de `main`, télécharger **AtelierApprentissage-Windows-Setup**.
2. Extraire l’archive, puis lancer `AtelierApprentissage-1.11.2-Setup.exe`.
3. Suivre l’assistant, puis utiliser le raccourci du Bureau ou du menu Démarrer.

L’installation est propre à l’utilisateur courant, par défaut dans
`%LOCALAPPDATA%/DuolingoKmpApp`. Les données de progression restent séparées,
dans `%LOCALAPPDATA%/AtelierApprentissage`. La version portable existante et
la version installée utilisent cette même sauvegarde : fermer l’une avant
d’utiliser l’autre. Aucun déplacement de données n’est nécessaire.

Pour désinstaller, utiliser les applications installées dans les paramètres
Windows. La progression n’est pas supprimée. Un installateur de version
supérieure peut remplacer l’application installée grâce à son identifiant
stable. La section Paramètres permet de vérifier et gérer les mises à jour
automatiques; voir ci-dessous.

L’exécutable n’est pas signé numériquement. `SHA256SUMS.txt` permet de vérifier
le téléchargement avec `Get-FileHash -Algorithm SHA256`.

## Utilisation portable

L’artefact **DuolingoKmpApp-Windows** reste un ZIP portable. Extraire aussi le
ZIP intérieur s’il est présent, puis lancer `DuolingoKmpApp.exe` en conservant
les dossiers `app` et `runtime`. Ne pas lancer l’exécutable dans l’archive.

Les deux distributions contiennent Java. Gradle et un JDK séparé ne sont pas
nécessaires. Les notes, exercices et documents audio sont intégrés; les supports
PDF externes et vidéos sont disponibles online.

## Vérifications utilisateur utiles

- Sélectionner une catégorie puis un cours et ouvrir une leçon disponible.
- Ouvrir ses notes et son aide, répondre, vérifier la correction et terminer.
- Fermer l'application puis la rouvrir : progression et cours choisi doivent
  être conservés.
- Redimensionner la fenêtre et utiliser Tab puis Entrée pour les contrôles.
- Dans les ateliers TCF, essayer l'écoute, la saisie et, si un microphone est
  disponible, l'enregistrement puis sa réécoute.
- Ouvrir « Lire le cours et les supports » : consulter une section via le
  sommaire, puis un support PDF disponible online : les pages s’enchaînent par
  défilement vertical, avec barre de défilement et zoom. Dans la zone de lecture,
  les touches Page haut/bas, les flèches, Début et Fin permettent aussi de naviguer.

L'exécution réelle du microphone, les réglages audio et le comportement du
gestionnaire de fenêtres restent distincts des tests Compose hors écran.
Les ateliers de production TCF utilisent une relecture personnelle, sans note
officielle ni correction automatique de langue. La couverture OSSU/TCF reste
partielle : voir [AUTHORED-COVERAGE.md](curriculum/AUTHORED-COVERAGE.md).

## Données conservées

Sur Windows, la progression se trouve dans
`%LOCALAPPDATA%/AtelierApprentissage/progress.json`. Remplacer le dossier du
paquet ne nécessite pas de supprimer cette sauvegarde. Une sauvegarde illisible
est préservée et l'application signale l'erreur. Il n'y a pas de synchronisation
entre appareils.

## Reprise vidéo et téléchargement avec audio français

Les positions YouTube et MP4 sont enregistrées séparément dans
`%LOCALAPPDATA%/AtelierApprentissage/video-playback.json`. Ce fichier ne modifie
pas `progress.json`. Un fichier illisible est conservé et un message signale
que sa position ne peut pas être enregistrée. La reprise ne remplace pas une
synchronisation entre appareils. Les vidéos terminées repartent du début.

Sous une carte YouTube, **Télécharger avec la piste audio française** examine
les pistes accessibles. Le doublage automatique de YouTube est accepté comme
une piste française existante; aucun doublage ni sous-titre traduit n’est
fabriqué par l’application. Sans piste française accessible, aucun téléchargement
n’est proposé. Le fichier MP4 est contrôlé avant publication à la destination;
un fichier existant exige une confirmation de remplacement.

Le poste doit disposer de `yt-dlp.exe`, `ffmpeg.exe`, `ffprobe.exe` et d’un moteur
JavaScript pris en charge par yt-dlp (Deno est utilisé sur le poste de validation).
Ces dépendances ne sont pas embarquées et ne sont pas installées automatiquement.
Les trois exécutables sont recherchés dans `PATH`. Pour un emplacement personnalisé,
les variables d’environnement `ATELIER_YT_DLP_PATH`, `ATELIER_FFMPEG_PATH` et
`ATELIER_FFPROBE_PATH` acceptent le chemin complet correspondant, avant lancement
de l’application. Un outil manquant est signalé dans le dialogue.

Vérification utilisateur : arrêter une vidéo à un instant identifiable, fermer
son lecteur puis le rouvrir; répéter après redémarrage de l’application. Pour le
téléchargement, choisir une vidéo qui propose effectivement une piste française
et vérifier le son du MP4 final. Les restrictions réseau ou les pistes que
YouTube n’expose pas à l’outil peuvent empêcher le téléchargement.

## Diagnostic sans ouvrir de fenêtre

Depuis PowerShell, dans le dossier extrait, choisir un nouveau nom de rapport :

```powershell
$rapport = Join-Path $PWD ('diagnostic-' + [guid]::NewGuid().ToString('N') + '.json')
$processus = Start-Process -FilePath '.\DuolingoKmpApp.exe' -ArgumentList @('--self-check', '--report', ('"{0}"' -f $rapport)) -PassThru -Wait -WindowStyle Hidden
$processus.ExitCode
Get-Content -LiteralPath $rapport
```

Le code de sortie attendu est `0`. Ce mode vérifie le catalogue, lit les
ressources audio intégrées et exerce les moteurs LaTeX/PDF avec des documents
originaux en mémoire. Il n'ouvre pas l'interface et n'accède pas à la
progression personnelle. Il refuse de remplacer un rapport existant. Il ne
vérifie ni les périphériques audio ni l'apparence d'une fenêtre réelle.

## Aperçus vidéo des mathématiques

Dans un cours de mathématiques, ouvrir les supports d’une leçon. Une carte affiche
le titre et la langue de chaque vidéo. « Ouvrir la vidéo » lance son lien exact
dans le navigateur. Une vignette YouTube est chargée lorsqu’elle est disponible;
les fichiers MP4 et les échecs réseau affichent « Aperçu non disponible ».
Les notes et exercices restent utilisables hors ligne. Le bouton « Lire dans l’app » ouvre désormais une fenêtre vidéo intégrée.

Les aperçus contactent uniquement `i.ytimg.com` en HTTPS, sans cookies ni clé API.
Le cache de huit images est limité à la session, sans enregistrement sur disque.
Une image ne peut dépasser 1 Mio ni 1280 × 720 pixels; les connexions et lectures
réseau sont limitées à quatre secondes chacune. Aucune vidéo n’est téléchargée.

## Mises à jour automatiques

Installer une première fois la version 1.11.2 depuis les [releases GitHub](https://github.com/Dreamsigner/duolingo-kmp-learning-app/releases/latest).
L’installation Windows standard `%LOCALAPPDATA%/DuolingoKmpApp` active les mises à jour automatiques.
Les copies portables et chemins d’installation personnalisés affichent que la mise à jour manuelle reste nécessaire.

- Vérification cinq secondes après le démarrage puis toutes les six heures. Le bouton de vérification permet de relancer la recherche.
- Seules les releases stables plus récentes et leurs fichiers attendus du dépôt officiel sont acceptés. Aucun compte GitHub n’est demandé.
- Après contrôle de taille et de SHA-256, l’application annonce la version prête. Si **Mises à jour auto** est activé, elle s’installe silencieusement à la fermeture, sans redémarrage du PC ni réouverture forcée de l’application.
- **Installer et redémarrer** ferme l’app, attend sa sauvegarde finale et relance l’app après installation. La case Auto désactivée empêche toute application automatique, y compris pour une version déjà téléchargée.
- Sans réseau, en cas de téléchargement incomplet, de checksum incorrect ou d’erreur GitHub, les cours restent accessibles et aucune nouvelle version n’est exécutée.

Les fichiers de travail et préférences restent dans `%LOCALAPPDATA%/AtelierApprentissage/updates`.
`last-result.json` et `updater.log` servent au diagnostic d’une installation échouée.
`progress.json` reste séparé et n’est pas remplacé par les paquets.
L’installateur reste non signé : les restrictions de sécurité de Windows ou d’un poste administré peuvent empêcher son exécution. Aucun réglage système n’est modifié par le programme de mise à jour.
Le checksum vérifie l’intégrité du fichier diffusé par GitHub; il ne remplace pas une signature d’éditeur indépendante.

### Publier une version compatible (maintenance)

Actualiser la version du paquet, les noms d’artefacts et la fixture de mise à niveau dans le workflow et son test, puis créer le tag `vMAJOR.MINOR.PATCH` correspondant. Le job de release vérifie que le tag stable et le commit source correspondent avant de publier l’EXE et `SHA256SUMS.txt`, après les contrôles Windows. Le paquet de fixture de version supérieure (actuellement 1.11.3) sert uniquement au test d’installation et n’est jamais publié. Les artefacts Actions seuls ne déclenchent aucune mise à jour.

## Lecteur vidéo intégré (mathématiques)

Dans les supports d’une leçon, choisir **Lire dans l’app**. La fenêtre vidéo
conserve les commandes du lecteur source : YouTube officiel ou lecteur MP4 de
WebView2. Utiliser les commandes de cette source pour lire, mettre en pause,
régler son/vitesse, sélectionner sous-titres ou qualité lorsqu’ils sont proposés.
Le bouton **Plein écran** masque l’en-tête de l’application et agrandit la fenêtre;
**Échap** restaure son placement précédent. Le lecteur suit le redimensionnement.
**Fermer la vidéo** libère le
lecteur natif pour arrêter sa lecture. Les cours restent consultables derrière
cette fenêtre. Aucune vidéo n’est recopiée ou réhébergée par l’application.

Le système Microsoft Edge WebView2 Runtime est un prérequis. Il n’est pas installé
automatiquement par l’application. En cas d’échec, suivre le lien Microsoft affiché
ou ouvrir la source dans le navigateur. Le lecteur web utilise un profil distinct
de celui du navigateur externe; les choix de confidentialité et restrictions de
YouTube restent ceux du fournisseur. L’application ne les accepte ni ne les contourne.
Les vidéos restent disponibles online, avec les limitations du réseau et de leur source.

Le composant natif `ca.weblite:webview:1.0.10` est figé (JAR Maven Central mesuré :
985 639 octets). Il réutilise le moteur système; aucun navigateur Chromium complet
n’est ajouté au paquet. Le moteur peut mettre des flux en cache dans son profil
local. La progression d’apprentissage reste dans son fichier séparé.

## Contrôle de l’espace de cours (1.11.2)

Dans Cours, vérifier le panneau sombre du cours, sa progression et Continuer, puis l’accès aux supports. La loupe recherche dans le titre et l’objectif des leçons; Toutes, Disponibles et Terminées conservent l’ordre d’origine et les prérequis. Une recherche sans résultat propose Réinitialiser. Changer de cours remet recherche et filtres à zéro.

Les tests Compose couvrent ces parcours, le clavier, les leçons verrouillées et la navigation vers Profil/Paramètres. Les captures avec le catalogue réel sont produites à 400, 900 et 1400 pixels dans `composeApp/build/reports/ui-snapshots/learning-path-*.png`. Elles utilisent une sauvegarde en mémoire et ne lisent pas la progression du poste.

Les transitions de navigation utilisent un fondu traversant la surface papier : l’écran sort avant l’apparition du suivant, pour éviter les textes superposés. Le survol et l’appui font évoluer brièvement les couleurs, l’échelle et les ombres des contrôles actifs. Vérifier les clics répétés sur Cours/Profil/Paramètres et le changement de cours; le dernier choix doit rester actif. Une capture native à horloge contrôlée documente un état intermédiaire de transition. Ces contrôles valident le comportement et le rendu; ils ne constituent pas une mesure de fréquence d’affichage sur chaque matériel.

## Sommaire adaptatif, compteurs et cache (1.11.2)

Le sommaire reste en panneau latéral à partir de1000dp de largeur du lecteur, avec défilement indépendant et recherche/favoris communs au popup compact. Tester le saut vers une leçon, le redimensionnement et le retour au cours. Les compteurs doivent rester visibles sans chevaucher le sélecteur de domaine, y compris avec de grands nombres.

Le test d’installation CI vérifie aussi la suppression de la copie d’installateur dans le cache et du payload, tout en conservant l’installateur externe d’origine. Les temporaires récents et les fichiers inconnus restent intacts; progression/préférences, dernier résultat et journal ne sont pas supprimés. Le cache système Windows Installer et les téléchargements manuels sont hors périmètre.

Le candidat1.11.2 corrige la taille des fractions mais n’est pas publié : le contrôle vidéo natif échoue en CI. Pour examiner son rendu localement, utiliser la distribution construite dans `composeApp/build/compose/binaries/main/app/DuolingoKmpApp/`; les mises à jour publiques restent sur1.11.0.

## Unités physiques et fractions

Le rendu partagé conserve les unités composées sur une ligne (`12 m/s`, `9,8 m/s²`, `km/h`), y compris les unités rédigées avec une fraction LaTeX en caractères droits dans le catalogue. Les fractions numériques et algébriques restent verticales : dans `1/2 m/s`, seule la valeur numérique est une fraction.

Le test du catalogue classe séparément unités et fractions, et continue de signaler les slashs non reconnus. Les captures natives `linear-units-readable-fractions-360.png` et `linear-units-readable-fractions-1000.png`, dans `composeApp/build/reports/ui-snapshots/`, comparent les unités simples, le cas LaTeX du cursus et les vraies fractions. Ce correctif est disponible dans la distribution locale; il ne constitue pas une nouvelle release publiée.

## Partager un défi

Pendant un exercice compatible, **Partager ce défi** ouvre un aperçu neutre : titre de leçon, question, choix et, si nécessaire, formule ou extrait de code. **Copier le texte** prépare le message pour une autre application. Sur Windows, **Enregistrer PNG** ouvre le sélecteur de fichier; un fichier existant nécessite confirmation avant remplacement. Annuler n'annonce pas une réussite, et une erreur permet de réessayer.

La carte ne contient ni réponse correcte, ni sélection de l'élève, ni indice, ni explication, ni progression personnelle. Le partage reste disponible après vérification d'une réponse, sans emporter le corrigé. Les formats nécessitant un audio, un passage de lecture ou une autre interaction sont exclus, ainsi que les questions trop longues pour ce format. Le contenu éligible est conservé en entier; il n'est pas tronqué pour créer une carte.

Sur une fenêtre étroite ou courte, faire défiler l'aperçu et vérifier que les commandes restent accessibles. L'image doit contenir tous les choix, y compris ceux hors de la zone visible de l'aperçu. Le partage ne crée pas de défi interactif sur Internet : l'image ou le texte est envoyé par l'utilisateur depuis son application de messagerie.
