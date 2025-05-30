
============
Installation
============

Macintosh
---------------------------------------------------

Homebrew originally was just for Macintosh, but also supports
Linux and Windows via Windows Subsystem for Linux.

Using `homebrew <https://brew.sh/>`_::

  brew tap crotwell/crotwell
  brew install taup
  taup --help


Linux
-----

Using `snap <https://snapcraft.io>`_::

  sudo snap install taup
  taup --help


Note: I have only tested this on Ubuntu on amd64.

You may also get the lastest development version via::

  sudo snap install taup --channel=latest/edge


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
