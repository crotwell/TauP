.. _taup_discon:

-----------------
TauP Discon
-----------------

TauP Discon lists the discontinuities within the given velocity model.

The lines beginning with three dashes are depth and radius. The lines
immediately above and below these lines are the velocity and density
just above and below the discontinuity. If a discontinuity is named, that
will also be printed on the discontinuity line.

For example, the lists the depths of disconinuties in the `ak135fcont` model:

.. literalinclude:: examples/taup_discon_--mod_ak135fcont.cmd
  :language: text

.. literalinclude:: examples/taup_discon_--mod_ak135fcont
  :language: text


The usage is:

.. literalinclude:: cmdLineHelp/taup_discon.usage
  :language: text
