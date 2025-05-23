package edu.sc.seis.TauP;

/**
 * Reflection and transmission coefficients at a solid-free surface boundary.
 */
public class ReflTransSolidFreeSurface extends ReflTransFreeSurface {

    public ReflTransSolidFreeSurface(double inVp, double inVs, double inDensity) throws VelocityModelException {
        super(inVp, inVs, inDensity, 0.0, 0.0, 0.0);
        if (topVp*topVs*topDensity == 0.0) {
            throw new VelocityModelException("Free Surface Solid-solid reflection and transmission coefficients must have non-zero layer params:"
                    +" in:"+topVp+" "+topVs+" "+topDensity);
        }
    }

    /**
     * Calculates incident P wave to reflected P wave complex coefficient at
     * free surface. Only topVp, topVs, and topDensity are used, the bottom
     * values are ignored. This is a little strange as free surface rays are
     * always upgoing, but it mantains consistency with the solid-solid
     * calculations.
     * <P>= (-1*((1/sqTopVs) - 2 * sqRP)^2 +<BR>
     * 4 * sqRP * topVertSlownessP * topVertSlownessS) / A
     */
    @Override
    public Complex getComplexRpp(double rayParam) {
        calcTempVars(rayParam, true);
        Complex numerator = CX.plus(-1.0 * ((1 / sqTopVs) - 2 * sqRP)
                * ((1 / sqTopVs) - 2 * sqRP), CX.times(topVertSlownessP,
                        topVertSlownessS)
                .times(4 * sqRP));
        return CX.over(numerator, fsA);
    }

    /**
     * Calculates incident P wave to reflected SV wave complex coefficient at
     * free surface. = (4 * (topVp/topVs) * rp * topVertSlownessP * ((1/sqTopVs) -
     * 2 * sqRP)) / fsA
     */
    @Override
    public Complex getComplexRps(double rayParam) {
        calcTempVars(rayParam, true);
        double realNumerator = 4 * (topVp / topVs) * rp
                * ((1 / sqTopVs) - 2 * sqRP);
        return CX.over(CX.times(realNumerator, topVertSlownessP), fsA);
    }

    @Override
    public Complex getComplexTpp(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for free surface");
    }

    @Override
    public Complex getComplexTps(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for free surface");
    }

    /**
     * Calculates incident SV wave to reflected P wave complex coefficient at
     * free surface.
     * <P>= (4 * (topVs/topVp) * rp * topVertSlownessS *<BR>
     * ((1/sqTopVs) - 2 * sqRP)) / fsA
     */
    @Override
    public Complex getComplexRsp(double rayParam) {
        calcTempVars(rayParam, false);
        double realNumerator = 4 * (topVs / topVp) * rp * ((1 / sqTopVs) - 2 * sqRP);
        return CX.over(CX.times(realNumerator, topVertSlownessS), fsA);
    }

    /**

    /**
     * Calculates incident SV wave to reflected SV wave complex coefficient at
     * free surface.
     * <P>= (-1 * ((1/sqTopVs) - 2 * sqRP)^2 +<BR>
     * 4 * sqRP * topVertSlownessP * topVertSlownessS) / fsA
     */
    @Override
    public Complex getComplexRss(double rayParam) {
        calcTempVars(rayParam, false);
        // Aki and Richards don't have -1
        double realNumerator = ((1 / sqTopVs) - 2 * sqRP)
                * ((1 / sqTopVs) - 2 * sqRP);
        Complex numerator = CX.minus(realNumerator, topVertSlownessP.times(topVertSlownessS).times(4 * sqRP));
        return CX.over(numerator, fsA);
    }

    @Override
    public Complex getComplexTsp(double rayParam) throws VelocityModelException {
         throw new VelocityModelException("Not legal for free surface");
    }

    @Override
    public Complex getComplexTss(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for free surface");
    }

    /**
     * Calculates incident SH wave to reflected SH wave complex coefficient at
     * free surface. Free surface SH is always 1.
     */
    @Override
    public Complex getComplexRshsh(double rayParam) {
        return new Complex(1);
    }

    @Override
    public Complex getComplexTshsh(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for free surface");
    }

    @Override
    public ReflTrans flip() throws VelocityModelException {
        throw new VelocityModelException("Not legal for free surface");
    }

