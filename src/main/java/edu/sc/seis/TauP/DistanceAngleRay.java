package edu.sc.seis.TauP;

public class DistanceAngleRay extends DistanceRay {

    public boolean isDegrees() {
        return this.degrees != null;
    }

    public double getDegrees(double radius) {
        return getDegrees();
    }

    public double getDegrees() {
        if (degrees != null) {
            return degrees;
        }
        if (radians != null) {
            return radians*SphericalCoords.rtod;
        }
        throw new RuntimeException("One of degrees or radians must be set");
    }

    public double getKilometers(double radius) {
        return getRadians(radius)*radius;
    }

    public double getRadians(double radius) {
        return getRadians();
    }

    public double getRadians() {
        if (radians != null) {
            return radians;
        }
        if (degrees != null) {
            return degrees*SphericalCoords.dtor;
        }
        throw new RuntimeException("One of degrees or radians must be set");
    }


    public String toString() {
        String out = "";
        if (radians != null) {
            out += radians+" rad";
        } else if (degrees != null) {
            out += degrees+" deg";
        }
        if (hasDescription()) {
            out += ", "+getDescription();
        }
        return out;
    }

    protected DistanceAngleRay duplicate()  {
        DistanceAngleRay dr;
        if (degrees != null) {
            dr = DistanceAngleRay.ofDegrees(degrees);
        } else {
            dr = DistanceAngleRay.ofRadians(degrees);
        }
        dr.copyFrom(this);
        return dr;
    }

    protected Double radians = null;
    protected Double degrees = null;
}
