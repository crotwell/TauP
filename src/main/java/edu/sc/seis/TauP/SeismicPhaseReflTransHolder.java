package edu.sc.seis.TauP;

import static edu.sc.seis.TauP.SphericalCoords.RtoD;
import static edu.sc.seis.TauP.SphericalCoords.TWOPI;
import static java.lang.Math.PI;

public class SeismicPhaseReflTransHolder {

    Complex complexReflTran;
    double energyFlux;
    int internalCaustics = 0;
    int turns = 0;
    public SeismicPhaseReflTransHolder(Complex complexReflTran, double energyFlux) {
        this(complexReflTran, energyFlux,  0,0);
    }

    public SeismicPhaseReflTransHolder(Complex complexReflTran, double energyFlux, int turns, int internalCaustics) {
        this.complexReflTran = complexReflTran;
        this.energyFlux = energyFlux;
        this.turns = turns;
        this.internalCaustics = internalCaustics;
    }

    public static SeismicPhaseReflTransHolder unity() {
        return new SeismicPhaseReflTransHolder(new Complex(1,0), 1.0, 0, 0);
    }

    public static SeismicPhaseReflTransHolder zero() {
        return new SeismicPhaseReflTransHolder(new Complex(0,0), 0.0, 0, 0);
    }

    public SeismicPhaseReflTransHolder times(SeismicPhaseReflTransHolder x) {
        return new SeismicPhaseReflTransHolder(
                this.complexReflTran.times(x.complexReflTran),
                this.energyFlux*x.energyFlux,
                this.turns+x.turns,
                this.internalCaustics+x.internalCaustics);
    }

    public double getPhase() {
        // each internal caustic is 90 shift (pi/2)
        // should each turn is like a 180 (down becomes up)?
        Complex intCaustic = Complex.pow(new Complex(0, 1), internalCaustics); // +2*turns
        double phaseRadians = (Complex.argument(complexReflTran.times(intCaustic)));
        while(phaseRadians > PI) {
            phaseRadians -= TWOPI;
        }
        while (phaseRadians <= -1*PI) {
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
