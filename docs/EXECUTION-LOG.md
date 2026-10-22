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

## Reprise après interruption de quota — 20 octobre, midi (Douala)

- Les deux contributions 6.004 et 18.335J ont été interrompues par la limite
  d’usage. Aucun de ces cours n’a été ajouté au catalogue ni annoncé rédigé.
- `28249ca`, run `39385026380` : Windows SUCCESS, iOS SUCCESS, Android FAILURE.
  Les paquets Windows et iOS ont passé le contrôle des trois WAV intégrés.
  Les correctifs d’imports iOS sont donc confirmés par compilation, tests Kotlin,
  XCTest iPhone/iPad et archive appareil non signée. Capture micro réelle et
  signature Apple restent distinctes et non établies.
- Tests desktop locaux : BUILD SUCCESSFUL, journal
  `work/gradle-filtered-tests.log` hors dépôt. Les trente tests passent, y compris
  capture simulée dans le modèle, stockage isolé et parseur WAV corrigé.
- Échec Android restant : lint MissingPermission sur la construction AudioRecord
  dans le worker, après le callback d’autorisation. Ajout d’un contrôle explicite
  juste avant construction et d’un traitement SecurityException pour retrait
  de permission entre les deux étapes. Aucun avertissement lint supprimé.
- Paquet Windows local : premier essai offline bloqué par la sonde JDK Compose
  1.9.3 absente du cache ; relance connectée en cours. Captures iOS du run réussi
  en récupération pour inspection, sans annoncer celle-ci déjà réalisée.

## Reprise manuelle sur D: — 20 octobre 2026

- Instructions Home/RTK relues ; CODEX_HOME effectif `D:\Coding\_Codex\Home`.
  Dépôt vérifié : `D:\Coding\_Codex\Chats-hors-projet\2026-10-19\c\duolingo-kmp-learning-app`,
  branche `codex/windows-test-package`, base `c3af563`. Aucun basculement vers
  l'autre copie, aucune migration, aucune configuration globale modifiée.
  L'automatisation reste suspendue. Computer Use interdit pour cette reprise.
- Tous les fichiers modifiés/non suivis conservés. Les écritures de build et
  temporaires Java/Gradle sont dirigées vers le workspace D:, sans écriture
  dans les anciens dossiers C:.
- API GitHub revérifiée : `c3af563`, run `39416233195`, trois jobs SUCCESS.
  Cette preuve porte sur ce commit, pas sur le lot courant.
- Validateur courant : 13 unités, 323 leçons/ateliers et 1051 exercices.
  Dix-neuf tests Python PASS ; contrôles 6.004 (72) et 18.335J (78) PASS.
- Nouvelle exécution locale `desktopTest --offline` dans le dépôt D: :
  BUILD SUCCESSFUL, 52 tests exécutés, zéro échec/erreur/ignoré. Comprend
  12 tests des vrais écrans Compose et dix tests du worker de capture factice.
  Journal hors Git : `../work/resume-desktop-tests.log`. Le mode headless est
  imposé et testé ; aucune fenêtre ou permission micro n'a été ouverte.
- Capture générée `spoken-compact.png` inspectée : consigne réelle lisible en
  corps de texte, écran scrollable et action finale visible à 400 × 900.
  Les captures cours compact/large et les nouveaux tests Android nécessitent
  encore leur contrôle dans le lot courant ; aucune réussite Android/iOS
  ultérieure à c3af563 n'est encore annoncée.
- Revue média indépendante en lecture seule : pas de défaut produit concret
  trouvé ; nettoyage systématique des modèles après assertions en échec ajouté
  aux tests audio. Nouvelle exécution requise après ce changement de fixture.
- La rédaction forallx reste incomplète et hors catalogue. La couverture OSSU
  et TCF demeure partielle ; les tests automatisés ne certifient pas les
  performances pédagogiques, le microphone physique ou l'accessibilité native.
- Après nettoyage des fixtures : nouvelle exécution desktopTest et
  createDistributable réussie (52 tests, zéro échec/erreur/ignoré). Les trois
  WAV du vrai paquet Windows sont présents avec leurs empreintes attendues.
  Les deux rendus cours compact/large ont été inspectés : navigation inférieure
  en compact, navigation latérale et colonnes distinctes en large. Ces rendus
  utilisent un catalogue miniature isolé, pas l'intégralité des textes des cours.
  Les contrôles de calcul des huit cours disposant d'un vérificateur passent
  à nouveau ; Prolog garde sa limite explicite d'absence d'exécution par moteur.

## Lot de contenus et de reprise du cours choisi — 20 octobre 2026

