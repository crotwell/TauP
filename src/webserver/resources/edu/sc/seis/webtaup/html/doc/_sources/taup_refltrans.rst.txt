.. _taup_refltrans:

-----------------
TauP ReflTrans
-----------------

TauP ReflTrans plots reflection and transmission coefficients for either a
discontinuity in a model or given velocities above and below a boundary. The
calculations are done using a flat earth ray parameter, 1/s, instead of the
usual spherical ray parameter, radian/sec, as in the rest of the calculations.

For example, this would generate an SVG plot of the displacement
reflection and transmission coefficients from the moho in the ak135 model:

.. literalinclude:: examples/taup_refltrans_--mod_ak135_--legend_--depth_35_--svg.cmd
  :language: text

and the result would be:

.. raw:: html
    :file:  examples/taup_refltrans_--mod_ak135_--depth_35_--legend_--svg

This calculates the coefficients for energy flux instead of displacement:

.. literalinclude:: examples/taup_refltrans_--mod_ak135_--depth_35_--legend_--energyflux_--svg.cmd
  :language: text

and the result would be:

.. raw:: html
    :file:  examples/taup_refltrans_--mod_ak135_--depth_35_--legend_--energyflux_--svg




The usage is:

.. literalinclude:: cmdLineHelp/taup_refltrans.usage
  :language: text
