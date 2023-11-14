
----------
TauP_Path
----------

TauP\_Path uses a model like TauP\_Time but
generates  the
angular distances from the epicenter at which the specified rays pierce
path that the phases travel. The output is in GMT~\cite{GMT} ``psxy'' format, and is
placed into the file ``taup\_path.gmt''.
If you specify the ``-gmt'' flag then this
is a complete script with the appropriate ``psxy'' command prepended, so if you
have GMT installed, you can just:

\begin{verbatim}
taup path -mod iasp91 -h 550 -deg 74 -ph S,ScS,sS,sScS --gmt
sh taup_path.gmt
\end{verbatim}

and you have a plot of the ray paths in ``taup\_path.pdf''. To avoid possible plotting errors for
phases like :code:`Sdiff`, the ray paths are interpolated to less than
1 degree increments.

The usage is:

.. literalinclude:: cmdLineHelp/taup_path.usage
