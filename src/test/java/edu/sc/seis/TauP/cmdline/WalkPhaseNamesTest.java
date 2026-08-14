package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static edu.sc.seis.TauP.PhaseInteraction.TURN;
import static org.junit.jupiter.api.Assertions.*;

public class WalkPhaseNamesTest {

    public static final String marsModelFile = "MarsKahnMay2025.nd";
    public TauModel mars;

    @BeforeEach
    public void setUp() throws Exception {
        VelocityModel vmod = VelocityModelTest.loadTestVelMod(marsModelFile);
        mars = TauModelLoader.createTauModel(vmod);
    }

    @Test
    public void walkNamesMars() throws Exception {
        double sourceDepth = 100;
        double receiverDepth = 0;
        TauModel tMod = mars.depthCorrect(sourceDepth);
        int maxActions = 3;
        TauP_Find find = new TauP_Find();
        // only  moho, cmb, iocb
        find.onlyNamedDiscon = true;
        find.onlyPWave = true;
        find.excludeDepthNames.addAll(List.of("1", "10" )); // moho 54, liquid silica 1540, cmb 1667 iocb 3389.5
        SeismicPhaseWalk allwalker = find.createWalker(tMod, receiverDepth, find.getExcludedDepths(tMod));
        List<ProtoSeismicPhase> allwalk = allwalker.findEndingPaths(maxActions);
        for (ProtoSeismicPhase p : allwalk) {
            SeismicPhase protoSP = p.asSeismicPhase();
            String pName = p.getPuristName();
            SeismicPhase sp = SeismicPhaseFactory.createPhase(pName, tMod, sourceDepth, receiverDepth);
            assertTrue(sp.phasesExistsInModel(), pName);
            assertTrue(sp instanceof SimpleContigSeismicPhase, pName);
            assertEquals(protoSP.getMinRayParam(), sp.getMinRayParam(), pName);
            assertEquals(protoSP.getMaxRayParam(), sp.getMaxRayParam(), pName);
            assertEquals(protoSP.getPuristName(), pName);
            assertEquals(sp.getPuristName(), pName);
        }

    }
    @Test
    public void walkNamesIasp91() throws Exception {
        double sourceDepth = 100;
        double receiverDepth = 0;
        TauModel tMod = TauModelLoader.load("iasp91").depthCorrect(sourceDepth);
        int maxActions = 3;
        TauP_Find find = new TauP_Find();
        find.onlyPWave = true;
        find.excludeDepthNames.addAll(List.of("20", "210", "660", "moho"));
        SeismicPhaseWalk allwalker = find.createWalker(tMod, receiverDepth, find.getExcludedDepths(tMod));
        List<ProtoSeismicPhase> allwalk = allwalker.findEndingPaths(maxActions);

        for (int i = 0; i < allwalk.size(); i++) {
            ProtoSeismicPhase p = allwalk.get(i);
            assertTrue(p.getPuristName().startsWith("p") || p.getPuristName().startsWith("P"),
                    p.getPuristName()+" "+p.branchNumSeqStrWithSegBreaks());
            for (ProtoSeismicPhase pp : allwalk.subList(i+1, allwalk.size())) {
                assertNotEquals(p.getPuristName(), pp.getPuristName(), i+" \n"+p.branchNumSeqStrWithSegBreaks()+"\n"+pp.branchNumSeqStrWithSegBreaks());
            }
        }
        for (int idx = 0; idx < allwalk.size(); idx++) {
            ProtoSeismicPhase p = allwalk.get(idx);
            SeismicPhase protoSP = p.asSeismicPhase();
            assertEquals(p.branchNumSeqStrWithSegBreaks(), protoSP.branchNumSeqStrWithSegBreaks());
            String pName = p.getPuristName();
            assertFalse(pName.startsWith("K"), p.getPuristName()+" "+p.branchNumSeqStrWithSegBreaks());
            SeismicPhase sp = null;
            try {
                sp = SeismicPhaseFactory.createPhase(pName, tMod, sourceDepth, receiverDepth);
            } catch (PhaseParseException e) {
                assertFalse(true, e.getMessage()+" "+pName);
            }
            assertTrue(sp.phasesExistsInModel(), pName);
            assertTrue(sp instanceof SimpleContigSeismicPhase, pName);
            SimpleContigSeismicPhase scp = (SimpleContigSeismicPhase)sp;

            // subtleties with ray param. ex PPPv410p. Is max rp the ray that turns below 410, or does it
            // include the critically reflected ray at 410, like PV410pPV410pPv410p
            assertEquals(protoSP.getMinRayParam(), sp.getMinRayParam(), pName);
            assertEquals(protoSP.getMaxRayParam(), sp.getMaxRayParam(), pName+" "+p.branchNumSeqStrWithSegBreaks()+" sp "+ sp.branchNumSeqStrWithSegBreaks());
            assertEquals(protoSP.getPuristName(), pName);
            assertEquals(sp.getPuristName(), pName);
            assertEquals(p.getSegmentList().size(), scp.getPhaseSegments().size());
            for (int i = 0; i < p.getSegmentList().size(); i++) {
                SeismicPhaseSegment protoSeg = p.getSegmentList().get(i);
                SeismicPhaseSegment phaseSeg = scp.getPhaseSegments().get(i);
                String msg = pName+" seg: "+i;
                assertEquals(protoSeg.getIsPWave(), phaseSeg.getIsPWave(), msg);
                assertEquals(protoSeg.getIsFlat(), phaseSeg.getIsFlat(), msg);
                assertEquals(protoSeg.getEndAction(), phaseSeg.getEndAction(), msg);
                assertEquals(protoSeg.getPrevEndAction(), phaseSeg.getPrevEndAction(), msg);
                assertEquals(protoSeg.getStartBranch(), phaseSeg.getStartBranch(), msg);
                assertEquals(protoSeg.getEndBranch(), phaseSeg.getEndBranch(), msg);
                assertEquals(protoSeg.getDepthRange()[0], phaseSeg.getDepthRange()[0], p.getPuristName()+" seg: "+i);

                // ok if all segments don't match ray param range as long as overall range matches
                //assertEquals(protoSeg.getMinRayParam(), phaseSeg.getMinRayParam(), p.getPuristName()+" seg: "+i);
                //assertEquals(protoSeg.getMaxRayParam(), phaseSeg.getMaxRayParam());

            }
            assertEquals(p.branchNumSeqStr(), scp.getProto().branchNumSeqStr(), pName);
            //assertEquals(p.branchNumSeqStrWithSegBreaks(), ((SimpleContigSeismicPhase)protoSP).getProto().branchNumSeqStrWithSegBreaks(), pName);
            //assertEquals(p.branchNumSeqStrWithSegBreaks(), scp.getProto().branchNumSeqStrWithSegBreaks(), pName);
        }
    }
}
