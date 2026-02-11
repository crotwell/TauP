package edu.sc.seis.TauP;

public class DistanceKmRay extends DistanceRay {

    public DistanceKmRay(double km) {
        this.kilometers = km;
    }

    @Override
    public double getDegrees() {
        double radius;
        if (isGeodetic()) {
            radius = DistAzKarney.averageRadiusKm(getGeodesic());
        } else {
            radius = getRadiusOfEarth();
        }
        return kilometers/DistAz.kmPerDeg(radius);
    }

    @Override
    public double getRadians() {
        double radius;
        if (isGeodetic()) {
            radius = DistAzKarney.averageRadiusKm(getGeodesic());
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
