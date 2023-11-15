
------------------
TauP VelocityMerge
------------------

TauP VelocityMerge overlays a partial velocity model over the given base velocity model. This makes is easy to overlay a new crust and
upper mantle structure on an existing global model. For example
if dumbcrust.nd contains:
\begin{verbatim}
0.0 3 2 4
5.0 3.1 2.1 4
21.0 3.2 2.2 4
mantle
21.0 8.1 4.4 3.3
30.0 8.1 4.4 3.3
\end{verbatim}

then running
\begin{verbatim}
taup velmerge -mod prem --ndmerge dumbcrust.nd
\end{verbatim}

would create a velocity model called prem\_dumbcrust that contained:

\begin{verbatim}
0.0 3.0 2.0 4.0
5.0 3.1 2.1 4.0
21.0 3.2 2.2 4.0
mantle
21.0 8.1 4.4 3.3
30.0 8.1 4.4 3.3
30.0 8.107228461538462 4.488757435897436 3.3801497435897434
40.0 8.10119 4.48486 3.37906
60.0 8.08907 4.47715 3.37688
\end{verbatim}

If the discontinuity at 30 km depth is not desired, the --smbot argument will create a smooth transition from the bottom of the new model to the first depth sample in the base model. Care should be taken with named discontinuities if they are
different in the merged model by making sure there is enough
depth in the merge to cover the named in both models. The merge
model does not have to start at the surface, it can be just a
range of depths.

The usage is:

.. literalinclude:: cmdLineHelp/taup_velmerge.usage
