package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonSimple;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.Geodesic;

import static edu.sc.seis.TauP.SphericalCoords.rtod;

public abstract class DistanceCalc {

    public DistanceCalc(Geodesic geodesic) {
        this.geodesic = geodesic;
    }

    public static DistanceCalc create(GeoDistType geoDistType, Geodesic geodesic) {
        switch (geoDistType) {
            case geocentric:
                return new DistanceCalcGeocentric(geodesic);
            case geodetic:
                return new DistanceCalcGeodetic(geodesic);
            case spherical:
                return new DistanceCalcSpherical(geodesic);
            default:
                throw new IllegalArgumentException("Unknown GeoDistType: "+geoDistType);
        }
    }

    public double angleBetweenDeg(Location locA, Location locB) {
        return angleBetweenDeg(locA.getLatitude(), locA.getLongitude(), depthKm(locA),
                locB.getLatitude(), locB.getLongitude(), depthKm(locB));
    }

    public double angleBetweenDeg(double latA, double lonA, double depthKmA,
                                  double latB, double lonB, double depthKmB) {
        return rtod*angleBetweenRadian( latA,  lonA,  depthKmA,  latB,  lonB,  depthKmB);
    }

    public abstract double angleBetweenRadian(double latA, double lonA, double depthKmA,
                                              double latB, double lonB, double depthKmB);


    public double angleBetweenKm(Location locA, Location locB) {
        return angleBetweenKm(locA.getLatitude(), locA.getLongitude(), depthKm(locA),
                locB.getLatitude(), locB.getLongitude(), depthKm(locB));
    }

    public abstract double angleBetweenKm(double latA, double lonA, double depthKmA,
                                              double latB, double lonB, double depthKmB);

    public abstract double azimuth(double latA, double lonA, double depthKmA,
                                   double latB, double lonB, double depthKmB);

    public double azimuth(Location locA, Location locB) {
        return azimuth(locA.getLatitude(), locA.getLongitude(), depthKm(locA),
                locB.getLatitude(), locB.getLongitude(), depthKm(locB));
    }

    public abstract double[] latLonForAzimuth(double lat, double lon, double depthKm,
                                              double azimuth, double distdeg, double pointDepthKm);

    public LatLonSimple locForAzimuthDeg(Location loc, double azimuth, double distdeg, double pointDepthKm) {
        double[] pt = latLonForAzimuth(loc.getLatitude(), loc.getLongitude(), depthKm(loc),
                azimuth, distdeg, pointDepthKm);
        return new LatLonSimple(pt[0], pt[1], pt[2]);
    }
    public Geodesic getGeodesic() {
        return geodesic;
    }

    public static double depthKm(Location loc) {
        return loc.hasDepth() ? loc.getDepthKm() : 0;
    }

    Geodesic geodesic;

    public abstract String getCalcType();
}
