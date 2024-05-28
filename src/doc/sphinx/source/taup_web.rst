.. _taup_web:

--------------
TauP Web
--------------

TauP Web is different, not exactly a tool, but provides access
to all of the other tools via a web interface. It takes the place of the old
gui tool that existed in TauP version 2. It can respond to either simple
queries where each of the query parameters matches the similar command line
argument, or you can use the "Calculator" page with a web form to create the
parameters. The "calculator" also displays the equivalent command line
version, which can be useful.
In addition, the TauP documentation is also available.
Because of strong
support within the web browser, SVG output from the graphical tools is
very useful.

By default it runs on port 7049, which kind of looks like T-A-U-P if you squint
just right. Note that due to the extra dependencies, the web tool uses a
separate startup script, :code:`taup_web`,
instead of the usual :code:`taup`.

To start, run

:code:`taup_web`

and then open your favorite web browser to

:code:`http://localhost:7049`

Note that the web server only listens for connections from the same computer,
as a security issue, and so the web interface is not available from other
systems. If you wish to have an instance of TauP accessible from other
machines, we recommend proxying via a regular web server. For example Apache2
via mod_proxy.

Also, the "Calculator" page uses fetch to process results and so
if you have privacy web blockers enabled on your browser you may need to
disable them for it to function. If you see a message like:

:code:`Network problem connecting to TauP server...`

this could be why.

The usage is:

.. literalinclude:: cmdLineHelp/taup_web.usage
  :language: text
