
--------------
TauP_Wavefront
--------------

TauP\_Wavefront is similar to TauP\_Path, but plots the wavefront at timesteps instead of the
ray paths. It also uses a model like TauP\_Time and
generates  depth versus
angular distances from the epicenter for the phases, but done at time slices instaed of depth slices.
The output is in GMT~\cite{GMT} ``psxy'' format, and is
placed into the file ``taup\_wavefront.gmt''.
The colums are distance, depth, time and ray param, although only the first two are used by the GMT script.
If you specify the ``-gmt'' flag then this
is a complete script with the appropriate ``psxy'' command prepended, so if you
have GMT installed, you can just:

\begin{verbatim}
taup wavefront -mod iasp91 -h 550 -ph S,ScS,sS,sScS --gmt
sh taup_wavefront.gmt
\end{verbatim}

and you have a plot of the wavefronts in ``taup\_wavefront.pdf''.

The usage is:

.. literalinclude:: cmdLineHelp/taup_wavefront.usage
