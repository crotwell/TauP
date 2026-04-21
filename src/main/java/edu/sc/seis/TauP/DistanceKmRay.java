package edu.sc.seis.TauP;

import net.sf.geographiclib.Geodesic;

public class DistanceKmRay extends DistanceRay {

    // see DistanceRay.ofKilometers
    DistanceKmRay(double km, DistanceCalc distCalc) {
        super(distCalc);
        this.kilometers = km;
    }

    @Override
    public double getDegrees() {
        double radius = DistAzKarney.averageRadiusKm(getGeodesic());
        return kilometers/DistAz.kmPerDeg(radius);
    }

    @Override
    public double getRadians() {
        double radius = DistAzKarney.averageRadiusKm(getGeodesic());
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
        DistanceKmRay dr = DistanceKmRay.ofKilometers(kilometers, distCalc);
        dr.copyFrom(this);
        return dr;
    }

    protected double kilometers;

}
