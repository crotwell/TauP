package edu.sc.seis.TauP;

import java.io.IOException;

public class TauP_SlownessPlot extends TauP_VelocityPlot {

    public static final String DEFAULT_OUTFILE = "taup_slownessmodel";
    
    public TauP_SlownessPlot() {
        setOutFileBase(DEFAULT_OUTFILE);
    }
    
    @Override
    public void start() throws SlownessModelException, TauModelException, VelocityModelException, IOException {
        VelocityModel vMod = TauModelLoader.loadVelocityModel(modelName, modelType);
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+modelName+", tried internally and from file");
        }

        if (getOutFileBase() == DEFAULT_OUTFILE) {
            setOutFileBase(vMod.modelName+"_slow");
        }
        TauP_Create taup_create = new TauP_Create();
        TauModel tMod = taup_create.createTauModel(vMod);
        tMod.getSlownessModel().printGMT(getOutFile());
    }

}
