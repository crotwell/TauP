package edu.sc.seis.TauP;

public class VelocityDiscontinuity {

    public static VelocityDiscontinuity freeSurface(VelocityModel vMod) throws VelocityModelException {
        return fromVelocityModel(vMod, 0, false);
    }

    public static VelocityDiscontinuity fromVelocityModel(VelocityModel vMod, double depth, boolean downgoing) throws VelocityModelException {
         if (!vMod.isDisconDepth(depth)) {
            throw new VelocityModelException("depth " + depth + " is not a discontinuity in the model");
        }
        double abovePVel = 0;
        double aboveSVel = 0;
        double aboveRho = 0;
        double belowPVel = vMod.evaluateBelow(depth, VelocityModelMaterial.P_VELOCITY);
        double belowSVel = vMod.evaluateBelow(depth, VelocityModelMaterial.S_VELOCITY);
        double belowRho = vMod.evaluateBelow(depth, VelocityModelMaterial.DENSITY);
        if (depth != 0) {
            abovePVel = vMod.evaluateAbove(depth, VelocityModelMaterial.P_VELOCITY);
            aboveSVel = vMod.evaluateAbove(depth, VelocityModelMaterial.S_VELOCITY);
            aboveRho = vMod.evaluateAbove(depth, VelocityModelMaterial.DENSITY);
        }
        VelocityDiscontinuity discon = new VelocityDiscontinuity(abovePVel, aboveSVel, aboveRho, belowPVel, belowSVel, belowRho);
        if (!downgoing) {
            discon = discon.flip();
        }
        return discon;
    }

    public VelocityDiscontinuity(double[] params) {
        if (params.length != 6) {
            throw new IllegalArgumentException("layer params must be 6 numbers, inbould vp, vs, rho, transmitted vp, vs, rho. Length="+params.length);
        }
        this.inVp = params[0];
        this.inVs = params[1];
        this.inRho = params[2];
        this.trVp = params[3];
        this.trVs = params[4];
        this.trRho = params[5];
    }
    public VelocityDiscontinuity(double inVp, double inVs, double inRho, double trVp, double trVs, double trRho) {
        this.inVp = inVp;
        this.inVs = inVs;
        this.inRho = inRho;
        this.trVp = trVp;
        this.trVs = trVs;
        this.trRho = trRho;
    }

    double inVp;
    double inVs;
    double inRho;
    double trVp;
    double trVs;
    double trRho;

    public double getInVp() {
        return inVp;
    }

    public double getInVs() {
        return inVs;
    }

    public double getInRho() {
        return inRho;
    }

    public double getTrVp() {
        return trVp;
    }

    public double getTrVs() {
        return trVs;
    }

    public double getTrRho() {
        return trRho;
    }

    public String asName() {
        return inVp + "," + inVs + "," + inRho + " " + trVp + "," + trVs + "," + trRho;
    }

    public VelocityDiscontinuity flip() {
        return new VelocityDiscontinuity(trVp, trVs, trRho, inVp, inVs, inRho);
    }
}
