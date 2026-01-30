package edu.sc.seis.TauP;

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
        return Complex.argument(complexReflTran) +internalCaustics*PI_TWO;
    }

    public int getInternalCaustics() {
        return internalCaustics;
    }

    static double PI_TWO = Math.PI/2;
}
