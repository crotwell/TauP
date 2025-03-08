package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

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
        assertTrue(factory.isLegDepth("35"));
        SeismicPhaseFactory.createSeismicPhases(phaseName, tMod, tMod.getSourceDepth(), 0, null, true);



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
}
