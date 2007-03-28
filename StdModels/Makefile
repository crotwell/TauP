
MODELS = 1066a.taup \
	1066b.taup \
	ak135.taup \
	alfs.taup \
	herrin.taup \
	iasp91.taup \
	jb.taup \
	prem.taup \
	qdt.taup \
	prem.taup \
	pwdk.taup \
	sp6.taup

MODELDEST = edu/sc/seis/TauP/StdModels

.SUFFIXES : .nd .tvel .taup

.nd.taup:
	java ${CLASSPATH} -cp ../src edu.sc.seis.TauP.TauP_Create $<
.tvel.taup:
	java ${CLASSPATH} -cp ../src edu.sc.seis.TauP.TauP_Create $<

stdmodels: ${MODELS}
	[ -d ../src/${MODELDEST} ] || mkdir ../src/${MODELDEST} 
	cp -f $? ../src/${MODELDEST} 
	for f in $? ; do chmod go+r ../src/${MODELDEST}/$$f ; done

clean:
	/bin/rm -f *.taup
