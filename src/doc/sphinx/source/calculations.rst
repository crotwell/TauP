
===================
Calculations
===================

Spherical vs Elliptical
-----------------------

The TauP Toolkit, and the underlying methodology, are inherently spherical. But
of course the earth is not quite a sphere, and so there are differences
between the calculated times in a model and the actual travel times within
the earth.

An additional source of error can be the calculation of distances
from source and receiver latitude and longitude. The angular distance
between two latitude and longitude points can be based on the surface distance
or the angle from the center of the earth. In a sphere, these are equivalent,
but on an ellipsoid they are not. It is generally more accurate to base the
traveltime calculations based on the geocentric angle instead of the surface
arc distance. This discrepancy arrised from the difference in definition of
the
`geodetic and geocentic latitude <https://en.wikipedia.org/wiki/Geodetic_coordinates#Geodetic_vs._geocentric_coordinates>`_.
Geodetic is defined as the angle between the surface normal plane and the equatorial
plane while geocentric is angle between the radius to the point and to the equator.
For example, the geocentric angle between
an earthquake on the equator, at latitude, longitude (0,30),
to a station at (0/0) is 30 degrees, which is slightly larger than that between
an earthquake at due north (30,0) of the same station, 29.83 degrees. Because TauP is
spherical, the default is to not take the elliptical nature of the earth
into account when calculating these distances. But this can be changed with
the :code:`--geodetic` parameter, which implies that the given latitudes
are geodetic instead of geocentric.
And for use with models of other planets,
the default flattening can be changed with the :code:`--geodeticflattening`
parameter.

Oceans
------

TauP is capable of handling models,
like `ak135favg <_static/StdModels/ak135favg.nd>`_, that have an ocean layer.
However, care should be taken with S waves as they cannot propigate in a fliud
layer. So the phase SS doesn't exist in ak135favg, but the phase
`S^3S` potentially does, if the source and receiver are below the ocean layer.
Also, a phase like `PP` is generally thought of as a reflection off of the
free surface of the solid earth, but in this model that phase
is a reflection from the surface of the ocean. Probably reflecting off of
the sea floor would be more accurate, using `P^3P`.
Also, the default station
depth is the model surface, but we generall don't think of seismic stations
floating on top of the ocean, so it may be more appropriate to locate the
station at depth, on the bottom of the ocean layer. Or perhaps in this case,
using `ak135fcont <_static/StdModels/ak135fcont.nd>`_
or  `ak135fsyngine <_static/StdModels/ak135fsyngine.nd>`_ is more appropriate.

Amplitude
---------

.. warning::

  Amplitudes are an experimental feature and may not generate correct
  results. They are provided in the hope that they are helpful and to
  allow feedback from the community, but testing of their correctness
  is ongoing.

TauP can calculate an amplitude factor estimate for some simple phases, as long
as the phase path is simple turning or reflection, but not for head or diffracted
phases. The amplitude factor, given for both the P-Sv and Sh systems is the
product of multiple factors. For details, see :cite:t:`fmgs`
`chapter 13 <https://doi.org/10.1016/C2017-0-03756-4>`_.

The factors that contribute to this estimate are:

* A nominal source magnitude term, default is for a Mw 4.0 earthquake.

* An optional source orientation, strike, dip and rake, default is a unity factor.

* A radiation term, based on the density and velocity at the source depth.

* Geometrical spreading factor.

* The product of energy reflection and transmission coefficients for each
  boundary encountered along the path.

* Attenuation for regular sampled frequencies, up to a maximum.

* The free surface receiver function value, if the receiver depth is less than 1 km.

Amplitudes for seismic waves are notoriously difficult to calculate without error,
and so the values given should be taken with a healthy dose of skepticism. In
addition, for large earthquakes the amplitude of body wave phases will saturate.
So a larger magnitude will not generate a larger arrival amplitude, even
though this calculation will be larger. These values may be more useful for
comparing relative amplitude between phases at a distance,
or for the same phase at multiple distances, rather than expecting the
observed amplitude on a real seismogram to match to any accuracy.


Time Errors
------------

There are several sources of error in the calculations, and while we have
attempted to reduce them, it is still useful to be aware of what can go
wrong.

Primary sources of time errors include:

1. Model relative to the Earth

  This is largely unavoidable, but may end up being the most important. The
  Earth's velocity structure is close to 1D, but does vary. A global model
  like iasp91 or prem, attempt to give a reasonable approximation to the
  global velocity structure, averaged in some sense, but arrivals from a
  particular earthquake at a particular station will feel an velocity structure
  that is not quite the same as the global average. For example,
  tomography models can have travel time differences from the global
  average by 2-3 seconds in some parts of the world.
  In addition, 3D structure within the Earth also effects the path taken,
  generating additional differences. TauP ignores this type of error, but
  the user should still be aware.

2. Velocity Model interpolation

  Velocity models are often given as velocities at a series of depth points,
  and the interpolation between these points can have subtle effects. The
  iasp91 and ak135 models are given this way, and we linearly interpolate
  velocities between these points.

3. Slowness integration

  In order to calculate travel times, TauP must integrate the traveltime across
  small layers in slowness. As slowness is the reciprocal of velocity times
  radius, a linear velocity is not a linear slowness. Moreover, the
  choice of interpolation needs to be integrable for the time and
  distance increments a ray parameter accumulates over that slowness layer
  and linear slowness is not easily integrable. We follow
  :cite:t:`bulandchapman` and use a Mohorovicic or Bullen law p=A*r^B
  which is integrable for time and distance, but is not the same as linear
  velocity.

  We can quantify the error this interpolant causes in the vertical ray case,
  as when the ray parameter is zero, we can integrate the velocity layers for
  time directly. For PcP at 0 degrees and for our default model sampling,
  we observe about 0.002 seconds of error, and for PKIKP at 180 degrees, the
  error is about 0.0056 seconds. Given the travel times of PcP and PKIKP for
  these distances are approximately 510 and 1210 seconds, these are small
  percentages, but are still somewhat close to the sample rate of modern
  seismic recordings.

  A similar effect is in other published travel times. For example the published
  travel time of PKIKP at 180 degrees for AK135 is 20 min 12.53 sec, or
  1212.53 seconds, but the direct calculation for zero ray parameter in the
  velocity model with linear interpolation gives 1212.48, or a difference
  of approximately 0.05 seconds.

4. Interpolation

  The calculation of travel times at a distance involves interpolating between
  calculated ray arrivals. We want to know travel time as a function of
  distance, but we can only calculate travel time as a function of ray
  parameter, and distance as a function of ray parameter. And so in effect
  we shoot a bunch of rays, find a pair of rays that bracket the distance of
  interest, and interpolate between those rays to find the time.

5. Ellipticity

  The calculations within The TauP Toolkit are inherently spherical, but the
  earth is elliptical. Depending on the relative location of the earthquake and
  station, this will have more or less of an effect both via the source to
  station distance and on the actual travel time calculation. TauP can
  distances using an elliptical flattening value, but does not try to
  correct for the second effect. There are external routines to
  calculate a correction to the travel time for this.

6. Other "unknown unknown" errors

  While we have tried to test the code extensively, using many test cases, it
  is still possible that some bugs remain. Buyer beware.
