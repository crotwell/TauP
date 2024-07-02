.. _taup_velmerge:

------------------
TauP VelocityMerge
------------------

TauP VelocityMerge overlays a partial velocity model over the given base
velocity model. This makes is easy to overlay a new crust and
upper mantle structure on an existing global model.

One common source of errors with cusom models is users forgetting that TauP
requires a full earth model because the radius of the earth is given by
the deepest sample in the velocity model. A model file that shows depths
only down to 30 kilometers may look like a model of the crust of the earth,
but TauP will assume your planet has a radius of only 30 kilometers, so
something more like asteroid seismology. In order to calculate even just
crustal phases, you will need layers between the crust and the center of
the earth.

For example
if dumbcrust.nd contains::

  0.0 3 2 4
  5.0 3.1 2.1 4
  21.0 3.2 2.2 4
  mantle
  21.0 8.1 4.4 3.3
  30.0 8.1 4.4 3.3

then we need to paste a model like prem beneath it. So running
:code:`taup velmerge --mod prem --ndmerge dumbcrust.nd -o prem_dumbcrust.nd`

would create a velocity model called prem\_dumbcrust that contained the layers
from our model and then prem below to give a model radius of 6371 kilometers::

  0.0 3.0 2.0 4.0
  5.0 3.1 2.1 4.0
  21.0 3.2 2.2 4.0
  mantle
  21.0 8.1 4.4 3.3
  30.0 8.1 4.4 3.3
  30.0 8.107228461538462 4.488757435897436 3.3801497435897434
  40.0 8.10119 4.48486 3.37906
  60.0 8.08907 4.47715 3.37688
  ...
  6271.0 11.26064 3.6667 13.0863
  6371.0 11.2622 3.6678 13.08848

If the discontinuity at 30 km depth is not desired, the --smbot argument will
create a smooth transition from the bottom of the new model to the first depth
sample in the base model. Care should be taken with named discontinuities
if they are
different in the merged model by making sure there is enough
depth in the merge to cover the named in both models. The merge
model does not have to start at the surface, it can be just a
range of depths.

The usage is:

.. literalinclude:: cmdLineHelp/taup_velmerge.usage
  :language: text
