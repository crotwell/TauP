package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TauModelTest {

    @Test
    public void testDiffLayers() throws TauModelException {
        String modelName = "ak135fcont";
        TauModel tMod = TauModelLoader.load(modelName);
        VelocityModel vMod = tMod.getVelocityModel();
        assertEquals(20, tMod.getTauBranchAtDepth(20, true).getTopDepth());
        assertEquals(20, tMod.getTauBranch(1, true).getTopDepth());
        assertTrue(tMod.isDiffractionBranch(1, SlownessModel.PWAVE));
        assertTrue(tMod.isDiffractionBranch(1, SlownessModel.SWAVE));

        assertEquals(2891.5, tMod.getTauBranch(tMod.getCmbBranch(), true).getTopDepth());
        assertTrue(tMod.isDiffractionBranch(tMod.getCmbBranch(), SlownessModel.PWAVE));
        assertTrue(tMod.isDiffractionBranch(tMod.getCmbBranch(), SlownessModel.SWAVE));

        assertEquals(5153.5, tMod.getTauBranch(tMod.getIocbBranch(), true).getTopDepth());
        assertTrue(tMod.isDiffractionBranch(tMod.getIocbBranch(), SlownessModel.PWAVE));
        assertTrue(tMod.isDiffractionBranch(tMod.getIocbBranch(), SlownessModel.SWAVE));

    }

    @Test
    public void testHeadWaveLayers() throws TauModelException {
        String modelName = "ak135fcont";
        TauModel tMod = TauModelLoader.load(modelName);
        VelocityModel vMod = tMod.getVelocityModel();
        assertEquals(20, tMod.getTauBranchAtDepth(20, true).getTopDepth());
        assertEquals(20, tMod.getTauBranch(1, true).getTopDepth());
        assertTrue(tMod.isHeadWaveBranch(1, true));
        assertTrue(tMod.isHeadWaveBranch(1, true));

        assertEquals(2891.5, tMod.getTauBranch(tMod.getCmbBranch(), true).getTopDepth());
        assertFalse(tMod.isHeadWaveBranch(tMod.getCmbBranch(), SlownessModel.PWAVE));
        assertFalse(tMod.isHeadWaveBranch(tMod.getCmbBranch(), SlownessModel.SWAVE));

        assertEquals(5153.5, tMod.getTauBranch(tMod.getIocbBranch(), true).getTopDepth());
        assertTrue(tMod.isHeadWaveBranch(tMod.getIocbBranch(), SlownessModel.PWAVE));
        assertTrue(tMod.isHeadWaveBranch(tMod.getIocbBranch(), SlownessModel.SWAVE));

    }


}
