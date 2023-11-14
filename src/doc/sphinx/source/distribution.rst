
============
Distribution
============


\subsection{What and Where}
The current distribution of the TauP package is 2.7, dated June XX, 2022.

The distribution directory obtained from either the gzipped tar file or the jar file contains:

\begin{center}
\begin{tabular}{lp{4in}}
README.md & getting started information \\
LICENSE & the GNU LGPL license \\
StdModels & standard models included in the jar in lib \\
bin & directory containing ``wrapper scripts'' to start the tools,
   each will print a usage with a --help command line argument. \\
lib & all the Java classes included in the package, along with
               saved models for prem, iasp91, and ak135 in the TauP jar file.
               Also included are dependancy jars. \\
docs & a directory with the TauP paper published in SRL and this manual,
installation instructions are in the appendix. Also included are javadocs,
example properties, history, Maple version of equations used, and a simple model file.\\
groovy & a directory with Groovy example script for accessing the TauP package directly within scripts. \\
jacl & a directory with Jacl examples for accessing the TauP package directly within scripts. \\
native & a directory with a C library and example program that use the
Java Native Interface, providing a basic interface between C programs
and the TauP package. \\
src & a directory with all of the java source code. \\
gradlew & rebuild TauP using Gradle. \\
gradle & helper file for rebuild TauP using Gradle. \\
\end{tabular}
\end{center}

The taup.jar file contains everything needed for a working version of the package.
This greatly simplifies the installation process and reduces potential errors.
See appendix \ref{install} for detailed installation instructions.

\subsection{Advantages of the Current Release}
The increased flexibility of this package provides significant advantages. Among
these are:
\begin{enumerate}
\item The ability to use many different models. We include a variety of previously created
models as well as the option of creating your own models. A conscious effort
was made to make as few assumptions as possible about the nature of the model.
Therefore,
even models that have very different structures than common global models can be
used.

\item Phase parsing. Phases are not hard coded into the program, instead the phase
names are parsed. This creates an opportunity for the study of less common
phases that are not present in previous travel time calculators.

\item Programming interface for Java. Because of the use of the Java programming
language, all of the tools exist simultaneously as both applications and libraries.
Thus, any Java code that has a need for travel times can load and manipulate
the objects within this package. In addition, Groovy, a popular Java scripting language, provides a simple means of directly accessing the public methods within the package.

\end{enumerate}


\subsection{Future Plans}

There are several ideas for improvements that we may pursue, such as:

\begin{enumerate}
\item A GUI. A graphical user interface would greatly improve the usefulness
of this package, especially for non command line uses such as on the Macintosh
or within web browsers. The beginings of such a GUI are there in the TauP tool,
but at present it cannot access all of the functionality of the tools.

\item Use of the $\tau$ function. In spite of the name, TauP does not yet use
Tau splines. At present I do not believe that this would provide a large
improvement over the current linear interpolation, but it is likely worth doing.

\item Web based applet. One of Java's main uses currently is for the development of web based applets. An applet is a small application that is downloaded and
executed within a web browser. This is an attractive opportunity and we have a simple
example of one included in this distribution.
There are difficulties as the network time to download the
model files may be unacceptable, as well as the lack of support for Java~1.1 in current browsers. A client server architecture as well as the continued improvement of commercial web browsers
may be able to address these issues.

\item 1.1D models. There is nothing in the method that requires the source and
receiver velocity models to be the same. With this idea, a separate crustal
model appropriate to each region could be used for the source and receiver.

\item WKBJ synthetics. The calculation of $\tau$ is a necessary step for WKBJ
synthetics, and so this is a natural direction. It likely involves significant
effort, however.
\end{enumerate}
