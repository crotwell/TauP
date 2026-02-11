package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;

/**
 * Distance, azimuth and back azimuth between two lat,lon pairs.
 */
public class DistAzLambert {

    public static final double wgs85_invflattening = 298.257223563;
    public static final double wgs85_flattening = 1/wgs85_invflattening;

    public static final double wgs85_meanEarthRadius = 6371.0088;

    /**
     * First location is generally the source/event and second is the receiver/station.
     c getDelta()  Great Circle Arc distance in degrees
     c getAz()     Azimuth from loc1 to loc2 in degrees
     c getBaz()    Back Azimuth from loc2 to loc1 in degrees
     */
    public DistAzLambert(LatLonLocatable loc1, LatLonLocatable loc2){
        this(loc1.asLocation(), loc2.asLocation());
    }

    /**
     * First location is generally the source/event and second is the receiver/station.
     c getDelta()  Great Circle Arc distance in degrees
     c getAz()     Azimuth from loc1 to loc2 in degrees
     c getBaz()    Back Azimuth from loc2 to loc1 in degrees
     */
    public DistAzLambert(LatLonLocatable loc1, LatLonLocatable loc2, double flattening){
        this(loc1.asLocation(), loc2.asLocation(), flattening);
    }

    /**
     * First location is generally the source/event and second is the receiver/station.
     c getDelta()  Great Circle Arc distance in degrees
     c getAz()     Azimuth from loc1 to loc2 in degrees
     c getBaz()    Back Azimuth from loc2 to loc1 in degrees
     */
    public DistAzLambert(Location loc1, Location loc2){
        this(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(), loc2.getLongitude());
    }

    /**
     c getDelta()  Great Circle Arc distance in degrees
     c getAz()     Azimuth from loc1 to loc2 in degrees
     c getBaz()    Back Azimuth from loc2 to loc1 in degrees
     */
    public DistAzLambert(Location loc1, Location loc2, double flattening){
        this(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(), loc2.getLongitude(), flattening);
    }

    /**
     c lat1  Latitude of first point (+N, -S) in degrees
     c lon1  Longitude of first point (+E, -W) in degrees
     c lat2  Latitude of second point
     c lon2  Longitude of second point
     c
     c getDelta()  Great Circle Arc distance in degrees
     c getAz()     Azimuth from pt. 1 to pt. 2 in degrees
     c getBaz()    Back Azimuth from pt. 2 to pt. 1 in degrees
     */
    public DistAzLambert(double lat1, double lon1, double lat2, double lon2) {
        this(lat1, lon1, lat2, lon2, wgs85_flattening);
    }
    public DistAzLambert(double lat1, double lon1, double lat2, double lon2, double flattening){
        this.evtlat = lat1;
        this.evtlon = lon1;
        this.stalat = lat2;
        this.stalon = lon2;
        if ((lat1 == lat2)&&(lon1 == lon2)) {
            delta = 0.0;
            az = 0.0;
            baz = 0.0;
            return;
        }
        double sph,rad;

        rad=2.*Math.PI/360.0;
        /*
         c
         c scolat and ecolat are the reduces colatitudes
         c via Andoyer-Lambert
         c
         c Earth Flattening of 1/298.257 take from Bott (pg. 3)
         c
         */
        //sph=1.0/298.257;
        sph = flattening;

        double slat = stalat*rad;
        double elat = evtlat*rad;
        double sredlat= Math.atan((1.-sph)*Math.tan(stalat*rad));
        double eredlat= Math.atan((1.-sph)*Math.tan(evtlat*rad));
        double slon=stalon*rad;
        double elon=evtlon*rad;

        //
        // https://en.wikipedia.org/wiki/Geographical_distance#Lambert's_formula_for_long_lines
        //
        double P = (eredlat+sredlat)/2;
        double Q = (sredlat-eredlat)/2;
        double haversine = (1-Math.cos(sredlat-eredlat)+Math.cos(eredlat)*Math.cos(sredlat)*(1-Math.cos(slon-elon)))/2;
        double sigma = 2*Math.asin(Math.sqrt(haversine));
        double sinP = Math.sin(P);
        double cosP = Math.cos(P);
        double sinQ = Math.sin(Q);
        double cosQ = Math.cos(Q);
        double cos_sigma_2 = Math.cos(sigma/2);
        double sin_sigma_2 = Math.sin(sigma/2);
        double X = (sigma - Math.sin(sigma))*sinP*sinP*cosQ*cosQ/(cos_sigma_2*cos_sigma_2);
        double Y = (sigma + Math.sin(sigma))*cosP*cosP*sinQ*sinQ/(sin_sigma_2*sin_sigma_2);
        double D = (sigma - sph/2 * (X+Y));

        delta=D/rad;

        // https://en.wikipedia.org/wiki/Azimuth#In_geodesy
        double e2 = sph*(2-sph);
        double lambda = (1-e2)*Math.tan(slon)/Math.tan(elon) + e2*Math.sqrt((1+(1-e2)*Math.tan(slon)*Math.tan(slon))/ (1+(1-e2)*Math.tan(elon)*Math.tan(elon)));
        double daz = Math.atan2(Math.sin(slat), (lambda-Math.cos(slat))*Math.tan(slon));
        if (daz<0.0) {
            daz=daz+2*Math.PI;
        }
        az = daz/rad;

        double bazlambda = (1-e2)*Math.tan(elon)/Math.tan(slon) + e2*Math.sqrt((1+(1-e2)*Math.tan(elon)*Math.tan(elon))/ (1+(1-e2)*Math.tan(slon)*Math.tan(slon)));
        double dbaz = Math.atan2(Math.sin(elat), (bazlambda-Math.cos(elat))*Math.tan(elon));
        if (dbaz<0.0) {
            dbaz=dbaz+2*Math.PI;
        }
        baz=dbaz/rad;
        /*
         c
         c   Make sure 0.0 is always 0.0, not 360.
         c
         */
        if(Math.abs(baz-360.) < .00001) baz=0.0;
        if(Math.abs(az-360.) < .00001) az=0.0;

    }

    public double getDelta() { return delta; }

    public double getAz() { return az; }

    public double getBaz() { return baz; }
    
    public double getRadialAzimuth() {
        return (180 + getBaz()) % 360;
    }
    
    public double getTransverseAzimuth() {
        return (270 + getBaz()) % 360;
    }

    public boolean equals(Object o){
        if(this == o){ return true; }
        else if(o instanceof DistAzLambert){
            DistAzLambert oAz = (DistAzLambert)o;
            if(oAz.stalat == stalat && oAz.stalon == stalon &&
               oAz.evtlat == evtlat && oAz.evtlon == evtlon){
                return true;
            }
        }
        return false;
    }

    public int hashCode(){
        int result = 24;
        result = 37*result + Double.hashCode(stalat);
        result = 37*result + Double.hashCode(stalon);
        result = 37*result + Double.hashCode(evtlat);
        result = 37*result + Double.hashCode(evtlon);
        return result;
    }


    private final double delta;
    private double az;
    private double baz;
    private final double stalat, stalon, evtlat, evtlon;

    public static double kmPerDeg() {
        return kmPerDeg(wgs85_meanEarthRadius);
    }
    public static double kmPerDeg(double radius) {
        return Math.PI*radius/180.0;
    }
    public static double degreesToKilometers(double degrees) {
        return degrees * kmPerDeg();
    }
    public static double degreesToKilometers(double degrees, double radius) {
        return degrees * kmPerDeg(radius);
    }
    public static double kilometersToDegrees(double kilometers, double radius) {
        return kilometers / kmPerDeg(radius);
    }

}
