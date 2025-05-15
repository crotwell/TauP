.. _taup_setsac:

-----------
TauP SetSac
-----------

TauP SetSac uses the depth and distance information in
*SAC* :cite:t:`sacmanual`
file headers to
put theoretical arrival times into the :code:`t0`--:code:`t9`
header variables. The header variable for a phase can be specified with by
a dash followed by a number, for instance :code:`S-9` puts the S arrival time
in :code:`t9`. If no header is specified then the time will be inserted in the
first header variable not allocated to another phase, starting with 0.
If there are no header variables not already allocated to a phase, then the
additional phases will not be added to the header. Note that this does not
refer to times that are already in the *SAC* file before TauP SetSac is run.
They will be overwritten. The ray parameter, in seconds per radian, is also
inserted into the corresponding :code:`user0`-:code:`user9` header.

Note that triplicated phases are a problem as there is only one
spot to put a time. For example, in iasp91 S has three arrivals at 20~degrees but only
one can be put into the choosen header. TauP SetSac assumes that the first arrival
is the most important, and uses it. Additional header variables may be appended for
the times of the multiples. For example, S-458 would put the first S arrival into
header :code:`t4` and the second into :code:`t5` and the third into :code:`t8`.
If there are more arrivals than headers, the later arrivals are not added. If there are
more headers than arrivals, then the header is not set.

**Warning:** TauP SetSac assumes the :code:`evdp` header has depth in meters unless
the :code:`--evdpkm`
flag is used, in which case kilometers are assumed. This may be a problem for
users that improperly use kilometers for the depth units. Due to much
abuse of the *SAC* depth header units, a warning message is
printed if the depth
appears to be in kilometers, i.e. it is < 1000, and :code:`--evdpkm` is not used.
This can be safely ignored
if the event really is less than 1000 meters deep. See the *SAC*
manual :cite:t:`sacmanual` for confirmation.

The *SAC* files must have :code:`evdp` and the :code:`o` marker set.
Also, if :code:`gcarc` or :code:`dist` is not
set then TauP SetSac can calculate a distance only if
:code:`stla`, :code:`stlo`, :code:`evla` and :code:`evlo`
are set.

The user should be very careful about previously set header variables.
TauP SetSac will
overwrite any previously set :code:`t` or :code:`user` headers.
A future feature may do
more careful checking, but the current version makes no effort to verify that
the header is undefined before writing.

If the given filename is a directory, TauP SetSac will recursively look for
files within that directory to process. Thus,
a large directory structure of Sac files can be processed easily.

For example, if the event and station headers are set in
my_earthquake.sac, then this will set travel times for P and S into the
T0 and T1 headers:

.. literalinclude:: examples/taup_setsac_-p_P_S_my_earthquake.sac.cmd
  :language: text

The usage is:

.. literalinclude:: cmdLineHelp/taup_setsac.usage
  :language: text
