#!/usr/bin/env python3
"""Check source coverage and independently recompute supported exercise answers."""
import json
import sys
from fractions import Fraction
from itertools import combinations
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from validate_curriculum import validate_course
ROOT=Path(__file__).resolve().parents[1]
COURSE=ROOT/'curriculum/courses/roughgarden_algorithmic_game_theory_2013.json'
MANIFEST=ROOT/'docs/curriculum/roughgarden-game-theory-source-sessions.json'

def compute(v):
 k=v['kind']
 if k=='identity': return v['result']
 if k=='second_price':
  b=v['bids']; w=max(b,key=b.get); return f"{w}, {max((x for n,x in b.items() if n!=w),default=0)}"
 if k=='second_price_utility':
  b=v['bids']; who=v['player']; w=max(b,key=b.get)
  return str(v['values'][who]-max(x for n,x in b.items() if n!=who)) if who==w else '0'
 if k=='critical_bid': return str(max(v['otherBids']))
 if k=='knapsack':
  names=list(v['items']); best=(0,())
  for r in range(len(names)+1):
   for g in combinations(names,r):
    size=sum(v['items'][x][1] for x in g); val=sum(v['items'][x][0] for x in g)
    if size<=v['capacity'] and val>best[0]: best=(val,g)
  return f"{' et '.join(best[1])} : {best[0]}"
 if k=='expected_revenue': return str(Fraction(str(v['price']))*sum((Fraction(str(p)) for a,p in v['valueProbabilities'] if a>=v['price']),Fraction()))
 if k=='multiply': return str(Fraction(str(v['a']))*Fraction(str(v['b'])))
 if k=='prob_at_least_one':
  q=Fraction(1)
  for p in v['probabilities']: q*=1-Fraction(str(p))
  return str(1-q)
 if k=='power_two': return str(2**v['n'])
 if k=='vcg_payment': return str(v['othersWithout']-v['othersWith'])
 if k=='subtract': return str(v['a']-v['b'])
 if k=='budget_check': return 'Non, le paiement dépasse le budget' if v['payment']>v['budget'] else 'Oui'
 if k=='sum': return str(sum(v['values']))
 if k=='pure_nash':
  r,c=v['rowCosts'],v['columnCosts']; eq=[]
  for i in range(len(r)):
   for j in range(len(r[i])):
    if r[i][j]==min(r[x][j] for x in range(len(r))) and c[i][j]==min(c[i][x] for x in range(len(c[i]))): eq.append(f"({v['rowActions'][i]},{v['columnActions'][j]})")
  return ' et '.join(eq) if eq else 'Aucune'
 if k=='ratio': return str(Fraction(str(v['numerator']))/Fraction(str(v['denominator'])))
 if k=='queue_delay': return str(Fraction(1,v['capacity']-v['load']))
 if k=='bound_ratio': return str(Fraction(v['lambda'])/(1-Fraction(str(v['mu']))))
 if k=='harmonic': return str(sum((Fraction(1,i) for i in range(1,v['n']+1)),Fraction()))
 if k=='minimum': return min(v['values'],key=v['values'].get)
 if k=='external_regret':
  total=sum(v['chosenCosts']); best=min(sum(a) for a in v['actionCosts'].values()); return str(Fraction(total-best,len(v['chosenCosts'])))
 if k=='regret': return str(Fraction(v['chosenTotal']-v['fixedTotal'],v['rounds']))
 if k=='local_optimum': return 'Oui' if all(v['current']>=x for x in v['neighbors']) else 'Non'
 if k in {'matching_pennies'}:
  assert sum(v['probabilities'])==1 and v['probabilities'][0]==v['probabilities'][1]==Fraction(1,2)
  return '1/2' if k=='matching_pennies' else 'Pile et face avec probabilité 1/2 chacune'
 raise ValueError(f'unsupported verification kind: {k}')

def main():
 d=json.loads(COURSE.read_text(encoding='utf-8')); m=json.loads(MANIFEST.read_text(encoding='utf-8'))
 assert d['status']=='reviewed_for_catalogue' and d['validatedLessonCount']==20
 assert d['expectedLessonCount']==m['sessionCount']==20
 assert d['authoredLessonCount']==len(d['lessons'])==len(m['sessions'])==20
 errors=validate_course(d,m)
 if errors: raise AssertionError('production schema validation failed: ' + '; '.join(errors))
 kinds=set(); count=0; recomputed=0; assertions=0; exercise_ids=set()
 for i,(lesson,session) in enumerate(zip(d['lessons'],m['sessions']),1):
  assert lesson['sourceSessionNumber']==session['number']==i
  assert lesson['sourceTitle']==session['sourceTitle'] and lesson['sourceUrls']==[session['sourceUrl']]
  assert len(lesson['learningNotes'])>=300 and lesson['objective'] and len(lesson['exercises'])>=2
  assert session['contentStatus']=='authored'
  proof=session['consultedMaterials'][0]
  assert proof['pageCount']>0 and proof['extractedTextCharacters']>1000 and len(proof['sha256'])==64
  for exercise_index,ex in enumerate(lesson['exercises'],1):
   eid=ex.get('id','')
   assert eid==f"{lesson['id']}_e{exercise_index:02d}" and eid not in exercise_ids, f"unstable or duplicate exercise ID: {eid!r}"
   exercise_ids.add(eid)
   assert 0<=ex['correctOptionIndex']<len(ex['options']) and ex.get('hint') and ex.get('explanation')
   result=compute(ex['verification']); answer=ex['options'][ex['correctOptionIndex']]
   assert result==answer, f"{lesson['id']}: independent result {result!r} != selected answer {answer!r}"
   kinds.add(ex['verification']['kind']); count+=1
   if ex['verification']['kind']=='identity': assertions+=1
   else: recomputed+=1
 required={'second_price','second_price_utility','knapsack','pure_nash','external_regret','matching_pennies'}
 assert required<=kinds, f'missing checks {required-kinds}'
 # Regression for the source-model mismatch caught in review: the Pigou example
 # is nonatomic (4/3), whereas the next lecture's atomic example has ratio 5/2.
 pigou_cost=lambda flow: flow*flow+1-flow
 optimum=pigou_cost(Fraction(1,2))
 assert optimum==Fraction(3,4)
 assert compute(d['lessons'][10]['exercises'][0]['verification'])==str(1/optimum)
 assert compute(d['lessons'][11]['exercises'][2]['verification'])==str(Fraction(10,4))
 assert all(e['explanation']!=e['options'][e['correctOptionIndex']] for l in d['lessons'] for e in l['exercises'])
 print(f"OK: validate_course passed; 20/20 source sessions; {count} exercise responses checked ({recomputed} independently recomputed, {assertions} authored concept assertions); {len(kinds)} check types.")
if __name__=='__main__': main()
