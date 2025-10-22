package edu.sc.seis.TauP.gson;

import edu.sc.seis.TauP.*;

import static edu.sc.seis.TauP.SphericalCoords.RtoD;

public class AboveBelowVelocityDiscon extends NamedVelocityDiscon {

    public AboveBelowVelocityDiscon(double depth, VelocityModel vMod) throws NoSuchLayerException {
        super(depth);
        if (!vMod.isDisconDepth(depth)) {
            throw new NoSuchLayerException(vMod.getRadiusOfEarth()-depth, "Depth "+depth+" is not a velocity discontinuity in "+vMod.getModelName());
        }
        if (vMod.isNamedDisconDepth(depth)) {
            NamedVelocityDiscon nd = vMod.getNamedDisconForDepth(depth);
            super.name = nd.getName();
            if (nd.hasPreferredName()) {
                super.preferredName = nd.getPreferredName();
            }
        }
        if (depth != 0.0) {
            this.above = vMod.getVelocityLayer(vMod.layerNumberAbove(depth));
        }
        if (depth != vMod.getRadiusOfEarth()) {
            this.below = vMod.getVelocityLayer(vMod.layerNumberBelow(depth));
        }
        this.vMod = vMod;
    }

    public VelocityLayer getAbove() {
        return above;
    }

    public VelocityLayer getBelow() {
        return below;
    }

    public VelocityModel getVelocityModel() {
        return vMod;
    }

    public double getAboveSlownessP() {
        SlownessLayer p_SlowLayer = new SlownessLayer(above, vMod.getSpherical(), vMod.getRadiusOfEarth(), true);
        return p_SlowLayer.getBotP()/RtoD;
    }

    public double getAboveSlownessS() {
        SlownessLayer s_SlowLayer = new SlownessLayer(above, vMod.getSpherical(), vMod.getRadiusOfEarth(), false);
        return s_SlowLayer.getBotP()/RtoD;
    }

    public double getBelowSlownessP() {
        SlownessLayer p_SlowLayer = new SlownessLayer(below, vMod.getSpherical(), vMod.getRadiusOfEarth(), true);
        return p_SlowLayer.getTopP()/RtoD;
    }

    public double getBelowSlownessS() {
        SlownessLayer s_SlowLayer = new SlownessLayer(below, vMod.getSpherical(), vMod.getRadiusOfEarth(), false);
        return s_SlowLayer.getTopP()/RtoD;
    }

    VelocityLayer above = null;
    VelocityLayer below = null;
    VelocityModel vMod = null;
}
