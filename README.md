

                           The TauP Toolkit

            Flexible Seismic Travel-Time and Raypath Utilities
                              Version 2.0
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

We use gradle to compile TauP. Should you wish to recompile, the gradlew wrapper will download gradle and compile. For example 'gradle/gradlew jar' will compile the java source and create a new jar. 'gradle eclipse' will generate an eclipse project files for importing. More information can be found at http://www.gradle.org.

If you like TauP and would like to cite it, please use the following:
Crotwell, H. P., T. J. Owens, and J. Ritsema (1999). The TauP Toolkit: Flexible seismic travel-time and ray-path utilities, Seismological Research Letters 70, 154–160.

Comments, criticism and random remarks may be sent to taup@seis.sc.edu.


### Rebuilding

You should not need to rebuild TauP unless you are trying to help debug, but if you do, you can build it using the Gradle wrapper script.

gradlew eB

will build TauP in build/explode.
