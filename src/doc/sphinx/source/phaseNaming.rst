.. _phasenaming:

====================
Phases
====================

A major feature of the TauP Toolkit is the implementation of a phase name parser
that allows the user to define essentially arbitrary phases through the earth.
Thus, the TauP Toolkit is extremely flexible in this respect since it is
not limited to a pre-defined set of phases.
Phase names are not hard-coded into the software, rather the names are interpreted
and the appropriate propagation path and resulting times are constructed at run time.
Designing a phase-naming convention that is general enough to support arbitrary phases
and easy to understand is an essential and somewhat challenging step.
The rules that we have developed are described here.
Most of phases resulting from these conventions should
be familiar to seismologists, e.g. pP, PP, PcS, PKiKP, etc.
However, the uniqueness required for parsing results in some new names for other
familiar phases.

In traditional `whole-earth` seismic phase names, there are 3 major
interfaces:  the free surface, the core-mantle boundary,
and the inner-outer core boundary.
Phases interacting with the core-mantle boundary and the inner core boundary are easy to
describe because the symbol for the wave type changes at the boundary (i.e. the symbol P
changes to K within the outer core even though the wave type is the same).
Phase multiples for these interfaces and the free surface are also easy to describe because
the symbols describe a unique path.
The challenge begins with the description of interactions with interfaces within the
crust and upper mantle.
We have introduced new symbols to existing
nomenclature to provide unique descriptions of potential paths.
Phase names are constructed from a sequence of symbols and numbers (with no spaces)
that either describe the wave type, the interaction a wave makes with an interface, or
the depth to an interface involved in an interaction.


1. Symbols that describe wave-type:

    ===========   ================================================================
    :code:`P`     compressional wave, upgoing or downgoing, in the crust or mantle
    :code:`p`     strictly upgoing P wave in the crust or mantle
    :code:`Ped`   compressional wave, exclusively downgoing, in the crust or mantle
    :code:`S`     shear wave, upgoing or downgoing, in the crust or mantle
    :code:`s`     strictly upgoing S wave in the crust or mantle
    :code:`Sed`   shear wave, exclusively downgoing, in the crust or mantle
    :code:`K`     compressional wave in the outer core
    :code:`k`     strictly upgoing compressional wave in the outer core
    :code:`Ked`   compressional wave, exclusively downgoing, in the outer core
    :code:`I`     compressional wave in the inner core
    :code:`y`     strictly upgoing compressional wave in the inner core
    :code:`Ied`   compressional, exclusively downgoing, wave in the inner core
    :code:`J`     shear wave in the inner core
    :code:`j`     strictly upgoing shear wave in the inner core
    :code:`Jed`   shear wave, exclusively downgoing, wave in the inner core
    ===========   ================================================================

2. Symbols that describe interactions with interfaces:

    ============   ================================================================
    :code:`m`      interaction with the moho
    :code:`g`      appended to P or S to represent a ray turning in the crust
    :code:`n`      appended to P or S to represent a head wave, e.g. along the moho
    :code:`c`      interaction with the core mantle boundary
    :code:`i`      interaction with the inner core outer core boundary
    :code:`^`      underside reflection, used primarily for crustal and mantle interfaces
    :code:`v`      topside reflection, used primarily for crustal and mantle interfaces
    :code:`V`      critical topside reflection, used primarily for crustal and mantle interfaces
    :code:`diff`   appended to P or S to represent a diffracted wave, e.g. along the core mantle boundary, or to K for a diffracted wave along the inner-outer core boundary
    :code:`kmps`   appended to a velocity to represent a horizontal phase velocity
    :code:`_`      delimits a named discontinuty within a phase
    ============   ================================================================

3.  Exclusively upgoing and downgoing:

    The characters :code:`p`, :code:`s`,
    :code:`k`, :code:`y` and :code:`j` **always** represent
    up-going legs.

    An example is the source to surface leg of the phase :code:`pP`
    from a source at depth.
    :code:`P` and :code:`S` can be turning waves, but
    always indicate downgoing waves leaving the source when they are the first symbol in a
    phase name.
    Thus, to get near-source, direct P-wave arrival times, you need to specify two
    phases :code:`p` and :code:`P` or use the :code:`ttimes` compatibility phases described
    below.
    However, :code:`P` may
    represent a upgoing leg in certain cases.
    For instance, :code:`PcP` is
    allowed since the direction of the phase is unambiguously determined by the symbol
    :code:`c`, but would be named :code:`Pcp` by a purist using our nomenclature. The phase
    :code:`k` is similar to :code:`p` but is an upgoing compressional wave in the outer core, while :code:`y`
    and :code:`j` are upgoing compressional and shear waves
    in the inner core. The name :code:`y` is used as the
    lower case of the :code:`I` phase is already used to indicate
    reflection from the inner-outer core boundary.

    With the ability to have sources at depth, there is a need to specify the difference between a wave that is
    exclusively downgoing to the receiver from one that turns and is upgoing at the receiver. The suffix :code:`ed`
    can be appended to indicate exclusively downgoing. So for a source at 10 km depth and a receiver at 20 km depth
    at 0 degree distance the turning ray :code:`P` does not have an arrival but :code:`Ped` does.

