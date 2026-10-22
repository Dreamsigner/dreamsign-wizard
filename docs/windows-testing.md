# Tester le paquet Windows

## Installer l’application

1. Dans une exécution GitHub Actions réussie de `main`, télécharger **AtelierApprentissage-Windows-Setup**.
2. Extraire l’archive, puis lancer `AtelierApprentissage-1.3.0-Setup.exe`.
3. Suivre l’assistant, puis utiliser le raccourci du Bureau ou du menu Démarrer.

L’installation est propre à l’utilisateur courant, par défaut dans
`%LOCALAPPDATA%/DuolingoKmpApp`. Les données de progression restent séparées,
dans `%LOCALAPPDATA%/AtelierApprentissage`. La version portable existante et
la version installée utilisent cette même sauvegarde : fermer l’une avant
d’utiliser l’autre. Aucun déplacement de données n’est nécessaire.

Pour désinstaller, utiliser les applications installées dans les paramètres
Windows. La progression n’est pas supprimée. Un futur installateur de version
supérieure peut remplacer l’application installée grâce à son identifiant
stable; la recherche et le téléchargement automatiques de mises à jour ne sont
pas encore implémentés.

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
Les notes et exercices restent utilisables hors ligne. La lecture intégrée dans
l’application n’est pas proposée.

Les aperçus contactent uniquement `i.ytimg.com` en HTTPS, sans cookies ni clé API.
Le cache de huit images est limité à la session, sans enregistrement sur disque.
Une image ne peut dépasser 1 Mio ni 1280 × 720 pixels; les connexions et lectures
réseau sont limitées à quatre secondes chacune. Aucune vidéo n’est téléchargée.
