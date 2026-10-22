# Consommation de tokens par tâche

Mesures natives Codex. Les chiffres couvrent seulement la période indiquée par chaque relevé.
Une tâche reste ouverte jusqu’au relevé suivant : la réponse finale et le dernier appel ne sont pas encore comptés au moment de leur propre mesure.

Les entrées incluent le contexte relu à chaque appel. Le cache est une partie des entrées; le raisonnement est une partie des sorties. Ne pas les additionner une seconde fois.
Ces tokens ne donnent ni un coût en dollars du forfait ChatGPT ni un pourcentage de quota.

Le relevé provient du journal actif, lu sans modification. Seuls les compteurs agrégés sont publiés : ni conversations, chemins locaux, identifiants de session ou secrets.
Les snapshots sont cumulatifs dans une tâche : retenir le dernier, ne jamais les sommer. Les anciennes tâches sans bornes fiables ne sont pas reconstruites artificiellement.

## Mise à jour du registre

Avant une tâche : annoncer une estimation de sorties, puis `python tools/task_tokens.py begin Txxx --title 'Titre' --estimate MIN MAX`. Utiliser `--marker` pour inclure l’inspection déjà faite depuis une requête utilisateur identifiable.
Pendant la tâche : `python tools/task_tokens.py checkpoint Txxx`. À la frontière suivante : `python tools/task_tokens.py close Txxx`, puis ouvrir la nouvelle tâche. Éviter les intervalles qui se chevauchent.
Le script utilise le journal du thread courant ayant la modification la plus récente, ou `--source` explicite. L’état local est ignoré par Git dans `build/task-tokens`; le JSON partageable se trouve dans `docs/metrics/task-tokens.json`.

| Tâche | État | Estimation sorties | Entrées | Cache inclus | Sorties | Raisonnement inclus | Total | Mesuré jusqu’au (UTC) |
|---|---|---:|---:|---:|---:|---:|---:|---|
| T001 · Suivi natif des tokens par tâche | closed_at_checkpoint | 3000–6000 | 1243820 | 1231232 | 5771 | 1272 | 1249591 | 2026-10-22T03:38:40.299Z |
| T002 · Recherche dans le sommaire des cours de maths | in_progress | 4000–7000 | 2405277 | 2383104 | 9304 | 3471 | 2414581 | 2026-10-22T03:48:03.328Z |
