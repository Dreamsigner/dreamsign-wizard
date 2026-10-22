# Bilan de livraison et travaux restants

État vérifié le 22 octobre 2026. Périmètre actif : Windows natif uniquement.
Ce bilan complète le registre; il ne transforme pas l'inventaire OSSU en cours rédigés.

## Livré et vérifié

- Catalogue : 15 unités, 391 leçons/ateliers, 1190 exercices; progression locale persistante.
- Ordre pédagogique OSSU documenté, glossaire français Prolog, reprise différée des questions ratées jusqu'à correction.
- Lecture des notes par cours, liens appariés aux exercices, lecteur PDF natif, formules composées localement, explications et icônes originales.
- Dernière validation locale : 126 tests desktop, 30 tests Python et contrôleurs pédagogiques réussis. Le contrôle global des fractions parcourt 9 535 champs et rend 1 425 fractions sans erreur ni barre non classée hors code. Captures Compose de calcul différentiel inspectées à 500 et 1 100 px. Diagnostic de l’EXE produit réussi pour LaTeX, PDF et audio sans accès aux données utilisateur.
- Code livré : `1c08fa1`, installateur Windows 1.3.0. CI réussie : [run 39719259245](https://github.com/Dreamsigner/duolingo-kmp-learning-app/actions/runs/39719259245). Installateur `12400074222`, ZIP portable `12400631407`, preuves `12400047837`. Rapports téléchargés : 126 tests desktop sans échec/erreur/ignoré, diagnostic de l’EXE installé, raccourcis et notices, mise à niveau de test, désinstallation et progression préservée.
- Recherche par titre/objectif dans le sommaire des mathématiques, sans modifier ordre, prérequis ou progression. Captures inspectées à 500/1100 px.
- Aperçus vidéo dans les supports de mathématiques : cartes adaptatives, vignettes YouTube si disponibles, lien direct vers la vidéo et repli hors ligne/MP4. Captures Compose inspectées à 500 et 1100 px. Les vignettes du test sont injectées; une vignette réelle a également été contrôlée par HTTP. La lecture intégrée et la disponibilité de toutes les vidéos ne sont pas certifiées.
- Génération IA retirée. Android/iOS conservés sans développement actif. Aucun Computer Use utilisé.

## 1. Couverture pédagogique complète — chantier majeur restant

- Finaliser les séquences sources encore inconnues ou partielles, identifier les éditions, résoudre les accès échoués et dédoublonner les alternatives. L'inventaire compte 63 références, pas 63 cours complets distincts.
- Rédiger puis contrôler les cours OSSU absents du catalogue, séance par séance. La liste des références et de leur état figure dans [OSSU-COVERAGE](curriculum/OSSU-COVERAGE.md); la liste réellement publiée dans [AUTHORED-COVERAGE](curriculum/AUTHORED-COVERAGE.md). Leur différence ne permet pas un pourcentage exact tant que les séquences attendues restent inconnues.
- Terminer le brouillon de conception orientée objet Northeastern et résoudre son support manquant avant publication; il reste hors catalogue.
- Compléter la correspondance des devoirs, problèmes ouverts, TP, projets et examens originaux. Les exercices adaptés actuels ne remplacent pas l'intégralité des évaluations MIT/OSSU.
- Revoir humainement les démonstrations, corrections conceptuelles et traductions; les contrôles exacts ou bornés ne prouvent pas tous les raisonnements. Exécuter les exercices Prolog dans un vrai moteur: revue actuelle sans exécution Prolog.
- Enrichir le TCF dans les quatre compétences. Les douze ateliers actuels sont originaux et partiels, sans programme officiel inventé, notation FEI ni correction automatique de production écrite/orale. Diversifier les écoutes au-delà des trois audios de synthèse intégrés.

## 2. Supports et lecture

- La lecture vidéo intégrée reste à développer; les aperçus livrés ouvrent le navigateur.

- L'application affiche ses notes originales françaises et donne accès aux supports officiels; elle ne contient pas une traduction intégrale des manuels MIT.
- Contrôler progressivement le contenu et le rendu de chaque PDF: les réponses HTTP et le rendu réussi d'un document ne vérifient pas l'ensemble des 1235 ressources indexées.
- Les PDF externes nécessitent Internet, sont chargés en mémoire et ne sont pas mis en cache pour une bibliothèque hors ligne. Aucun manuel original n'est réhébergé sur GitHub.
- Lecture PDF verticale continue implémentée avec rendu à la demande des pages visibles, zoom et barre de défilement. Les vérifications automatisées et le lot applicatif correspondant sont consignés dans le registre.
- Poursuivre la restructuration éditoriale des notes après les neuf leçons Missing Semester les trois premières séances d'algèbre linéaire et les deux premières séances de calcul différentiel. Le nouveau lecteur typographique commun ne constitue pas une réécriture de toutes les séances.
- Tout ajout futur de manuel complet traduit ou réhébergé devra respecter sa licence et conserver attribution, édition et provenance.

## 3. Vérifications Windows restant manuelles ou matérielles

- Tester réellement microphone, autorisation système, capture/réécoute et qualité sonore sur du matériel Windows; les simulations et contrôles de fichiers ne le prouvent pas.
- Compléter les contrôles d'accessibilité avec lecteur d'écran natif, mise à l'échelle/DPI et variété de tailles réelles. Les captures automatisées ne certifient pas toutes les fenêtres, tous les cours ni toutes les configurations.
- Le comportement du gestionnaire de fenêtres réel n'est pas certifié par les rendus Compose hors écran. Ces vérifications doivent utiliser des moyens directs/automatisés autorisés ou un retour utilisateur; Computer Use demeure interdit.

## 4. Distribution et fonctions absentes

- Distributions : ZIP portable et installateur Windows par utilisateur avec Java embarqué. Le lot installateur est vérifié séparément dans le registre. La signature de code et la mise à jour automatique ne sont pas livrées.
- Aucune synchronisation entre appareils ni sauvegarde cloud. La progression est conservée localement; préserver progress.json lors des mises à jour.
- Android et iOS sont explicitement abandonnés dans le périmètre actif, pas présentés comme des livrables en attente. Leur reprise nécessiterait une nouvelle décision, des builds/essais adaptés et, pour un iPhone réel, une signature Apple disponible.

## Lecture des preuves

Les modifications documentaires ne doivent pas être confondues avec le commit applicatif du ZIP testé. Vérifier le SHA de la construction téléchargée. Voir [EXECUTION-LOG](EXECUTION-LOG.md), [tests automatisés](automated-testing.md) et [guide Windows](windows-testing.md).
