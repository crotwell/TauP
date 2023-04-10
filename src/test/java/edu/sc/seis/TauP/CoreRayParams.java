package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class CoreRayParams {

    @Test
    public void S_SVcs_SKS_rayparam() throws Exception {
        TauModel tMod = TauModelLoader.load("ak135");
        SeismicPhase S = SeismicPhaseFactory.createPhase("S", tMod);
        SeismicPhase SVcs = SeismicPhaseFactory.createPhase("SVcs", tMod);
        SeismicPhase SKS = SeismicPhaseFactory.createPhase("SKS", tMod);
        assertEquals(S.getMinRayParam(), SVcs.getMaxRayParam());
        assertEquals(SVcs.getMinRayParam(), SKS.getMaxRayParam());
    }

    @Test
    public void testMinRParamForK() throws Exception {
        /*
        At CMB in ak135, smallest crit ref rp for S is 7.588,
        For P smallest turn rp is 4.439, same as Pdiff
         */
        TauModel tMod = TauModelLoader.load("ak135");
        double topDepth = 2891.5;
        int sNumAboveP = tMod.getSlownessModel().layerNumberAbove(topDepth, true);
        int sNumAboveS = tMod.getSlownessModel().layerNumberAbove(topDepth, false);
        SlownessLayer aboveP = tMod.getSlownessModel().getSlownessLayer(sNumAboveP, true);
        SlownessLayer aboveS = tMod.getSlownessModel().getSlownessLayer(sNumAboveS, false);
        System.out.println("Above: P, S");
        System.out.println(aboveP);
        System.out.println(aboveS);
        int sNumBelowP = tMod.getSlownessModel().layerNumberBelow(topDepth, true);
        int sNumBelowS = tMod.getSlownessModel().layerNumberBelow(topDepth, true);
        SlownessLayer belowP = tMod.getSlownessModel().getSlownessLayer(sNumBelowP, true);
        SlownessLayer belowS = tMod.getSlownessModel().getSlownessLayer(sNumBelowS, false);
        System.out.println("Below: P,S");
        System.out.println(belowP);
        System.out.println(belowS);
        double cmbSlownessP = tMod.getSlownessModel().getMinTurnRayParam(topDepth, true);
        double cmbSlownessS = tMod.getSlownessModel().getMinTurnRayParam(topDepth, false);
        double maxRayParamP = tMod.getSlownessModel().getMinRayParam(topDepth, true);
        double maxRayParamS = tMod.getSlownessModel().getMinRayParam(topDepth, false);
        // 7.588 s/deg ~= 434.9375
        assertEquals(434.9375, maxRayParamS, 0.0001, maxRayParamP+" "+maxRayParamS);
    }
}
