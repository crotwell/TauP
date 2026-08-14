package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.sc.seis.TauP.PhaseInteraction.*;
import static edu.sc.seis.TauP.SeismicPhase.PWAVE;
import static edu.sc.seis.TauP.SeismicPhase.SWAVE;
import static org.junit.jupiter.api.Assertions.*;

public class WalkPhaseTest {


    @Test
    public void findEndDisconTest() throws TauModelException {
        double sourceDepth = 5.0;
        double receiverDepth = 0.0;
        TauModel tMod = TauModelLoader.load("iasp91").depthCorrect(sourceDepth);
        tMod = tMod.splitBranch(6);
        int endBranchNum = ProtoSeismicPhase.findEndDiscon(tMod, 2, true, LayerPropogationType.UP);
        assertEquals(0, endBranchNum);
        int turnEndBranchNum = new SeismicNamingLayers(tMod).disconBranchBelow(2);
        assertEquals(tMod.getCmbBranch(), turnEndBranchNum, "turn at cmb");
    }

    @Test
    public void startingLegs() throws TauModelException {
        double sourceDepth = 100.0;
        double receiverDepth = 0.0;
        TauModel tMod = TauModelLoader.load("iasp91").depthCorrect(sourceDepth);
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<ProtoSeismicPhase> outTree = walker.createSourceSegments(tMod, SlownessModel.PWAVE, receiverDepth);
        for (ProtoSeismicPhase p : outTree) {
            assertNotEquals(sourceDepth, p.endSegment().getEndDepth(), p.getPuristName()+" "+p.branchNumSeqStrWithSegBreaks());
        }
        List<ProtoSeismicPhase> nextTree = new ArrayList<>();
        for (ProtoSeismicPhase p : outTree) {
            List<ProtoSeismicPhase> protoTree = walker.nextLegs(tMod, p, SlownessModel.PWAVE);
            for (ProtoSeismicPhase pp : protoTree) {
                assertNotEquals(sourceDepth, pp.endSegment().getEndDepth(), pp.getPuristName()+" "+pp.branchNumSeqStrWithSegBreaks());
            }
            nextTree.addAll(protoTree);
        }
        outTree = nextTree;
        nextTree = new ArrayList<>();
        for (ProtoSeismicPhase p : outTree) {
            List<ProtoSeismicPhase> protoTree = walker.nextLegs(tMod, p, SlownessModel.PWAVE);
            for (ProtoSeismicPhase pp : protoTree) {
                assertNotEquals(sourceDepth, pp.endSegment().getEndDepth(), pp.getPuristName()+" "+pp.branchNumSeqStrWithSegBreaks());
            }
            nextTree.addAll(protoTree);
        }
    }

    @Test
    public void transAcrossExcludedBoundariesTest() throws TauModelException {
        double sourceDepth = 100.0;
        double receiverDepth = 0.0;
        TauModel tMod = TauModelLoader.load("iasp91").depthCorrect(sourceDepth);
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        walker.excludeBoundaries(new double[] {20.0, 35.0, 210, 410, 660});
        int startBranch = tMod.getSourceBranch();
        assertEquals(3, startBranch);
        TauBranch sourceBranchP = tMod.getTauBranch(tMod.getSourceBranch(), PWAVE);
        ProtoSeismicPhase transDProto = ProtoSeismicPhase.start( tMod,
                3, 3,
                PWAVE, TRANSDOWN, LayerPropogationType.DOWN,
                tMod.getNamingLayers().legNameForTauBranch(tMod.getSourceBranch(), PWAVE, LayerPropogationType.DOWN, TRANSDOWN),
                0, sourceBranchP.getMaxRayParam(), receiverDepth);
        assertTrue(transDProto.isSuccessful());
        List<ProtoSeismicPhase> protoList = walker.transAcrossExcludedBoundaries(tMod, transDProto, PWAVE, TRANSDOWN);
        assertEquals(3, protoList.size());
        ProtoSeismicPhase p = null;
        for (ProtoSeismicPhase psp : protoList) {
            if (psp.getEndAction()==TRANSDOWN) {
                p = psp;
            }
        }
        assertNotNull(p);
        assertEquals(tMod.getCmbBranch()-1, p.endSegment().endBranch);
        assertEquals("Ped", p.endSegment().legName);
        transDProto = p;

        // K leg
        p = walker.nextLegWithAction(tMod, transDProto, PWAVE, TURN);
        assertTrue(p.isSuccessful());
        assertEquals(tMod.getCmbBranch(), p.endSegment().startBranch);
        assertEquals(tMod.getCmbBranch(), p.endSegment().endBranch);
        assertEquals("K", p.endSegment().legName); // not Ked as TURN
        transDProto = p;

        // other half of K leg, up
        protoList = walker.transAcrossExcludedBoundaries(tMod, transDProto, PWAVE, TRANSUP);

        assertEquals(2, protoList.size());
        for (ProtoSeismicPhase psp : protoList) {
            if (psp.getEndAction()==TRANSUP) {
                p = psp;
            }
        }
        assertNotNull(p);
        assertTrue(p.isSuccessful(), p.branchNumSeqStrWithSegBreaks());
        assertEquals(tMod.getCmbBranch(), p.endSegment().endBranch);
        assertEquals("k", p.endSegment().legName);
        transDProto = p;

        // mantle/crust p leg
        protoList = walker.transAcrossExcludedBoundaries(tMod, transDProto, PWAVE, TRANSUP);
        assertEquals(3, protoList.size());
        for (ProtoSeismicPhase psp : protoList) {
            if (psp.getEndAction()==END) {
                p = psp;
            }
        }
        assertEquals(0, p.endSegment().endBranch);
        assertEquals("p", p.endSegment().legName);
        assertEquals("PKp", walker.consolidateTrans(p).getPuristName());
        assertEquals("PKp", walker.consolidateTrans(p).asSeismicPhase().getPuristName());

    }

