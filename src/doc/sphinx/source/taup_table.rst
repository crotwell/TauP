
----------
TauP_Table
----------

TauP\_Table creates an ASCII table of arrival times for a range of depths and
distances. Its main use is for generating travel time tables for earthquake
location programs such as LOCSAT. The :code:`--generic` flag generates a flat
table with all arrivals at each depth and distance, one arrival per line.
The :code:`--csv` flag generates a comma separated value file, including
a header while the :code:`--json` flag will output the table data as JSON.
The :code:`--locsat` flag generates a LOCSAT style travel time table with
only the first arrival of all the phases listed at each distance and depth.
Thus, the program must be run several times in order to generate files for
several phases. Also, all options write to standard out unless a file is
given with the -o flag.

There is a default phase, distance and depth list, but this is easily
customizable with the :code:`--header` option. An example LOCSAT style
file for use as a header can be generated with
:code:`taup table -locsat -o example.locsat`. The first
three sections specify the phase list, distances and depths to use.
After editing, a custom table can be created with
:code:`taup table -header example.locsat`.

Note that the :code:`taup.table.locsat.maxdiff` property sets the cutoff beyond which
Pdiff and Sdiff while not be output. This is to align the output with preexisting
locsat style travel time files.

The usage is:
\begin{verbatim}
piglet 1>taup table --help
Usage: taup table [arguments]
  or, for purists, java edu.sc.seis.TauP.TauP_Table [arguments]

Arguments are:
-ph phase list     -- comma separated phase list
-pf phasefile      -- file containing phases

-mod[el] modelname -- use velocity model "modelname" for calculations
                      Default is iasp91.


-header filename   -- reads depth and distance spacing data
                      from a LOCSAT style file.
--csv              -- outputs a CSV ascii table
--generic          -- outputs a "generic" ascii table
--locsat           -- outputs a "locsat" style ascii table
--json             -- outputs a table as JSON

-o [stdout|outfile]         -- output is redirected to stdout or to the "outfile" file
--prop [propfile]   -- set configuration properties
--debug             -- enable debugging output
--verbose           -- enable verbose output
--version           -- print the version
--help              -- print this out, but you already know that!
\end{verbatim}
