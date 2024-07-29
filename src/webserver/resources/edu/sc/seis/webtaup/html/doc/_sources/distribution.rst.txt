
============
Distribution
============

--------------
What and Where
--------------

The current distribution of the TauP package is 3.0, dated July XX, 2024.

The distribution directory obtained from either the gzipped tar file or
the zip file contains:

+-------------+----------------------------------------------------------------+
| README.md   | getting started information                                    |
+-------------+----------------------------------------------------------------+
| LICENSE     | the GNU LGPL license                                           |
+-------------+----------------------------------------------------------------+
| CITATION.cff| citation information                                           |
+-------------+----------------------------------------------------------------+
| StdModels   | standard models included in the jar in lib                     |
+-------------+----------------------------------------------------------------+
| bin         | directory containing the *wrapper script* to start the tools   |
|             | each will print a usage with a --help command line argument.   |
+-------------+----------------------------------------------------------------+
| lib         | all the Java classes included in the package, along with       |
|             | saved models for prem, iasp91, and ak135 in the TauP jar file. |
|             | Also included are dependancy jars.                             |
+-------------+----------------------------------------------------------------+
| docs        | a directory with the TauP paper published in SRL and this      |
|             | manual,installation instructions are in the appendix. Also     |
|             | included are javadocs, example properties, history, Maple      |
|             | version of equations used, and a simple model file.            |
+-------------+----------------------------------------------------------------+
| src         | a directory with all of the java source code.                  |
+-------------+----------------------------------------------------------------+
| gradlew     | rebuild TauP using Gradle.                                     |
+-------------+----------------------------------------------------------------+
| gradle      | helper file for rebuild TauP using Gradle.                     |
+-------------+----------------------------------------------------------------+



---------------------------------
Advantages of the Current Release
---------------------------------

The increased flexibility of this package provides significant advantages. Among
these are:

* The ability to use many different models. We include a variety of previously created
  models as well as the option of creating your own models. A conscious effort
  was made to make as few assumptions as possible about the nature of the model.
  Therefore,
  even models that have very different structures than common global models can be
  used.
* Phase parsing. Phases are not hard coded into the program, instead the phase
  names are parsed. This creates an opportunity for the study of less common
  phases that are not present in previous travel time calculators.
* Programming interface for Java. Because of the use of the Java programming
  language, all of the tools exist simultaneously as both applications and
  libraries.
  Thus, any Java code that has a need for travel times can load and manipulate
  the objects within this package. In addition, Groovy, a popular Java
  scripting language, provides a simple means of directly accessing the public
  methods within the package.


------------
Future Plans
------------

There are several ideas for improvements that we may pursue, such as:

* A GUI. A graphical user interface would greatly improve the usefulness
  of this package, especially for non command line uses such as on the Macintosh
  or within web browsers. The beginings of such a GUI are there in the TauP tool,
  but at present it cannot access all of the functionality of the tools.
* Use of the $\tau$ function. In spite of the name, TauP does not yet use
  Tau splines. At present I do not believe that this would provide a large
  improvement over the current linear interpolation, but it is likely worth doing.
* Web based applet. One of Java's main uses currently is for the development of web based applets. An applet is a small application that is downloaded and
  executed within a web browser. This is an attractive opportunity and we have a simple
  example of one included in this distribution.
  There are difficulties as the network time to download the
  model files may be unacceptable, as well as the lack of support for Java~1.1 in current browsers. A client server architecture as well as the continued improvement of commercial web browsers
  may be able to address these issues.

* 1.1D models. There is nothing in the method that requires the source and
  receiver velocity models to be the same. With this idea, a separate crustal
  model appropriate to each region could be used for the source and receiver.

* WKBJ synthetics. The calculation of $\tau$ is a necessary step for WKBJ
  synthetics, and so this is a natural direction. It likely involves significant
  effort, however.
