package edu.sc.seis.TauP;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;


public class NoOuterCoreTest {

    public NoOuterCoreTest() throws VelocityModelException, SlownessModelException, TauModelException, IOException {
         vMod = VelocityModelTest.loadTestVelMod(modelName);
         TauP_Create taupCreate = new TauP_Create();
         tMod = taupCreate.createTauModel(vMod);
    }
    
    String modelName = "noOuterCore.nd";
    VelocityModel vMod;
    TauModel tMod;
    
    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test() throws VelocityModelException, SlownessModelException, TauModelException {
        SeismicPhase PIP = new SeismicPhase("PIP", tMod);
        List<Arrival> arrivals = PIP.calcTime(180);
        assertEquals("num arrivals", 1, arrivals.size());
    }
    
    @Test
    public void testNoOuterCorePhase() throws TauModelException {
        String[] badPhaseList = new String[] {"PKP", "PKIKP", "PKiKP", "SKS" };
        for (String phaseName : badPhaseList) {
            SeismicPhase pPhase = new SeismicPhase(phaseName, tMod);
            assertFalse(phaseName+" should not exist in model "+pPhase.getMaxRayParam(), pPhase.phasesExistsInModel());
            List<Arrival> arrivals = pPhase.calcTime(30);
            assertEquals(phaseName+" no arrival", 0, arrivals.size());
        }
    }
    
}
