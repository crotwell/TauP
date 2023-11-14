
----------
TauP_Path
----------

TauP\_Path uses a model like TauP\_Time but
generates  the
angular distances from the epicenter at which the specified rays pierce
path that the phases travel. The output is in GMT~\cite{GMT} ``psxy'' format, and is
placed into the file ``taup\_path.gmt''.
If you specify the ``-gmt'' flag then this
is a complete script with the appropriate ``psxy'' command prepended, so if you
have GMT installed, you can just:

\begin{verbatim}
taup path -mod iasp91 -h 550 -deg 74 -ph S,ScS,sS,sScS --gmt
sh taup_path.gmt
\end{verbatim}

and you have a plot of the ray paths in ``taup\_path.pdf''. To avoid possible plotting errors for
phases like :code:`Sdiff`, the ray paths are interpolated to less than
1 degree increments.

The usage is:
\begin{verbatim}
piglet 5>taup path --help
Usage: taup path [arguments]
  or, for purists, java edu.sc.seis.TauP.TauP_Path [arguments]

Arguments are:
-ph phase list     -- comma separated phase list
-pf phasefile      -- file containing phases

-mod[el] modelname -- use velocity model "modelname" for calculations
                      Default is iasp91.

-h depth           -- source depth in km

--stadepth depth   -- receiver depth in km

Distance is given by:

-deg degrees       -- distance in degrees,
-km kilometers     -- distance in kilometers,
                      assumes radius of earth is 6371km,

or by giving the station and event latitude and lonitude,
                      assumes a spherical earth,

-sta[tion] lat lon -- sets the station latitude and longitude
-evt       lat lon -- sets the event latitude and longitude


--first            -- only output the first arrival for each phase, no triplications
--withtime        -- include time for each path point
--gmt             -- outputs path as a complete GMT script.
--svg             -- outputs path as a complete SVG file.
--mapwidth        -- sets map width for GMT script.

-o [stdout|outfile]         -- output is redirected to stdout or to the "outfile" file
--prop [propfile]   -- set configuration properties
--debug             -- enable debugging output
--verbose           -- enable verbose output
--version           -- print the version
--help              -- print this out, but you already know that!
\end{verbatim}
