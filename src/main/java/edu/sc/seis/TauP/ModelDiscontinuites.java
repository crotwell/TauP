package edu.sc.seis.TauP;

import edu.sc.seis.TauP.gson.AboveBelowVelocityDiscon;

import java.util.ArrayList;
import java.util.List;

public class ModelDiscontinuites {

    public ModelDiscontinuites(VelocityModel vMod) throws NoSuchLayerException {
        this.modelname = vMod.getModelName();
        for (double d : vMod.getDisconDepths()) {
            discontinuities.add(new AboveBelowVelocityDiscon(d, vMod));
        }
    }

    String modelname;
    List<AboveBelowVelocityDiscon> discontinuities = new ArrayList<>();


}
