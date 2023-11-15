
=====
Tools
=====


.. toctree::
    :hidden:

    taup_time
    taup_pierce
    taup_path
    taup_curve
    taup_phase
    taup_table
    taup_setsac
    taup_velplot
    taup_slowplot
    taup_velmerge
    taup_wavefront
    taup_create
    taup_gui


Tools included with the TauP package:

=======================   =========================================================================
:code:`taup time`         calculates travel times.
:code:`taup pierce`       calculates pierce points at model discontinuities and specified depths.
:code:`taup path`         calculates ray paths, depth versus epicentral distance.
:code:`taup wavefront`    calculates wavefronts in steps of time, depth versus epicentral distance.
:code:`taup gui`          a GUI that incorporates the time, pierce and path tools.
:code:`taup curve`        calculates travel time curves, time versus epicentral distance.
:code:`taup table`        outputs travel times for a range of depths and distances in an ASCII file
:code:`taup setsac`       puts theoretical arrival times into sac header variables.
:code:`taup velplot`      output velocity model as a gmt script.
:code:`taup slowplot`     output slowness model as a gmt script.
:code:`taup phase`        textual description of the path the phase takes through the model.
:code:`taup create`       creates a .taup model from a velocity model.
=======================   =========================================================================

Each tool is a Java application and has an associated wrapper to make
execution easier: sh scripts
for \textsc{Unix} and
bat files for windows.  The applications are machine independent but the
wrappers are OS specific.
For example, to invoke TauP Time under \textsc{Unix}, you could type

:code:`java -Dtaup.model.path=\$\{TAUPPATH\} edu.sc.seis.TauP.TauP Time -mod prem`

or simply use the script that does the same thing,

:code:`taup time -mod prem`

Each tool has a :code:`--help` flag that will print a usage summary, as well
as a :code:`--version` flag that will print the version.

TauP is moving towards a single application with subcommands,
but we provide individual scripts for compatibility. These
two commands produce the same result, but the first is
preferred and the second style will be removed in version 3.0.

:code:`taup time -mod prem -deg 30 -ph P`

:code:`taup\_time -mod prem -deg 30 -ph P`

------------------
Default Parameters
------------------

.. _properties

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
\begin{verbatim}
taup.property.name=value
\end{verbatim}
 one property per line.
Comment lines are allowed, and begin with a :code:`\#`.
Additionally, the names of all of the properties follow a convention of
prepending ``:code:`taup.`'' to the name of the property.
This helps to avoid name collisions when new properties
are added.

The currently used properties are:
\begin{description}

\item[taup.model.name] the name of the initial model to be loaded,
iasp91 by default.
\item[taup.model.path] search path for models. There is no default,
but the value
in the .taup file will be concatinated with any value of taup.model.path
from the system properties. For example, the environment variable TAUPPATH
is put into the system property taup.model.path by the wrapper shell scripts.
\item[taup.source.depth] initial depth of the source, 0.0 km by default.
\item[taup.phase.list] initial phase list, combined with taup.phase.file. The
defaults are p, s, P, S, Pn, Sn, PcP, ScS, Pdiff, Sdiff, PKP, SKS, PKiKP,
SKiKS, PKIKP, SKIKS.
\item[taup.phase.file] initial phase list, combined with taup.phase.list. There
is no default value, but the default value for taup.phase.list will not be
used if there is a taup.phase.file property.
\item[taup.depth.precision] precision for depth output, the default is 1 decimal digit.
 Note that this is precision, not accuracy. Just
because you get more digits doesn't imply that they have any meaning.
\item[taup.distance.precision] precision for distance output,
the default is 2 decimal digits.
Note that this
is precision, not accuracy. Just because you get more
digits doesn't imply that they have any meaning.
\item[taup.latlon.precision] precision for latitude and longitude output, the
default is 2 decimal digits.
Note that this is precision, not accuracy. Just because you get more
digits doesn't imply that they have any meaning.
\item[taup.time.precision] precision for time, the default is 2 decimal digits.
Note that this is precision, not accuracy. Just because you get more
digits doesn't imply that they have any meaning.
\item[taup.rayparam.precision] precision for ray parameter, the default is 3 decimal digits.
Note that this is precision, not accuracy. Just because you get more
 digits doesn't imply that they have any meaning.
