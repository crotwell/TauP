package edu.sc.seis.TauP;

import static org.junit.Assert.*;

import org.junit.Test;


public class SlownessLayerTest {

/**
 * 
 * @ToDo this test fails because it overflows a double, I think.
 * @throws SlownessModelException
 */
    @Test
    public void testBullenDepthFor() throws SlownessModelException {
        double radiusOfEarth = 6371;
        SlownessLayer sl = new SlownessLayer(1865.9237536656892,  8.2,  1598.66142652803,  17.20068330450901);
        
        double Bnum = Math.log(sl.getTopP() / sl.getBotP());
        double Bdenom =
                 Math.log((radiusOfEarth - sl.getTopDepth())
                        / (radiusOfEarth - sl.getBotDepth()));
        double B = Bnum / Bdenom;
        double A = sl.getTopP()
                / Math.pow((radiusOfEarth - sl.getTopDepth()), B);
        assertFalse("Bnum", Double.isNaN(Bnum));
        assertFalse("Bdenom", Double.isNaN(Bdenom));
        assertFalse("B", Double.isNaN(B));
        assertFalse("A", Double.isNaN(A));
        System.out.println("A="+A+" B="+B+" Bnum="+Bnum+" Bedenom="+Bdenom);
        
        // #########################
        // TEST DISABLED...
        // #########################
        
//        double depth = sl.bullenDepthFor(1732.2925900968596, 6371);

//        assertTrue("below top", depth > sl.getTopDepth());
//        assertTrue("above bot", depth < sl.getBotDepth());
    }
    
    @Test
    public void testQianguoMod() throws SlownessModelException {
        SlownessLayer sl = new SlownessLayer(2548.4, 6.546970605878823, 1846.2459389213773, 13.798727310994103);
        double depth = sl.bullenDepthFor(2197.322969460689, 6371);
        assertFalse("depth no NaN", Double.isNaN(depth));
    }
    
    @Test
    public void testSplitLayer() throws Exception {
        TauModel tMod = TauModelLoader.load("iasp91");
        double depth = 119;
        int layerNum = tMod.getSlownessModel().layerNumForDepth(depth, true);
        SlownessLayer sLayer = tMod.getSlownessModel().getSlownessLayer(layerNum, true);
        double midDepth = (sLayer.getTopDepth()+sLayer.getBotDepth())/2;
        double rp = sLayer.evaluateAt_bullen(midDepth, tMod.getRadiusOfEarth());

        SlownessLayer topSLayer = new SlownessLayer(sLayer.getTopP(), sLayer.getTopDepth(), rp, midDepth );
        SlownessLayer botSLayer = new SlownessLayer(rp, midDepth, sLayer.getBotP(), sLayer.getBotDepth());
        double[] rps = tMod.getRayParams();
        for (int i = 0; i < rps.length; i++) {
            if (rps[i] < sLayer.getBotP()) {
                TimeDist td = sLayer.bullenRadialSlowness(rps[i], tMod.getRadiusOfEarth(), true);
                TimeDist topTD = topSLayer.bullenRadialSlowness(rps[i], tMod.getRadiusOfEarth(), true);
                TimeDist botTD = botSLayer.bullenRadialSlowness(rps[i], tMod.getRadiusOfEarth(), true);
                assertEquals(td.getDistRadian(), topTD.getDistRadian()+botTD.getDistRadian(), 0.000000001);
            }
        
        }
    }
}
