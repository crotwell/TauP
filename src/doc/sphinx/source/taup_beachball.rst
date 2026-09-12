.. _taup_beachball:

----------
TauP Beachball
----------

TauP Beachball creates a focal mechanism "beachball" plots of
seismic sources, optionally overlain by takeoff angles for seismic phases,
and arrivals at their takeoff and azimuth.

Note that only phases starting with a P leg will plot on the P beachball, and only phases starting with
an S leg will plot on the Sv, Sh and S beachball. The S beachball is plotted
as the vector magnitude of the combination of Sh and Sv waves, which means that
it is always positive and so does not show the quadrant effect as do the
other three types.

For example, this command plots a beachball, phase distance circles and
a few arrivals.


.. literalinclude:: examples/taup_beachball_--bbtype_ampp_--phasecircles_--degree_210_--az_222_--evdepth_607_--model_prem_--phase_PKP_PKIKP_SKS_--strikediprake_17_7_-62_--svg.cmd
  :language: text

and results in

.. raw:: html
    :file:  examples/taup_beachball_--bbtype_ampp_--phasecircles_--degree_210_--az_222_--evdepth_607_--model_prem_--phase_PKP_PKIKP_SKS_--strikediprake_17_7_-62_--svg

The :code:`--arrows` draws the magnitude of the motion as an arrow on the plot.
For the P wave beachball, the sense of the arrow is perpendicular to the sphere,
so arrow towards the center represent dillitational P wave motion, while
arrows away from the center represent compressional motion. For the three
S wave plots, the sense of the arrow is tangential to the surface of the sphere.
So for Sv motion, an arrow away from the center represents a tangential motion with
both an upward component and a horizontal away from the source component.
For Sh motion it is horizontal vector tangent to the sphere. And for S it is
the vector sum of Sv and Sh.

The usage is:

.. literalinclude:: cmdLineHelp/taup_beachball.usage
  :language: text
