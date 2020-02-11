package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;



public class ShootTest {

    @Test
    public void testShootExistingRayParam() throws Exception {
        testShootExistingRayParamForPhase("P");
        testShootExistingRayParamForPhase("S");
        testShootExistingRayParamForPhase("p");
        testShootExistingRayParamForPhase("s");
        testShootExistingRayParamForPhase("PP");
        testShootExistingRayParamForPhase("SS");
        testShootExistingRayParamForPhase("PcP");
        testShootExistingRayParamForPhase("ScS");
        testShootExistingRayParamForPhase("PKP");
        testShootExistingRayParamForPhase("SKS");
        testShootExistingRayParamForPhase("PKIKP");
        testShootExistingRayParamForPhase("SKIKS");
    }
    
    public void testShootExistingRayParamForPhase(String phaseName) throws Exception {
        TauModelLoader.clearCache();
        double depth = 119;
        SeismicPhase phase = new SeismicPhase(phaseName, "iasp91", depth);
        for (int i = 0; i < phase.getRayParams().length; i++) {
            Arrival maxRPArrival = phase.shootRay(phase.getRayParams()[i]);
            assertEquals(phase.getDist()[i], maxRPArrival.getDist(), 0.0001, i+"th ray param dist");
            assertEquals( phase.getTime()[i], maxRPArrival.getTime(), 0.0001, i+"th ray param time");
        }
    }
    
    @Test
    public void testShootMiddleRayParam() throws Exception {
        TauModelLoader.clearCache();
        double depth = 119;
        SeismicPhase phase = new SeismicPhase("P", "iasp91", depth);
        for (int i = 0; i < phase.getRayParams().length-1; i++) {
            double rp = (phase.getRayParams()[i]+phase.getRayParams()[i+1])/2;
            double timeTol = Math.abs(phase.getTime()[i]-phase.getTime()[i+1]);
            Arrival maxRPArrival = phase.shootRay(rp);
            assertEquals(phase.getDist()[i], maxRPArrival.getDist(), 0.1, i+"th ray param dist");
            assertEquals( phase.getTime()[i], maxRPArrival.getTime(), timeTol, i+"th ray param time");
            assertEquals( phase.getDist()[i+1], maxRPArrival.getDist(), 0.1, i+"th+1 ray param dist");
            assertEquals( phase.getTime()[i+1], maxRPArrival.getTime(), timeTol, i+"th+1 ray param time");
        }
    }
}
