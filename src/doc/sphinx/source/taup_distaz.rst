.. _taup_distaz:

---------
TauP Distaz
---------

TauP Distaz calculates great circle path distances between lat/lon points.

For example:

.. literalinclude:: examples/taup_distaz_--sta_31_-80_--evt_-11_21.cmd
  :language: text

gives you spherical distance in degrees, azimuth and back azimuth between
latitude longitude points -11/21 and 31/-80.


.. literalinclude:: examples/taup_distaz_--sta_31_-80_--evt_-11_21
  :language: text

If you have a QuakeML file for earthquakes or a StationXML file for stations
and channels, you can use those to calculate the distances directly. It is
often useful to use either :code:`--geodist geodetic` or
:code:`--geodist geocentric` when using latitudes and longitudes
so that the distance calculation is more accurate. For
example:

.. literalinclude:: examples/taup_distaz_--quakeml_my_midatlantic.qml_--staxml_my_stations.staml_--geodist_geodetic.cmd
  :language: text

gives you distances from these earthquakes to those stations.

.. literalinclude:: examples/taup_distaz_--quakeml_my_midatlantic.qml_--staxml_my_stations.staml_--geodist_geodetic
  :language: text

It can also calculate from event, distance and azimuth.

.. literalinclude:: examples/taup_distaz_--sta_-11_21_--baz_-135_--deg_30_--geodist_geodetic.cmd
  :language: text

.. literalinclude:: examples/taup_distaz_--sta_-11_21_--baz_-135_--deg_30_--geodist_geodetic
  :language: text

Or from station, distance and back azimuth.

.. literalinclude:: examples/taup_distaz_--evt_-11_21_--az_35_--km_3000_--geodist_geodetic.cmd
  :language: text

.. literalinclude:: examples/taup_distaz_--evt_-11_21_--az_35_--km_3000_--geodist_geodetic
  :language: text


The usage is:

.. literalinclude:: cmdLineHelp/taup_distaz.usage
  :language: text
