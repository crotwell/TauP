package edu.sc.seis.TauP;

import java.io.IOException;

public class TauP_VelocityPlot extends TauP_Create {

    @Override
    public void start() throws SlownessModelException, TauModelException, VelocityModelException, IOException {
        loadVMod();
        vMod.printGMT("taup_velocitymodel.gmt");
    }
}