    @Override
    public double getFreeSurfaceReceiverFunSh(double rayParam) {
        return 2.0;
    }

    @Override
    public double getFreeSurfaceReceiverFunP_r(double rayParam) {
        return Complex.abs(getFreeSurfaceReceiverFunP(rayParam)[0]);
    }

    @Override
    public double getFreeSurfaceReceiverFunP_z(double rayParam) {
        return Complex.abs(getFreeSurfaceReceiverFunP(rayParam)[1]);
    }

    @Override
    public Complex[] getFreeSurfaceReceiverFunP(double rayParam) {
        calcTempVars(rayParam, true);
        double betaRpFactor = (1/(topVs*topVs)-2*sqRP);
        Complex reyleighDenom = topVertSlownessS.times(topVertSlownessP).times(4*sqRP).plus(betaRpFactor*betaRpFactor );
        Complex etarpFactor = topVertSlownessS.times(topVertSlownessS).minus(sqRP);
        Complex rDisp = (topVertSlownessS.times(topVertSlownessP).times(4*topVp*rayParam/(topVs*topVs))) //.times( etarpFactor)
            .over(reyleighDenom);
        Complex zDisp = (topVertSlownessP.times(2*topVp/(topVs*topVs)).times( etarpFactor))
            .over(reyleighDenom);
        if (Complex.abs(zDisp) > 10) { zDisp = new Complex(10.0);}
        if (Complex.abs(rDisp) > 10) { rDisp = new Complex(10.0);}
        return new Complex[] { rDisp, zDisp };
    }


    @Override
    public double getFreeSurfaceReceiverFunSv_r(double rayParam) {
        return Complex.abs(getFreeSurfaceReceiverFunSv(rayParam)[0]);
    }

    @Override
    public double getFreeSurfaceReceiverFunSv_z(double rayParam) {
        return Complex.abs(getFreeSurfaceReceiverFunSv(rayParam)[1]);
    }

    @Override
    public Complex[] getFreeSurfaceReceiverFunSv(double rayParam) {
        calcTempVars(rayParam, false);
        double betaRpFactor = (1/(topVs*topVs)-2*sqRP);
        Complex reyleighDenom = topVertSlownessS.times(topVertSlownessP).times(4*sqRP).plus(betaRpFactor*betaRpFactor );
        Complex etarpFactor = topVertSlownessS.times(topVertSlownessS).minus(sqRP);
        Complex rDisp = topVertSlownessS.times(2/topVs).times(etarpFactor)
                .over(reyleighDenom);
        Complex zDisp = topVertSlownessS.times(topVertSlownessP).times(-4*rayParam/topVs)
                .over(reyleighDenom);
        if (Complex.abs(zDisp) > 10) { zDisp = new Complex(10.0);}
        if (Complex.abs(rDisp) > 10) { rDisp = new Complex(10.0);}
        return new Complex[] { rDisp, zDisp };
    }

    @Override
    public String toString() {
        return "Solid-free surface: "+" in: Vp: "+topVp+" Vs: "+topVs+" d: "+topDensity;
    }


    protected void calcTempVars(double rayParam, boolean inIsPWave) {
        if(rayParam < 0) {
            throw new IllegalArgumentException("rayParam cannot be negative");
        }
        this.rp = rayParam; // ray parameter

        if(rayParam != lastRayParam || inIsPWave != lastInIsPWave) {
            lastRayParam = -1.0; // in case of failure in method
            sqRP = rp * rp; // rp squared
            topVertSlownessP = calcInVerticalSlownessP(rp);
            topVertSlownessS = calcInVerticalSlownessS(rp);

            // free surface denominator
            // fsA = ((1/sqBotVs) - 2 * sqRP)^2 +
            // 4 * sqRP * topVertSlownessP * topVertSlownessS
            fsA = CX.plus(new Complex(((1 / sqTopVs) - 2 * sqRP)
                    * ((1 / sqTopVs) - 2 * sqRP)), CX.times(topVertSlownessP,
                            topVertSlownessS)
                    .times(4 * sqRP));

            lastRayParam = rayParam;
            lastInIsPWave = inIsPWave;
        }
    }


    /** used only in free surface calculations */
    protected Complex fsA;
}
