![Alt TauP](https://crotwell.github.io/TauP/taupLogo.svg)
[![Maven Central](https://img.shields.io/maven-central/v/edu.sc.seis/TauP.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22edu.sc.seis%22%20AND%20a:%22TauP%22)
 [![javadoc](https://javadoc.io/badge2/edu.sc.seis/TauP/javadoc.svg)](https://javadoc.io/doc/edu.sc.seis/TauP)
[![taup](https://snapcraft.io/taup/badge.svg)](https://snapcraft.io/taup)



                           The TauP Toolkit

            Flexible Seismic Travel-Time and Raypath Utilities
                              Version 2.6
                      http://www.seis.sc.edu/taup/

The TauP Toolkit is a seismic travel time calculator. In addition to travel
times, it can calculate derivative information such as ray paths through the
earth, pierce and turning points. It handles many types of velocity models and
can calculate times for virtually any seismic phase with a phase parser.
It is written in Java so it should run on any Java enabled platform.

The manual for the TauP Toolkit can be found in doc/taup.pdf. A copy of the TauP paper published in SRL can be found in doc/SRL_paper/taup_srl_with_figs.pdf.

Scripts for the various tool for both unix (sh) and windows (bat) are in the bin directory.

The website for the TauP Toolkit is:
http://www.seis.sc.edu/TauP

If you like TauP and would like to cite it, please use the following:
Crotwell, H. P., T. J. Owens, and J. Ritsema (1999). The TauP Toolkit: Flexible seismic travel-time and ray-path utilities, Seismological Research Letters 70, 154–160.

Comments, criticism and random remarks may be sent to taup@seis.sc.edu.

### Install

## Macintosh

Using [homebrew](https://brew.sh/):
```
brew tap crotwell/crotwell
brew install taup
taup --help
```

## Linux

Using [snap](https://snapcraft.io):
```
sudo snap install taup
taup --help
```

Note: I have only tested this on Ubuntu on amd64.

You may also get the lastest development version via
```
sudo snap install taup --channel=--channel=latest/edge
```

## Manually

Download tarball here:
https://www.seis.sc.edu/downloads/TauP/TauP-2.6.0.tgz
or from the releases section on Github
https://github.com/crotwell/TauP/releases

`tar zxf` and add the bin to your PATH.


### Rebuilding

You should not need to rebuild TauP unless you are trying to help debug, but if you do, you can build it using the Gradle wrapper script.

gradlew eB

will build TauP in build/explode.
