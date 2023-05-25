package edu.sc.seis.TauP;

public enum PhaseInteraction {
    START,

    /**
     * Used by addToBranch when the path turns within a segment. We assume that
     * no ray will turn downward so turning implies turning from downward to
     * upward, ie U.
     */
    TURN,

    /**
     * Used by addToBranch when the path reflects off the top of the end of a
     * segment, ie ^.
     */
    REFLECT_UNDERSIDE,

    /**
     * Used by addToBranch when the path reflects off the bottom of the end of a
     * segment, ie v.
     */
    REFLECT_TOPSIDE,

    /**
     * Used by addToBranch when the path transmits up through the end of a
     * segment.
     */
    TRANSUP,

    /**
     * Used by addToBranch when the path transmits down through the end of a
     * segment.
     */
    TRANSDOWN,

    /**
     * Used by addToBranch when the path diffracts along a boundary.
     */
    DIFFRACT,

    /**
     * Used by addToBranch when the path is head wave along a boundary.
     */
    HEAD,

    /**
     * Used by addToBranch when the path is surface wave, so just a velocity.
     */
    KMPS,

    /**
     * indicates end of the inbound phase to a scatterer. The ray continues on after scattering in the
     * forward sense, so if inbound clockwise, continues clockwise.
     *
     */
    SCATTER,

    /**
     * indicates end of the inbound phase to a scatterer, where last segment is downgoing.
     * The ray continues on after scattering in the
     * forward sense, so if inbound clockwise, continues clockwise.
     *
     */
    SCATTER_DOWN,

    /**
     * indicates end of the inbound phase to a scatterer. The ray continues on after scattering in the
     * backward sense, so if inbound clockwise, it reverses and propigates counterclockwise.
     *
     */
    BACKSCATTER,

    /**
     * indicates end of the inbound phase to a scatterer, where last segment is downgoing.
     * The ray continues on after scattering in the
     * backward sense, so if inbound clockwise, it reverses and propigates counterclockwise.
     *
     */
    BACKSCATTER_DOWN,

    /**
     * Used by addToBranch for the last segment of a phase.
     */
    END,

    /**
     * Used by addToBranch for the last segment of a phase ending downgoing, like Ped to receiver at depth.
     */
    END_DOWN,

    /**
     * Used by addToBranch when the path critically reflects off the top of the end of a
     * segment, ie "^x". Note this is disabled as it is hard to create a model where this
     * phase interaction is physically possible, delay implement this feature for now.
     */
    REFLECT_UNDERSIDE_CRITICAL,

    /**
     * Used by addToBranch when the path critically reflects off the bottom of the end of a
     * segment, ie "V".
     */
    REFLECT_TOPSIDE_CRITICAL,

    FAIL
}
