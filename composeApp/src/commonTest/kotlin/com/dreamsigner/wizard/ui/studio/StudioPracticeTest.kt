package com.dreamsigner.wizard.ui.studio

import kotlin.test.*

class StudioPracticeTest {
    private val fence = 0x60.toChar().toString().repeat(3)

    @Test fun quizIsValidatedAndAnswerKeyIsRemovedFromVisibleText() {
        val body = """{"question":"Combien font 2 + 2 ?","options":[{"id":"a","label":"3"},{"id":"b","label":"4"}],"multiSelect":false,"correctIds":["b"],"explanation":"2 + 2 = 4."}"""
        val result = parseStudioTutorReply("La réponse est 4.\n" + fence + "studio-quiz\n" + body + "\n" + fence)
        assertEquals("", result.text)
        assertEquals("Combien font 2 + 2 ?", result.quiz?.question)
        assertEquals(listOf("b"), result.quiz?.correctIds)
        assertEquals("2 + 2 = 4.", result.quiz?.explanation)
    }

    @Test fun malformedAndDuplicateQuestionsNeverExposeStructuredKey() {
        val invalid = """{"question":"Question","options":[{"id":"a","label":"Même"},{"id":"a","label":"Autre"}],"correctIds":["a"],"explanation":"SECRET"}"""
        val result = parseStudioTutorReply("SECRET en préambule.\n" + fence + "studio-quiz\n" + invalid + "\n" + fence + "\nSECRET en conclusion")
        assertNull(result.quiz)
        assertFalse("SECRET" in result.text)
        assertTrue("invalide" in result.text)
        val unclosed = parseStudioTutorReply("Préambule\n" + fence + "studio-quiz\n" + invalid)
        assertFalse("SECRET" in unclosed.text)
        val repeated = parseStudioTutorReply(fence + "studio-quiz\n" + invalid + "\n" + fence + "\n" + fence + "studio-quiz\nSECRET2")
        assertFalse("SECRET2" in repeated.text)
        assertEquals(ParsedStudioReply("Texte ordinaire", null), parseStudioTutorReply("Texte ordinaire"))
    }

    @Test fun onlyKnownDistinctCorrectAnswersAreAccepted() {
        val base = StudioQuiz("Question", listOf(StudioQuizOption("a", "Oui"), StudioQuizOption("b", "Non")), false, listOf("a"), "Explication")
        assertTrue(validStudioQuiz(base))
        assertFalse(validStudioQuiz(base.copy(correctIds = listOf("missing"))))
        assertFalse(validStudioQuiz(base.copy(correctIds = listOf("a", "b"))))
        assertTrue(validStudioQuiz(base.copy(multiSelect = true, correctIds = listOf("a", "b"))))
        assertFalse(validStudioQuiz(base.copy(options = listOf(StudioQuizOption("a", "Oui"), StudioQuizOption("b", " OUI ")))))
    }
}
