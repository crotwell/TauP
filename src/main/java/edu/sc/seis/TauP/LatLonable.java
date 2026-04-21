package edu.sc.seis.TauP;

import net.sf.geographiclib.Geodesic;

/**
 * Provided by calculatable rays that have knowledge of lat and lon for the path.
 */
public abstract class LatLonable {

    public LatLonable(GeoDistType geoDistType, Geodesic geodesic) {
        this.geoDistType = geoDistType;
        this.geodesic = geodesic;
    }

    public abstract double[] calcLatLon(double calcDist, double totalDist, double depthKm);

    public boolean isGeodetic() {
        return geoDistType == GeoDistType.geodetic && this.geodesic != null;
    }

    public boolean isGeocentric() {
        return geoDistType == GeoDistType.geocentric && this.geodesic != null;
    }

    public boolean isSpherical() {
        return this.geodesic == null || GeoDistType.spherical == this.geoDistType ;
    }

    Geodesic geodesic = null;

    GeoDistType geoDistType = GeoDistType.spherical;
}
