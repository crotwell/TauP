package edu.sc.seis.TauP;

import java.io.IOException;

public class TauP_SlownessPlot extends TauP_Create {

    @Override
    public void start() throws SlownessModelException, TauModelException, VelocityModelException, IOException {

        loadVMod();

        TauModel tMod = createTauModel(vMod);
        sMod.printGMT("taup_slownessmodel.gmt");
    }

}
