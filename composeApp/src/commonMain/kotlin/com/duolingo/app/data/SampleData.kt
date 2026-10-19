package com.duolingo.app.data

import com.duolingo.app.model.*

/**
 * First authored curriculum release. IDs are permanent: new releases append lessons and never
 * rename an existing ID, so progress can be migrated without tying content to UI code.
 * Topics follow OSSU CS and OSSU Math; lesson explanations and exercises are original.
 */
object SampleData {
    private const val OSSU_CS = "https://github.com/ossu/computer-science"
    private const val OSSU_MATH = "https://github.com/ossu/math"
    private const val MIT_MATH_CS = "https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-spring-2015/"
    private const val MIT_LINEAR = "https://ocw.mit.edu/courses/18-06-linear-algebra-spring-2010/"
    private const val MIT_PROBABILITY = "https://ocw.mit.edu/courses/18-05-introduction-to-probability-and-statistics-spring-2022/"
    private const val MIT_CALCULUS = "https://ocw.mit.edu/courses/18-01sc-single-variable-calculus-fall-2010/"
    private const val MIT_ALGORITHMS = "https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/"

    private fun mc(
        id: String, prompt: String, options: List<String>, answer: Int, explanation: String, hint: String
    ) = Exercise.MultipleChoice(id, prompt, options, answer, explanation, hint)

    private fun math(
        id: String, prompt: String, formula: String? = null, options: List<String>, answer: Int,
        explanation: String, hint: String
    ) = Exercise.MathChallenge(id, prompt, formula, options, answer, explanation, hint)

    private fun code(
        id: String, prompt: String, snippet: String, options: List<String>, answer: Int,
        explanation: String, hint: String
    ) = Exercise.CodeChallenge(id, prompt, snippet, options, answer, explanation, hint)

    private fun lesson(
        id: String, title: String, objective: String, notes: String, source: String,
        exercises: List<Exercise>, xp: Int = 20, prerequisites: List<String> = emptyList()
    ) = Lesson(
        id = id,
        title = title,
        xpReward = xp,
        isLocked = false,
        exercises = exercises,
        objective = objective,
        learningNotes = notes,
        sourceUrls = listOf(source),
        prerequisiteLessonIds = prerequisites
    )

    private fun unit(id: String, number: Int, title: String, description: String, source: String, lesson: Lesson) =
        LearningUnit(id, number, title, description, listOf(lesson), source)

