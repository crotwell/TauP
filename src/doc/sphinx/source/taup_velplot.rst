
-----------------
TauP VelocityPlot
-----------------

TauP VelocityPlot creates a plot the given velocity model. It can create
traditional plots of velocity and density as a function of depth, but can
also plot various derived quantities like Poisson's Ratio and other material
properties.

For example, a plot of P and S velocity against depth on the
y axis for `ak135fcont` and `prem`:


.. literalinclude:: examples/taup_velplot_--mod_ak135fcont_--mod_prem_--svg.cmd
  :language: text

.. raw:: html
    :file:  examples/taup_velplot_--mod_ak135fcont_--mod_prem_--svg

Or a plot of Poisson's Ratio for the `ak135fcont` model:

.. literalinclude:: examples/taup_velplot_--mod_ak135fcont_-x_poisson_--svg.cmd
  :language: text

.. raw:: html
    :file:  examples/taup_velplot_--mod_ak135fcont_-x_poisson_--svg


The usage is:

.. literalinclude:: cmdLineHelp/taup_velplot.usage
  :language: text
