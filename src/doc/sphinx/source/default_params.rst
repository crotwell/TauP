.. _default_params:

------------------
Default Parameters
------------------

.. _properties:

Each of the tools use Java Properties to allow the user to specify values
for various
parameters. The properties all have default values, which are overridden by
values from a Properties file. The tools use a text file to load the properties. It reads  :code:`.taup` in the
current directory, which overwrites values read in from
:code:`.taup` in the user's home directory. Properties may also be specified by
the :code:`--prop` command line argument.
In addition, many of the properties can be overridden by command line arguments.

The form of the properties file is very simple. Each property is set using
the form
:code:`taup.property.name=value` one property per line.
Comment lines are allowed, and begin with a :code:`#`.
Additionally, the names of all of the properties follow a convention of
prepending :code:`taup.` to the name of the property.
This helps to avoid name collisions when new properties
are added.

The currently used properties are:


* :code:`taup.model.name`
  the name of the initial model to be loaded,
  iasp91 by default.
* :code:`taup.model.path`
  search path for models. There is no default,
  but the value
  in the .taup file will be concatinated with any value of taup.model.path
  from the system properties. For example, the environment variable TAUPPATH
  is put into the system property taup.model.path by the wrapper shell scripts.
* :code:`taup.source.depth`
  initial depth of the source, 0.0 km by default.
* :code:`taup.phase.list`
  initial phase list, combined with taup.phase.file. The
  defaults are p, s, P, S, Pn, Sn, PcP, ScS, Pdiff, Sdiff, PKP, SKS, PKiKP,
  SKiKS, PKIKP, SKIKS.
* :code:`taup.phase.file`
  initial phase list, combined with taup.phase.list. There
  is no default value, but the default value for taup.phase.list will not be
  used if there is a taup.phase.file property.
* :code:`taup.depth.precision`
  precision for depth output, the default is 1 decimal digit.
  Note that this is precision, not accuracy. Just
  because you get more digits doesn't imply that they have any meaning.
* :code:`taup.distance.precision`
  precision for distance output,
  the default is 2 decimal digits.
  Note that this
  is precision, not accuracy. Just because you get more
  digits doesn't imply that they have any meaning.
* :code:`taup.latlon.precision`
  precision for latitude and longitude output, the
  default is 2 decimal digits.
  Note that this is precision, not accuracy. Just because you get more
  digits doesn't imply that they have any meaning.
* :code:`taup.time.precision`
  precision for time, the default is 2 decimal digits.
  Note that this is precision, not accuracy. Just because you get more
  digits doesn't imply that they have any meaning.
* :code:`taup.rayparam.precision`
  precision for ray parameter, the default is 3 decimal digits.
  Note that this is precision, not accuracy. Just because you get more
  digits doesn't imply that they have any meaning.
* :code:`taup.maxRefraction`
  The maximum degrees that a Pn or Sn can refract along the moho. Note this
  is not the total distance, only the segment along the moho. The default is 20 degrees.
* :code:`taup.maxDiffraction`
  The maximum degrees that a Pdiff or Sdiff can diffract along the CMB.
  Note this is not the total distance, only the segment along the CMB. The default is 60 degrees.
* :code:`taup.maxKmpsLaps`
  The maximum number of laps around the earth for
  kpms style phases.
  Note this is the number of laps, not number of arrivals, so a value of 1
  would give 2 arrivals, one going the short path and one the long way around. This can be fractional, so 0.5 would exclude the long way around
  path. The default is 1.
* :code:`taup.path.maxPathInc`
  maximum distance in degrees between points of a path. This does a simple linear interpolant between nearby values in order to make plots look better. There is noo improvement in the accuracy of the path.
* :code:`taup.table.locsat.maxdiff`
  maximum distance in degrees for which Pdiff
  or Sdiff are put into a locsat table. Beyond this distance Pdiff and Sdiff will
  not be added to the table, even though they may show up in the output of
  TauP Time. Instead, the next later arriving phase, if any, will be used
  instead. The default is 105 degrees.
* :code:`taup.table.locsat.depth.precision`
  precison for depth for locsat output, defaults to 2 decimal digits.
* :code:`taup.table.locsat.distance.precision`
  precison for distance for locsat output, defaults to 2 decimal digits.
* :code:`taup.table.locsat.time.precision`
  precison for time for locsat output, defaults to 4 decimal digits.
* :code:`taup.create.minDeltaP`
  Minimum difference in slowness between
  successive slowness samples. This is used to decide when to stop adding new
  samples due to the distance check.
  Used by TauP Create to create new models.
  The default is 0.1 sec/rad.
* :code:`taup.create.maxDeltaP`
  Maximum difference in slowness between
  successive slowness samples. This is used to split any layers that exceed
  this slowness gap.
  Used by TauP Create to create new models.
  The default is 11.0 sec/rad.
* :code:`taup.create.maxDepthInterval`
  Maximum difference between successive depth
  samples. This is used immediately after reading in a velocity model, with
  layers being split as needed.
  Used by TauP Create to create new models.
  The default is 115 km.
* :code:`taup.create.maxRangeInterval`
  Maximum difference between successive
  ranges, in degrees. If the difference in distance for two adjacent rays
  is greater than this, then a new slowness sample is inserted halfway between
  the two existing slowness samples.
  The default is 2.5 degrees.
* :code:`taup.create.maxInterpError`
  Maximum error for linear interpolation
  between successive sample in seconds. TauP Create uses this to try to insure
  that the maximum error due to linear interpolation is less than this amount.
  Of course, this is only an approximation based upon an estimate of the
  curvature of the travel time curve for surface focus turning waves.
  In particular, the error for more complicated phases is greater. For instance,
  if the true error for P at 30 degrees is 0.03 seconds, then the error for
  PP at 60 degrees would be twice that, 0.06 seconds.
  Used by TauP Create to create new models. The default is 0.05 seconds.
* :code:`taup.create.allowInnerCoreS`
  Should we allow J phases, S in the inner core?
  Used by TauP Create to create new models.
  The default is true. Setting it to false slightly reduces storage and model
  load time.

Phase files, specified with the :code:`taup.phase.file` property,
are just text files with phase names, separated by either
spaces, commas or newlines. In section :ref:`phasenaming` the details of
the phase naming convention are introduced.
By and large, it is compatible with traditional
seismological naming conventions, with a few additions and exceptions.
Also, for compatiblity with *ttimes*, you may specify
:code:`ttp`, :code:`ttp+`, :code:`tts`, :code:`tts+`,
:code:`ttbasic` or :code:`ttall` to get a phase list corresponding
to the *ttimes* options.
