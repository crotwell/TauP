
-----------------
TauP_VelocityPlot
-----------------

TauP\_VelocityPlot outputs a gmt script that plots the given velocity model.

The usage is:
\begin{verbatim}
piglet 1>taup velplot --help
Usage: taup velplot [arguments]
  or, for purists, java edu.sc.seis.TauP.TauP_VelocityPlot [arguments]

Arguments are:
-mod[el] modelname -- use velocity model "modelname" for calculations
                      Default is iasp91.


-nd modelfile       -- "named discontinuities" velocity file
-tvel modelfile     -- ".tvel" velocity file, ala ttimes


-o [stdout|outfile]         -- output is redirected to stdout or to the "outfile" file
--prop [propfile]   -- set configuration properties
--debug             -- enable debugging output
--verbose           -- enable verbose output
--version           -- print the version
--help              -- print this out, but you already know that!
\end{verbatim}
