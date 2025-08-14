
=====
Tools
=====


.. toctree::
    :hidden:

    default_params
    taup_time
    taup_pierce
    taup_path
    taup_distaz
    taup_find
    taup_curve
    taup_phase
    taup_table
    taup_setms3
    taup_setsac
    taup_velmerge
    taup_velplot
    taup_discon
    taup_refltrans
    taup_wavefront
    taup_spikes
    taup_create
    taup_web
    taup_version
    taup_help


Tools included with the TauP package:

=================================   =========================================================================
:ref:`time <taup_time>`             calculates travel times.
:ref:`pierce <taup_pierce>`         calculates pierce points at model discontinuities and specified depths.
:ref:`path <taup_path>`             calculates ray paths, depth versus epicentral distance.
:ref:`find <taup_find>`             calculates all possible phases in a model with number of interactions.
:ref:`distaz <taup_distaz>`         distance, azimuth and back azimuth between points.
:ref:`wavefront <taup_wavefront>`   calculates wavefronts in steps of time, depth versus epicentral distance.
:ref:`curve <taup_curve>`           calculates travel time curves, time versus epicentral distance.
:ref:`phase <taup_phase>`           textual description of the path the phase takes through the model.
:ref:`table <taup_table>`           outputs travel times for a range of depths and distances in an ASCII file
:ref:`setms3 <taup_setms3>`         puts theoretical arrival times into extra headers.
:ref:`setsac <taup_setsac>`         puts theoretical arrival times into sac header variables.
:ref:`velmerge <taup_velmerge>`     merges part of one velocity model into another.
:ref:`velplot <taup_velplot>`       output velocity model as a gmt script.
:ref:`discon <taup_discon>`         list discontinuities in a velocity model.
:ref:`create <taup_create>`         creates a .taup model from a velocity model.
:ref:`spikes <taup_spikes>`         create spike seismogram.
:ref:`refltrans <taup_refltrans>`   plot reflection and transmission coefficients for a discontinuity.
:ref:`web <taup_web>`               http access to the tools
:ref:`version <taup_version>`       print the version.
:ref:`help <taup_help>`             display help information about the specified command.
=================================   =========================================================================

Usage
--------------

Each tool is a subcommand of the overall :code:`taup` Java application which is
a wrapper to make execution easier, and installed with a simple sh script
for *Unix* and a .bat files for windows. In general the tools are run like

:code:`taup time -p P --deg 35`

where the pattern is :code:`taup <tool> <arg> <arg> <arg>...`.

Help and Version
^^^^^^^^^^^^^^^^

Each tool has a :code:`--help` flag that will print a usage summary, as well
as a :code:`--version` flag that will print the version. For help in general,

:code:`taup --help`

gives the tools available while

:code:`taup <tool> --help`

gives help for a particular tool.

Output Formats
^^^^^^^^^^^^^^

Most tools support
several output formats. Some tools, like time, are textual, while other like
path can also be graphical. In general, the most commonly useful output format
is the default, usually :code:`--text` for textual output and :code:`--svg` for
`SVG <https://developer.mozilla.org/en-US/docs/Web/SVG>`_ graphical output.
The output formats support by one or more tools are:

================ ============
:code:`--text`   Textual output, usually best for human reading
:code:`--html`   HTML web page output, also for human reading
:code:`--json`   `JSON <http://json.org>`_, usually best for parsing
:code:`--csv`    CSV, comma separated values, sometimes good for parsing
:code:`--nd`     named discontinuity, velocity model file format
:code:`--gmt`    `GMT <https://www.generic-mapping-tools.org/>`_ plot style
:code:`--svg`    `SVG <https://developer.mozilla.org/en-US/docs/Web/SVG>`_ image
:code:`--ms3`    `Miniseed3 <https://docs.fdsn.org/projects/miniseed3/en/latest/>`_ waveform file
:code:`--sac`    `SAC <https://ds.iris.edu/ds/nodes/dmc/software/downloads/sac/102-0/>`_ waveform file
:code:`--locsat` `LOCSAT <https://www.seiscomp.de/doc/apps/global_locsat.html>`_ style traveltime table
================ ============

Velocity Model
^^^^^^^^^^^^^^

Almost all tools require an input velocity model. This can be specified via the
:code:`--mod` argument, and TauP will search for either an already created
`.taup` model file, or a `.nd` named discontinuities model file or a `.tvel`
ttimes style model file. The `.taup` file, being already interpolated, is usually
faster, especially for repeated uses, but being able to read, interpolate and
calculate a `.nd` or `.tvel` file is very useful.

Distances
^^^^^^^^^

Many of the tools require a way to specify the ray to be calculated, either
via some parameter of the ray or of a distance. See the
:ref:`Distances <distances>` section for more on the various arguments used to specify this.

Also see the :ref:`Calculations <calculations>` section for important information
on how distances are calculated internally.

.. include:: default_params
.. include:: taup_time
.. include:: taup_pierce
.. include:: taup_path
.. include:: taup_distaz
.. include:: taup_find
.. include:: taup_curve
.. include:: taup_phase
.. include:: taup_table
.. include:: taup_setsac
.. include:: taup_setms3
.. include:: taup_velplot
.. include:: taup_velmerge
.. include:: taup_wavefront
.. include:: taup_refltrans
.. include:: taup_web
.. include:: taup_spikes
.. include:: taup_create
.. include:: taup_version
.. include:: taup_help
