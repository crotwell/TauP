
========
Overview
========

This documentation is for version |release| of The TauP Toolkit
created |today|.

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

A paper was been published in Seismological Research Letters,
:cite:t:`taupsrl`,
that is intended to be used as a companion to this manual. While this manual
mainly focuses on the practicalities of using the codes,
the paper is able to go into more detail on the methodology. A pdf copy of
this paper is also available in the distribution.

The latest version of this documentation is available at
`Read The Docs <https://taup.readthedocs.io/en/latest/>`_.

The latest source is at GitHub,
https://github.com/crotwell/TauP
.
Please file bug reports there,
https://github.com/crotwell/TauP/issues
.

If you find TauP useful and would like to cite it, please use the following:

  Crotwell, H. P., T. J. Owens, and J. Ritsema (1999). The TauP Toolkit: Flexible seismic travel-time and ray-path utilities, Seismological Research Letters 70, 154–160.

  Crotwell, H. P. |release_year|. The TauP Toolkit (|release|). Zenodo. |zenodo_doi|


I really enjoy writing software that others find useful. And while the journey
is its own reward, it is nice to hear from people that use and like TauP. If you
find the TauP Toolkit to be a useful item in your seismology toolbox, drop
me an email, crotwell@seis.sc.edu, and tell me something you appreciate about
TauP and something you wish it could do. And if you find a bug, please file
an `issue <https://github.com/crotwell/TauP/issues>`_.

Thank you for using TauP and good luck.
