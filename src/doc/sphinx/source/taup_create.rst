.. _taup_create:

-----------
TauP Create
-----------

TauP Create takes a ASCII velocity model file, samples the model
and saves the tau model to a binary file.
The output file holds all
information about the model and need only be computed once. It
is used by all of the other tools. There are several parameters controlling
the density of sampling. Their values can be set with properties. See section
:ref:`default_params`.

Note that the use of TauP Create is no longer required as the various tools can
read velocity models directly
and effectively call TauP Create internally. However, if a model file will be
used repeatedly, using a
precomputed `.taup` file is more efficient.

:code:`modelfile` is the ASCII text file holding the velocity model.
The :code:`--nd` format is preferred
because the depths, and thus identities, of the major internal boundaries can
be unambiguously determined, making phase name parsing easier.
For compatiblity, we support the :code:`--tvel` format
currently used by the latest ttimes package, :cite:t:`kennett:ak135`.

The output will be a file named after the name of the
velocity file, followed by :code:`.taup`. For example

:code:`taup create -nd mymodel.nd`

produces the file :code:`mymodel.taup` which can be used later like

:code:`taup time --mod mymodel.taup --deg 35 -p P,S`


The usage is:

.. literalinclude:: cmdLineHelp/taup_create.usage
  :language: text
