![Alt TauP](https://crotwell.github.io/TauP/taupLogo.svg)
[![Maven Central](https://img.shields.io/maven-central/v/edu.sc.seis/TauP.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22edu.sc.seis%22%20AND%20a:%22TauP%22)
 [![javadoc](https://javadoc.io/badge2/edu.sc.seis/TauP/javadoc.svg)](https://javadoc.io/doc/edu.sc.seis/TauP)
[![taup](https://snapcraft.io/taup/badge.svg)](https://snapcraft.io/taup)
[![Tests](https://github.com/crotwell/TauP/actions/workflows/gradle.yml/badge.svg)](https://github.com/crotwell/TauP/actions/workflows/gradle.yml)
[![Documentation Status](https://readthedocs.org/projects/taup/badge/?version=latest)](https://taup.readthedocs.io/en/latest/?badge=latest)




                           The TauP Toolkit

            Flexible Seismic Travel-Time and Raypath Utilities
                              Version 3.0
                      http://www.seis.sc.edu/taup/

The TauP Toolkit is a seismic travel time calculator. In addition to travel
times, it can calculate derivative information such as ray paths through the
earth, pierce and turning points. It handles many types of velocity models and
can calculate times for virtually any seismic phase with a phase parser.
It is written in Java so it should run on any Java enabled platform.

The website for the TauP Toolkit is:
https://github.com/crotwell/TauP

Documentation is available at [Read The Docs](https://taup.readthedocs.io/en/latest/), or in docs/manual directory of the distribution.

If you like TauP and would like to cite it, please use the following:
Crotwell, H. P., T. J. Owens, and J. Ritsema (1999). The TauP Toolkit: Flexible seismic travel-time and ray-path utilities, Seismological Research Letters 70, 154–160, as well as the [Zenodo DOI](https://zenodo.org/doi/10.5281/zenodo.10794857) for the version you are using..

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

Download tarball from Zenodo here:
https://zenodo.org/records/10794858
or from the releases section on Github
https://github.com/crotwell/TauP/releases


### Rebuilding

You should not need to rebuild TauP unless you are trying to help debug, but if you do, you can build it using the Gradle wrapper script.

```
./gradlew installDist
```

will build TauP into the build/install directory.
