package edu.sc.seis.TauP;

public class ReflTransFluidSolid extends ReflTrans {

    public ReflTransFluidSolid(double topVp,
                               double topDensity,
                               double botVp,
                               double botVs,
                               double botDensity) throws VelocityModelException {
        if (topVp*topDensity*botVp*botVs*botDensity == 0.0) {
            throw new VelocityModelException("Fluid-solid reflection and transmission coefficients must have non-zero layer params:"
                    +" in:"+topVp+" "+topDensity+" tr: "+botVp+" "+botVs+" "+botDensity);
        }
        this.topVp = topVp;
        this.topVs = 0.0;
        this.topDensity = topDensity;
        this.botVp = botVp;
        this.botVs = botVs;
        this.botDensity = botDensity;
    }


    /**
     * Calculates incident P wave in fluid (over solid) to reflected P wave Complex coefficient.
     */
    @Override
    public Complex getComplexRpp(double rayParam) {
        calcTempVars(rayParam, true);
        Complex b24Term = botVertSlownessP.times(botVertSlownessS).times(4*sqBotVs*sqBotVs*sqRP);
        double b2Term = (1-2*sqBotVs*sqRP)*(1-2*sqBotVs*sqRP);
        Complex numeratorTerm = botVertSlownessP.times( botVp*botDensity).times(CX.plus(b24Term, b2Term))
                .minus(botVertSlownessP.times(topVp*topDensity));
        Complex out = CX.over(numeratorTerm, DFluidSolid);
        return out;
    }

    /**
     * Calculates incident P wave in fluid (over solid) to transmitted P wave Complex coefficient.
     */
    @Override
    public Complex getComplexTpp(double rayParam) {
        calcTempVars(rayParam, true);
        Complex numeratorTerm = CX.plus(topVertSlownessP.times(topVp), botVertSlownessP.times(botVp))
                .times( -1*(topVp/botVp)*topDensity*(2*sqBotVs*sqRP-1));
        Complex out = CX.over(numeratorTerm, DFluidSolid);
        return out;
    }


    /**
     * Calculates incident P wave in fluid (over solid) to transmitted S wave Complex coefficient.
     */
    @Override
    public Complex getComplexTps(double rayParam) {
        calcTempVars(rayParam, true);
        Complex numeratorTerm = botVertSlownessP
                .times( -2*(topVp*botVs)*rp*topDensity)
                .times(CX.plus(topVertSlownessP.times(topVp), botVertSlownessP.times(botVp)));
        Complex out = CX.over(numeratorTerm, DFluidSolid);
        return out;
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

    protected void calcTempVars(double rayParam, boolean inIsPWave) {
        if(rayParam < 0) {
            throw new IllegalArgumentException("rayParam cannot be negative");
        }
        this.rp = rayParam; // ray parameter

        if(rayParam != lastRayParam || inIsPWave != lastInIsPWave) {
            lastRayParam = -1.0; // in case of failure in method
            sqBotVs = botVs * botVs; // botVs squared
            sqTopVs = topVs * topVs; // topVs squared
            sqBotVp = botVp * botVp; // botVp squared
            sqTopVp = topVp * topVp; // topVp squared
            sqRP = rp * rp; // rp squared
            topVertSlownessP = Complex.sqrt(new Complex(1.0 / sqTopVp - sqRP));
            topVertSlownessS = Complex.sqrt(new Complex(1.0 / sqTopVs - sqRP));
            botVertSlownessP = Complex.sqrt(new Complex(1.0 / sqBotVp - sqRP));
            botVertSlownessS = Complex.sqrt(new Complex(1.0 / sqBotVs - sqRP));


            // fluid-solid
            Complex dfsParenTerm = CX.plus(botVertSlownessP.times(botVertSlownessS.times(4*sqTopVs*sqTopVs*sqRP)),
                    (1-2*sqBotVs*sqRP)*(1-2*sqBotVs*sqRP));
            DFluidSolid = botVertSlownessP.times(topVp*topDensity)
                    .plus(topVertSlownessP.times(topVp*botDensity).times(dfsParenTerm));

            lastRayParam = rayParam;
            lastInIsPWave = inIsPWave;
        }
    }

    Complex DFluidSolid;
}
