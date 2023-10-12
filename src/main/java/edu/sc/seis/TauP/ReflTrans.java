package edu.sc.seis.TauP;

public abstract class ReflTrans {

    public abstract Complex getComplexRpp(double rayParam) throws VelocityModelException;

    public abstract Complex getComplexRps(double rayParam) throws VelocityModelException;

    public abstract Complex getComplexTpp(double rayParam) throws VelocityModelException;

    public abstract Complex getComplexTps(double rayParam) throws VelocityModelException;

    public abstract Complex getComplexRsp(double rayParam) throws VelocityModelException;

    public abstract Complex getComplexRss(double rayParam) throws VelocityModelException;

    public abstract Complex getComplexTsp(double rayParam) throws VelocityModelException;

    public abstract Complex getComplexTss(double rayParam) throws VelocityModelException;

    public abstract Complex getComplexRshsh(double rayParam) throws VelocityModelException;

    public abstract Complex getComplexTshsh(double rayParam) throws VelocityModelException;

    /**
     * Flips the sense of the layers, useful when you have a ray going through
     * the same layer in the opposite direction.
     */
    public abstract ReflTrans flip() throws VelocityModelException;

    /**
     * Calculates incident P wave to reflected P wave coefficient.
     */
    public double getRpp(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexRpp(rayParam));
    }

    /**
     * Calculates incident P wave to reflected SV wave coefficient.
     */
    public double getRps(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexRps(rayParam));
    }

    /**
     * Calculates incident P wave to transmitted P wave coefficient.
     */
    public double getTpp(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexTpp(rayParam));
    }

    /**
     * Calculates incident P wave to transmitted SV wave coefficient.
     */
    public double getTps(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexTps(rayParam));
    }

    /**
     * Calculates incident SV wave to reflected P wave coefficient.
     */
    public double getRsp(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexRsp(rayParam));
    }

    /**
     * Calculates incident SV wave to reflected SV wave coefficient.
     */
    public double getRss(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexRss(rayParam));
    }

    /**
     * Calculates incident SV wave to transmitted P wave coefficient.
     */
    public double getTsp(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexTsp(rayParam));
    }

    /**
     * Calculates incident SV wave to transmitted SV wave coefficient.
     */
    public double getTss(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexTss(rayParam));
    }

    /**
     * Calculates incident SH wave to reflected SH wave coefficient.
     */
    public double getRshsh(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexRshsh(rayParam));
    }

    /**
     * Calculates incident SH wave to transmitted SH wave coefficient.
     */
    public double getTshsh(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexTshsh(rayParam));
    }

    public static double getRealCoefficient(Complex coef) {
        if (Math.abs(coef.im) > 1e-6) {
            System.err.println(coef.re+" i"+coef.im);
            return Complex.abs(coef);
        }
        return coef.re;
    }


    public double[] calcCriticalRayParams() {
        // shoudl filter NaN?
        double[] criticalSlownesses = new double[] {1/topVp, 1/botVp, 1/topVs, 1/botVs};
        return criticalSlownesses;
    }

    protected double topVp;

    protected double topVs;

    protected double topDensity;

    protected double botVp;

    protected double botVs;

    protected double botDensity;

    // "flat earth" ray parameter
    protected double rp;

    // store the vertical slownesses for both the top and bottom halfspaces
    // for both P and S waves
    protected Complex topVertSlownessP, topVertSlownessS;

    protected Complex botVertSlownessP, botVertSlownessS;

    // we need the squared terms so often that it is worthwhile to store them
    protected double sqBotVs; // botVs squared

    protected double sqTopVs; // topVs squared

    protected double sqBotVp; // botVp squared

    protected double sqTopVp; // topVp squared

    protected double sqRP; // rp squared

    // remember last calculated ray param and wave type to avoid repeating
    protected double lastRayParam = -1.0;

    protected boolean lastInIsPWave = true;


    // IMPORTANT!!!!
    // Where ever "CX" appears in this class, it is used as a shorthand for
    // the Complex class, so CX.times() is the same as Complex.times, but
    // the code is, IMHO, less cluttered.
    /** just to avoid having Complex all over the place. */
    protected static final Complex CX = new Complex();

}
