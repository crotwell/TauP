package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NoInnerCoreTest {


    @Test
    public void noInnerTest() throws Exception {
        List<String> phases = List.of("P", "PKP");
        VelocityModel velMod = VelocityModelTest.loadTestVelMod("noInnerCore.nd");
        TauModel tMod = TauModelLoader.createTauModel(velMod);
        for (String phName : phases) {
            SeismicPhase phase = SeismicPhaseFactory.createPhase(phName, tMod);
            assertTrue(phase.phasesExistsInModel());
        }
    }
}
