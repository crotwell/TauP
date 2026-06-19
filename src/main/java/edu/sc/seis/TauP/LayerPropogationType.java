package edu.sc.seis.TauP;

/**
 * Possible ways a ray can propogate within a single layer. UP and DOWN are normal body waves, HEAD is flat at the top
 * and DIFF is flat at the bottom. SURFACE is special case for dealing with surface waves of given velocity.
 */
public enum LayerPropogationType {
    UP,
    DOWN,
    HEAD,
    DIFF,
    SURFACE;

    public static boolean isFlat(LayerPropogationType layerPropogationType) {
        return layerPropogationType == HEAD || layerPropogationType == DIFF || layerPropogationType == SURFACE;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
