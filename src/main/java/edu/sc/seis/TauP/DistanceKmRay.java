package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;

public class DistanceKmRay extends DistanceRay {

    public DistanceKmRay(double km) {
        this.kilometers = km;
    }

    @Override
    public double getDegrees() {
        double radius;
        if (isGeodetic()) {
            radius = RayCalculateable.averageRadiusKm(getGeodesic());
        } else {
            radius = getRadiusOfEarth();
        }
        return kilometers/DistAz.kmPerDeg(radius);
    }

    @Override
    public double getRadians() {
        double radius;
        if (isGeodetic()) {
            radius = RayCalculateable.averageRadiusKm(getGeodesic());
        } else {
            radius = getRadiusOfEarth();
        }
        return kilometers/radius;
    }

    @Override
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
