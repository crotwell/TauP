.. _taup_path:

----------
TauP Path
----------

TauP Path uses a model like TauP Time but
generates  the
angular distances from the epicenter at which the specified rays pierce
path that the phases travel. The output can be JSON, an SVG image or
in GMT :cite:t:`GMT` :code:`psxy` format, and is
placed into the file `taup_path` with approproate extension.
If you specify the `--gmt` flag then this
is a complete script with the appropriate `psxy` command prepended, so if you
have GMT installed, you can just:

.. literalinclude:: examples/taup_path_--mod_iasp91_-h_550_--deg_74_-p_S_ScS_sS_sScS_--gmt.cmd
  :language: text

:code:`sh taup_path.gmt`

and you have a plot of the ray paths in `TauP path.pdf`. To avoid possible plotting errors for
phases like :code:`Sdiff`, the ray paths are interpolated to less than
1 degree increments. Or similarly

.. literalinclude:: examples/taup_path_--mod_iasp91_-h_550_--deg_74_-p_S_ScS_sS_sScS_--svg.cmd
  :language: text

and the result would be:

.. raw:: html
    :file:  examples/taup_path_--mod_iasp91_-h_550_--deg_74_-p_S_ScS_sS_sScS_--svg


The usage is:

.. literalinclude:: cmdLineHelp/taup_path.usage
  :language: text
