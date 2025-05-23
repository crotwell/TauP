package edu.sc.seis.TauP;

import static edu.sc.seis.TauP.SphericalCoords.RtoD;

/**
 * Abstract class for reflection and transmission coefficients at a boundary of some kind.
 */
public abstract class ReflTrans {

    public ReflTrans(double topVp,
                     double topVs,
                     double topDensity,
                     double botVp,
                     double botVs,
                     double botDensity) {
        this.topVp = topVp;
        this.topVs = topVs;
        this.topDensity = topDensity;
        this.botVp = botVp;
        this.botVs = botVs;
        this.botDensity = botDensity;
        this.sqBotVs = botVs * botVs; // botVs squared
        this.sqTopVs = topVs * topVs; // topVs squared
        this.sqBotVp = botVp * botVp; // botVp squared
        this.sqTopVp = topVp * topVp; // topVp squared
    }
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
            return Complex.abs(coef);
        }
        return coef.re;
    }

    public Complex calcInVerticalSlownessP(double flatRP) {
        return Complex.sqrt(new Complex(1.0 / sqTopVp - flatRP*flatRP));
    }

    public Complex calcInVerticalSlownessS(double flatRP) {
        return Complex.sqrt(new Complex(1.0 / sqTopVs - flatRP*flatRP));
    }

    public Complex calcTransVerticalSlownessP(double flatRP) {
        return Complex.sqrt(new Complex(1.0 / sqBotVp - flatRP*flatRP));
    }

    public Complex calcTransVerticalSlownessS(double flatRP) {
        return Complex.sqrt(new Complex(1.0 / sqBotVs - flatRP*flatRP));
    }

    public double[] calcCriticalRayParams() {
        // shoudl filter NaN?
        return new double[] {1/topVp, 1/botVp, 1/topVs, 1/botVs};
    }

    public double getAngleR_p(double flatRP) {
        double cosTopVp = Math.sqrt(1 - flatRP * flatRP * topVp * topVp);
        return Math.acos(cosTopVp)*RtoD;
    }
    public double getAngleR_s(double flatRP) {
        double cosTopVs = Math.sqrt(1 - flatRP * flatRP * topVs * topVs);
        return Math.acos(cosTopVs)*RtoD;
    }
    public double getAngleT_p(double flatRP) {
        double cosBotVp = Math.sqrt(1 - flatRP * flatRP * botVp * botVp);
        return Math.acos(cosBotVp)*RtoD;
    }
    public double getAngleT_s(double flatRP) {
        double cosBotVs = Math.sqrt(1-flatRP*flatRP*botVs*botVs);
        return Math.acos(cosBotVs)*RtoD;
    }

    public double inboundEnergyP(double flatRP) {
        calcTempVars(flatRP, true);
        if (this.topVertSlownessP.isInfinite()) {
            // horizontal ray, inbound energy is zero?
            return 0.0;
        }
        double cos_j1 = Complex.abs(this.topVertSlownessP) * topVp;
        return topDensity*topVp*cos_j1;
    }

    public double inboundEnergyS(double flatRP) {
        calcTempVars(flatRP, false);
        if (this.topVertSlownessP.isInfinite()) {
            // horizontal ray, inbound energy is zero?
            return 0.0;
        }
        double cos_j1 = Complex.abs(this.topVertSlownessS) * topVs;
        return topDensity*topVs*cos_j1;
    }

    public double getEnergyFluxRpp(double flatRP) throws VelocityModelException {
        double eff =  getEnergyFluxFactorRpp(flatRP);
        return eff*eff;
    }
    public double getEnergyFluxFactorRpp(double flatRP) throws VelocityModelException {
        if (1/flatRP == topVp) {
            // in horizontal case, flux is zero, so no change in path energy as turn doesn't lose energy
            return 1.0;
        }
        // careful sqrt neg number due to small rounding error
        double cosArg = 1-flatRP*flatRP* topVp* topVp;
        if (cosArg < 0) {return 0.0;}
        double inboundEnergy = inboundEnergyP(flatRP);
        if (inboundEnergy == 0) { return 0;}
        double Rpp_calc = getRpp(flatRP);
        return Rpp_calc * Math.sqrt(topDensity * topVp * Math.sqrt(cosArg)
                / inboundEnergy);
    }


    public double getEnergyFluxTpp(double flatRP) throws VelocityModelException {
        double eff =  getEnergyFluxFactorTpp(flatRP);
        return eff*eff;
    }
    public double getEnergyFluxFactorTpp(double flatRP) throws VelocityModelException {
        // careful sqrt neg number due to small rounding error
        double cosArg = 1-flatRP*flatRP* botVp*botVp;
        if (cosArg < 0) {return 0.0;}
        double inboundEnergy = inboundEnergyP(flatRP);
        if (inboundEnergy == 0.0) { return 0;}
        double Tpp_calc = getTpp(flatRP);
        return Tpp_calc * Math.sqrt(botDensity * botVp * Math.sqrt(cosArg)
                / inboundEnergy);
    }


    public double getEnergyFluxRps(double flatRP) throws VelocityModelException {
        double eff =  getEnergyFluxFactorRps(flatRP);
        return eff*eff;
    }
    public double getEnergyFluxFactorRps(double flatRP) throws VelocityModelException {
        if (topVs == 0.0) { return 0;}
        // careful sqrt neg number due to small rounding error
        double cosArg = 1-flatRP*flatRP* topVs*topVs;
        if (cosArg < 0) {return 0.0;}
        double inboundEnergy = inboundEnergyP(flatRP);
        if (inboundEnergy == 0) { return 0;}
        double Rps_calc = getRps(flatRP);
        return Rps_calc * Math.sqrt(topDensity * topVs * Math.sqrt(cosArg)
                / inboundEnergy);
    }


    public double getEnergyFluxTps(double flatRP) throws VelocityModelException {
        double eff =  getEnergyFluxFactorTps(flatRP);
        return eff*eff;
    }
    public double getEnergyFluxFactorTps(double flatRP) throws VelocityModelException {
        if (botVs == 0.0) { return 0;}
        // careful sqrt neg number due to small rounding error
        double cosArg = 1-flatRP*flatRP* botVs*botVs;
        if (cosArg < 0) {return 0.0;}
        double inboundEnergy = inboundEnergyP(flatRP);
        if (inboundEnergy == 0) { return 0;}
        double Tps_calc = getTps(flatRP);
        return Tps_calc * Math.sqrt(botDensity * botVs * Math.sqrt(cosArg)
                / inboundEnergy);
    }



    public double getEnergyFluxRsp(double flatRP) throws VelocityModelException {
        double eff =  getEnergyFluxFactorRsp(flatRP);
        return eff*eff;
    }
    public double getEnergyFluxFactorRsp(double flatRP) throws VelocityModelException {
        if ( topVs == 0.0) { return 0;}
        // careful sqrt neg number due to small rounding error
        double cosArg = 1-flatRP*flatRP* topVp* topVp;
        if (cosArg < 0) {return 0.0;}
        double inboundEnergy = inboundEnergyS(flatRP);
        if (inboundEnergy == 0) { return 0;}
        double Rsp_calc = getRsp(flatRP);
        return Rsp_calc * Math.sqrt(topDensity * topVp * Math.sqrt(cosArg)
                / inboundEnergy);
    }


    public double getEnergyFluxTsp(double flatRP) throws VelocityModelException {
        double eff =  getEnergyFluxFactorTsp(flatRP);
        return eff*eff;
    }
    public double getEnergyFluxFactorTsp(double flatRP) throws VelocityModelException {
        if ( topVs == 0.0) { return 0;}
        // careful sqrt neg number due to small rounding error
        double cosArg = 1-flatRP*flatRP* botVp* botVp;
        if (cosArg < 0) {return 0.0;}
        double inboundEnergy = inboundEnergyS(flatRP);
        if (inboundEnergy == 0) { return 0;}
        double Tsp_calc = getTsp(flatRP);
        return Tsp_calc * Math.sqrt(botDensity * botVp * Math.sqrt(cosArg)
                / inboundEnergy);
    }


    public double getEnergyFluxRss(double flatRP) throws VelocityModelException {
        double eff =  getEnergyFluxFactorRss(flatRP);
        return eff*eff;
    }
    public double getEnergyFluxFactorRss(double flatRP) throws VelocityModelException {
        if ( topVs == 0.0 ) { return 0;}
        if (1/flatRP == topVs) {
            // in horizontal case, flux is zero, so no change in path energy as turn doesn't lose energy
            return 1.0;
        }
        // careful sqrt neg number due to small rounding error
        double cosArg = 1-flatRP*flatRP* topVs* topVs;
        if (cosArg < 0) {return 1.0;}
        double inboundEnergy = inboundEnergyS(flatRP);
        if (inboundEnergy == 0) { return 1.0;}
        double Rss_calc = getRss(flatRP);
        return Rss_calc * Math.sqrt(topDensity * topVs * Math.sqrt(cosArg)
                / inboundEnergy);
    }


    public double getEnergyFluxTss(double flatRP) throws VelocityModelException {
        double eff =  getEnergyFluxFactorTss(flatRP);
        return eff*eff;
    }
    public double getEnergyFluxFactorTss(double flatRP) throws VelocityModelException {
        // careful sqrt neg number due to small rounding error
        double cosArg = 1-flatRP*flatRP* botVs* botVs;
        if (cosArg < 0) {return 0.0;}
        double inboundEnergy = inboundEnergyS(flatRP);
        if (inboundEnergy == 0) { return 0;}
        if ( topVs == 0.0  || botVs == 0.0) { return 0;}
        double Tss_calc = getTss(flatRP);
        return Tss_calc * Math.sqrt(botDensity * botVs * Math.sqrt(cosArg)
                / inboundEnergy);
    }




    public double getEnergyFluxRshsh(double flatRP) throws VelocityModelException {
        double eff =  getEnergyFluxFactorRshsh(flatRP);
        return eff*eff;
    }
    public double getEnergyFluxFactorRshsh(double flatRP) throws VelocityModelException {
        if ( topVs == 0.0 ) { return 0;}
        if (1/flatRP == topVs) {
            // in horizontal case, flux is zero, so no change in path energy as turn doesn't lose energy
            return 1.0;
        }
        // careful sqrt neg number due to small rounding error
        double cosArg = 1-flatRP*flatRP* topVs* topVs;
        if (cosArg < 0) {return 0.0;}
        double inboundEnergy = inboundEnergyS(flatRP);
        if (inboundEnergy == 0) { return 0;}
        double Rshsh_calc = getRshsh(flatRP);
        return Rshsh_calc * Math.sqrt( topDensity * topVs * Math.sqrt(cosArg)
                / inboundEnergy);
    }


    public double getEnergyFluxTshsh(double flatRP) throws VelocityModelException {
        double eff =  getEnergyFluxFactorTshsh(flatRP);
        return eff*eff;
    }
    public double getEnergyFluxFactorTshsh(double flatRP) throws VelocityModelException {
        if (topVs == 0.0  || botVs == 0.0) { return 0;}
        // careful sqrt neg number due to small rounding error
        double cosArg = 1-flatRP*flatRP* botVs* botVs;
        if (cosArg < 0) {return 0.0;}
        double inboundEnergy = inboundEnergyS(flatRP);
        if (inboundEnergy == 0) { return 0;}
        double Tshsh_calc = getTshsh(flatRP);
        return Tshsh_calc * Math.sqrt(botDensity * botVs * Math.sqrt(cosArg)
                / inboundEnergy);
    }

    public double getTopVp() {
        return topVp;
    }

    public double getTopVs() {
        return topVs;
    }

    public double getTopDensity() {
        return topDensity;
    }

    public double getBotVp() {
        return botVp;
    }

    public double getBotVs() {
        return botVs;
    }

    public double getBotDensity() {
        return botDensity;
    }

    protected abstract void calcTempVars(double rayParam, boolean inIsPWave);

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
