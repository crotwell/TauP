package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;

public class DistanceKmRay extends DistanceRay {

    public DistanceKmRay(double km) {
        this.kilometers = km;
    }

    @Override
    public double getDegrees(double radius) {
        if (isGeodetic()) {
            if (hasSource()  && hasAzimuth()) {
                Location evt = getSource().asLocation();
                GeodesicData gData = geodesic.Direct(evt.getLatitude(), evt.getLongitude(), getAzimuth(), kilometers*1000);
                return gData.a12;
            }
            if (hasReceiver() && hasBackAzimuth()) {
                Location sta = getReceiver().asLocation();
                GeodesicData gData = geodesic.Direct(sta.getLatitude(), sta.getLongitude(), getBackAzimuth(), kilometers*1000);
                return gData.a12;
            }
            // use mean radius, r = (2*er+pr)/3, mean of two equitorial radii and polar radius
            double polarRadius = geodesic.EquatorialRadius() * (1 - geodesic.Flattening());
            double meanRadius = (2*geodesic.EquatorialRadius()+polarRadius)/3;
            return kilometers/meanRadius;
        }
        // assume earth average 111.19
        return kilometers/DistAz.kmPerDeg(radius);
    }

    @Override
    public double getRadians(double radius) {
        return kilometers/radius;
    }

    @Override
    public double getKilometers(double radius) {
        return kilometers;
    }

    public double getKilometers() {
        return kilometers;
    }

    public String toString() {
        String out = "";
        out += kilometers + " km";
        if (hasDescription()) {
            out += ", "+getDescription();
        }
        return out;
    }

    protected DistanceKmRay duplicate()  {
        DistanceKmRay dr = DistanceKmRay.ofKilometers(kilometers);
        dr.copyFrom(this);
        return dr;
    }

    protected double kilometers;

}
