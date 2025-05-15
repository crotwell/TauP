
============
Installation
============

Macintosh
---------

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

Download `tarball here <https://www.seis.sc.edu/downloads/TauP/TauP-3.0.0.tgz>`_
or from the `releases <https://github.com/crotwell/TauP/releases>`_
page on Github. Then::

  tar zxf TauP-3.0.0.tgz

and add the bin directory to your PATH.


Rebuilding
-----------

You should not need to rebuild TauP from source unless you are trying
to help debug an issue, but if
you do, you can build it using the Gradle wrapper script::

  ./gradlew installDist

will rebuild TauP into the build/install directory.



Tab completion
--------------

TauP is distributed with a command line completion file for the Bash and ZSH
Unix shells, named taup_completion. By sourcing this file either in your
.bashrc or .zshrc file you can use <TAB><TAB> to autocomplete command line
arguments. The TauP tool also can output this completion file as needed.

For Bash add this to your .bashrc file in your home directory, and
for ZSH add to you .zshrc file::

  source <(/path/to/taup generate-completion)

This runs the taup command to generate the completion file on the fly, making
sure it is the latest version.
