import ast,json,pathlib,subprocess,sys
root=pathlib.Path.cwd()
c=json.loads((root/'curriculum/courses/mit_6_100l_2022.json').read_text(encoding='utf8'))
m=json.loads((root/c['manifest']).read_text(encoding='utf8'))
assert len(c['lessons'])==m['sessionCount']==26
exercises={e['id']:e for l in c['lessons'] for e in l['exercises']}
def answer(n,i):
 e=exercises[f'mit_6_100l_2022_s{n:03}_e{i:02}']
 return e['options'][e['correctOptionIndex']]
count=0
for lesson,session in zip(c['lessons'],m['sessions']):
 assert lesson['sourceSessionNumber']==session['number']
 assert lesson['sourceTitle']==session['sourceTitle'] and lesson['kind']==session['kind']
 assert session['sourceUrl'] in lesson['sourceUrls']
 for e in lesson['exercises']:
  if e['type']!='code':continue
  code=e['codeSnippet'];tree=ast.parse(code)
  assert not any(isinstance(x,(ast.Import,ast.ImportFrom)) for x in ast.walk(tree))
  assert not any(isinstance(x,ast.Call) and isinstance(x.func,ast.Name) and x.func.id in {'open','input','exec','eval','compile','__import__','breakpoint'} for x in ast.walk(tree))
  p=subprocess.run([sys.executable,'-I','-c',code],capture_output=True,text=True,encoding='utf8',timeout=3)
  assert p.returncode==0,(e['id'],p.stderr)
  assert p.stdout.strip()==e['options'][e['correctOptionIndex']],(e['id'],p.stdout)
  count+=1
# Independent arithmetic checks for numerical choice questions.
assert answer(4,3)==str(1*16+0*8+1*4+1*2+0)
assert answer(6,2)==str(16//2**4)
assert answer(6,3)=='[0, 1]' and 0<=0.04**0.5<=1
assert answer(21,2)==str(3**2)
assert answer(22,4)==str((1024).bit_length()-1)
assert answer(23,3)==str(2**6)
assert answer(24,3)==f'{100+10*5} contre {10*25}'
assert answer(26,4)=='3/4' and sum(a or b for a in (False,True) for b in (False,True))==3
assert 0.5*(6/1+6/3)==4 and 6/((1+3)/2)==3
assert count==70 and len(exercises)==104
print('PASS: 26 source mappings, 104 exercises, 70 executed code answers, 9 arithmetic checks')