- `9a8982f` poussé sur la branche existante. Run `39488554876` : Windows et
  Android SUCCESS; iOS encore en cours au présent relevé. Rapport Android
  instrumenté téléchargé : trois tests de LearningAndroidUiTest exécutés,
  zéro échec/erreur/ignoré sur émulateur API 34. Capture d'accueil Android
  inspectée : charte, carte du vrai cours, navigation inférieure et zones
  système séparées. Aucune manipulation Computer Use.
- forall x: Calgary : 48 chapitres réellement rédigés, 98 exercices. Revue
  indépendante des questions et comparaison ciblée aux sources quantificateurs,
  identité, preuves et modalité. s023 reformulé pour distinguer D et D(x) et
  éviter un faux positif anti-placeholder; s036_e03 ne présuppose plus des
  domaines vides. Schéma, correspondances et vérifications finies PASS.
- Stanford CS364A Fall 2013 : 20 leçons, 41 exercices. Revue indépendante :
  IDs absents et option dupliquée corrigés; 40 corrigés enrichis de raisonnement.
  Attribution corrigée : 4/3 non atomique affine en séance 11, 5/2 atomique en
  séance 12. L'exemple 15/10 incompatible avec le modèle de s11 est remplacé
  par Pigou 1/(3/4); s12 ajoute le cas 10/4. Les passages des deux PDF officiels
  ont été vérifiés par le parent. Checker : 29 résultats recalculés et 12
  assertions conceptuelles, ces dernières explicitement non indépendantes.
- Catalogue validé/généré : 15 unités, 391 leçons/ateliers, 1190 exercices;
  couverture OSSU/TCF toujours partielle. Les problèmes longs et projets
  originaux ne sont pas déclarés réalisés par ces adaptations de pratique.
- Affichage du numéro/type de séance ajouté dans le parcours et les notes.
  Test rouge observé sur l'étiquette d'examen absente, puis suite verte après
  ajout, sans changement des IDs publiés. Contrôle d'activation Entrée au
  clavier réussi directement dans la scène Compose, sans événement système.
- Test rouge observé : un choix de cours était perdu après reconstruction du
  modèle. Ajout de selectedCourseIds par matière, valeur par défaut pour les
  anciennes sauvegardes, sélection validée avant sauvegarde. Tests du JSON
  legacy, du redémarrage, des sélections invalides/répétées et des catégories
  indépendantes. Revue indépendante : aucun défaut P1/P2 trouvé dans ce lot.
- Exécution avec catalogue étendu : 59 tests, un échec de fixture qui cherchait
  un cours non encore composé hors viewport. La fixture utilise désormais le
  défilement sémantique du LazyColumn; aucun contrôle produit n'est désactivé.
  Nouvelle suite complète : BUILD SUCCESSFUL, 59 tests, zéro échec/erreur/ignoré,
  journal `../work/expanded-course-list-green.log` hors Git.
- Le paquet Windows a été reconstruit avec le catalogue étendu lors du même
  lot (avant la correction de la fixture seulement). La relance finale et les
  builds CI des changements non encore poussés restent à confirmer.
- Rédaction indépendante du cours Northeastern OOD en cours, hors catalogue;
  aucun décompte de leçons supplémentaires n'est encore annoncé pour ce cours.

## Contrôle du paquet et périmètre Windows uniquement

- Demande utilisateur : si avancement global sous 50 %, abandonner les
  développements Android et iOS. Estimation annoncée : environ 40 % du
  périmètre initial, notamment du fait du catalogue intégral encore incomplet.
  Périmètre actif désormais Windows uniquement, décrit dans DELIVERY-SCOPE.md.
  Sources mobiles conservées, jobs Android/iOS retirés du workflow actif;
  aucune automatisation Codex réactivée. Le run 39488554876 est terminé avec
  conclusion globale cancelled; ses jobs Windows et Android avaient réussi.
  Aucune réussite iOS de ce run n'est revendiquée et sa poursuite n'est plus
  un prérequis du travail demandé.
- Diagnostic --self-check ajouté au véritable lanceur Windows : catalogue
  généré, intégrité élémentaire des IDs/contenus et lecture des WAV via Res.
  Pas de création de fenêtre, modèle de progression, lecteur ou microphone.
  Le rapport est créé avec CREATE_NEW, sans remplacement de fichier existant.
- Tests diagnostic : première tentative JUnit rejetée parce qu'un test à
  expression renvoyait une exception plutôt que Unit. Signature corrigée,
  puis RED observé sur les fonctions non implémentées avant réalisation.
  L'intégration a ensuite révélé que Json omettait les valeurs par défaut :
  nouveau test RED sur les drapeaux absents, corrigé avec encodeDefaults=true.
