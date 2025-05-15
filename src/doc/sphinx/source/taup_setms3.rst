.. _taup_setms3:

--------------
TauP SetMS3
--------------

TauP setms3 saves travel times into the extra headers of miniseed3 records.
The source depth and distance used for the calculations can be provided
on the command line or can be extracted from
`bag style json <https://crotwell.github.io/ms3eh/>`_ in extra headers
in the records.

In the simplest case, with miniseed3 records that contain *bag* style
extra headers with earthquake depth and station distance, travel times
can be calculated by just giving the phases of interest. Alternatively,
a depth and distance can be given, or earthquake and station information
can be extracted from QuakeML and StationXML files.

For example, if the records in my_earthquake.ms3 have bag style extra
headers, then this will add markers to the extra headers for P and S:

.. literalinclude:: examples/taup_setms3_-p_P_S_my_earthquake.ms3.cmd
  :language: text

The usage is:

.. literalinclude:: cmdLineHelp/taup_setms3.usage
  :language: text
