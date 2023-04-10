package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;


public class TauBranchTest {

    @Test
    public void testInsertNewDepth() throws TauModelException, SlownessModelException, NoSuchLayerException {
        TauModelLoader.clearCache();
        TauModel tMod = TauModelLoader.load("iasp91");
        double depth = 119;
        int layerNum = tMod.getSlownessModel().layerNumForDepth(depth, true);
        SlownessLayer sLayer = tMod.getSlownessModel().getSlownessLayer(layerNum, true);
        double midDepth = (sLayer.getTopDepth() + sLayer.getBotDepth()) / 2;
        double rp = sLayer.evaluateAt_bullen(midDepth, tMod.getRadiusOfEarth());
        SlownessLayer topSLayer = new SlownessLayer(sLayer.getTopP(), sLayer.getTopDepth(), rp, midDepth);
        SlownessLayer botSLayer = new SlownessLayer(rp, midDepth, sLayer.getBotP(), sLayer.getBotDepth());
        double[] rayParams = tMod.getRayParams();
        int rpIndex = 0;
        while (rayParams[rpIndex] > rp) {
            rpIndex++;
        }
        TauBranch orig = tMod.getTauBranch(tMod.findBranch(midDepth), true);
        TauBranch tBranch = orig.clone();
        tBranch.insert(rp, tMod.getSlownessModel(), rpIndex);
        double layerDepth = orig.getTopDepth();
        int slNum = tMod.getSlownessModel().layerNumberBelow(layerDepth, true);
        SlownessLayer tmpSL = tMod.getSlownessModel().getSlownessLayer(slNum, true);
        TimeDist newRPTimeDist = new TimeDist(rp);
        while (tmpSL.getBotDepth() <= orig.getBotDepth() && tmpSL.getBotP() >= rp) {
            newRPTimeDist = newRPTimeDist.add(tmpSL.bullenRadialSlowness(rp, tMod.getRadiusOfEarth(), true));
            slNum++;
            tmpSL = tMod.getSlownessModel().getSlownessLayer(slNum, true);
        }
        assertEquals( rayParams.length, orig.dist.length);
        assertEquals(rayParams.length, tBranch.dist.length - 1);
        assertEquals( orig.dist.length, tBranch.dist.length - 1);
        assertEquals( orig.time.length, tBranch.time.length - 1);
        int i = 0;
        while (i < rpIndex) {
            assertEquals( orig.dist[i], tBranch.dist[i], 0.000000001, i + " pre dist");
            assertEquals( orig.time[i], tBranch.time[i], 0.000000001, i + " pre time");
            i++;
        }
        assertEquals( newRPTimeDist.getDistRadian(), tBranch.dist[rpIndex], 0.000001, "new val dist");
        assertEquals( newRPTimeDist.getTime(), tBranch.time[rpIndex], 0.000001, "new val time");
        while (i < orig.dist.length) {
            assertEquals(orig.dist[i], tBranch.dist[i + 1], 0.000000001, i + " post dist");
            assertEquals( orig.time[i], tBranch.time[i + 1], 0.000000001, i + " post time");
            i++;
        }
    }

    @Test
    public void testDifferenceExistingDepth() throws Exception {
       // TauModelLoader.clearCache();
        TauModel tMod = TauModelLoader.load("iasp91");
        double depth = 120;
        int layerNum = tMod.getSlownessModel().layerNumberBelow(depth, true);
        SlownessLayer sLayer = tMod.getSlownessModel().getSlownessLayer(layerNum, true);
        assertEquals( depth, sLayer.getTopDepth(), 0.00000000001, "depth in slowness model");
        double rp = sLayer.getTopP();
        double[] rayParams = tMod.getRayParams();
        int rpIndex = 0;
        while (rayParams[rpIndex] > rp) {
            rpIndex++;
        }
        assertTrue( rpIndex < rayParams.length, "rpIndex rp length");
        assertEquals( rp, rayParams[rpIndex], 0.000000000001, "rp rpIndex");
        TauBranch orig = tMod.getTauBranch(tMod.findBranch(depth), true);
        

        TauBranch topBranch = new TauBranch(orig.getTopDepth(),
                                                            depth,
                                                            true);
        topBranch.createBranch(tMod.getSlownessModel(),
                               rayParams);
        assertEquals( rayParams.length, orig.dist.length, "orig branch dist length");
        assertEquals(rayParams.length, topBranch.dist.length, "new branch dist length");
        assertEquals(rayParams.length, topBranch.time.length, "new branch time length");
        TauBranch botBranch = orig.difference(topBranch,
                                              -1,
                                              -1,
                                              tMod.getSlownessModel(),
                                              rayParams);
        for (int i = 0; i <= rpIndex; i++) {
            assertEquals(orig.getDist()[i], topBranch.getDist(i)+botBranch.getDist(i), 0.000000001);
            assertEquals( orig.getTime()[i], topBranch.getTime(i)+botBranch.getTime(i), 0.000000001);
            assertEquals( botBranch.getDist(i), 0, 0.000000001);
            assertEquals( botBranch.getTime(i), 0, 0.000000001);
        }
        for (int i = rpIndex+1; i<rayParams.length; i++) {
            assertEquals( orig.getDist()[i], topBranch.getDist(i)+botBranch.getDist(i), 0.000000001);
            assertEquals( orig.getTime()[i], topBranch.getTime(i)+botBranch.getTime(i), 0.000000001);
        }
        
    }
    
