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
     * Used by addToBranch when the path transmits up across a boundary then diffracts along that boundary.
     */
    TRANSUPDIFFRACT,

    /**
     * An upward turn after a flat diffracted segment, such as the upward leg of Pdiff. Similar to TURN, but
     * must ray start at bottom of layer.
     */
    DIFFRACTTURN,

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
            case END:
                isDowngoing = false;
                break;
            case TURN:
            case TRANSDOWN:
            case REFLECT_TOPSIDE:
            case REFLECT_TOPSIDE_CRITICAL:
            case END_DOWN:
                isDowngoing = true;
                break;
            case FAIL:
            case START:
                throw new IllegalArgumentException("End action cannot be FAIL or START: "+endAction);
            default:
                throw new IllegalArgumentException("End action case not yet impl: "+endAction);
        }
        return isDowngoing;
    }

    public static boolean isUpgoingActionAfter(PhaseInteraction endAction) {
        return ! isDowngoingActionAfter(endAction);
    }

    public static boolean isDowngoingActionAfter(PhaseInteraction endAction) {
        boolean isDowngoing;
        switch (endAction) {
            case TRANSUP:
            case REFLECT_TOPSIDE:
            case REFLECT_TOPSIDE_CRITICAL:
            case TURN:
            case DIFFRACTTURN:
            case END:
                isDowngoing = false;
                break;
            case TRANSDOWN:
            case REFLECT_UNDERSIDE:
            case REFLECT_UNDERSIDE_CRITICAL:
            case END_DOWN:
                isDowngoing = true;
                break;
            case FAIL:
            case START:
                throw new IllegalArgumentException("End action cannot be FAIL or START: "+endAction);
            default:
                throw new IllegalArgumentException("End action case not yet impl: "+endAction);
        }
        return isDowngoing;
    }

    public static int endOffset(PhaseInteraction endAction) {
        switch (endAction) {
            case TRANSUP:
            case TRANSUPDIFFRACT:
                return -1;
            case TRANSDOWN:
            case HEAD:
                return 1;
            default:
                return 0;
        }
    }
}
