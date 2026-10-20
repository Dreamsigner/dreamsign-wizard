"""Check forall x: Calgary source coverage and bounded semantic examples.

Finite-model checks below validate selected authored answers and concepts; they
are not proofs of general FOL claims or a substitute for pedagogical review.
"""
from itertools import product
import json
from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[1]
COURSE_PATH = ROOT / "curriculum/courses/forallx_calgary_2025.json"
MANIFEST_PATH = ROOT / "docs/curriculum/forallx-calgary-source-sessions.json"


def exists(domain, predicate):
    return any(predicate(item) for item in domain)


def forall(domain, predicate):
    return all(predicate(item) for item in domain)


def verify_finite_semantics():
    # TFL characteristic tables for conditional and biconditional.
    rows = list(product([False, True], repeat=2))
    assert [(a, b, (not a) or b) for a, b in rows] == [
        (False, False, True), (False, True, True),
        (True, False, False), (True, True, True),
    ]
    assert [(a, b, a == b) for a, b in rows] == [
        (False, False, True), (False, True, False),
        (True, False, False), (True, True, True),
    ]

    # FOL quantifiers over a non-empty finite domain and an empty predicate.
    domain = ("a", "b")
    assert domain  # Calgary FOL uses non-empty domains
    empty_extension = set()
    assert forall(domain, lambda x: not (x in empty_extension) or x in set())  # ∀x(F→G), both predicates empty
    assert not exists(domain, lambda x: x in empty_extension)
    extension = {"a"}
    assert exists(domain, lambda x: x in extension)
    assert not forall(domain, lambda x: x in extension)

    # Identity and exact cardinality in a finite interpretation.
    def exactly_one_f(domain, f_extension):
        return exists(domain, lambda x: x in f_extension and
                      forall(domain, lambda y: y not in f_extension or y == x))

    assert not exactly_one_f(domain, set())
    assert exactly_one_f(domain, {"a"})
    assert not exactly_one_f(domain, {"a", "b"})
    assert exists(domain, lambda x: x == "a")  # identity with a denoting domain object
    assert not exists(domain, lambda x: x != x)

    # Multiple generality: each object has its own witness, but no shared one.
    pairs = {("a", "a"), ("b", "b")}
    assert forall(domain, lambda x: exists(domain, lambda y: (x, y) in pairs))
    assert not exists(domain, lambda y: forall(domain, lambda x: (x, y) in pairs))

    # Vacuous quantification: when x is not free in G(a), changing domain objects
    # cannot alter G(a); the quantifier contributes its non-empty-domain truth condition.
    g_of_a = True
    assert all(g_of_a for _x in domain) == g_of_a

    # Identity/cardinality: extensions are sets of denotations, not name tokens.
    exactly_two = {"a", "b"}
    assert len(exactly_two) == 2
    assert len({"a"}) <= 1
    assert "a" == "a" and "a" != "b"

    # Relation properties on small structures.
    d3 = (0, 1, 2)
    less_equal = {(x, y) for x in d3 for y in d3 if x <= y}
    assert all((x, x) in less_equal for x in d3)  # reflexive
    assert all((x, z) in less_equal for x, y, z in product(d3, repeat=3)
               if (x, y) in less_equal and (y, z) in less_equal)  # transitive
    assert all(x == y for x, y in product(d3, repeat=2)
               if (x, y) in less_equal and (y, x) in less_equal)  # antisymmetric
    one_way = {(0, 1)}
    assert (0, 1) in one_way and (1, 0) not in one_way  # a relation need not be symmetric

    # Existential/identity and Russell description conditions on finite extensions.
    one_f = {"a"}
    two_f = {"a", "b"}
    no_f = set()
    assert len(one_f) == 1 and len(two_f) == 2 and len(no_f) == 0
    assert (len(one_f) >= 1 and len(one_f) <= 1)
    assert not (len(no_f) >= 1)  # empty definite description fails existence

    # Quantifier negation dualities checked on every subset of a finite domain.
    for mask in range(1 << len(domain)):
        ext = {domain[i] for i in range(len(domain)) if mask & (1 << i)}
        assert (not exists(domain, lambda x: x in ext)) == forall(domain, lambda x: x not in ext)
        assert (not forall(domain, lambda x: x in ext)) == exists(domain, lambda x: x not in ext)

    # A two-world Kripke frame for modal truth conditions.
    worlds = ("w", "u")
    accessible = {("w", "w"), ("w", "u")}
    atom_true = {"w"}
    box_at_w = forall((v for v in worlds if ("w", v) in accessible), lambda v: v in atom_true)
    diamond_at_w = exists((v for v in worlds if ("w", v) in accessible), lambda v: v in atom_true)
    assert not box_at_w and diamond_at_w


