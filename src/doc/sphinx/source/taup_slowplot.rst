
-----------------
TauP_SlownessPlot
-----------------

TauP\_SlownessPlot outputs a gmt script that plots the given slowness model.

The usage is:
\begin{verbatim}
piglet 1>taup slowplot --help
Usage: taup_slownessplot [arguments]
  or, for purists, java edu.sc.seis.TauP.TauP_SlownessPlot [arguments]

Arguments are:

   To specify the velocity model:
-nd modelfile       -- "named discontinuities" velocity file
-tvel modelfile     -- ".tvel" velocity file, ala ttimes

--debug              -- enable debugging output
--prop [propfile]    -- set configuration properties
--verbose            -- enable verbose output
--version            -- print the version
--help               -- print this out, but you already know that!
\end{verbatim}
