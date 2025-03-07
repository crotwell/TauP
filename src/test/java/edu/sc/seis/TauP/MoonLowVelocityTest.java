package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoonLowVelocityTest {

    @Test
    public void testsplitForHighSlowness() throws Exception {
        String lunarModelFile = "MoonKhan_JGR_2014_mod1.nd";
        VelocityModel vmod = VelocityModelTest.loadTestVelMod(lunarModelFile);
        SlownessModel smod = new SphericalSModel(vmod);
        TauModel tmod = new TauModel(smod);
        DepthRange hsz = smod.getHighSlowness(true)[0];
        SimpleSeismicPhase P_phase = SeismicPhaseFactory.createPhase("P", tmod, 0);
        List<ProtoSeismicPhase> split = P_phase.getProto().splitForHighSlowness(hsz, true);
        assertEquals(2, split.size());
        System.err.println("HSZ: "+hsz.topDepth+" "+hsz.botDepth);
        System.err.println("above HSZ: "+split.get(0).branchNumSeqStr());
        System.err.println("below HSZ: "+split.get(1).branchNumSeqStr());
        List<SimpleSeismicPhase> contigList = SeismicPhaseFactory.sumBranches(tmod, split.get(0));
        assertEquals(2, contigList.size());
        SimpleSeismicPhase P_phase_above = contigList.get(0);
        System.err.println("above HSZ: "+P_phase_above.describe());
        SimpleSeismicPhase P_phase_below = contigList.get(1);
        System.err.println("below HSZ: "+P_phase_below.describe());
        assertEquals(P_phase_above.minRayParam, P_phase_below.maxRayParam);

        List<ProtoSeismicPhase> allSplit = P_phase.getProto().splitForAllHighSlowness();
        System.err.println("allSplit: "+allSplit.size());
        assertTrue(false, "autofail");

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
