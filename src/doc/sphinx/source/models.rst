
===================================
Creating and Saving Velocity Models
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
by whitespace. They are, in order, depth in kilometers to the sample point,
P velocity in kilometers per second,
S velocity in kilometers per second,
density in grams per cubic centimeter,
$Q_p$ attenuation for compressional waves and
$Q_s$ attenuation for shear waves.
Only  depth, $V_p$ and $V_s$ are required.
The remaining parameters, while not needed for travel time
calculations, are included to allow the model to be used for other purposes
in the future. The model is assumed to be linear between given depths and
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
discontinuity does the phase \texttt{PvmP} reflect?
Explicit labeling eliminates potential ambiguity.

=================     =============================================
Labels                Description
=================     =============================================
mantle, moho          moho, crust-mantle boundary
outer-core, cmb       mantle-core boundary
inner-core, icocb     inner outer core boundary
ice                   top of ice layer
ice-ocean             ice above ocean boundary
ice-crust             ice above crust boundary
ocean                 top of ocean layer
=================     =============================================

One further enhancement to these model file formats is the support for comments
embedded within the model files. As in shell scripting, everything after
a :code:`\#` on a line is ignored. In addition, *C* style :code:`/* ... */`
and :code:`// ...` comments are recognized.

A very simple \textit{named discontinuities} model file might look like this:
.. include:: verysimple.nd

In many cases it is better and easier to make use of taup velmerge
to create a new model by making changes to an existing global model,
especially when for example the user only cares about crust and upper mantle
structure and is happy with an existing global model for
the lower mantle and core. Hand editing velocity model files
often results in hard to catch errors.

----------------------
Using Saved Tau Models
----------------------

There are three ways of finding a previously generated model file. First, as
a standard model as part of the distribution. Second, a list of directories and jar files to
be searched can be specified with the taup.model.path property.
Lastly, the path to the actual model file may be specified.
TauP searches each of these
places in order until it finds a model that matches the name.

* Standard Model.

  TauP first checks to see if the model name is associated with a standard model.
  Several standard models are included within the distributed jar file.
  They include

  ===========  =============================
  iasp91       :cite:t:`iasp`
  prem         :cite:t:`dziewonski_anderson`
  ak135        :cite:t:`kennett:ak135`
  ak135favg    :cite:t:`kennett:ak135f`
  ak135fcont   :cite:t:`kennett:ak135f`
  jb           :cite:t:`jb`
  1066a        :cite:t:`gilbert_dziewonski`
  1066b        :cite:t:`gilbert_dziewonski`
  pwdk         :cite:t:`weber_davis`
  sp6          :cite:t:`morelli`
  herrin       :cite:t:`herrin`
  ===========  =============================

  Lastly, we have included qdt, which is a coarsely sampled version of iasp91 :cite:t:`iasp`.
  It is samller, and thus loads quicker, but has significantly reduced accuracy.
  We will consider adding other models to the distribution if
  they are of wide interest.
  They are included within the distribution jar file but
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
  set the taup.model.path in a properties file. See section :ref:`properties` for
  more details.

  The taup.model.path property is constructed in the manner of standard Java CLASSPATH
  which is itself based loosely on the manner of the *Unix* PATH.
  The only real
  differences between CLASSPATH and PATH are that a jar file
  may be placed directly in the path and the path separator character
  is machine dependent, *Unix* is ``:`` but other systems may vary.

  The taup.model.path allows you to have directories containing saved model files
  as well as jar files of models.
  For instance, in a \textsc{Unix} system using the c shell,
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
  always check there, see :ref:`cwdmodel` below, but it may be used to
  change the search order.

* .. _cwdmodel:
  The last place TauP looks is for a model file specified
  on the command line.
  So, if you generate newModel.taup and want to get some times, you can just say:
  ``taup time -mod newModel.taup``
  or even just
  ``taup time -mod newModel``
  as TauP can add the taup suffix if necessary. A relative or absolute pathname
  may precede the model, e.g.
  ``taup time -mod ../OtherDir/newModel.taup``.
  New in version 2.0 is the ability of the tools to load a velocity model directly
  and handle the create functionality internall, so in addition to ``.taup`` files,
  the ``.nd`` and ``.tvel`` model files can be loaded if there is not a ``.taup`` file found.
  Note that there is extra work involved in processing the velocity file, and so
  frequently used models should still be converted using * to avoid
  reprocessing them each time the tool starts.
