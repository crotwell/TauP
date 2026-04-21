package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cmdline.args.GeodeticArgs;
import net.sf.geographiclib.Geodesic;

/**
 * Represents a scatterer within a model. Acts as an intermediary receiver and then source in calculation of a
 * scattered seismic phase.
 */
public class Scatterer {
    public Scatterer(double depth, double dist, GeoDistType geoDistType, Geodesic geodesic) {
        this(depth, DistanceRay.ofFixedHemisphereDegrees(dist, geoDistType, geodesic));
    }

    public Scatterer(double depth, FixedHemisphereDistanceRay dist) {
        this.depth = depth;
        this.dist = dist;
    }

    public double getDistanceDegree() {
        // we know created via ofDegrees, so do not need radius to calc
        return dist.getDegrees();
    }

    public final double depth;
    public final FixedHemisphereDistanceRay dist;

}
