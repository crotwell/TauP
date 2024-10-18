package edu.sc.seis.TauP;

public class ReflTransFluidFreeSurface extends ReflTransFreeSurface {

    public ReflTransFluidFreeSurface(double inVp, double inDensity) throws VelocityModelException {
        super(inVp, 0.0, inDensity, 0.0, 0.0, 0.0);
        if (topVp*topDensity == 0.0) {
            throw new VelocityModelException("Fluid free surface reflection and transmission coefficients must have non-zero Vp and density params:"
                    +" in:"+topVp+" "+topDensity);
        }
    }

    /**
     * Sh always zero due to fliud.
     */
    @Override
    public double getFreeSurfaceReceiverFunSh(double rayParam) {
        return 0.0;
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
        return new Complex[] { new Complex(2), new Complex(0)};
    }

    // no-ops for S, always 0
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
        return new Complex[] { new Complex(0), new Complex(0)};
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
