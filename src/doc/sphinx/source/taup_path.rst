
----------
TauP Path
----------

TauP Path uses a model like TauP Time but
generates  the
angular distances from the epicenter at which the specified rays pierce
path that the phases travel. The output is in GMT~\cite{GMT} ``psxy'' format, and is
placed into the file `taup_path.gmt`.
If you specify the `--gmt` flag then this
is a complete script with the appropriate `psxy` command prepended, so if you
have GMT installed, you can just:

.. literalinclude:: examples/taup_path_--mod_iasp91_-h_550_--deg_74_-p_S_ScS_sS_sScS_--gmt.cmd

\begin{verbatim}
sh taup_path.gmt
\end{verbatim}

and you have a plot of the ray paths in `TauP path.pdf`. To avoid possible plotting errors for
phases like :code:`Sdiff`, the ray paths are interpolated to less than
1 degree increments.

The usage is:

.. literalinclude:: cmdLineHelp/taup_path.usage
