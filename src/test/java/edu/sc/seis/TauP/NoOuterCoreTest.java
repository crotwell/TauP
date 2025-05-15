package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;


public class NoOuterCoreTest {

    public NoOuterCoreTest() throws VelocityModelException, SlownessModelException, TauModelException, IOException {
         vMod = VelocityModelTest.loadTestVelMod(modelName);
         tMod = TauModelLoader.createTauModel(vMod);
    }
    
    String modelName = "noOuterCore.nd";
    VelocityModel vMod;
    TauModel tMod;
    

    @Test
    public void test() throws VelocityModelException, SlownessModelException, TauModelException {
        SeismicPhase PIP = SeismicPhaseFactory.createPhase("PIP", tMod);
        List<Arrival> arrivals = DistanceRay.ofDegrees(180).calculate(PIP);
        assertEquals( 1, arrivals.size());
    }
    
    @Test
    public void testNoOuterCorePhase() throws TauModelException {
        String[] badPhaseList = new String[] {"PKP", "PKIKP", "PKiKP", "SKS" };
        for (String phaseName : badPhaseList) {
            SeismicPhase pPhase = SeismicPhaseFactory.createPhase(phaseName, tMod);
            assertFalse( pPhase.phasesExistsInModel(), phaseName+" should not exist in model "+pPhase.getMaxRayParam());
            List<Arrival> arrivals = DistanceRay.ofDegrees(30).calculate(pPhase);
            assertEquals( 0, arrivals.size(), phaseName+" no arrival");
        }
    }
    
}
