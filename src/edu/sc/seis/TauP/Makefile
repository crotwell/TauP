
DESTDIR = ..
INSTALLDIR = /opt/local/External/Java/jre/lib/ext
VPATH = $(DESTDIR)/edu/sc/seis/TauP:${DESTDIR}

XOBJS =	XYPlot.class \
	VelocityPlot.class \
	SlownessPlot.class \
	TauPlot.class \
	DistPlot.class \
	TimePlot.class \
	TimeDistPlot.class \
	TauP_WCreate.class \
	TauPClient.class \
	TauPDaemon.class \
	PolarPlot.class \
	TauPApplet.class

DISTOBJS =	Alert.class \
	Assert.class \
	Format.class \
	Outputs.class \
	VelocityModelException.class \
	NoSuchMatPropException.class \
	NoSuchLayerException.class \
	CriticalDepth.class \
	VelocityLayer.class \
	VelocityModel.class \
	TimeDist.class \
	DepthRange.class \
	SplitLayerInfo.class \
	SlownessModelException.class \
	SlownessLayer.class \
	SlownessModel.class \
	SphericalSModel.class \
	TauModelException.class \
	TauBranch.class \
	TauModel.class \
	TauPException.class \
	TauModelLoader.class \
	Version.class \
	PropertyLoader.class \
	TauP_Create.class \
	PhaseName.class \
	Arrival.class \
	SeismicPhase.class \
	TauP_Peek.class \
	SphericalCoords.class \
	TauP_Time.class \
	TauP_Pierce.class \
	TauP_Curve.class \
	SacTimeSeries.class \
	TauP_SetSac.class \
	TauP_Path.class \
	TauP_Table.class \
        ArrivalTableModel.class \
        ArrivalPlot.class \
        PathPlot.class \
        PhaseDialog.class \
        PierceTableModel.class \
        TauP.class

ALPHAOBJS =	Theta.class \
	ReflTransCoefficient.class \
	WKBJArrival.class \
	TauP_WKBJ.class

OBJS = ${DISTOBJS}

DOCOBJS = ${OBJS}

.SUFFIXES : .class .html .java

.java.class :
	javac -target 1.1 -classpath .:${DESTDIR} \
		-d ${DESTDIR} $<

all:	${OBJS} ${ALPHAOBJS}

dist:	${OBJS}

jdoc: 
	cp ${OBJS:.class=.java} $(DESTDIR)/edu/sc/seis/TauP/.
	javadoc -sourcepath .. -public \
		-d ../html edu.sc.seis.TauP
	/bin/rm -f $(DESTDIR)/edu/sc/seis/TauP/*.java

install: jar
	cp -f $(DESTDIR)/taup.jar ${INSTALLDIR}/taup.jar

jar:	dist stdmodels
	rm -rf ../StdModels/.HSResource ../StdModels/.HSancillary
	cp -r ${DESTDIR}/defaultProps ${DESTDIR}/StdModels ${DESTDIR}/edu/sc/seis/TauP/.
	cd ${DESTDIR} ; jar -cf taup.jar edu

bjar:	dist basicmodels
	rm -rf ../StdModels/.HSResource ../StdModels/.HSancillary
	mkdir ${DESTDIR}/edu/sc/seis/TauP/StdModels
	cp -r ${DESTDIR}/StdModels/prem.taup \
		${DESTDIR}/StdModels/iasp91.taup \
		${DESTDIR}/StdModels/ak135.taup \
		${DESTDIR}/StdModels/qdt.taup ${DESTDIR}/edu/sc/seis/TauP/StdModels/.
	cp -r ${DESTDIR}/defaultProps \
		${DESTDIR}/edu/sc/seis/TauP/.
	cd ${DESTDIR}; jar -cf taupbasic.jar edu 

qdtjar:	dist qdtmodels
	mkdir ${DESTDIR}/edu/sc/seis/TauP/StdModels
	cp -r \
		${DESTDIR}/StdModels/qdt.taup ${DESTDIR}/edu/sc/seis/TauP/StdModels/.
	cp -r ${DESTDIR}/defaultProps \
		${DESTDIR}/edu/sc/seis/TauP/.
	cd ${DESTDIR}; jar -cf taupqdt.jar edu \
		StdModels/qdt.taup \
		defaultProps

tar:	jar doc jdoc
	cd ../distribution ; make tar

stdmodels:	
	cd ../StdModels ; make

basicmodels:	
	cd ../StdModels ; make basic

qdtmodels:	
	cd ../StdModels ; make qdt

doc:
	cd ../latex ; make doc

distsrc:
	rm -f $(DESTDIR)/source/*.java
	cp -p ${OBJS:.class=.java} $(DESTDIR)/source/.

print:
	enscript -2r -G Makefile ${OBJS:.class=.java}

rebuild:
	touch ${OBJS:.class=.java}
	all

clean:
	rm -f *.class $(DESTDIR)/edu/sc/seis/TauP/*.class $(DESTDIR)/edu/sc/seis/TauP/*.java ../html/*.html
	rm -rf ../html/edu
	rm -rf  $(DESTDIR)/edu/sc/seis/TauP/StdModels

distclean:	clean
	cd ../distribution ; make clean


