package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PuristNameTest {

    @Test
    public void puristNameP20P() throws TauModelException {
        List<List<String>> names = new ArrayList<>();
        names.add(List.of("P20P", "Ped20P"));
        names.add(List.of("P20p", "P20p"));
        names.add(List.of("P20S", "Ped20S"));
        names.add(List.of("P20s", "P20s"));

        TauModel tauModel = TauModelLoader.load("ak135fcont");
        for (List<String> ph : names) {
            SeismicPhase sp = SeismicPhaseFactory.createPhase(ph.get(0), tauModel);
            assertTrue(sp.phasesExistsInModel());
            assertEquals(ph.get(1), sp.getPuristName(), ph.get(0)+" -> "+ph.get(1));
        }
    }

    @Test
    public void puristName() throws TauModelException {
        List<List<String>> names = new ArrayList<>();
        names.add(List.of("P660diff^410P", "P660diff^410P"));
        names.add(List.of("P660diff^410P660diff", "P660diff^410P660diff"));

        TauModel tauModel = TauModelLoader.load("ak135fcont");
        for (List<String> ph : names) {
            SeismicPhase sp = SeismicPhaseFactory.createPhase(ph.get(0), tauModel);
            assertTrue(sp.phasesExistsInModel());
            assertEquals(ph.get(1), sp.getPuristName(), ph.get(0)+" -> "+ph.get(1));
        }
    }

    @Test
    public void otherDiffDownTest() throws Exception {
        String phasename = "P1554diffdnPcp";
        String pureName = "P1554diffdnPcp";
        String modelName = "MarsLiquidLowerMantle.nd";
        VelocityModel vMod = VelocityModelTest.loadTestVelMod(modelName);
        TauModel tauModel = TauModelLoader.createTauModel(vMod);
        SeismicPhase sp = SeismicPhaseFactory.createPhase(phasename, tauModel);

        assertTrue(sp.phasesExistsInModel());
        assertEquals(pureName, sp.getPuristName(), phasename+" -> "+pureName);
    }


}
