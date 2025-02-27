package edu.sc.seis.TauP;

/**
 * Represents a scatterer within a model. Acts as an intermediary receiver and then source in calculation of a
 * scattered seismic phase.
 */
public class Scatterer {
    public Scatterer(double depth, double dist) {
        this(depth, FixedHemisphereDistanceRay.ofDegrees(dist));
    }

    public Scatterer(double depth, FixedHemisphereDistanceRay dist) {
        this.depth = depth;
        this.dist = dist;
    }

    public Double getDistanceDegree() {
        // we know created via ofDegrees, so do not need radius to calc
        return dist.degrees;
    }

    public final double depth;
    public final FixedHemisphereDistanceRay dist;


}
