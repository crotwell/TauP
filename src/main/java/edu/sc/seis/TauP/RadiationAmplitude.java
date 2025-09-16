package edu.sc.seis.TauP;

public class RadiationAmplitude {

    public RadiationAmplitude(SphericalCoordinate coord, double[] radPSvSh) {
        this.coord = coord;
        this.radialAmplitude = radPSvSh[0];
        this.phiAmplitude = radPSvSh[1];
        this.thetaAmplitude = radPSvSh[2];
    }

    public RadiationAmplitude(SphericalCoordinate coord, double radialAmp, double thetaAmp, double phiAmp) {
        this.coord = coord;
        this.radialAmplitude = radialAmp;
        this.thetaAmplitude = thetaAmp;
        this.phiAmplitude = phiAmp;
    }

    public SphericalCoordinate getCoord() {
        return coord;
    }

    public double getRadialAmplitude() {
        return radialAmplitude;
    }

    public double getThetaAmplitude() {
        return thetaAmplitude;
    }

    public double getPhiAmplitude() {
        return phiAmplitude;
    }

    public String toString() {
        return getCoord()+" ["+getRadialAmplitude()+", "+getPhiAmplitude()+", "+getThetaAmplitude()+" ]";
    }

    SphericalCoordinate coord;
    double radialAmplitude;
    double thetaAmplitude;
    double phiAmplitude;
}
