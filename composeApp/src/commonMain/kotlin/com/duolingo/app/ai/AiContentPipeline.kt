package com.duolingo.app.ai

import com.duolingo.app.model.*
import kotlinx.coroutines.delay

class DemoLessonGenerator {

    suspend fun createDemoLesson(
        sourceUrlOrPath: String,
        sourceType: IngestionSourceType,
        targetSubjectId: String,
        onProgressUpdate: (String) -> Unit
    ): Lesson {
        onProgressUpdate("Démo : préparation de leçon exemple...")
        delay(300)

        onProgressUpdate("Démo : PDF et vidéo non lus.")
        delay(300)

        onProgressUpdate("Démo : exercices basés sur la matière choisie...")
        delay(300)

        val sourceLabel = sourceUrlOrPath.substringAfterLast('/').take(40)
        val sourceKind = if (sourceType == IngestionSourceType.YOUTUBE_VIDEO) "YouTube" else "PDF"
        val extractedTitle = "Leçon de démonstration — $sourceKind: $sourceLabel"

        // Exercices exemples associés à la matière sélectionnée, sans analyse de la ressource.
        val generatedExercises = when (targetSubjectId) {
            "math_vectors" -> listOf(
                Exercise.MathChallenge(
                    id = "gen_m1",
                    prompt = "Exercice de démonstration : Calculer la valeur propre de la matrice A = [[2, 0], [0, 5]].",
                    formula = "det(A - \\lambda I) = 0",
                    options = listOf("λ₁ = 2, λ₂ = 5", "λ₁ = 1, λ₂ = 10", "λ₁ = 0, λ₂ = 7", "λ₁ = -2, λ₂ = -5"),
                    correctOptionIndex = 0,
                    explanation = "Pour une matrice diagonale, les valeurs propres sont directement les éléments de la diagonale principale (2 et 5)."
                ),
                Exercise.MultipleChoice(
                    id = "gen_m2",
                    prompt = "Que signifie un produit scalaire nul entre deux vecteurs ?",
                    options = listOf("Ils sont orthogonaux (perpendiculaires)", "Ils sont colinéaires", "Ils ont la même longueur", "L'un des vecteurs est nul"),
                    correctOptionIndex = 0,
                    explanation = "u · v = 0 <=> cos(θ) = 0 <=> θ = 90° (vecteurs orthogonaux)."
                )
            )

            "cs_algo_cpp" -> listOf(
                Exercise.CodeChallenge(
                    id = "gen_c1",
                    prompt = "Exercice de démonstration C++ : Quelle est la complexité temporelle du Tri Rapide (QuickSort) dans le pire des cas ?",
                    codeSnippet = "void quickSort(int arr[], int low, int high);",
                    options = listOf("O(n²)", "O(n log n)", "O(n)", "O(1)"),
                    correctOptionIndex = 0,
                    explanation = "Dans le pire des cas (pivot mal choisi sur tableau déjà trié), QuickSort s'exécute en O(n²)."
                ),
                Exercise.CodeChallenge(
                    id = "gen_c2",
                    prompt = "Quelle fonctionnalité C++11 aide à éviter les fuites mémoire ?",
                    codeSnippet = "std::unique_ptr<Data> data = std::make_unique<Data>();",
                    options = listOf("RAII & Smart Pointers", "Pointeurs bruts (raw pointers)", "goto & malloc", "Garbage Collector automatique"),
                    correctOptionIndex = 0,
                    explanation = "C++ utilise le motif RAII et les smart pointers pour libérer automatiquement la mémoire à la fin de portée."
                )
            )

            else -> listOf(
                Exercise.MultipleChoice(
                    id = "gen_l1",
                    prompt = "Question TCF de démonstration : Quel connecteur logique exprime l'opposition ?",
                    options = listOf("Néanmoins", "En effet", "C'est pourquoi", "De plus"),
                    correctOptionIndex = 0,
                    explanation = "'Néanmoins' est un adverbe d'opposition (équivalent à 'cependant' ou 'toutefois')."
                ),
                Exercise.Translation(
                    id = "gen_l2",
                    prompt = "Reconstituez cette phrase formelle :",
                    sourceSentence = "We would appreciate your prompt response regarding the application.",
                    targetTokens = listOf("promptement", "répondiez", "votre demande.", "Nous apprécierions", "que vous", "à"),
                    correctSequence = listOf("Nous apprécierions", "que vous", "répondiez", "promptement", "à", "votre demande."),
                    explanation = "Structure soutenue avec verbe au subjonctif après 'apprécier que'."
                )
            )
        }

        onProgressUpdate("Leçon de démonstration créée.")

        return Lesson(
            id = "generated_lesson_" + System.currentTimeMillis(),
            title = extractedTitle,
            xpReward = 25,
            isCompleted = false,
            isLocked = false,
            exercises = generatedExercises
        )
    }
}
