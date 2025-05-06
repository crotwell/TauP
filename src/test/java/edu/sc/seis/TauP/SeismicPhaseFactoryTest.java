package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.cmdline.args.PhaseArgs.extractPhaseNames;
import static org.junit.jupiter.api.Assertions.*;

public class SeismicPhaseFactoryTest {

    @Test
    public void phase_s35p() throws TauModelException {
        double sourceDepth = 100;
        double receiverDepth = 0;
        boolean debug = true;
        TauModel tMod = TauModelLoader.load("ak135").depthCorrect(100);
        String phaseName = "s35p";
        SeismicPhaseFactory factory = new SeismicPhaseFactory(phaseName, tMod, sourceDepth, receiverDepth, debug);
        assertTrue(PhaseSymbols.isBoundary("35"));
        assertEquals(tMod.getMohoDepth(), LegPuller.legAsDepthBoundary(tMod, "35"));
        List<SeismicPhase> phaseList = SeismicPhaseFactory.createSeismicPhases(phaseName, tMod, tMod.getSourceDepth(), 0, null, true);
        for (SeismicPhase phase : phaseList) {
            assertTrue(phase.phasesExistsInModel());
        }
    }

    @Test
    public void testPuristName() throws Exception {
        double sourceDepth = 100;
        double receiverDepth = 0;
        boolean debug = false;
        TauModel tMod = TauModelLoader.load("ak135");
        TauModel tModDepth = tMod.depthCorrect(sourceDepth);
        List<SeismicPhase> phaseList;
        /*
        String phaseName = "p";
         phaseList =
                SeismicPhaseFactory.createSeismicPhases(phaseName, tModDepth, tModDepth.getSourceDepth(), receiverDepth, null, debug);
        assertTrue(phaseList.size()>0);
        SeismicPhase p_Phase = phaseList.get(0);
        assertEquals("p", p_Phase.getPuristName());
*/
        String Pn_name = "Pn";
        phaseList =
                SeismicPhaseFactory.createSeismicPhases(Pn_name, tMod, tMod.getSourceDepth(), receiverDepth, null, debug);
        assertTrue(phaseList.size()>0);
        SeismicPhase Pn_Phase = phaseList.get(0);
        assertEquals("Pn", Pn_Phase.getPuristName());
        String PnPn_name = "PnPn";
        phaseList =
                SeismicPhaseFactory.createSeismicPhases(PnPn_name, tMod, tMod.getSourceDepth(), receiverDepth, null, debug);
        assertTrue(phaseList.size()>0);
        SeismicPhase PnPn_Phase = phaseList.get(0);
        assertEquals("PnPn", PnPn_Phase.getPuristName());
    }

    @Test
    public void purestName_ttall() throws Exception {
        TauModel tMod = TauModelLoader.load("ak135");
        double sourceDepth = 100;
        TauModel tModDepth = tMod.depthCorrect(sourceDepth);
        List<String> ttall = extractPhaseNames("ttall");
        for (String name : ttall) {
            ArrayList<String> legs = LegPuller.legPuller(name);
            String leg_puristName = LegPuller.createPuristName(tModDepth, legs);
            SimpleSeismicPhase phase = SeismicPhaseFactory.createPhase(name, tModDepth, sourceDepth);
            assertInstanceOf(SimpleContigSeismicPhase.class, phase);
            SimpleContigSeismicPhase contigPhase = (SimpleContigSeismicPhase)phase;
            ProtoSeismicPhase proto = contigPhase.proto;
            proto.phaseName = name;
            proto.validateSegList();


            if (proto.isSuccessful()){
                assertTrue(proto.getPuristName().equalsIgnoreCase(leg_puristName),
                        "WARN purist name not same as legpuller: " + proto.getPuristName() + " " + leg_puristName);
            }
        }
    }


    @Test
    public void purestName_Pdiffs() throws Exception {
        double sourceDepth = 0.0;
        TauModel tMod = TauModelLoader.load("ak135");
        String name = "Pdiffs";
        ArrayList<String> legs = LegPuller.legPuller(name);
        String leg_puristName = LegPuller.createPuristName(tMod, legs);

        SimpleSeismicPhase phase = SeismicPhaseFactory.createPhase(name, tMod, sourceDepth);
        assertInstanceOf(SimpleContigSeismicPhase.class, phase);
        SimpleContigSeismicPhase contigPhase = (SimpleContigSeismicPhase)phase;
        ProtoSeismicPhase proto = contigPhase.proto;
        proto.phaseName = name;
        proto.validateSegList();
        assertTrue(proto.isSuccessful());
        assertTrue(proto.getPuristName().equalsIgnoreCase(leg_puristName),
                "WARN purist name not same as legpuller: " + proto.getPuristName() + " " + leg_puristName);
        assertTrue(proto.getPuristName().equalsIgnoreCase(name),
                "WARN purist name not same as name: " + proto.getPuristName() + " " + name);
        assertTrue(phase.getPuristName().equalsIgnoreCase(name),
                "WARN purist name not same as name: " + proto.getPuristName() + " " + name);
    }

    @Test
    public void branchSeqForPhase() throws TauModelException, IOException, SlownessModelException {
        String mars = "MarsLiquidLowerMantle.nd";
        double sourceDepth = 0.0;
        String name = "P";
        VelocityModel vmod = VelocityModelTest.loadTestVelMod(mars);
        SlownessModel smod = new SphericalSModel(vmod);
        TauModel tMod = new TauModel(smod);

        SimpleSeismicPhase phase = SeismicPhaseFactory.createPhase(name, tMod, sourceDepth);
        assertInstanceOf(SimpleContigSeismicPhase.class, phase);
        SimpleContigSeismicPhase contigPhase = (SimpleContigSeismicPhase)phase;
        double rayParam = 370; // turns above, but can exist in the high slowness near CMB
        List<TauBranch> branchList = SeismicPhaseFactory.calcBranchSeqForRayparam(contigPhase.getProto(), rayParam);

        assertEquals(8, branchList.size());
        // 1554.45 is above liquid cmb zone, rp 370 turns above this, so branch seq should not include liquid layer
        assertEquals(1554.45, branchList.get(3).getBotDepth(), 0.001);

        double deepestRP = 195.7971447472312;
        assertEquals(deepestRP, contigPhase.getMinRayParam(), 0.01);
        List<TauBranch> deepBranchList = SeismicPhaseFactory.calcBranchSeqForRayparam(contigPhase.getProto(), deepestRP);
        assertEquals(8, deepBranchList.size());
        // 1554.45 is above liquid cmb zone, rp 195 turns just above this, so branch seq should not include liquid layer
        assertEquals(1554.45, deepBranchList.get(3).getBotDepth(), 0.001);

        SimpleSeismicPhase PcP_phase = SeismicPhaseFactory.createPhase("PcP", tMod, sourceDepth);
        assertInstanceOf(SimpleContigSeismicPhase.class, PcP_phase);
        SimpleContigSeismicPhase contigPcP_phase = (SimpleContigSeismicPhase)PcP_phase;
        List<TauBranch> PcPBranchList = SeismicPhaseFactory.calcBranchSeqForRayparam(contigPcP_phase.getProto(), 0);
        assertEquals(10, PcPBranchList.size());
        assertEquals(deepestRP, contigPcP_phase.getMaxRayParam(), 0.0001);

    }
}
