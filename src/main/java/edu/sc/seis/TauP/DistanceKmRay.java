package edu.sc.seis.TauP;

public class DistanceKmRay extends DistanceRay {

    public DistanceKmRay(double km) {
        this.kilometers = km;
    }

    @Override
    public double getDegrees(double radius) {
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
