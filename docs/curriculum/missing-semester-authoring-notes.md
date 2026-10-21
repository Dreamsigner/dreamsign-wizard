# The Missing Semester — édition 2026

Consultation : 2026-10-19.

## Choix et décompte de l’édition

- La page d’accueil https://missing.csail.mit.edu/ affiche l’édition 2026.
- L’index annuel figé https://missing.csail.mit.edu/2026/ énumère neuf séances, du 12 au 23 janvier 2026. La section « Class structure » de la première séance confirme neuf conférences d’une heure.
- Les sujets historiques de 2019/2020 présentés séparément sur l’accueil ne sont pas ajoutés aux séances 2026. Ils restent des ressources complémentaires, et non des cours manquants inventés pour cette édition.
- Une unité : `mit_missing_semester_2026`. Une leçon originale par séance : neuf leçons, 51 exercices. Aucun examen ou révision distinct ne figure dans la séquence 2026 retenue ; toutes les entrées sont de type `lesson`.

## Méthode de rédaction

L’index annuel a fourni les titres et liens exacts du manifeste. Chaque page de séance a ensuite été ouverte ; ses sections de matière nécessaires aux explications et aux exercices ont été lues, avec récupération du HTML pour une empreinte de consultation. La rédaction ne provient donc pas des seuls titres de l’accueil. Aucun visionnage intégral des conférences n’est revendiqué. Les textes sont une rédaction pédagogique française originale ; les exercices sont des questions de raisonnement ou de lecture de code, avec quatre choix, correction et indice, plutôt qu’une prétention de reproduire tous les ateliers ouverts du cours.

Les exemples de shell sont explicitement Bash/Unix. Ils ne sont pas présentés comme des commandes PowerShell. Les hypothèses d’existence de dossiers, de réussite des déplacements et de curseur Vim sont indiquées lorsque le résultat en dépend. Les commandes montrées dans les exercices ne sont pas exécutées automatiquement par l’application.

Les recommandations de produits et de modèles qui peuvent évoluer rapidement ne sont pas transformées en classements durables. La leçon sur les agents enseigne des contrats, une boucle de retour et des limites de contexte. La leçon sur la livraison borne les règles SemVer à une bibliothèque stable respectant cette convention. Aucune affirmation selon laquelle le formatage, la couverture ou un résultat IA prouverait la correction totale n’est faite.

## Matière consultée par séance

| N° | Date officielle | Page et titre exacts | SHA-256 HTML abrégé | Sections utilisées |
|---:|---|---|---|---|
| 1 | 2026-01-12 | [Course Overview + Introduction to the Shell](https://missing.csail.mit.edu/2026/course-shell/) | `e2b0b59eae326af6` | Bash : chemins, guillemets, flux, redirections, tri et regroupement, codes de sortie. |
| 2 | 2026-01-13 | [Command-line Environment](https://missing.csail.mit.edu/2026/command-line-environment/) | `a710c5b753551fbd` | Arguments et environnement ; signaux ; SSH ; clés publiques ; sessions tmux. |
| 3 | 2026-01-14 | [Development Environment and Tools](https://missing.csail.mit.edu/2026/development-environment/) | `81a1b1b1f006b84f` | Modes et objets de texte Vim ; serveurs de langage et environnement ; assistance IA. |
| 4 | 2026-01-15 | [Debugging and Profiling](https://missing.csail.mit.edu/2026/debugging-profiling/) | `11dc95b33b0f3b2f` | Reproduction ; points de surveillance ; trace système ; relecture ; profilage et mesures. |
| 5 | 2026-01-16 | [Version Control and Git](https://missing.csail.mit.edu/2026/version-control/) | `4b9f0cf4a01b235f` | Objets et références ; HEAD ; index et travail ; différences ; graphes et fusion. |
| 6 | 2026-01-20 | [Packaging and Shipping Code](https://missing.csail.mit.edu/2026/shipping-code/) | `e862e7d1a5d2dc73` | Dépendances et résolution ; paquet wheel ; SemVer ; image et conteneur ; configuration. |
| 7 | 2026-01-21 | [Agentic Coding](https://missing.csail.mit.edu/2026/agentic-coding/) | `b007b92b05de62fa` | Boucle outils-modèle ; objectif observable ; contexte ; worktrees ; confidentialité. |
| 8 | 2026-01-22 | [Beyond the Code](https://missing.csail.mit.edu/2026/beyond-code/) | `8564833c39148e7a` | Raisons et contraintes ; rapports reproductibles ; modifications cohérentes ; revue. |
| 9 | 2026-01-23 | [Code Quality](https://missing.csail.mit.edu/2026/code-quality/) | `a738204c1eaaa273` | Formatage et analyse statique ; assertions et propriétés ; couverture ; CI ; regex. |

## Contrôles effectués

- `validate_course` importé depuis `tools/validate_curriculum.py` : aucune erreur pour ce cours et son manifeste. Aucun fichier généré, Kotlin ou autre catalogue modifié dans ce lot.
- Neuf correspondances exactes : ordre, numéro, titre, type et URL source. Identifiants et énoncés uniques ; tous les exercices ont quatre choix distincts, index correct, correction et indice.
- 51 exercices : six exercices dans six leçons et cinq exercices dans trois leçons. Notes propres à chaque séance : de 115 à 129 mots.
- Douze contrôles de scénarios ont réussi : sept exécutions Bash isolées (argument avec espaces, transmission de "$@", &&/||, environnement enfant, séparation stdout/stderr, sort/uniq, ajout par >>), puis cinq contrôles exacts (normalisation du chemin, quatre réponses du motif regex, index de fusion, intersection de contraintes de versions et quatre combinaisons de CI).
- Les questions de Git, de signaux, de serveur de langage et d’architecture sont relues contre les sections officielles citées. Aucune connexion SSH réelle, modification globale, publication, création de commit ou construction d’application n’a été effectuée pour les exercices.
- La validation du contenu ne constitue pas une vérification visuelle du rendu desktop ; celle-ci appartient à l’intégration principale.

## Attribution

Référence : Anish Athalye, Jon Gjengset et Jose Javier Gonzalez, The Missing Semester of Your CS Education, MIT IAP 2026. Le site indique une licence CC BY-NC-SA pour ses matériaux : https://missing.csail.mit.edu/ . Les questions et corrections présentes dans le JSON sont rédigées pour ce parcours ; les URL permettent de revenir aux conférences et ateliers originaux.