\item[taup.maxRefraction] The maximum degrees that a Pn or Sn can refract along the moho. Note this
 is not the total distance, only the segment along the moho. The default is 20 degrees.
\item[taup.maxDiffraction] The maximum degrees that a Pdiff or Sdiff can diffract along the CMB.
 Note this is not the total distance, only the segment along the CMB. The default is 60 degrees.
 \item[taup.maxKmpsLaps] The maximum number of laps around the earth for
  kpms style phases.
  Note this is the number of laps, not number of arrivals, so a value of 1
  would give 2 arrivals, one going the short path and one the long way around. This can be fractional, so 0.5 would exclude the long way around
  path. The default is 1.
\item[taup.path.maxPathInc] maximum distance in degrees between points of a path. This does a simple linear interpolant between nearby values in order to make plots look better. There is noo improvement in the accuracy of the path.
\item[taup.table.locsat.maxdiff] maximum distance in degrees for which Pdiff
or Sdiff are put into a locsat table. Beyond this distance Pdiff and Sdiff will
not be added to the table, even though they may show up in the output of
TauP Time. Instead, the next later arriving phase, if any, will be used
instead. The default is 105 degrees.
\item[taup.create.minDeltaP] Minimum difference in slowness between
successive slowness samples. This is used to decide when to stop adding new
samples due to the distance check.
Used by TauP Create to create new models.
The default is 0.1 sec/rad.
\item[taup.create.maxDeltaP] Maximum difference in slowness between
successive slowness samples. This is used to split any layers that exceed
this slowness gap.
Used by TauP Create to create new models.
 The default is 11.0 sec/rad.
\item[taup.create.maxDepthInterval] Maximum difference between successive depth
samples. This is used immediately after reading in a velocity model, with
layers being split as needed.
Used by TauP Create to create new models.
 The default is 115 km.
\item[taup.create.maxRangeInterval] Maximum difference between successive
ranges, in degrees. If the difference in distance for two adjacent rays
is greater than this, then a new slowness sample is inserted halfway between
the two existing slowness samples.
The default is 2.5 degrees.
\item[taup.create.maxInterpError] Maximum error for linear interpolation
 between successive sample in seconds. TauP Create uses this to try to insure
that the maximum error due to linear interpolation is less than this amount.
Of course, this is only an approximation based upon an estimate of the
 curvature of the travel time curve for surface focus turning waves.
In particular, the error for more complicated phases is greater. For instance,
if the true error for P at 30 degrees is 0.03 seconds, then the error for
PP at 60 degrees would be twice that, 0.06 seconds.
Used by TauP Create to create new models. The default is 0.05 seconds.
\item[taup.create.allowInnerCoreS] Should we allow J phases, S in
the inner core?
Used by TauP Create to create new models.
 The default is true. Setting it to false slightly reduces storage and model
load time.
\end{description}

Phase files, specified with the taup.phase.file property,
 are just text files with phase names, separated by either
spaces, commas or newlines. In section \ref{phasenaming} the details of
the phase naming convention are introduced.
By and large, it is compatible with traditional
seismological naming conventions, with a few additions and exceptions.
Also, for compatiblity with \textit{ttimes}, you may specify
:code:`ttp`, :code:`ttp+`, :code:`tts`, :code:`tts+`,
:code:`ttbasic` or :code:`ttall` to get a phase list corresponding
to the \textit{ttimes} options.

.. include:: taup_time
.. include:: taup_pierce
.. include:: taup_path
.. include:: taup_curve
.. include:: taup_phase
.. include:: taup_table
.. include:: taup_setsac
.. include:: taup_velplot
.. include:: taup_slowplot
.. include:: taup_velmerge
.. include:: taup_wavefront
.. include:: taup_create
.. include:: taup_gui
