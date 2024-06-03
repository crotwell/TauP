
========
Overview
========

The algorithms employed within the TauP package are based on the
method of :cite:t:`bulandchapman`.
The IASPEI *ttimes* package is a widely-used implementation of
the methodology.

The main benefit of this new package is a marked increase in flexibility. It
will handle many types of velocity models, instead of being limited to
just a few. A new phase parser allows times to be computed for virtually
any seismic phase. The use of Java enables
this code to be run on a variety of machine and operating system types,
without recompiling. This package also offers
the extraction of derivative information, such as ray paths through the
earth, pierce and turning points, as well as travel time curves.

A paper has been published in Seismological Research Letters,
:cite:t:`taupsrl`,
that is intended to be used as a companion to this manual. While this manual
mainly focuses on the praticalities of using the codes,
the paper is able to go into more detail on the methodology. A pdf copy of
this paper is also available in the distribution.

The latest version of this documentation is available at
`Read The Docs <https://taup.readthedocs.io/en/latest/>`_.