- Dernière suite/package : BUILD SUCCESSFUL, 64 tests, zéro échec/erreur/ignoré.
  Journal hors Git ../work/installation-final-suite-package.log. Le véritable
  DuolingoKmpApp.exe a terminé son contrôle local avec exit 0 : 15 cours,
  391 leçons/ateliers, 1190 exercices, trois SHA-256 audio exacts, drapeaux
  uiStarted=false et userDataAccessed=false dans le JSON. Aucun Computer Use.
- Rapport local conservé hors Git :
  ../work/installation-check-9f09b6ee41af461a909d5017710c1231.json.
  Le workflow Windows doit encore confirmer ce nouveau contrôle sur runner.
- Revue indépendante du diagnostic demandée, mais interrompue par une limite
  d'usage avant livraison de findings. Ne pas présenter cette revue comme faite;
  le parent a inspecté le code et exécuté les tests et le lanceur réel.
- Northeastern OOD : 24 leçons/48 exercices en brouillon hors catalogue sur
  25 réunions sources (23 cours, deux examens; Thanksgiving exclu). La séance
  JavaScript du 26 novembre n'a pas de support officiel trouvé; aucune leçon
  n'est inventée depuis son titre. Checker volontairement PARTIAL, non inclus
  dans les contrôles des seuls cours publiés. Ces 24 leçons ne s'ajoutent pas
  au décompte publié de 391.

## Preuve CI Windows — commit 86e0f59

- Run 39503525124, unique job package-windows 118906653381 : SUCCESS.
  https://github.com/Dreamsigner/duolingo-kmp-learning-app/actions/runs/39503525124
- Le contrôle des sources et la génération déterministe ont réussi sur Windows,
  puis desktopTest et createDistributable. Le rapport HTML téléchargé dans
  Windows-checks confirme 64 tests, zéro échec et zéro ignoré.
- Le véritable lanceur produit par le runner a réussi --self-check : 15 cours,
  391 leçons/ateliers, 1190 exercices, trois SHA-256 audio identiques aux ressources
  attendues; uiStarted=false et userDataAccessed=false. Rapport JSON lu dans
  l'artefact 12302032183, dont l'intégrité ZIP a été contrôlée sans entrée corrompue.
- Les captures CI learning-wide.png (fixture miniature) et
  published-notes-mathematics.png (véritable première leçon forallx) ont été
  inspectées avec view_image, sans Computer Use : navigation latérale et colonnes
  lisibles, notes et bouton de continuation visibles. Il s'agit de rendus Compose
  hors écran, pas d'une inspection du gestionnaire de fenêtres Windows.
- Paquet Windows : artefact 12302522162, 56 996 388 octets sur GitHub. Le guide
  windows-testing.md décrit l'extraction, le runtime intégré, la sauvegarde et le
  diagnostic facultatif. La capture physique du microphone reste non vérifiée.
- Mise au propre des mentions de CI mobile : jobs arrêtés conformément au
  périmètre actif. Aucun nouveau succès Android/iOS revendiqué. La couverture
  pédagogique globale reste partielle; cette preuve ne vaut pas finalisation
  du cursus OSSU/TCF complet.
- Artefact Windows téléchargé puis les deux niveaux ZIP contrôlés sans entrée
  corrompue. SHA-256 du téléchargement GitHub :
  dfd20cd019af32d423b69c5f537dc2f64151d6b45f4e75d99849f000ac4719b8.
  Dossier extrait hors Git ../work/windows-delivery-86e0f59, 115 919 381 octets.
  Le lanceur issu de ce téléchargement a également réussi son diagnostic local
  avec exit 0 et les mêmes décomptes/drapeaux; le vérificateur Python retrouve
  les trois fichiers audio avec leurs SHA exacts. Aucune fenêtre ni donnée
  utilisateur ouverte pour ce contrôle. Ce paquet est celui du commit 86e0f59;
  les commits documentaires suivants ne modifient pas son application.

## Retours utilisateur — ordre, cours, mathématiques et reprise des erreurs

- Travaux repris dans la même copie D:, branche codex/windows-test-package,
  sans changement global, réactivation automatique ni Computer Use.
- Ordre explicite OSSU ajouté aux cours publiés, avec adaptation locale des
  cours de remplacement. MIT 18.335J est un ajout local avancé, pas un cours
  revendiqué comme listé par OSSU. Le TCF conserve son ordre éditorial distinct.
- Revue française de Learn Prolog Now! : glossaire sourcé et précision de
  l’échec fini pour la négation par l’échec. IDs, options et index corrects
  conservés. Les notes françaises ne sont pas une traduction intégrale du livre.
