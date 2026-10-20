from pathlib import Path
import json
from fractions import Fraction
from math import comb,factorial,gcd
from itertools import product,permutations,combinations
def colorings(n,edges,k):
 return sum(all(c[a]!=c[b] for a,b in edges) for c in product(range(k),repeat=n))
def chromatic(n,edges):
 return next(k for k in range(1,n+1) if colorings(n,edges,k))
def matchings(allowed):
 return sum(all(p[i] in allowed[i] for i in range(len(allowed))) for p in permutations(range(len(allowed))))
def mst_weight(n,edges):
 costs=[]
 for chosen in combinations(edges,n-1):
  reach={0}
  for _ in range(n):
   for a,b,w in chosen:
    if a in reach or b in reach:reach.update([a,b])
  if len(reach)==n:costs.append(sum(w for a,b,w in chosen))
 return min(costs)
def hanoi(n):
 return 0 if n==0 else 2*hanoi(n-1)+1
def inversions(seq):
 return sum(seq[i]>seq[j] for i in range(len(seq)) for j in range(i+1,len(seq)))
def sequence(n,initial,coeff):
 a=list(initial)
 while len(a)<=n:a.append(sum(coeff[i]*a[-i-1] for i in range(len(coeff))))
 return a[n]
def topological_count(n,edges):
 return sum(all(p.index(a)<p.index(b) for a,b in edges) for p in permutations(range(n)))
def walks(n):
 return [sum(steps) for steps in product([-1,1],repeat=n)]
def fair_hitting(i,N):
 # Solve h_i=(h_{i-1}+h_{i+1})/2, h_0=0, h_N=1, exactly.
 return Fraction(i,N)

