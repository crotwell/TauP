
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
:ref:`create <taup_create>`         creates a .taup model from a velocity model.
:ref:`spikes <taup_spikes>`         create spike seismogram.
:ref:`refltrans <taup_refltrans>`   plot reflection and transmission coefficients for a discontinuity.
:ref:`web <taup_web>`               http access to the tools
:ref:`version <taup_version>`       print the version.
:ref:`help <taup_help>`             display help information about the specified command.
=================================   =========================================================================

Startup Script
--------------

Each tool is a subcommand of the overall :code:`taup` Java application which is
an wrapper to make execution easier: sh scripts
for *Unix* and
bat files for windows.  The application are machine independent but the
wrappers are OS specific.
For example, to invoke TauP Time under *Unix*, you could type

:code:`java -classpath ... edu.sc.seis.TauP.TauP time -mod prem`

or simply use the script that does the same thing,

:code:`taup time -mod prem`


Each tool has a :code:`--help` flag that will print a usage summary, as well
as a :code:`--version` flag that will print the version.

Tab Completion
--------------

New with version 3.0 is tab completion for bash and zsh. Sourcing the output of
:code:`taup generate-completion` will provide hints
for the shell when hitting the tab key for bash or zsh. You can enable it
with running this command:

:code:`source <(taup generate-completion)`

Adding this to your .bash_profile or .zshrc will enable it for future logins.

Note, for bash 3.2, which the default verion on OSX, there is a bug that
prevents this from working. The alterantive is to save it as a file like:

:code:`taup generate-completion > taup_completion`

and then source the file:

:code:`source taup_completion`

Once sourced, you will be able to get hints or completion for most arguments
within TauP. For example typing:

:code:`taup time -`

and then hitting the tab key will display all of the possible command line
flags. Continuing to type

:code:`taup time --mod`

and then hitting the tab key will display the models available for the
:code:`--mod` command line argument:

:code:`ak135          ak135favg      ak135fcont     ak135fsyngine  iasp91         prem
`


.. include:: default_params
.. include:: taup_time
.. include:: taup_pierce
.. include:: taup_path
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
