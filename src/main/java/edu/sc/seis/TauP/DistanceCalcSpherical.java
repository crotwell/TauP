package edu.sc.seis.TauP;

import net.sf.geographiclib.Geodesic;

public class DistanceCalcSpherical extends DistanceCalc {

    public static final DistanceCalcSpherical EARTH_SPHERE = new DistanceCalcSpherical(Geodesic.WGS84);

    public DistanceCalcSpherical(Geodesic geodesic) {
        super(geodesic);
    }

    @Override
    public String getCalcType() {
        return "spherical";
    }


    @Override
    public double angleBetweenRadian(double latA, double lonA, double depthKmA,
                                     double latB, double lonB, double depthKmB) {
        return SphericalCoords.distanceRadian(latA, lonA, latB, lonB);
    }

    @Override
    public double angleBetweenKm(double latA, double lonA, double depthKmA, double latB, double lonB, double depthKmB) {
        return angleBetweenRadian(latA, lonA, depthKmA, latB, lonB, depthKmB)
                *DistAzKarney.averageRadiusKm(getGeodesic());
    }

    @Override
    public double azimuth(double latA, double lonA, double depthKmA,
                          double latB, double lonB, double depthKmB) {
        return SphericalCoords.azimuth(latA, lonA, latB, lonB);
    }

    @Override
    public double[] latLonForAzimuth(double lat, double lon, double depthKm,
                                     double azimuth, double distdeg, double pointDepthKm) {
        double ptLat = SphericalCoords.latFor(lat, lon, distdeg, azimuth);
        double ptLon = SphericalCoords.lonFor(lat, lon, distdeg, azimuth);
        return new double[] {ptLat, ptLon, pointDepthKm};
    }
}
