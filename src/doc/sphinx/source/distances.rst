.. _distances:

====================
Rays and Distances
====================

For many of the tools within the TauP Toolkit, a large part
of the work is determining the ray through the model that
answers the question. But depending on the question there are
many ways to specify the ray. For example, what time does
:code:`P` arrive at 35 degrees? Or where does a ray that leaves
the source at a 25 degree takeoff angle arrive?

Traditionally, we are interested in arrivals at a known distance. So
Using the :code:`--deg`: option gives that distance in degrees while
the :code:`--km` gives it in kilometers. Multiple of either of these
can be given, like :code:`--deg 10,30,60`.
But if we want a regular sampling, we can give
:code:`--degreerange 5` to get every 5 degrees or
:code:`--degreerange 30,60` to get every 10 degrees from 30 to 60, or even
:code:`--degreerange 30,60,5` to get every 5 degrees from 30 to 60.
The :code:`--kilometerrange` argument provides the same functionality,
except for kilometers along the surface, with defaul step of 100 and
default maximum of 1000.

Some times it is helpful to generate only arrivals at an exact distance
and to disregard those that arrive the other way around, or that lap
around the earth multiple times. For example a phase like SKKKS that can travel
a path of more than once around, we may want to get
the arrival at 200 degrees but not the one at 160 degrees. The
argument :code:`--exactdegree 200` does this. Similar functionality exists
for kilometers with :code:`--exactkilometer`.

If we know the event and station locations, we can use the :code:`--evt` and
:code:`--sta` to give the latitude and longitudes and TauP can calculate the
distance. The :code:`--geodetic` argument does the distance calculation using
an ellipticity, the default is purely spherical. The internal time calculations
are always spherical. We can also read station locations from a StationXML
file with :code:`--staxml` and event locations from a QuakeML file with
:code:`--qml`. Even more fun is to give a station id, like CO_HAW or a
USGS event id, like us7000pn9s, and TauP will use the IRIS FedCat and
USGS FDSN Event web services to get the station and event locations.
TauP will do the calculations for all event station pairs.

There are also times when we do not know the final distance, but are interested
in rays that start or end in a particular way. For example we can shoot
rays of specific ray parameters with :code:`--rayparamdeg`, :code:`--rayparamkm`
and :code:`--rayparamrad` for ray parameters of seconds per degree, kilometer
or radian. Or we can give a takeoff angle with :code:`--takeoff` and a range
of takeoff angles with :code:`--takeoffrange` similar to the distance range
arguments. And we can give an incident angle with :code:`--incident` and a range
of incident angles with :code:`--incidentrange`.

As long as we are using spherical calulcations, not :code:`--geodetic`, then
if we give event location via :code:`--evt` and azimuth via :code:`--az`, then
the resulting station location will be calculated. The reverse, giving
station location via :code:`--sta` and back azimuth via :code:`--baz` will
calculate the event location.

For debugging purposes, the :code:`--allindex` and :code:`--rayparamidx`
will show the calculation at the model sampling, for all or one ray by
index.