4.  Depths within the model:

    Numbers, except velocities for :code:`kmps`
    phases (see 11 below),
    represent depths at which interactions take place.
    For example, :code:`P410s` represents a P-to-S conversion at a discontinuity at 410km
    depth.
    Since the S-leg is given by a lower-case symbol and no reflection indicator is
    included, this represents a P-wave  converting to an S-wave when it hits the interface
    from below.
    The numbers given need not be the actual depth, the closest depth corresponding to a
    discontinuity in the model will be used, within a tolerance of 10 km.
    For example, if the time for :code:`P410s` is requested in a model where the discontinuity
    was really located at 406.7 kilometers depth, the time returned would actually be for
    :code:`P406.7s`.
    The code `taup time` would note that this had been done via the *Purist Name*.
    Obviously, care should be taken to ensure that there are no other discontinuities
    closer than the one of interest, but this approach allows generic interface
    names like `410` and `660` to be used without knowing the exact depth in a given
    model. Use of a depth number greater than 10 km from any discontinuity
    will result in a failed phase name.

    In addition, models given in the *named discontinuities* format,
    the name for a discontinuity can be used instead of the depth within a phase.
    The name is pre and postpended by an underscore, :code:`_`, and can only
    contain letters, numbers and the dash symbol, :code:`-`.
    For example, if a model contained a discontinuity at 1000 km depth that
    was named `mid-mantle`, then the phase :code:`Pv1000p` and
    :code:`Pv_mid-mantle_p` would both refer to the same phase, which reflects
    off of the top of the discontinuity at 1000 km depth. The standard
    discontinuity names can also be used in this manner, with
    :code:`Pvmp` and :code:`Pv_moho_p` being the same phase.

5.  Conversion at depth:

    If a number appears between two phase legs, e.g. :code:`S410P`,
    it represents a transmitted phase conversion, not a reflection.
    Thus, :code:`S410P` would be a transmitted conversion
    from :code:`S` to :code:`P` at 410km depth.
    Whether the conversion occurs
    on the down-going side or up-going side is determined by the upper or lower
    case of the following leg.
    For instance, the phase :code:`S410P`
    propagates down as an :code:`S`, converts at the 410
    to a :code:`P`, continues down, turns as a P-wave, and propagates back across the
    410 and to the surface.
    :code:`S410p` on the other hand, propagates down
    as a :code:`S` through the 410, turns as an :code:`S`,
    hits the 410 from the bottom, converts to a :code:`p` and then goes up to the surface.
    In these cases, the case of the phase symbol (P vs. p) is critical because the direction
    of propagation (upgoing or downgoing) is not unambiguously defined elsewhere in the
    phase name.
    The importance is clear when you consider a source depth below 410 compared to above 410.
    For a source depth greater than 410 km, :code:`S410P` technically cannot exist while
    :code:`S410p` maintains the same path (a receiver side conversion) as it does for a
    source depth above the 410.

    The first letter can be lower case to indicate a conversion from
    an up-going ray, e.g. :code:`p410S` is a depth phase from
    a source at greater than 410 kilometers depth that phase converts
    at the 410 discontinuity.
    It is strictly upgoing over
    its entire path, and hence could also be labeled :code:`p410s`.
    :code:`p410S` is often used to mean a reflection in the literature, but there
    are too many possible interactions for the phase parser to allow this.
    If the underside reflection is desired, use the :code:`p\^410S` notation from
    rule 7.

6.  Non-conversion conversions:

    Due to the two previous rules, :code:`P410P` and :code:`S410S`
    are over specified, but still legal.
    They are almost equivalent to :code:`P` and :code:`S`, respectively,
    but restrict the path to phases transmitted through (turning below) the 410.
    This notation is useful to
    limit arrivals to just those that turn deeper than a discontinuity (thus avoiding
    travel time curve triplications), even though they have no real interaction with it.