    @Test
    public void phasePturn() throws TauModelException {
        double sourceDepth = 5.0;
        double receiverDepth = 0.0;
        TauModel tMod = TauModelLoader.load("iasp91").depthCorrect(sourceDepth);
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<ProtoSeismicPhase> outTree = new ArrayList<>();
        boolean isPWave = true;
        int startBranch = tMod.getSourceBranch();
        assertEquals(1, startBranch);
        TauBranch sourceBranchP = tMod.getTauBranch(tMod.getSourceBranch(), isPWave);
        ProtoSeismicPhase transDProto = ProtoSeismicPhase.start( tMod,
                startBranch, startBranch,
                isPWave, TURN, LayerPropogationType.DOWN,
                tMod.getNamingLayers().legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN, TURN),
                0, sourceBranchP.getMaxRayParam(), receiverDepth);

        assertEquals(1, transDProto.endSegment().endBranch);
        assertEquals(TURN, transDProto.endSegment().endAction);
        outTree = walker.nextLegs(tMod, transDProto, true);
        assertEquals(2, outTree.size());
        ProtoSeismicPhase Pedp = null;
        for (ProtoSeismicPhase p : outTree) {
            // only keep transup after turn
            if (p.endSegment().endAction == TRANSUP) {
                Pedp = p;
            }
        }
        assertNull(Pedp); // cannot transup at surface
    }
    @Test
    public void phasePdiff() throws TauModelException {
        double sourceDepth = 5.0;
        double receiverDepth = 0.0;
        TauModel tMod = TauModelLoader.load("iasp91").depthCorrect(sourceDepth);
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        walker.excludeBoundaries(List.of(20.0, 35.0, 410.0, 660.0));
        List<ProtoSeismicPhase> outTree = new ArrayList<>();
        boolean isPWave = true;
        int startBranch = tMod.getSourceBranch();
        assertEquals(1, startBranch);
        TauBranch sourceBranchP = tMod.getTauBranch(tMod.getSourceBranch(), isPWave);
        ProtoSeismicPhase transDProto = ProtoSeismicPhase.start( tMod,
                startBranch, tMod.getCmbBranch()-1,
                isPWave, DIFFRACT, LayerPropogationType.DOWN,
                tMod.getNamingLayers().legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN, DIFFRACT),
                0, sourceBranchP.getMaxRayParam(), receiverDepth);

        assertEquals(tMod.getCmbBranch()-1, transDProto.endSegment().endBranch);
        assertEquals(DIFFRACT, transDProto.endSegment().endAction);
        outTree = walker.nextLegs(tMod, transDProto, true);
        ProtoSeismicPhase Pdiff = null;
        assertEquals(2, outTree.size());
        Pdiff = null;
        for (ProtoSeismicPhase p : outTree) {
            // only keep diffractturn after diff
            if (p.endSegment().endAction == DIFFRACTTURN) {
                Pdiff = p;
            }
        }
        assertNotNull(Pdiff);
        outTree = walker.nextLegs(tMod, Pdiff, true);
        assertNotEquals(0, outTree.size(), "No next legs found after "+Pdiff.getPuristName()+" "+Pdiff.branchNumSeqStrWithSegBreaks());
        ProtoSeismicPhase nextProto = outTree.get(0);
        assertTrue(nextProto.isSuccessful(), "fail phase after "+nextProto.getPuristName()+" "+nextProto.branchNumSeqStrWithSegBreaks()+" reason: "+nextProto.failReason);
        while(nextProto.endSegment().endAction != END) {
            outTree = walker.nextLegs(tMod, outTree.get(0), true);
            assertNotEquals(0, outTree.size(), "No next legs found after "+Pdiff.getPuristName()+" "+Pdiff.branchNumSeqStrWithSegBreaks());
            nextProto = outTree.get(0);
            assertTrue(nextProto.isSuccessful(), "fail phase after "+nextProto.getPuristName()+" "+nextProto.branchNumSeqStrWithSegBreaks()+" reason: "+nextProto.failReason);
        }
        outTree = walker.onlySuccessfulEndingPhases(outTree);

        for (ProtoSeismicPhase p : outTree) {

            p = walker.consolidateSegment(p);
        }
        assertEquals(1, outTree.size());
        assertEquals("Pdiff", outTree.get(0).getPuristName());
    }


    @Test
    public void phaseP35diff() throws TauModelException {
        double sourceDepth = 0.0;
        double receiverDepth = 0.0;
        TauModel tMod = TauModelLoader.load("iasp91").depthCorrect(sourceDepth);
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        walker.excludeBoundaries(Arrays.asList(20.0, 210.0, 410.0, 660.0, 2889.0, 5153.9)); // moho 35, iocb 5153.9
        assertEquals(2, tMod.getMohoBranch());
        List<ProtoSeismicPhase> outTree = new ArrayList<>();
        boolean isPWave = true;
        int startBranch = tMod.getSourceBranch();
        assertEquals(0, startBranch);
        TauBranch sourceBranchP = tMod.getTauBranch(tMod.getSourceBranch(), isPWave);
        ProtoSeismicPhase proto = ProtoSeismicPhase.start(tMod,
                startBranch, startBranch,
                isPWave, TRANSDOWN, LayerPropogationType.DOWN,
                tMod.getNamingLayers().legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN, TRANSDOWN),
                0, sourceBranchP.getTopRayParam(), receiverDepth);
        assertEquals(20.0, proto.endSegment().getEndDepth());
        ProtoSeismicPhase P35diff = proto;

        outTree = walker.nextLegs(tMod, P35diff, isPWave);
        P35diff = null;
        for (ProtoSeismicPhase p : outTree) {
            // only keep diff
            if (p.endSegment().endAction == DIFFRACT) {
                P35diff = p;
            }
        }
        assertNotNull(P35diff, "no DIFFRACT phase after "+proto.branchNumSeqStrWithSegBreaks());
        outTree = walker.nextLegs(tMod, P35diff, isPWave);
        String legs = "";
        for (ProtoSeismicPhase p : outTree) {
            legs+= "\n"+p.branchNumSeqStrWithSegBreaks();
        }
        assertEquals(2, outTree.size(), legs);
        for (ProtoSeismicPhase p : outTree) {
            // only keep diff
            if (p.endSegment().endAction == DIFFRACTTURN) {
                P35diff = p;
            }
        }
        assertEquals(DIFFRACTTURN, P35diff.getEndAction());
        outTree = walker.nextLegs(tMod, P35diff, isPWave);
        P35diff = null;
        for (ProtoSeismicPhase p : outTree) {
            // only keep transup
            if (p.endSegment().endAction == TRANSUP) {
                P35diff = p;
            }
        }
        assertNotNull(P35diff);
        outTree = walker.nextLegs(tMod, P35diff, isPWave);

        P35diff = null;
        for (ProtoSeismicPhase p : outTree) {
            // only keep end
            if (p.endSegment().endAction == END) {
                P35diff = p;
            }
        }
        assertNotNull(P35diff);
    }

    @Test
    public void phasePkpPKp() throws TauModelException {
        double sourceDepth = 5.0;
        double receiverDepth = 0.0;
        TauModel tMod = TauModelLoader.load("iasp91").depthCorrect(sourceDepth);
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        walker.excludeBoundaries(List.of(20.0, 35.0, 410.0, 660.0));
        walker.setAllowPWave(true);
        walker.setAllowSWave(false);
        List<ProtoSeismicPhase> outTree = new ArrayList<>();
        boolean isPWave = true;
        int startBranch = tMod.getSourceBranch();
        assertEquals(1, startBranch);
        TauBranch sourceBranchP = tMod.getTauBranch(tMod.getSourceBranch(), isPWave);
        ProtoSeismicPhase manualWalk = ProtoSeismicPhase.start(tMod,
                startBranch, tMod.getCmbBranch()-1,
                isPWave, TRANSDOWN, LayerPropogationType.DOWN,
                tMod.getNamingLayers().legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN, TRANSDOWN),
                0, sourceBranchP.getMaxRayParam(), receiverDepth);

        ProtoSeismicPhase autoWalk = manualWalk;
        assertEquals(tMod.getCmbBranch()-1, manualWalk.endSegment().endBranch);
        assertEquals(TRANSDOWN, manualWalk.endSegment().endAction);
        manualWalk = walker.nextLegWithAction(tMod, manualWalk, true, TURN);
        assertNotNull(manualWalk);
        assertTrue(manualWalk.isSuccessful());
        assertEquals(tMod.getCmbBranch(), manualWalk.endSegment().endBranch);
        manualWalk = walker.nextLegWithAction(tMod, manualWalk, true, TRANSUP);
        assertNotNull(manualWalk);
        assertTrue(manualWalk.isSuccessful());
        //"First walk to surface after K turn");
        manualWalk = walker.walkToSurface(tMod, manualWalk, true, REFLECT_UNDERSIDE);
        assertNotNull(manualWalk);
        //"Second walk to surface after PKpP turn");
        manualWalk = walker.walkToSurface(tMod, manualWalk, true, END);
        assertNotNull(manualWalk);


        ProtoSeismicPhase PKpPKp = autoWalk;
        outTree = walker.walkPhases(tMod, List.of(PKpPKp), 1);
        outTree = walker.onlySuccessfulEndingPhases(outTree);
        assertFalse(outTree.isEmpty());
        ProtoSeismicPhase searchPKpPKp = null;
        for (ProtoSeismicPhase p : outTree) {
            assertEquals(END, p.getEndAction());
            if (p.getPuristName().equals("PKpPKp")) {searchPKpPKp=p;}
        }
        assertNotNull(searchPKpPKp);
        assertEquals(END, searchPKpPKp.getEndAction());
        searchPKpPKp = walker.consolidateTrans(searchPKpPKp);
        assertEquals(END, searchPKpPKp.getEndAction());
    }

    @Test
    public void phasePKIkp() throws TauModelException {
        double sourceDepth = 5.0;
        double receiverDepth = 0.0;
        TauModel tMod = TauModelLoader.load("iasp91").depthCorrect(sourceDepth);
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        walker.excludeBoundaries(List.of(20.0, 35.0, 410.0, 660.0));
        walker.setAllowPWave(true);
        walker.setAllowSWave(false);
        List<ProtoSeismicPhase> outTree = new ArrayList<>();
        boolean isPWave = true;
        int startBranch = tMod.getSourceBranch();
        assertEquals(1, startBranch);
        TauBranch sourceBranchP = tMod.getTauBranch(tMod.getSourceBranch(), isPWave);
        ProtoSeismicPhase manualWalk = ProtoSeismicPhase.start( tMod,
                startBranch, tMod.getCmbBranch()-1,
                isPWave, TRANSDOWN, LayerPropogationType.DOWN,
                tMod.getNamingLayers().legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN, TRANSDOWN),
                0, sourceBranchP.getMaxRayParam(), receiverDepth);

        ProtoSeismicPhase autoWalk = manualWalk;
        assertEquals(tMod.getCmbBranch()-1, manualWalk.endSegment().endBranch);
        assertEquals(TRANSDOWN, manualWalk.endSegment().endAction);
        manualWalk = walker.nextLegWithAction(tMod, manualWalk, true, TRANSDOWN);
        assertEquals(TRANSDOWN, manualWalk.endSegment().endAction);
        assertEquals(tMod.getIocbBranch()-1, manualWalk.endSegment().endBranch);
        manualWalk = walker.nextLegWithAction(tMod, manualWalk, true, TURN);
        assertNotNull(manualWalk);
        //("First walk to surface after K turn");
        manualWalk = walker.walkToSurface(tMod, manualWalk, true, END);
        assertNotNull(manualWalk);
        assertEquals(END, manualWalk.getEndAction());


        ProtoSeismicPhase PKIkp = autoWalk;
        assertTrue(autoWalk.isSuccessful());
        outTree = walker.walkPhases(tMod, List.of(PKIkp), 2);
        assertFalse(outTree.isEmpty());

        outTree = walker.onlySuccessfulEndingPhases(outTree);
        assertFalse(outTree.isEmpty());
        PKIkp = null;
        ProtoSeismicPhase byBranchSeqProto = null;
        for (ProtoSeismicPhase p : outTree) {
            if (p.getEndAction()==END && p.getPuristName().equals("PKIkp")) {PKIkp=p;}
            if (p.getEndAction()==END && p.branchNumSeqStr().equals("1 2 3 4 5 6 7 8 8 7 6 5 4 3 2 1 0")) {
                byBranchSeqProto = p;
            }
        }
        assertNotNull(PKIkp);
        assertEquals(byBranchSeqProto, PKIkp);
        PKIkp = walker.consolidateTrans(PKIkp);

        assertEquals(END, PKIkp.getEndAction());
    }

    @Test
    public void phasePedvmp() throws TauModelException {
        double sourceDepth = 0.0;
        double receiverDepth = 0.0;
        TauModel tMod = TauModelLoader.load("iasp91").depthCorrect(sourceDepth);
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<ProtoSeismicPhase> outTree = new ArrayList<>();
        int startBranch = tMod.getSourceBranch();
        assertEquals(0, startBranch);
        boolean isPWave = true;
        TauBranch sourceBranchP = tMod.getTauBranch(tMod.getSourceBranch(), isPWave);
        ProtoSeismicPhase transDProto = ProtoSeismicPhase.start(tMod,
                startBranch, startBranch,
                isPWave, TRANSDOWN, LayerPropogationType.DOWN,
                tMod.getNamingLayers().legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN, TRANSDOWN),
                0, sourceBranchP.getMinRayParam(), receiverDepth);
        outTree = walker.nextLegs(tMod, transDProto, true);
        ProtoSeismicPhase Pedvmp = null;
        for (ProtoSeismicPhase p : outTree) {
            // only keep refl topside at moho
            if (p.endSegment().endAction == REFLECT_TOPSIDE) {
                Pedvmp = p;
            }
        }
        assertNotNull(Pedvmp);
        outTree = walker.nextLegs(tMod, Pedvmp, true);
        Pedvmp = null;
        for (ProtoSeismicPhase p : outTree) {
            // only keep transup at 20
            if (p.endSegment().endAction == TRANSUP) {
                Pedvmp = p;
            }
        }
        assertNotNull(Pedvmp);

        outTree = walker.nextLegs(tMod, Pedvmp, true);
        Pedvmp = null;
        for (ProtoSeismicPhase p : outTree) {
            // only keep END at surface
            if (p.endSegment().endAction == END) {
                Pedvmp = p;
            }
        }
        assertNotNull(Pedvmp);
    }


    @Test
    public void phaseS20Pcrust() throws TauModelException {
        double sourceDepth = 0.0;
        double receiverDepth = 0.0;
        TauModel tMod = TauModelLoader.load("iasp91").depthCorrect(sourceDepth);
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<ProtoSeismicPhase> outTree = new ArrayList<>();
        int startBranch = 0;
        boolean isPWave = true;
        TauBranch sourceBranchP = tMod.getTauBranch(tMod.getSourceBranch(), SWAVE);
        ProtoSeismicPhase transDProto = null;
        outTree = walker.createSourceSegments(tMod, SWAVE, receiverDepth);
        for (ProtoSeismicPhase p : outTree) {
            if (p.getEndAction()==TRANSDOWN && p.endSegment().endBranch==0) {
                transDProto = p;
            }
        }
        assertNotNull(transDProto);
        assertEquals("Sed", transDProto.get(0).getLegName());

        outTree = walker.nextLegs(tMod, transDProto, PWAVE);
        assertNotEquals(0, outTree.size());
        ProtoSeismicPhase phaseS20P = null;
        for (ProtoSeismicPhase p : outTree) {
            // only keep turn
            if (p.endSegment().endAction == TURN) {
                phaseS20P = p;
                break;
            }
        }
        assertNotNull(phaseS20P);
        phaseS20P = walker.walkToSurface(tMod, phaseS20P, PWAVE, END);
        assertNotNull(phaseS20P);
        assertTrue(phaseS20P.isSuccessful());
    }

    @Test
    public void mergeTest() throws TauModelException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<SeismicPhaseSegment> upper = new ArrayList<>();
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TURN, LayerPropogationType.DOWN, "P", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, END, LayerPropogationType.UP, "p", 0, 100));
        ProtoSeismicPhase upperProto = new ProtoSeismicPhase(upper, 0);
        List<SeismicPhaseSegment> lower = new ArrayList<>();
        lower.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, LayerPropogationType.DOWN, "P", 0, 10));
        lower.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, END, LayerPropogationType.UP, "p", 0, 10));
        ProtoSeismicPhase lowerProto = new ProtoSeismicPhase(lower, 0);

        assertTrue(walker.canMergePhases(upperProto, lowerProto));
        ProtoSeismicPhase merged = walker.mergePhases(upperProto, lowerProto);
        assertEquals(merged.get(merged.size()-1).maxRayParam, 100);
        assertEquals(merged.get(merged.size()-1).minRayParam, 0);

    }

    @Test
    public void merge210Test() throws TauModelException {
        TauModel tMod = TauModelLoader.load("ak135").depthCorrect(100);
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        boolean isPWave = true;
        walker.allowSWave = false;


        List<SeismicPhaseSegment> upper = new ArrayList<>();
        upper.add(new SeismicPhaseSegment(tMod, 1, 1, isPWave, TURN, LayerPropogationType.DOWN, "P", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, END, LayerPropogationType.UP, "p", 0, 100));
        ProtoSeismicPhase upperProto = new ProtoSeismicPhase(upper, 0);
        List<SeismicPhaseSegment> lower = new ArrayList<>();
        lower.add(new SeismicPhaseSegment(tMod, 1, 2, isPWave, TURN, LayerPropogationType.DOWN, "P", 0, 10));
        lower.add(new SeismicPhaseSegment(tMod, 2, 0, isPWave, END, LayerPropogationType.UP, "p", 0, 10));
        ProtoSeismicPhase lowerProto = new ProtoSeismicPhase(lower, 0);

        assertTrue(walker.canMergePhases(upperProto, lowerProto));

    }

    @Test
    public void mergeUndersideTest() throws TauModelException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<SeismicPhaseSegment> upper = new ArrayList<>();
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TURN, LayerPropogationType.DOWN, "P", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, REFLECT_UNDERSIDE, LayerPropogationType.UP, "p", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TURN, LayerPropogationType.DOWN, "P", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, END, LayerPropogationType.UP, "p", 0, 100));
        ProtoSeismicPhase upperProto = new ProtoSeismicPhase(upper, 0);
        List<SeismicPhaseSegment> lower = new ArrayList<>();
        lower.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, LayerPropogationType.DOWN, "P", 0, 10));
        lower.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, REFLECT_UNDERSIDE, LayerPropogationType.UP, "p", 0, 10));
        lower.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, LayerPropogationType.DOWN, "P", 0, 10));
        lower.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, END, LayerPropogationType.UP, "p", 0, 10));
        ProtoSeismicPhase lowerProto = new ProtoSeismicPhase(lower, 0);

        assertTrue(walker.canMergePhases(upperProto, lowerProto));
        ProtoSeismicPhase merged = walker.mergePhases(upperProto, lowerProto);
        assertEquals(merged.get(merged.size()-1).maxRayParam, 100);
        assertEquals(merged.get(merged.size()-1).minRayParam, 0);

    }
    @Test
    public void mergeUnderside_pP() throws TauModelException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<SeismicPhaseSegment> upper = new ArrayList<>();
        upper.add( SeismicPhaseSegment.startingSegment(tMod, 0, 0, isPWave, REFLECT_UNDERSIDE, LayerPropogationType.UP, "p", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TURN, LayerPropogationType.DOWN, "P", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, END, LayerPropogationType.UP, "p", 0, 100));
        ProtoSeismicPhase upperProto = new ProtoSeismicPhase(upper, 0);
        List<SeismicPhaseSegment> lower = new ArrayList<>();
        lower.add( SeismicPhaseSegment.startingSegment(tMod, 0, 0, isPWave, REFLECT_UNDERSIDE, LayerPropogationType.UP, "p", 0, 100));
        lower.add(new SeismicPhaseSegment(tMod, 0, 2, isPWave, TURN, LayerPropogationType.DOWN, "P", 0, 100));
        lower.add(new SeismicPhaseSegment(tMod, 2, 0, isPWave, END, LayerPropogationType.UP, "p", 0, 100));
        ProtoSeismicPhase lowerProto = new ProtoSeismicPhase(lower, 0);

        assertTrue(walker.canMergePhases(upperProto, lowerProto));
        ProtoSeismicPhase merged = walker.mergePhases(upperProto, lowerProto);
        assertEquals(merged.get(merged.size()-1).maxRayParam, 100);
        assertEquals(merged.get(merged.size()-1).minRayParam, 0);

    }

    @Test
    public void interactNumTest() throws TauModelException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<SeismicPhaseSegment> turnOnly = new ArrayList<>();
        turnOnly.add( SeismicPhaseSegment.startingSegment(tMod, 0, 1, isPWave, TURN, LayerPropogationType.DOWN, "P", 0, 10));
        turnOnly.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, END, LayerPropogationType.UP, "p", 0, 10));
        ProtoSeismicPhase proto = new ProtoSeismicPhase(turnOnly, 0);
        int num = proto.calcInteractionNumber();
        assertEquals(0, num);

        List<SeismicPhaseSegment> reflUnder = new ArrayList<>();
        reflUnder.add( SeismicPhaseSegment.startingSegment(tMod, 0, 1, isPWave, TURN, LayerPropogationType.DOWN, "P", 0, 10));
        reflUnder.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, REFLECT_UNDERSIDE, LayerPropogationType.UP, "p", 0, 10));
        reflUnder.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, LayerPropogationType.DOWN, "P", 0, 10));
        reflUnder.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, END, LayerPropogationType.UP, "p", 0, 10));
        ProtoSeismicPhase reflProto = new ProtoSeismicPhase(reflUnder, 0);
        assertEquals(1, reflProto.calcInteractionNumber());
    }
    @Test
    public void interactConvTest() throws TauModelException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<SeismicPhaseSegment> convTransDown = new ArrayList<>();
        convTransDown.add( SeismicPhaseSegment.startingSegment(tMod, 0, 1, isPWave, TRANSDOWN, LayerPropogationType.DOWN, "P", 0, 10));
        convTransDown.add(new SeismicPhaseSegment(tMod, 2, 2, !isPWave, TURN, LayerPropogationType.DOWN, "s", 0, 10));
        convTransDown.add(new SeismicPhaseSegment(tMod, 2, 0, !isPWave, END, LayerPropogationType.UP, "s", 0, 10));
        ProtoSeismicPhase transProto = new ProtoSeismicPhase(convTransDown, 0);
        assertEquals(1,  transProto.calcInteractionNumber());


    }

    @Test
    public void excludeDiscon() throws TauPException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("ak135");
        double receiverDepth = 0;
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        Double d = 20.0;
        Double d210 = 210.0;
        walker.excludeBoundaries(List.of(d, d210));

        ProtoSeismicPhase proto = ProtoSeismicPhase.start(tMod, 0, 0, isPWave, TRANSDOWN, LayerPropogationType.DOWN, "P", 0, 10, receiverDepth);
        List<ProtoSeismicPhase> next = walker.nextLegs(tMod, proto, !isPWave);
        assertEquals(0, next.size(), "no P20S");

        ProtoSeismicPhase proto_pPv20 = ProtoSeismicPhase.start(tMod, 0, 0, isPWave, REFLECT_UNDERSIDE, LayerPropogationType.UP, "P", 0, 10, receiverDepth);
        List<ProtoSeismicPhase> next_Pv20 = walker.nextLegs(tMod, proto_pPv20, isPWave);
        assertEquals(1, next_Pv20.size(), "no pPv20p, only trans, " + next_Pv20.get(0).phaseName );

        ProtoSeismicPhase s20p = ProtoSeismicPhase.start(tMod, 1, 1, false, TRANSUP, LayerPropogationType.UP, "s", 0, 1000, receiverDepth);
        List<ProtoSeismicPhase> next_s20p = walker.nextLegs(tMod, s20p, true);
        assertEquals(0, next_s20p.size(), "no s20p, s^20");

        List<SeismicPhaseSegment> legs = new ArrayList<>();
        legs.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TRANSDOWN, LayerPropogationType.DOWN, "P", 0, 10));
        legs.add(new SeismicPhaseSegment(tMod, 1, 1, isPWave, TURN, LayerPropogationType.DOWN, "P", 0, 10));
        legs.get(0).prevEndAction = START_DOWN;
        legs.get(1).prevEndAction = legs.get(0).endAction;
        ProtoSeismicPhase proto_Punder20P = new ProtoSeismicPhase(legs, 0);
        assertEquals(1, proto_Punder20P.endSegment().endBranch);
        assertEquals(TURN, proto_Punder20P.endSegment().endAction);
        List<ProtoSeismicPhase> next_Punder20P = walker.nextLegs(tMod, proto_Punder20P, isPWave);

        assertEquals(2, next_Punder20P.size(), "no P^20p, only surface reflect and end");
        assertTrue(next_Punder20P.get(0).isSuccessful());
        assertTrue(next_Punder20P.get(1).isSuccessful());
        ProtoSeismicPhase ending = next_Punder20P.get(0).endSegment().endAction==END?next_Punder20P.get(0):next_Punder20P.get(1);
        ProtoSeismicPhase reflecting = next_Punder20P.get(0).endSegment().endAction==REFLECT_UNDERSIDE?next_Punder20P.get(0):next_Punder20P.get(1);
        assertEquals(END, ending.getEndAction());
        assertEquals(REFLECT_UNDERSIDE, reflecting.getEndAction());
    }

    @Test
    public void exclude210_pPedv210p() throws TauPException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("ak135");
        TauModel tModDepth = tMod.depthCorrect(100);
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tModDepth);
        Double d = 20.0;
        Double d210 = 210.0;
        walker.excludeBoundaries(List.of(d, d210));

        int branchNum210 = tModDepth.findBranch(210);
        ProtoSeismicPhase pPedv210 = ProtoSeismicPhase.startNewPhase(tModDepth, true, TRANSUP, LayerPropogationType.UP, 0);
        while(pPedv210.endSegment().endBranch > 1) {
            pPedv210 = pPedv210.nextSegment(true, TRANSUP);
        }
        pPedv210 = pPedv210.nextSegment(true, REFLECT_UNDERSIDE);
        while(pPedv210.endSegment().endBranch < branchNum210-3) {
            assertNotEquals(branchNum210, pPedv210.endSegment().endBranch);
            pPedv210 = pPedv210.nextSegment(true, TRANSDOWN);
        }
        List<ProtoSeismicPhase> nextLegs = walker.nextLegs(tModDepth, pPedv210, isPWave);
        for (ProtoSeismicPhase p : nextLegs) {
            assertNotEquals(branchNum210, pPedv210.endSegment().endBranch);
            assertNotEquals(REFLECT_TOPSIDE, p.endSegment().endAction, p.getPuristName()+" "+p.branchNumSeqStr()+" "+ Arrays.toString(p.endSegment().getDepthRange()));
        }
    }


    @Test
    public void excludeDisconWalk() throws TauPException {
        TauModel tMod = TauModelLoader.load("ak135");
        int maxLegs = 4;
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        Double d = 20.0;
        Double d35 = 35.0;
        Double d210 = 210.0;
        Double d410 = 410.0;
        double d660 = 660.0;
        walker.excludeBoundaries(List.of(d, d35, d210, d410, d660));
        List<ProtoSeismicPhase> walk = walker.findEndingPaths(maxLegs);
        for (ProtoSeismicPhase segList : walk) {
            assertTrue(segList.isSuccessful());
            assertNotNull(segList);
            assertNotNull(segList.getPuristName());
            String phaseName = segList.getPuristName();
            assertNotNull(segList.getName());
            assertTrue(segList.isSuccessful());
            assertFalse(phaseName.contains("20"), phaseName);
            assertFalse(phaseName.contains("35"), phaseName);
            assertFalse(phaseName.contains("210"), phaseName);
            assertFalse(phaseName.contains("410"), phaseName);
            assertFalse(phaseName.contains("660"), phaseName);
            if (segList.isSuccessful()) {
                SeismicPhase sp = SeismicPhaseFactory.createPhase(segList.getPuristName(), tMod);
                assertTrue(sp.phasesExistsInModel(), sp.getPuristName() + " " + sp.branchNumSeqStrWithSegBreaks()+" proto: "+segList.branchNumSeqStrWithSegBreaks());
            }
        }
    }

    @Test
    public void PKpTest() throws TauModelException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("ak135");
        double receiverDepth = 0;
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        Double d = 20.0;
        Double d35 = 35.0;
        Double d210 = 210.0;
        Double d410 = 410.0;
        double d660 = 660.0;
        walker.excludeBoundaries(List.of(d, d35, d210, d410, d660));
        walker.setAllowPWave(true);
        walker.setAllowSWave(false);
        List<ProtoSeismicPhase> segmentTree = new ArrayList<>();
        segmentTree.addAll( walker.createSourceSegments(tMod, PWAVE, receiverDepth));
        ProtoSeismicPhase PKp = null;
        for (ProtoSeismicPhase psp : segmentTree) {
            SeismicPhaseSegment seg = psp.getSegmentList().get(0);
            if (seg.layerPropogationType==LayerPropogationType.DOWN && seg.endAction == TRANSDOWN ) {
                PKp = psp;
                break;
            }
        }
        assertEquals(0, PKp.endSegment().endBranch);
        segmentTree = walker.transAcrossExcludedBoundaries(tMod, PKp, PWAVE, TRANSDOWN);

        for (ProtoSeismicPhase psp : segmentTree) {
            if (psp.getEndAction()==TRANSDOWN) { PKp = psp;}
        }
        assertNotNull(PKp);
        assertTrue(PKp.isSuccessful());
        assertEquals(tMod.getCmbBranch()-1, PKp.endSegment().endBranch);
        PKp = PKp.nextSegment(PWAVE, TURN);
        assertNotNull(PKp);
        assertTrue(PKp.isSuccessful());
        assertEquals(tMod.getIocbBranch()-1, PKp.endSegment().endBranch);
        PKp = PKp.nextSegment(PWAVE, TRANSUP);
        List<ProtoSeismicPhase> toSurface = walker.transAcrossExcludedBoundaries(tMod, PKp, PWAVE, TRANSUP);
        assertEquals(3, toSurface.size(), toSurface.get(0).branchNumSeqStrWithSegBreaks());
        PKp = toSurface.get(0).getEndAction()==END?toSurface.get(0):toSurface.get(1);
        assertEquals(END, PKp.getEndAction(), PKp.getPuristName()+" "+PKp.branchNumSeqStrWithSegBreaks());
        assertTrue(PKp.isSuccessful());
        SeismicPhase sp = walker.consolidateSegment(PKp).asSeismicPhase();
        assertEquals("PKp", sp.getPuristName());
    }

    @Test
    public void phaseActionDowngoing() {
        assertFalse(PhaseInteraction.isDowngoingActionBefore(END));
        assertFalse(PhaseInteraction.isDowngoingActionBefore(REFLECT_UNDERSIDE));
        assertTrue(PhaseInteraction.isDowngoingActionBefore(REFLECT_TOPSIDE));

    }


    @Test
    public void phaseChangeRayParam() throws TauModelException {
        TauModel tMod = TauModelLoader.load("iasp91");
        double receiverDepth = 0.0;
        double sourceDepth = 579.0;
        tMod = tMod.depthCorrect(sourceDepth);
        int maxLegs = 4;
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        Double d = 20.0;
        Double d35 = 35.0;
        Double d210 = 210.0;
        Double d410 = 410.0;
        double d660 = 660.0;
        walker.excludeBoundaries(List.of(d, d35, d210, d410, d660));
        List<ProtoSeismicPhase> segmentTree = new ArrayList<>();
        segmentTree.addAll( walker.createSourceSegments(tMod, SeismicPhase.SWAVE, receiverDepth));
        ProtoSeismicPhase SKp = null;
        for (ProtoSeismicPhase psp : segmentTree) {
            SeismicPhaseSegment seg = psp.getSegmentList().get(0);
            if (!seg.isPWave && seg.layerPropogationType==LayerPropogationType.DOWN && seg.endAction == TRANSDOWN) {
                SKp = psp;
            }
        }
        assertTrue(SKp.isSuccessful());
        segmentTree = walker.transAcrossExcludedBoundaries(tMod, SKp, false, TRANSDOWN);
        for (ProtoSeismicPhase psp : segmentTree) {
            if (psp.getEndAction() == TRANSDOWN && psp.endSegment().endBranch == tMod.getCmbBranch()-1) {
                SKp = psp;
            }
        }
        assertNotNull(SKp);
        assertTrue(SKp.isSuccessful());
        assertEquals(2889.0, SKp.endSegment().getBotDepth());
        assertEquals(tMod.getCmbBranch()-1, SKp.endSegment().endBranch);
        assertEquals(tMod.getAboveCmbTauBranch(SWAVE).getBotRayParam(), SKp.endSegment().maxRayParam);
        assertEquals(0.0, SKp.endSegment().minRayParam);
        // into outer core
        SKp = SKp.nextSegment(true, TURN);
        assertTrue(SKp.isSuccessful());
        assertEquals(tMod.getCmbBranch(), SKp.endSegment().endBranch);
        assertEquals(tMod.getAboveIocbTauBranch(true).getBotRayParam(), SKp.endSegment().minRayParam);
        assertEquals(tMod.getBelowCmbTauBranch(true).getTopRayParam(), SKp.endSegment().maxRayParam);
        // back into mantle
        SKp = SKp.nextSegment(true, TRANSUP);
        assertTrue(SKp.isSuccessful());
        assertEquals(2889.0, SKp.endSegment().getTopDepth());
        assertEquals(tMod.getAboveIocbTauBranch(true).getBotRayParam(), SKp.endSegment().minRayParam);
        assertEquals(tMod.getBelowCmbTauBranch(true).getTopRayParam(), SKp.endSegment().maxRayParam);
        while (SKp.endSegment().endBranch > 1) {
            SKp = SKp.nextSegment(true, TRANSUP);
        }
        assertEquals(tMod.getAboveCmbTauBranch(true).getBotRayParam(), SKp.endSegment().maxRayParam);
        SKp = SKp.nextSegment(true, END);
        assertEquals(tMod.getAboveIocbTauBranch(true).getBotRayParam(), SKp.endSegment().minRayParam);
        assertEquals(tMod.getAboveCmbTauBranch(true).getBotRayParam(), SKp.endSegment().maxRayParam);
    }

}