- Première reproduction TDD de reprise différée : deux assertions métier ont
  échoué avec l’ancien comportement. Implémentation d’une file par tentative :
  premier tour complet, puis rotation des erreurs restantes; XP après correction
  de toutes les questions. La revue indépendante a trouvé un retry immédiat
  laissant une erreur en file; test ajouté, échec reproduit puis retrait corrigé.
- Lecteur réunissant toutes les notes originales d’un cours, sommaire et liens
  vers exercices associés; lecture possible même avant le déverrouillage des
  exercices. Objectif placé avant l’énoncé, contexte avant la question, bouton
  explicite pour l’explication après vérification.
- Rendu mathématique local JLaTeXMath (exception Classpath), sans WebView :
  fractions, matrices, puissances/indices reconnus prudemment dans les champs
  mathématiques, TeX explicite possible; aucun interpréteur appliqué au code
  Prolog. Modules de polices grecques/cyrilliques optionnels exclus. Les formes
  non reconnues restent du texte, sans invention de structure mathématique.
- Icônes pixel originales pour flamme, cœur, XP et leçon terminée, bibliothèque
  voxel décorative, palette de marque conservée. Aucune ressource Minecraft copiée.
- Catalogue de supports appariés à chaque séance/exercice; PDF complets distingués
  des supports partiels et collections. Les originaux restent liés à leurs sites.
  Bibliothèque Markdown générée hors du code dans docs/courses et README principal
  refondu, avec bannière originale et captures du catalogue réellement publié.
- Premiers rendus inspectés : catalogue PC, exercice réel d’algèbre avec matrice,
  lecteur de cours sur fixture et bannière SVG rasterisée. Aucun périphérique
  physique ni fenêtre du système n’est déclaré vérifié.
- Intégration en cours : les erreurs de compilation des premiers essais ont
  révélé un import de test manquant, une propriété sémantique Compose mal typée
  et un ancien index de ressources non régénéré dépassant 64 KB en <clinit>.
  Corrections apportées; génération désormais par petits helpers. Les résultats
  finaux et le paquet de ce lot seront inscrits après leur vérification.

### Vérification locale du lot — 21 octobre 2026

- Suite finale desktop : 93 tests, zéro échec/erreur/ignoré; BUILD SUCCESSFUL,
  avec createDistributable. Journal ../work/remarks-final-package.log. Tests PDF
  sous le vrai thème : erreur/réessai, navigation sur deux pages, zoom et fermeture.
- Test réseau distinct sourcePdfSmokeTest réussi : véritable PDF de séance
  MIT 18.06SC, quatre pages, première rendue en 1000 × 1294 pixels par le lecteur
  de production. Ce test reste opt-in et n'est pas compté comme ignoré dans la
  suite ordinaire. Aucun support original n'est publié dans le dépôt.
- Les 24 tests Python, les dix contrôleurs pédagogiques, la validation des
  15 cours/391 séances/1190 exercices, l'index de 1326 supports et l'export
  Markdown ont réussi. La couverture globale OSSU reste explicitement false.
- Le diagnostic du lanceur distribué a confirmé les trois WAV, le rendu LaTeX
  et le rendu PDF originaux en mémoire. Première exécution : avertissement
  PDFBox dû au module jdk.unsupported absent du runtime jlink réduit, sans
  échec fonctionnel. La source IOUtils 3.0.8 confirme le recours à sun.misc.Unsafe.
  Module ajouté à nativeDistributions; nouvelle construction et diagnostic
  réussis sans InaccessibleObjectException ni avertissement d'unmapping.
  Rapport : ../work/remarks-installation-d3f260e5d4a94ff9b8d6a2847f142465.json.
  Les drapeaux mathRenderingVerified et pdfRenderingVerified valent true;
  uiStarted et userDataAccessed restent false. La CI impose aussi ce module.
- Inspection visuelle supplémentaire : contrôles du lecteur PDF et page d'un
  document de test original via capture Compose. Les captures de catalogue et
  de matrice dans le README proviennent du contenu publié, stockage mémoire.
- Licences/notices exactes des JAR distribués JLaTeXMath, PDFBox, PDFBox IO,
  FontBox et Commons Logging conservées; copie prévue dans le ZIP Windows.
  La CI et les artefacts du commit à pousser restent à confirmer.

### Écart CI et correction du lecteur PDF

- Runs 39513880534 / 39513886562 du commit f93e697 : échec d'un test PDF,
  92 autres tests réussis. Le XML montre UncompletedCoroutinesError et un scan
  initial de 147 polices Windows par la fixture Helvetica, prenant environ
  86 secondes sur le runner neuf. Ce n'est pas un échec des données de cours.
