package edu.sc.seis.TauP;

/**
 * Reflection and transmission coefficients at a fluid-fluid boundary.
 */
public class ReflTransFluidFluid extends ReflTrans {


    public ReflTransFluidFluid(double topVp,
                               double topDensity,
                               double botVp,
                               double botDensity) throws VelocityModelException {
        super(topVp, 0.0, topDensity, botVp, 0.0, botDensity);
        if (topVp*topDensity*botVp*botDensity == 0.0) {
            throw new VelocityModelException("Fluid-fluid reflection and transmission coefficients must have non-zero layer params:"
                    +" in:"+topVp+" "+topDensity+" tr: "+botVp+" "+botDensity);
        }
    }

    @Override
    public Complex getComplexRpp(double rayParam) {
        calcTempVars(rayParam, true);
        Complex num = botVertSlownessP.times(topDensity).minus(topVertSlownessP.times(botDensity));
        return num.over(denom);

    }

    @Override
    public Complex getComplexRps(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to fluid");
    }

    @Override
    public Complex getComplexTpp(double rayParam) {
        calcTempVars(rayParam, true);
        Complex num = new Complex(topVp/botVp*2*topDensity).times(topVertSlownessP);
        return num.over(denom);
    }

    @Override
    public Complex getComplexTps(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to fluid");
    }

    @Override
    public Complex getComplexRsp(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to fluid");
    }

    @Override
    public Complex getComplexRss(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to fluid");
    }

    @Override
    public Complex getComplexTsp(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to fluid");
    }

    @Override
    public Complex getComplexTss(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to fluid");
    }

    @Override
    public Complex getComplexRshsh(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to fluid");
    }

    @Override
    public Complex getComplexTshsh(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid to fluid");
    }

    @Override
    public ReflTrans flip() throws VelocityModelException {
        return new ReflTransFluidFluid(botVp, botDensity, topVp, topDensity);
    }

    @Override
    protected void calcTempVars(double rayParam, boolean inIsPWave) {
        if(rayParam < 0) {
            throw new IllegalArgumentException("rayParam cannot be negative");
        }
        this.rp = rayParam; // ray parameter

        if(rayParam != lastRayParam || inIsPWave != lastInIsPWave) {
            topVertSlownessP = calcInVerticalSlownessP(rp);
            botVertSlownessP = calcTransVerticalSlownessP(rp);
            lastRayParam = rayParam;
            lastInIsPWave = inIsPWave;

            denom = topVertSlownessP.times(botDensity).plus(botVertSlownessP.times(topDensity));
        }
    }

    Complex denom;

    @Override
    public String toString() {
        return "Fluid-fluid: "+" in: Vp: "+topVp+" d: "+topDensity+" tr: Vp"+botVp+" d: "+botDensity;
    }
}
