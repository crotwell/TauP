
============
Distribution
============

--------------
What and Where
--------------

The current distribution of the TauP package is |release|, created |today|.

Downloads available from either
`GitHub <https://github.com/crotwell/TauP/releases>`_ or
`Zenodo <https://zenodo.org/records/15426279>`_. Installation also
available for Macintosh OSX via
`Homebrew <https://brew.sh/>`_ with:

.. code-block:: bash

  brew tap crotwell/crotwell && brew install taup

or for Linux via
`Snap <https://snapcraft.io/taup>`_ with:

.. code-block:: bash

  sudo snap install taup

The distribution directory obtained from either the gzipped tar file or
the zip file contains:

+-----------------+----------------------------------------------------------------+
| README.md       | getting started information                                    |
+-----------------+----------------------------------------------------------------+
| LICENSE         | the GNU LGPL license                                           |
+-----------------+----------------------------------------------------------------+
| VERSION         | the current version                                            |
+-----------------+----------------------------------------------------------------+
| CITATION.cff    | citation information                                           |
+-----------------+----------------------------------------------------------------+
| StdModels       | standard models included within the jar in lib                 |
+-----------------+----------------------------------------------------------------+
| bin             | directory containing the *wrapper script* to start the tools   |
|                 | each will print a usage with a --help command line argument.   |
+-----------------+----------------------------------------------------------------+
| lib             | all the Java classes included in the package, along with       |
|                 | saved models for prem, iasp91, and ak135 in the TauP jar file. |
|                 | Also included are dependancy jars.                             |
+-----------------+----------------------------------------------------------------+
| docs            | a directory with the TauP paper published in SRL and this      |
|                 | manual,installation instructions are in the appendix. Also     |
|                 | included are javadocs, example properties, history, Maple      |
|                 | version of equations used, and a simple model file.            |
+-----------------+----------------------------------------------------------------+
| src             | a directory with all of the java source code.                  |
+-----------------+----------------------------------------------------------------+
| gradlew*         | rebuild TauP using Gradle. Not needed unless recompiling for   |
|                 | debugging or code modification.                                |
+-----------------+----------------------------------------------------------------+



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
  used. Users have calculated travel times for various earth models, models
  of Mars, the Moon, and even a kiwi. Yes, the fruit!
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
* Web server interface. Version 3 ships with an embedded web server and web
  page that allows access to the tools via a web browser. This functions both as
  a GUI and as a way to extract results from other programming languages via
  JSON and a HTTP query.
* Simple amplitudes. Also new is the ability to calculate an amplitude
  approximation from reflection and transmission coefficients and spreading.
