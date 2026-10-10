package com.duolingo.app.data

import com.duolingo.app.model.*

object SampleData {
    val sampleSubjects = listOf(
        // 1. MATHEMATICS: Vectors, Matrices, Probability, Statistics
        Subject(
            id = "math_vectors",
            title = "Mathématiques (Vecteurs & Probabilités)",
            icon = "📐",
            description = "Algèbre linéaire, produit scalaire, matrices, théoreme de Bayes et statistiques.",
            category = SubjectCategory.MATHEMATICS,
            units = listOf(
                LearningUnit(
                    id = "math_u1",
                    unitNumber = 1,
                    title = "Vecteurs & Produit Scalaire",
                    description = "Maîtrisez les calculs vectoriels et la géométrie de l'espace.",
                    lessons = listOf(
                        Lesson(
                            id = "math_l1",
                            title = "Norme et Produit Scalaire",
                            xpReward = 15,
                            isCompleted = true,
                            isLocked = false,
                            exercises = listOf(
                                Exercise.MathChallenge(
                                    id = "m1",
                                    prompt = "Soit u = (3, 4). Quelle est la norme ||u|| du vecteur ?",
                                    formula = "||u|| = \\sqrt{x^2 + y^2}",
                                    options = listOf("5", "7", "25", "12"),
                                    correctOptionIndex = 0,
                                    explanation = "||u|| = √(3² + 4²) = √(9 + 16) = √25 = 5."
                                ),
                                Exercise.MathChallenge(
                                    id = "m2",
                                    prompt = "Calculer le produit scalaire u · v avec u = (1, 2) et v = (4, -1).",
                                    formula = "u \\cdot v = x_1 x_2 + y_1 y_2",
                                    options = listOf("2", "6", "-2", "4"),
                                    correctOptionIndex = 0,
                                    explanation = "u · v = (1 * 4) + (2 * -1) = 4 - 2 = 2."
                                )
                            )
                        ),
                        Lesson(
                            id = "math_l2",
                            title = "Multiplication de Matrices",
                            xpReward = 20,
                            isCompleted = false,
                            isLocked = false,
                            exercises = listOf(
                                Exercise.MathChallenge(
                                    id = "m3",
                                    prompt = "Si A est une matrice (2x3) et B est une matrice (3x2), quelle est la taille de A x B ?",
                                    formula = "(m \\times n) \\times (n \\times p) = (m \\times p)",
                                    options = listOf("2x2", "3x3", "2x3", "Impossible"),
                                    correctOptionIndex = 0,
                                    explanation = "Le résultat a le nombre de lignes de A (2) et le nombre de colonnes de B (2), donc 2x2."
                                )
                            )
                        )
                    )
                ),
                LearningUnit(
                    id = "math_u2",
                    unitNumber = 2,
                    title = "Probabilités & Théorème de Bayes",
                    description = "Statistiques conditionnelles et variables aléatoires.",
                    lessons = listOf(
                        Lesson(
                            id = "math_l3",
                            title = "Probabilités Conditionnelles",
                            xpReward = 20,
                            isCompleted = false,
                            isLocked = true,
                            exercises = listOf(
                                Exercise.MathChallenge(
                                    id = "m4",
                                    prompt = "Que calcule la formule P(A|B) ?",
                                    formula = "P(A|B) = \\frac{P(A \\cap B)}{P(B)}",
                                    options = listOf(
                                        "La probabilité de A sachant que B est réalisé",
                                        "La probabilité de B sachant que A est réalisé",
                                        "La somme de P(A) et P(B)",
                                        "L'indépendance de A et B"
                                    ),
                                    correctOptionIndex = 0,
                                    explanation = "P(A|B) représente la probabilité conditionnelle de l'événement A étant donné B."
                                )
                            )
                        )
                    )
                )
            )
        ),

        // 2. COMPUTER SCIENCE: Algorithms, Data Structures, C++
        Subject(
            id = "cs_algo_cpp",
            title = "C++ & Algorithmique Avancée",
            icon = "💻",
            description = "Structures de données, algorithmes, pointeurs, gestion mémoire et RAII en C++.",
            category = SubjectCategory.COMPUTER_SCIENCE,
            units = listOf(
                LearningUnit(
                    id = "cs_u1",
                    unitNumber = 1,
                    title = "Fondamentaux C++ & Pointeurs",
                    description = "Gestion manuelle de la mémoire et références.",
                    lessons = listOf(
                        Lesson(
                            id = "cs_l1",
                            title = "Pointeurs vs Références en C++",
                            xpReward = 15,
                            isCompleted = true,
                            isLocked = false,
                            exercises = listOf(
                                Exercise.CodeChallenge(
                                    id = "c1",
                                    prompt = "Que fait l'opérateur '*' dans ce contexte ?",
                                    codeSnippet = "int x = 42;\nint* ptr = &x;\nstd::cout << *ptr;",
                                    options = listOf(
                                        "Il déréférence le pointeur pour afficher la valeur (42)",
                                        "Il affiche l'adresse mémoire de x",
                                        "Il multiplie ptr par 2",
                                        "Il provoque une erreur de compilation"
                                    ),
                                    correctOptionIndex = 0,
                                    explanation = "'*ptr' accède à la valeur pointée à l'adresse de x, donc affiche 42."
                                )
                            )
                        ),
                        Lesson(
                            id = "cs_l2",
                            title = "Smart Pointers & RAII",
                            xpReward = 20,
                            isCompleted = false,
                            isLocked = false,
                            exercises = listOf(
                                Exercise.CodeChallenge(
                                    id = "c2",
                                    prompt = "Quel pointeur intelligent garantit une propriété unique (unique ownership) ?",
                                    codeSnippet = "auto p = std::make_unique<int>(100);",
                                    options = listOf(
                                        "std::unique_ptr",
                                        "std::shared_ptr",
                                        "std::weak_ptr",
                                        "void*"
                                    ),
                                    correctOptionIndex = 0,
                                    explanation = "std::unique_ptr n'autorise pas la copie de sa ressource, garantissant un propriétaire unique."
                                )
                            )
                        )
                    )
                ),
                LearningUnit(
                    id = "cs_u2",
                    unitNumber = 2,
                    title = "Structures de Données (Arbres & Graphes)",
                    description = "Complexité temporelle O(n) et parcours de graphes.",
                    lessons = listOf(
                        Lesson(
                            id = "cs_l3",
                            title = "Parcours BFS vs DFS",
                            xpReward = 25,
                            isCompleted = false,
                            isLocked = true,
                            exercises = listOf(
                                Exercise.MultipleChoice(
                                    id = "c3",
                                    prompt = "Quelle structure de données utilise un parcours en largeur (BFS) ?",
                                    options = listOf("Une File (Queue / FIFO)", "Une Pile (Stack / LIFO)", "Un Tableau statique", "Un Arbre Rouge-Noir"),
                                    correctOptionIndex = 0,
                                    explanation = "Le parcours BFS utilise une Queue (FIFO) pour traiter les nœuds niveau par niveau."
                                )
                            )
                        )
                    )
                )
            )
        ),

        // 3. LANGUAGES: French TCF Canada & English
        Subject(
            id = "lang_tcf_canada",
            title = "Français TCF Canada & Anglais",
            icon = "🇨🇦",
            description = "Préparation intensive au TCF Canada (Compréhension orale & écrite, grammaire) et Anglais technique.",
            category = SubjectCategory.LANGUAGES,
            units = listOf(
                LearningUnit(
                    id = "tcf_u1",
                    unitNumber = 1,
                    title = "TCF Canada - Compréhension Écrite & Grammaire",
                    description = "Subjonctif, concordance des temps et structures formelles.",
                    lessons = listOf(
                        Lesson(
                            id = "tcf_l1",
                            title = "Le Subjonctif Présent (TCF Niveau B2/C1)",
                            xpReward = 15,
                            isCompleted = false,
                            isLocked = false,
                            exercises = listOf(
                                Exercise.MultipleChoice(
                                    id = "t1",
                                    prompt = "Complétez la phrase : 'Il est indispensable que vous ___ ce document avant la réunion.'",
                                    options = listOf("lisiez", "lisez", "lirez", "avez lu"),
                                    correctOptionIndex = 0,
                                    explanation = "Après 'Il est indispensable que', on emploie le subjonctif présent : vous lisiez."
                                ),
                                Exercise.Translation(
                                    id = "t2",
                                    prompt = "Reconstituez la phrase correcte pour exprimer une condition :",
                                    sourceSentence = "If you work hard, you will succeed in TCF.",
                                    targetTokens = listOf("réussirez", "Si vous", "travaillez", "au TCF.", "dur, vous"),
                                    correctSequence = listOf("Si vous", "travaillez", "dur, vous", "réussirez", "au TCF."),
                                    explanation = "En français : Si + présent -> futur simple."
                                )
                            )
                        )
                    )
                )
            )
        )
    )
}
