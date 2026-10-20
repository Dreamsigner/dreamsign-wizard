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

## Reprise après interruption de quota — 20 octobre

- L’exécution et les sous-agents ont été interrompus par la limite d’utilisation.
  Les modifications ont été conservées ; la finalisation n’a pas été annoncée.
- Commit `d5425c4` poussé. Run GitHub Actions `39313889860` : Windows SUCCESS,
  Android SUCCESS (APK, lint, tests et lancement émulateur), iOS compilation et
  tests Kotlin réussis mais XCTest échoue à trouver les contrôles d’interface.
  Capture iPhone extraite et inspectée : le contenu et les boutons sont visibles.
  L’exposition de l’arborescence d’accessibilité Compose aux tests est corrigée
  localement ; nouveau run requis, sans suppression des assertions.
- Windows inspecté en 1266×833 et 400×833 : accueil, notes, deux réponses,
  corrections, réussite +20 XP, déverrouillage et profil. Fichier réel de
  progression retrouvé avec la première séance terminée. La navigation clavier
  a été exercée ; l’arbre UI Automation Windows reste limité au cadre de fenêtre,
  donc aucune compatibilité complète avec lecteur d’écran n’est revendiquée.
- 114 leçons/ateliers et 290 exercices validés/générés : MIT 18.01SC (102/236),
  Missing Semester 2026 (9/51), atelier écrit TCF original (3/3).
- 15 tests Kotlin desktop réussis : parcours de toutes les leçons publiées avec
  relecture du stockage, logique de progression, fichiers et production écrite.
  10 tests du validateur Python réussis. Rejeu indépendant des scripts conservés :
  95 calculs exacts SymPy et 12 scénarios Missing Semester réussis.
- Preuves mathématiques reproductibles : `tools/check_calculus_math.py`
  (SymPy 1.12) ; shell/scénarios : `tools/check_missing_semester_scenarios.py`.
- 18.06SC Fall 2011 : 40 séances rédigées, intégrées et contrôlées ; 131 exercices,
  83 calculs exacts rejoués et liés aux réponses publiées. Script reproductible
  `tools/check_linear_algebra_math.py`, indépendant des fichiers temporaires.
- Catalogue intégré porté à 154 leçons/ateliers et 421 exercices. Les 16 tests
  desktop passent après intégration et correction de la saisie : sauvegarde des
  brouillons regroupée après 600 ms sur un worker, écritures sérialisées, vidage
  à la sortie/navigation et état visible dans l’éditeur.
- Inventaire OSSU enrichi : 63 références, 25 index vérifiés, huit compositions
  de spécialisations, trois index partiels, six références regroupées sous deux
  alternatives, 21 séquences non établies. Ce sont des preuves de sources,
  pas autant de cours pédagogiques rédigés. Le collecteur préserve désormais les
  manifestes et audits existants ; ses nouvelles collectes sont des candidats.

## Reprise du 20 octobre à 07 h 18 (Douala)

- Nouvelle interruption de quota entre les reprises ; aucun cours supplémentaire
  n’est annoncé terminé pendant cette interruption.
- Run `39353730474` du commit `c0c82e9` : Windows et Android SUCCESS, iOS FAILURE.
  L’hôte contenait le mode d’accessibilité XCTest, mais le fichier de test avec
  la variable d’environnement n’avait pas été inclus dans le commit. Cette
  omission d’intégration est corrigée séparément avant une nouvelle validation.
- Lot local suivant préservé : Python MIT 6.100L (26 leçons/104 exercices),
  écoute originale TCF (trois documents/six questions), lecteurs audio natifs,
  gestion arrière-plan, transcription et mode lecture accessible identifié dans
  le profil. Catalogue local : 183 leçons/ateliers, 531 exercices. Tests desktop
  verts avant interruption ; nouveaux builds natifs et inspection encore requis.
- Les contributions 6.042J et enregistrement oral ont été interrompues au début.
  Ne pas les traiter comme terminées, ni ajouter un écran de production orale
  sans capture/relecture réelle et contrôle des permissions natives.

## Reprise du 20 octobre — lot audio et migration

- `c3439cc`, run `39379770520` : les trois jobs Windows/Android/iOS sont
  SUCCESS. XCTest iPhone (8,315 s) et iPad (11,330 s), zéro échec ; archive
  appareil non signée réussie à 06:38:29 UTC. Cette preuve concerne l’ancienne
  chaîne, pas les fichiers de migration encore locaux.
