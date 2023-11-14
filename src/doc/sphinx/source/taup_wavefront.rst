
--------------
TauP_Wavefront
--------------

TauP\_Wavefront is similar to TauP\_Path, but plots the wavefront at timesteps instead of the
ray paths. It also uses a model like TauP\_Time and
generates  depth versus
angular distances from the epicenter for the phases, but done at time slices instaed of depth slices.
The output is in GMT~\cite{GMT} ``psxy'' format, and is
placed into the file ``taup\_wavefront.gmt''.
The colums are distance, depth, time and ray param, although only the first two are used by the GMT script.
If you specify the ``-gmt'' flag then this
is a complete script with the appropriate ``psxy'' command prepended, so if you
have GMT installed, you can just:

\begin{verbatim}
taup wavefront -mod iasp91 -h 550 -ph S,ScS,sS,sScS --gmt
sh taup_wavefront.gmt
\end{verbatim}

and you have a plot of the wavefronts in ``taup\_wavefront.pdf''.

The usage is:
\begin{verbatim}
piglet 5>taup wavefront --help
Usage: taup wavefront [arguments]
  or, for purists, java edu.sc.seis.TauP.TauP_Wavefront [arguments]

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


--rays  num      -- number of raypaths/distances to sample.
--timestep  num  -- steps in time (seconds) for output.
--timefiles      -- outputs each time into a separate .ps file within the gmt script.
--negdist        -- outputs negative distance as well so wavefronts are in both halves.

-o [stdout|outfile]         -- output is redirected to stdout or to the "outfile" file
--prop [propfile]   -- set configuration properties
--debug             -- enable debugging output
--verbose           -- enable verbose output
--version           -- print the version
--help              -- print this out, but you already know that!
\end{verbatim}
