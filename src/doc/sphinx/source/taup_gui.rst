
----------
TauP_GUI
----------

TauP\_GUI is unlike the rest of the tools in that it doesn't have any functionality
 beyond the other tools. It is just a GUI that uses TauP\_Time, TauP\_Pierce
and TauP\_Path. This is a nice feature of the java language in that each of
these applications exists simultaneously as a library. The GUI does not
currently have full access to all the things that these
three tools can do, and certainly has a few rough edges, but can be useful
for browsing. Lastly, it currently does more work than it has to in that it
always calculates times, pierce points and paths, even if only one is actually
needed. So, it may be a bit pokey.

:code:`taup gui`
