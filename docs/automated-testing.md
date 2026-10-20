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

Les trois tests Compose Android instrumentés s'exécutent dans l'émulateur CI
avec une activité et un stockage mémoire de test : voir `android-ui-testing.md`.
Ils n'utilisent pas Computer Use. Ils sont distincts des tests hors écran JVM.
Les tests Kotlin iOS et les XCTest de l'hôte iPhone/iPad s'exécutent sur le runner
macOS du workflow `Build`, qui construit aussi l'archive appareil non signée.

## Limites des preuves

Un test média simulé ne prouve pas la qualité sonore, l'autorisation système,
l'interruption réelle ni le fonctionnement d'un microphone physique. Les captures
des tests prouvent un rendu donné ; elles ne couvrent pas toutes les tailles,
la lecture d'écran native ou chaque écran du catalogue. Les rapports CI du commit
livré doivent être consultés avant de déclarer Android ou iOS vérifié. Une archive
iOS non signée n'est pas installable sur un iPhone réel.
