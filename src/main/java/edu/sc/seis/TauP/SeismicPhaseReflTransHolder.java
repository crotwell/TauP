package edu.sc.seis.TauP;

import static edu.sc.seis.TauP.SphericalCoords.RtoD;
import static edu.sc.seis.TauP.SphericalCoords.TWOPI;
import static java.lang.Math.PI;

public class SeismicPhaseReflTransHolder {

    Complex complexReflTran;
    double energyFlux;
    int internalCaustics = 0;
    public SeismicPhaseReflTransHolder(Complex complexReflTran, double energyFlux) {
        this(complexReflTran, energyFlux, 0);
    }

    public SeismicPhaseReflTransHolder(Complex complexReflTran, double energyFlux, int internalCaustics) {
        this.complexReflTran = complexReflTran;
        this.energyFlux = energyFlux;
        this.internalCaustics = internalCaustics;
    }

    public static SeismicPhaseReflTransHolder unity() {
        return new SeismicPhaseReflTransHolder(new Complex(1,0), 1.0);
    }

    public static SeismicPhaseReflTransHolder zero() {
        return new SeismicPhaseReflTransHolder(new Complex(0,0), 0.0);
    }

    public SeismicPhaseReflTransHolder times(SeismicPhaseReflTransHolder x) {
        return new SeismicPhaseReflTransHolder(
                this.complexReflTran.times(x.complexReflTran),
                this.energyFlux*x.energyFlux,
                this.internalCaustics+x.internalCaustics);
    }

    public double getPhase() {
        Complex intCaustic = Complex.pow(new Complex(0, 1), internalCaustics);
        double phaseRadians = (Complex.argument(complexReflTran.times(intCaustic)));
        while(phaseRadians > PI) {
            phaseRadians -= TWOPI;
        }
        while (phaseRadians < -1*PI) {
            phaseRadians+= TWOPI;
        }
        return phaseRadians;
    }

    public double getPhaseDeg() {
        return getPhase()*RtoD;
    }

    public int getInternalCaustics() {
        return internalCaustics;
    }

    static double PI_TWO = PI/2;
}
