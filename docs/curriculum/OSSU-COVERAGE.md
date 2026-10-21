# OSSU : références, équivalences et séquences source

Audit des 19 et 20 octobre 2026. Le JSON associé conserve les 63 références du cursus fourni, leurs URL originales, les éditions retenues, les index effectivement récupérés et les erreurs d'accès. **63 références ne signifie pas 63 cours distincts** : certaines lignes regroupent plusieurs cours, et les options de projet comprennent des spécialisations.

## Règles de couverture

Un index lu permet de fixer une séquence ; il ne prouve ni la lecture de toute sa matière, ni la rédaction des leçons, ni une application complète. Les champs `sourceSequence` sont des inventaires de source. `playableCoverageStatus` reste non établi par cet audit. Les nombres de conférences, chapitres, projets et cours composants portent chacun leur unité explicite et ne s'additionnent pas en un nombre de « séances ».

Un nombre inconnu reste `null`. Une page publique de présentation accessible n'est pas un programme complet. Les URL des index et les empreintes SHA-256 de leurs réponses figurent dans le JSON. Les réponses HTML ne sont pas archivées ici ; les empreintes identifient la collecte, mais ne remplacent pas une copie reproductible.

## Équivalences et recouvrements établis

- **Calculus 1A/1B/1C** : OSSU indique explicitement que son alternative couvre les trois lignes. L'édition retenue est [MIT 18.01SC Fall 2010](https://ocw.mit.edu/courses/18-01sc-single-variable-calculus-fall-2010/), avec les 102 séances du manifeste existant, laissé intact. Une seule unité 18.01SC remplace les trois références MITx ; les 102 séances ne doivent pas être attribuées trois fois.
- **Computation Structures 1/2/3** : OSSU indique que chacun des liens alternatifs couvre les trois cours MITx. Retenir [MIT 6.004 Spring 2017](https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/), sans ajouter Spring 2009 ni les trois morceaux MITx comme cinq autres cours. Le [calendrier 2017](https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/calendar/) compte 24 conférences L1–L24 et distingue 21 unités thématiques. Il signale que les laboratoires ne sont pas inclus : la séquence de conférences ne constitue donc pas un cours pratique complet.
- **Mathématiques discrètes** : l'alternative OSSU [6.042J Fall 2010](https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-fall-2010/) est retenue pour l'index ; ne pas fusionner sa numérotation avec l'édition Open Learning Library 2T2019.
- **Systematic Program Design** : le guide OSSU relie trois archives UBC SPD1x/2x/3x et l'alternative How to Code Simple Data/Complex Data. Ce sont deux découpages du parcours, pas cinq cours à additionner. L'index détaillé des archives authentifiées reste à établir.
- **Parallel Programming** apparaît aussi comme cours composant de la [spécialisation Scala](https://www.coursera.org/specializations/scala). La même URL canonique `/learn/scala-parallel-programming` doit correspondre à un seul contenu réutilisé dans les deux parcours.
- **Essence of Linear Algebra** et **Linear Algebra** sont un coréquisit et un cours, pas des alternatives équivalentes. Les deux références restent présentes.
- **Nand to Tetris I et II** sont complémentaires : projets 1–6 et 7–12. Le [site des auteurs](https://www.nand2tetris.org/course) confirme 12 projets au total ; ce nombre ne détermine pas le nombre de conférences Coursera.

Les alternatives viennent du [README OSSU](https://github.com/ossu/computer-science), dont l'empreinte de la collecte est enregistrée. Les autres alternatives indiquées dans les lignes du README sont conservées dans `alternativeReferences`; elles ne deviennent pas automatiquement des unités supplémentaires.

## Précautions d'édition et d'ordre

L'accueil de Missing Semester renvoie maintenant à l'édition 2026. Son index comprend neuf conférences ; les liens complémentaires vers quatre contenus de 2020 ne doivent pas être présentés comme quatre séances 2026 supplémentaires.

Le livre [forall x: Calgary en HTML](https://forallx.openlogicproject.org/html/) annonce « Fall 2025 », révision `c5cb415` du 27 septembre 2026. Il comporte 48 chapitres numérotés ; annexes et solutions restent distinctes. L'éditeur prévient que ses liens courants changent : figer la révision avant rédaction.

L'index [CS2510 Spring 2022](https://course.ccs.neu.edu/cs2510sp22/Lectures.html) contient des emplacements 33 et 36 sans page de matière. Les 34 pages de conférence publiées ne doivent pas être déclarées « 36 leçons complètes ». Le [guide OSSU](https://github.com/ossu/computer-science/blob/master/coursepages/class-based/README.md) place volontairement la conférence 30 avant 28 et 29 ; conserver cet ordre pédagogique lors d'une future adaptation.

L'index 18.06SC contient une conférence ajoutée en 2023. Elle est identifiée comme supplément, hors de l'édition Fall 2011 retenue. Les révisions et examens 2011 restent des séances identifiées, plutôt que d'être confondus avec des vidéos supplémentaires.

## Travail restant

Les séquences vérifiées doivent encore recevoir une lecture de leur matière, une correspondance des exercices/TP/examens, puis des leçons rédigées et contrôlées. Les spécialisations nécessitent un manifeste par cours composant. Les cours dont seul l'accueil est accessible nécessitent encore leur véritable programme ; aucun décompte n'est inventé à partir des durées en semaines.

Les erreurs HTTP, TLS, redirections ou délais sont enregistrés tels que reçus. Aucun certificat désactivé, authentification inventée ou refus contourné. Harvard Stat 110 renvoie HTTP 403 lors de cette collecte ; ce refus n'établit pas la disparition du cours.

Le collecteur `tools/inventory_sources.py` écrit désormais des candidats `*-discovery.json`, avec un `referenceCount` explicite. Il préserve cet audit enrichi et les manifestes publiés. Les découvertes ultérieures doivent être rapprochées et relues avant intégration ; une nouvelle réponse de page d’accueil n’efface pas un programme vérifié.

## Compléments vérifiés le 20 octobre

Treize plans publics Coursera exposent leurs modules. Par exemple, Software Architecture en compte quatre ; Nand to Tetris I et II affichent huit et neuf modules Coursera, en plus des six projets de chaque partie chez les auteurs. Ces unités sont conservées séparément. Le nombre de modules ne détermine pas le nombre de conférences ou d’exercices.

Les sept spécialisations explicitement nommées dans les options OSSU exposent 44 cours composants. Le site des auteurs de Modern Robotics renvoie aussi vers une spécialisation de six cours : huit conteneurs et 50 appartenances de cours sont donc documentés, avant dédoublonnage des recouvrements. Aucun nombre global de cours distincts n’est annoncé.

L’alternative officielle UBC CPSC310 comprend six modules de fond et une introduction numérotée 0 : les sept sections 0–6 et leurs liens de ressources sont conservés. Ce ne sont ni sept séances officielles ni treize leçons déduites de la durée du semestre.

Les douze URL edX examinées ne fournissent pas de séquence ordonnée exploitable dans les réponses obtenues. Les trois anciens liens Secure Software Development redirigent vers le catalogue du fournisseur Linux Foundation, sans programme du cours : l’URL finale et cette différence sont enregistrées. Les réponses des autres pages exposent principalement l’application du catalogue ; aucun nombre de séances n’est déduit du seul libellé « Syllabus » présent dans les scripts.

Les quinze URL de parties Full Stack Open (0–14) sont repérées, mais le parseur HTML n’a pas extrait leurs titres dans les pages récupérées. Ce relevé reste donc partiel ; il faut encore lire l’index rendu, ses sections et exercices.

## Relevé par référence

Les liens conduisent à l’index effectivement consulté lorsqu’il est établi ; sinon à la référence initiale. « Index vérifié » concerne uniquement l’unité indiquée. Les inventaires partiels ne donnent pas un total officiel de séances. Les erreurs détaillées et liens individuels restent dans le JSON.

| ID | Référence | Édition ou instantané | Décompte et unité | État |
|---|---|---|---|---|
| ossu_001 | [Introduction to Computer Science and Programming using Python](https://ocw.mit.edu/courses/6-100l-introduction-to-cs-and-programming-using-python-fall-2022/pages/material-by-lecture/) | Fall 2022, MIT 6.100L | 26 lecture | Index vérifié |
| ossu_002 | [Systematic Program Design](https://learning.edx.org/course/course-v1:UBCx+SPD1x+2T2015) | Non établie | Inconnu | Séquence à établir |
| ossu_003 | [Class-based Program Design](https://course.ccs.neu.edu/cs2510sp22/Lectures.html) | Spring 2022, Northeastern CS2510 | 34 published_lecture_page | Index partiel |
| ossu_004 | [Programming Languages](https://courses.cs.washington.edu/courses/cse341/19sp/) | Spring 2019, University of Washington CSE341 | 26 lecture_slide_set | Index partiel |
| ossu_005 | [Object-Oriented Design](https://course.ccs.neu.edu/cs3500f19/) | Fall 2019, Northeastern CS3500 | 25 scheduled_meeting | Index vérifié |
| ossu_006 | [Software Architecture](https://www.coursera.org/learn/software-architecture) | Live public course outline, consulted 2026-10-20; fixed edition not stated | 4 module | Index vérifié |
| ossu_007 | [Calculus 1A: Differentiation](https://openlearninglibrary.mit.edu/courses/course-v1:MITx+18.01.1x+2T2019/about) | Non établie | mit_calculus (compter une fois) | Alternative commune sélectionnée |
| ossu_008 | [Calculus 1B: Integration](https://openlearninglibrary.mit.edu/courses/course-v1:MITx+18.01.2x+3T2019/about) | Non établie | mit_calculus (compter une fois) | Alternative commune sélectionnée |
| ossu_009 | [Calculus 1C: Coordinate Systems & Infinite Series](https://openlearninglibrary.mit.edu/courses/course-v1:MITx+18.01.3x+1T2020/about) | Non établie | mit_calculus (compter une fois) | Alternative commune sélectionnée |
| ossu_010 | [Mathematics for Computer Science](https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-fall-2010/video_galleries/video-lectures/) | Fall 2010, MIT 6.042J | 25 lecture | Index vérifié |
| ossu_011 | [The Missing Semester of Your CS Education](https://missing.csail.mit.edu/2026/) | IAP 2026, The Missing Semester | 9 lecture | Index vérifié |
| ossu_012 | [Build a Modern Computer from First Principles: From Nand to Tetris](https://www.coursera.org/learn/build-a-computer) | Live public course outline, consulted 2026-10-20; fixed edition not stated | 8 module | Index vérifié |
| ossu_013 | [Build a Modern Computer from First Principles: Nand to Tetris Part II](https://www.coursera.org/learn/nand2tetris2) | Live public course outline, consulted 2026-10-20; fixed edition not stated | 9 module | Index vérifié |
| ossu_014 | [Operating Systems: Three Easy Pieces](https://pages.cs.wisc.edu/~remzi/Classes/537/Spring2018/Discussion/videos.html) | Spring 2018, University of Wisconsin CS537 | 14 lecture | Index vérifié |
| ossu_015 | [Computer Networking: a Top-Down Approach](https://gaia.cs.umass.edu/kurose_ross/online_lectures.htm) | Non établie | Inconnu | Échec accès |
| ossu_016 | [Algorithms: Design and Analysis, Part 1](https://www.edx.org/learn/algorithms/stanford-university-algorithms-design-and-analysis-part-1) | Non établie | Inconnu | Séquence à établir |
| ossu_017 | [Algorithms: Design and Analysis, Part 2](https://www.edx.org/learn/algorithms/stanford-university-algorithms-design-and-analysis-part-2) | Non établie | Inconnu | Séquence à établir |
| ossu_018 | [Cybersecurity Fundamentals](https://www.edx.org/learn/cybersecurity/rochester-institute-of-technology-cybersecurity-fundamentals) | Non établie | Inconnu | Séquence à établir |
| ossu_019 | [Principles of Secure Coding](https://www.coursera.org/learn/secure-coding-principles) | Live public course outline, consulted 2026-10-20; fixed edition not stated | 4 module | Index vérifié |
| ossu_020 | [Identifying Security Vulnerabilities](https://www.coursera.org/learn/identifying-security-vulnerabilities) | Live public course outline, consulted 2026-10-20; fixed edition not stated | 4 module | Index vérifié |
| ossu_021 | [Identifying Security Vulnerabilities in C/C++ Programming](https://www.coursera.org/learn/identifying-security-vulnerabilities-c-programming) | Live public course outline, consulted 2026-10-20; fixed edition not stated | 4 module | Index vérifié |
| ossu_022 | [Exploiting and Securing Vulnerabilities in Java Applications](https://www.coursera.org/learn/exploiting-securing-vulnerabilities-java-applications) | Live public course outline, consulted 2026-10-20; fixed edition not stated | 4 module | Index vérifié |
| ossu_023 | [Databases: Modeling and Theory](https://www.edx.org/learn/databases/stanford-university-databases-modeling-and-theory) | Non établie | Inconnu | Séquence à établir |
| ossu_024 | [Databases: Relational Databases and SQL](https://www.edx.org/learn/relational-databases/stanford-university-databases-relational-databases-and-sql) | Non établie | Inconnu | Séquence à établir |
| ossu_025 | [Databases: Semistructured Data](https://www.edx.org/learn/relational-databases/stanford-university-databases-semistructured-data) | Non établie | Inconnu | Séquence à établir |
| ossu_026 | [Machine Learning](https://www.deeplearning.ai/courses/machine-learning-specialization/) | Non établie | Inconnu | Séquence à établir |
| ossu_027 | [Computer Graphics](https://www.edx.org/learn/computer-graphics/the-university-of-california-san-diego-computer-graphics) | Non établie | Inconnu | Séquence à établir |
| ossu_028 | [Software Engineering: Introduction](https://raw.githubusercontent.com/ubccpsc/310/main/resources/README.md) | Live UBC CPSC310 resources main branch, consulted 2026-10-20; fixed semester not stated | 7 module_section | Index vérifié |
| ossu_029 | [Ethics, Technology and Engineering](https://www.coursera.org/learn/ethics-technology-engineering) | Live public course outline, consulted 2026-10-20; fixed edition not stated | 10 module | Index vérifié |
| ossu_030 | [Introduction to Intellectual Property](https://www.coursera.org/learn/introduction-intellectual-property) | Live public course outline, consulted 2026-10-20; fixed edition not stated | 4 module | Index vérifié |
| ossu_031 | [Data Privacy Fundamentals](https://www.coursera.org/learn/northeastern-data-privacy) | Live public course outline, consulted 2026-10-20; fixed edition not stated | 3 module | Index vérifié |
| ossu_032 | [Parallel Programming](https://www.coursera.org/learn/scala-parallel-programming) | Live public course outline, consulted 2026-10-20; fixed edition not stated | 4 module | Index vérifié |
| ossu_033 | [Compilers](https://www.edx.org/learn/computer-science/stanford-university-compilers) | Non établie | Inconnu | Séquence à établir |
| ossu_034 | [Introduction to Haskell](https://www.seas.upenn.edu/~cis194/fall16/) | Non établie | Inconnu | Échec accès |
| ossu_035 | [Learn Prolog Now!](https://www.let.rug.nl/bos/lpn//lpnpage.php?pageid=online) | Learn Prolog Now! online textbook, live index consulted 2026-10-20; fixed edition not stated | 12 chapter | Index vérifié |
| ossu_036 | [Software Debugging](https://www.youtube.com/playlist?list=PLAwxTw4SYaPkxK63TiT88oEe-AIBhr96A) | Non établie | Inconnu | Séquence à établir |
| ossu_037 | [Software Testing](https://www.youtube.com/playlist?list=PLAwxTw4SYaPkWVHeC_8aSIbSxE_NXI76g) | Non établie | Inconnu | Séquence à établir |
| ossu_038 | [Computation Structures 1: Digital Circuits](https://learning.edx.org/course/course-v1:MITx+6.004.1x_3+3T2016) | Non établie | mit_computation_structures (compter une fois) | Alternative commune sélectionnée |
| ossu_039 | [Computation Structures 2: Computer Architecture](https://learning.edx.org/course/course-v1:MITx+6.004.2x+3T2015) | Non établie | mit_computation_structures (compter une fois) | Alternative commune sélectionnée |
| ossu_040 | [Computation Structures 3: Computer Organization](https://learning.edx.org/course/course-v1:MITx+6.004.3x_2+1T2017) | Non établie | mit_computation_structures (compter une fois) | Alternative commune sélectionnée |
| ossu_041 | [Theory of Computation](https://ocw.mit.edu/courses/18-404j-theory-of-computation-fall-2020/video_galleries/video-lectures/) | Fall 2020, MIT 18.404J | 25 lecture | Index vérifié |
| ossu_042 | [Computational Geometry](https://www.edx.org/learn/geometry/tsinghua-university-ji-suan-ji-he-computational-geometry) | Non établie | Inconnu | Séquence à établir |
| ossu_043 | [Algorithmic Game Theory](https://timroughgarden.org/f13/f13.html) | Fall 2013, Stanford CS364A | 20 lecture | Index vérifié |
| ossu_044 | [Web Security Fundamentals](https://www.edx.org/learn/computer-security/ku-leuven-web-security-fundamentals) | Non établie | Inconnu | Séquence à établir |
| ossu_045 | [Security Governance & Compliance](https://www.coursera.org/learn/security-governance-compliance) | Live public course outline, consulted 2026-10-20; fixed edition not stated | 4 module | Index vérifié |
| ossu_046 | [Digital Forensics Concepts](https://www.coursera.org/learn/digital-forensics-concepts) | Live public course outline, consulted 2026-10-20; fixed edition not stated | 11 module | Index vérifié |
| ossu_047 | [Secure Software Development: Requirements, Design, and Reuse](https://www.edx.org/learn/software-development/the-linux-foundation-secure-software-development-requirements-design-and-reuse) | Non établie | Inconnu | Séquence à établir |
| ossu_048 | [Secure Software Development: Implementation](https://www.edx.org/learn/software-development/the-linux-foundation-secure-software-development-implementation) | Non établie | Inconnu | Séquence à établir |
| ossu_049 | [Secure Software Development: Verification and More Specialized Topics](https://www.edx.org/learn/software-engineering/the-linux-foundation-secure-software-development-verification-and-more-specialized-topics) | Non établie | Inconnu | Séquence à établir |
| ossu_050 | [Essence of Linear Algebra](https://www.youtube.com/playlist?list=PLZHQObOWTQDPD3MizzM2xVFitgF8hE_ab) | Non établie | Inconnu | Séquence à établir |
| ossu_051 | [Linear Algebra](https://ocw.mit.edu/courses/18-06sc-linear-algebra-fall-2011/pages/ax-b-and-the-four-subspaces/) | Fall 2011, MIT 18.06SC | 40 session | Index vérifié |
| ossu_052 | [Introduction to Numerical Methods](https://ocw.mit.edu/courses/18-335j-introduction-to-numerical-methods-spring-2019/pages/calendar/) | Spring 2019, MIT 18.335J | 39 lecture | Index vérifié |
| ossu_053 | [Introduction to Formal Logic](https://forallx.openlogicproject.org/html/) | Fall 2025, revision c5cb415, 2026-09-27 | 48 chapter | Index vérifié |
| ossu_054 | [Probability](https://stat110.hsites.harvard.edu/) | Non établie | Inconnu | Échec accès |
| ossu_055 | [Fullstack Open](https://fullstackopen.com/en/) | Full Stack Open live English index, consulted 2026-10-20; fixed release not stated | 15 part | Index partiel |
| ossu_056 | [Modern Robotics](https://www.coursera.org/specializations/modernrobotics) | Modern Robotics live specialization, consulted 2026-10-20; fixed edition not stated | 6 component_course | Cours composants vérifiés |
| ossu_057 | [Data Mining (Specialization)](https://www.coursera.org/specializations/data-mining) | Live specialization structure consulted 2026-10-20; fixed edition not stated | 6 component_course | Cours composants vérifiés |
| ossu_058 | [Big Data (Specialization)](https://www.coursera.org/specializations/big-data) | Live specialization structure consulted 2026-10-20; fixed edition not stated | 6 component_course | Cours composants vérifiés |
| ossu_059 | [Internet of Things (Specialization)](https://www.coursera.org/specializations/internet-of-things) | Live specialization structure consulted 2026-10-20; fixed edition not stated | 6 component_course | Cours composants vérifiés |
| ossu_060 | [Cloud Computing (Specialization)](https://www.coursera.org/specializations/cloud-computing) | Live specialization structure consulted 2026-10-20; fixed edition not stated | 6 component_course | Cours composants vérifiés |
| ossu_061 | [Data Science (Specialization)](https://www.coursera.org/specializations/jhu-data-science) | Live specialization structure consulted 2026-10-20; fixed edition not stated | 10 component_course | Cours composants vérifiés |
| ossu_062 | [Functional Programming in Scala (Specialization)](https://www.coursera.org/specializations/scala) | Live specialization structure consulted 2026-10-20; fixed edition not stated | 5 component_course | Cours composants vérifiés |
| ossu_063 | [Game Design and Development with Unity 2020 (Specialization)](https://www.coursera.org/specializations/game-design-and-development) | Live specialization structure consulted 2026-10-20; fixed edition not stated | 5 component_course | Cours composants vérifiés |
