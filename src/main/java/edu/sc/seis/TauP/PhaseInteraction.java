package edu.sc.seis.TauP;

public enum PhaseInteraction {
    START_UP,
    START_DOWN,
    START_FLAT,

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
     * Used by addToBranch when the path transmits up across a boundary then diffracts along that boundary.
     */
    TRANSUPDIFFRACT,

    /**
     * An upward turn after a flat diffracted segment, such as the upward leg of Pdiff. Similar to TURN, but
     * must ray start at bottom of layer.
     */
    DIFFRACTTURN,

    /**
     * A downward turn after a flat diffracted segment, such as the K leg of PdiffdnKS
     */
    DIFFRACTDOWN,

    /**
     * Used by addToBranch when the path is head wave along a boundary.
     */
    HEAD,
    /**
     * An upward turn after a flat head segment, such as the upward leg of Pn. Similar to TRANSUP, as
     * must ray start at top of lower layer.
     */
    HEADTURN,

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
     * backward sense, so if inbound clockwise, it reverses and propagates counterclockwise.
     *
     */
    BACKSCATTER,

    /**
     * indicates end of the inbound phase to a scatterer, where last segment is downgoing.
     * The ray continues on after scattering in the
     * backward sense, so if inbound clockwise, it reverses and propagates counterclockwise.
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

    FAIL;


    public static boolean isUpgoingActionBefore(PhaseInteraction endAction) {
        return ! isDowngoingActionBefore(endAction);
    }

    public static boolean isDowngoingActionBefore(PhaseInteraction endAction) {
        boolean isDowngoing;
        switch (endAction) {
            case TRANSUP:
            case REFLECT_UNDERSIDE:
            case REFLECT_UNDERSIDE_CRITICAL:
            case TRANSUPDIFFRACT:
            case END:
                isDowngoing = false;
                break;
            case TURN:
            case TRANSDOWN:
            case REFLECT_TOPSIDE:
            case REFLECT_TOPSIDE_CRITICAL:
            case DIFFRACT:
            case HEAD:
            case END_DOWN:
                isDowngoing = true;
                break;
            case DIFFRACTTURN:
            case DIFFRACTDOWN:
            case HEADTURN:
            case KMPS:
                throw new IllegalArgumentException("End action is flat before, not up or down: "+endAction);
            case FAIL:
            case START_DOWN:
            case START_UP:
            case START_FLAT:
                throw new IllegalArgumentException("End action cannot be FAIL or START: "+endAction);
            default:
                throw new IllegalArgumentException("End action case not yet impl: "+endAction);
        }
        return isDowngoing;
    }

    public static boolean isUpgoingActionAfter(PhaseInteraction endAction) {
        return ! isDowngoingActionAfter(endAction);
    }

    public static LayerPropogationType layerPropogationTypeAfter(PhaseInteraction endAction) {
        return switch (endAction) {
            case START_DOWN, TRANSDOWN, REFLECT_UNDERSIDE, REFLECT_UNDERSIDE_CRITICAL, END_DOWN, DIFFRACTDOWN,
                 SCATTER_DOWN, BACKSCATTER_DOWN -> LayerPropogationType.DOWN;
            case START_UP, TRANSUP, REFLECT_TOPSIDE, REFLECT_TOPSIDE_CRITICAL,
                 TURN, DIFFRACTTURN, HEADTURN, END,
                 SCATTER, BACKSCATTER -> LayerPropogationType.UP;
            case START_FLAT, DIFFRACT, TRANSUPDIFFRACT -> LayerPropogationType.DIFF;
            case HEAD -> LayerPropogationType.HEAD;
            case KMPS -> LayerPropogationType.SURFACE;
            case FAIL -> LayerPropogationType.DOWN; // doesn't matter
        };
    }

    public static boolean isDowngoingActionAfter(PhaseInteraction endAction) {
        boolean isDowngoing;
        switch (endAction) {
            case TRANSUP:
            case REFLECT_TOPSIDE:
            case REFLECT_TOPSIDE_CRITICAL:
            case TURN:
            case DIFFRACTTURN:
            case HEADTURN:
            case START_UP:
            case END:
                isDowngoing = false;
                break;
            case TRANSDOWN:
            case REFLECT_UNDERSIDE:
            case REFLECT_UNDERSIDE_CRITICAL:
            case DIFFRACTDOWN:
            case END_DOWN:
            case START_DOWN:
                isDowngoing = true;
                break;
            case FAIL:
            case START_FLAT:
                throw new IllegalArgumentException("End action cannot be FAIL or START_FLAT: "+endAction);
            default:
                throw new IllegalArgumentException("End action case not yet impl: "+endAction);
        }
        return isDowngoing;
    }

    public static int endOffset(PhaseInteraction endAction) {
        switch (endAction) {
            case TRANSUP:
            case HEADTURN:
            case TRANSUPDIFFRACT:
                return -1;
            case TRANSDOWN:
            case DIFFRACTDOWN:
            case HEAD:
                return 1;
            default:
                return 0;
        }
    }
}
