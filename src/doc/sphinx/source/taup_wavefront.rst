
--------------
TauP Wavefront
--------------

TauP Wavefront is similar to TauP Path, but plots the wavefront at timesteps instead of the
ray paths. It also uses a model like TauP Time and
generates  depth versus
angular distances from the epicenter for the phases, but done at time slices instead of depth slices.
The output is in GMT~\cite{GMT} `psxy` format, and is
placed into the file `taup_wavefront.gmt`.
The colums are distance, depth, time and ray param, although only the first two are used by the GMT script.
If you specify the `-gmt` flag then this
is a complete script with the appropriate `psxy` command prepended, so if you
have GMT installed, you can just:

.. literalinclude:: examples/taup_wavefront_--mod_iasp91_-h_550_-p_s_S_ScS_sS_sScS_--gmt.cmd
  :language: text

\begin{verbatim}
taup wavefront -mod iasp91 -h 550 -ph s,S,ScS,sS,sScS --gmt
sh taup_wavefront.gmt
\end{verbatim}

and you have a plot of the wavefronts in `taup_wavefront.pdf`.

Or use --svg to generate a SVG plot

.. literalinclude:: examples/taup_wavefront_--mod_iasp91_-h_550_-p_s_S_ScS_sS_sScS_--color_wavetype_--svg.cmd
  :language: text


.. raw:: html
    :file:  examples/taup_wavefront_--mod_iasp91_-h_550_-p_s_S_ScS_sS_sScS_--color_wavetype_--svg


The usage is:

.. literalinclude:: cmdLineHelp/taup_wavefront.usage
  :language: text
