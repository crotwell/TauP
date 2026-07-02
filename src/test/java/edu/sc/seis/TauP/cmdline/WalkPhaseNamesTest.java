package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            System.err.println("proto: "+protoSP.describe());
            System.err.println("from name: "+sp.describe());
            assertEquals(protoSP.getMinRayParam(), sp.getMinRayParam(), pName);
            assertEquals(protoSP.getMaxRayParam(), sp.getMaxRayParam(), pName);
            assertEquals(protoSP.getPuristName(), pName);
            assertEquals(sp.getPuristName(), pName);
            //assertEquals(p.branchNumSeqStr(), ((SimpleContigSeismicPhase)sp).getProto().branchNumSeqStr(), pName);
            //assertEquals(p.branchNumSeqStrWithSegBreaks(), ((SimpleContigSeismicPhase)sp).getProto().branchNumSeqStrWithSegBreaks());
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
        find.excludeDepthNames.addAll(List.of("20", "210"));
        SeismicPhaseWalk allwalker = find.createWalker(tMod, receiverDepth, find.getExcludedDepths(tMod));
        List<ProtoSeismicPhase> allwalk = allwalker.findEndingPaths(maxActions);
        for (ProtoSeismicPhase p : allwalk) {
            SeismicPhase protoSP = p.asSeismicPhase();
            String pName = p.getPuristName();
            SeismicPhase sp = SeismicPhaseFactory.createPhase(pName, tMod, sourceDepth, receiverDepth);
            assertTrue(sp.phasesExistsInModel(), pName);
            assertTrue(sp instanceof SimpleContigSeismicPhase, pName);
            SimpleContigSeismicPhase scp = (SimpleContigSeismicPhase)sp;
            //System.err.println("proto: "+protoSP.describe());
            //System.err.println("from name: "+sp.describe());
            assertEquals(protoSP.getMinRayParam(), sp.getMinRayParam(), pName);
            assertEquals(protoSP.getMaxRayParam(), sp.getMaxRayParam(), pName);
            assertEquals(protoSP.getPuristName(), pName);
            assertEquals(sp.getPuristName(), pName);
            assertEquals(p.getSegmentList().size(), scp.getPhaseSegments().size());
            for (int i = 0; i < p.getSegmentList().size(); i++) {
                SeismicPhaseSegment protoSeg = p.getSegmentList().get(i);
                SeismicPhaseSegment phaseSeg = scp.getPhaseSegments().get(i);
                assertEquals(protoSeg.getIsPWave(), phaseSeg.getIsPWave());
                assertEquals(protoSeg.getIsFlat(), phaseSeg.getIsFlat());
                assertEquals(protoSeg.getEndAction(), phaseSeg.getEndAction());
                assertEquals(protoSeg.getPrevEndAction(), phaseSeg.getPrevEndAction());
                assertEquals(protoSeg.getMinRayParam(), phaseSeg.getMinRayParam());
                assertEquals(protoSeg.getMaxRayParam(), phaseSeg.getMaxRayParam());
                assertEquals(protoSeg.getDepthRange()[0], phaseSeg.getDepthRange()[0]);

            }
            //assertEquals(p.branchNumSeqStr(), ((SimpleContigSeismicPhase) sp).getProto().branchNumSeqStr(), pName);
            //assertEquals(p.branchNumSeqStrWithSegBreaks(), ((SimpleContigSeismicPhase)protoSP).getProto().branchNumSeqStrWithSegBreaks());
            //assertEquals(p.branchNumSeqStrWithSegBreaks(), ((SimpleContigSeismicPhase)sp).getProto().branchNumSeqStrWithSegBreaks());
        }
    }
}