    @Test
    public void testDifferenceNewDepth() throws Exception {
        TauModelLoader.clearCache();
        TauModel tMod = TauModelLoader.load("iasp91");
        double depth = 119;
        int layerNum = tMod.getSlownessModel().layerNumForDepth(depth, true);
        SlownessLayer sLayer = tMod.getSlownessModel().getSlownessLayer(layerNum, true);
        assertTrue( depth > sLayer.getTopDepth(), "depth in slowness model");
        assertTrue(depth < sLayer.getBotDepth(), "depth in slowness model");
        double rp = sLayer.evaluateAt_bullen(depth, tMod.getRadiusOfEarth());
        double[] rayParams = tMod.getRayParams();
        int rpIndex = 0;
        while (rayParams[rpIndex] > rp) {
            rpIndex++;
        }
        assertTrue( rpIndex < rayParams.length, "rpIndex rp length");
        double[] outRayParams = new double[tMod.getRayParams().length+1];
        System.arraycopy(tMod.getRayParams(), 0, outRayParams, 0, rpIndex);
        outRayParams[rpIndex] = rp;
        
        System.arraycopy(tMod.getRayParams(), rpIndex, outRayParams, rpIndex+1, tMod.getRayParams().length-rpIndex);
        TauBranch orig = tMod.getTauBranch(tMod.findBranch(depth), true);
        TauBranch tBranch = orig.clone();
        tBranch.insert(rp, tMod.getSlownessModel(), rpIndex);
        

        TauBranch topBranch = new TauBranch(orig.getTopDepth(),
                                                            depth,
                                                            true);
        SlownessModel smod = tMod.getSlownessModel().splitLayer(depth, true).getSlownessModel();
        topBranch.createBranch(smod,
                               outRayParams);
        TauBranch botBranch = orig.difference(topBranch,
                                              rpIndex,
                                              -1,
                                              smod,
                                              outRayParams);
        for (int i = 0; i < rpIndex; i++) {
            assertEquals( orig.getDist()[i], topBranch.getDist(i)+botBranch.getDist(i), 0.000000001);
            assertEquals( orig.getTime()[i], topBranch.getTime(i)+botBranch.getTime(i), 0.000000001);
        }
        for (int i = rpIndex; i<rayParams.length; i++) {
            assertEquals( orig.getDist()[i], topBranch.getDist(i+1)+botBranch.getDist(i+1), 0.000000001);
            assertEquals( orig.getTime()[i], topBranch.getTime(i+1)+botBranch.getTime(i+1), 0.000000001);
        }
        
    }

    @Test
    public void testShiftBranch() throws TauModelException {
        double tol = 0.0000000001;
        TauModel tMod = TauModelLoader.load("iasp91");
        int numBranches = tMod.getNumBranches();
        for (int i = 0; i < numBranches; i++) {
            TauBranch orig = tMod.getTauBranch(i, true);
            TauBranch branch = orig.clone();
            branch.shiftBranch(1);
            assertEquals(orig.getDist(0), branch.getDist(0), tol);
            SplitModelTest.arrayEqualsSubrange(i + " dist",
                                               orig.getDist(),
                                               1,
                                               branch.getDist(),
                                               2,
                                               orig.getDist().length - 1);
            SplitModelTest.arrayEqualsSubrange(i + " time",
                                               orig.getTime(),
                                               1,
                                               branch.getTime(),
                                               2,
                                               orig.getTime().length - 1);
        }
    }
    
    @Test
    public void testSplitBranch() throws TauModelException { 
        double splitDepth = 200;
        TauModel tMod = TauModelLoader.load("prem");
        TauModel splitTMod = tMod.splitBranch(splitDepth);
        boolean found = false;
        for (int i = 0; i < splitTMod.getBranchDepths().length; i++) {
            if (splitDepth == splitTMod.getBranchDepths()[i]) {
                found = true;
                System.out.println("split: "+splitTMod.getTauBranch(i, true));
            }
        }
        assertTrue( found, "Found split depth after split tmod");
    }
}