- Catalogue local : 226 leçons/ateliers, 711 exercices, neuf unités. Ajouts
  rédigés : MIT 6.042J Fall 2010 (28/117), Learn Prolog Now! (12 chapitres/60),
  trois ateliers oraux originaux TCF. Prolog a été relu manuellement ; aucun
  moteur installé et aucune exécution Prolog revendiquée.
- Windows réel : lecture d’un document original TCF de 24,07 s jusqu’au callback
  de fin, puis réponse et correction avec transcription. La progression utilisateur
  antérieure (20 XP, première séance de calcul) est préservée. Cela prouve le
  chemin de lecture natif, pas une évaluation subjective de l’intelligibilité.
- Capture orale implémentée sur les trois plateformes, WAV privé, sauvegarde
  atomique et réécoute avant auto-évaluation. Revue indépendante : corrections
  de stop synchrone iOS, course Android de finalisation, interruption native,
  sauvegarde réessayable et état occupé en arrière-plan. Aucun micro activé par
  les outils ; les permissions et captures natives restent à vérifier réellement.
- 13 tests Python du validateur réussis. Les 22 tests desktop du lot précédent
  passaient avant ajout de l’oral ; les nouveaux tests d’oral et de stockage isolé
  attendent la nouvelle compilation. Ne pas annoncer ces nouveaux tests verts.
- Migration préparée : Kotlin 2.2.21, Compose 1.9.3, AGP 8.10.1, Gradle 8.11.1,
  Xcode 26.0.1 sur runner macOS 15 ARM64. ZIP Gradle téléchargé dans work et
  SHA-256 officiel vérifié. Première résolution bloquée longtemps en lecture
  HTTP/TLS (thread dump), relancée avec journal info et délais réseau bornés.
  Aucun résultat de build de cette nouvelle chaîne n’est encore établi.
- Couverture OSSU toujours partielle. Le cours 18.404J est en rédaction ; son
  calendrier contient 25 conférences et deux examens, soit 27 séances, et non
  25 séances totales. Aucun cours en rédaction n’est publié par anticipation.

### Intégration suivante : 256 leçons/ateliers, 835 exercices

- MIT 18.404J Fall 2020 rédigé : 25 conférences et deux entraînements associés
  aux examens du calendrier, soit 27 leçons/112 exercices. Les sujets 2020 ne
  sont pas publiés ; les exemples historiques restent explicitement datés.
  `tools/check_computation_theory.py` : 32 calculs finis exacts PASS, réponses
  liées au JSON courant. Aucune validation automatique de preuves universelles.
- Lecture TCF : trois ateliers originaux et douze QCM sur quatre textes fictifs.
  `ReadingChoice` sépare document et question pour afficher le passage en corps
  de lecture normal. Le catalogue généré contient onze unités ; couverture
  OSSU et préparation TCF toujours partielles. 19 tests Python réussis.
- `0607e5a`, run `39383746011` : trois jobs FAILURE. Android : callback
  `onRequestPermissionsResult` exige `Array<String>` avec la nouvelle chaîne.
  Windows : un test de débit WAV mettait à zéro un octet déjà nul ; le fixture
  est corrigé pour réellement le modifier, sans assouplir le parseur.
  iOS : import de l’extension AVFAudio `setActive` manquant dans le lecteur,
  constante PCM désormais importée du module `CoreAudioTypes`. Correctifs
  locaux à confirmer par nouveau build, non annoncés déjà validés.
- Téléchargement des journaux API réussi avec curl et credential GCM en mémoire,
  sans secret écrit en fichier ou argument. Les clients HTTP précédents avaient
  expiré. Le premier push HTTP408 a réussi au second essai HTTP/1.1.
- La relance locale avec paramètres réseau avait échoué avant les tâches :
  PowerShell avait fragmenté un argument -D en nom de tâche. Les propriétés
  passent maintenant par JAVA_TOOL_OPTIONS. Les recherches inutiles de groupes
  Kotlin/JVM sur Google Maven ont été filtrées après observation des requêtes
  HEAD manquantes ; nouvelle résolution locale en cours.
- Contrôle des vrais paquets ajouté : présence et SHA-256 des documents WAV
  dans APK, JAR Windows et bundles iOS. Deux tests de ce vérificateur passent ;
  le prochain build doit encore prouver que les produits natifs les contiennent.