- Fixture autonome remplacée par une police TTF intégrée dans le PDF de test,
  issue des ressources PDFBox déjà distribuées et couvertes par leurs notices.
  Aucun délai de test augmenté et aucune assertion retirée.
- Le rendu lent a également motivé un test de fermeture non bloquante : RED
  reproduit par TimeoutException pendant un rendu retenu par un latch. La session
  marque maintenant la fermeture immédiatement; le travailleur de rendu possède
  la libération finale du document sous verrou, une seule fois. Les travaux de
  rendu annulés sont vérifiés avant d'accéder au document.
- Nouvelle suite avec un répertoire de cache de polices vide : 94 tests réussis,
  zéro échec/erreur/ignoré, aucune collecte système de polices dans les journaux.
  Journal ../work/pdf-cold-font-cache-green.log. Nouvelle CI encore requise.

### Livraison Windows intégrée — 21 octobre 2026

- Commit applicatif 9b8681637313c1dc6005b334f675cddac7cd4414 : les runs
  push 39515363231 et pull_request 39515370212 ont tous deux réussi.
  XML du paquet Windows-checks 12307103912 : 20 suites, 94 tests,
  zéro échec, erreur ou test ignoré. Diagnostic du lanceur : 15 cours,
  391 leçons/ateliers, 1190 exercices; PDF et LaTeX rendus avec succès,
  trois WAV vérifiés; aucune fenêtre ni donnée utilisateur ouverte.
- Reconstruction locale et sourcePdfSmokeTest également réussis
  (../work/pdf-cold-fix-package-smoke.log). Ce dernier valide le chargement
  et le rendu d'un vrai support MIT, indépendamment de la suite hors réseau.
- PR #2 intégrée dans main : merge 8aabafe8123c2e073a32e83e83c5926683fec27b.
  Artefact Windows testé par la CI : 12308001751, run 39515363231.
  https://github.com/Dreamsigner/duolingo-kmp-learning-app/actions/runs/39515363231
- Cette preuve concerne le code 9b86816 et son merge sans modification de
  contenu. Elle ne constitue pas une validation de l'intégralité OSSU/TCF,
  du microphone physique ou d'un lecteur d'écran; les limites précédentes
  demeurent. Android/iOS et Computer Use restent hors du périmètre de reprise.

### Typographie pédagogique et liens vidéo — 21 octobre 2026

- Le placement indépendant des fragments mathématiques dans FlowRow provoquait
  un décalage visuel des indices par rapport à la prose. Reproduction par capture
  et test RED de l'absence de mise en page native de paragraphe; remplacement
  par InlineTextContent dans un seul Text. Matrices en blocs séparés défilables,
  prise en compte de la densité, descriptions parlées enrichies pour les indices,
  vecteurs et opérations de lignes. Dates, chemins et divisions chaînées exclus
  de l'inférence de fractions, avec régressions dont la date 2026/01/09.
- Le rendu couvre aussi les contenus mathématiques des cours informatiques,
  objectifs, énoncés, choix, indices et explications. Code balisé conservé littéral.
- Lecteur pédagogique commun : titres sémantiques, paragraphes, listes, repères,
  gras ciblé, code en chasse fixe sur fond distinct, exemples séparés et mesure
  de lecture bornée. Les neuf notes Missing Semester et trois premières notes
  18.06SC ont des sections explicites et exemples. Une passe de ponctuation et
  de balisage a modifié 284 notes sur 391; les deux points-virgules conservés
  appartiennent à un exemple logique cité. Les exercices, IDs, URLs de cours
  et autres métadonnées sont strictement inchangés par cette passe éditoriale.
- Index vidéo : 383 entrées, 333 URL distinctes, provenance conservée. Liens
  YouTube de vidéo ou MP4, jamais chaîne générique. Suivi borné d'un lien vidéo
  officiel; 18.06SC couvre 35/40 séances, 6.004 27/28. Plusieurs clips ou formats
  peuvent appartenir à une séance. Aucun média inventé lorsqu'absent; aucune
  prétention de lecture intégrale de tous les flux externes. Un test de clic
  vérifie que la vidéo de la première séance 18.06SC ouvre son URL YouTube,
  plutôt que sa page MIT de provenance.
- Vérification locale finale : 105 tests desktop, zéro échec/erreur/ignoré,
  createDistributable réussi (../work/lesson-formatting-final.log). Diagnostic
  du véritable EXE réussi : PDF/LaTeX et WAV, sans fenêtre ni données utilisateur.
  30 tests Python et validations des 15 cours/391 séances/1190 exercices,
  1235 ressources et exports Markdown; contrôleurs d'algèbre linéaire et Bash
  réussis. Les captures shell 1100/500 et aperçu matriciel ont été inspectées
  sur surfaces du thème réel. La vérification d'un lecteur d'écran physique
  reste distincte. La CI du nouveau commit est encore à confirmer au push.