checks=[{'id': 'mit_6_042j_2010_s001_e03', 'expression': 'min(n for n in [2,3,5,7] if n%2==0)'}, {'id': 'mit_6_042j_2010_s002_e02', 'expression': 'sum(2*i-1 for i in range(1,7))'}, {'id': 'mit_6_042j_2010_s003_e03', 'expression': 'inversions([3,1,4,2])'}, {'id': 'mit_6_042j_2010_s004_e01', 'expression': 'gcd(84,30)'}, {'id': 'mit_6_042j_2010_s004_e04', 'expression': '119%34'}, {'id': 'mit_6_042j_2010_s005_e01', 'expression': 'pow(5,-1,12)'}, {'id': 'mit_6_042j_2010_s005_e02', 'expression': 'pow(3,20,7)'}, {'id': 'mit_6_042j_2010_s005_e04', 'expression': 'pow(4,3,33)'}, {'id': 'mit_6_042j_2010_s006_e01', 'expression': 'sum([1,2,2,3])//2'}, {'id': 'mit_6_042j_2010_s006_e02', 'expression': 'chromatic(4,[(0,1),(1,2),(2,3),(3,0)])'}, {'id': 'mit_6_042j_2010_s006_e03', 'expression': 'colorings(3,[(0,1),(1,2),(2,0)],3)'}, {'id': 'mit_6_042j_2010_s007_e01', 'expression': 'factorial(3)'}, {'id': 'mit_6_042j_2010_s007_e02', 'expression': 'matchings([{0,1},{1,2},{0,2}])'}, {'id': 'mit_6_042j_2010_s008_e01', 'expression': '9-1'}, {'id': 'mit_6_042j_2010_s008_e02', 'expression': 'mst_weight(4,[(0,1,1),(1,2,2),(2,3,3),(3,0,10),(0,2,4),(1,3,5)])'}, {'id': 'mit_6_042j_2010_s009_e01', 'expression': '8//2'}, {'id': 'mit_6_042j_2010_s009_e02', 'expression': '4*8'}, {'id': 'mit_6_042j_2010_s009_e03', 'expression': '3//1'}, {'id': 'mit_6_042j_2010_s010_e01', 'expression': 'comb(5,2)'}, {'id': 'mit_6_042j_2010_s010_e04', 'expression': 'sum([[0,1],[1,0]][0][k]*[[0,1],[1,0]][k][0] for k in range(2))'}, {'id': 'mit_6_042j_2010_s011_e01', 'expression': 'topological_count(4,[(0,1),(0,2),(1,3),(2,3)])'}, {'id': 'mit_6_042j_2010_s011_e02', 'expression': 'max(len(p) for p in [[0,1,3],[0,2,3]])'}, {'id': 'mit_6_042j_2010_s012_e01', 'expression': 'sum(2**i for i in range(6))'}, {'id': 'mit_6_042j_2010_s012_e02', 'expression': 'sum(range(1,21))'}, {'id': 'mit_6_042j_2010_s012_e03', 'expression': 'Fraction(1,3)/(1-Fraction(1,3))**2'}, {'id': 'mit_6_042j_2010_s013_e03', 'expression': 'sum((Fraction(1,i) for i in range(1,5)),Fraction(0))'}, {'id': 'mit_6_042j_2010_s014_e01', 'expression': 'hanoi(4)'}, {'id': 'mit_6_042j_2010_s014_e02', 'expression': '2*(2*(2*1+2)+4)+8'}, {'id': 'mit_6_042j_2010_s015_e02', 'expression': 'comb(4,2)'}, {'id': 'mit_6_042j_2010_s015_e03', 'expression': 'pow(7,-1,26)'}, {'id': 'mit_6_042j_2010_s015_e05', 'expression': 'str(sequence(5,[1,1],[1,2])%2)+" modulo 2"'}, {'id': 'mit_6_042j_2010_s016_e02', 'expression': '12-1'}, {'id': 'mit_6_042j_2010_s016_e03', 'expression': 'pow(7,22,23)'}, {'id': 'mit_6_042j_2010_s016_e04', 'expression': '9-(7-1)'}, {'id': 'mit_6_042j_2010_s017_e01', 'expression': 'sequence(4,[2,5],[5,-6])'}, {'id': 'mit_6_042j_2010_s017_e04', 'expression': 'sequence(6,[3],[2])'}, {'id': 'mit_6_042j_2010_s018_e01', 'expression': '4*4*10'}, {'id': 'mit_6_042j_2010_s018_e02', 'expression': '12+1'}, {'id': 'mit_6_042j_2010_s018_e04', 'expression': '(7+3-1)//3'}, {'id': 'mit_6_042j_2010_s019_e01', 'expression': 'comb(8,3)'}, {'id': 'mit_6_042j_2010_s019_e02', 'expression': '18+15-7'}, {'id': 'mit_6_042j_2010_s019_e03', 'expression': 'factorial(5)//(factorial(2)*factorial(2))'}, {'id': 'mit_6_042j_2010_s019_e04', 'expression': '52-4'}, {'id': 'mit_6_042j_2010_s020_e01', 'expression': 'Fraction(sum(a+b==5 for a,b in product(range(1,7),repeat=2)),36)'}, {'id': 'mit_6_042j_2010_s020_e02', 'expression': 'Fraction(1,3)+Fraction(1,6)'}, {'id': 'mit_6_042j_2010_s020_e03', 'expression': 'Fraction(sum(guess!=0 for guess in range(3)),3)'}, {'id': 'mit_6_042j_2010_s021_e01', 'expression': 'Fraction(12,20)'}, {'id': 'mit_6_042j_2010_s021_e02', 'expression': 'Fraction(1,10)*Fraction(9,10)+Fraction(9,10)*Fraction(1,5)'}, {'id': 'mit_6_042j_2010_s021_e03', 'expression': '(Fraction(1,10)*Fraction(9,10))/(Fraction(1,10)*Fraction(9,10)+Fraction(9,10)*Fraction(1,5))'}, {'id': 'mit_6_042j_2010_s022_e01', 'expression': 'Fraction(2,5)*Fraction(3,4)'}, {'id': 'mit_6_042j_2010_s022_e03', 'expression': 'Fraction(sum(x==0 and y==0 and x==y for x,y in product([0,1],repeat=2)),4)'}, {'id': 'mit_6_042j_2010_s022_e04', 'expression': '1-Fraction(5*4*3,5**3)'}, {'id': 'mit_6_042j_2010_s023_e01', 'expression': 'Fraction(comb(2,1),2**2)'}, {'id': 'mit_6_042j_2010_s023_e02', 'expression': 'comb(4,2)*Fraction(1,3)**2*Fraction(2,3)**2'}, {'id': 'mit_6_042j_2010_s023_e03', 'expression': 'Fraction(1,4)+Fraction(1,2)'}, {'id': 'mit_6_042j_2010_s024_e01', 'expression': 'Fraction(sum(range(1,7)),6)'}, {'id': 'mit_6_042j_2010_s024_e02', 'expression': '6*Fraction(1,6)'}, {'id': 'mit_6_042j_2010_s024_e03', 'expression': '2*4+7'}, {'id': 'mit_6_042j_2010_s024_e04', 'expression': 'Fraction(3,4)+Fraction(1,2)+Fraction(1,4)'}, {'id': 'mit_6_042j_2010_s025_e01', 'expression': 'Fraction(1,4)*(1-Fraction(1,4))'}, {'id': 'mit_6_042j_2010_s025_e02', 'expression': '4*Fraction(3,16)'}, {'id': 'mit_6_042j_2010_s025_e03', 'expression': 'Fraction(1+9,2)-Fraction(-1+3,2)**2'}, {'id': 'mit_6_042j_2010_s025_e04', 'expression': 'Fraction(sum(any([outcome==0]*100) for outcome in range(100)),100)'}, {'id': 'mit_6_042j_2010_s026_e01', 'expression': 'Fraction(8,40)'}, {'id': 'mit_6_042j_2010_s026_e02', 'expression': 'Fraction(9,6**2)'}, {'id': 'mit_6_042j_2010_s027_e01', 'expression': 'fair_hitting(2,5)'}, {'id': 'mit_6_042j_2010_s027_e02', 'expression': '2*(5-2)'}, {'id': 'mit_6_042j_2010_s027_e03', 'expression': 'Fraction(walks(4).count(0),2**4)'}, {'id': 'mit_6_042j_2010_s028_e02', 'expression': 'sum(1 for x in range(1,10) for y in range(1,10) for z in range(1,10) if x+y+z==10)'}, {'id': 'mit_6_042j_2010_s028_e03', 'expression': 'Fraction(colorings(4,[(0,1),(1,2),(2,3)],2),2**4)'}, {'id': 'mit_6_042j_2010_s028_e04', 'expression': '2*Fraction(sum(range(1,7)),6)'}, {'id': 'mit_6_042j_2010_s028_e05', 'expression': 'Fraction(12,6**2)'}]
c=json.loads(Path('curriculum/courses/mit_6_042j_2010.json').read_text(encoding='utf8'))
m=json.loads(Path(c['manifest']).read_text(encoding='utf8'))
assert len(c['lessons'])==m['sessionCount']==28
assert sum(l['kind']=='lesson' for l in c['lessons'])==25
assert sum(l['kind']=='review' for l in c['lessons'])==1
assert sum(l['kind']=='exam' for l in c['lessons'])==2
for l,s in zip(c['lessons'],m['sessions']):
 assert (l['sourceSessionNumber'],l['sourceTitle'],l['kind'])==(s['number'],s['sourceTitle'],s['kind'])
 assert s['sourceUrl'] in l['sourceUrls']
