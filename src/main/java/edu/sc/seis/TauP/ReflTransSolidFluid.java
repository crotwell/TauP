package edu.sc.seis.TauP;

public class ReflTransSolidFluid extends ReflTrans {

    public ReflTransSolidFluid(double topVp,
            double topVs,
            double topDensity,
            double botVp,
            double botDensity) throws VelocityModelException {
        if (topVp*topVs*topDensity*botVp*botDensity == 0.0) {
            throw new VelocityModelException("Solid-fluid reflection and transmission coefficients must have non-zero layer params:"
                    +" in:"+topVp+" "+topVs+" "+topDensity+" tr: "+botVp+" "+botDensity);
        }
        this.topVp = topVp;
        this.topVs = topVs;
        this.topDensity = topDensity;
        this.botVp = botVp;
        this.botVs = 0.0;
        this.botDensity = botDensity;
    }


    /**
     * Calculates incident P wave in solid (over fluid) to reflected P wave Complex coefficient.
     */
    @Override
    public Complex getComplexRpp(double rayParam) {
        calcTempVars(rayParam, true);
        Complex a1Term = topVertSlownessP.times(
                botVertSlownessP.times(topVertSlownessS).times(4*sqTopVs*sqTopVs*sqRP*topDensity).plus(botDensity)
        );
        Complex a2Term = botVertSlownessP.times(topDensity).times((1-2*sqTopVs*sqRP)*(1-2*sqTopVs*sqRP));
        Complex out = CX.over(CX.minus(a1Term, a2Term), DSolidFluid);
        return out;
    }

    /**
     * Calculates incident P wave in solid (over fluid) to reflected P wave Complex coefficient.
     */
    @Override
    public Complex getComplexRps(double rayParam) {
        calcTempVars(rayParam, true);
        Complex numeratorTerm = CX.times(topVertSlownessP, botVertSlownessP).times(4*topVp*topVs*rp*topDensity).times(2*sqTopVs*sqRP-1);

        Complex out = CX.over(numeratorTerm, DSolidFluid);
        return out;
    }

    /**
     * Calculates incident P wave in solid (over fluid) to reflected P wave Complex coefficient.
     */
    @Override
    public Complex getComplexTpp(double rayParam) {
        calcTempVars(rayParam, true);
        Complex numeratorTerm = topVertSlownessP.times( 2*(topVp/botVp)*topDensity).times(1-2*sqTopVs*sqRP);
        Complex out = CX.over(numeratorTerm, DSolidFluid);
        return out;
    }


    /**
     * Calculates incident S wave in solid (over fluid) to reflected S wave Complex coefficient.
     */
    @Override
    public Complex getComplexRss(double rayParam) throws VelocityModelException {
        calcTempVars(rayParam, false);
        Complex a2Term = CX.times(botVertSlownessP, topDensity*(1-2*sqTopVs*sqRP)*(1-2*sqTopVs*sqRP));
        Complex a1Term = CX.times(topVertSlownessP, CX.minus(botDensity, CX.times(botVertSlownessP, topVertSlownessS).times(4*sqTopVs*sqTopVs*sqRP*topDensity)));
        Complex out = CX.over(CX.plus(a2Term, a1Term), DSolidFluid);
        return out;
    }

    /**
     * Calculates incident S wave in solid (over fluid) to reflected S wave Complex coefficient.
     */
    @Override
    public Complex getComplexRsp(double rayParam) throws VelocityModelException {
        calcTempVars(rayParam, false);
        Complex numeratorTerm = CX.times(botVertSlownessP, topVertSlownessS).times(4/topVp*sqTopVs*topVs*rp*topDensity*(1-2*sqTopVs*sqRP));
        Complex out = CX.over(numeratorTerm, DSolidFluid);
        return out;
    }

    /**
     * Calculates incident S wave in solid (over fluid) to reflected S wave Complex coefficient.
     */
    @Override
    public Complex getComplexTsp(double rayParam) throws VelocityModelException {
        calcTempVars(rayParam, false);

        Complex numeratorTerm = CX.times(topVertSlownessP, topVertSlownessS).times(4/botVp*sqTopVs*topVs*topDensity*rp);
        Complex out = CX.over(numeratorTerm, DSolidFluid);
        return out;
    }


    /**
     * Calculates incident SH wave in solid (over fluid) to reflected SH wave Complex coefficient.
     * Always 1.0, so just for completeness.
     */
    @Override
    public Complex getComplexRshsh(double rayParam) throws VelocityModelException {
        return new Complex(1);
    }

    @Override
    public Complex getComplexTps(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for solid to fluid");
    }

    @Override
    public Complex getComplexTss(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for solid to fluid");
    }

    @Override
    public Complex getComplexTshsh(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for solid to fluid");
    }

    @Override
    public ReflTrans flip() throws VelocityModelException {
        return null;
    }

    @Override
    public String toString() {
        return "Solid-fluid: "+" in: Vp: "+topVp+" Vs: "+topVs+" d: "+topDensity+" tr: Vp"+botVp+" d: "+botDensity;
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


            // solid-fluid
            Complex dsfBracketTerm = CX.plus(topVertSlownessP.times(topVertSlownessS).times(4*sqTopVs*sqTopVs*sqRP),
                    (1-2*sqTopVs*sqRP)*(1-2*sqTopVs*sqRP));
            DSolidFluid = CX.plus(topVertSlownessP.times(botDensity),
                    botVertSlownessP.times(topDensity).times(dsfBracketTerm));

            lastRayParam = rayParam;
            lastInIsPWave = inIsPWave;
        }
    }

    Complex DSolidFluid;
}
