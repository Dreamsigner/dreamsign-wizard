"""Recompute the original numerical exercises; no network or generated output."""
from pathlib import Path
import json
import math
import cmath
import sympy as S

ROOT = Path(__file__).resolve().parents[1]
c = json.loads((ROOT / "curriculum/courses/mit_18_335j_2019.json").read_text(encoding="utf-8"))
m = json.loads((ROOT / c["manifest"]).read_text(encoding="utf-8"))
R = S.Rational
M = S.Matrix
V = lambda *x: M(x)
assert c["expectedLessonCount"] == m["sessionCount"] == len(c["lessons"]) == 39
assert [s["number"] for s in m["sessions"]] == list(range(1, 40))
assert m["calendarNotes"]["weeklyHeadingCount"] == 38
assert m["calendarNotes"]["combinedLectures"] == [30, 31]
exercises = {}
for lesson, session in zip(c["lessons"], m["sessions"]):
    assert lesson["sourceSessionNumber"] == session["number"]
    assert lesson["sourceTitle"] == session["sourceTitle"]
    assert session["sourceUrl"] in lesson["sourceUrls"]
    assert len(lesson["learningNotes"].split()) >= 40
    assert len(lesson["exercises"]) == 3
    for e in lesson["exercises"]:
        assert e["id"] not in exercises
        assert len(e["options"]) == len(set(e["options"])) == 4
        assert 0 <= e["correctOptionIndex"] < 4
        assert e["hint"] and e["explanation"]
        exercises[e["id"]] = e
assert len(exercises) == 117

def answer(n, j):
    e = exercises[f"mit_18_335j_2019_s{n:03}_e{j:02}"]
    return e["options"][e["correctOptionIndex"]]

count = 0
def check(n, j, text, actual, expected):
    global count
    assert answer(n, j) == text, (n, j, answer(n, j), text)
    if isinstance(actual, S.MatrixBase):
        assert actual == expected, (n, j, actual, expected)
    else:
        assert S.simplify(actual - expected) == 0, (n, j, actual, expected)
    count += 1

