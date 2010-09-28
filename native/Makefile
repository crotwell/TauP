DESTDIR=.

FLAGS = -g
OBJS = taupnative.o

#Solaris shared library - .so
#LIBF = libtaup.so
#MacOS X dynamically shared library - .dylib
LIBF = libtaup.dylib

#
# modify INCLUDES for your local installation directories. We use:
# INCLUDES = -I /opt/local/External/Java/include -I /opt/local/External/Java/include/solaris
#INCLUDES = -I /seis/local/linux/stow/sun-jdk-1.5.0_09/java/jdk1.5.0_09/include -I /seis/local/linux/stow/sun-jdk-1.5.0_09/java/jdk1.5.0_09/include/linux
#

# works for the default installation of the JDK1.1.6 production release
# INCLUDES = -I /usr/java1.1/include -I /usr/java1.1/include/solaris

#
# modify LIBS for the location of the java library, libjava.so for the
# Solaris JDK. It may be named differently for other java virtual machines.
#
# for the default installation of the JDK1.4.2 release on solaris
JAVALIB=/seis/local/linux/stow/sun-jdk-1.5.0_09/java/jdk1.5.0_09
#LIBS = -L${JAVALIB}/jre/lib/amd64 -L${JAVALIB}/jre/lib/amd64/server -L${JAVALIB}/xawt -L${JAVALIB}/native_threads -ljava  -ljvm  -lverify
#LIBS = -L${JAVALIB}/jre/lib/amd64  -ljava    -lverify
# for MacOS X 10.2 we use:
INCLUDES = -isystem /System/Library/Frameworks/JavaVM.framework/Headers
#INCLUDES = -isystem /System/Library/Frameworks/JavaVM.framework/Versions/1.3.1/Headers
LIBS = -Wl,-framework -Wl,JavaVM
#

# worksfor the default installation of the JDK1.1.6 production release
# LIBS = -L/usr/java1.1/lib/sparc/native_threads -ljava 

CFLAGS = ${FLAGS} ${INCLUDES}
FFLAGS = ${FLAGS} ${INCLUDES}

# compiles both the library and the test program
all:	taupnative.o gettimes 

# create the shared library

# create the test application using the shared library
gettimes: gettimes.c taupnative.o
	cc gettimes.c taupnative.o ${FLAGS} ${INCLUDES} ${LIBS} -o gettimes

gettimesf: gettimesf.o taupnative.o
	$(FC) -o gettimesf gettimesf.o taupnative.o ${LIBS}

install: ${LIBF}
	install -c -m 644 ${LIBF} ${DESTDIR}/${LIBF}

test: ${LIBF} gettimes.o gettimesf.o
	cc -o gettimes gettimes.o -L. -ltaup
	g77 -o gettimesf gettimesf.o -L. -ltaup
	CLASSPATH=../taup.jar gettimes
	CLASSPATH=../taup.jar gettimesf

taupnative.o: taupnative.h

clean:
	/bin/rm -f ${LIBF} *.o gettimes gettimesf
