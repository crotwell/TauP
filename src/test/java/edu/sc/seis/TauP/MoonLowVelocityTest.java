package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoonLowVelocityTest {


    public void critReflCoreTest() throws TauModelException {
        TauModel tMod = TauModelLoader.load("iasp91");
        String phaseName = "Scs^20S";
        SeismicPhase phase = SeismicPhaseFactory.createPhase(phaseName, tMod, 0, 100);
        assertTrue(phase.phasesExistsInModel(), phase.failReason());
        assertTrue(phase.getMinRayParam() < phase.getMaxRayParam());
        assertEquals("Scs^20Scs", phase.getPuristName());
    }

    @Test
    public void testsplitForHighSlowness_B() throws Exception {
        String lunarModelFile = "MoonKhan_JGR_2014_mod1.nd";
        VelocityModel vmod = VelocityModelTest.loadTestVelMod(lunarModelFile);
        SlownessModel smod = new SphericalSModel(vmod);
        TauModel tmod = new TauModel(smod);
        ProtoSeismicPhase proto = ProtoSeismicPhase.startEmpty("P", tmod, 0);
        proto.addToBranch(3, true, true, PhaseInteraction.TURN, "P");
        proto.addToBranch(0, true, true, PhaseInteraction.END, "P");
        assertEquals("0 1 2 3 3 2 1 0 ", proto.branchNumSeqStr());
        List<ShadowOrProto> hszSplitProtoList = proto.splitForAllHighSlowness();
        assertEquals(3, hszSplitProtoList.size());
    }

    @Test
    public void testsplitForHighSlowness() throws Exception {
        String lunarModelFile = "MoonKhan_JGR_2014_mod1.nd";
        VelocityModel vmod = VelocityModelTest.loadTestVelMod(lunarModelFile);
        SlownessModel smod = new SphericalSModel(vmod);
        TauModel tmod = new TauModel(smod);
        assertEquals(1407.415, tmod.getCmbDepth());
        DepthRange hsz = smod.getHighSlowness(true)[0];
        SimpleSeismicPhase P_phase = SeismicPhaseFactory.createPhase("P", tmod, 0);
        System.err.println("P "+P_phase.getMaxRayParam()+" to "+P_phase.getMinRayParam()+" ");
        System.err.println("HSZ: "+hsz.topDepth+" "+hsz.botDepth);
        if (P_phase instanceof SimpleContigSeismicPhase) {
            SimpleContigSeismicPhase contigPhase = (SimpleContigSeismicPhase) P_phase;
            System.err.println("P_phase: " + contigPhase.proto.branchNumSeqStr());
        }
        for (TauBranch tb : tmod.tauBranches[0]) {
            System.err.println(tb);
        }
        assertInstanceOf(CompositeSeismicPhase.class, P_phase);
        CompositeSeismicPhase compPhase = (CompositeSeismicPhase)P_phase;
        assertEquals(2, compPhase.getSubPhaseList().size());
        SimpleContigSeismicPhase P_phase_above = compPhase.getSubPhaseList().get(0);
        System.err.println("above HSZ: "+P_phase_above.describe());
        SimpleContigSeismicPhase P_phase_below = compPhase.getSubPhaseList().get(1);
        System.err.println("below HSZ: "+P_phase_below.describe());

        assertEquals("0 1 1 0 ", P_phase_above.proto.branchNumSeqStr());
        assertEquals("0 1 2 3 3 2 1 0 ", P_phase_below.proto.branchNumSeqStr());
        assertEquals(P_phase_above.minRayParam, P_phase_below.maxRayParam);

        assertEquals(P_phase_above.maxRayParam, P_phase_above.getRayParams(0));
        assertEquals(P_phase_above.minRayParam, P_phase_above.getRayParams(P_phase_above.getNumRays()-1));
        assertEquals(P_phase_below.maxRayParam, P_phase_below.getRayParams(0));
        assertEquals(P_phase_below.minRayParam, P_phase_below.getRayParams(P_phase_below.getNumRays()-1));
        assertEquals(P_phase.getNumRays(), P_phase_above.getNumRays()+P_phase_below.getNumRays(), "num rayparams");

        System.err.println("above tbranch "+P_phase_above.proto.branchNumSeqStr());
        SeismicPhase P_phase_above_interp = P_phase_above.interpolatePhase(2);
        SeismicPhase P_phase_below_interp = P_phase_below.interpolatePhase(2);
        SeismicPhase P_phase_interp = P_phase.interpolatePhase(2);

        assertInstanceOf(SimpleContigSeismicPhase.class, P_phase_above_interp);
        assertInstanceOf(SimpleContigSeismicPhase.class, P_phase_below_interp);
        assertEquals(P_phase_above_interp.getMaxRayParam(), P_phase_above.getMaxRayParam());
        assertEquals(P_phase_above_interp.getMinRayParam(), P_phase_above.getMinRayParam());
        assertEquals(P_phase_below_interp.getMaxRayParam(), P_phase_below.getMaxRayParam());
        assertEquals(P_phase_below_interp.getMinRayParam(), P_phase_below.getMinRayParam());
        assertEquals(P_phase_above_interp.getMaxRayParam(), P_phase_interp.getMaxRayParam());
        assertEquals(P_phase_below_interp.getMinRayParam(), P_phase_interp.getMinRayParam());
        assertEquals(P_phase_interp.getNumRays(), P_phase_above_interp.getNumRays()+P_phase_below_interp.getNumRays(), "num rayparams");

    }

    @Test
    public void testHighSlownessDiscon() throws TauModelException, IOException, SlownessModelException {
        String model = "highSlownessDiscon.nd";

        VelocityModel vmod = VelocityModelTest.loadTestVelMod(model);
        SlownessModel smod = new SphericalSModel(vmod);
        TauModel tmod = new TauModel(smod);

        SimpleSeismicPhase P_phase = SeismicPhaseFactory.createPhase("P", tmod, 0);

        assertInstanceOf(CompositeSeismicPhase.class, P_phase);
        CompositeSeismicPhase compPhase = (CompositeSeismicPhase)P_phase;
        assertEquals(2, compPhase.getSubPhaseList().size());
        SimpleContigSeismicPhase P_phase_above = compPhase.getSubPhaseList().get(0);
        System.err.println("above HSZ: "+P_phase_above.describe());
        SimpleContigSeismicPhase P_phase_below = compPhase.getSubPhaseList().get(1);
        System.err.println("below HSZ: "+P_phase_below.describe());

    }

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

    @Test
    public void testMoonKhan_JGR_2014() throws SlownessModelException, VelocityModelException, IOException, TauModelException {
        String lunarModelFile = "MoonKhan_JGR_2014_mod1.nd";
        VelocityModel vmod = VelocityModelTest.loadTestVelMod(lunarModelFile);
        SlownessModel smod = new SphericalSModel(vmod);
        TauModel tmod = new TauModel(smod);
        SeismicPhase P_phase = SeismicPhaseFactory.createPhase("P", tmod, 800);
        DistanceRay dr = DistanceRay.ofDegrees(50);
        List<Arrival> arrivalList = dr.calculate(P_phase);
        assertEquals(1, arrivalList.size());
        assertEquals(521.75, arrivalList.get(0).getTime(), 0.01);
        SeismicPhase Pdiff = SeismicPhaseFactory.createPhase("Pdiff", tmod);
        // think this phase actually should exist???
        assertTrue(Pdiff.phasesExistsInModel());

    }

    @Test
    public void testMarsLiquidLowerMantle() throws TauModelException, IOException, SlownessModelException {
        String mars = "MarsLiquidLowerMantle.nd";
        VelocityModel vmod = VelocityModelTest.loadTestVelMod(mars);
        SlownessModel smod = new SphericalSModel(vmod);
        TauModel tmod = new TauModel(smod);
        SeismicPhase Pdiff = SeismicPhaseFactory.createPhase("Pdiff", tmod);
        assertTrue(Pdiff.phasesExistsInModel());
        SeismicPhase PdiffUnderRefl = SeismicPhaseFactory.createPhase("Pdiff^1554Pdiff", tmod);
        assertTrue(PdiffUnderRefl.phasesExistsInModel());
        SeismicPhase PCMBHead = SeismicPhaseFactory.createPhase("P1690n", tmod);
        assertFalse(PCMBHead.phasesExistsInModel());
    }
}
