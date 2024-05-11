.. _taup_find:

---------
TauP Find
---------

TauP Find generates possible phase paths through the given earth model.
It is a way of cheating in that favorite game of seismologists, what's that
wiggle? It will attempt all possible paths within a model, subject to the
constraints. The output is either a simple list of possible phase names or
travel times in the same format as taup time.


The complexity or length of the path is limited by the :code:`--max`
command line argument, which limits the number of interactions with
discontinuities in the model, either reflection or phase conversion.
For example, P and PKP both have 0 interactions while
the phases sS and PcP both have one interaction due to the
reflections and PKS has one due to the KS phase conversion.
SKS has 2 because of the two shear-compression conversions at the core-mantle
boundary and sP has 2 due to one phase conversion and one reflection,
both at the surface.

Great care should be take as the combinatorial explosion is
significant.  In the iasp91 model for a surface source, :code:`--max 1`
results in 82 phases while :code:`--max 2` results in over 1000 phases.
Then :code:`--max 3` results in over 13,000 phases and
:code:`--max 4` is over 150,000. The number of phase paths can be limited
by a range of ray parameters or times or by disabling interactions with
discontinuities in the model.


The usage is:

.. literalinclude:: cmdLineHelp/taup_find.usage
  :language: text

The phases may also be limited by a ray parameter range or optionally by a
travel time range. Note that limiting by interaction number, via :code:`--max`
or limiting the ray parameter range via :code:`--rayparam` or
:code:`--rayparamkm` are simple.
Limiting by arrival time of the phase using the :code:`--time` is more
complex as the phase must be evaluated fully in the model, which takes longer.
Both :code:`--rayparam` and :code:`--time` accept one or two values. If one,
then the resulting phase must overlap that value, if two then it must overlap
at least a part of that range. Note :code:`--time` also working with
:code:`--deltatime` to allow specifying a single time and a delta time before
and after it.

For example:

.. literalinclude:: examples/taup_find_--max_2_-h_100.cmd
  :language: text

gives you all phases with 0,1 or 2 interactions for a 100 kilometer
deep source, excluding interactions with the 210 discontinuity. Both the
`iasp91` and `ak135` velocity models have very small discontinuities only in
S velocity at 210, and so it is probably best to exclude that as it is too
small to generate interacting phases.
Note that many of these phases may be of very small amplitude, below what
is detectable,
or may not exist at all for some distances and source depths.


.. literalinclude:: examples/taup_find_--max_2_--deg_35_-h_100_--time_400_420_--exclude_210.cmd
  :language: text
