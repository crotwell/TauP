{VERSION 1 0 "X11/Motif" "1.0"}{GLOBALS 3 1}{FONT 0 "-adobe-helve
tica-bold-r-normal--*-140-*" "helvetica" "Helvetica-Bold" 8 14 0 
"Helvetica-Bold" 12}{FONT 1 "-adobe-times-medium-r-normal--*-140-
*" "times" "Times-Roman" 4 14 64 "Times-Roman" 12}{FONT 2 "-adobe
-courier-medium-r-normal--*-140-*" "courier" "Courier" 4 14 192 "
Courier" 12}{SCP_R 1 0 37{INP_R 2 0 "> "{TEXT 0 14 "with(student)
:"}}{SEP_R 3 0}{INP_R 4 0 "> "{TEXT 0 50 "x := Int( p / (r * sqrt
(u(r)^2 - p^2)), r=rb..rt);"}}{OUT_R 5 0 4{DAG :3n3\`x`(3n3\`Int`
,3*7n3\`p`j2x0001n3\`r`i2x0001+5*3(3n3\`u`,2pCj2x0002pA*3p8p17pE/
3pEp17=3pC~3n3\`rb`n3\`rt`}}{SEP_R 6 0}{INP_R 7 0 "> "{TEXT 0 16 
"u := r -> A*r^B;"}}{OUT_R 8 0 7{DAG :3n3\`u`@8,2n3\`r`,1,3n5\`op
erator`n4\`arrow`p7*5n3\`A`j2x0001^3a2x0001n3\`B`p14p7p7}}{SEP_R 
9 0}{INP_R 10 0 "> "{TEXT 0 8 "'x' = x;"}}{OUT_R 11 0 10{DAG =3n3
\`x`(3n3\`Int`,3*7n3\`p`j2x0001n3\`r`i2x0001+5*5n3\`A`j2x0002^3pC
n3\`B`p14pA*3p8p14pE/3pEp14=3pC~3n3\`rb`n3\`rt`}}{SEP_R 12 0}
{INP_R 13 0 "> "{TEXT 0 29 "x := changevar(A*r^B = u, x);"}}
{OUT_R 14 0 13{DAG :3n3\`x`(3n3\`Int`,3*9n3\`p`j2x0001+5*3n3\`u`j
2x0002pA*3p8p10i2x0001/3p16p10n3\`B`p16pEp16=3pE~3*5n3\`A`pA^3n3\
`rb`p1BpA*5p24pA^3n3\`rt`p1BpA}}{SEP_R 15 0}{INP_R 16 0 "> "{TEXT
 0 107 "assume( p , nonneg): additionally(ut >0): additionally(ut
 >= p): additionally(ub>0):additionally(ub >= p): "}}{INP_R 17 0 
"> "{TEXT 0 61 "dist :=  simplify(factor(subs(A*rt^B = ut, A*rb^B
 = ub, x)));"}}{OUT_R 18 0 17{DAG :3n4\`dist`*7n3\`p~~`j2x0001n3\
`B`i2x0001(3n3\`Int`,3*5+5*3n3\`u`j2x0002p7*3p5p16pB/3pBp16p14pB=
3p14~3n3\`ub~~`n3\`ut~~`p7}}{INP_R 19 0 "> "{TEXT 0 69 "dist := s
implify(value( dist), assume=real, additionally(p, nonneg));"}}
{OUT_R 20 0 19{DAG :3n4\`dist`+3*5n3\`B`i2x0001+5(3n4\`arctan`,2*
5n3\`p~~`j2x0001+5*3n3\`ut~~`j2x0002p13*3p11p19p8/3p8p19p13(3pC,2
*5p11p13+5*3n3\`ub~~`p19p13p1Cp8p20p8p13p8}}{SEP_R 21 0}{SEP_R 22
 0}{INP_R 23 0 "> "{TEXT 0 27 "u:='u': ut:='ut': ub:='ub':"}}
{INP_R 24 0 "> "{TEXT 0 55 "t := Int( u(r)^2 / (r * sqrt(u(r)^2 -
 p^2)), r=rb..rt);"}}{OUT_R 25 0 24{DAG :3n3\`t`(3n3\`Int`,3*7(3n
3\`u`,2n3\`r`j2x0002pCi2x0001+5*3p8pEj2x0001*3n3\`p~~`pEp11/3p11p
E=3pC~3n3\`rb`n3\`rt`}}{SEP_R 26 0}{INP_R 27 0 "> "{TEXT 0 12 "u:
=r->A*r^B;"}}{OUT_R 28 0 27{DAG :3n3\`u`@8,2n3\`r`,1,3n5\`operato
r`n4\`arrow`p7*5n3\`A`j2x0001^3a2x0001n3\`B`p14p7p7}}{SEP_R 29 0}
{INP_R 30 0 "> "{TEXT 0 6 "'t'=t;"}}{OUT_R 31 0 30{DAG =3n3\`t`(3
n3\`Int`,3*9n3\`A`j2x0002^3n3\`r`n3\`B`pApDi2x0001+5*5p8pApCpAj2x
0001*3n3\`p~~`pAp13/3p13pA=3pD~3n3\`rb`n3\`rt`}}{INP_R 32 0 "> "
{TEXT 0 27 "t := changevar(A*r^B=u, t);"}}{OUT_R 33 0 32{DAG :3n3
\`t`(3n3\`Int`,3*7n3\`u`j2x0001+5*3p8j2x0002pA*3n3\`p~~`pFi2x0001
/3p16pFn3\`B`p16=3p8~3*5n3\`A`pA^3n3\`rb`p1BpA*5p22pA^3n3\`rt`p1B
pA}}{SEP_R 34 0}{INP_R 35 0 "> "{TEXT 0 37 "value(subs(A*rt^B=ut,
 A*rb^B=ub, t));"}}{OUT_R 36 0 35{DAG +5*5+5*3n3\`ut`j2x0002j2x00
01*3n3\`p~~`p6i2x0001/3p8p6n3\`B`pEp8*5+5*3n3\`ub`p6p8pApEp10p13p
EpE}}{SEP_R 37 0}{INP_R 38 0 "> "{TEXT 0 0 ""}}}{END}
