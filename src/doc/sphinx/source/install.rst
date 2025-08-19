
============
Installation
============

Homebrew
---------------------------------------------------

Homebrew originally was just for Macintosh, but also supports
Linux and Windows via Windows Subsystem for Linux.

Using `homebrew <https://brew.sh/>`_::

  brew tap crotwell/crotwell
  brew install taup
  taup --help


Manually
--------

Download tarball from |zenodo_url|_. Then::

unzip |dist_zip|

and add the bin directory to your PATH.


Rebuilding
-----------

You should not need to rebuild TauP from source unless you are trying
to help debug an issue, but if
you do, you can build it using the Gradle wrapper script::

  ./gradlew installDist

will rebuild TauP into the build/install directory.


Tab Completion
--------------

New with version 3.0 is tab completion for bash and zsh. Sourcing the output of
:code:`taup generate-completion` will provide hints
for the shell when hitting the tab key for bash or zsh. You can enable it
with running this command:

:code:`source <(taup generate-completion)`

Adding this to your .bash_profile or .zshrc will enable it for future logins.

Note, for bash 3.2, which the default verion on OSX, there is a bug that
prevents this from working. The alterantive is to save it as a file like:

:code:`taup generate-completion > taup_completion`

and then source the file:

:code:`source taup_completion`

Once sourced, you will be able to get hints or completion for most arguments
within TauP. For example typing:

:code:`taup time -`

and then hitting the tab key will display all of the possible command line
flags. Continuing to type

:code:`taup time --mod`

and then hitting the tab key will display the models available for the
:code:`--mod` command line argument:

:code:`ak135        ak135favg    ak135fcont   ak135fsyngine  iasp91       prem`


What and Where
--------------

The current distribution of the TauP package is |release|, created |today|.

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
| gradlew*        | rebuild TauP using Gradle. Not needed unless recompiling for   |
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


Changes from Version 2
----------------------

Version 3 of the The TauP Toolkit is a major revision with breaking changes,
so users should not expect the exact command from version 2 to work identically
in version 3. However, in most cases the differences are minor.

The difference most likely to break is the number of dashes for arguments. In
version 2, TauP would accept one or two dashes for most argmunents, so

:code:`taup time -ph P -deg 35`

and

:code:`taup time --ph P --deg 35`

were equivalent. In version 3 TauP uses
`picocli <https://picocli.info/>`_
to handle all the command line argument parsing. This brings significant
benefits. In migrating to picocli, we have adopted the
`GNU convention <https://www.gnu.org/prep/standards/html_node/Command_002dLine-Interfaces.html>`_
that command line arguments that are a single character have one dash while
longer arguments require two dashes. Also, a few arguments that seemed confusing
have been renamed, for example using `-p` instead of `-ph` to give phases.
Thus, only this style command works in version 3:

:code:`taup time -p P --deg 35`

We have also made changes to the output in some cases. The basic textual output
from :code:`taup time` is the same, with the addition of a column for the
receiver/station depth. The JSON output format has been updated. Before
the output included a single source and receiver depth, but TauP can handle
multiple source depths in a single call, these are now lists called
`sourcedepthlist` and `receiverdepthlist` and each individual arrival
contains a `sourcedepth` and `receiverdepth`.

For users that cannot easily update their scripts, version 2 can still be used,
via manual install or from homebrew (as taup2), but no further development
will occur for it.
