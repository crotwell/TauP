package edu.sc.seis.TauP;

public class ReflTransFluidFreeSurface extends ReflTrans {

    public ReflTransFluidFreeSurface(double inVp, double inDensity) throws VelocityModelException {
        super(inVp, 0.0, inDensity, 0.0, 0.0, 0.0);

        if (topVp*topDensity == 0.0) {
            throw new VelocityModelException("Fluid free surface reflection and transmission coefficients must have non-zero layer params:"
                    +" in:"+topVp+" "+topDensity);
        }
    }

    @Override
    public Complex getComplexRpp(double rayParam) throws VelocityModelException {
        return new Complex(1);
    }

    @Override
    public Complex getComplexRps(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid free surface");
    }

    @Override
    public Complex getComplexTpp(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid free surface");
    }

    @Override
    public Complex getComplexTps(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid free surface");
    }

    @Override
    public Complex getComplexRsp(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid free surface");
    }

    @Override
    public Complex getComplexRss(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid free surface");
    }

    @Override
    public Complex getComplexTsp(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid free surface");
    }

    @Override
    public Complex getComplexTss(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid free surface");
    }

    @Override
    public Complex getComplexRshsh(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid free surface");
    }

    @Override
    public Complex getComplexTshsh(double rayParam) throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid free surface");
    }

    @Override
    public ReflTrans flip() throws VelocityModelException {
        throw new VelocityModelException("Not legal for fluid free surface");
    }

    @Override
    protected void calcTempVars(double rayParam, boolean inIsPWave) {
        if(rayParam < 0) {
            throw new IllegalArgumentException("rayParam cannot be negative");
        }
        this.rp = rayParam; // ray parameter

        if(rayParam != lastRayParam || inIsPWave != lastInIsPWave) {

            lastRayParam = rayParam;
            lastInIsPWave = inIsPWave;
        }
    }

    @Override
    public String toString() {
        return "Fluid-free surface: "+" in: Vp: "+topVp+" d: "+topDensity;
    }
}
