package edu.sc.seis.TauP;


import java.io.BufferedReader;
import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


public class MoonTest extends TestCase {

    public static final String lunarModel = "MoonQR420.0";
    public static final String lunarModelFile = lunarModel+".nd";
    
    public SlownessModel smod;
    
    @Before
    public void setUp() throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(this.getClass()
                                                                     .getClassLoader()
                                                                     .getResourceAsStream("edu/sc/seis/TauP/" + lunarModelFile)));
        VelocityModel vmod = VelocityModel.readNDFile(in, lunarModel);
        smod = new SphericalSModel(vmod);
    }
    
    @Test
    @Ignore
    public void testlayerNumberForDepth() throws Exception {
        boolean SWave = false;
        boolean PWave = true;
        for (int i = 0; i < 2; i++) {
            assertEquals(0, smod.layerNumForDepth(0, PWave));
            assertEquals(0, smod.layerNumberAbove(0, PWave));
            assertEquals(0, smod.layerNumberBelow(0, PWave));
            int lastLayer = smod.getNumLayers(PWave)-1;
            assertEquals(lastLayer, smod.layerNumForDepth(smod.getRadiusOfEarth(), PWave));
            assertEquals(lastLayer, smod.layerNumberAbove(smod.getRadiusOfEarth(), PWave));
            assertEquals(lastLayer, smod.layerNumberBelow(smod.getRadiusOfEarth(), PWave));
        }
        
        double depth = 1320.0;
        int foundLayerNum = smod.layerNumForDepth(depth, PWave);
        assertTrue(538 <= foundLayerNum && foundLayerNum <= 547 );
        assertEquals(538, smod.layerNumberAbove(depth, PWave));
        assertEquals(545, smod.layerNumberBelow(depth, PWave));
        assertEquals(545, smod.layerNumForDepth(depth+0.01, PWave));
        assertEquals(538, smod.layerNumForDepth(depth-0.01, PWave));
        
        foundLayerNum = smod.layerNumForDepth(depth, SWave);
        assertTrue(533 <= foundLayerNum && foundLayerNum <= 536 );
        assertEquals(533, smod.layerNumberAbove(depth, SWave));
        assertEquals(536, smod.layerNumberBelow(depth, SWave));
        assertTrue(smod.getSlownessLayer(smod.layerNumberBelow(depth, SWave), SWave).containsDepth(depth));
        assertEquals(536, smod.layerNumForDepth(depth+0.01, SWave));
        assertEquals(533, smod.layerNumForDepth(depth-0.01, SWave));

        assertEquals(220, smod.layerNumForDepth(24, PWave));
        assertEquals(220, smod.layerNumberAbove(24, PWave));
        assertEquals(220, smod.layerNumberBelow(24, PWave));
        
        foundLayerNum = smod.layerNumForDepth(38, PWave);
        assertTrue(285 <= foundLayerNum && foundLayerNum <= 286);
        assertEquals(285, smod.layerNumberAbove(38, PWave));
        assertEquals(286, smod.layerNumberBelow(38, PWave));
        
        assertEquals(285, smod.layerNumForDepth(37.99, PWave));
        assertEquals(285, smod.layerNumberAbove(37.99, PWave));
        assertEquals(285, smod.layerNumberBelow(37.99, PWave));
        assertEquals(286, smod.layerNumForDepth(38.01, PWave));
        assertEquals(286, smod.layerNumberAbove(38.01, PWave));
        assertEquals(286, smod.layerNumberBelow(38.01, PWave));
    }
    
    @Test
    public void testTMod() throws Exception {
        TauModel tmod = new TauModel(smod);
        TauModel tmoddepth = tmod.depthCorrect(933);
    }
}
