
===================================
Velocity Models
===================================

--------------------
Velocity Model Files
--------------------

There are currently two variations of velocity model files that can be read.
Both are piecewise linear between given depth points. Support for cubic spline
velocity models would be useful and is planned for a future release.

The first format is that used by the most recent ttimes
codes :cite:t:`kennett:ak135`, `.tvel`.
This format has two comment lines, followed by lines composed of depth, Vp, Vs and density, all separated by whitespace. TauP ignores the first two lines of this format and reads the remaining lines.

The second format is based on the format used by Xgbm, :cite:t:`xgbmreport,xgbmmanual`.
It is referred to here
as the `.nd` format for *named discontinuities.*
Its biggest advantage is that it can specify the location of the major
boundaries and this makes it the preferred format.
The file consists of two types of lines, those that specify velocity at
a depth, and those that specify the name of a discontinuity.

The first type of line has between 3 and 6 numbers on a line separated
by whitespace. They are, in order:

===========     =============================================
Depth           Kilometers from surface to the sample point
V\ :sub:`p`     P velocity in kilometers per second
V\ :sub:`s`     S velocity in kilometers per second
Rho             Density in grams per cubic centimeter
Q\ :sub:`p`     Attenuation for compressional waves
Q\ :sub:`s`     Attenuation for shear waves
===========     =============================================

Only  depth, V\ :sub:`p` and V\ :sub:`s` are required.
The remaining parameters, while not needed for travel time
calculations, are included to allow the model to be used for other purposes
like amplitude calculates. The model is assumed to be linear between given depths and
repeated depths are used to represent discontinuities.

The second type of line within the `.nd` format specifies one of the
major internal boundaries. The original format was limited to *mantle*,
*outer-core* and *inner-core*, but in version 3.0 this was expanded to include
more crustal boundaries and synonyms. User defined labels are also allowed but
must start with a non-number character and care should be taken when using
in phase names to avoid confusing the phase parser. These labels
are placed on a line by themselves between the two lines representing the
sample points above and below the depth of the
discontinuity.

These help to determine where a particular phase propagates. For instance,
in a model that has many crustal and upper mantle layers, from which
discontinuity does the phase :code:`PvmP` reflect?
Explicit labeling eliminates potential ambiguity.

=================     =============================================
Labels                Description
=================     =============================================
crust                 top of crust
mantle, moho          moho, crust-mantle boundary
outer-core, cmb       mantle-core boundary
inner-core, icocb     inner outer core boundary
ice                   top of ice layer
ice-ocean             ice above ocean boundary
ice-crust             ice above crust boundary
ocean                 top of ocean layer
seabed                ocean above crust boundary
=================     =============================================


For example a very simplistic model of Europa, showing the use of other
named discontinuities, might look like:


.. literalinclude:: EuropaLike.nd
  :language: text

One further enhancement to these model file formats is the support for comments
embedded within the model files. As in shell scripting, everything after
a :code:`\#` on a line is ignored. In addition, *C* style :code:`/* ... */`
and :code:`// ...` comments are recognized.

A very simple named discontinuities model file might look like this:

.. literalinclude:: verysimple.nd
  :language: text


In many cases it is better and easier to make use of taup velmerge
to create a new model by making changes to an existing global model,
especially when for example the user only cares about crust and upper mantle
structure and is happy with an existing global model for
the lower mantle and core. Hand editing velocity model files
often results in hard to catch errors.

----------------------
Using Saved Tau Models
----------------------

There are three ways of finding a previously generated model file. If no
model is given, then the iasp91 model will be used. The search is first, as
a standard model as part of the distribution.
Second, a list of directories and jar files to
be searched can be specified with the taup.model.path property.
Lastly, the path to the actual model file may be specified.
TauP searches each of these
places in order until it finds a model that matches the name.

