
-----------
TauP_Create
-----------

TauP\_Create takes a ASCII velocity model file, samples the model
 and saves the tau model to a binary file.
The output file holds all
information about the model and need only be computed once. It
is used by all of the other tools. There are several parameters controlling
the density of sampling. Their values can be set with properties. See section
\ref{properties}, above.

Note that the use of TauP\_Create is no longer required as the various tools can read velocity models directly
and effectively call TauP\_Create internally. However, if a model file will be used repeatedly, using a
precomputed {\texttt .taup} file is more efficient.

The usage is:
\begin{verbatim}
piglet 8>taup create --help
Usage: taup_create [arguments]
  or, for purists, java edu.sc.seis.TauP.TauP_Create [arguments]

Arguments are:

   To specify the velocity model:
-nd modelfile       -- "named discontinuities" velocity file
-tvel modelfile     -- ".tvel" velocity file, ala ttimes

--debug              -- enable debugging output
--prop [propfile]    -- set configuration properties
--verbose            -- enable verbose output
--version            -- print the version
--help               -- print this out, but you already know that!
\end{verbatim}

:code:`modelfile` is the ASCII text file holding the velocity model.
The :code:`-nd` format is preferred
because the depths, and thus identities, of the major internal boundaries can
be unambiguously determined, making phase name parsing easier.
See section \ref{exampleCreate} for an example.
For compatiblity, we support the :code:`-tvel` format
currently used by the latest ttimes package,~\cite{kennett:ak135}.

The output will be a file named after the name of the
velocity file, followed by :code:`.taup`. For example

:code:`taup create -nd prem.nd`

produces :code:`prem.taup`.
