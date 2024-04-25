package edu.sc.seis.TauP.cli;

import edu.sc.seis.TauP.FixedHemisphereDistanceRay;

public class Scatterer {
    public Scatterer(double depth, double dist) {
        this(depth, FixedHemisphereDistanceRay.ofDegrees(dist));
    }

    public Scatterer(double depth, FixedHemisphereDistanceRay dist) {
        this.depth = depth;
        this.dist = dist;
    }

    public final double depth;
    public final FixedHemisphereDistanceRay dist;


}