* Standard Model.

  TauP first checks to see if the model name is associated with a standard model.
  Several standard models are precomputed and included within the distributed jar file.
  They are:

  ====================================================== =============================
  `iasp91 <_static/StdModels/iasp91.tvel>`_              :cite:t:`iasp` (default)
  `prem <_static/StdModels/prem.nd>`_                    :cite:t:`dziewonski_anderson`
  `ak135 <_static/StdModels/ak135.tvel>`_                :cite:t:`kennett:ak135`
  `ak135favg <_static/StdModels/ak135favg.nd>`_          :cite:t:`kennett:ak135f`
  `ak135fcont <_static/StdModels/ak135fcont.nd>`_        :cite:t:`kennett:ak135f`
  `ak135fsyngine <_static/StdModels/ak135fsyngine.nd>`_  :cite:t:`kennett:ak135f`
  ====================================================== =============================

  We will consider adding other models to the distribution if
  they are of wide interest.
  They are included within the distribution jar file and
  taup can locate them with just the model name.

* Within the taup.model.path property.

  Users can create custom models, and place the stored models in a convenient
  location. If the :code:`taup.model.path` property includes those
  directories or jar files, then they can be located.
  The search is done in the order of taup.model.path until a model matching the model
  name is found. While taup.model.path is a Java property, the shell scripts provided
  translate the environment variable TAUPPATH into this property. The user
  generally need not be aware of this fact except when the tools are invoked
  without using the provided shell scripts. A more desirable method is to
  set the taup.model.path in a properties file. See section :ref:`default_params` for
  more details.

  The taup.model.path property is constructed in the manner of standard Java CLASSPATH
  which is itself based loosely on the manner of the *Unix* PATH.
  The only real
  differences between CLASSPATH and PATH are that a jar file
  may be placed directly in the path and the path separator character
  is machine dependent, *Unix* is ``:`` but other systems may vary.

  The taup.model.path allows you to have directories containing saved model files
  as well as jar files of models.
  For instance, in a *Unix* system using the c shell,
  you could set your TAUPPATH to be, (all one line):

  ``setenv TAUPPATH /home/xxx/MyModels.jar:/home/xxx/ModelDir:
  /usr/local/lib/localModels.jar``

  or you could place a line in the *.taup* file in your home directory
  that accomplished the same thing, again all one line:

  ``taup.model.path=/home/xxx/MyModels.jar:/home/xxx/ModelDir:
  /usr/local/lib/localModels.jar``

  If you place models in a jar, TauP assumes that they are placed
  in a directory called ``Models`` before they are jarred.
  For example, you might
  use ``taup create`` to create several taup models in the Models directory
  and then create a jar file.

  :code:`jar -cf MyModels.jar Models`

  Including a ``.`` for the current working directory with the taup.model.path
  is not necessary since we
  always check there, see below, but it may be used to
  change the search order.

* The last place TauP looks is for a tau model file specified
  on the command line.
  So, if you generate newModel.taup and want to get some times, you can just say:
  ``taup time -mod newModel.taup``
  or even just
  ``taup time -mod newModel``
  as TauP can add the taup suffix if necessary. A relative or absolute pathname
  may precede the model, e.g.
  ``taup time -mod ../OtherDir/newModel.taup``.

* New in version 2.0 is the ability of the tools to load a velocity model directly
  and handle the tau model create functionality internally,
  so in addition to ``.taup`` files,
  the ``.nd`` and ``.tvel`` model files can be loaded directly
  if there is not a ``.taup`` file found.
  Note that there is extra work involved in processing the velocity file, and so
  frequently used models should still be converted using 'taup create' to avoid
  reprocessing them each time the tool starts.


------------------------
Notes on Standard Models
------------------------