- Le défilement PDF vertical continu est ajouté au reste à faire, sans changement
  du lecteur page par page dans ce lot. La couverture pédagogique globale reste
  partielle. Voir docs/lesson-presentation.md pour la charte éditoriale et les
  références W3C/IES, sans extrapolation de résultats d'apprentissage.


## 2026-10-21 — Fractions dans l’ensemble du catalogue Windows

- Ajout d’un parseur partagé de quotients équilibrés : fractions imbriquées,
  parenthèses, puissances/indices Unicode, fonctions, factorielles et signes.
  Le rendu produit des fractions LaTeX superposées dans les notes, objectifs,
  énoncés, options, indices, corrections et champs de formule. Le code et les
  chemins restent littéraux ; les arités Prolog sont balisées comme code.
- Réécriture explicite en LaTeX des notations ambiguës : rapports en français,
  unités, intégrales pondérées, normes, adjoints, reste de Taylor et somme de
  Riemann. Les deux premières notes de calcul différentiel ont des explications
  structurées et des exemples résolus avec fractions complètes.
- Contrôle du catalogue généré réel : 9 535 champs, 1 425 occurrences de
  fractions rendues par le moteur desktop, aucune barre oblique non classée
  hors code et aucune fraction invalide. Les contenus externes PDF/vidéo ne
  sont pas réécrits par ce contrôle. Les tests algébriques ciblés vérifient
  les priorités des opérateurs et les limites des numérateurs/dénominateurs.
- Diagnostic visuel : les captures Compose ont aussi révélé que les équations
  LaTeX en bloc sans commande antislash repassaient par le normaliseur de
  texte ancien. Le composant transmet désormais les blocs explicitement
  balisés au moteur LaTeX directement. Un test d’intégration couvre ce cas.
- Libellé des supports externes : « Disponible online ».
- Les 30 tests Python et les contrôleurs des dix cours techniques passent.
  Quatre empreintes d’énoncés d’architecture ont été revues après modification
  du seul balisage : données numériques, choix et réponses inchangés.
- Les rapports de couverture des fractions sont archivés par la CI avec les
  autres preuves Windows. Compilation, tests et inspection finale du paquet
  de ce lot : à confirmer ci-dessous après exécution complète.

- Vérification locale finale : 113 tests desktop, zéro échec/erreur/ignoré,
  createDistributable réussi (`../work/fraction-delivery-verified.log`).
  L’EXE produit réussit son diagnostic PDF/LaTeX/audio sans ouvrir de fenêtre
  ni accéder aux données utilisateur. Captures Compose réinspectées à 500 et
  1 100 px : fractions complètes et équations en bloc rendues correctement.
  Le contrôle symbolique des dix cours techniques et les 30 tests Python ont
  été relancés dans ce lot. La CI distante reste à confirmer après publication.

- Publication vérifiée : code `2dfe292`, CI GitHub Actions `39681467646` réussie.
  ZIP Windows `12384767059` (61 672 336 octets). Les preuves `Windows-checks`
  `12383319440` ont été téléchargées et relues : 113 tests sans échec, 9 535
  champs / 1 425 fractions / zéro non classé ou invalide; diagnostic du véritable
  EXE du paquet positif pour LaTeX/PDF, sans interface ni données utilisateur.


## 2026-10-21 — Lecteur PDF à défilement vertical continu

- Remplacement de l’affichage d’une seule page par une liste verticale continue.
  Les pages visibles sont rendues à la demande et leurs bitmaps sont attachés
  aux éléments de la liste, sans collection globale d’images du document.
  Les dimensions avant rendu tiennent compte du format et de la rotation pour
  stabiliser les emplacements pendant le chargement. Les rapports de forme
  extrêmes sont bornés pour protéger les contraintes de mise en page.
- Barre de défilement desktop native, zoom 75–200 %, défilement horizontal
  lorsque nécessaire et compteur de page visible. Navigation clavier dans
  la zone de lecture : flèches, Page haut/bas, Début et Fin. Les boutons de
  changement de page ne sont plus nécessaires et ont été retirés.
- Tests ajoutés : document de 200 pages, accès à la page 100 puis geste de
  défilement, rendu limité aux pages demandées, accès clavier à la dernière et
  à la première page, géométrie portrait/paysage/rotation et captures Compose
  à 500 et 1 100 px montrant deux pages dans la même fenêtre. Les scénarios
  existants de zoom, erreur/réessai et fermeture de document restent couverts.
