package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TauModelTest {

    @Test
    public void testDiffLayers() throws TauModelException {
        String modelName = "ak135fcont";
        double sourceDepth = 100;

        TauModel tMod = TauModelLoader.load(modelName).depthCorrect(sourceDepth);
        VelocityModel vMod = tMod.getVelocityModel();
        assertEquals(20, tMod.getTauBranchAtDepth(20, true).getTopDepth());
        assertEquals(20, tMod.getTauBranch(1, true).getTopDepth());
        assertTrue(tMod.isDiffractionBranch(1, SlownessModel.PWAVE));
        assertTrue(tMod.isDiffractionBranch(1, SlownessModel.SWAVE));


        assertEquals(sourceDepth, tMod.getSourceDepth());
        assertFalse(tMod.isDiscontinuityBranch(tMod.getSourceBranch(), SlownessModel.PWAVE));
        assertFalse(tMod.isDiscontinuityBranch(tMod.getSourceBranch(), SlownessModel.SWAVE));
        assertFalse(tMod.isDiffractionBranch(tMod.getSourceBranch(), SlownessModel.PWAVE));
        assertFalse(tMod.isDiffractionBranch(tMod.getSourceBranch(), SlownessModel.SWAVE));


        assertTrue(tMod.isDiffractionBranch(tMod.getMohoBranch(), SlownessModel.PWAVE));
        assertTrue(tMod.isDiffractionBranch(tMod.getMohoBranch(), SlownessModel.SWAVE));

        assertEquals(2891.5, tMod.getTauBranch(tMod.getCmbBranch(), true).getTopDepth());
        assertTrue(tMod.isDiffractionBranch(tMod.getCmbBranch(), SlownessModel.PWAVE));
        assertTrue(tMod.isDiffractionBranch(tMod.getCmbBranch(), SlownessModel.SWAVE));

        assertEquals(5153.5, tMod.getTauBranch(tMod.getIocbBranch(), true).getTopDepth());
        assertTrue(tMod.isDiffractionBranch(tMod.getIocbBranch(), SlownessModel.PWAVE));
        assertFalse(tMod.isDiffractionBranch(tMod.getIocbBranch(), SlownessModel.SWAVE), "fluid");

    }

    @Test
    public void testHeadWaveLayers() throws TauModelException {
        String modelName = "ak135fcont";
        double sourceDepth = 100;
        TauModel tMod = TauModelLoader.load(modelName).depthCorrect(sourceDepth);
        VelocityModel vMod = tMod.getVelocityModel();
        assertEquals(20, tMod.getTauBranchAtDepth(20, true).getTopDepth());
        assertEquals(20, tMod.getTauBranch(1, true).getTopDepth());

        assertEquals(sourceDepth, tMod.getSourceDepth());
        assertFalse(tMod.isHeadWaveBranch(tMod.getSourceBranch(), SlownessModel.PWAVE, SlownessModel.PWAVE));
        assertFalse(tMod.isHeadWaveBranch(tMod.getSourceBranch(), SlownessModel.PWAVE, SlownessModel.SWAVE));
        assertFalse(tMod.isHeadWaveBranch(tMod.getSourceBranch(), SlownessModel.SWAVE, SlownessModel.PWAVE));
        assertFalse(tMod.isHeadWaveBranch(tMod.getSourceBranch(), SlownessModel.SWAVE, SlownessModel.SWAVE));

        assertTrue(tMod.isHeadWaveBranch(1, SlownessModel.PWAVE, SlownessModel.PWAVE));
        assertTrue(tMod.isHeadWaveBranch(1, SlownessModel.SWAVE, SlownessModel.PWAVE));

        assertTrue(tMod.isHeadWaveBranch(tMod.getMohoBranch(), SlownessModel.PWAVE, SlownessModel.PWAVE));
        assertFalse(tMod.isHeadWaveBranch(tMod.getMohoBranch(), SlownessModel.PWAVE, SlownessModel.SWAVE));
        assertTrue(tMod.isHeadWaveBranch(tMod.getMohoBranch(), SlownessModel.SWAVE, SlownessModel.PWAVE));
        assertTrue(tMod.isHeadWaveBranch(tMod.getMohoBranch(), SlownessModel.SWAVE, SlownessModel.SWAVE));

        assertEquals(2891.5, tMod.getTauBranch(tMod.getCmbBranch(), true).getTopDepth());
        assertFalse(tMod.isHeadWaveBranch(tMod.getCmbBranch(), SlownessModel.PWAVE, SlownessModel.PWAVE));
        assertFalse(tMod.isHeadWaveBranch(tMod.getCmbBranch(), SlownessModel.SWAVE, SlownessModel.SWAVE));
        assertTrue(tMod.isHeadWaveBranch(tMod.getCmbBranch(), SlownessModel.SWAVE, SlownessModel.PWAVE));

        assertEquals(5153.5, tMod.getTauBranch(tMod.getIocbBranch(), true).getTopDepth());
        assertTrue(tMod.isHeadWaveBranch(tMod.getIocbBranch(), SlownessModel.PWAVE, SlownessModel.PWAVE));
        assertFalse(tMod.isHeadWaveBranch(tMod.getIocbBranch(), SlownessModel.PWAVE, SlownessModel.SWAVE));
        assertFalse(tMod.isHeadWaveBranch(tMod.getIocbBranch(), SlownessModel.SWAVE, SlownessModel.PWAVE));
        assertFalse(tMod.isHeadWaveBranch(tMod.getIocbBranch(), SlownessModel.SWAVE, SlownessModel.SWAVE));

    }


}