* ak135

  There is a discontinuity in S velocity, but not for P or density at 210 km.
  The effect on travel times is small, but this discontinuity in S
  velocity at 210 creates odd reflection and transmission coefficients at
  this depth. This discontinuity does not exist in the ak135f models for
  velocity, but the 210 is a discontinuity in the Q model.

  .. code-block::

    210.000      8.3000      4.5180      3.4258
    210.000      8.3000      4.5230      3.4258


  There is a non-discontinuity discontinuity within the published ak135 model
  at 2740 km depth. This causes no issues within TauP as there is no change
  in velocity at that depth.

  .. code-block::

    2740.000     13.6494      7.2490      5.4817
    2740.000     13.6494      7.2490      5.4817

  Between 4700 and 4800 km depth there appears to be a missing line in the
  published model as before and after the depth increment is approximately
  50 km, but is 100 at this depth.
  This may have a small effect on outer core phases. The
  missing line does appear in the ak135f models, which are supposed to share
  the velocity structure, but to be consistent with the published paper we
  have not changed the file. We do recomment using ak135fcont instead,
  which is ak135 with Q and the "continental structure" above 120 km.

  .. code-block::

    4650.590     10.0439      0.0000     11.8437
    4700.920     10.0768      0.0000     11.8772
    4801.580     10.1415      0.0000     11.9414
    4851.910     10.1739      0.0000     11.9722

  The source web page for ak135f lists attentuation parameters as Qkappa and Qmu,
  but the reference paper, :cite:t:`kennett:ak135f` says:

  :quote: We did not attempt to invert for Q K ,which is
    known to be poorly resolved. To a first approximation, Q K-1
    is very close to zero except in the core.

  and so it seems reasonable that this column is actually Qp (or equivalently
  Q_alpha) and is derived from the Qs =Q_mu paramter via equation 2.6 where

    1/Q_alpha = (4/3)(beta/alpha)^2 (1/Q_beta)

  Checking the given values shows that the Q_kappa column to actually be Q_p within
  +-0.03 for all values except fluid layers, where Qp is set to a generic large
  value of 57822.0, which is the same default value used in PREM. For comparison,
  the PREM model here:
  https://ds.iris.edu/ds/products/emc-prem/
  does in fact contain Q_kappa as the last column in PREM_1s.csv and the value is
  constant at 57823 except in the inner core, where it is 1327.7. We therefore
  are using this column as Q_alpha and not Q_kappa.

* ak135fcont

  There is a small discrepancy in how to create a "continental structure"
  variant of ak135f at 120 km depth. In the online version
  at https://rses.anu.edu.au/seismology/ak135/ak135f.html
  there is a small discontinuity in P velocity if the continental model
  is pasted on top of the base model that does not exist for the average
  model, where vp=8.0500 in the continent vs 8.0505 in the base,
  which would make the 120 km depth a very odd
  discontinuity as density and S velocity are continuous.
  We have constructed
  the "continental structure" model, ak135fcont, so that Vp is continuous
  at this depth with Vp=8.0505.

  There is a difference between the text version of the model,
  https://rses.anu.edu.au/seismology/ak135/ak135f.txt
  and the html version here
  https://rses.anu.edu.au/seismology/ak135/ak135f.html
  with the text version showing a depth sample in the average structure at 120 km
  that matches the 120 km sample in the base model. As both lines are the same,
  this doesn't change the model, but seems to indicate that the 120 km depth
  sample in the continental model should not be viewed as creating a discontinuity.

  At 210 km depth, there is a non-discontinuity in velocity, but a discontinuity
  in the Q model.

  .. code-block::

    210.00    3.3243    8.3007    4.5184    200.97     79.40     0.000
    210.00    3.3243    8.3007    4.5184    338.47    133.72     0.000

  The "continental structure" model does not list density, Qp or Qs for the
  upper 120 km. Because these values are useful for amplitude calculations,
  we have inserted the density from ak135 for these depth samples and used values
  roughly compatible with the ak135f average model for Qp and Qs.

* Syngine/Instaseis

  The very useful syngine web service hosted by IRIS,
  https://service.iris.edu/irisws/syngine/1/
  allows calculation of synthetic seismograms for several 1D earth models. The
  ak135f model used by Syngine appears to be a variant of the ak135favg model
  where the 3 km ocean and 0.3 km sediment layers have been replaced by
  velocities from the lower crustal layer, resulting in a 10 km thick
  crust with constant velocity. Note this is different from the ak135fcont
  model that includes a thicker, 35 km thick two layer crust and a slightly different
  uppermost mantle structure, from 35 to 120 km. Travel times from TauP
  for ak135f will thus be similar, but not exactly match the output of syngine.
  We have included a ak135fsyngine model with these modifications for
  compatibility.

* PREM

  The Qp values in prem.nd are all also derived from Qs via the equation above
  for ak135f, except for the inner core, where values near 600 are given but
  the equation from Qs predicts a Qp value closer to 430-445. Note that PREM
  gives a different Qkappa in the inner core from the rest of the model,
  which likely explains the difference.
