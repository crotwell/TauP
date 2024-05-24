package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.PhaseInteraction.*;
import static org.junit.jupiter.api.Assertions.*;

public class WalkPhaseTest {

    @Test
    public void phasePedvmp() throws TauModelException {
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<ProtoSeismicPhase> outTree = new ArrayList<>();
        int startBranch = 0;
        double receiverDepth = 0.0;
        boolean isPWave = true;
        TauBranch sourceBranchP = tMod.getTauBranch(tMod.getSourceBranch(), isPWave);
        ProtoSeismicPhase transDProto = ProtoSeismicPhase.start( new SeismicPhaseSegment(tMod,
                startBranch, startBranch,
                isPWave, TRANSDOWN, true,
                walker.legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, true),
                0, sourceBranchP.getMinRayParam()), receiverDepth);
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
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<ProtoSeismicPhase> outTree = new ArrayList<>();
        int startBranch = 0;
        double receiverDepth = 0.0;
        boolean isPWave = true;
        TauBranch sourceBranchP = tMod.getTauBranch(tMod.getSourceBranch(), isPWave);
        ProtoSeismicPhase transDProto = ProtoSeismicPhase.start( new SeismicPhaseSegment(tMod,
                startBranch, startBranch,
                false, TRANSDOWN, true,
                walker.legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, true),
                0, sourceBranchP.getMinRayParam()), receiverDepth);
        outTree = walker.nextLegs(tMod, transDProto, isPWave);
        ProtoSeismicPhase phaseS20P = null;
        for (ProtoSeismicPhase p : outTree) {
            // only keep refl topside at moho
            if (p.endSegment().endAction == TURN) {
                phaseS20P = p;
            }
        }
        assertNotNull(phaseS20P);
        outTree = walker.nextLegs(tMod, phaseS20P, isPWave);
        phaseS20P = null;
        for (ProtoSeismicPhase p : outTree) {
            // only keep transup at 20
            if (p.endSegment().endAction == TRANSUP) {
                phaseS20P = p;
            }
        }
        assertNotNull(phaseS20P);

        outTree = walker.nextLegs(tMod, phaseS20P, isPWave);
        phaseS20P = null;
        for (ProtoSeismicPhase p : outTree) {
            // only keep END at surface
            if (p.endSegment().endAction == END) {
                phaseS20P = p;
            }
        }
        assertNotNull(phaseS20P);
    }

    @Test
    public void namedDisconWalk() throws TauModelException {
        TauModel tMod = TauModelLoader.load("iasp91");
        int maxActions = 1;
        TauP_Find find = new TauP_Find();
        // only  moho, cmb, iocb
        find.onlyNamedDiscon = true;
        find.excludeDepth.addAll(List.of(20.0, 210.0, 410.0, 660.0));
        SeismicPhaseWalk allwalker = find.createWalker(tMod, find.excludeDepth);
        List<ProtoSeismicPhase> allwalk = allwalker.findEndingPaths(maxActions);

        List<String> zeroActionNames = List.of("P", "S", "PKp", "PKIkp");
        List<String> oneActionPBounceNames = List.of("Pvmp", "P^mP", "PP", "Pcp", "PKKp", "PKikp",
                "PKpPKp","PKp^mPKp",
                "PKIIkp", "PKIkpPKIkp", "PKIkp^mPKIkp");
        List<String> oneActionSBounceNames = List.of("Svms", "S^mS", "SS", "Scs");
        List<String> oneActionConvertNames = List.of("PedmS","Pms", "SedmP", "SedmPKp", "SedmPKIkp",
                "PKpms", "PKIkpms",
                "Smp", "SKp", "SKIkp", "PKs", "PKIks"); // PKJkp has 2
        List<String> oneAction = new ArrayList<>();
        oneAction.addAll(zeroActionNames);
        oneAction.addAll(oneActionPBounceNames);
        oneAction.addAll(oneActionSBounceNames);
        oneAction.addAll(oneActionConvertNames);

        for (String phase : oneAction) {
            boolean notFound = true;
            for (ProtoSeismicPhase proto : allwalk) {
                if (proto.getPuristName().equals(phase)) {
                    notFound = false;
                }
            }
            assertFalse(notFound, "One action does not contain phase "+phase);
        }
        for (ProtoSeismicPhase proto : allwalk) {
            boolean notFound = true;
            for (String phase : oneAction) {
                if (proto.getPuristName().equals(phase)) {
                    notFound = false;
                }
            }
            assertFalse(notFound, "List does not contain phase "+proto.getPuristName());
        }

        assertEquals(oneAction.size(), allwalk.size(), "size of one action walk");
    }

    @Test
    public void mergeTest() throws TauModelException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<SeismicPhaseSegment> upper = new ArrayList<>();
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TURN, true, "P", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, END, false, "p", 0, 100));
        ProtoSeismicPhase upperProto = new ProtoSeismicPhase(upper, 0);
        List<SeismicPhaseSegment> lower = new ArrayList<>();
        lower.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, true, "P", 0, 10));
        lower.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, END, false, "p", 0, 10));
        ProtoSeismicPhase lowerProto = new ProtoSeismicPhase(lower, 0);

        assertTrue(walker.canMergePhases(upperProto, lowerProto));
        ProtoSeismicPhase merged = walker.mergePhases(upperProto, lowerProto);
        assertEquals(merged.get(merged.size()-1).maxRayParam, 100);
        assertEquals(merged.get(merged.size()-1).minRayParam, 0);

    }
    @Test
    public void mergeUndersideTest() throws TauModelException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<SeismicPhaseSegment> upper = new ArrayList<>();
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TURN, true, "P", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, REFLECT_UNDERSIDE, false, "p", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TURN, true, "P", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, END, false, "p", 0, 100));
        ProtoSeismicPhase upperProto = new ProtoSeismicPhase(upper, 0);
        List<SeismicPhaseSegment> lower = new ArrayList<>();
        lower.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, true, "P", 0, 10));
        lower.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, REFLECT_UNDERSIDE, false, "p", 0, 10));
        lower.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, true, "P", 0, 10));
        lower.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, END, false, "p", 0, 10));
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
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, REFLECT_UNDERSIDE, false, "p", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TURN, true, "P", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, END, false, "p", 0, 100));
        ProtoSeismicPhase upperProto = new ProtoSeismicPhase(upper, 0);
        List<SeismicPhaseSegment> lower = new ArrayList<>();
        lower.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, REFLECT_UNDERSIDE, false, "p", 0, 100));
        lower.add(new SeismicPhaseSegment(tMod, 0, 2, isPWave, TURN, true, "P", 0, 100));
        lower.add(new SeismicPhaseSegment(tMod, 2, 0, isPWave, END, false, "p", 0, 100));
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
        turnOnly.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, true, "P", 0, 10));
        turnOnly.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, END, false, "p", 0, 10));
        ProtoSeismicPhase proto = new ProtoSeismicPhase(turnOnly, 0);
        int num = proto.calcInteractionNumber();
        assertEquals(0, num);

        List<SeismicPhaseSegment> reflUnder = new ArrayList<>();
        reflUnder.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, true, "P", 0, 10));
        reflUnder.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, REFLECT_UNDERSIDE, false, "p", 0, 10));
        reflUnder.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, true, "P", 0, 10));
        reflUnder.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, END, false, "p", 0, 10));
        ProtoSeismicPhase reflProto = new ProtoSeismicPhase(reflUnder, 0);
        assertEquals(1, reflProto.calcInteractionNumber());
    }
    @Test
    public void interactConvTest() throws TauModelException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<SeismicPhaseSegment> convTransDown = new ArrayList<>();
        convTransDown.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TRANSDOWN, true, "P", 0, 10));
        convTransDown.add(new SeismicPhaseSegment(tMod, 2, 2, !isPWave, TURN, true, "s", 0, 10));
        convTransDown.add(new SeismicPhaseSegment(tMod, 2, 0, !isPWave, END, false, "s", 0, 10));
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

        ProtoSeismicPhase proto = ProtoSeismicPhase.start(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TRANSDOWN, true, "P", 0, 10), receiverDepth);
        List<ProtoSeismicPhase> next = walker.nextLegs(tMod, proto, !isPWave);
        assertEquals(0, next.size(), "no P20S");

        ProtoSeismicPhase proto_pPv20 = ProtoSeismicPhase.start(new SeismicPhaseSegment(tMod, 0, 0, isPWave, REFLECT_UNDERSIDE, false, "P", 0, 10), receiverDepth);
        List<ProtoSeismicPhase> next_Pv20 = walker.nextLegs(tMod, proto_pPv20, isPWave);
        assertEquals(1, next_Pv20.size(), "no pPv20p, only trans, " + next_Pv20.get(0).phaseName );

        ProtoSeismicPhase s20p = ProtoSeismicPhase.start(new SeismicPhaseSegment(tMod, 1, 1, false, TRANSUP, false, "s", 0, 1000), receiverDepth);
        List<ProtoSeismicPhase> next_s20p = walker.nextLegs(tMod, s20p, true);
        assertEquals(0, next_s20p.size(), "no s20p, s^20");

        List<SeismicPhaseSegment> legs = new ArrayList<>();
        legs.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TRANSDOWN, true, "P", 0, 10));
        legs.add(new SeismicPhaseSegment(tMod, 1, 1, isPWave, TURN, true, "P", 0, 10));
        legs.get(0).prevEndAction = START;
        legs.get(1).prevEndAction = legs.get(0).endAction;
        ProtoSeismicPhase proto_Punder20P = new ProtoSeismicPhase(legs, 0);
        List<ProtoSeismicPhase> next_Punder20P = walker.nextLegs(tMod, proto_Punder20P, isPWave);
        assertEquals(1, next_Punder20P.size(), "no P^20p, only trans");

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
        ProtoSeismicPhase pPedv210 = ProtoSeismicPhase.startNewPhase(tModDepth, true, TRANSUP, false, 0);
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
            assertNotEquals(REFLECT_TOPSIDE, p.endSegment().endAction);
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
            assertNotNull(segList);
            assertNotNull(segList.getPuristName());
            String phaseName = segList.getPuristName();
            assertNotNull(segList.getName());
            assertFalse(phaseName.contains("20"), phaseName);
            assertFalse(phaseName.contains("35"), phaseName);
            assertFalse(phaseName.contains("210"), phaseName);
            assertFalse(phaseName.contains("410"), phaseName);
            assertFalse(phaseName.contains("660"), phaseName);
        }
    }

    @Test
    public void phaseActionDowngoing() {
        assertFalse(PhaseInteraction.isDowngoingActionBefore(END));
        assertFalse(PhaseInteraction.isDowngoingActionBefore(REFLECT_UNDERSIDE));
        assertTrue(PhaseInteraction.isDowngoingActionBefore(REFLECT_TOPSIDE));

    }
}
