package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


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
        TauModel tMod = TauModelLoader.load("iasp91");
        double depth = 119;
        SeismicPhase phase = SeismicPhaseFactory.createPhase(phaseName, tMod, depth);
        for (int i = 0; i < phase.getRayParams().length; i++) {
            Arrival maxRPArrival = phase.shootRay(phase.getRayParams()[i]);
            assertEquals(phase.getDist()[i], maxRPArrival.getDist(), 0.0001, i+"th ray param dist");
            assertEquals( phase.getTime()[i], maxRPArrival.getTime(), 0.0001, i+"th ray param time");
        }
    }
    
    @Test
    public void testShootMiddleRayParam() throws Exception {
        TauModelLoader.clearCache();
        TauModel tMod = TauModelLoader.load("iasp91");
        double depth = 119;
        SeismicPhase phase = SeismicPhaseFactory.createPhase("P", tMod, depth);
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

    @Test
    public void takeoff90Test() throws Exception {
        TauPConfig.DEBUG = false;
        TauModel tMod = TauModelLoader.load("iasp91");
        double depth = 200;
        double takeoffAngle = 90.0;
        boolean PWAVE = true;

        TauModel tModDepth = tMod.depthCorrect(depth);
        SimpleSeismicPhase p_phase = SeismicPhaseFactory.createPhase("p", tModDepth, depth, 0, true);
        SimpleSeismicPhase P_phase = SeismicPhaseFactory.createPhase("P", tModDepth, depth, 0, true);

        assertEquals(p_phase.getMaxRayParam(), P_phase.getMaxRayParam());

        TakeoffAngleRay toRay = new TakeoffAngleRay(90.0);
        List<Arrival> p_arrList = toRay.calculate(p_phase);
        List<Arrival> P_arrList = toRay.calculate(P_phase);
        assertFalse(p_arrList.isEmpty());
        assertFalse(P_arrList.isEmpty());

        int p_slowIdx = tMod.getSlownessModel().layerNumberAbove(depth, PWAVE);
        double p_horRPFromSMod = tMod.getSlownessModel().getSlownessLayer(p_slowIdx, PWAVE).evaluateAt_bullen(depth, tMod.radiusOfEarth);
        int P_slowIdx = tMod.getSlownessModel().layerNumberBelow(depth, PWAVE);
        double P_horRPFromSMod = tMod.getSlownessModel().getSlownessLayer(P_slowIdx, PWAVE).evaluateAt_bullen(depth, tMod.radiusOfEarth);
        assertEquals(p_horRPFromSMod, P_horRPFromSMod);
        assertEquals(p_horRPFromSMod, p_phase.calcRayParamForTakeoffAngle(takeoffAngle));
        assertEquals(p_horRPFromSMod, P_phase.calcRayParamForTakeoffAngle(takeoffAngle));

        assertEquals(p_arrList.get(0).getRayParam(), P_arrList.get(0).getRayParam());


        assertEquals(p_arrList.get(0).getDist(), P_arrList.get(0).getDist());
        assertEquals(p_arrList.get(0).getTime(), P_arrList.get(0).getTime());

    }
}
