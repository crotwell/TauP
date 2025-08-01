package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;

/**
 * Distance, azimuth and back azimuth between two lat,lon pairs.
 */
public class DistAz {

    public static final double wgs85_invflattening = 298.257223563;
    public static final double wgs85_flattening = 1/wgs85_invflattening;

    public static final double wgs85_meanEarthRadius = 6371.0088;

    /**
     * First location is generally the source/event and second is the receiver/station.
     c getDelta()  Great Circle Arc distance in degrees
     c getAz()     Azimuth from loc1 to loc2 in degrees
     c getBaz()    Back Azimuth from loc2 to loc1 in degrees
     */
    public DistAz(LatLonLocatable loc1, LatLonLocatable loc2){
        this(loc1.asLocation(), loc2.asLocation());
    }

    /**
     * First location is generally the source/event and second is the receiver/station.
     c getDelta()  Great Circle Arc distance in degrees
     c getAz()     Azimuth from loc1 to loc2 in degrees
     c getBaz()    Back Azimuth from loc2 to loc1 in degrees
     */
    public DistAz(LatLonLocatable loc1, LatLonLocatable loc2, double flattening){
        this(loc1.asLocation(), loc2.asLocation(), flattening);
    }

    /**
     * First location is generally the source/event and second is the receiver/station.
     c getDelta()  Great Circle Arc distance in degrees
     c getAz()     Azimuth from loc1 to loc2 in degrees
     c getBaz()    Back Azimuth from loc2 to loc1 in degrees
     */
    public DistAz(Location loc1, Location loc2){
        this(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(), loc2.getLongitude());
    }

    /**
     c getDelta()  Great Circle Arc distance in degrees
     c getAz()     Azimuth from loc1 to loc2 in degrees
     c getBaz()    Back Azimuth from loc2 to loc1 in degrees
     */
    public DistAz(Location loc1, Location loc2, double flattening){
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
    public DistAz(double lat1, double lon1, double lat2, double lon2) {
        this(lat1, lon1, lat2, lon2, wgs85_flattening);
    }
    public DistAz(double lat1, double lon1, double lat2, double lon2, double flattening){
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
        double scolat, slon, ecolat, elon;
        double a,b,c,d,e,aa,bb,cc,dd,ee,g,gg,h,hh,k,kk;
        double rhs1,rhs2,sph,rad,del,daz,dbaz;

        rad=2.*Math.PI/360.0;
        /*
         c
         c scolat and ecolat are the geocentric colatitudes
         c as defined by Richter (pg. 318)
         c
         c Earth Flattening of 1/298.257 take from Bott (pg. 3)
         c
         */
        //sph=1.0/298.257;
        sph = flattening;

        scolat=Math.PI/2.0 - Math.atan((1.-sph)*(1.-sph)*Math.tan(stalat*rad));
        ecolat=Math.PI/2.0 - Math.atan((1.-sph)*(1.-sph)*Math.tan(evtlat*rad));
        slon=stalon*rad;
        elon=evtlon*rad;
        /*
         c
         c  a - e are as defined by Bullen (pg. 154, Sec 10.2)
         c     These are defined for the pt. 1
         c
         */
        a=Math.sin(scolat)*Math.cos(slon);
        b=Math.sin(scolat)*Math.sin(slon);
        c=Math.cos(scolat);
        d=Math.sin(slon);
        e=-Math.cos(slon);
        g=-c*e;
        h=c*d;
        k=-Math.sin(scolat);
        /*
         c
         c  aa - ee are the same as a - e, except for pt. 2
         c
         */
        aa=Math.sin(ecolat)*Math.cos(elon);
        bb=Math.sin(ecolat)*Math.sin(elon);
        cc=Math.cos(ecolat);
        dd=Math.sin(elon);
        ee=-Math.cos(elon);
        gg=-cc*ee;
        hh=cc*dd;
        kk=-Math.sin(ecolat);
        /*
         c
         c  Bullen, Sec 10.2, eqn. 4
         c
         */
        del=Math.acos(a*aa + b*bb + c*cc);
        delta=del/rad;
        /*
         c
         c  Bullen, Sec 10.2, eqn 7 / eqn 8
         c
         c    pt. 1 is unprimed, so this is technically the baz
         c
         c  Calculate baz this way to avoid quadrant problems
         c
         */
        rhs1=(aa-d)*(aa-d)+(bb-e)*(bb-e)+cc*cc - 2.;
        rhs2=(aa-g)*(aa-g)+(bb-h)*(bb-h)+(cc-k)*(cc-k) - 2.;
        dbaz=Math.atan2(rhs1,rhs2);
        if (dbaz<0.0) {
            dbaz=dbaz+2*Math.PI;
        }
        baz=dbaz/rad;
        /*
         c
         c  Bullen, Sec 10.2, eqn 7 / eqn 8
         c
         c    pt. 2 is unprimed, so this is technically the az
         c
         */
        rhs1=(a-dd)*(a-dd)+(b-ee)*(b-ee)+c*c - 2.;
        rhs2=(a-gg)*(a-gg)+(b-hh)*(b-hh)+(c-kk)*(c-kk) - 2.;
        daz=Math.atan2(rhs1,rhs2);
        if(daz<0.0) {
            daz=daz+2*Math.PI;
        }
        az=daz/rad;
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
        else if(o instanceof DistAz){
            DistAz oAz = (DistAz)o;
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
