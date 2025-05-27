package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class OuterCoreDisconTest {

    public static String customDisconUnob = "molten-unobtainium";

    public OuterCoreDisconTest() throws VelocityModelException, SlownessModelException, TauModelException, IOException {
        vMod = VelocityModelTest.loadTestVelMod(modelName);
        tMod = TauModelLoader.createTauModel(vMod);
    }

    String modelName = "outerCoreDiscon.nd";
    VelocityModel vMod;
    TauModel tMod;
    // all should have arrival at 0
    String[] zeroDistPhaseNames = {
            "PKiKP", "PKv3000kp", "PKik^3000KiKP", "PKv3000kKiKP",
            "PKIIKP", "PKIv5500Ikp", "PKI^5500IKP", "PKIv5500IKP",
            "PKIIkp", "PKIv5500ykp",
            "PKv"+PhaseSymbols.NAMED_DISCON_START+customDisconUnob+PhaseSymbols.NAMED_DISCON_END+"KP"
    };
    String[] otherPhaseNames = {
            "PKIv5500yIkpPKIKP", "PKI5500JKP"
    };

    @Test
    public void branchForCustomDiscon() throws TauModelException {
        String customInPhase = PhaseSymbols.NAMED_DISCON_START+customDisconUnob+PhaseSymbols.NAMED_DISCON_END;
        int disconBranch = LegPuller.closestDisconBranchToDepth(tMod, customInPhase, 10);
        assertNotEquals(-1, disconBranch);
        NamedVelocityDiscon namedDiscon = null;
        for (NamedVelocityDiscon nd : tMod.getVelocityModel().namedDiscon) {
            if (nd.getName().equalsIgnoreCase(customDisconUnob) || nd.getPreferredName().equalsIgnoreCase(customDisconUnob)) {
                namedDiscon = nd;
            }
        }
        assertNotNull(namedDiscon);
        int found = tMod.findBranch(namedDiscon.getDepth());
        assertEquals(7, found);
        assertEquals(7, disconBranch);
    }

    @Test
    void zeroDistPhasesTest() throws TauModelException {
        boolean debug = false;
        for(String name : zeroDistPhaseNames) {
            SeismicPhase phase = SeismicPhaseFactory.createPhase(name, tMod, tMod.getSourceDepth(), 0, debug);
            assertTrue(phase.phasesExistsInModel(), name+" exists");
            List<Arrival> arrivals = DistanceRay.ofDegrees(0).calculate(phase);
            assertEquals( 1, arrivals.size(), name);
        }
    }

    @Test
    void PhasesExistTest() throws TauModelException {
        boolean debug = false;
        for(String name : otherPhaseNames) {
            SeismicPhase phase = SeismicPhaseFactory.createPhase(name, tMod, tMod.getSourceDepth(), 0, debug);
            List<Arrival> arrivals = DistanceRay.ofDegrees(120).calculate(phase);
            // dumb check, just to make sure calc happens without exception
            assertNotNull( arrivals, name);
        }
    }

    @Test
    void notSamePcP() throws TauModelException {
        SeismicPhase phase_PcP = SeismicPhaseFactory.createPhase("PcP", tMod);
        List<Arrival> arrivals_PcP = DistanceRay.ofDegrees(0).calculate(phase_PcP);
        SeismicPhase phase_PKv3000KP = SeismicPhaseFactory.createPhase("PKv3000KP", tMod);
        List<Arrival> arrivals_PKv3000KP = DistanceRay.ofDegrees(0).calculate(phase_PKv3000KP);
        assertEquals(arrivals_PcP.size(), arrivals_PKv3000KP.size());
        assertNotEquals(arrivals_PcP.get(0).getTime(), arrivals_PKv3000KP.get(0).getTime());
    }


    @Test
    void deepSource() throws TauModelException {
        double depth = 3050;
        TauModel tauModelDepth = tMod.depthCorrect(depth);
        SeismicPhase phase_kP = SeismicPhaseFactory.createPhase("kP", tMod, depth);
        List<Arrival> arrivals_kP = DistanceRay.ofDegrees(0).calculate(phase_kP);
        assertEquals( 1, arrivals_kP.size(), "kP");

        SeismicPhase phase_kKv3000kp = SeismicPhaseFactory.createPhase("kKv3000kp", tMod, depth);
        List<Arrival> arrivals_kKv3000kp = DistanceRay.ofDegrees(0).calculate(phase_kKv3000kp);
        assertEquals( 1, arrivals_kKv3000kp.size(), "kP");

        SeismicPhase phase_under_ref = SeismicPhaseFactory.createPhase("k^3000KIKP", tMod, depth);
        List<Arrival> arrivals__under_ref = DistanceRay.ofDegrees(180).calculate(phase_under_ref);
        assertEquals(1, arrivals__under_ref.size());
    }
}
