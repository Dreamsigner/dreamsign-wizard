"""Recheck finite authored examples against the current curriculum JSON.

Run: python tools/check_computation_theory.py
Uses only the Python standard library. These checks do not certify universal
proofs, undecidability, complexity separations, or the full MIT assignments.
"""
from pathlib import Path
from fractions import Fraction
from itertools import product
from math import comb
import json

def words(n):return [''.join(w) for w in product('01',repeat=n)]
def even_ones(w):
 state=0
 for symbol in w:
  if symbol=='1':state=1-state
 return state==0
def nfa_step(states):
 edges={0:{0,1},1:{2},2:set()}
 return set().union(*(edges[q] for q in states))
def state_set(states):return '{'+','.join(map(str,sorted(states)))+'}'
def sat_count(n,fn):return sum(bool(fn(*a)) for a in product([False,True],repeat=n))
def pcp_match(sequence):
 tiles={1:('a','ab'),2:('ba','a')}
 top=''.join(tiles[i][0] for i in sequence);bottom=''.join(tiles[i][1] for i in sequence)
 return top+' = '+bottom
def majority_error(n,p):return sum((comb(n,k)*p**k*(1-p)**(n-k) for k in range(n//2+1,n+1)),Fraction(0))

CHECKS = [{'id': 'mit_18_404j_2020_s001_e02', 'expression': 'sum(w.endswith("1") for w in words(4))'}, {'id': 'mit_18_404j_2020_s001_e04', 'expression': 'len({len(w.replace("0",""))%2 for w in words(3)})'}, {'id': 'mit_18_404j_2020_s002_e01', 'expression': '2**4'}, {'id': 'mit_18_404j_2020_s002_e02', 'expression': 'state_set(nfa_step(nfa_step({0})))'}, {'id': 'mit_18_404j_2020_s003_e03', 'expression': '"0"*3+"1"*3'}, {'id': 'mit_18_404j_2020_s004_e01', 'expression': 'len("000")'}, {'id': 'mit_18_404j_2020_s007_e01', 'expression': '3*5'}, {'id': 'mit_18_404j_2020_s007_e04', 'expression': '2*5-1'}, {'id': 'mit_18_404j_2020_s010_e02', 'expression': '3*4*2**4'}, {'id': 'mit_18_404j_2020_s010_e03', 'expression': 'pcp_match([1,2])'}, {'id': 'mit_18_404j_2020_s012_e02', 'expression': '3*2'}, {'id': 'mit_18_404j_2020_s013_e03', 'expression': '"a"*4+"b"*4'}, {'id': 'mit_18_404j_2020_s013_e06', 'expression': '2**5'}, {'id': 'mit_18_404j_2020_s014_e03', 'expression': 'sum(range(1,7))'}, {'id': 'mit_18_404j_2020_s014_e04', 'expression': '2*3'}, {'id': 'mit_18_404j_2020_s015_e04', 'expression': 'sat_count(3,lambda x,y,z:(x or y) and ((not x) or z))'}, {'id': 'mit_18_404j_2020_s016_e01', 'expression': '2*2'}, {'id': 'mit_18_404j_2020_s016_e02', 'expression': 'comb(3,2)'}, {'id': 'mit_18_404j_2020_s017_e03', 'expression': '2**5'}, {'id': 'mit_18_404j_2020_s019_e02', 'expression': '7-1'}, {'id': 'mit_18_404j_2020_s020_e01', 'expression': '(15).bit_length()'}, {'id': 'mit_18_404j_2020_s022_e01', 'expression': '(1024).bit_length()'}, {'id': 'mit_18_404j_2020_s023_e01', 'expression': 'Fraction(1,2)+Fraction(1,2)*Fraction(1,2)'}, {'id': 'mit_18_404j_2020_s023_e02', 'expression': 'majority_error(3,Fraction(1,3))'}, {'id': 'mit_18_404j_2020_s023_e04', 'expression': '(1-3)%7'}, {'id': 'mit_18_404j_2020_s024_e01', 'expression': 'sum((x*x-1)%7==0 for x in range(7))'}, {'id': 'mit_18_404j_2020_s024_e02', 'expression': 'Fraction(2,7)'}, {'id': 'mit_18_404j_2020_s025_e02', 'expression': 'Fraction(1,2)**2'}, {'id': 'mit_18_404j_2020_s025_e04', 'expression': '5-2'}, {'id': 'mit_18_404j_2020_s026_e01', 'expression': 'sat_count(2,lambda x,y:x or y)'}, {'id': 'mit_18_404j_2020_s026_e03', 'expression': '((1+0)+(1+1))%7'}, {'id': 'mit_18_404j_2020_s027_e04', 'expression': '3*Fraction(2,101)'}]

def main():
    root = Path(__file__).resolve().parents[1]
    course = json.loads((root / 'curriculum/courses/mit_18_404j_2020.json').read_text(encoding='utf8'))
    manifest = json.loads((root / course['manifest']).read_text(encoding='utf8'))
    assert len(course['lessons']) == manifest['sessionCount'] == 27
    assert [l['sourceSessionNumber'] for l in course['lessons']] == list(range(1, 28))
    assert sum(l['kind'] == 'lesson' for l in course['lessons']) == 25
    assert [l['sourceSessionNumber'] for l in course['lessons'] if l['kind'] == 'exam'] == [13, 27]
    for lesson, session in zip(course['lessons'], manifest['sessions']):
        assert (lesson['sourceSessionNumber'], lesson['sourceTitle'], lesson['kind']) == (session['number'], session['sourceTitle'], session['kind'])
        assert session['sourceUrl'] in lesson['sourceUrls']
    inventory = json.loads((root / 'docs/curriculum/ossu-course-inventory.json').read_text(encoding='utf8'))
    entry = next(c for c in inventory['courses'] if c['inventoryId'] == 'ossu_041')
    lectures = [l for l in course['lessons'] if l['kind'] == 'lesson']
    for lesson, source in zip(lectures, entry['sourceSequence']['items']):
        assert lesson['sourceTitle'] == source['sourceTitle']
        assert source['sourceUrl'] in lesson['sourceUrls']
    exercises = {e['id']: e for l in course['lessons'] for e in l['exercises']}
    assert len(exercises) == 112
    assert len(CHECKS) == 32
    for check in CHECKS:
        exercise = exercises[check['id']]
        # Expression is a trusted mathematical fixture stored in this script,
        # never text read from the curriculum or downloaded source material.
        actual = str(eval(check['expression'], globals()))
        expected = exercise['options'][exercise['correctOptionIndex']]
        assert actual == expected, (exercise['id'], actual, expected)
    # Bounded consistency checks supplement the manually explained proofs.
    assert even_ones('')
    for n in range(1, 7):
        assert sum(even_ones(w) for w in words(n)) == 2 ** (n - 1)
    # Every permitted regular-pumping split of 0^p1^p in these small instances
    # loses some zeros when pumped with i=0. The general proof is in the notes.
    for p in range(1, 9):
        word = '0' * p + '1' * p
        for x_len in range(p):
            for y_len in range(1, p - x_len + 1):
                pumped = word[:x_len] + word[x_len + y_len:]
                assert pumped.count('0') < pumped.count('1')
    assert all(any(x == y for y in [False, True]) for x in [False, True])
    assert not any(all(x == y for x in [False, True]) for y in [False, True])
    assert sat_count(2, lambda x, y: x or y) == 3
    assert sum((x + y - x * y) for x, y in product([0, 1], repeat=2)) == 3
    assert {(x * x - 1) % 7 for x in [1, 6]} == {0}
    print('PASS: 27 source mappings, 112 exercises, 32 exact finite answer checks; bounded automata/logic checks')


if __name__ == '__main__':
    main()
