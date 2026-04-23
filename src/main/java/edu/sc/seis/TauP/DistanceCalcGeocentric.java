package edu.sc.seis.TauP;

import net.sf.geographiclib.Geodesic;

public class DistanceCalcGeocentric extends DistanceCalc {

    public DistanceCalcGeocentric(Geodesic geodesic) {
        super(geodesic);
        this.geocentric = new Geocentric(geodesic);
    }

    @Override
    public String getCalcType() {
        return "geocentric";
    }

    @Override
    public double angleBetweenRadian(double latA, double lonA, double depthKmA,
                                     double latB, double lonB, double depthKmB) {
        // geocentric uses meters
        return geocentric.angleBetweenRadian(latA, lonA, depthKmA*1000, latB, lonB, depthKmB*1000);
    }

    @Override
    public double angleBetweenKm(double latA, double lonA, double depthKmA, double latB, double lonB, double depthKmB) {
        return angleBetweenRadian(latA, lonA, depthKmA, latB, lonB, depthKmB)
                *DistAzKarney.averageRadiusKm(getGeodesic());
    }

    @Override
    public double azimuth(double latA, double lonA, double depthKmA,
                          double latB, double lonB, double depthKmB) {
        return geocentric.azimuth(latA, lonA, depthKmA*1000, latB, lonB, depthKmB*1000);
    }

    @Override
    public double[] latLonForAzimuth(double lat, double lon, double depthKm,
                                     double azimuth, double distdeg, double pointDepthKm) {
        double[] lld = geocentric.latLonForAzimuth(lat, lon, depthKm*1000, azimuth, distdeg, pointDepthKm*1000);
        lld[2] /= 1000; // m to km
        return lld;
    }

    Geocentric geocentric;
}
