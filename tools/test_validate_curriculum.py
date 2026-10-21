import copy
import unittest
import contextlib
import io
from unittest.mock import patch
from validate_curriculum import validate_course, kotlin_string, exercise_kotlin, main


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

    def test_reading_requires_a_separate_nonblank_passage(self):
        exercise = self.course["lessons"][0]["exercises"][0]
        exercise.update(type="reading", prompt="Quel horaire est annoncé ?", passage="Le rendez-vous est à dix heures.")
        self.assertEqual([], validate_course(self.course, self.manifest))
        for invalid in (None, "", "   ", 123):
            exercise["passage"] = invalid
            self.assertTrue(any("reading passage" in error for error in validate_course(self.course, self.manifest)))
        exercise.pop("passage")
        self.assertTrue(any("reading passage" in error for error in validate_course(self.course, self.manifest)))

    def test_reading_generator_keeps_passage_separate_from_question(self):
        exercise = self.course["lessons"][0]["exercises"][0]
        exercise.update(type="reading", prompt="Quelle est la demande ?", passage="Message original :" + chr(10) + "Réserve une place.")
        result = exercise_kotlin(exercise)
        self.assertTrue(result.startswith("Exercise.ReadingChoice("))
        self.assertIn('prompt = "Quelle est la demande ?"', result)
        self.assertIn("passage = " + kotlin_string(exercise["passage"]), result)

    def test_invalid_draft_prevents_generation_even_when_requested(self):
        errors = ["draft_course: lesson sequence/count differs from source sequence"]
        with patch("validate_curriculum.load_authored", return_value=([(self.course, self.manifest)], errors)), \
                patch("validate_curriculum.generate") as generate, \
                patch("sys.argv", ["validate_curriculum.py", "--generate"]), \
                contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(1, main())
            generate.assert_not_called()

    def test_missing_middle_session_rejected(self):
        self.course["lessons"].pop(1)
        self.assertTrue(any("sequence" in error for error in validate_course(self.course, self.manifest)))

    def test_duplicate_exercise_id_rejected(self):
        self.course["lessons"][1]["exercises"][0]["id"] = "test_s1_e1"
        self.assertTrue(any("duplicate" in error for error in validate_course(self.course, self.manifest)))

    def test_false_count_is_not_trusted(self):
        self.course["expectedLessonCount"] = 999
        self.assertTrue(any("count" in error for error in validate_course(self.course, self.manifest)))

    def test_stable_ids_cannot_be_paths_or_overlong_recording_keys(self):
        for value in ("../escape", "bad-id.wav", "e" * 129):
            course = copy.deepcopy(self.course)
            course["lessons"][0]["exercises"][0]["id"] = value
            self.assertTrue(any("stable exercise ID" in error for error in validate_course(course, self.manifest)))

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

    def test_listening_requires_bundled_audio_and_a_transcript(self):
        exercise = self.course["lessons"][0]["exercises"][0]
        exercise.update(type="listening", audioResourcePath="../external.wav", transcript="Bonjour.")
        self.assertTrue(any("audio path" in error for error in validate_course(self.course, self.manifest)))
        exercise["audioResourcePath"] = "files/audio/nonexistent_audio_fixture.wav"
        self.assertTrue(any("audio is missing" in error for error in validate_course(self.course, self.manifest)))
        exercise["transcript"] = ""
        self.assertTrue(any("transcript" in error for error in validate_course(self.course, self.manifest)))

    def test_spoken_practice_checks_native_limits_and_self_review_contract(self):
        exercise = self.course["lessons"][0]["exercises"][0]
        exercise.update(type="spoken", maxDurationSeconds=120, preparationSeconds=0,
                        minimumPracticeSeconds=5, reviewCriteria=["Réécouter ma réponse."],
                        exampleOutline="Présenter son activité et donner un exemple.", roleplayTurns=[])
        self.assertEqual([], validate_course(self.course, self.manifest))
        for field, value, fragment in (("maxDurationSeconds", 271, "recording duration"),
                                       ("preparationSeconds", True, "preparation duration"),
                                       ("minimumPracticeSeconds", 121, "minimum practice"),
                                       ("reviewCriteria", [], "review criteria")):
            invalid = copy.deepcopy(self.course)
            invalid["lessons"][0]["exercises"][0][field] = value
            self.assertTrue(any(fragment in error for error in validate_course(invalid, self.manifest)))


if __name__ == "__main__":
    unittest.main()
