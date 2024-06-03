.. _taup_setmseed3:

--------------
TauP SetMSeed3
--------------

TauP setmseed3 saves travel times into the extra headers of miniseed3 records.
The source depth and distance used for the calculations can be provided
on the command line or can be extracted from
`bag style json <https://crotwell.github.io/ms3eh/>` in the records.

In the simplest case, with miniseed3 records that contain *bag* style
extra headers with earthquake depth and station distance, travel times
can be calculated by just giving the phases of interest. Alternatively,
a depth and distance can be given, or earthquake and station information
can be extracted from QuakeML and StationXML files.

The usage is:

.. literalinclude:: cmdLineHelp/taup_setmseed3.usage
  :language: text