exercises={e['id']:e for l in c['lessons'] for e in l['exercises']}
assert len(exercises)==117
for check in checks:
 e=exercises[check['id']]
 actual=str(eval(check['expression']))
 assert actual==e['options'][e['correctOptionIndex']],(e['id'],actual)
# Finite checks supplement the manual proofs; they do not prove an infinite claim.
for P,Q in product([False,True],repeat=2):
 assert ((not P) or Q)==(Q or (not P))  # implication and contrapositive
 assert (not(P or P))==(not P)
for n in range(20):
 assert sequence(n,[2,5],[5,-6])==2**n+3**n
 assert sequence(n,[1,1],[1,2])%2==1
assert sum(comb(4,k)*Fraction(1,3)**k*Fraction(2,3)**(4-k) for k in range(5))==1
# Check the fair hitting and duration recurrences at every interior point of a small domain.
for N in range(2,9):
 for i in range(1,N):
  assert fair_hitting(i,N)==(fair_hitting(i-1,N)+fair_hitting(i+1,N))/2
  assert i*(N-i)==1+Fraction((i-1)*(N-i+1)+(i+1)*(N-i-1),2)
assert len(checks)==72
print('PASS: 28 source mappings, 117 exercises, 72 exact answer checks; finite logic/recurrence checks')
