# Exécution de la finalisation multiplateforme

Reprise automatique autorisée le 19 octobre 2026 à partir de 20 h 20 (Douala).
Plan de contenu : `superpowers/plans/2026-10-19-source-complete-curriculum.md`.
Ce registre décrit le travail réel ; les tâches non cochées restent à terminer.

## Décisions de réalisation

- Le périmètre a été étendu par l'utilisateur aux interfaces et livrables Windows,
  Android et iOS. Les contraintes de versions du premier plan restent provisoires
  face aux exigences des plateformes et doivent être réévaluées avant distribution.
- Skill Dispatching Parallel Agents : trois domaines indépendants et fichiers
  distincts (cours 18.01SC, écrans, hôtes/builds). Le parent intègre, teste et
  contrôle la progression. Les contenus des autres cours restent requis.
- La progression est locale à chaque appareil, avec stockage natif. Aucun cloud
  ou compte utilisateur n'est prétendu configuré.
- La première réussite attribue les XP une seule fois. Chaque exercice doit être
  correctement résolu ; une réponse fausse reçoit une correction et un nouvel
  essai. Aucun type d'exercice non pris en charge ne valide silencieusement.
- Chaque cours commence indépendamment ; l'ordre s'applique dans le cours et
  les prérequis explicites restent respectés.
- Une sauvegarde illisible reste intacte : l'application signale le problème
  et conserve la nouvelle progression pour la session, sans écraser l'original.

## État de cette reprise

- [x] Relire le plan, l'inventaire et l'état Git au commit `ecc43da`.
- [x] Préserver puis intégrer le thème de marque local dans `App.kt`.
- [x] Écrire la navigation adaptative PC/tablette/téléphone, les notes avant
  exercices et les aides défilantes. Validation visuelle encore requise.
- [x] Ajouter le contrat de sauvegarde et les implémentations Windows, Android
  et iOS ; ajouter les métadonnées de cours et séances.
- [x] Écrire les tests de progression/relecture/XP/prérequis/erreurs/séries.
  Le premier build rouge identifie les API d'intégration manquantes ; les tests
  doivent encore passer et leur sensibilité aux régressions doit être contrôlée.
- [x] Écrire l'hôte SwiftUI et le pont Compose, corriger l'icône Android absente,
  ajouter les jobs CI Windows/Android/iOS et leurs contrôles.
- [x] Terminer et contrôler les 102 leçons de 18.01SC, puis les intégrer.
- [x] Valider le contrat de contenu, son compilateur et les erreurs de couverture.
- [ ] Terminer les autres cours OSSU et les contenus TCF (audio/production inclus).
- [ ] Vérifier les tests et builds locaux et les runs CI des trois plateformes.
- [ ] Inspecter l'application Windows et les preuves visuelles mobiles.
- [ ] Livrer les artefacts vérifiés et signaler les prérequis de signature iOS.

## Risques identifiés

- Kotlin 2.0.0/Xcode 15.3 : une archive de développement non signée ou une app
  simulateur ne prouve pas une distribution App Store. Les exigences Apple
  d'octobre 2026 nécessitent une vérification/migration coordonnée.
- Le runner macOS 14 est annoncé en retrait le 2 novembre 2026 ; ne pas laisser
  une nouvelle chaîne iOS durable dépendre de cette image sans plan de migration.
- Le catalogue historique de 33 leçons est un échantillon et n'est pas le cursus
  exhaustif demandé. Les manifestes seuls ne constituent pas des leçons rédigées.

## Preuves de la reprise du 19 octobre, 20 h 20–20 h 50 (Douala)

- `python -m unittest discover -s tools -p test_validate_curriculum.py` :
  9 tests réussis, dont couverture manquante, identifiants doubles, réponses
  invalides et caractères Kotlin à échapper.
- `python tools/validate_curriculum.py --generate` : 1 cours validé,
  102 leçons, 236 exercices ; couverture OSSU globale encore fausse.
- Gradle 8.5 / Java 17 local, `:composeApp:desktopTest` : 12 tests réussis,
  zéro échec le 19 octobre à 19:46 UTC. Régression de série expirée reproduite
  par un test rouge, puis corrigée et test vert. Contrôle négatif précédent :
  désactivation temporaire de la sauvegarde et garde d’idempotence → 4 échecs ;
  code restauré avant vérification finale.
- `:composeApp:createDistributable` : BUILD SUCCESSFUL, paquet natif produit.
  L’ancien exécutable de build portait ReadOnly ; attribut retiré uniquement
  sur ce fichier de build pour permettre sa régénération.
- Revue indépendante : trois défauts de navigation/défilement corrigés ;
  requêtes XCTest adaptées aux éléments d’accessibilité Compose fusionnés.
  Aucune erreur mathématique importante relevée dans la revue des exercices,
  sans prétendre fournir une preuve exhaustive de toutes les réponses.
- The Missing Semester, édition 2026 : neuf séances officielles identifiées,
  rédaction en cours. Les autres cours OSSU et le TCF restent à compléter.
- Les jobs Android/iOS nouvellement écrits ne sont pas encore prouvés sur CI.
