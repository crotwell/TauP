package edu.sc.seis.TauP;

public class ReflTransFluidSolid extends ReflTrans {

    public ReflTransFluidSolid(double topVp,
                               double topDensity,
                               double botVp,
                               double botVs,
                               double botDensity) throws VelocityModelException {
        super(topVp, 0.0, topDensity, botVp, botVs, botDensity);
        if (topVp*topDensity*botVp*botVs*botDensity == 0.0) {
            throw new VelocityModelException("Fluid-solid reflection and transmission coefficients must have non-zero layer params:"
                    +" in:"+topVp+" "+topDensity+" tr: "+botVp+" "+botVs+" "+botDensity);
        }
    }


    /**
     * Calculates incident P wave in fluid (over solid) to reflected P wave Complex coefficient.
     */
    @Override
    public Complex getComplexRpp(double rayParam) {
        calcTempVars(rayParam, true);
        Complex t1 = botVertSlownessP.times(botVertSlownessS).times(4*sqBotVs*sqBotVs*sqRP).plus(cos2fterm*cos2fterm);
        Complex parenA = topVertSlownessP.times(botDensity).times(t1).times(-1);
        Complex parenB = botVertSlownessP.times(topDensity).times(2*sqBotVs*sqRP+cos2fterm);
        Complex out = (parenA.plus(parenB)).times(sqBotVp/(botVs*topDensity));
        return out.over(DFluidSolid);
    }

    /**
     * Calculates incident P wave in fluid (over solid) to transmitted P wave Complex coefficient.
     */
    @Override
    public Complex getComplexTpp(double rayParam) {
        calcTempVars(rayParam, true);
        Complex out = topVertSlownessP.times((-2*topVp*botVp*cos2fterm)/botVs);
        return out.over(DFluidSolid);
    }


    /**
     * Calculates incident P wave in fluid (over solid) to transmitted S wave Complex coefficient.
     */
    @Override
    public Complex getComplexTps(double rayParam) {
        calcTempVars(rayParam, true);
        Complex out = topVertSlownessP.times(botVertSlownessP).times(4*topVp*sqBotVp*rp);
        return out.over(DFluidSolid);
    }

    @Override
    public Complex getComplexRps(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to solid");
    }

    @Override
    public Complex getComplexRsp(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to solid");
    }

    @Override
    public Complex getComplexRss(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to solid");
    }

    @Override
    public Complex getComplexTsp(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to solid");
    }

    @Override
    public Complex getComplexTss(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to solid");
    }

    @Override
    public Complex getComplexRshsh(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to solid");
    }

    @Override
    public Complex getComplexTshsh(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to solid");
    }

    @Override
    public ReflTrans flip() throws VelocityModelException {
        return new ReflTransSolidFluid(botVp, botVs, botDensity, topVp, topDensity);
    }

    @Override
    public String toString() {
        return "Fluid-solid: "+" in: Vp: "+topVp+" d: "+topDensity+" tr: Vp"+botVp+" Vs: "+botVs+" d: "+botDensity;
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
            botVertSlownessP = calcTransVerticalSlownessP(rp);
            botVertSlownessS = calcTransVerticalSlownessS(rp);


            // new style
            cos2fterm = 1 -2*sqBotVs*sqRP;
            Complex t1 = topVertSlownessP.times(botVertSlownessP).times(botVertSlownessS).times(4*sqBotVs*sqBotVs*sqRP*botDensity);
            Complex t2 = topVertSlownessP.times(botDensity*cos2fterm*cos2fterm);
            Complex t3 = botVertSlownessP.times(2*sqBotVs*sqRP*topDensity);
            Complex t4 = botVertSlownessP.times(topDensity*cos2fterm);
            Complex paren = t1.plus(t2).plus(t3).plus(t4);
            DFluidSolid = paren.times(-1*sqBotVp/(botVs*topDensity));

            // fluid-solid

            lastRayParam = rayParam;
            lastInIsPWave = inIsPWave;
        }
    }

    Complex DFluidSolid;
    double cos2fterm;
}
