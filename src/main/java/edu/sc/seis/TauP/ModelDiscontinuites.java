package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

public class ModelDiscontinuites {

    public ModelDiscontinuites(VelocityModel vMod) {
        this.modelname = vMod.getModelName();
        for (double d : vMod.getDisconDepths()) {
            if (d == vMod.getRadiusOfEarth()) {
                // center not really a discon
                continue;
            }
            if (vMod.isNamedDisconDepth(d)) {
                discontinuities.add(vMod.getNamedDisconForDepth(d));
            } else {
                discontinuities.add(new NamedVelocityDiscon(d));
            }
        }
    }

    String modelname;
    List<NamedVelocityDiscon> discontinuities = new ArrayList<>();


}
