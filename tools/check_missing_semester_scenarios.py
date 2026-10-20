import subprocess,pathlib,tempfile,json,re,os,posixpath,itertools,shutil
git_bash=pathlib.Path('C:/Program Files/Git/bin/bash.exe')
bash=str(git_bash) if os.name=='nt' and git_bash.is_file() else shutil.which('bash')
assert bash, 'Bash is required for the shell scenario checks'
checks=[]
def B(label,script,expected,args=(),env=None):
 r=subprocess.run([bash,'--noprofile','--norc','-c',script,'exercise',*args],text=True,capture_output=True,encoding='utf-8',env=env);assert r.returncode==0,(label,r.stderr);assert r.stdout.strip()==expected,(label,r.stdout,expected);checks.append(label)
B('argument quoted',"printf '%s\\n' \"plan final.pdf\"",'plan final.pdf')
B('all arguments',"printf '%s\\n' \"$@\"",'alpha beta\ngamma',('alpha beta','gamma'))
B('and or',"false && printf '%s\\n' suite || printf '%s\\n' secours",'secours')
B('environment',"APP_MODE=local\nAPP_MODE=distant bash -c ':'\nprintf '%s\\n' \"$APP_MODE\"",'local')
B('pipeline standard streams',"outil() { printf '%s\\n' succès; printf '%s\\n' erreur >&2; }; outil | grep succès",'succès')
B('sort uniq',"printf '%s\\n' bleu rouge bleu | sort | uniq -c | awk '{print $1, $2}'",'2 bleu\n1 rouge')
f=pathlib.Path(tempfile.gettempdir())/'missing-semester-2026-append-fixture.txt';f.write_text('debut\n',encoding='utf-8');env=os.environ.copy();env['MISSING_FIXTURE']=f.as_posix();B('append',"printf '%s\\n' fin >> \"$MISSING_FIXTURE\"\ncat \"$MISSING_FIXTURE\"",'debut\nfin',env=env)
assert posixpath.normpath('/tmp/atelier/src/../.')=='/tmp/atelier';checks.append('relative path')
assert re.fullmatch(r'\d{4}-\d{2}-\d{2}','2026-01-99');assert all(not re.fullmatch(r'\d{4}-\d{2}-\d{2}',v) for v in ['26-01-09','2026/01/09','texte']);checks.append('regex four choices')
left=[1,4];right=[2,3,5];i=1;j=0;assert right[i]==3 and right[j]==2;checks.append('merge right index')
assert [v for v in [(1,9),(1,5),(2,0),(0,9)] if (1,4)<=v<(2,0) and v>=(1,8)]==[(1,9)];checks.append('dependency constraints')
assert len(list(itertools.product(['3.11','3.12'],['Linux','Windows'])))==4;checks.append('ci matrix')
print('PASS',len(checks),'executable or exact scenario checks:',checks)
(pathlib.Path(tempfile.gettempdir())/'missing-semester-2026-validation.json').write_text(json.dumps(dict(status='PASS',scenarioChecks=len(checks),checks=checks)),encoding='utf-8')
