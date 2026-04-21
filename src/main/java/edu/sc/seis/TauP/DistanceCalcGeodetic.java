package edu.sc.seis.TauP;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;

public class DistanceCalcGeodetic extends DistanceCalc {

    public DistanceCalcGeodetic(Geodesic geodesic) {
        super(geodesic);
    }

    @Override
    public String getCalcType() {
        return "geodetic";
    }

    @Override
    public double angleBetweenRadian(double latA, double lonA, double depthKmA, double latB, double lonB, double depthKmB) {
        double avgRadius = DistAzKarney.averageRadiusKm(geodesic);
        double distKm = angleBetweenKm(latA, lonA, depthKmA, latB, lonB, depthKmB);
        return distKm/avgRadius;
    }

    @Override
    public double angleBetweenKm(double latA, double lonA, double depthKmA, double latB, double lonB, double depthKmB) {
        GeodesicLine azGLine = geodesic.InverseLine(latA, lonA, latB, lonB);
        double distKm = azGLine.Distance()/1000;
        return distKm;
    }

    @Override
    public double azimuth(double latA, double lonA, double depthKmA, double latB, double lonB, double depthKmB) {
        GeodesicLine azGLine = geodesic.InverseLine(latA, lonA, latB, lonB);
        return azGLine.Azimuth();
    }

    @Override
    public double[] latLonForAzimuth(double lat, double lon, double depthKm, double azimuth, double distdeg, double pointDepthKm) {
        double avgRadius = DistAzKarney.averageRadiusKm(geodesic);
        double km = distdeg*SphericalCoords.dtor*avgRadius;
        double meters = km*1000;
        GeodesicLine gLine = geodesic.DirectLine(lat, lon, azimuth, meters);
        GeodesicData point = gLine.Position(meters);
        return new double[] {point.lat2, point.lon2, pointDepthKm};
    }
}