def main():
    course = json.loads(COURSE_PATH.read_text(encoding="utf-8"))
    manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    assert manifest["edition"] == course["edition"]
    sessions = manifest["sessions"]
    assert manifest["sessionCount"] == len(sessions) == 48
    assert course["expectedLessonCount"] == manifest["sessionCount"]
    lessons = course["lessons"]
    assert course["authoringStatus"] == "reviewed_for_catalogue"
    assert course["authoredLessonCount"] == len(lessons) == 48
    assert [lesson["sourceSessionNumber"] for lesson in lessons] == list(range(1, 49))

    lesson_ids, exercise_ids = set(), set()
    for lesson, source in zip(lessons, sessions):
        assert lesson["sourceSessionNumber"] == source["number"]
        assert lesson["sourceTitle"] == source["sourceTitle"]
        assert lesson["kind"] == source["kind"] == "lesson"
        assert source["sourceUrl"] in lesson["sourceUrls"]
        assert len(source["chapterSha256"]) == 64 and re.fullmatch(r"[0-9a-f]{64}", source["chapterSha256"])
        assert len(lesson["learningNotes"].split()) >= 40
        assert lesson["id"] not in lesson_ids
        lesson_ids.add(lesson["id"])
        assert len(lesson["exercises"]) >= 2
        for index, exercise in enumerate(lesson["exercises"], 1):
            assert exercise["id"] == f"{lesson['id']}_e{index:02d}"
            assert exercise["id"] not in exercise_ids
            exercise_ids.add(exercise["id"])
            assert exercise["type"] == "choice"
            assert exercise["prompt"] and exercise["hint"]
            assert len(exercise["explanation"].split()) >= 4
            assert len(exercise["options"]) >= 2
            assert 0 <= exercise["correctOptionIndex"] < len(exercise["options"])

    # Explicit rule conditions and scope distinctions must be taught in the
    # corresponding material, rather than merely appearing in chapter titles.
    notes_by_number = {lesson["sourceSessionNumber"]: lesson["learningNotes"] for lesson in lessons}
    for chapter, required_terms in {
        25: ("propre y", "ordre des places"),
        26: ("identité", "deux objets F distincts"),
        27: ("occurrences libres", "lie les occurrences"),
        28: ("existence", "unicité"),
        29: ("portée", "ambiguë"),
        31: ("domaine", "extension"),
        33: ("interprétation", "extension"),
        35: ("réflexive", "transitive", "antisymétrique"),
        36: ("domaines non vides", "nom témoin neuf", "nom arbitraire"),
        37: ("nom arbitraire", "témoin nouveau"),
        38: ("¬∃", "∀x¬"),
        39: ("identité", "réflexivité"),
    }.items():
        text = notes_by_number[chapter].lower()
        for term in required_terms:
            assert term.lower() in text, (chapter, term)

    exercises = {e["id"]: e for lesson in lessons for e in lesson["exercises"]}
    expected_options = {
        "forallx_calgary_2025_s001_e01": "Le site est inaccessible",
        "forallx_calgary_2025_s002_e01": "Un cas avec prémisses vraies et conclusion fausse",
        "forallx_calgary_2025_s003_e01": "Elles sont conjointement impossibles",
        "forallx_calgary_2025_s004_e01": "A, A→C ∴ C",
        "forallx_calgary_2025_s005_e01": "¬A→B",
        "forallx_calgary_2025_s005_e02": "Si au moins une des deux est vraie, y compris lorsque les deux le sont",
        "forallx_calgary_2025_s006_e01": "(A∧B)",
        "forallx_calgary_2025_s007_e01": "Structurelle",
        "forallx_calgary_2025_s008_e01": "Le nom écrit “Aristote”",
        "forallx_calgary_2025_s009_e01": "Vraie",
        "forallx_calgary_2025_s009_e02": "Fausse",
        "forallx_calgary_2025_s010_e01": "Sa vérité ne dépend pas seulement de la valeur de l’énoncé enchâssé",
        "forallx_calgary_2025_s011_e01": "8",
        "forallx_calgary_2025_s012_e01": "Tautologie",
        "forallx_calgary_2025_s013_e01": "Les deux lettres sont indépendantes dans la valuation",
        "forallx_calgary_2025_s014_e01": "Une ligne où toutes les prémisses sont vraies et la conclusion fausse",
        "forallx_calgary_2025_s015_e01": "Une valuation avec P vraie et Q fausse",
        "forallx_calgary_2025_s016_e01": "Elle rend visibles les règles de raisonnement utilisées",
        "forallx_calgary_2025_s017_e01": "∧I",
        "forallx_calgary_2025_s018_e01": "Ouvrir une sous-preuve avec A et y viser B",
        "forallx_calgary_2025_s019_e01": "¬A",
        "forallx_calgary_2025_s020_e01": "Il existe une dérivation de C à partir des hypothèses de Γ",
        "forallx_calgary_2025_s021_e01": "Chaque application de R peut être remplacée par une dérivation avec les règles de base",
        "forallx_calgary_2025_s022_e01": "Toute dérivation préserve la conséquence sémantique",
        "forallx_calgary_2025_s023_e01": "Il existe au moins un objet du domaine qui est un chien",
        "forallx_calgary_2025_s024_e03": "Vraie par vacuité",
        "forallx_calgary_2025_s025_e01": "Chaque personne aime au moins une personne, qui peut varier",
        "forallx_calgary_2025_s026_e01": "∃x∃y(F(x)∧F(y)∧x≠y)",
        "forallx_calgary_2025_s027_e01": "∀xP(x)",
        "forallx_calgary_2025_s028_e01": "Qu’il existe un unique F et que cet objet soit G",
        "forallx_calgary_2025_s029_e02": "∃x(F(x)∧¬G(x))",
        "forallx_calgary_2025_s030_e01": "Rien par leur seule extension : ils sont vrais des mêmes objets",
        "forallx_calgary_2025_s031_e01": "Vraie",
        "forallx_calgary_2025_s032_e01": "Une interprétation où Γ est entièrement vraie et C fausse",
        "forallx_calgary_2025_s033_e01": "Fausse, vraie",
        "forallx_calgary_2025_s034_e01": "Une validité doit être vraie dans toutes les interprétations",
        "forallx_calgary_2025_s035_e01": "Transitivité",
        "forallx_calgary_2025_s036_e03": "Elle est refusée : les domaines doivent contenir au moins un objet",
        "forallx_calgary_2025_s037_e01": "Un nom arbitraire qui n’apparaît dans aucune hypothèse non déchargée",
        "forallx_calgary_2025_s038_e01": "¬∃xA(x) ≡ ∀x¬A(x)",
        "forallx_calgary_2025_s039_e01": "a=a",
        "forallx_calgary_2025_s040_e01": "A(c) et ¬A(c)",
        "forallx_calgary_2025_s041_e01": "Qu’une dérivation formelle existe",
        "forallx_calgary_2025_s042_e01": "A est possible",
        "forallx_calgary_2025_s043_e02": "¬□¬A",
        "forallx_calgary_2025_s044_e01": "Fausse",
        "forallx_calgary_2025_s045_e01": "Disjonction de conjonctions de littéraux",
        "forallx_calgary_2025_s046_e02": "A↑A",
        "forallx_calgary_2025_s047_e02": "¬R∧¬(S∧T)",
        "forallx_calgary_2025_s048_e01": "Si Γ⊢C, alors Γ⊨C",
    }
    for exercise_id, expected in expected_options.items():
        exercise = exercises[exercise_id]
        assert exercise["options"][exercise["correctOptionIndex"]] == expected

    verify_finite_semantics()
    exercise_count = sum(len(lesson["exercises"]) for lesson in lessons)
    print(f"PASS: {len(lessons)}/48 chapter mappings, {exercise_count} playable exercises, selected answer keys and bounded finite-structure checks")


if __name__ == "__main__":
    main()
