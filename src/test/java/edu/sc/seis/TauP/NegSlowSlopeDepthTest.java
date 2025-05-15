package edu.sc.seis.TauP;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NegSlowSlopeDepthTest {
    public static String modname = "negSlowSlopeDepth.nd";
    TauModel tmod;

    @BeforeEach
    public void setUp() throws Exception {
        VelocityModel vmod = VelocityModelTest.loadTestVelMod(modname);
        SlownessModel smod = new SphericalSModel(vmod);
        tmod = new TauModel(smod);
    }

    @Test
    public void testShadowSPhase() throws TauModelException, SlownessModelException {
        SimpleSeismicPhase phaseSS = SeismicPhaseFactory.createPhase("S", tmod);

        List<ShadowZone> shadowZoneList = phaseSS.getShadowZones();
        assertTrue(shadowZoneList!= null);
        assertInstanceOf(CompositeSeismicPhase.class, phaseSS);
        CompositeSeismicPhase compPhaseS = (CompositeSeismicPhase)phaseSS;
        assertEquals(2, compPhaseS.getSubPhaseList().size());
        SimpleContigSeismicPhase pre = compPhaseS.getSubPhaseList().get(0);
        SimpleContigSeismicPhase post = compPhaseS.getSubPhaseList().get(1);
        for (ShadowZone shadow : shadowZoneList) {
            assertEquals(pre.getMinRayParam(), shadow.rayParam);
            assertEquals(post.getMaxRayParam(), shadow.rayParam);
        }
    }

    @Test
    public void testSSPhase() throws TauModelException, SlownessModelException {
        double badDist = 126;
        DistanceRay dr = DistanceRay.ofDegrees(126);
        SeismicPhase phaseSS = SeismicPhaseFactory.createPhase("SS", tmod);
        List<Arrival> arrivalList = dr.calculate(phaseSS);
        assertTrue(arrivalList!= null);
    }

    @Test
    public void testSSPhaseForRP() throws TauPException {
        double badRP = 737.7642067530021;
        SeismicPhase phaseSS = SeismicPhaseFactory.createPhase("SS", tmod);
        Arrival arrival = phaseSS.shootRay(badRP);
        assertNotNull(arrival);
    }
}