    private val baseSubjects = listOf(
        Subject(
            id = "math_vectors",
            title = "Mathématiques",
            icon = "📐",
            description = "Du raisonnement mathématique aux outils utilisés en informatique.",
            category = SubjectCategory.MATHEMATICS,
            curriculumVersion = 2,
            units = listOf(
                unit("math_reasoning", 1, "Raisonnement & preuves", "Logique, implications et preuves par récurrence.", MIT_MATH_CS,
                    lesson("math_reasoning_logic", "Raisonner avec une implication", "Distinguer une implication de sa réciproque.",
                        "Une implication P ⇒ Q est fausse uniquement lorsque P est vraie et Q est fausse. Sa contraposée ¬Q ⇒ ¬P lui est équivalente.", MIT_MATH_CS, listOf(
                            mc("math_logic_1", "Quelle phrase est équivalente à « si n est divisible par 4, alors n est pair » ?", listOf("Si n est pair, alors il est divisible par 4", "Si n n’est pas pair, alors il n’est pas divisible par 4", "n est divisible par 4 si et seulement s’il est pair", "Aucune valeur de n n’est paire"), 1, "La contraposée inverse et nie les deux propositions : si n n’est pas pair, alors il n’est pas divisible par 4.", "Inverse l’ordre des deux parties et nie chacune d’elles."),
                            mc("math_logic_2", "Pour réfuter « tout entier premier est impair », quel exemple suffit ?", listOf("3", "5", "2", "9"), 2, "2 est premier et pair : un seul contre-exemple invalide une affirmation universelle.", "Cherche le plus petit nombre premier qui ne respecte pas la propriété.")), xp = 15)),
                unit("math_calculus", 2, "Calcul différentiel", "Taux de variation, dérivées et optimisation.", MIT_CALCULUS,
                    lesson("math_calculus_derivative", "Lire une dérivée", "Interpréter la dérivée comme pente locale et taux de variation.",
                        "La dérivée f′(a) est la pente de la tangente au graphe en a. Elle mesure aussi le taux de variation instantané de f.", MIT_CALCULUS, listOf(
                            math("math_derivative_1", "Si f(x)=x², quelle est f′(3) ?", "f′(x)=2x", listOf("3", "6", "9", "12"), 1, "f′(x)=2x, donc f′(3)=6.", "Dérive d’abord x², puis remplace x par 3."),
                            mc("math_derivative_2", "Que représente géométriquement f′(a) ?", listOf("L’aire sous la courbe", "La pente de la tangente en a", "La valeur maximale de f", "La distance entre deux points"), 1, "La dérivée donne la pente locale de la tangente au point considéré.", "Pense à la limite des pentes des sécantes.")), xp = 20)),
                unit("math_calculus_integral", 3, "Calcul intégral", "Intégrales comme accumulation et aire signée.", MIT_CALCULUS,
                    lesson("math_calculus_integral_intro", "Comprendre une intégrale définie", "Relier une intégrale à une accumulation sur un intervalle.",
                        "L’intégrale définie accumule les contributions de f(x) entre a et b. Si f est positive, elle correspond à l’aire sous la courbe.", MIT_CALCULUS, listOf(
                            math("math_integral_1", "Quelle est ∫₀² 3 dx ?", "∫₀² 3 dx = 3 × (2 − 0)", listOf("3", "5", "6", "9"), 2, "La fonction constante vaut 3 sur une largeur de 2 : l’aire vaut 6.", "Pour une constante, multiplie sa hauteur par la largeur de l’intervalle."),
                            mc("math_integral_2", "Si f(x) est négative sur [a,b], que peut-on dire de ∫ₐᵇ f(x)dx ?", listOf("Elle est négative", "Elle est forcément nulle", "Elle est positive", "Elle n’existe jamais"), 0, "L’intégrale est une aire signée : les contributions sous l’axe sont négatives.", "Une intégrale tient compte du signe de la fonction.")), xp = 20, prerequisites = listOf("math_calculus_derivative"))),
                unit("math_discrete", 4, "Mathématiques discrètes", "Dénombrement, induction, graphes et probabilités discrètes.", MIT_MATH_CS,
                    lesson("math_discrete_counting", "Compter sans énumérer", "Utiliser les règles de somme et de produit en dénombrement.",
                        "Pour des choix successifs indépendants, le principe multiplicatif multiplie le nombre de possibilités de chaque étape. Pour des cas disjoints, le principe additif additionne.", MIT_MATH_CS, listOf(
                            math("math_count_1", "Un code a 2 lettres puis 3 chiffres. Combien de codes sont possibles si les répétitions sont permises ?", null, listOf("26 + 10", "26² × 10³", "26 × 3", "36⁵"), 1, "Il y a 26 choix pour chacune des deux lettres et 10 pour chacun des trois chiffres.", "Les choix sont successifs : applique le principe multiplicatif."),
                            mc("math_count_2", "Combien de façons de choisir 2 personnes parmi 5, sans ordre ?", listOf("10", "20", "25", "5"), 0, "Le nombre de paires est C(5,2)=5×4/(2×1)=10.", "Quand l’ordre ne compte pas, utilise un coefficient binomial.")), xp = 20)),
                unit("math_linear", 5, "Algèbre linéaire", "Vecteurs, systèmes linéaires et transformations matricielles.", MIT_LINEAR,
                    lesson("math_linear_vectors", "Norme et produit scalaire", "Calculer une longueur et mesurer l’alignement de deux vecteurs.",
                        "La norme euclidienne mesure la longueur d’un vecteur. Le produit scalaire u·v=Σuᵢvᵢ relie les coordonnées à l’angle entre deux vecteurs.", MIT_LINEAR, listOf(
                            math("math_vector_1", "Quelle est la norme de u=(3,4) ?", "||u|| = √(x²+y²)", listOf("5", "7", "25", "12"), 0, "||u||=√(3²+4²)=√25=5.", "Applique Pythagore aux coordonnées."),
                            math("math_vector_2", "Calculer (1,2)·(4,−1).", "u·v = u₁v₁ + u₂v₂", listOf("2", "6", "−2", "4"), 0, "1×4 + 2×(−1)=4−2=2.", "Multiplie les coordonnées correspondantes puis additionne.")), xp = 20)),
                unit("math_probability", 6, "Probabilités & statistiques", "Variables aléatoires, inférence et raisonnement bayésien.", MIT_PROBABILITY,
                    lesson("math_probability_conditional", "Probabilité conditionnelle", "Interpréter P(A|B) et appliquer la formule de Bayes.",
                        "P(A|B)=P(A∩B)/P(B) quand P(B)>0. On restreint l’univers aux cas où B est réalisé. Bayes inverse le conditionnement avec les probabilités a priori et la vraisemblance.", MIT_PROBABILITY, listOf(
                            math("math_prob_1", "Dans une classe de 20, 8 étudient l’info et 5 de ces 8 font aussi des maths. Quelle est P(maths | info) ?", "P(M|I)=P(M∩I)/P(I)", listOf("5/20", "8/20", "5/8", "13/20"), 2, "Parmi les 8 élèves qui étudient l’informatique, 5 font aussi des maths : 5/8.", "Une fois l’information connue, le dénominateur devient le groupe des 8."),
                            mc("math_prob_2", "Laquelle décrit la formule de Bayes ?", listOf("Elle permet d’inverser un conditionnement avec les probabilités associées", "Elle additionne deux événements incompatibles", "Elle calcule toujours une moyenne", "Elle prouve l’indépendance"), 0, "Bayes exprime P(A|B) à partir de P(B|A), P(A) et P(B).", "Cherche l’outil qui transforme P(B sachant A) en P(A sachant B).")), xp = 25, prerequisites = listOf("math_discrete_counting"))),
                unit("math_diff_eq", 7, "Équations différentielles", "Modélisation de phénomènes qui évoluent dans le temps.", "https://ocw.mit.edu/courses/18-03sc-differential-equations-fall-2011/",
                    lesson("math_diff_eq_growth", "Croissance exponentielle", "Reconnaître un modèle où la variation est proportionnelle à la quantité.",
                        "Dans y′=ky, la variation instantanée est proportionnelle à y. La solution est y(t)=y₀eᵏᵗ ; k positif décrit une croissance, k négatif une décroissance.", "https://ocw.mit.edu/courses/18-03sc-differential-equations-fall-2011/", listOf(
                            mc("math_diff_1", "Dans y′=−0,2y, le signe de k décrit plutôt…", listOf("une croissance", "une décroissance", "une oscillation périodique", "une valeur constante obligatoire"), 1, "Le coefficient négatif fait décroître la solution exponentielle.", "Observe le signe du coefficient qui multiplie y."),
                            math("math_diff_2", "Pour y′=3y, quelle forme a la solution générale ?", null, listOf("y=C+3t", "y=Ce³ᵗ", "y=3eᵗ", "y=Ct³"), 1, "L’équation y′=ky a pour solution y=Ceᵏᵗ.", "La fonction doit rester proportionnelle à sa propre dérivée.")), xp = 25, prerequisites = listOf("math_calculus_derivative"))),
                unit("math_advanced", 8, "Analyse & algèbre abstraite", "Limites, structures algébriques et approfondissements au choix.", "https://github.com/ossu/math#advanced-topics",
                    lesson("math_analysis_limits", "Lire une limite", "Comprendre le comportement local d’une fonction.",
                        "Une limite décrit la valeur vers laquelle f(x) tend quand x s’approche d’un point, sans exiger que f soit définie en ce point.", "https://github.com/ossu/math#advanced-topics", listOf(
                            math("math_limit_1", "Pour x≠1, f(x)=(x²−1)/(x−1). Quelle est limₓ→₁ f(x) ?", null, listOf("0", "1", "2", "La limite n’existe pas"), 2, "On factorise x²−1=(x−1)(x+1), donc f(x)=x+1 pour x≠1 et la limite vaut 2.", "Factorise avant de remplacer x par 1."),
                            mc("math_algebra_1", "Dans un groupe, quel élément possède chaque élément ?", listOf("Un inverse", "Un carré", "Un ordre total", "Une dérivée"), 0, "Un groupe est muni d’une opération, d’un élément neutre et d’un inverse pour chaque élément.", "Pense à l’élément qui annule l’effet de l’opération.")), xp = 25, prerequisites = listOf("math_discrete_counting")))
            )
        ),
        Subject(
            id = "cs_algo_cpp",
            title = "Informatique",
            icon = "💻",
            description = "Programmation, systèmes, algorithmes et fondements de la sécurité.",
            category = SubjectCategory.COMPUTER_SCIENCE,
            curriculumVersion = 2,
            units = listOf(
                unit("cs_intro", 1, "Programmation & conception", "Décomposition, fonctions, types et tests.", OSSU_CS,
                    lesson("cs_intro_decomposition", "Décomposer un problème", "Transformer un problème en étapes testables.",
                        "Une fonction utile a une responsabilité claire, des entrées explicites et un résultat observable. Décomposer réduit la complexité et facilite les tests.", "https://www.edx.org/learn/computer-science/university-of-british-columbia-how-to-code-simple-data",
                        listOf(mc("cs_decomp_1", "Quel est le meilleur premier pas face à un problème complexe ?", listOf("Le découper en sous-problèmes avec entrées et résultats clairs", "Écrire toute l’application d’un seul bloc", "Supprimer les tests", "Optimiser avant de comprendre"), 0, "Des sous-problèmes bien définis sont plus faciles à comprendre, implémenter et vérifier.", "Choisis la démarche qui rend le résultat de chaque étape vérifiable."),
                            mc("cs_decomp_2", "À quoi sert principalement un test unitaire ?", listOf("Vérifier une petite unité de comportement isolément", "Mesurer la vitesse du réseau", "Remplacer les types", "Compiler sans code source"), 0, "Un test unitaire vérifie le comportement attendu d’une petite partie du programme.", "Le mot unitaire désigne une partie testée séparément.")), xp = 15)),
                unit("cs_tools", 2, "Outils de développement", "Terminal, scripts, édition et contrôle de version.", "https://missing.csail.mit.edu/",
                    lesson("cs_tools_git", "Comprendre les commits Git", "Lire un historique de versions et distinguer les états du dépôt.",
                        "Un commit enregistre un instantané du projet. Le répertoire de travail, l’index et l’historique sont trois états distincts ; add prépare, commit enregistre.", "https://missing.csail.mit.edu/2020/version-control/", listOf(
                            mc("cs_git_1", "Quelle commande prépare un fichier avant le commit ?", listOf("git add", "git log", "git branch", "git status --delete"), 0, "git add place les modifications sélectionnées dans l’index.", "Le commit n’enregistre que ce qui a été placé dans la zone de préparation."),
                            mc("cs_git_2", "À quoi sert principalement git status ?", listOf("Voir les changements et l’état du dépôt", "Créer automatiquement un commit", "Télécharger un cours", "Fusionner toutes les branches"), 0, "git status indique les fichiers modifiés, préparés ou non suivis.", "Cette commande inspecte l’état courant sans modifier l’historique.")), xp = 15)),
                unit("cs_systems", 3, "Systèmes & architecture", "Bits, logique, mémoire, processeur et système d’exploitation.", "https://www.nand2tetris.org/",
                    lesson("cs_systems_bits", "Du binaire aux octets", "Lire les unités élémentaires de représentation en mémoire.",
                        "Un bit vaut 0 ou 1. Un octet contient 8 bits et peut représenter 256 motifs distincts ; le processeur exécute des instructions en manipulant ces représentations.", "https://www.nand2tetris.org/", listOf(
                            math("cs_bits_1", "Combien de valeurs différentes représentent 8 bits ?", "2⁸", listOf("8", "16", "128", "256"), 3, "Chaque bit a deux états, donc 2⁸=256 combinaisons.", "Un bit double le nombre de motifs possibles."),
                            mc("cs_bits_2", "Quel composant conserve temporairement les données utilisées par le processeur ?", listOf("La mémoire vive (RAM)", "Le clavier", "Le compilateur", "Le protocole HTTP"), 0, "La RAM conserve temporairement les instructions et données actives.", "Pense à la mémoire de travail, volatile et directement utilisée pendant l’exécution.")), xp = 20)),
                unit("cs_algorithms", 4, "Algorithmes & structures", "Complexité, tri, graphes et programmation dynamique.", MIT_ALGORITHMS,
                    lesson("cs_algorithms_bfs", "Parcours en largeur (BFS)", "Choisir la structure de données adaptée à un parcours de graphe.",
                        "BFS visite les sommets par distance croissante depuis la source. Une file FIFO garantit que les voisins proches sont traités avant les niveaux suivants.", MIT_ALGORITHMS, listOf(
                            mc("cs_bfs_1", "Quelle structure de données utilise BFS ?", listOf("Une file FIFO", "Une pile LIFO uniquement", "Un tas min obligatoire", "Une table de hachage ordonnée"), 0, "La file garde l’ordre de découverte et visite les sommets niveau par niveau.", "Les premiers sommets découverts doivent être traités les premiers."),
                            mc("cs_bfs_2", "Dans un graphe non pondéré, que calcule BFS depuis une source ?", listOf("Le plus court nombre d’arêtes vers chaque sommet accessible", "Le plus grand arbre couvrant pondéré", "Une coloration minimale garantie", "La liste triée des étiquettes"), 0, "Le parcours en largeur atteint les sommets par couches, donc donne les distances minimales en nombre d’arêtes.", "La distance est mesurée en nombre d’arêtes, toutes de même coût.")), xp = 25)),
                unit("cs_security", 5, "Sécurité informatique", "Confidentialité, intégrité, disponibilité et programmation défensive.", "https://owasp.org/www-project-top-ten/",
                    lesson("cs_security_cia", "Le triptyque CIA", "Relier les besoins de sécurité aux propriétés d’un système.",
                        "Confidentialité : seuls les acteurs autorisés lisent les données. Intégrité : les changements non autorisés sont détectés ou empêchés. Disponibilité : le service reste accessible.", "https://owasp.org/www-project-top-ten/", listOf(
                            mc("cs_cia_1", "Une signature numérique permet surtout de vérifier…", listOf("l’intégrité et l’authenticité", "la disponibilité du serveur", "la vitesse du réseau", "la taille du fichier"), 0, "La signature permet de vérifier l’origine et que le message n’a pas été altéré.", "Demande-toi quelle propriété révèle une modification du contenu."),
                            mc("cs_cia_2", "Un service protégé contre une panne prolongée vise d’abord…", listOf("la disponibilité", "la confidentialité", "l’anonymat", "la compression"), 0, "La disponibilité concerne l’accès aux services et données lorsqu’ils sont nécessaires.", "La propriété recherchée concerne la continuité d’accès.")), xp = 20)),
                unit("cs_apps", 6, "Applications & données", "Bases relationnelles, modèles et interfaces de service.", "https://www.postgresql.org/docs/current/tutorial.html",
                    lesson("cs_apps_relational", "Clés et relations", "Comprendre les identifiants et les liens entre tables.",
                        "Une clé primaire identifie une ligne de manière unique. Une clé étrangère référence une clé d’une autre table et exprime une relation vérifiable.", "https://www.postgresql.org/docs/current/tutorial.html", listOf(
                            mc("cs_db_1", "Quel est le rôle d’une clé primaire ?", listOf("Identifier chaque ligne de façon unique", "Chiffrer automatiquement la base", "Trier toutes les colonnes", "Créer une sauvegarde"), 0, "La clé primaire identifie sans ambiguïté une ligne de la table.", "Une clé primaire doit permettre de retrouver une ligne précise."),
                            mc("cs_db_2", "Une clé étrangère sert principalement à…", listOf("référencer une ligne d’une autre table", "stocker un mot de passe en clair", "compresser les colonnes", "remplacer une requête SQL"), 0, "La clé étrangère lie les enregistrements et aide à préserver l’intégrité référentielle.", "Pense à un lien entre deux tables.")), xp = 20)),
                unit("cs_ethics", 7, "Éthique & société", "Vie privée, propriété intellectuelle et conséquences des systèmes.", OSSU_CS,
                    lesson("cs_ethics_privacy", "Minimiser les données", "Réduire les risques liés à la collecte et à la conservation.",
                        "La minimisation consiste à ne collecter et conserver que les données nécessaires à une finalité définie. Moins de données réduit l’exposition en cas d’accès abusif.", "https://github.com/ossu/computer-science#core-ethics", listOf(
                            mc("cs_privacy_1", "Quel choix suit le mieux le principe de minimisation ?", listOf("Ne demander que les informations nécessaires au service", "Collecter tous les champs au cas où", "Conserver éternellement les journaux", "Partager les données sans raison"), 0, "La minimisation limite la collecte aux données nécessaires à une finalité précise.", "Compare la finalité du service avec chaque donnée demandée."),
                            mc("cs_privacy_2", "Pourquoi fixer une durée de conservation ?", listOf("Réduire l’exposition des données devenues inutiles", "Augmenter la taille de la base", "Éviter les sauvegardes", "Rendre les données publiques"), 0, "Effacer ce qui n’est plus nécessaire réduit les risques et respecte la finalité annoncée.", "Pense au risque créé par une donnée gardée sans utilité.")), xp = 20)),
                unit("cs_advanced", 8, "Approfondissements", "Compilateurs, théorie du calcul, systèmes avancés et cours au choix.", "https://ocw.mit.edu/courses/6-045j-automata-computability-and-complexity-spring-2011/",
                    lesson("cs_theory_automata", "Automates finis", "Modéliser un langage simple avec des états et transitions.",
                        "Un automate fini lit une entrée symbole par symbole, change d’état selon sa transition, puis accepte ou rejette selon l’état atteint.", "https://ocw.mit.edu/courses/6-045j-automata-computability-and-complexity-spring-2011/", listOf(
                            mc("cs_auto_1", "Que faut-il pour décrire un automate fini déterministe ?", listOf("Un ensemble d’états, un alphabet, des transitions, un état initial et des états acceptants", "Seulement une base de données", "Un compilateur et un système d’exploitation", "Un graphe sans état initial"), 0, "Ces éléments déterminent l’évolution de l’automate et la condition d’acceptation.", "Liste ce qui est nécessaire pour décider le prochain état puis conclure."),
                            mc("cs_auto_2", "Un compilateur transforme généralement…", listOf("un programme source en une représentation cible", "une clé primaire en clé étrangère", "un réseau local en graphe non orienté", "un bit en octet"), 0, "Le compilateur analyse puis traduit le programme vers une représentation cible.", "Pense au rôle de traduction entre langages.")), xp = 25, prerequisites = listOf("cs_algorithms_bfs")))
            )
        ),
        Subject(
            id = "lang_tcf_canada",
            title = "Français & anglais",
            icon = "🇨🇦",
            description = "Grammaire et communication professionnelle.",
            category = SubjectCategory.LANGUAGES,
            units = listOf(
                unit("tcf_u1", 1, "Grammaire française", "Subjonctif, concordance et formulations utiles.", OSSU_CS,
                    Lesson(
                        id = "tcf_l1", title = "Le subjonctif présent", xpReward = 15, isLocked = false,
                        objective = "Reconnaître le subjonctif après une expression de nécessité.",
                        learningNotes = "Après « il est indispensable que », on emploie le subjonctif : que vous lisiez.",
                        sourceUrls = listOf("https://www.lepointdufle.net/p/subjonctif.htm"),
                        exercises = listOf(
                            mc("t1", "Complétez : « Il est indispensable que vous ___ ce document. »", listOf("lisiez", "lisez", "lirez", "avez lu"), 0, "Après « il est indispensable que », on emploie le subjonctif présent : vous lisiez.", "Repère l’expression qui déclenche le subjonctif."),
                            Exercise.Translation("t2", "Reconstituez la phrase correcte :", "If you work hard, you will succeed in TCF.", listOf("réussirez", "Si vous", "travaillez", "au TCF.", "dur, vous"), listOf("Si vous", "travaillez", "dur, vous", "réussirez", "au TCF."), "En français, si + présent se combine ici avec le futur dans la proposition principale.", "Place d’abord la proposition introduite par « si », puis la conséquence au futur.")
                        )
                    ))
            )
        )
    )