- Régression reproduite avant modification : le test de défilement échouait
  car l’ancien lecteur n’exposait aucune liste continue. Pendant l’intégration,
  un repositionnement avant création de la liste suspendait l’ouverture du
  document ; ce repositionnement a été retiré avant validation.
- Validation locale : 116 tests desktop sans échec/erreur/ignoré et création
  de distribution réussie. Captures du passage entre deux pages inspectées.
  Le diagnostic de l’EXE réussit pour PDF/LaTeX/audio, sans fenêtre ni données
  utilisateur. Le contenu des cours et la progression enregistrée sont inchangés.
  Confirmation de la CI et du ZIP de ce lot : après publication.

- Livraison distante confirmée : code `7a337b5`, CI `39686861477` réussie,
  ZIP Windows `12386053808` (61 692 771 octets). Les preuves `12386148981`
  ont été téléchargées et relues : 116 tests sans échec/erreur/ignoré,
  captures du lecteur continu à 500/1 100 px et diagnostic de l’EXE du paquet
  positif pour LaTeX/PDF, sans accès aux données utilisateur ni fenêtre.


## 2026-10-22 — Installateur Windows par utilisateur

- Lot autorisé : distribution Windows classique, sans modification pédagogique.
  Version installateur 1.1.0, UUID de mise à niveau stable, installation par
  utilisateur, choix du dossier, raccourcis Bureau/menu Démarrer. Les données
  `%LOCALAPPDATA%/AtelierApprentissage` restent séparées de l’application
  `%LOCALAPPDATA%/DuolingoKmpApp`. Aucun déplacement de sauvegarde.
- Construction native via Compose/jpackage et WiX déjà disponibles. Les notices
  et le guide sont générés comme ressources communes pour chaque distribution.
  Le premier essai combiné a détecté une dépendance Gradle implicite entre la
  préparation des notices et celle des ressources applicatives : dépendance
  explicitée, avec configuration différée de la tâche enregistrée par Compose.
- Ajout d’un test d’installation exclusif aux VM GitHub Windows jetables :
  sauvegarde fictive préexistante, installation 1.1.0, diagnostic de l’EXE
  installé, raccourcis, notices, inscription MSI par utilisateur, mise à niveau de test vers
  1.1.1, désinstallation et intégrité exacte de la sauvegarde à chaque étape.
  La version 1.1.1 est uniquement une fixture CI, jamais publiée comme livrable.
- Aucun installateur exécuté sur le poste utilisateur. Pas de certificat de
  signature disponible dans ce lot; les avertissements d’éditeur inconnu
  restent possibles. La mise à jour automatique n’est pas implémentée.
- Vérifications de construction et résultat du test CI à confirmer ci-dessous.

- Validation locale : 116 tests desktop sans échec, création app-image et EXE
  réussie. Lecture du MSI incorporé sans installation : version 1.1.0,
  UpgradeCode stable et notices/guide présents. Aucun installateur exécuté sur
  le poste utilisateur. Les erreurs CI de paramètre PowerShell tronqué et de
  lecture d’une vue ARP du registre ont été corrigées : argument version cité,
  identité du produit lue dans l’EXE puis contrôlée par l’API Windows Installer
  (`AssignmentType=0`, produit installé, version exacte).
- Livraison confirmée : `a6658d6`, CI `39713029803` réussie. Artefact Setup
  `12397039260`, portable `12398165623`, preuves `12397768247`. Les preuves
  téléchargées confirment 116 tests sans échec/erreur/ignoré, installation 1.1.0,
  mise à niveau 1.1.1 de test, raccourcis/notices et diagnostics, désinstallation
  et sauvegarde inchangée. La fixture 1.1.1 n’est pas publiée. L’assistant
  graphique d’installation n’a pas été inspecté avec Computer Use; les opérations
  vérifiées sont exécutées silencieusement sur une VM GitHub jetable.


## 22 octobre 2026 — Aperçus vidéo des mathématiques

- Lot annoncé avant modification : estimation 8–15 k tokens générés. Périmètre :
  aperçus et ouverture directe des vidéos des maths sur Windows. Lecture intégrée
  différée; aucune modification du contenu pédagogique des autres catégories.
- Cartes natives dans les notes avant exercice, l’aide et le lecteur de cours.
  Disposition horizontale sur grand écran, verticale en largeur réduite.
  Titre source, langue, disponibilité online et action explicite vers le navigateur.
- Vignettes YouTube chargées hors du thread UI; cache mémoire de huit images.
  Hôte HTTPS fixe, identifiants validés, redirections refusées, plafond 1 Mio,
  dimensions contrôlées avant décodage, délais connexion/lecture de 4 s.
  Aucun téléchargement de vidéo, cookie, clé API ou fichier de cache sur disque.