7.  Reflections:

    The characters :code:`\^`, :code:`v` and :code:`V` are new symbols introduced here to
    represent bottom-side and top-side reflections, respectively.
    They are followed by a number to
    represent the approximate depth of the reflection or
    a letter for standard discontinuities, :code:`m`, :code:`c` or :code:`i`.
    The lower-case :code:`v` represents a generic reflection while :code:`V` is
    a critical reflection. Note however, that  :code:`V` is critical in the sense of
    without phase conversion. In other words, :code:`PVmp` is critical for ray parameters
    where a P wave cannot propagate into the mantle, regardless of whether
    or not S can propagate. A critical reflection phase using :code:`V` is always
    a subset of the non-critical reflection using :code:`v`.
    Reflections from discontinuities besides the
    core-mantle boundary, :code:`c`;
    or inner-core outer-core boundary, :code:`i`, must use the :code:`\^`
    and :code:`v` notation.
    For instance, in the TauP convention, :code:`p\^410S` is used to describe
    a near-source underside reflection from the 410 discontinuity.

    Underside reflections, except at the
    surface (:code:`PP`, :code:`sS`, etc.),
    core-mantle boundary (:code:`PKKP`, :code:`SKKKS`, etc.), or
    outer-core-inner-core boundary (:code:`PKIIKP`, :code:`SKJJKS`,
    :code:`SKIIKS`, etc.), must
    be specified with the :code:`\^` notation.
    For example, :code:`P\^410P` and
    :code:`P\^mP` would both be underside
    reflections from the 410km discontinuity and the Moho, respectively.
    Because of the difficultly of creating interfaces where critical underside reflections
    can occur in earth-like models, we have not added this capability.

    The phase :code:`PmP`, the traditional name for a top-side reflection from the Moho
    discontinuity, must change names under our new convention.
    The new name is :code:`PvmP` or :code:`PVmp`
    while :code:`PmP` just describes a P-wave that turns beneath the Moho.
    The reason the Moho must be handled differently from the core-mantle boundary is that
    traditional nomenclature did not introduce a phase symbol change at the Moho.
    Thus, while :code:`PcP` makes sense since a P-wave in the core would be labeled
    :code:`K`, :code:`PmP` could have several meanings.
    The :code:`m` symbol just allows the user to describe phases interaction with the Moho
    without knowing its exact depth.
    In all other respects, the :code:`\^`-:code:`v` nomenclature is maintained.

8.  Core reflections:

    Starting in version 3.0, :code:`\^` and :code:`v` are now allowed
    for for all discontinuities in the model, including
    the crust, mantle and core.
    However, because
    ":code:`p` is to :code:`P`" is not the same as
    ":code:`i` is to :code:`I`",
    a naming convention was created to use :code:`y` as an exclusively
    upgoing leg in the inner core. For example in a model with a discontinuity
    at 5500 km depth in the inner core, the phases
    :code:`PKIv5500IKP` and :code:`PKIv5500ykp` are the same. Note that
    because standard models do not have discontinuities in the core, these
    phases have not received the same level of testing.

9   Scattered phases:

    Starting in version 3.0, :code:`o` and :code:`O` represent forward and back
    scattering, if the model is constructed with a scatterer (depth and distance).
    Forward scattering is in the sense that the phase continues around the earth
    in the same direction, while backscattering would reverse direction.
    Note that for some phases that go the long way around the earth, the sense of
    scattering may not match the positive angular direction.

10  Core phase names:

    Currently there is no support for :code:`PKPab`, :code:`PKPbc`,
    or :code:`PKPdf` phase names.
    They lead to increased algorithmic complexity that at this point seems
    unwarranted, and TauP uses phase names to describe paths, but
    :code:`PKPab` and :code:`PKPbc` differentiate between two arrivals from
    the same path, :code:`PKP`.
    Currently, in regions where triplications develop, the triplicated phase will have multiple
    arrivals at a given distance.
    So, :code:`PKPab` and :code:`PKPbc` are
    both labeled just :code:`PKP` while :code:`PKPdf` is called :code:`PKIKP`.

11  Surface waves:

    The symbol :code:`kmps` is used to get the travel time for a
    specific horizontal phase velocity.
    For example, :code:`2kmps` represents a horizontal phase
    velocity of 2 kilometers per second.
    While the calculations for these are trivial, it is convenient
    to have them available to estimate surface wave travel times or to define windows of
    interest for given paths.

12  ttimes compatibility:

    As a convenience, a :code:`ttimes` phase name compatibility mode is available.
    So :code:`ttp` gives
    you the phase list corresponding to :code:`P` in :code:`ttimes`.
    Similarly there are :code:`tts`, :code:`ttp+`,
    :code:`tts+`, :code:`ttbasic` and :code:`ttall`.

The :code:`taup phase` tool can be very helpful in understanding the phase
naming convention by providing a detailed description of the path a phase
takes through the model.
It is also possible to generate a list of all possible phase paths within
a model, using the :code:`taup find` tool. This takes a
:code:`--max n` argument that specifies the maximum number of interactions
that the phase has with discontinuities in the model, other than start,
end and transmission without phase change.
