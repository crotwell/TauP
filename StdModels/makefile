
CLASSPATH = -classpath /opt/local/External/Java/lib/classes.zip:..:/opt/local/Internal/Java/taup.jar
#DEBUG = -debug
DEBUG = 

.SUFFIXES: .taup .nd .tvel

QDT = qdt.taup
BASIC = prem.taup iasp91.taup ak135.taup qdt.taup
VMODS = ${BASIC} pwdk.taup jb.taup \
	1066a.taup 1066b.taup alfs.taup herrin.taup sp6.taup

.tvel.taup :	
	java ${CLASSPATH} edu.sc.seis.TauP.TauP_Create -tvel $< ${DEBUG} -verbose

.nd.taup :	
	java ${CLASSPATH} edu.sc.seis.TauP.TauP_Create -nd $< ${DEBUG} -verbose

all: ${VMODS}

basic:  ${BASIC}

qdt:  ${QDT}

qdt.taup:
	cp taup_qdt.prop .taup
	java ${CLASSPATH} edu.sc.seis.TauP.TauP_Create -tvel qdt.tvel ${DEBUG} -verbose
	rm .taup