- Repli explicite pour MP4, échec réseau et image invalide. L’ouverture du lien
  exact reste possible; une erreur navigateur est signalée sans quitter la leçon.
- Six nouveaux tests desktop réussis : analyse d’URL, bornes/cache/décodage,
  cartes à 500/1100 px, image injectée, MP4, activation clavier/échec navigateur
  et absence de chargement dans la présentation historique utilisée hors maths.
  Les captures de ces deux dispositions ont été inspectées sans Computer Use;
  l’image chargée de la capture est une fixture grise, pas une vidéo vérifiée.
- Contrôle réseau direct d’une vignette référencée : HTTP 200, image/jpeg, 9690
  octets. Il ne constitue pas une vérification de disponibilité de toutes les vidéos.
- Installateur porté à 1.2.0; identité de mise à niveau inchangée. Fixture CI
  non publiée : 1.2.1. Suite complète, paquet et preuves CI à confirmer ci-dessous.
- Validation locale complète : 122 tests desktop sans échec/erreur/ignoré,
  app-image et installateur 1.2.0 construits. Captures à 500/1100 px inspectées.
- Livraison confirmée : code `3335bd1`, CI `39716799517` réussie.
  Setup `12399109481`, portable `12399836844`, preuves `12399124187`.
  Rapports téléchargés : 122 tests desktop sans échec/erreur/ignoré; installation
  1.2.0, mise à niveau fixture 1.2.1, diagnostic natif, raccourcis, notices,
  désinstallation et progression inchangée. La fixture n’est pas publiée.
  Les contrôles pédagogiques/Python du workflow ont également réussi.
  Aucun installateur exécuté sur le poste utilisateur. Installateur non signé;
  lecture vidéo intégrée et mise à jour automatique toujours absentes.


## 22 octobre 2026 — Recherche du sommaire des mathématiques

- Recherche par titre et objectif dans le sommaire des maths. Accents et casse
  ignorés, tous les mots doivent correspondre. Index d’origine et prérequis
  conservés. Effacement et état sans résultat explicites. Les autres catégories
  gardent leur présentation existante.
- Validation locale : 126 tests desktop sans échec/erreur/ignoré, app-image et
  installateur 1.3.0 construits. Captures à 500/1100 px inspectées.
- Fixture de mise à niveau CI 1.3.1 non publiée. Résultat CI à confirmer.
- Livraison confirmée : code `1c08fa1`, CI `39719259245` réussie,
  Setup `12400074222`, portable `12400631407`, preuves `12400047837`.
  126 tests desktop sans échec/erreur/ignoré, 30 tests Python du projet;
  installation 1.3.0, fixture 1.3.1, désinstallation et progression préservée.
  Aucun installateur exécuté sur le poste utilisateur.


## 22 octobre 2026 — Mise à jour automatique Windows

- Vérification des releases stables du dépôt officiel, staging limité et contrôle
  SHA-256 avant installation. Vérifications au démarrage et toutes les six heures,
  téléchargement en arrière-plan, application à la fermeture ou redémarrage explicite.
- Réglage Auto persistant et désactivable; portable et chemins personnalisés exclus
  de l’installation automatique. Progression et préférences de mise à jour séparées.
- Assistant local fixe, données JSON séparées du code, attente de la fin du processus
  puis recontrôle d’intégrité et installation `/quiet /norestart`. Les commandes encodées
  initialement bloquées ont été abandonnées; aucune protection système n’a été modifiée.
- Publication GitHub Releases ajoutée pour le tag correspondant à 1.4.0, après succès
  des vérifications. La fixture 1.4.1 ne doit pas être publiée.
- Dix tests ciblés réussis. Validation complète, captures, installation et publication
  effective de la release à confirmer ci-dessous.
- Validation locale complète : 136 tests desktop sans échec/erreur/ignoré,
  app-image et installateur 1.4.0 construits. Captures des contrôles inspectées
  à 500/1100 px. Diagnostic EXE sans accès aux données utilisateur réussi;
  version applicative 1.4.0 transmise au runtime. La copie portable reste
  exclue des mises à jour automatiques. Vérification installée réservée au CI.
- Le premier run tagué `39722314843` a validé l’installation initiale,
  la détection du chemin installé et la version runtime, puis bloqué la release :
  `Get-FileHash` absent dans le PowerShell enfant. Le checksum a été déplacé
  vers SHA256 .NET, indépendant du chargement de modules PowerShell. Aucun
  réglage système changé; aucune release publiée pour le tag 1.4.0.
- Nouvelle candidate 1.4.2 (fixture de test 1.4.3), sans déplacer le tag existant.
