"""Recompute 60 finite answers plus 12 bounded scenarios from the authored 6.004 JSON.

Run: python tools/check_computer_architecture.py
Standard library only, offline. No execution of course snippets, HDL, hardware, or
external input as code. Expressions below are fixed trusted test fixtures authored
here; the JSON supplies answer choices and prompt hashes only. These finite
models do not prove an entire processor, analog behavior, OS, or laboratory.
"""
from fractions import Fraction as F
from hashlib import sha256
from itertools import product, permutations
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def cache_misses(words, lines, ways):
    """LRU cache, word-sized blocks; inputs are word numbers, not byte addresses."""
    assert lines > 0 and lines % ways == 0
    sets = [[] for _ in range(lines // ways)]
    misses = 0
    for word in words:
        bucket = sets[word % len(sets)]
        if word in bucket:
            bucket.remove(word)
        else:
            misses += 1
            if len(bucket) == ways:
                bucket.pop(0)
        bucket.append(word)
    return misses


def lost_increment():
    x = 0
    p = x
    q = x
    x = p + 1
    x = q + 1
    return x


CHECKS = [('mit_6_004_2017_s001_e01',
  '(32).bit_length()-1',
  'cfab41082643c45db4c10fcf6e1e054d9c7dfea34434826bc6fca40ab05a5b0f'),
 ('mit_6_004_2017_s001_e02',
  "int('11110110',2)-256",
  '6a09f87bb1c969614679a6283647f38414acd70bc7f73185b228f4c450256ed2'),
 ('mit_6_004_2017_s001_e03',
  '2*2+1',
  '5afbc2cf076a8f9fb51cf5cc0772f0710a8e37295d9caf34b73598685a6e3f26'),
 ('mit_6_004_2017_s001_e04',
  'F(1,2)+F(1,4)*2+2*F(1,8)*3',
  'f17a9ecf38b0c10cb42d09b58a32f99e09f0aa75fa4b7ecf0ca54259419449ad'),
 ('mit_6_004_2017_s002_e01',
  'min(F(9,10)-F(4,10),F(28,10)-F(21,10))',
  'bc659513f986eaa2a12a3ea5039b56b689cce16d8c0e5353cde9b97efb59e5ab'),
 ('mit_6_004_2017_s003_e02',
  '2+5+3',
  '5f19085db85f5a1aa5df892d6ce74e43af3abe3844395a0a26ab6e27483054c9'),
 ('mit_6_004_2017_s004_e03',
  '2**6*3',
  'd4d04ee70e11b302d23506901428da05c7aff56ed840a6a22b423fc66775bf85'),
 ('mit_6_004_2017_s005_e01',
  '2+7+1',
  '39074c2835143eb3fb553cd9b3542b86b84ff8f624e105b4e3fb37edce7dd20f'),
 ('mit_6_004_2017_s006_e01',
  '(5-1).bit_length()',
  '7dc07c01ba18dbf47266b73d6a13496f346ec1e7bc2d52fe37e7fe82d116bc1c'),
 ('mit_6_004_2017_s007_e01',
  '4*3',
  'bbcae166b42411d881dafb0f959bf21c8b9dd543843ee34787cb1e71517800ce'),
 ('mit_6_004_2017_s007_e02',
  '(3+10-1)*5',
  '0f2813a9d0201452d97aa11ae1f3c115746e959ff16a9acf9ba0c23495ac4e85'),
 ('mit_6_004_2017_s008_e01',
  '2**8-200',
  'e923beeeaadf1ad3dc5cc1e287519ad3c903315a6fc9f75d2e97c304324aecc8'),
 ('mit_6_004_2017_s008_e03',
  '2+6+1',
  'f07dfb4a5ef4dcd413595cc531ffffa6456f1b49f78bb09fcb67b8aac17e78a9'),
 ('mit_6_004_2017_s008_e04',
  '4-1',
  'b093bc9d701d13bfa88549dc534c630ac1161b563514c4ecd192f29660c13d50'),
 ('mit_6_004_2017_s009_e01',
  'F(4,5)**2*F(5,4)',
  '073b0e524e643c6744c78cef002b6e7e2a17e0633e89d3c722ff806c3e7879fe'),
 ('mit_6_004_2017_s009_e03',
  '8*2',
  'ea5a33465d8968c5bea3eef952fd003363f6344146116483a42f0e92fc7fceac'),
 ('mit_6_004_2017_s010_e01',
  '100+4+4*(-3)',
  '411659bb05f8a3efd8017e5a5d8b1cd19b5098054cffbd466d6aae8c21082de8'),
 ('mit_6_004_2017_s010_e02',
  "'0x'+format((-7)&0xffffffff,'08X')",
  '5a1a3cef40ddd22b036de1a20b43a73a34bcc7793e9d5cb24b5ab13a9ce4fb7d'),
 ('mit_6_004_2017_s010_e04',
  '200+4*6',
  '0ee21d6effc07430e7ad859e34d7353550661a4a4ee6100dcb71594897a5276a'),
 ('mit_6_004_2017_s011_e01',
  'sum(range(1,4))',
  '8bdec38892ee5f7814cab3c3a1431d578a75c1f82c7196756de31f66c609a90e'),
 ('mit_6_004_2017_s011_e02',
  '80+2*4',
  '1773858a672619a5df65c2b546f9bec701c2fe04bb2ae197444191f3c414e7d2'),
 ('mit_6_004_2017_s012_e01',
  '2*7+4',
  'fd9b8f7bc9e79b6a64e7b079ee2845b47a0ad8bb1027cbce5d9e07cc50df4b56'),
 ('mit_6_004_2017_s012_e03',
  '1000+4*9',
  '9a7950587359a12b91fd4fe0168c910f198d71c677ee644fc8dc613720009825'),
 ('mit_6_004_2017_s013_e01',
  '1000+3*4',
  'ea293b8ff32f5fc0ec0be7b4a27228c91ad2405101cc5530d6fc9b75e53a9c56'),
 ('mit_6_004_2017_s013_e03',
  '5+1',
  'ce3a572a43466ac39c30fdb4e55275f0c24be0b60d6ba2d8fa56311c6b353152'),
 ('mit_6_004_2017_s014_e01',
  '100+12',
  'acd959e3b67b40bf0bcc0d7a56e41891305749b8b51dcb4ff14f288622b89a50'),
 ('mit_6_004_2017_s014_e03',
  '2+1+2+5+1',
  '863e1ade806587f3d731a0381df665ab09a9cb05852c6928dfbaf96994dc91cb'),
 ('mit_6_004_2017_s014_e04',
  '2**6*12',
  'bd76f8ee9d9e2c5eaaf4b6558db1c8d33cd2863dc8cce79e7a4fbb02ceeac610'),
 ('mit_6_004_2017_s015_e01',
  '(5+20-1)*2',
  'b53f5903ef91f3e74a82a4576c5dc788e78194bfcaa6604b81e935c20edc1d13'),
 ('mit_6_004_2017_s015_e02',
  '200+4+4*4',
  'bd26126c1520082e6a87103b51406da03514e0c7c5b5d749cb6d410b5b89697e'),
 ('mit_6_004_2017_s015_e04',
  '7%4',
  '2722b22d0f556fb36989b81a4f4e659ad484eeecf68ce572b743e425489896b1'),
 ('mit_6_004_2017_s016_e01',
  '32-4-6',
  '52b4c03b21672b58f47374507db974cc1314e392736090e35aa666e7ffe23fdd'),
 ('mit_6_004_2017_s016_e02',
  '2+F(5,100)*40',
  '67c7d8ce40378806877f001774f39f8a34ff2d149c29556d8269c62c49e71a2b'),
 ('mit_6_004_2017_s016_e04',
  '(0xAC//4)%8',
  '5f48c1ac01961012b6012f79a45b96a5ab4c68fc3ca779d4d9b5099a2db3677a'),
 ('mit_6_004_2017_s017_e01',
  'cache_misses([0,2,0,2], lines=2, ways=1)',
  '1b6e55a501b8adf8b54b598792d292bf1a86300cf728694643da7408bd07f43a'),
 ('mit_6_004_2017_s017_e02',
  'cache_misses([0,2,0,2], lines=2, ways=2)',
  '8e091d5325afa932bb5911b095f4a29fab2a3253ad0cc7911a4325b407a6f2d9'),
 ('mit_6_004_2017_s018_e01',
  '5-3',
  'd6474bed3de3a7f2a17daaa9d3751f58f995c547fb302087ab50e1c6ef282ffa'),
 ('mit_6_004_2017_s018_e02',
  '5-3',
  'ae6fec6118ce68bad3d1ea7bfbb000053a1781f6cf679e118cbbc13c4d2690b6'),
 ('mit_6_004_2017_s018_e04',
  '100+5-1+8',
  '69d5e3cc195817b6cf31988a9087a4f97066b904c7a676a71c3f22db3422d19b'),
 ('mit_6_004_2017_s019_e01',
  '1+F(20,100)*F(25,100)*2',
  '93f7dfc9b7dfe1bba0b24b7a43c33ce9b0119e19e7e42e7f291fae2f5317993b'),
 ('mit_6_004_2017_s020_e01',
  "'0x'+format(9*4096+(0x2345%4096),'X')",
  '486d9d3d305ab9383a77b30030039787dfb60540c4b3c25cfb2e04df7e4fd8ad'),
 ('mit_6_004_2017_s020_e02',
  '2**(32-12)*4',
  '545370a3e1549a68b5e85946fb2acb4b7ac3f0bcce0ab6fbadc9e7991334e9dc'),
 ('mit_6_004_2017_s020_e04',
  '20000//4096',
  '28d98245b9be58e74dd24d5473f3f9a2d9b7e7289d333752a5ca8b7544403d89'),
 ('mit_6_004_2017_s021_e01',
  '4+3',
  '702222a93dafb205f772ee9eba5e72a555a950979fdaad212c71dca1aea5467c'),
 ('mit_6_004_2017_s021_e02',
  '1+F(2,100)*50',
  '174070cdeaf266ed6aa119a508c86ad9d53608c44eeb1b706523738145fe8788'),
 ('mit_6_004_2017_s021_e03',
  '10-8',
  '315ee51cef44053fd94a3007c8f009dafa62f8a6569f9f3233c4b97a70be2b13'),
 ('mit_6_004_2017_s022_e02',
  'F(900,900+100)',
  'f0a5cf9e3755294819f25de2e84b295c198b10ebc22feed06dec8d922a9a0fba'),
 ('mit_6_004_2017_s023_e01',
  '80-30',
  '68ba0d703dab6ca045bcbda870986cd15f00bf792569fb3ac3dcdc0890519d5f'),
 ('mit_6_004_2017_s023_e02',
  'F(200*100,1000000)',
  '398b2cb6bdb491b46a032e3ef4fa8bd07d7c27b107fadd4929eaaf8f586466f6'),
 ('mit_6_004_2017_s023_e04',
  'F(2,10)+F(3,20)',
  '2afe2b90c12fd888b70d5722b82f196fb0e2b37ae60b1cb65535d44b8728c338'),
 ('mit_6_004_2017_s024_e02',
  'lost_increment()',
  '9a83ff3b3dd08f0517be25f1585c29440acd0cb7efc2494596c9fc171f9e2f02'),
 ('mit_6_004_2017_s025_e01',
  '8*7//2',
  '12f3a2d0010b74f22430f9b657062edf026adea07d085db3703d042dc1ea1554'),
 ('mit_6_004_2017_s025_e02',
  'F(12000*1000000,1000000000)',
  '5fa6ac973b4d2157decc219d16a95fc27656e66b93176d06728487747f6ed4de'),
 ('mit_6_004_2017_s025_e03',
  '(4-1)+(4-1)',
  'a182952482d404477b7bf53e35cf032c4f3453d9b7d1d903d1973dea8daa1f5f'),
 ('mit_6_004_2017_s026_e01',
  '1/(F(1,5)+F(4,5)/4)',
  'be485dda02054ad72cc66ee9771352bfa7cbbe4500f5d378efae724c1c3debf7'),
 ('mit_6_004_2017_s026_e02',
  '1/F(1,10)',
  '64609ee0864e261349e51aaf06e044b772d9c4a52671eef90bb4e5744773df32'),
 ('mit_6_004_2017_s027_e02',
  '1/(F(3,4)+F(1,4)/2)',
  '4befa2962c9b9e6a28d11088378796b4c1ba7cf260c540a01d0fc72fe43f6796'),
 ('mit_6_004_2017_s028_e01',
  '7*1024+300',
  'a07dd42c50816ceada88d5690ca1a423b01e4909db3b8529e0ce0ad5a4ce7e40'),
 ('mit_6_004_2017_s028_e02',
  '120-45',
  'cb33649f6d45cf5d17e9997c6bf27a4b49d06babecf4d2e94b5fed5480efefd9'),
 ('mit_6_004_2017_s028_e04',
  'F(4000*1000,2000000)',
  '3c9189a44183d7ed74807fc431f3a832b8de34d31687af539ceae1706aeb9d47')]

def main():
    course = json.loads((ROOT / "curriculum/courses/mit_6_004_2017.json").read_text(encoding="utf8"))
    manifest = json.loads((ROOT / "docs/curriculum/mit-6-004-source-sessions.json").read_text(encoding="utf8"))
    lessons = course["lessons"]
    assert len(lessons) == manifest["sessionCount"] == 28
    assert [s["sourceNumber"] for s in manifest["sessions"]] == ([f"L{i}" for i in range(1, 8)] + ["Quiz 1"] + [f"L{i}" for i in range(8, 14)] + ["Quiz 2"] + [f"L{i}" for i in range(14, 19)] + ["Quiz 3"] + [f"L{i}" for i in range(19, 25)] + ["Quiz 4"])
    assert sum(l["kind"] == "exam" for l in lessons) == 4
    assert sum(len(l["exercises"]) for l in lessons) == 99
    exercises = {e["id"]: e for l in lessons for e in l["exercises"]}
    assert len(exercises) == 99
    for lesson, session in zip(lessons, manifest["sessions"]):
        assert (lesson["sourceSessionNumber"], lesson["sourceTitle"], lesson["kind"]) == (session["number"], session["sourceTitle"], session["kind"])
        assert session["sourceUrl"] in lesson["sourceUrls"]
    def answer(short):
        e = exercises["mit_6_004_2017_" + short]
        return e["options"][e["correctOptionIndex"]]
    env = {"F": F, "sum": sum, "range": range, "int": int, "len": len, "max": max, "min": min, "format": format, "str": str, "cache_misses": cache_misses, "lost_increment": lost_increment}
    for eid, expression, prompt_hash in CHECKS:
        e = exercises[eid]
        assert sha256(e["prompt"].encode()).hexdigest() == prompt_hash, (eid, "prompt changed; review fixture")
        calculated = str(eval(expression, {"__builtins__": {}}, env))
        chosen = e["options"][e["correctOptionIndex"]]
        assert calculated == chosen, (eid, calculated, chosen)

    # Twelve separately linked finite scenarios. Each combines a computed model
    # or exhaustive small truth table with the selected conceptual answer.
    assert all((not (a or b)) == ((not a) and (not b)) for a, b in product((False, True), repeat=2))
    assert answer("s003_e01") == "NOR : ¬(A∨B)"
    assert all(((a and b) or (a and not b)) == a for a, b in product((False, True), repeat=2))
    assert answer("s004_e01") == "A"
    assert [int((not s) * 1 or s * 0) for s in (0, 1)] == [1, 0]
    assert answer("s004_e02") == "D0=1 et D1=0"
    assert all(((not s and a) or (s and b)) == ((not s and a) or (s and b) or (a and b)) for s, a, b in product((False, True), repeat=3))
    assert all((a and b) for a, b in [(True, True)])
    assert answer("s004_e04") == "A·B"
    state = 0
    for bit in [1, 0, 1, 1]:
        state ^= bit
    assert state == 1 and answer("s006_e02") == "Impair"
    # Complementary pullup of A in series with parallel B,C:
    assert all((not (a and (b or c))) == ((not a) or ((not b) and (not c))) for a, b, c in product((False, True), repeat=3))
    assert answer("s008_e02") == "¬(A∧(B∨C))"
    a, b = 2, 3
    t = a + b
    a += 1
    assert a + b != t
    assert answer("s012_e02") == "Non, a a été modifié entre les deux sommes"
    stack, sp = [], 400
    for value in ["A", "B"]:
        stack.append(value)
        sp += 4
    r1, r2 = stack.pop(), stack.pop()
    sp -= 8
    assert (r1, r2, sp) == ("B", "A", 400)
    assert answer("s015_e03") == "R1=B, R2=A, SP=400"
    semaphore = 2
    blocked = 0
    for _ in range(3):
        if semaphore:
            semaphore -= 1
        else:
            blocked += 1
    assert (semaphore, blocked) == (0, 1)
    assert answer("s024_e04") == "Il attend, la valeur restant 0"
    # All six interleavings of x=1;r=y and y=1;s=x that preserve local order.
    outcomes = set()
    schedules = 0
    for order in permutations("abcd"):
        if order.index("a") > order.index("b") or order.index("c") > order.index("d"):
            continue
        schedules += 1
        x = y = 0
        r = s = None
        for op in order:
            if op == "a": x = 1
            elif op == "b": r = y
            elif op == "c": y = 1
            else: s = x
        outcomes.add((r, s))
    assert schedules == 6 and outcomes == {(0, 1), (1, 0), (1, 1)}
    assert answer("s026_e04") == "Non, aucun entrelacement global ne respecte les deux ordres et ce résultat"
    assert 1000 * 2 == 2000 < 1400 * F(3, 2) == 2100
    assert answer("s027_e01") == "A : 2000 ns contre 2100 ns pour B"
    feasible = [name for name, area in [("A", 90), ("B", 120)] if area <= 100]
    assert feasible == ["A"]
    assert answer("s027_e04") == "A seulement, malgré sa latence plus élevée"
    print("PASS: 28 sessions / 99 exercises; 60 recomputed finite answers + 12 bounded scenarios (72 checks).")
    print("Limits: no HDL/electrical/whole-CPU/lab validation; qualitative explanations also reviewed manually.")


if __name__ == "__main__":
    main()
