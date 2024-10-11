
===================
Calculations
===================

Distance
--------

The TauP Toolkit, and the underlying methodology, are inheirently spherical. But
of course the earth is not quite a sphere, and so there are differences
between the calculated times in a model and the actual travel times within
the earth.

An additional source of error can be the calculation of distances
from source and receiver latitude and longitude. The angular distance
between two latitude and longitude points can be based on the surface distance
or the angle from the center of the earth. In a sphere, these are equivalent,
but on an ellipsoid they are not. It is generally more accurate to base the
traveltime calculations based on the geocentric angle instead of the surface
arc distance. This discrepency arrised from the difference in definition of
the `geodetic and geocenticlatitude <https://en.wikipedia.org/wiki/Geodetic_coordinates#Geodetic_vs._geocentric_coordinates>`_.
Geodetic, defined as the angle of the surface normal plane and the equatorial
plane. Geocentric is angle between the radius to the point and the equator.
For example, the geocentric angle between
an earthquake at latitude, longitude (0,30)
to a station at (0/0) is 30 degrees, which is slightly larger than that between
an earthquake at (30,0) to the same station, 29.83 degrees. Because TauP is
spherical, the default is to not take the elliptical nature of the earth
into account when calculating these distances. But this can be changed with
the :code:`--geodetic` parameter, which implies that the given latitudes
are geodetic instead of geocentric.
And for use with models of other planets,
the default flattening can be changed with the :code:`--geodeticflattening`.

Amplitude
---------

TauP can calculate an amplitude factor estimate for some simple phases, as long
as the phase path is simple turning or reflection, but not for head or diffracted
phases. The amplitude factor, given for both the P-Sv and Sh systems is the
product of multiple factors. For details, see :cite:t:`fmgs`
`chapter 13 <https://doi.org/10.1016/C2017-0-03756-4>`_.

The factors that contribute to this estimate are:

* A nominal source term for a Mw 4.0 earthquake, but without the
  source orientation, so should be thought of as a bound, rather than an actual
  amplitude.

* A radiation term, based on the density and velocity at the source depth.

* Geometrical spreading factor.

* The product of energy reflection and transmission coefficients for each
  boundary encountered along the path.

* Attenuation for a 1 Hz wave.

* The free surface receiver function value, if the receiver depth is zero.

Amplitudes for seismic waves are notoriously difficult to calulate without error,
and so the values given should be taken with a healthy dose of skepticism.
