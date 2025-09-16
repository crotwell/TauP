package edu.sc.seis.TauP;

import static edu.sc.seis.TauP.SphericalCoords.DtoR;
import static edu.sc.seis.TauP.SphericalCoords.RtoD;

public class SphericalCoordinate {
    public SphericalCoordinate(double phi, double theta) {
        this.phi = phi;
        this.theta = theta;
    }

    public static SphericalCoordinate fromAzTakeoffDegree(double azDeg, double takeoffDeg) {
        return new SphericalCoordinate(takeoffDegreeToPhiRadian(takeoffDeg), Math.PI/2-azDeg*DtoR);
    }

    public static SphericalCoordinate fromCartesian(double[] vector) {
        double r = Math.sqrt(vector[0]*vector[0]+vector[1]*vector[1]+vector[2]*vector[2]);
        double phi = Math.acos((vector[2]/r));
        double theta = Math.atan2(vector[1], vector[0]);
        return new SphericalCoordinate(phi, theta);
    }

    /**
     * Angle from north pole, 0 to pi.
     */
    double phi;

    /**
     * Angle from zero meridian, longitude, -2*PI to 2*PI.
     */
    double theta;


    public double getPhi() {
        return phi;
    }

    public double getTheta() {
        return theta;
    }

    public double getAzimuthRadian() {
        return Math.PI/2-theta;
    }

    public double getAzimuthDegree() {
        return SphericalCoords.RtoD*getAzimuthRadian();
    }

    public double getTakeoffAngleDegree() {
        return phiToTakeoffToDegree(phi);
    }

    public double stereoR() {
        return Math.sin(phi)/(1-Math.cos(phi));
    }

    public String toString() {
        return "Az: "+getAzimuthDegree()+" TO: "+getTakeoffAngleDegree()+" (phi:"+getPhi()+" theta:"+getTheta()+")";
    }

    public static double takeoffDegreeToPhiRadian(double takeoffDeg) {
        return Math.PI-takeoffDeg*DtoR;
    }
    public static double phiToTakeoffToRadian(double phi) {
        return Math.PI-phi;
    }
    public static double phiToTakeoffToDegree(double phi) {
        return phiToTakeoffToRadian(phi)*RtoD;
    }

    public static double[] crossProduct(double[] a, double[] b) {
        return new double[] {
                a[1]*b[2] - a[2]*b[1],
                a[2]*b[0] - a[0]*b[2],
                a[0]*b[1] - a[1]-b[0]
        };
    }

}
