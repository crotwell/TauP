
------------
TauP Pierce
------------

TauP Pierce uses a model like TauP Time but
determines the
angular distances from the epicenter at which the specified rays pierce
discontinuities or specified depths in the model.

The usage is:

.. literalinclude:: cmdLineHelp/taup_pierce.usage
  :language: text



The :code:`--rev`, :code:`--turn` and :code:`--under` flags are useful
for limiting the output
to just those points you care about. The :code:`--pierce depth` option
allows you
to specify a `pierce` depth that does not correspond to an
actual discontinuity. For instance, where does a ray pierce 300 kilometers
above the CMB?

For example:

.. literalinclude:: examples/taup_pierce_--mod_prem_-h_200_-p_S_P_--deg_57.4.cmd
  :language: text

would give you pierce points for S, and P for a 200 kilometer
deep source at a distance of 57.4 degrees.

While

.. literalinclude:: examples/taup_pierce_--turn_--mod_prem_-h_200_-p_S_P_--deg_57.4.cmd
  :language: text

would give you just the points that each ray turns from downgoing to upgoing.

Using :code:`-rev` would give you all points that the ray changes direction
and :code:`-under` gives just the underside reflections.

Using the :code:`-pierce` option

.. literalinclude:: examples/taup_pierce_--mod_prem_-h_200_-p_S_--sta_12_34.2_--evt_-28_122_--pierce_2591_--nodiscon.cmd
  :language: text

would give you just the points at which S crossed a depth of 2591 kilometers
from an event at (28 S, 122 E)
to a station at (12 N, 34.2 E).
Because we specified the latitudes and longitudes, we also get the
latitudes and longitudes of
the pierce points, useful for making
a map view of where the rays encounter the chosen depth. Here is the output,
distance, depth, latitude and longitude, respectively.

.. literalinclude:: examples/taup_pierce_--mod_prem_-h_200_-p_S_--sta_12_34.2_--evt_-28_122_--pierce_2591_--nodiscon
  :language: text
