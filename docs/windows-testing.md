# Tester le paquet Windows

## Ouvrir l'application

1. Dans le run GitHub Actions du commit à tester, télécharger l'artefact
   **DuolingoKmpApp-Windows**.
2. Extraire le téléchargement. S'il contient encore un fichier
   `DuolingoKmpApp-Windows.zip`, extraire aussi cette archive.
3. Conserver ensemble `DuolingoKmpApp.exe`, `app` et `runtime`, puis ouvrir
   `DuolingoKmpApp.exe`. Ne pas lancer l'exécutable depuis l'archive.

Le paquet contient son environnement Java. Gradle et un JDK séparé ne sont
pas nécessaires pour le tester. Il s'agit d'un dossier d'application portable,
pas d'un installateur. Les pages de référence des cours nécessitent Internet;
les leçons, corrections et documents audio sont intégrés.

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
