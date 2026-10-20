# État du catalogue fondé sur les cours sources

Mise à jour : 20 octobre 2026.

## Exigence retenue

Une catégorie contient des unités correspondant à des cours complets.
Une unité reprend les séances du cours source dans leur ordre réel. Chaque
séance doit avoir son contenu pédagogique et ses exercices avant publication.

## Inventaire vérifié

- 63 entrées de cours/propositions dans les tableaux du cursus OSSU fourni,
  y compris les options avancées et les propositions de projet final.
- Ce sont des références à inventorier, pas 63 cours rédigés. Les programmes
  alternatifs équivalents doivent être dédoublonnés avant publication.
- 57 pages d'accueil ont répondu au collecteur ; cela ne prouve pas que leur
  programme détaillé est accessible ou complet. Six lectures ont échoué,
  pour des causes enregistrées dans l'inventaire JSON (certificat, refus ou
  interruption de téléchargement). Les certificats n'ont pas été désactivés.
- MIT 18.01SC, automne 2010 : correspondance vérifiée des séances 1 à 102,
  sans lacune ni doublon de lien. Répartition : 93 séances de cours,
  4 révisions et 5 examens.
- TCF Canada : référence FEI sélectionnée, préparation TV5MONDE/RFI
  recommandée par FEI. Voir `TCF-SOURCES.md` pour le format et les limites.

## Contenu effectivement disponible dans l'application

Le cours MIT 18.01SC est rédigé et intégré : 102 leçons et 236 exercices,
correspondant aux 102 séances vérifiées. Le validateur contrôle les numéros,
l’ordre, les titres sources, les liens, les identifiants et les types jouables.
La provenance et les limites de vérification mathématique figurent dans
`mit-18-01sc-authoring-notes.md`. Les examens sont des entraînements adaptés,
sans prétention de reproduire le barème MIT ou tous ses problèmes ouverts.

The Missing Semester 2026 est rédigé et intégré : neuf conférences officielles,
51 exercices. Les pages de matière et les scénarios de shell ont été vérifiés :
voir `missing-semester-authoring-notes.md`. L’ancien échantillon informatique
est remplacé dans le catalogue affiché, avec de nouveaux identifiants.

TCF : trois ateliers originaux d’expression écrite sont intégrés, avec saisie
et brouillon persistant, exemples rédigés et relecture explicite. Ce découpage
local correspond aux trois formats FEI ; il ne prétend pas être un programme
officiel ou une préparation complète. Voir `tcf-writing-authoring-notes.md`.
L’écoute dispose maintenant de trois documents originaux intégrés, six questions,
voix de synthèse, transcription en correction et mode lecture accessible distinct.
Les lecteurs natifs sont implémentés ; les contrôles Android/iOS de ce nouveau lot
restent requis. Trois ateliers de production orale sont implémentés avec capture,
stockage local et réécoute, mais attendent la vérification native des permissions
et de l’enregistrement. Le reste de la préparation reste à rédiger.

MIT 18.06SC Fall 2011 est rédigé et intégré : 40 séances (32 cours, quatre
révisions, quatre examens adaptés), 131 exercices et 83 contrôles exacts
SymPy liés aux réponses du fichier publié. Voir la note de provenance associée.

MIT 6.100L Fall 2022 est rédigé et intégré : 26 conférences, 104 exercices,
dont 70 extraits Python exécutés et comparés aux réponses. Récitations, PS0–PS5
et microquizzes sont distingués des conférences, sans prétention de les remplacer.

Le décompte reproductible à jour figure dans `AUTHORED-COVERAGE.md`.
MIT 6.042J Fall 2010 ajoute 28 séances et 117 exercices ; Learn Prolog Now!
ajoute 12 chapitres et 60 exercices, relus manuellement sans moteur Prolog exécuté.
Aucun décompte de cours
achevés n’est déduit des seules pages d’accueil de l’inventaire OSSU.

Le manifeste 18.01SC conserve le statut `not_authored` de la collecte initiale :
il constitue une trace source figée. Le contenu rédigé se trouve dans
`curriculum/courses/mit_18_01sc_2010.json` et son état est contrôlé séparément
par `tools/validate_curriculum.py`. Les décomptes des autres cours restent
inconnus tant que leur séquence source n’a pas été vérifiée.

## Suite requise avant livraison conforme

1. Terminer les manifestes des programmes restants, avec éditions et sources.
2. Rédiger et contrôler chaque leçon et ses exercices à partir du contenu réel.
3. Intégrer les cours complets et leurs interactions dans le catalogue.
4. Vérifier la couverture, la compilation et le paquet Windows livré.

Plan : `../superpowers/plans/2026-10-19-source-complete-curriculum.md`.
Spécification : `../superpowers/specs/2026-10-19-source-complete-curriculum-design.md`.
