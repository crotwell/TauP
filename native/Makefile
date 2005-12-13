
FLAGS = -g
OBJS = taupnative.o

# works for the default installation of the JDK1.4.2 release on solaris
# of course /opt/local/External should be changed to reflect your java installation
INCLUDES = -I /opt/local/External/j2sdk1.4.2_01/include/ -I /opt/local/External/j2sdk1.4.2_01/include/solaris

#
# modify LIBS for the location of the java library, libjava.so for the
# Solaris JDK. It may be named differently for other java virtual machines.
#
# for the default installation of the JDK1.4.2 release on solaris
LIBS = -L/opt/local/External/j2sdk1.4.2_01/jre/lib/sparc -ljava  -ljvm

# compiles both the library and the test program
all:	tauplib gettimes 

# create the shared library
tauplib: taupnative.c
	cc ${OBJS:.o=.c} ${FLAGS} ${INCLUDES} ${LIBS} -G -o libtaup.so

# create the test application using the shared library
gettimes: gettimes.c
	cc gettimes.c ${FLAGS} ${INCLUDES} ${LIBS} -o gettimes -L. -ltaup

clean:
	/bin/rm -f libtaup.so *.o gettimes
