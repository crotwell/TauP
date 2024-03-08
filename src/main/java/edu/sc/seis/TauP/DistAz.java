package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;
import edu.sc.seis.seisFile.fdsnws.quakeml.Event;
import edu.sc.seis.seisFile.fdsnws.quakeml.Origin;
import edu.sc.seis.seisFile.fdsnws.stationxml.Channel;
import edu.sc.seis.seisFile.fdsnws.stationxml.Station;

public class DistAz {

    /**
     c getDelta() => Great Circle Arc distance in degrees
     c getAz()    => Azimuth from channel to event in degrees
     c getBaz()   => Back Azimuth from event to channel in degrees
     */
    public DistAz(Channel chan, Event ev){
        this(chan, ev.getPreferredOrigin());
    }

    /**
     c getDelta() => Great Circle Arc distance in degrees
     c getAz()    => Azimuth from channel to origin in degrees
     c getBaz()   => Back Azimuth from origin to channel in degrees
     */
    public DistAz(Channel chan, Origin origin){
        this(chan.asLocation(), origin.asLocation());
    }

    /**
     c getDelta() => Great Circle Arc distance in degrees
     c getAz()    => Azimuth from channel to origin in degrees
     c getBaz()   => Back Azimuth from origin to channel in degrees
     */
    public DistAz(Channel chan, Location eventLoc){
        this(chan.asLocation(), eventLoc);
    }

    /**
     c getDelta() => Great Circle Arc distance in degrees
     c getAz()    => Azimuth from loc1 to loc2 in degrees
     c getBaz()   => Back Azimuth from loc2 to loc1 in degrees
     */
    public DistAz(Location loc1, Location loc2){
        this(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(), loc2.getLongitude());
    }

    /**
     c lat1 => Latitude of first point (+N, -S) in degrees
     c lon1 => Longitude of first point (+E, -W) in degrees
     c lat2 => Latitude of second point
     c lon2 => Longitude of second point
     c
     c getDelta() => Great Circle Arc distance in degrees
     c getAz()    => Azimuth from pt. 1 to pt. 2 in degrees
     c getBaz()   => Back Azimuth from pt. 2 to pt. 1 in degrees
     */
    public DistAz(double lat1, double lon1, double lat2, double lon2){
        this.stalat = lat1;
        this.stalon = lon1;
        this.evtlat = lat2;
        this.evtlon = lon2;
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
        sph=1.0/298.257;

        scolat=Math.PI/2.0 - Math.atan((1.-sph)*(1.-sph)*Math.tan(lat1*rad));
        ecolat=Math.PI/2.0 - Math.atan((1.-sph)*(1.-sph)*Math.tan(lat2*rad));
        slon=lon1*rad;
        elon=lon2*rad;
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
        if(!hashSet){
            int result = 24;
            result = 37*result + hashDouble(stalat);
            result = 37*result + hashDouble(stalon);
            result = 37*result + hashDouble(evtlat);
            result = 37*result + hashDouble(evtlon);
            hash = result;
        }
        return hash;
    }

    private static int hashDouble(double d){
        long bits = Double.doubleToLongBits(d);
        return (int)(bits^(bits>>>32));
    }

    private double delta, az, baz;
    private double stalat, stalon, evtlat, evtlon;
    private int hash;
    private boolean hashSet = false;

    public static double degreesToKilometers(double degrees) {
        return degrees * 111.19;
    }
    public static double kilometersToDegrees(double kilometers) {
        return kilometers / 111.19;
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: java DistAz sta_lat sta_lon evt_lat evt_lon");
            System.out.println("       Returns:  Delta Baz Az");
        } else {
            double stalat = Double.valueOf(args[0]).doubleValue();
            double stalon = Double.valueOf(args[1]).doubleValue();
            double evtlat = Double.valueOf(args[2]).doubleValue();
            double evtlon = Double.valueOf(args[3]).doubleValue();

            DistAz distaz = new DistAz(stalat, stalon, evtlat, evtlon);
            System.out.println("   dist="+distaz.delta+"   baz="+distaz.baz+
                                   "   az="+distaz.az);
        } // end of else
    }
}
