# Dreamsign Wizard — Kotlin/Compose

- Direction actuelle : faire évoluer cette application vers le tutorat IA par objectifs, inspiré de LearnHarness ; ne pas repartir de leur code ni migrer vers Electron/Next.js.
- Lire `D:\Coding\DREAMSIGN-WIZARD.md`, `docs/product/2026-10-26-learning-studio.md` et `tasks/todo.md` pour le bilan, les limites et la suite.
- Le code est préparé pour la version 2.0.0 et le nouveau nom du dépôt. Une autre copie de travail existe sous `D:\Coding\_Codex\Chats-hors-projet\2026-10-19\c\dreamsign-wizard` : vérifier les deux états avant tout transfert, sans écraser les changements ni déplacer les historiques privés.

- Lire `README.md`, les fichiers Gradle et `.github/workflows/build.yml` avant modification.
- Conserver Kotlin Multiplatform / Compose et les cibles documentées ; ne pas imposer une migration d'architecture.
- Aucun wrapper Gradle n'est fourni. La CI utilise Java 17 et Gradle 8.11.1 : `gradle :composeApp:desktopTest :composeApp:createDistributable` depuis la racine ; pour compiler seulement : `gradle :composeApp:compileKotlinDesktop`.
- Avant exécution locale, vérifier les dépendances installées ; demander accord pour installer ou télécharger ce qui manque.
- Vérifier `git status --short` et préserver les changements existants.
- Pour toute tache substantielle, lancer un sous-agent executeur par defaut. Ne travailler seul que pour une tache triviale ou si le cout de coordination est manifestement superieur a celui de l execution directe; expliquer ce choix avant de commencer.
- Sélectionner les skills disponibles Kotlin, Compose Multiplatform et tests selon la modification ; ne pas charger toute la bibliothèque.
- La progression des cours et les séances IA disposent d'une sauvegarde locale Windows, dans des fichiers distincts. La clé IA reste en mémoire jusqu'à la fermeture. Vérifier les adaptateurs de chaque plateforme avant d'annoncer une capacité hors Windows ou un import documentaire.
- Le plan pédagogique structuré, le graphe de connaissances et la pratique adaptative restent à développer. Ne pas présenter le tuteur conversationnel actuel comme une parité complète avec LearnHarness.
- Distinguer code synchronisé, distribution locale et release installable ; cette synchronisation ne publie ni n'installe une mise à jour.

## Publication après chaque fonctionnalité terminée

- Décision utilisateur du 1er novembre 2026 : une fonctionnalité terminée pour Windows doit être accompagnée d'une release installable publiée, sans attendre un nouveau rappel de publication. Cette demande autorise les publications ordinaires des fonctionnalités achevées dans ce dépôt.
- Avant de déclarer la fonctionnalité terminée, choisir une version, mettre à jour les notes et la version du paquet, valider les tests adaptés, pousser le code et le tag stable, attendre le workflow de packaging, puis vérifier la release GitHub, l'installateur et sa somme SHA-256. Si un contrôle échoue, réparer et reprendre la publication avant de conclure.
- La version et les notes doivent décrire les changements réellement inclus. Incrémenter la version mineure pour une nouvelle capacité, la version corrective pour une correction compatible, et la version majeure pour une rupture de compatibilité explicitement assumée. Ne pas réutiliser un tag publié.
- Conserver les protections de mise à jour et l'identité d'upgrade Windows. Une release GitHub n'est pas prouvée par un simple push, un tag ou un artefact CI. Signaler précisément tout blocage empêchant sa publication.
