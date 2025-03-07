package edu.sc.seis.TauP;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void critDepthsInSMod() {
        SlownessModel sMod = tmod.getSlownessModel();
        for(int critNum = 0; critNum < sMod.getNumCriticalDepths() - 1; critNum++) {
            System.err.println(critNum+" "+sMod.getCriticalDepth(critNum).getDepth()+" "+sMod.getSlownessLayer(critNum, false));
        }
        System.err.println("TauBranch");
        for (int bNum = 0; bNum < tmod.getNumBranches(); bNum++) {
            System.err.println(bNum+" "+tmod.getTauBranch(bNum, false).getBotDepth());
        }
        assertTrue(false);
    }

    @Test
    public void testShadowSPhase() throws TauModelException, SlownessModelException {
        SimpleSeismicPhase phaseSS = SeismicPhaseFactory.createPhase("S", tmod);

        List<ShadowZone> shadowZoneList = phaseSS.getShadowZones();
        assertTrue(shadowZoneList!= null);
        for (ShadowZone shadow : shadowZoneList) {
            System.err.println("rp: "+shadow.rayParam+" "+shadow.preArrival.getRayCalculateable()+" "+shadow.postArrival.getRayCalculateable());
            System.err.println("ph idx "+phaseSS.minRayParamIndex+" "+phaseSS.maxRayParamIndex);
            System.err.println("sh rp idx "+shadow.rayParamIndex+" "+phaseSS.getRayParams(shadow.rayParamIndex)+" "+phaseSS.getRayParams(shadow.rayParamIndex+1));
            System.err.println("      "+Arrival.toStringHeader());
            System.err.println("pre:  "+shadow.preArrival+" "+shadow.preArrival.getDeepestPierce());
            System.err.println("post: "+shadow.postArrival+" "+shadow.postArrival.getDeepestPierce());
        }
        assertTrue(false);
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
    public void testSSPhaseForRP() throws TauModelException, SlownessModelException {
        double badRP = 737.7642067530021;
        SeismicPhase phaseSS = SeismicPhaseFactory.createPhase("SS", tmod);
        Arrival arrival = phaseSS.shootRay(badRP);
    }
}
