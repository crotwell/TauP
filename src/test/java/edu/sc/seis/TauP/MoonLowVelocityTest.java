package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MoonLowVelocityTest {


    @Test
    public void testTMod() throws Exception {

        // Moon_trial1 has low velocity zone at surface, and contains lvz that contains section with
        // positive velo slope, so tough test to sample correctly
        String lunarModel = "Moon_trial1";
        String lunarModelFile = lunarModel+".nd";

        SlownessModel smod;

        VelocityModel vmod = VelocityModelTest.loadTestVelMod(lunarModelFile);
        smod = new SphericalSModel(vmod);
        TauModel tmod = new TauModel(smod);
        assertNotNull(tmod); // just to make sure loaded
        TauModel tmoddepth = tmod.depthCorrect(933);
        assertNotNull(tmoddepth);
    }
}
