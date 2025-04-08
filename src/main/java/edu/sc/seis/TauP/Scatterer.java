package edu.sc.seis.TauP;

/**
 * Represents a scatterer within a model. Acts as an intermediary receiver and then source in calculation of a
 * scattered seismic phase.
 */
public class Scatterer {
    public Scatterer(double depth, double dist) {
        this(depth, DistanceRay.ofFixedHemisphereDegrees(dist));
    }

    public Scatterer(double depth, FixedHemisphereDistanceRay dist) {
        this.depth = depth;
        this.dist = dist;
    }

    public double getDistanceDegree() {
        // we know created via ofDegrees, so do not need radius to calc
        return dist.getDegrees(0);
    }

    public final double depth;
    public final FixedHemisphereDistanceRay dist;

}
