
------------
TauP_Pierce
------------

TauP\_Pierce uses a model like TauP\_Time but
determines the
angular distances from the epicenter at which the specified rays pierce
discontinuities or specified depths in the model.

The usage is:
\begin{verbatim}
piglet 3>taup pierce --help
Usage: taup pierce [arguments]
  or, for purists, java edu.sc.seis.TauP.TauP_Pierce [arguments]

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


-az azimuth        -- sets the azimuth (event to station)
                      used to output lat and lon of pierce points
                      if the event lat lon and distance are also
                      given. Calculated if station and event
                      lat and lon are given.
-baz backazimuth   -- sets the back azimuth (station to event)
                      used to output lat and lon of pierce points
                      if the station lat lon and distance are also
                      given. Calculated if station and event
                      lat and lon are given.

--first            -- only output the first arrival for each phase, no triplications
-rev               -- only prints underside and bottom turn points, e.g. ^ and v
-turn              -- only prints bottom turning points, e.g. v
-under             -- only prints underside reflection points, e.g. ^

-pierce depth      -- adds depth for calculating pierce points
-nodiscon          -- only prints pierce points for the depths added with -pierce


-o [stdout|outfile]         -- output is redirected to stdout or to the "outfile" file
--prop [propfile]   -- set configuration properties
--debug             -- enable debugging output
--verbose           -- enable verbose output
--version           -- print the version
--help              -- print this out, but you already know that!
\end{verbatim}

The :code:`-rev`, :code:`-turn` and :code:`-under` flags are useful
for limiting the output
to just those points you care about. The :code:`-pierce depth` option
allows you
to specify a ``pierce'' depth that does not correspond to an
actual discontinuity. For instance, where does a ray pierce 300 kilometers
above the CMB?

For example:

:code:`taup pierce -mod prem -h 200 -ph S,P -deg 57.4`

.. literalinclude:: examples/taup_pierce_-turn_-mod_prem_-h_200_-ph_S_P_-deg_57.4.cmd

would give you pierce points for S, and P for a 200 kilometer
deep source at a distance of 57.4 degrees.

While

:code:`taup pierce -turn -mod prem -h 200 -ph S,P -deg 57.4`

would give you just the points that each ray turns from downgoing to upgoing.

Using :code:`-rev` would give you all points that the ray changes direction and :code:`-under` gives just the underside reflections.

Using the :code:`-pierce` option

:code:`taup pierce -mod prem -h 200 -ph S -sta 12 34.2 -evt -28 122 --pierce 2591 --nodiscon`

would give you just the points at which S crossed a depth of 2591 kilometers
from an event at ($28^\circ$ S, $122^\circ$ E)
to a station at ($12^\circ$ N, $34.2^\circ$ E).
Because we specified the latitudes and longitudes, we also get the
latitudes and longitudes of
the pierce points, useful for making
a map view of where the rays encounter the chosen depth. Here is the output,
distance, depth, latitude and longitude, respectively.
\begin{verbatim}
  > S at  1424.11 seconds at    93.70 degrees for a    200.0 km deep source in the prem model with rayParam    8.717 s/deg.
     31.56  2591.0   552.6    -17.87     89.41
     61.47  2591.0   822.0     -3.89     62.40
\end{verbatim}
