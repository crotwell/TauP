package edu.sc.seis.TauP;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class OuterCoreDisconTest {


    public OuterCoreDisconTest() throws VelocityModelException, SlownessModelException, TauModelException, IOException {
        vMod = VelocityModelTest.loadTestVelMod(modelName);
        TauP_Create taupCreate = new TauP_Create();
        tMod = taupCreate.createTauModel(vMod);
    }

    String modelName = "outerCoreDiscon.nd";
    VelocityModel vMod;
    TauModel tMod;
    // all should have arrival at 0
    String[] zeroDistPhaseNames = {
            "PKiKP", "PKv3000kp", "PKik^3000KiKP", "PKv3000kKiKP",
            "PKIIKP", "PKIv5500Ikp", "PKI^5500IKP", "PKIv5500IKP",
            "PKIIkp", "PKIv5500ykp"
    };
    String[] otherPhaseNames = {
            "PKIv5500yIkpPKIKP"
    };


    @Test
    void zeroDistPhasesTest() throws TauModelException {
        boolean debug = false;
        for(String name : zeroDistPhaseNames) {
            SeismicPhase phase = SeismicPhaseFactory.createPhase(name, tMod, tMod.getSourceDepth(), 0, debug);
            List<Arrival> arrivals = phase.calcTime(0);
            assertEquals( 1, arrivals.size(), name);
        }
    }

    @Test
    void PhasesExistTest() throws TauModelException {
        boolean debug = false;
        for(String name : otherPhaseNames) {
            SeismicPhase phase = SeismicPhaseFactory.createPhase(name, tMod, tMod.getSourceDepth(), 0, debug);
            List<Arrival> arrivals = phase.calcTime(120);
            // dumb check, just to make sure calc happens without exception
            assertNotNull( arrivals, name);
        }
    }

    @Test
    void notSamePcP() throws TauModelException {
        SeismicPhase phase_PcP = SeismicPhaseFactory.createPhase("PcP", tMod);
        List<Arrival> arrivals_PcP = phase_PcP.calcTime(0);
        SeismicPhase phase_PKv3000KP = SeismicPhaseFactory.createPhase("PKv3000KP", tMod);
        List<Arrival> arrivals_PKv3000KP = phase_PKv3000KP.calcTime(0);
        assertEquals(arrivals_PcP.size(), arrivals_PKv3000KP.size());
        assertNotEquals(arrivals_PcP.get(0).getTime(), arrivals_PKv3000KP.get(0).getTime());
    }


    @Test
    void deepSource() throws TauModelException {
        double depth = 3050;
        TauModel tauModelDepth = tMod.depthCorrect(depth);
        SeismicPhase phase_kP = SeismicPhaseFactory.createPhase("kP", tMod, depth);
        List<Arrival> arrivals_kP = phase_kP.calcTime(0);
        assertEquals( 1, arrivals_kP.size(), "kP");

        SeismicPhase phase_kKv3000kp = SeismicPhaseFactory.createPhase("kKv3000kp", tMod, depth);
        List<Arrival> arrivals_kKv3000kp = phase_kKv3000kp.calcTime(0);
        assertEquals( 1, arrivals_kKv3000kp.size(), "kP");

        SeismicPhase phase_under_ref = SeismicPhaseFactory.createPhase("k^3000KIKP", tMod, depth);
        List<Arrival> arrivals__under_ref = phase_under_ref.calcTime(180);
        assertEquals(1, arrivals__under_ref.size());
    }
}
