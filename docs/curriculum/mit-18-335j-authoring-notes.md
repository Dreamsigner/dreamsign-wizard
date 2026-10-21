# MIT 18.335J — Introduction to Numerical Methods, Spring 2019

## Portée de la rédaction

39 leçons françaises, 117 exercices originaux (trois par séance), chacun avec quatre choix distincts, une correction et un indice. Les identifiants stables suivent `mit_18_335j_2019_sNNN_eNN`. Il s’agit d’une unité de micro-apprentissage alignée sur chaque séance ; elle ne reproduit pas les devoirs MIT et ne vaut pas réalisation intégrale du cours de niveau graduate, de ses expériences Julia ou du projet.

Le [calendrier officiel](https://ocw.mit.edu/courses/18-335j-introduction-to-numerical-methods-spring-2019/pages/calendar/) a été lu directement le 20 octobre 2026 : 39 cours numérotés. Les 14 pages hebdomadaires ont été récupérées et leurs résumés de séances examinés ; les URL avec fragments réels, titres et empreintes SHA-256 se trouvent dans `mit-18-335j-source-sessions.json`.

**38 en-têtes hebdomadaires ne signifient pas 38 cours** : les leçons 30 et 31 figurent dans un même en-tête et partagent la note BFGS. Le calendrier les distingue : aperçu quasi-Newton/BFGS puis dérivation de la mise à jour. Les deux leçons de l’application suivent cette répartition et déclarent leur support commun.

Le calendrier annonce les échéances des problem sets, un examen à domicile à remettre avant le cours 25 et un projet final. Ces événements ne deviennent pas artificiellement des séances 40 et suivantes. Les exercices de l’application sont originaux ; ils ne sont ni les sujets, ni les solutions officielles de cet examen ou du projet. Les notebooks externes, manuel commercial et articles cités par les pages ne sont pas déclarés lus par simple présence d’un lien.

## Lecture des supports et limites

Outre les résumés hebdomadaires, 15 PDF distincts ont été effectivement récupérés depuis OCW et traités en mémoire : racines de Newton, normes, solveurs ODE, Gram-Schmidt, Householder, Hessenberg, redémarrage Arnoldi, matrices creuses, aperçu optimisation, adjoints, adjoints des récurrences, BFGS, trapèzes, FFT et FFTW. Le manifeste donne URL directe, nombre de pages, longueur du texte extrait et empreinte binaire. La note BFGS est liée aux deux séances 30–31.

Le texte intégral de ces PDF a été extrait techniquement, mais seuls des extraits textuels d’ouverture ont été inspectés pendant cette rédaction : **aucune affirmation de lecture exhaustive de chaque PDF, de contrôle de tous ses schémas ou de validation de toutes les pages**. Les explications mathématiques rédigées s’appuient aussi sur les résumés détaillés propres aux séances ; les exercices utilisent des petits cas originaux vérifiables indépendamment.

Certains documents ont des dates internes plus anciennes que le semestre d’accueil : Newton 2015 ; normes 2012 ; Gram-Schmidt/Householder et matrices creuses 2006 ; adjoints mis à jour en 2012 ; récurrences mises à jour en 2011 ; trapèzes 2011 ; FFT 2008. La note BFGS est datée du 25 avril 2019. L’édition sélectionnée reste **Spring 2019**, qui publie ou réutilise ces documents. Ces dates sont consignées sans les remplacer par une date fictive de 2019.

Certaines séances ont uniquement un résumé et des lectures conseillées, sans PDF autonome identifié dans la page, notamment la séance 7. Cela est représenté par l’absence de `readSupports`, sans fabriquer de notes MIT manquantes. La séance invitée 6 présente des solveurs ODE modernes ; les deux calculs d’Euler constituent une pratique introductive originale, pas une transcription des slides. Les séances 8 (GSVD), 28 (CCSA), 23 (BiCG) et 39 (FFTW) comprennent des contrôles conceptuels ; les petites questions fermées n’épuisent pas la technicité de ces thèmes.

## Choix mathématiques

- Référence aux erreurs avant/arrière et au conditionnement : stabilité algorithmique et sensibilité des données restent distinctes.
- Convention flottante explicite : écart après 1 en binary64 = 2⁻⁵² ; unité d’arrondi au plus proche = 2⁻⁵³. Le modèle relatif simple exclut les débordements et demande un traitement distinct des sous-normaux.
- La première étape GMRES de l’exemple diagonal donne 3/5 ; la première étape CG donne 2/3, car leurs critères minimisés diffèrent.
- Le conditionnement au carré des équations normales suppose le rang colonne plein et la norme 2.
- Les affirmations de convergence CG en un nombre fini d’étapes sont limitées à l’arithmétique exacte. Les bornes en fonction de κ peuvent être pessimistes.
- Les gradients adjoints utilisent la convention Aᵀλ=∂g/∂u et le signe db/dp−(dA/dp)u ; BFGS distingue approximation de Hessienne B et approximation inverse H.
- Les garanties exponentielles d’approximation ne sont pas attribuées à toute fonction continue. La quadrature de Gauss est présentée dans le cadre habituel d’un poids positif.
- DFT non normalisée à l’aller, inverse divisée par N ; FFT calcule la même transformation. Les affirmations de performances historiques de FFTW ne deviennent pas une promesse actuelle de vitesse sur tout matériel.

## Vérifications reproductibles

Depuis la racine du dépôt, avec Python et SymPy :

```sh
python tools/check_numerical_methods.py
```

Le script vérifie les 39 correspondances de séances, les 117 structures de questions (choix uniques, indices, identifiants et sources), et recalcule les 78 réponses numériques avec arithmétique rationnelle, calcul symbolique, algèbre matricielle ou DFT directe. Il ne se contente pas de comparer deux copies du texte de réponse. Les 39 réponses conceptuelles ont fait l’objet d’une revue éditoriale ; leur vérité n’est pas prétendue prouvée automatiquement par ce script.

Résultat observé lors de la rédaction : PASS pour le checker ; la fonction existante `validate_course` valide aussi ce cours et son manifeste. Aucune génération Kotlin, configuration CI, compilation d’application ou modification de catalogue commun n’a été effectuée par ce lot. L’intégration et les builds relèvent du parent.
