# Learn Prolog Now! — ordre OSSU et terminologie française

## Ordre des cours affichés

La version courante de `ossu/computer-science` recommande de parcourir les cours de haut en bas, les cours étant ordonnés par prérequis. L’ordre retenu suit les sections OSSU et non l’ordre lexical des fichiers générés :

| Catégorie | Ordre de cours publié |
| --- | --- |
| Informatique | MIT 6.100L (Intro CS), MIT Missing Semester (CS Tools), MIT 6.004 (Core Systems), Learn Prolog Now! (Advanced Programming), MIT 18.404J (Advanced Theory), Roughgarden (Advanced Theory) |
| Mathématiques | MIT 18.01SC (Calculus), MIT 6.042J (Discrete Mathematics), MIT 18.06SC (Linear Algebra), forall x: Calgary (Advanced Topics / Mathematical Logic), MIT 18.335J (Advanced Topics / Mathematical Analysis) |

Les cours LPN, MIT, Roughgarden et forallx sont ceux actuellement publiés dans ces catégories. LPN est explicitement dans la section Advanced Programming, après Introduction to Haskell et avant Software Debugging/Testing. Les cours MIT correspondant à des matières OSSU sont placés à la position de leur section; les deux cours de mathématiques absents des cours fondamentaux OSSU sont rangés dans les rubriques avancées auxquelles leurs sujets correspondent : logique mathématique, puis analyse numérique. OSSU Math recommande l’ordre de haut en bas pour les prérequis; les matières avancées sont des choix électifs. Un cours non inscrit dans le classement OSSU passe après les cours classés, en conservant l’ordre éditorial reçu, notamment pour TCF.

Source de l’ordre, consultée le 2026-10-20 : [OSSU Computer Science README](https://github.com/ossu/computer-science/blob/master/README.md) (Intro CS, Core CS, Advanced Programming, Advanced Systems et Advanced Theory) et [OSSU Math README](https://github.com/ossu/math/blob/master/README.md) (Core Mathematics et Advanced Topics). Le classement est une adaptation des sections source aux cours effectivement présents dans l’application, pas une liste complète du cursus OSSU. Les URL `master` évoluent avec OSSU; `CourseOrdering.kt` fige l’ordre retenu pour ce catalogue.

MIT 18.335J n’est pas directement listé dans le parcours OSSU consulté : sa
position dans les sujets avancés est un choix local, selon son domaine numérique.
Elle ne constitue pas un prérequis ni une recommandation officielle OSSU. La
version locale de forallx est associée à la logique formelle, sans revendiquer
une édition identique à celle du lien OSSU.

## Revue du français et glossaire

La leçon publiée emploie déjà les termes techniques français usuels. Je n’ai pas remplacé de réponses correctes ni d’identifiants. La correction de fond apportée au chapitre 10 précise que la négation par l’échec exige un **échec fini** de la recherche; un calcul qui ne termine pas ne donne pas une preuve de négation. Le libellé a été normalisé en « négation par l’échec ». Les autres expressions examinées sont conservées, avec équivalent anglais et sens pratique :

| Anglais | Français retenu | Précision |
| --- | --- | --- |
| fact / rule / query | fait / règle / requête | Les trois formes de base d’un programme LPN. |
| atom | atome | Symbole comme `bleu`; les atomes Prolog ne sont pas des propositions logiques. |
| term / compound term | terme / terme composé | Une structure de données; un terme composé a un foncteur et des arguments. |
| functor / arity | foncteur / arité | `trajet/2` désigne le foncteur `trajet` à deux arguments. |
| variable / anonymous variable | variable / variable anonyme | Chaque occurrence de `_` est indépendante. |
| unification / substitution | unification / substitution | L’unification cherche une substitution qui rend les termes identiques. |
| proof search / backtracking | recherche de preuve / retour arrière | Le moteur explore des choix; un échec d’exécution n’est pas une négation logique générale. |
| finite failure | échec fini | La recherche a terminé et n’a trouvé aucune preuve. Condition pertinente pour `\\+ Goal`. |
| cut / negation as failure | coupure / négation par l’échec | `!` contrôle les alternatives de recherche; `\\+ Goal` signifie succès sur échec fini, avec les limites du monde clos et de la terminaison. |
| head / tail | tête / queue | Décomposition d’une liste non vide en `[Tête|Queue]`. |
| definite clause grammar (DCG) | grammaire à clauses définies | Grammaire traduite en clauses Prolog, avec états de liste implicites. |
| difference list / ground term / stream | liste différence / terme clos / flux | Une liste différence encode une entrée et son reste; ground signifie sans variable; stream désigne une ressource d’entrée/sortie. |

Références terminologiques : [Learn Prolog Now!, chapitre 1](https://www.let.rug.nl/bos/lpn/lpnpage.php?pagetype=html&pageid=lpn-htmlch1) définit les atomes, variables, termes et les exemples de base; la [présentation française des auteurs](https://www.let.rug.nl/bos/lpn/lpnpage.php?pageid=french) emploie « programmation logique » et annonce la traduction française *Prolog tout de suite !*. Le texte intégral de cette édition française est un livre imprimé, donc ses choix terminologiques détaillés n’ont pas été vérifiés ici. Pour « négation par l’échec » et coupure, les usages français sont croisés avec le [cours universitaire de Lyon 1 sur la programmation logique](https://offre-de-formations.univ-lyon1.fr/ue-4002-1224%2Fprogrammation-logique.html) et l’[introduction française de Claire Solnon](https://perso.citi-lab.fr/csolnon/prolog.html). La [documentation SWI-Prolog sur la syntaxe](https://www.swi-prolog.org/pldoc/man?section=syntax) sert de référence d’implémentation pour ne pas présenter les détails de syntaxe comme universels à toutes les variantes Prolog.

Cette revue porte sur la terminologie et les formulations de la traduction française; elle ne constitue pas une nouvelle exécution des exemples dans un moteur Prolog. Les identifiants, les indices de bonne réponse et les options n’ont pas changé.
