package edu.sc.seis.TauP;

import java.io.IOException;

public class TauP_SlownessPlot extends TauP_VelocityPlot {

    public TauP_SlownessPlot() {
        setOutFileBase("taup_slownessmodel");
    }
    
    @Override
    public void start() throws SlownessModelException, TauModelException, VelocityModelException, IOException {
        VelocityModel vMod = TauModelLoader.loadVelocityModel(modelName, modelType);
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+modelName+", tried internally and from file");
        }

        TauP_Create taup_create = new TauP_Create();
        TauModel tMod = taup_create.createTauModel(vMod);
        tMod.getSlownessModel().printGMT(getOutFile());
    }

}
