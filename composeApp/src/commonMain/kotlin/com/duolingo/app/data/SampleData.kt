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

    val sampleSubjects = listOf(
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
}