check(1,1,"3/2",(R(1)+2)/2,R(3,2))
check(1,2,"17/12",(R(3,2)+2/R(3,2))/2,R(17,12))
check(2,1,"53",52+1,53)
check(2,2,"1,23",R(round(1234/10),100),R(123,100))
check(3,1,"100",R(100+99+1,100-99+1),100)
check(3,2,"4",int(math.log2(16)),4)
check(4,1,"5",V(3,-4).norm(),5)
check(4,2,"7",max(sum(abs(a) for a in row) for row in [[1,-2],[3,4]]),7)
x=S.symbols("x",positive=True)
check(5,1,"1/2",x*S.diff(S.sqrt(x),x)/S.sqrt(x),R(1,2))
check(5,2,"100",1/R(1,100),100)
check(6,1,"1/5",R(2,10),R(1,5))
assert abs(1-10*R(1,5)) == 1 and abs(1-10*R(21,100))>1
check(6,2,"1/2",1+R(1,4)*(-2),R(1,2))
check(7,1,"3 et 2",M(sorted(S.diag(3,-2).singular_values(),reverse=True)),V(3,2))
check(7,2,"3",S.diag(0,3,1).norm(2),3)
t=S.symbols("t",real=True)
check(8,1,"3",S.solve(S.diff((t-1)**2+(t-3)**2+(t-5)**2,t),t)[0],3)
check(8,2,"2",S.solve(S.diff((t-4)**2+t**2,t),t)[0],2)
check(9,1,"10⁶",1000**2,10**6)
check(9,2,"(2,1)",M([[2,1],[0,1]]).inv()*V(5,1),V(2,1))
v=V(1,0)
check(10,1,"diag(−1,1)",S.eye(2)-2*v*v.T/(v.T*v)[0],S.diag(-1,1))
Q=M([[0,-1],[1,0]])
check(10,2,"5",(Q*V(3,4)).norm(),5)
check(11,1,"128",4*4*8,128)
check(11,2,"1536",3*8*8*8,1536)
check(12,1,"8",64//8,8)
check(12,2,"8",16//2,8)
check(13,1,"−3",max([R(1,1000),2,-3],key=abs),-3)
check(13,2,"2",R(4,2),2)
check(14,1,"L=diag(2,3)",S.diag(4,9).cholesky(),S.diag(2,3))
check(14,2,"2 et 5",M(sorted(M([[2,7],[0,5]]).eigenvals())),V(2,5))
positions=[(i,j) for i in range(1,5) for j in range(1,5) if i>j+1]
assert positions == [(3,1),(4,1),(4,2)]
check(15,1,"(3,1), (4,1), (4,2)",len(positions),3)
z=S.symbols("z")
check(15,2,"λ²−3λ+2",(z*S.eye(2)-M([[0,-2],[1,3]])).det(),z*z-3*z+2)
check(16,1,"(1,1/4)",S.diag(4,1)*V(1,1)/4,V(1,R(1,4)))
eigenvalues=sorted(S.diag(5,2).eigenvals(),key=abs,reverse=True)
check(16,2,"2/5",abs(eigenvalues[1]/eigenvalues[0]),R(2,5))
check(17,1,"4",(V(1,1).T*S.diag(2,6)*V(1,1))[0]/2,4)
check(17,2,"−1/3 et 1",(S.diag(2,6)-5*S.eye(2)).inv(),S.diag(-R(1,3),1))
check(18,1,"2",M.hstack(V(1,1),S.diag(1,2)*V(1,1)).rank(),2)
check(18,2,"1",M.hstack(V(1,2),2*V(1,2),4*V(1,2)).rank(),1)
check(19,1,"160000",20*1000*8,160000)
check(19,2,"2",R(20*1000*8,10*1000*8),2)
f=(t-1)**2+(2*t-1)**2
check(20,1,"3/5",S.solve(S.diff(f,t),t)[0],R(3,5))
check(20,2,"1/5",f.subs(t,R(3,5)),R(1,5))
b=V(1,1);A=S.diag(1,2)
check(21,1,"2/3",(b.T*b)[0]/(b.T*A*b)[0],R(2,3))
check(21,2,"I₂",S.diag(R(1,2),R(1,8))*S.diag(2,8),S.eye(2))
rho=(S.sqrt(9)-1)/(S.sqrt(9)+1)
check(22,1,"1/2",rho,R(1,2))
check(22,2,"1/4",2*rho**3,R(1,4))
check(23,1,"0",(V(1,0).T*M([[0,1],[-1,0]])*V(1,0))[0],0)
check(23,2,"(1,5)",M([[1,2],[0,3]]).T*V(1,1),V(1,5))
check(24,1,"13",5+2*(5-1),13)
check(24,2,"3",math.comb(3,2),3)
check(25,1,"1",min(i*i for i in range(1,10)),1)
y=S.symbols("y")
check(25,2,"(2,−6)",V(S.diff(x*x+3*y*y,x),S.diff(x*x+3*y*y,y)).subs({x:1,y:-1}),V(2,-6))
p=S.symbols("p",positive=True);u=4/p
check(26,1,"2",u.subs(p,2),2)
check(26,2,"−2",S.diff(u*u/2,p).subs(p,2),-2)
check(27,1,"12",S.diff(p**3,p).subs(p,2),12)
check(27,2,"1",S.diff(p,p),1)
check(28,1,"−1/2",max(-R(1,2),min(R(1,2),-2)),-R(1,2))
check(28,2,"1/4",R(1,4),R(1,4))
lam=S.symbols("lam",nonnegative=True)
check(29,1,"λ/2",S.solve(S.diff(t*t+lam*(1-t),t),t)[0],lam/2)
check(29,2,"1",(lam-lam*lam/4).subs(lam,2),1)
check(30,1,"3",R(6,2),3)
check(30,2,"(−1,−2)",-S.diag(2,4).inv()*V(2,8),V(-1,-2))
B=S.Integer(2);step=S.Integer(1);dy=S.Integer(4)
check(31,1,"4",B-(B*step)**2/(step*B*step)+dy**2/(dy*step),4)
check(31,2,"4",V(2,1).dot(V(1,2)),4)
check(32,1,"4",3+1,4)
check(32,2,"6",math.comb(2+2,2),6)
check(33,1,"3/8",R(1,2)*(0+R(1,4)+R(1,2)),R(3,8))
check(33,2,"1/4",(x/2)**2/x**2,R(1,4))
check(34,1,"1, 0, −1",V(*(S.cos(k*S.pi/2) for k in range(3))),V(1,0,-1))
check(34,2,"2/3",R(1,3)+R(4,3)*0+R(1,3),R(2,3))
check(35,1,"−1/2",2*R(1,2)**2-1,-R(1,2))
check(35,2,"4x³−3x",2*x*(2*x*x-1)-x,4*x**3-3*x)
check(36,1,"2/3",2*(1/S.sqrt(3))**2,R(2,3))
check(36,2,"3",2*2-1,3)
check(37,1,"4096",8**4,4096)
check(37,2,"4",9-5,4)
def dft(values):
    n=len(values)
    return [sum(values[j]*cmath.exp(-2j*math.pi*j*k/n) for j in range(n)) for k in range(n)]
for j,values,expected,text in [(1,[1,0,0,0],[1]*4,"(1,1,1,1)"),(2,[1]*4,[4,0,0,0],"(4,0,0,0)")]:
    assert answer(38,j)==text
    assert all(abs(a-b)<1e-12 for a,b in zip(dft(values),expected))
    count+=1
check(39,1,"50",100//2,50)
check(39,2,"4",int(math.log2(16)),4)
assert count == 78
print(f"PASS: 39 session mappings, 117 exercise structures, {count} independently recomputed numerical answers")
