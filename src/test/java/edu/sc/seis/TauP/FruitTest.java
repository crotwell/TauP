package edu.sc.seis.TauP;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FruitTest {

    public static final String fruitModel = "fruit";
    public static final String fruitModelFile = fruitModel+".nd";

    public SlownessModel sMod;
    public TauModel tMod;

    @BeforeEach
    public void setUp() throws Exception {
        VelocityModel vMod = VelocityModelTest.loadTestVelMod(fruitModelFile);
        TauP_Create taupCreate = new TauP_Create();
        tMod = taupCreate.createTauModel(vMod);
        sMod = tMod.getSlownessModel();
    }

    @Test
    public void highSlowDepth() throws Exception {
        DepthRange highSlowRange = sMod.highSlownessLayerDepthsP.get(0);
        assertTrue(sMod.depthInHighSlowness(1360,18, true), "a");
        assertTrue(sMod.depthInHighSlowness(1360,highSlowRange.rayParam+0.00000001, true), "b");
        assertFalse(sMod.depthInHighSlowness(1360,highSlowRange.rayParam, true), "c");
        assertFalse(sMod.depthInHighSlowness(1360,highSlowRange.rayParam-0.00000001, true), "d");
    }

    @Test
    public void noMantleP() throws Exception {
        SimpleSeismicPhase P = SeismicPhaseFactory.createPhase("P", tMod);
        double tol = 0.01;
        assertEquals(0.0, P.getMinDistanceDeg(), tol);
        assertEquals(72.66, P.getMaxDistanceDeg(), tol);
        assertEquals(2, P.branchSeq.size());
        assertEquals(0, P.branchSeq.get(0));
        assertEquals(0, P.branchSeq.get(1));
    }

}
