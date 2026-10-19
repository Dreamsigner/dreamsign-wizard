# Catalogue de cours complets, fidèle aux sources

## Demande et autorisation

Demande directe du 19 octobre 2026 : dans les catégories Mathématiques,
Informatique et TCF, chaque unité représente un cours de référence complet.
Chaque séance du cours doit correspondre à une leçon jouable dans l'application.
L'exemple « 70 leçons » illustre une correspondance, ce n'est pas un décompte
fourni. L'utilisateur demande d'exécuter le travail et a déjà demandé de prendre
les décisions courantes sans lui faire effectuer les manipulations.

## Sources autorisées

- Le cursus https://github.com/ossu/computer-science fourni par l'utilisateur,
  notamment Core math et Advanced math, ainsi que les cours de ces listes.
  Interprétation de « tous les cours » : inventorier aussi toutes les options
  Advanced CS et les propositions Final project ; ne pas en choisir seulement
  un sous-ensemble minimal. Un parcours alternatif équivalent reste dédoublonné.
- Les éditions précises des cours MIT référencées par ce cursus. Pour 18.01,
  utiliser 18.01SC, Fall 2010, alternative officielle liée par OSSU. Cette
  alternative regroupe Calculus 1A, 1B et 1C : ne pas les compter deux fois.
- TCF : l'utilisateur demande maintenant de sélectionner la meilleure source.
  Choix de réalisation : France Éducation international pour TCF Canada et les
  ressources de préparation qu'il recommande (TV5MONDE et RFI).
- Les documents MIT déjà cités dans le projet sont des ressources candidates,
  pas une autorisation de substituer arbitrairement une autre édition au cours
  du cursus OSSU.

## Structure et exigences de contenu

Catégorie -> unité/cours -> leçon/séance -> exercices.

Pour chaque cours, figer le titre, l'établissement, l'édition et l'URL de
programme. Chaque leçon conserve le numéro, le titre et le lien de sa séance
source, un objectif français, une explication pédagogique rédigée, des exercices
pertinents avec corrections et indices, et les ressources associées.

Le décompte d'un programme doit préciser son unité : séance, chapitre ou module.
Ne jamais remplacer une séance par une semaine, une liste de fichiers ou un
examen sans le dire. Les révisions et examens restent identifiés comme tels.
Le nombre d'exercices n'est pas fixé arbitrairement à deux pour tout le catalogue.

Ne pas fabriquer une leçon à partir de son seul titre. Lire la matière de
référence nécessaire pour rédiger le contenu. Les questions doivent porter sur
les notions et leur application, pas sur la mémorisation des titres du catalogue.

## Exhaustivité contrôlable

Créer un inventaire des cours OSSU et, pour chacun, un manifeste ordonné des
séances source. Garder séparés le nombre de séances attendues et le nombre de
leçons pédagogiques réellement écrites et validées. Un manifeste ou un lien vers
un cours n'est pas une leçon terminée. Aucun cours ne peut être affiché comme
complet si ces nombres diffèrent, s'il manque une source ou si un exercice est
absent. Les accès refusés ou programmes incomplets sont consignés explicitement.

Conserver une correspondance des identifiants publiés quand le contenu est le
même. La nouvelle structure ne doit pas donner artificiellement du crédit à
une séance différente via un ancien identifiant. Les cours commencent chacun
par une séance accessible ; les prérequis intra-cours suivent l'ordre source.

## Particularité du TCF

FEI indique que le TCF Canada n'est pas basé sur un programme de cours. Ne pas
annoncer un nombre officiel fictif de leçons TCF. Fonder les unités sur des
ressources précisément identifiées et les quatre épreuves officielles. Les
exercices de compréhension orale nécessitent de l'audio ; un texte affiché ne
doit pas être présenté comme une véritable épreuve d'écoute. L'écrit et l'oral
requièrent une production et des critères de correction, sans prétendre fournir
une note officielle avec un simple QCM.

## Technique et livraison

Conserver Kotlin 2.0.0, Compose Multiplatform 1.6.11, la cible Windows et la
palette de marque déjà fournie. La génération IA dans l'application reste
retirée. Le contenu pédagogique est préparé dans le dépôt.

Tester les invariants de catalogue : numéros, ordre, décomptes, identifiants,
sources, exercices, indices et prérequis atteignables. Compiler le desktop,
construire son paquet autonome et contrôler l'interface avant de livrer une
nouvelle version. Les avertissements de dépréciation n'équivalent pas à un
échec. La couverture réelle doit être documentée ; ne pas appeler une tranche
partielle « catalogue intégral ».