    /** Additional lessons are kept separate from the first lesson in each unit for easy growth. */
    private val supplementalLessons = mapOf(
        "math_reasoning" to listOf(lesson("math_reasoning_induction", "Preuve par récurrence", "Prouver une propriété pour tous les entiers naturels à partir d’un cas initial.",
            "Une récurrence comporte une initialisation, une hypothèse à l’étape n, puis une preuve que cette hypothèse implique l’étape n+1.", MIT_MATH_CS, listOf(
                mc("math_induction_1", "Quelle étape permet de passer du rang n au rang n+1 ?", listOf("L’hérédité", "L’initialisation", "La conclusion seule", "Un contre-exemple"), 0, "L’hérédité établit l’implication entre deux rangs consécutifs.", "L’initialisation vérifie le départ; quelle étape relie deux rangs ?"),
                math("math_induction_2", "Dans une récurrence, que suppose-t-on temporairement à l’étape héréditaire ?", null, listOf("La propriété au rang n", "La propriété pour tous les réels", "La conclusion est fausse", "Le cas initial est inutile"), 0, "On suppose P(n) vraie pour démontrer P(n+1).", "L’hypothèse de récurrence porte sur un rang donné.")), xp = 15))),
        "math_calculus" to listOf(lesson("math_calculus_chain_rule", "Règle de la chaîne", "Dériver une fonction composée.",
            "Pour f(g(x)), la règle de la chaîne donne f′(g(x))×g′(x). On dérive la fonction extérieure puis l’intérieur.", MIT_CALCULUS, listOf(
                math("math_chain_1", "Si h(x)=(2x+1)³, quelle est h′(x) ?", "(f∘g)′=(f′∘g)·g′", listOf("3(2x+1)²", "6(2x+1)²", "2(2x+1)³", "6x²+1"), 1, "La dérivée de u³ est 3u² et u′=2 : h′=6(2x+1)².", "Dérive la puissance, puis multiplie par la dérivée de 2x+1."),
                mc("math_chain_2", "Dans la règle de la chaîne, que faut-il multiplier après avoir dérivé la fonction extérieure ?", listOf("La dérivée de la fonction intérieure", "La fonction intérieure au carré", "La constante d’intégration", "La limite à l’infini"), 0, "La dérivée de l’intérieur est le facteur supplémentaire de la composition.", "La variation de l’intérieur doit aussi être prise en compte.")), xp = 20))),
        "math_calculus_integral" to listOf(lesson("math_integral_fundamental", "Théorème fondamental", "Calculer une intégrale à l’aide d’une primitive.",
            "Si F′=f, alors ∫ₐᵇ f(x)dx=F(b)−F(a). L’intégrale mesure l’accumulation nette entre les bornes.", MIT_CALCULUS, listOf(
                math("math_ftc_1", "Calculer ∫₀¹ 2x dx.", "Une primitive de 2x est x²", listOf("0", "1", "2", "3"), 1, "F(1)−F(0)=1²−0²=1.", "Évalue une primitive à la borne supérieure puis soustrais sa valeur à la borne inférieure."),
                mc("math_ftc_2", "Si F′=f, quelle expression donne ∫ₐᵇ f(x)dx ?", listOf("F(b)−F(a)", "F(a)+F(b)", "f(b)−f(a)", "F′(b)"), 0, "Le théorème fondamental relie l’intégrale aux valeurs d’une primitive aux bornes.", "L’ordre des bornes compte : valeur finale moins valeur initiale.")), xp = 20))),
        "math_discrete" to listOf(lesson("math_discrete_graphs", "Arbres et graphes", "Utiliser une propriété fondamentale des arbres finis.",
            "Un arbre est un graphe connexe sans cycle. Un arbre fini à n sommets possède exactement n−1 arêtes.", MIT_MATH_CS, listOf(
                math("math_tree_1", "Combien d’arêtes possède un arbre à 9 sommets ?", "|E| = |V| − 1", listOf("7", "8", "9", "10"), 1, "Un arbre à 9 sommets possède 9−1=8 arêtes.", "Applique le lien entre le nombre de sommets et d’arêtes d’un arbre."),
                mc("math_tree_2", "Quelle propriété distingue un arbre d’un graphe connexe quelconque ?", listOf("Il ne contient aucun cycle", "Il possède toujours 2 sommets", "Toutes ses arêtes sont orientées", "Chaque sommet a le même degré"), 0, "Un arbre est connexe et acyclique.", "Pense à la condition qui empêche une boucle.")), xp = 20))),
        "math_linear" to listOf(lesson("math_linear_matrix_product", "Dimensions d’un produit matriciel", "Vérifier et calculer la taille d’un produit de matrices.",
            "Le produit AₘₓₙBₙₓₚ est défini lorsque les dimensions intérieures n concordent. Le résultat a la taille m×p.", MIT_LINEAR, listOf(
                mc("math_matrix_1", "A est de taille 2×3 et B de taille 3×4. Quelle est la taille de AB ?", listOf("2×4", "3×3", "2×3", "Le produit est impossible"), 0, "Les dimensions intérieures 3 concordent et le résultat garde les dimensions extérieures 2×4.", "Compare les deux nombres intérieurs, puis conserve le premier et le dernier."),
                mc("math_matrix_2", "Quand le produit AB est-il défini ?", listOf("Quand le nombre de colonnes de A égale le nombre de lignes de B", "Quand A et B ont toujours la même taille", "Quand A est carrée seulement", "Quand les deux matrices ont une ligne"), 0, "Les dimensions intérieures doivent être égales pour former les produits scalaires des lignes et colonnes.", "Vérifie les dimensions qui se rencontrent au milieu.")), xp = 20))),
        "math_probability" to listOf(lesson("math_probability_expectation", "Espérance d’une variable aléatoire", "Calculer une moyenne théorique pondérée par les probabilités.",
            "L’espérance discrète est E[X]=ΣxP(X=x). Elle représente la moyenne à long terme d’un grand nombre de répétitions, pas nécessairement une valeur possible unique.", MIT_PROBABILITY, listOf(
                math("math_expect_1", "Quelle est l’espérance d’un dé équilibré à six faces ?", "E[X]=(1+2+3+4+5+6)/6", listOf("3", "3,5", "4", "6"), 1, "La moyenne des faces vaut 21/6=3,5.", "Additionne les six issues puis divise par six."),
                mc("math_expect_2", "L’espérance d’une variable aléatoire est…", listOf("sa moyenne théorique pondérée", "toujours une issue possible", "sa plus grande valeur", "sa variance"), 0, "L’espérance pondère chaque issue par sa probabilité.", "Elle résume une moyenne à long terme, pas une issue garantie.")), xp = 20))),
        "math_diff_eq" to listOf(lesson("math_diff_eq_initial", "Condition initiale", "Déterminer une solution particulière à partir d’une valeur connue.",
            "Pour y′=ky, les solutions sont y=Ceᵏᵗ. La condition y(0)=y₀ fixe C=y₀.", "https://ocw.mit.edu/courses/18-03sc-differential-equations-fall-2011/", listOf(
                mc("math_initial_1", "Si y′=2y et y(0)=3, quelle solution satisfait la condition ?", listOf("y=3e²ᵗ", "y=2e³ᵗ", "y=3+2t", "y=e²ᵗ+3"), 0, "La forme générale est Ce²ᵗ; y(0)=C=3.", "Substitue t=0 pour déterminer la constante C."),
                mc("math_initial_2", "À quoi sert une condition initiale ?", listOf("Choisir une solution parmi une famille de solutions", "Changer l’équation différentielle", "Calculer une intégrale impropre", "Supprimer la variable indépendante"), 0, "La condition fixe la constante libre dans la solution générale.", "Elle apporte une valeur connue à un instant donné.")), xp = 20))),
        "math_advanced" to listOf(lesson("math_advanced_group", "Éléments d’un groupe", "Reconnaître les axiomes de base d’une structure de groupe.",
            "Un groupe combine une opération associative, un élément neutre et un inverse pour chaque élément. Il n’est pas nécessaire que l’opération soit commutative.", "https://github.com/ossu/math#introduction-to-abstract-algebra", listOf(
                mc("math_group_1", "Laquelle est une exigence pour tout groupe ?", listOf("Associativité de l’opération", "Commutativité obligatoire", "Un ordre total", "Une distance entre éléments"), 0, "L’associativité est requise; la commutativité distingue les groupes abéliens.", "Repère la propriété commune aux groupes abéliens et non abéliens."),
                mc("math_group_2", "Comment appelle-t-on un groupe dont l’opération est commutative ?", listOf("Abélien", "Compact", "Ordonné", "Euclidien"), 0, "Un groupe abélien satisfait a·b=b·a pour tous ses éléments.", "Le nom vient du mathématicien Niels Henrik Abel.")), xp = 20))),
        "cs_intro" to listOf(lesson("cs_intro_recursion", "Récursion et cas de base", "Éviter une récursion infinie en définissant un arrêt.",
            "Une fonction récursive s’appelle sur un problème plus petit. Un cas de base résout directement une entrée minimale et arrête la chaîne d’appels.", OSSU_CS, listOf(
                code("cs_recursion_1", "Quel risque existe si la fonction récursive n’a pas de cas de base ?", "fun f(n) = f(n - 1)", listOf("Les appels continuent jusqu’à une erreur de pile", "Le compilateur trie les appels", "La fonction retourne toujours 0", "Le programme devient parallèle"), 0, "Sans condition d’arrêt, les appels récursifs ne se terminent pas normalement.", "Cherche la condition qui met fin aux appels imbriqués."),
                mc("cs_recursion_2", "Pour calculer n!, quel cas de base est naturel ?", listOf("0! = 1", "0! = 0", "0! = −1", "Le cas de base n’existe pas"), 0, "0!=1 termine la définition récursive de la factorielle.", "Choisis le plus petit argument dont le résultat est défini directement.")), xp = 15))),
        "cs_tools" to listOf(lesson("cs_tools_pipe", "Enchaîner des outils dans le terminal", "Comprendre le rôle d’un tube entre deux commandes.",
            "Un tube transmet la sortie standard d’une commande comme entrée de la suivante. Cela permet de composer de petits outils qui font chacun une tâche claire.", "https://missing.csail.mit.edu/2020/data-wrangling/", listOf(
                mc("cs_pipe_1", "Dans `commande1 | commande2`, que reçoit généralement commande2 ?", listOf("La sortie de commande1", "Le code source de commande1", "Un nouveau terminal", "Le répertoire parent"), 0, "Le tube relie la sortie standard de la première commande à l’entrée standard de la seconde.", "Suis le flux de données de gauche à droite."),
                mc("cs_pipe_2", "Quel est l’intérêt de composer de petits outils ?", listOf("Réutiliser et combiner des opérations ciblées", "Supprimer tous les fichiers", "Éviter les entrées et sorties", "Empêcher les scripts"), 0, "Des outils spécialisés peuvent être assemblés selon le besoin.", "Pense à combiner des étapes simples en pipeline.")), xp = 15))),
        "cs_systems" to listOf(lesson("cs_systems_stack_heap", "Pile et tas mémoire", "Distinguer deux zones de gestion mémoire utilisées par les programmes.",
            "La pile gère notamment les appels de fonctions et variables locales avec une durée de vie structurée. Le tas sert aux allocations dynamiques dont la durée de vie est contrôlée séparément.", "https://pages.cs.wisc.edu/~remzi/OSTEP/", listOf(
                mc("cs_memory_1", "Où se trouvent typiquement les variables locales d’un appel de fonction ?", listOf("Sur la pile", "Dans le DNS", "Dans le cache du navigateur seulement", "Dans le code source"), 0, "Les variables locales et les informations d’appel sont généralement stockées dans une frame de pile.", "Pense à la structure qui suit l’imbrication des appels."),
                mc("cs_memory_2", "Quel est l’intérêt d’une allocation dynamique sur le tas ?", listOf("Contrôler une durée de vie qui peut dépasser un appel de fonction", "Trier les instructions", "Remplacer la mémoire vive", "Convertir un entier en chaîne"), 0, "Les allocations du tas peuvent rester vivantes indépendamment de la pile d’un appel donné.", "Compare la durée de vie d’un objet à celle d’une fonction.")), xp = 20))),
        "cs_algorithms" to listOf(lesson("cs_algorithms_binary_search", "Recherche binaire", "Réduire la zone de recherche de moitié à chaque étape.",
            "La recherche binaire exige une séquence triée. À chaque comparaison, elle élimine la moitié des candidats, d’où un coût logarithmique O(log n).", MIT_ALGORITHMS, listOf(
                mc("cs_binary_1", "Quelle complexité décrit la recherche binaire dans un tableau trié ?", listOf("O(log n)", "O(n²)", "O(1) dans tous les cas", "O(2ⁿ)"), 0, "Le nombre d’étapes croît comme le logarithme du nombre d’éléments.", "Chaque étape élimine environ la moitié des éléments restants."),
                mc("cs_binary_2", "Quelle condition est nécessaire pour la recherche binaire classique ?", listOf("Les éléments sont ordonnés", "Le tableau contient uniquement des chaînes", "La longueur est paire", "Le tableau est vide"), 0, "L’ordre permet de savoir quelle moitié éliminer après chaque comparaison.", "Sans ordre, une comparaison ne permet pas de rejeter toute une moitié.")), xp = 25))),
        "cs_security" to listOf(lesson("cs_security_least_privilege", "Principe du moindre privilège", "Limiter les autorisations à ce qui est nécessaire.",
            "Le moindre privilège donne à chaque compte, processus ou service uniquement les permissions indispensables, pour la durée nécessaire.", "https://owasp.org/www-project-top-ten/", listOf(
                mc("cs_privilege_1", "Pourquoi un service web ne devrait-il pas tourner avec les droits administrateur ?", listOf("Une compromission aurait moins de privilèges à exploiter", "Cela rend le service plus rapide", "Cela chiffre automatiquement le disque", "Cela remplace les mises à jour"), 0, "Limiter les droits réduit les conséquences d’une vulnérabilité dans le service.", "Pense à l’impact si le processus est compromis."),
                mc("cs_privilege_2", "Quelle pratique suit le mieux le moindre privilège ?", listOf("Donner uniquement les permissions nécessaires à la tâche", "Partager un compte administrateur", "Accorder toutes les permissions par défaut", "Ne jamais retirer les accès"), 0, "Les autorisations sont réduites au périmètre et à la durée nécessaires.", "Compare les permissions accordées aux besoins réels.")), xp = 20))),
        "cs_apps" to listOf(lesson("cs_apps_sql_select", "Lire une requête SQL", "Comprendre la sélection et le filtrage de lignes.",
            "SELECT choisit les colonnes, FROM nomme la table et WHERE filtre les lignes qui satisfont une condition.", "https://www.postgresql.org/docs/current/tutorial.html", listOf(
                mc("cs_sql_1", "Que renvoie `SELECT nom FROM eleves WHERE actif = true` ?", listOf("Les noms des élèves actifs", "Toutes les colonnes de tous les élèves", "Les tables supprimées", "Le nombre de bases"), 0, "La projection sélectionne nom et la condition conserve les lignes actives.", "Sépare les colonnes demandées du filtre appliqué aux lignes."),
                mc("cs_sql_2", "Quel mot-clé filtre les lignes d’une requête SQL ?", listOf("WHERE", "ORDER", "TABLE", "INDEX BY"), 0, "WHERE applique une condition aux lignes retournées.", "Cherche la clause qui exprime une condition ligne par ligne.")), xp = 20))),
        "cs_ethics" to listOf(lesson("cs_ethics_license", "Respecter une licence logicielle", "Vérifier les conditions avant de réutiliser une ressource.",
            "Une licence précise les conditions de copie, modification et redistribution. Le fait qu’un fichier soit accessible en ligne ne signifie pas qu’il est libre de droits.", "https://choosealicense.com/", listOf(
                mc("cs_license_1", "Avant de redistribuer un code trouvé en ligne, que faut-il vérifier ?", listOf("La licence et ses conditions", "La couleur de l’éditeur", "Le nombre de lignes seulement", "La date du dernier commit uniquement"), 0, "La licence définit les droits et obligations associés à la réutilisation.", "L’accessibilité publique et l’autorisation de réutiliser sont deux choses différentes."),
                mc("cs_license_2", "Quel principe général est le plus sûr pour les ressources de cours ?", listOf("Citer la source et respecter sa licence", "Copier le contenu complet sans vérification", "Retirer l’attribution", "Supposer que tout est dans le domaine public"), 0, "Les conditions de licence et d’attribution doivent être respectées.", "Pense à l’origine du contenu et aux règles de partage.")), xp = 20))),
        "cs_advanced" to listOf(lesson("cs_advanced_regular_languages", "Langages réguliers", "Associer les langages réguliers aux automates finis.",
            "Un langage régulier peut être reconnu par un automate fini et décrit par une expression régulière. L’automate n’a pas besoin de mémoire non bornée.", "https://ocw.mit.edu/courses/6-045j-automata-computability-and-complexity-spring-2011/", listOf(
                mc("cs_regular_1", "Quel modèle reconnaît exactement les langages réguliers ?", listOf("Les automates finis", "Les bases relationnelles uniquement", "Les systèmes d’équations différentielles", "Les processeurs graphiques"), 0, "Les langages réguliers sont reconnus par les automates finis.", "Cherche le modèle avec un nombre fini d’états."),
                mc("cs_regular_2", "Quel besoin un automate fini n’a-t-il pas ?", listOf("Une mémoire non bornée", "Un état courant", "Un alphabet", "Des transitions"), 0, "Son état courant suffit; il n’a pas de pile ou mémoire non bornée.", "Compare un nombre fixe d’états à un stockage de taille arbitraire.")), xp = 25, prerequisites = listOf("cs_algorithms_bfs")))
    )

    /** New lessons are appended to stable units; the lesson path renders them without UI changes. */
    val sampleSubjects = baseSubjects.map { subject ->
        subject.copy(units = subject.units.map { unit ->
            unit.copy(lessons = unit.lessons + supplementalLessons[unit.id].orEmpty())
        })
    }
}
