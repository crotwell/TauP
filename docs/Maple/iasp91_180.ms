{VERSION 1 0 "X11/Motif" "1.0"}{GLOBALS 3 1}{FONT 0 "-adobe-helve
tica-bold-r-normal--*-140-*" "helvetica" "Helvetica-Bold" 8 14 0 
"Helvetica-Bold" 12}{FONT 1 "-adobe-times-medium-r-normal--*-140-
*" "times" "Times-Roman" 4 14 64 "Times-Roman" 12}{FONT 2 "-adobe
-courier-medium-r-normal--*-140-*" "courier" "Courier" 4 14 192 "
Courier" 12}{SCP_R 1 0 68{INP_R 2 0 "> "{TEXT 0 0 ""}}{INP_R 3 0 
"> "{TEXT 0 10 "R := 6371;"}}{OUT_R 4 0 3{DAG :3n3\`R`j2x6371}}
{COM_R 5 0{TEXT 1 40 "This is the IASP91 P wave velocity model"}}
{INP_R 6 0 "> "{TEXT 0 62 "v := r -> piecewise(-1, r<=1217.1, 11.
24094 - 4.09689*(r/R)^2,"}}{INP_R 7 0 "> "{TEXT 0 51 "r<=3482, 10
.03904 + 3.75665*(r/R)-13.67046*(r/R)^2,"}}{INP_R 8 0 "> "{TEXT 0
 32 "r<=3631, 14.49470-1.47089*(r/R),"}}{INP_R 9 0 "> "{TEXT 0 63
 "r<=5611, 25.1486-41.1538*(r/R)+51.9932*(r/R)^2-26.6083*(r/R)^3,
"}}{INP_R 10 0 "> "{TEXT 0 33 "r<=5711, 25.96984-16.93412*(r/R),"
}}{INP_R 11 0 "> "{TEXT 0 33 "r<=5961, 29.38896-21.40656*(r/R),"}
}{INP_R 12 0 "> "{TEXT 0 33 "r<=6161, 30.78765-23.25415*(r/R),"}}
{INP_R 13 0 "> "{TEXT 0 33 "r<=6251, 25.41389-17.69722*(r/R),"}}
{INP_R 14 0 "> "{TEXT 0 31 "r<=6336, 8.78541-0.74953*(r/R),"}}
{INP_R 15 0 "> "{TEXT 0 13 "r<=6351, 6.5,"}}{INP_R 16 0 "> "{TEXT
 0 14 "r<=7371, 5.8):"}}{INP_R 17 0 "> "{TEXT 0 0 ""}}{COM_R 18 0
{TEXT 1 71 "Below are the same layers as above, just split into s
eparate functions."}}{INP_R 19 0 "> "{TEXT 0 38 "v1 := r -> 11.24
094 - 4.09689*(r/R)^2;"}}{OUT_R 20 0 19{DAG :3n3\`v1`@8,2n3\`r`,1
,3n5\`operator`n4\`arrow`p7+5e3j3x01124094i2x0005j2x0001*5a2x0001
j2x0002n3\`R`i2x0002e3i3x00409689p16p7p7}}{INP_R 21 0 "> "{TEXT 0
 53 "v2 := r -> 10.03904 + 3.75665*(r/R)-13.67046*(r/R)^2;"}}
{OUT_R 22 0 21{DAG :3n3\`v2`@8,2n3\`r`,1,3n5\`operator`n4\`arrow`
p7+7e3j3x01003904i2x0005j2x0001*5a2x0001p18n3\`R`i2x0001e3j3x0037
5665p16*5p1Bj2x0002p1Ei2x0002e3i3x01367046p16p7p7}}{INP_R 23 0 ">
 "{TEXT 0 34 "v3 := r -> 14.49470-1.47089*(r/R);"}}{OUT_R 24 0 23
{DAG :3n3\`v3`@8,2n3\`r`,1,3n5\`operator`n4\`arrow`p7+5e3j3x01449
470i2x0005j2x0001*5a2x0001p18n3\`R`i2x0001e3i3x00147089p16p7p7}}
{INP_R 25 0 "> "{TEXT 0 65 "v4:= r ->  25.1486-41.1538*(r/R)+51.9
932*(r/R)^2-26.6083*(r/R)^3;"}}{OUT_R 26 0 25{DAG :3n3\`v4`@8,2n3
\`r`,1,3n5\`operator`n4\`arrow`p7+9e3j3x00251486i2x0004j2x0001*5a
2x0001p18n3\`R`i2x0001e3i3x00411538p16*5p1Bj2x0002p1Ei2x0002e3j3x
00519932p16*5p1Bj2x0003p1Ei2x0003e3i3x00266083p16p7p7}}{INP_R 27 
0 "> "{TEXT 0 35 "v5:= r ->  25.96984-16.93412*(r/R);"}}{OUT_R 28
 0 27{DAG :3n3\`v5`@8,2n3\`r`,1,3n5\`operator`n4\`arrow`p7+5e3j3x
02596984i2x0005j2x0001*5a2x0001p18n3\`R`i2x0001e3i3x01693412p16p7
p7}}{INP_R 29 0 "> "{TEXT 0 35 "v6:= r ->  29.38896-21.40656*(r/R
);"}}{OUT_R 30 0 29{DAG :3n3\`v6`@8,2n3\`r`,1,3n5\`operator`n4\`a
rrow`p7+5e3j3x02938896i2x0005j2x0001*5a2x0001p18n3\`R`i2x0001e3i3
x02140656p16p7p7}}{INP_R 31 0 "> "{TEXT 0 35 "v7:= r ->  30.78765
-23.25415*(r/R);"}}{OUT_R 32 0 31{DAG :3n3\`v7`@8,2n3\`r`,1,3n5\`
operator`n4\`arrow`p7+5e3j3x03078765i2x0005j2x0001*5a2x0001p18n3\
`R`i2x0001e3i3x02325415p16p7p7}}{INP_R 33 0 "> "{TEXT 0 35 "v8:= \+
r ->  25.41389-17.69722*(r/R);"}}{OUT_R 34 0 33{DAG :3n3\`v8`@8,2
n3\`r`,1,3n5\`operator`n4\`arrow`p7+5e3j3x02541389i2x0005j2x0001*
5a2x0001p18n3\`R`i2x0001e3i3x01769722p16p7p7}}{INP_R 35 0 "> "
{TEXT 0 33 "v9:= r ->  8.78541-0.74953*(r/R);"}}{OUT_R 36 0 35
{DAG :3n3\`v9`@8,2n3\`r`,1,3n5\`operator`n4\`arrow`p7+5e3j3x00878
541i2x0005j2x0001*5a2x0001p18n3\`R`i2x0001e3i3x00074953p16p7p7}}
{INP_R 37 0 "> "{TEXT 0 16 "v10:= r ->  6.5;"}}{OUT_R 38 0 37{DAG
 :3n3\`v10`e3j2x0065i2x0001}}{INP_R 39 0 "> "{TEXT 0 16 "v11:= r \+
->  5.8;"}}{OUT_R 40 0 39{DAG :3n3\`v11`e3j2x0058i2x0001}}{SEP_R 
41 0}{COM_R 42 0{TEXT 1 72 "Now integrate through each layer, mul
tiplying by 2 for down and back up."}}{INP_R 43 0 "> "{TEXT 0 47 
"t1 := 2*evalf(Int (1.0/v1(r), r=0..1217.1),10);"}}{OUT_R 44 0 43
{DAG :3n3\`t1`e3j4x002175155732i2x0007}}{INP_R 45 0 "> "{TEXT 0 
50 "t2 := 2*evalf(Int (1.0/v2(r), r=1217.1..3482),10);"}}{OUT_R 
46 0 45{DAG :3n3\`t2`e3j4x004832913642i2x0007}}{INP_R 47 0 "> "
{TEXT 0 48 "t3 := 2*evalf(Int (1.0/v3(r), r=3482..3631),10);"}}
{OUT_R 48 0 47{DAG :3n3\`t3`e3j4x002179383122i2x0008}}{INP_R 49 0
 "> "{TEXT 0 48 "t4 := 2*evalf(Int (1.0/v4(r), r=3631..5611),10);
"}}{OUT_R 50 0 49{DAG :3n3\`t4`e3j4x003191498456i2x0007}}{INP_R 
51 0 "> "{TEXT 0 48 "t5 := 2*evalf(Int (1.0/v5(r), r=5611..5711),
10);"}}{OUT_R 52 0 51{DAG :3n3\`t5`e3j4x001831105897i2x0008}}
{INP_R 53 0 "> "{TEXT 0 48 "t6 := 2*evalf(Int (1.0/v6(r), r=5711.
.5961),10);"}}{OUT_R 54 0 53{DAG :3n3\`t6`e3j4x005115620824i2x000
8}}{INP_R 55 0 "> "{TEXT 0 48 "t7 := 2*evalf(Int (1.0/v7(r), r=59
61..6161),10);"}}{OUT_R 56 0 55{DAG :3n3\`t7`e3j4x004619005626i2x
0008}}{INP_R 57 0 "> "{TEXT 0 48 "t8 := 2*evalf(Int (1.0/v8(r), r
=6161..6251),10);"}}{OUT_R 58 0 57{DAG :3n3\`t8`e3j4x002202005600
i2x0008}}{INP_R 59 0 "> "{TEXT 0 48 "t9 := 2*evalf(Int (1.0/v9(r)
, r=6251..6336),10);"}}{OUT_R 60 0 59{DAG :3n3\`t9`e3j4x002113114
624i2x0008}}{INP_R 61 0 "> "{TEXT 0 50 "t10 := 2*evalf(Int (1.0/v
10(r), r=6336..6351),10);"}}{OUT_R 62 0 61{DAG :3n3\`t10`e3j4x004
615384614i2x0009}}{INP_R 63 0 "> "{TEXT 0 50 "t11 := 2*evalf(Int \+
(1.0/v11(r), r=6351..6371),10);"}}{OUT_R 64 0 63{DAG :3n3\`t11`e3
j4x006896551724i2x0009}}{COM_R 65 0{TEXT 1 116 "Sum these up and \+
we get the time for PKIKP at 180 degrees. ttimes predicts 1212.12
, while taup_time\015predicts 1212.08"}}{INP_R 66 0 "> "{TEXT 0 
40 "t := t1+t2+t3+t4+t5+t6+t7+t8+t9+t10+t11;"}}{OUT_R 67 0 66{DAG
 :3n3\`t`e3j4x001212071076i2x0006}}{SEP_R 68 0}{INP_R 69 0 "> "
{TEXT 0 0 ""}}}{END}
