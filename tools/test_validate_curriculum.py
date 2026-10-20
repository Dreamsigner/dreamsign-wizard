import copy
import unittest
from validate_curriculum import validate_course, kotlin_string


class CurriculumValidationTest(unittest.TestCase):
    def setUp(self):
        self.manifest = {"sessionCount": 3, "sourceUrl": "https://ocw.mit.edu/test/", "sessions": [
            {"number": n, "sourceTitle": f"Session {n}", "sourceUrl": f"https://ocw.mit.edu/test/{n}", "kind": "lesson"}
            for n in range(1, 4)]}
        self.course = {"courseId": "test_course", "title": "Cours de test", "sourceTitle": "Test", "edition": "Test only",
                       "category": "MATHEMATICS", "sourceUrl": self.manifest["sourceUrl"], "lessons": []}
        for n in range(1, 4):
            self.course["lessons"].append({"id": f"test_s{n}", "sourceSessionNumber": n, "sourceTitle": f"Session {n}",
                "kind": "lesson", "title": f"Leçon de test {n}", "objective": "Calculer un résultat.",
                "learningNotes": (f"Exemple pédagogique numéro {n}. " + "Une somme ajoute les termes sans changer leur valeur. " * 6),
                "sourceUrls": [f"https://ocw.mit.edu/test/{n}"], "exercises": [{"id": f"test_s{n}_e1", "type": "math",
                "prompt": "Combien vaut 2 + 3 ?", "options": ["5", "6", "7", "8"], "correctOptionIndex": 0,
                "explanation": "Deux et trois font cinq.", "hint": "Additionne les deux nombres."}]})

    def test_complete_valid_course(self):
        self.assertEqual([], validate_course(self.course, self.manifest))

    def test_missing_middle_session_rejected(self):
        self.course["lessons"].pop(1)
        self.assertTrue(any("sequence" in error for error in validate_course(self.course, self.manifest)))

    def test_duplicate_exercise_id_rejected(self):
        self.course["lessons"][1]["exercises"][0]["id"] = "test_s1_e1"
        self.assertTrue(any("duplicate" in error for error in validate_course(self.course, self.manifest)))

    def test_false_count_is_not_trusted(self):
        self.course["expectedLessonCount"] = 999
        self.assertTrue(any("count" in error for error in validate_course(self.course, self.manifest)))

    def test_invalid_answer_and_boolean_are_rejected(self):
        for answer in (99, -1, True):
            course = copy.deepcopy(self.course)
            course["lessons"][0]["exercises"][0]["correctOptionIndex"] = answer
            self.assertTrue(any("answer" in error for error in validate_course(course, self.manifest)))

    def test_missing_precise_source_rejected(self):
        self.course["lessons"][0]["sourceUrls"] = [self.manifest["sourceUrl"]]
        self.assertTrue(any("session source" in error for error in validate_course(self.course, self.manifest)))

    def test_unsupported_exercise_is_not_published(self):
        self.course["lessons"][0]["exercises"][0]["type"] = "audio_without_player"
        self.assertTrue(any("unsupported" in error for error in validate_course(self.course, self.manifest)))

    def test_source_edition_course_url_is_checked(self):
        self.course["sourceUrl"] = "https://ocw.mit.edu/different-edition/"
        self.assertTrue(any("course source" in error for error in validate_course(self.course, self.manifest)))

    def test_kotlin_literal_preserves_dollar_and_multiline(self):
        value = kotlin_string('Coût $5\n"x" \\')
        self.assertIn('\\$', value)
        self.assertIn('\\n', value)
        self.assertIn('\\"', value)

    def test_written_practice_requires_a_real_in_range_example_and_rubric(self):
        exercise = self.course["lessons"][0]["exercises"][0]
        exercise.update(type="written", minWords=3, maxWords=5,
                        exampleResponse="Un texte personnel.", reviewCriteria=["J’ai relu."])
        self.assertEqual([], validate_course(self.course, self.manifest))
        exercise["exampleResponse"] = "Trop court"
        self.assertTrue(any("word limits" in error for error in validate_course(self.course, self.manifest)))
        exercise["exampleResponse"] = "Un texte personnel."
        exercise["reviewCriteria"] = []
        self.assertTrue(any("review criteria" in error for error in validate_course(self.course, self.manifest)))


if __name__ == "__main__":
    unittest.main()
