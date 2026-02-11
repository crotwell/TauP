package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.*;

import static edu.sc.seis.TauP.SphericalCoords.DtoR;

/**
 * Distance calculations using
 * Karney, C.F.F. Algorithms for geodesics. J Geod 87, 43–55 (2013). https://doi.org/10.1007/s00190-012-0578-z
 *
 * https://github.com/geographiclib/geographiclib-java
 */
public class DistAzKarney {

    /**
     * First location is generally the source/event and second is the receiver/station.
     c getDelta()  Great Circle Arc distance in degrees
     c getAz()     Azimuth from loc1 to loc2 in degrees
     c getBaz()    Back Azimuth from loc2 to loc1 in degrees
     */
    public static GeodesicData calc(LatLonLocatable loc1, LatLonLocatable loc2){
        return calc(loc1.asLocation(), loc2.asLocation());
    }

    /**
     * First location is generally the source/event and second is the receiver/station.
     c getDelta()  Great Circle Arc distance in degrees
     c getAz()     Azimuth from loc1 to loc2 in degrees
     c getBaz()    Back Azimuth from loc2 to loc1 in degrees
     */
    public static GeodesicData calc(LatLonLocatable loc1, LatLonLocatable loc2, Geodesic planetGeodesic){
        return calc(loc1.asLocation(), loc2.asLocation(), planetGeodesic);
    }

    /**
     * First location is generally the source/event and second is the receiver/station.
     c getDelta()  Great Circle Arc distance in degrees
     c getAz()     Azimuth from loc1 to loc2 in degrees
     c getBaz()    Back Azimuth from loc2 to loc1 in degrees
     */
    public static GeodesicData calc(Location loc1, Location loc2){
        return calc(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(), loc2.getLongitude());
    }

    /**
     c getDelta()  Great Circle Arc distance in degrees
     c getAz()     Azimuth from loc1 to loc2 in degrees
     c getBaz()    Back Azimuth from loc2 to loc1 in degrees
     */
    public static GeodesicData calc(Location loc1, Location loc2, Geodesic planetGeodesic){
        return calc(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(), loc2.getLongitude(), planetGeodesic);
    }

    /**
     * Calc geodesic data, uses WSG84.
     *
     c lat1  Latitude of first point (+N, -S) in degrees
     c lon1  Longitude of first point (+E, -W) in degrees
     c lat2  Latitude of second point
     c lon2  Longitude of second point
     c
     */
    public static GeodesicData calc(double lat1, double lon1, double lat2, double lon2) {
        return calc(lat1, lon1, lat2, lon2, Geodesic.WGS84);
    }
    public static GeodesicData calc(double lat1, double lon1, double lat2, double lon2, Geodesic planetGeodesic){
        return planetGeodesic.Inverse(lat1, lon1, lat2, lon2);
    }

    public static GeodesicData calcLocationDeg(Location a,
                                               double azimuth,
                                               double degrees,
                                               Geodesic geod) {
        double km = degrees*DtoR* averageRadiusKm(geod);
        return calcLocationKm(a, km, azimuth, geod);
    }

    public static GeodesicData calcLocationKm(Location a,
                                            double kilometers,
                                            double azimuth,
                                            Geodesic geod) {
        GeodesicLine line = new GeodesicLine(geod, a.getLatitude(), a.getLongitude(), azimuth);
        GeodesicData geodesicData = line.Position(kilometers*1000);
        return geodesicData;
    }

    public static double averageRadiusKm(Geodesic geodesic) {
        // use mean radius, r = (2*er+pr)/3, mean of two equitorial radii and polar radius
        return geodesic.EquatorialRadius()* (3- geodesic.Flattening()) / 3 / 1000; // m to km
    }
}
