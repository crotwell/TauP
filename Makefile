# Make file for the TauP toolkit.
#
# There are a few user-customizable items in this Makefile.  Here is a guide to
# them.
#
# BINDIR - location of the command line interface to the Toolkit, "taup"
#
# LIBDIR - location of the Java archive (taup.jar) that contains the Java code
#   and taup tables for the travel time models.  Also the location of the
#   linkable C and Fortran library interface to the taup routines, if your
#   system can build the proper libraries.
#
# TGT - version of Java Virtual Machine (JVM) for the compiled Java code (class
#   files) to run on.  You might need to change this to a different version
#   if your Java compiler complains.  As of early 2007, JVMs are at versions
#   around 1.5.
#
# To use, try "make test" and if that works, then "make install"
#
# There is a strong likelihood that the dynamically linked library interface to
# invoke the JVM will not work properly on different systems.  These may be
# safely ignored for an install -- the basic command line functionality will
# still work.
#
# This Makefile would benefit from an automake configure, particularly for
# TGT and the rather horrible Java VM linkage commands native/Makefile.
#
# G. Helffrich/U. Bristol 28 Feb. 2007

BUILD = ${PWD}
BINDIR = /usr/local/bin/geophy
LIBDIR = /usr/local/lib/geophy

PKG = edu/sc/seis/TauP
TGT = 1.1

CLASSPATH = -classpath ${BUILD}:lib/log4j-1.2.8.jar:lib/seisFile-1.0.1.jar

SUBDIRS = modelFiles native

dist all:
	make DESTDIR=${BUILD} CLASSPATH="${CLASSPATH}" TGT=${TGT} -f src/${PKG}/Makefile $@

stdmodels: dist
	(cd modelFiles; make CLASSPATH="${CLASSPATH}" $@)

tauplib:
	(cd native; make CLASSPATH="${CLASSPATH}" DESTDIR=${DESTDIR} $@)

jar ${BUILD}/taup.jar:
	make stdmodels
	[ -d ${PKG} ] || mkdir -p ${PKG}
	cp -f src/${PKG}/*.class ${PKG}
	[ -d ${PKG}/StdModels ] || mkdir -p ${PKG}/StdModels
	cp -f modelFiles/*.taup ${PKG}/StdModels
	chmod -R go+r ${PKG}
	jar cf ${BUILD}/taup.jar `echo ${PKG} | awk -F/ '{print $$1}'`
	jar uf ${BUILD}/taup.jar -C lib META-INF

install: ${BUILD}/taup.jar tauplib
	install -m 644 ${BUILD}/taup.jar ${LIBDIR}
	sed -e 's;@@INSTALL@@;${LIBDIR};' bin/taup.sh > ${BUILD}/taup
	install ${BUILD}/taup ${BINDIR}
	-(cd native; make DESTDIR=${DESTDIR} install)

clean:
	-make DESTDIR=${BUILD} -f src/${PKG}/Makefile $@
	-for d in ${SUBDIRS} ; do \
	   (cd $$d ; make CLASSPATH="${CLASSPATH}" $@) ; \
	done
	/bin/rm -rf `echo ${PKG} | awk -F/ '{print $$1}'`
	/bin/rm -r ${BUILD}/taup

distclean: clean
	/bin/rm -f ${BUILD}/taup.jar

test: ${BUILD}/taup.jar
	java -cp ${BUILD}/taup.jar edu.sc.seis.TauP.TauP_Time \
	   -h 0 -ph PKJKP -deg 113
	(cd native; make DESTDIR=${DESTDIR} test)
