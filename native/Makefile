
FLAGS = -g
OBJS = taupnative.o

#
# modify INCLUDES for your local installation directories. We use:
# INCLUDES = -I /opt/local/External/Java/include -I /opt/local/External/Java/include/solaris
#

# works for the default installation of the JDK1.1.6 production release
INCLUDES = -I /usr/java1.1/include -I /usr/java1.1/include/solaris

#
# modify LIBS for the location of the java library, libjava.so for the
# Solaris JDK. It may be named differently for other java virtual machines.
#
# for green threads on Solaris we use:
# LIBS = -L/opt/local/External/Java/lib/sparc/green_threads -ljava 
#

# worksfor the default installation of the JDK1.1.6 production release
LIBS = -L/usr/java1.1/lib/sparc/native_threads -ljava 

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
