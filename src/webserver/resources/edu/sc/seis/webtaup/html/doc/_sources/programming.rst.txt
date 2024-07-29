
===================
Programmatic Access
===================

TauP can be used as a library in addition to the command line tools. The web
interface provides an alternative for non-JVM languages.


Java
----

As it is written in Java, TauP is most easily used by other Java programs. A
very simple Java program to calculate arrival times is below. The general flow
is that you start with a VelocityModel and convert to a TauModel, unless using
one of the built in TauModels. Then create SeismicPhase objects for each phase
of interest from the model. Lastly, use the phase to calculate Arrival objects
for a distance.

Javadocs for the package are at available at
`javadoc.io <https://javadoc.io/doc/edu.sc.seis/TauP>`_.

While the TauP jar and its dependencies are included in the distribution, it is
often easier to depend on the publication with
`Maven Central <https://central.sonatype.com/artifact/edu.sc.seis/TauP/overview>`_
repository
as this facilitates download and updates and provides for automatic dependency
resolution when using a build tool like maven or gradle.

Converting the initial model for a source depth is more costly than calculating
arrivals at a distance, so it is usually more efficient to process all stations
for each earthquake rather than all earthquakes for a station.


.. literalinclude:: programming/TimeExample.java
  :language: java

HTTP Access
-----------

While using Java, or perhaps another language that can run on the JVM, is the
most computationally efficient way to access TauP,
it does limit the languages available. For
other languages, the simplest way is to make use of the built in web access within
TauP to access the tools via an HTTP request returning JSON.

Running :code:`taup_web` in the distribution will start up a web server
on port 7049 to allow this type of access, although only from connections on the
local machine.

An example in Python using the
`Requests library <https://docs.python-requests.org/en/latest/user/quickstart/>`_
is below. Each of the normal command line tools are available via this web
interface, and almost all command line arguments can be sent
via a corresponding URL query parameter by removing leading dashes.
For example to calculate the at a distance in kilometers, where the command
line tool uses '--km 100', the URL would include 'km=100' like:

http://localhost:7049/time?phase=P,S&km=100&format=json

Boolean command line flags, like '--amp', can be given a true value as in
'amp=true'. Many options have default values, just as in the command line
tools. It may be helpful to experiment with the web gui at

http://localhost:7049/taup.html

to see how the URL is encoded and what the results are. Note that the web form
does not yet include all possible parameters that the web tools support.

.. literalinclude:: programming/get_times.py
  :language: python
