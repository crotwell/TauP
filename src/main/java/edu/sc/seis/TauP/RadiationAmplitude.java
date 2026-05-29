package edu.sc.seis.TauP;

public class RadiationAmplitude {

    public RadiationAmplitude() {
        this.coord = new SphericalCoordinate(0,0);
        this.radialAmplitude = 1;
        this.phiAmplitude = 1;
        this.thetaAmplitude = 1;
    }
    public RadiationAmplitude(SphericalCoordinate coord, double[] radPSvSh) {
        this.coord = coord;
        this.radialAmplitude = radPSvSh[0];
        this.phiAmplitude = radPSvSh[1];
        this.thetaAmplitude = radPSvSh[2];
    }

    /**
     * Create amplitude for point on sphere
     * @param coord spherical direction
     * @param radialAmp P amplitude
     * @param thetaAmp Sv amplitude
     * @param phiAmp Sh amplitude
     */
    public RadiationAmplitude(SphericalCoordinate coord, double radialAmp, double thetaAmp, double phiAmp) {
        this.coord = coord;
        this.radialAmplitude = radialAmp;
        this.thetaAmplitude = thetaAmp;
        this.phiAmplitude = phiAmp;
    }

    public SphericalCoordinate getCoord() {
        return coord;
    }

    /**
     * P amplitude.
     * @return amplitude
     */
    public double getRadialAmplitude() {
        return radialAmplitude;
    }

    /**
     * Sv amplitude.
     * @return amplitude
     */
    public double getPhiAmplitude() {
        return phiAmplitude;
    }

    /**
     * Sh amplitude.
     * @return amplitude
     */
    public double getThetaAmplitude() {
        return thetaAmplitude;
    }

    /**
     * Gets total S amplitude, sqrt of Sv*Sv+Sh*Sh
     * @return total S amp
     */
    public double getSTotalAmplitude() {
        return Math.sqrt(thetaAmplitude*thetaAmplitude+phiAmplitude*phiAmplitude);
    }

    public String toString() {
        return getCoord()+" ["+getRadialAmplitude()+", "+getPhiAmplitude()+", "+getThetaAmplitude()+" ]";
    }

    SphericalCoordinate coord;
    double radialAmplitude;
    double thetaAmplitude;
    double phiAmplitude;
}
