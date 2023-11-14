
------------------
TauP_PhaseDescribe
------------------

TauP\_PhaseDescribe outputs a textual description of the path the
phase takes through the model.

For example:
\begin{verbatim}
taup phase -mod prem -h 200 -ph PKiKP
PKiKP:
  exists from     0.00 to   152.44 degrees.
  with ray parameter from    2.059 down to    0.000 sec/deg.
  travel times from  1170.37 to   966.27 sec.
P going down as a P in the mantle, layer 3 to 6, depths 200.0 to 2891.0, then transmit down
K going down as a P in the outer core, layer 7, depths 2891.0 to 5149.5, then reflect topside
K going up   as a P in the outer core, layer 7, depths 5149.5 to 2891.0, then transmit up
P going up   as a P in the crust/mantle, layer 6 to 0, depths 2891.0 to 0.0, then end
\end{verbatim}

The usage is:

.. literalinclude:: cmdLineHelp/taup_phase.usage
