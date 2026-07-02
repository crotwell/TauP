package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cmdline.TauP_Tool;
import edu.sc.seis.TauP.cmdline.ToolRun;

import java.util.*;

import static edu.sc.seis.TauP.PhaseInteraction.*;

/**
 * Walks all possible seismic phases, up to a maximum number of turns and reflections within a model.
 */
public class SeismicPhaseWalk {


    public SeismicPhaseWalk(TauModel tMod) throws TauModelException {
        this(tMod, null, null, 0);
    }

    public SeismicPhaseWalk(TauModel tMod, Double minRayParam, Double maxRayParam, double receiverDepth) throws TauModelException {
        this.tMod = tMod;
        this.minRayParam = minRayParam;
        this.maxRayParam = maxRayParam;
        this.receiverBranch = tMod.findBranch(receiverDepth);
        this.receiverDepth = receiverDepth;
    }

    public void excludeBoundaries(List<Double> layerDepths) throws TauModelException {
        double tol = 1e-6;
        for (double d: layerDepths) {
            int depthBranch = tMod.findBranch(d);
            if (Math.abs(tMod.getTauBranch(depthBranch, true).getTopDepth() - d) < tol
                    && Math.abs(tMod.getTauBranch(depthBranch, true).getTopDepth() - d) <
                    Math.abs(tMod.getTauBranch(depthBranch, true).getBotDepth() - d)
            ) {
                excludeBranch.add(depthBranch);
            } else if (Math.abs(tMod.getTauBranch(depthBranch, true).getBotDepth() - d) < tol) {
                excludeBranch.add(depthBranch+1);
            } else {
                throw new TauModelException("Unable to find discontinuity within "+tol+" km of "+d+" in "+tMod.getModelName());
            }
        }
    }


    /**
     * Temporary assume receiver is at surface.
     */
    public int receiverBranch;
    public double receiverDepth = 0.0;
    Double minRayParam;
    Double maxRayParam;
    TauModel tMod;

    List<Integer> excludeBranch = new ArrayList<>();


    public List<ProtoSeismicPhase> findEndingPaths(int maxAction) throws TauModelException {
        List<ProtoSeismicPhase> segmentTree = walkPhases(maxAction);
        return onlySuccessfulEndingPhases(segmentTree);
    }

    public List<ProtoSeismicPhase> onlySuccessfulEndingPhases(List<ProtoSeismicPhase> segmentTree) throws TauModelException {
        List<ProtoSeismicPhase> endingSegments = new ArrayList<>();
        for (ProtoSeismicPhase proto : segmentTree) {
            SeismicPhaseSegment endSeg = proto.endSegment();
            if (endSeg.endAction == END || endSeg.endAction == END_DOWN) {
                ProtoSeismicPhase cons = consolidateSegment(proto);
                cons.phaseName = cons.getPuristName();
                endingSegments.add(cons);
            }
        }
        endingSegments.sort(Comparator.naturalOrder()); // sorts alpha by proto name
        ProtoSeismicPhase prev = null;
        ArrayList<ProtoSeismicPhase> out = new ArrayList<>();
        for (ProtoSeismicPhase curr : endingSegments) {
            if (prev == null) {
                prev = curr;
            } else if (canMergePhases(prev, curr)) {
                prev = mergePhases(prev, curr);
            } else {
                out.add(prev);
                prev = curr;
            }
        }
        if (prev != null) {
            out.add(prev);
        }
        return out;
    }

    public List<ProtoSeismicPhase> walkPhases(int maxAction) throws TauModelException {

        List<ProtoSeismicPhase> segmentTree = new ArrayList<>();
        if (allowSWave) {
            segmentTree.addAll( createSourceSegments(tMod, SeismicPhase.SWAVE, receiverDepth));
        }
        if (allowPWave) {
            segmentTree.addAll( createSourceSegments(tMod, SeismicPhase.PWAVE, receiverDepth));
        }
        segmentTree = overlapsRayParam(segmentTree, minRayParam, maxRayParam);
        segmentTree = walkPhases(tMod, segmentTree, maxAction);
        return segmentTree;
    }

    public List<ProtoSeismicPhase> createSourceSegments(TauModel tMod, boolean isPWave, double receiverDepth) {
        List<ProtoSeismicPhase> segmentTree =  new ArrayList<>();
        if (tMod.getSourceBranch() > 0) {
            int aboveDisconBranch =  ProtoSeismicPhase.findEndDiscon(tMod, tMod.getSourceBranch()-1, isPWave,
                    LayerPropogationType.UP);
            int startUpBranchNum = tMod.sourceBranch-1;
            TauBranch aboveSourceBranchP = tMod.getTauBranch(startUpBranchNum, isPWave);
            if (receiverBranch <= startUpBranchNum && receiverBranch >= aboveDisconBranch) {
                // one branch away from receiver, so can just go direct and END
                ProtoSeismicPhase upProto = ProtoSeismicPhase.start(tMod,
                        startUpBranchNum, receiverBranch,
                        isPWave, END, LayerPropogationType.UP,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, LayerPropogationType.UP),
                        0, aboveSourceBranchP.getMinTurnRayParam(), receiverDepth);
                segmentTree.add(upProto);
            }
            if ( ! excludeBranch.contains(aboveDisconBranch) && tMod.isDiscontinuityBranch(aboveDisconBranch, isPWave) ) {
                ProtoSeismicPhase reflProto = ProtoSeismicPhase.start(tMod,
                        startUpBranchNum, aboveDisconBranch,
                        isPWave, REFLECT_UNDERSIDE, LayerPropogationType.UP,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, LayerPropogationType.UP),
                        0, aboveSourceBranchP.getMinTurnRayParam(), receiverDepth);
                segmentTree.add(reflProto);
            }
            if (aboveDisconBranch > 0) {
                ProtoSeismicPhase upProto = ProtoSeismicPhase.start(tMod,
                        startUpBranchNum, aboveDisconBranch,
                        isPWave, TRANSUP, LayerPropogationType.UP,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, LayerPropogationType.UP),
                        0, aboveSourceBranchP.getMinTurnRayParam(), receiverDepth);
                segmentTree.add(upProto);
                if (tMod.isDiffractionBranch(aboveDisconBranch, isPWave) && ! excludeBranch.contains(aboveDisconBranch)) {
                    ProtoSeismicPhase upDiff = ProtoSeismicPhase.start(tMod,
                            startUpBranchNum, aboveDisconBranch,
                            isPWave, TRANSUPDIFFRACT, LayerPropogationType.UP,
                            legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, LayerPropogationType.UP),
                            0, aboveSourceBranchP.getMinTurnRayParam(), receiverDepth);
                    segmentTree.add(upDiff);
                }
                if (tMod.isDiffractionBranch(startUpBranchNum+1, isPWave) && ! excludeBranch.contains(startUpBranchNum+1)) {
                    ProtoSeismicPhase diffProto = ProtoSeismicPhase.start( tMod,
                            startUpBranchNum, startUpBranchNum,
                            isPWave, DIFFRACTTURN, LayerPropogationType.DIFF,
                            legNameForTauBranch(tMod, tMod.getSourceBranch()-1, isPWave, LayerPropogationType.DOWN),
                            aboveSourceBranchP.getMinTurnRayParam(),
                            aboveSourceBranchP.getMinTurnRayParam(), receiverDepth);
                    segmentTree.add(diffProto);
                }
            }
        }

        int startBranch = tMod.getSourceBranch();

        TauBranch sourceBranchP = tMod.getTauBranch(tMod.getSourceBranch(), isPWave);
        int endDisconBranchNum = ProtoSeismicPhase.findEndDiscon(tMod, tMod.getSourceBranch(), isPWave,
                LayerPropogationType.DOWN);
        int endDownBranchNum = endDisconBranchNum-1;


        // downgoing options are END, TURN, HEAD, REFLECT_TOPSIDE or TRANSDOWN
        ProtoSeismicPhase turnProto = ProtoSeismicPhase.start(tMod,
                startBranch, endDownBranchNum,
                isPWave, TURN, LayerPropogationType.DOWN,
                legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN),
                sourceBranchP.getMinRayParam(),
                sourceBranchP.getMaxRayParam(), receiverDepth);
        segmentTree.add(turnProto);
        try {
            if (tMod.isHeadWaveBranch(endDisconBranchNum, isPWave, isPWave) && ! excludeBranch.contains(endDisconBranchNum)) {
                ProtoSeismicPhase headProto = ProtoSeismicPhase.start(tMod,
                        startBranch, endDownBranchNum,
                        isPWave, HEAD, LayerPropogationType.DOWN,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN),
                        sourceBranchP.getMinRayParam(),
                        sourceBranchP.getMaxRayParam(), receiverDepth);
                segmentTree.add(headProto);
            }
        } catch (NoSuchLayerException e) {
            // oh well
        }
        if (tMod.isDiffractionBranch(endDisconBranchNum, isPWave) && ! excludeBranch.contains(endDisconBranchNum)) {
            ProtoSeismicPhase headProto = ProtoSeismicPhase.start( tMod,
                    startBranch, endDownBranchNum,
                    isPWave, DIFFRACT, LayerPropogationType.DOWN,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN),
                    sourceBranchP.getMinRayParam(),
                    sourceBranchP.getMaxRayParam(), receiverDepth);
            segmentTree.add(headProto);
        }

        if (receiverBranch <= endDisconBranchNum && receiverBranch > startBranch) {
            ProtoSeismicPhase endProto = ProtoSeismicPhase.start( tMod,
                    startBranch, receiverBranch,
                    isPWave, END, LayerPropogationType.DOWN,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN),
                    sourceBranchP.getMinRayParam(),
                    sourceBranchP.getMaxRayParam(), receiverDepth);
            segmentTree.add(endProto);
        }
        if (tMod.getSourceBranch() < tMod.getNumBranches() - 1) {
            double maxRP = sourceBranchP.getTopRayParam();
            for (int bnum = startBranch; bnum < endDisconBranchNum; bnum++) {
                maxRP = Math.min(maxRP, tMod.getTauBranch(bnum, isPWave).getMinTurnRayParam());
            }
            if ( ! excludeBranch.contains(endDisconBranchNum) && tMod.isDiscontinuityBranch(endDisconBranchNum, isPWave)) {
                ProtoSeismicPhase reflProto = ProtoSeismicPhase.start(tMod,
                        startBranch, endDownBranchNum,
                        isPWave, REFLECT_TOPSIDE, LayerPropogationType.DOWN,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN),
                        0, maxRP, receiverDepth);
                segmentTree.add(reflProto);
            }
            ProtoSeismicPhase transDProto = ProtoSeismicPhase.start( tMod,
                    startBranch, endDownBranchNum,
                    isPWave, TRANSDOWN, LayerPropogationType.DOWN,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN),
                    0, maxRP, receiverDepth);
            segmentTree.add(transDProto);

            if (tMod.isDiffractionBranch(endDisconBranchNum, isPWave) ) {
                ProtoSeismicPhase diffProto = ProtoSeismicPhase.start( tMod,
                        startBranch, endDownBranchNum,
                        isPWave, DIFFRACT, LayerPropogationType.DOWN,
                        legNameForTauBranch(tMod, endDisconBranchNum, isPWave, LayerPropogationType.DOWN),
                        sourceBranchP.getMinTurnRayParam(),
                        maxRP, receiverDepth);
                segmentTree.add(diffProto);
            }
        }
        return segmentTree;
    }

    public List<ProtoSeismicPhase> cleanDuplicates(List<ProtoSeismicPhase> in) {
        List<ProtoSeismicPhase> out = new ArrayList<>();
        List<ProtoSeismicPhase> sameSize = new ArrayList<>();

        int currSize = in.get(0).size();
        for (ProtoSeismicPhase next : in) {
            if (currSize == next.size()) {
                List<ProtoSeismicPhase> merged = new ArrayList<>();
                for (ProtoSeismicPhase p : sameSize) {
                    if (canMergePhases(p, next)) {
                        next = mergePhases(p, next);
                    } else {
                        merged.add(p);
                    }
                }
                merged.add(next);
                sameSize = merged;
            } else {
                out.addAll(sameSize);
                sameSize.clear();
                sameSize.add(next);
                currSize = next.size();
            }
        }
        out.addAll(sameSize);
        return out;
    }

    public boolean canMergePhases(ProtoSeismicPhase curr, ProtoSeismicPhase other) {

        if (curr.size() == other.size()-1
                && curr.get(0).endAction == TURN
                && other.get(0).endAction==TRANSDOWN
                && curr.get(0).endBranch == other.get(0).endBranch
                && curr.get(0).isPWave == other.get(0).isPWave) {
            // phase like P vs Ped20P
            // later legs match, but with offset of 1
            for (int s = 0; s < curr.size() && s < other.size()-1; s++) {
                SeismicPhaseSegment cS = curr.get(s);
                SeismicPhaseSegment oS = other.get(s+1);
                if (cS.isPWave != oS.isPWave
                        || cS.layerPropogationType != oS.layerPropogationType
                        || cS.endAction != oS.endAction
                        || !Objects.equals(cS.legName.substring(0,1), oS.legName)) {
                    return false;
                }
            }
            return true;
        } else if (curr.size() != other.size()) {
            return false;
        }
        SeismicPhaseSegment pS = null;
        for (int s = 0; s < curr.size(); s++) {
            SeismicPhaseSegment cS = curr.get(s);
            SeismicPhaseSegment oS = other.get(s);
            if (cS.isPWave != oS.isPWave
                    || cS.layerPropogationType != oS.layerPropogationType
                    || cS.endAction != oS.endAction
                    || !Objects.equals(cS.legName, oS.legName)) {
                return false;
            }
            if (cS.layerPropogationType==LayerPropogationType.DOWN) {
                if (cS.startBranch != oS.startBranch) {
                    return false;
                } else if (cS.endAction != TURN &&  cS.endBranch != oS.endBranch) {
                    return false;
                }
            } else {
                // upgoing
                if (cS.endBranch != oS.endBranch) {
                    return false;
                } else if (pS == null && cS.startBranch != oS.startBranch) {
                    return false;
                } else if (pS != null && (pS.endAction != TURN && cS.startBranch != oS.startBranch)) {
                    return false;
                }
            }
            pS = cS;
        }
        return true;
    }
    public ProtoSeismicPhase mergePhases(ProtoSeismicPhase curr, ProtoSeismicPhase other) {
        List<SeismicPhaseSegment> out = new ArrayList<>();
        SeismicPhaseSegment prevS = null;
        for (int s = 0; s < curr.size(); s++) {
            SeismicPhaseSegment cS = curr.get(s);
            SeismicPhaseSegment oS = other.get(s);
            if (cS.endAction == TURN) {
                if (cS.endBranch == oS.endBranch) {
                    out.add(cS);
                } else if (cS.endBranch < oS.endBranch) {
                    SeismicPhaseSegment m = new SeismicPhaseSegment(cS.tMod, cS.startBranch, oS.endBranch, cS.isPWave, cS.endAction, cS.layerPropogationType, cS.legName, oS.minRayParam, cS.maxRayParam, cS.prevEndAction);
                    out.add(m);
                } else  {
                    //if (cS.endBranch > oS.endBranch)
                    SeismicPhaseSegment m = new SeismicPhaseSegment(cS.tMod, cS.startBranch, oS.endBranch, cS.isPWave, cS.endAction, cS.layerPropogationType, cS.legName, cS.minRayParam, oS.maxRayParam, cS.prevEndAction);
                    out.add(m);
                }
            } else if (prevS != null && (prevS.endAction == TURN || prevS.endAction == DIFFRACTTURN|| prevS.endAction == HEADTURN)) {
                if (cS.startBranch == oS.startBranch) {
                    out.add(cS);
                } else if (cS.startBranch < oS.startBranch) {
                    SeismicPhaseSegment m = new SeismicPhaseSegment(cS.tMod, oS.startBranch, oS.endBranch, cS.isPWave, cS.endAction, cS.layerPropogationType, cS.legName, oS.minRayParam, cS.maxRayParam, cS.prevEndAction);
                    out.add(m);
                } else {
                    //if (cS.startBranch > oS.startBranch) {
                    SeismicPhaseSegment m = new SeismicPhaseSegment(cS.tMod, oS.startBranch, oS.endBranch, cS.isPWave, cS.endAction, cS.layerPropogationType, cS.legName, cS.minRayParam, oS.maxRayParam, cS.prevEndAction);
                    out.add(m);
                }
            } else {
                out.add(cS);
            }
            prevS = cS;
        }
        return new ProtoSeismicPhase(out, 0);
    }

    public List<ProtoSeismicPhase> walkPhases(TauModel tMod, List<ProtoSeismicPhase> segmentTree, int maxInteractions) throws TauModelException {
        List<ProtoSeismicPhase> nextSegmentTree = new ArrayList<>();
        boolean walkedAStep = false;
        for (ProtoSeismicPhase segList : segmentTree) {
            SeismicPhaseSegment endSeg = segList.endSegment();
            if (endSeg.endAction == END || endSeg.endAction == END_DOWN) {
                nextSegmentTree.add(segList);
            } else {
                int interactionNum = segList.calcInteractionNumber();
                if (interactionNum <= maxInteractions) {
                    segList.validateSegList();
                    List<ProtoSeismicPhase> calcedNext = new ArrayList<>();
                    if (allowPWave) {
                        calcedNext.addAll(nextLegs(tMod, segList, SeismicPhase.PWAVE));
                    }
                    if (allowSWave) {
                        calcedNext.addAll(nextLegs(tMod, segList, SeismicPhase.SWAVE));
                    }
                    for (ProtoSeismicPhase calcSegList : calcedNext) {
                        SeismicPhaseSegment calcendSeg = calcSegList.get(calcSegList.size()-1);
                        if (calcSegList.calcInteractionNumber() <= maxInteractions && ! calcSegList.isFail) {
                            ProtoSeismicPhase conProto = consolidateSegment(calcSegList);
                            nextSegmentTree.add(conProto);
                            walkedAStep = true;
                        } else {
                            if (TauPConfig.DEBUG) {
                                ProtoSeismicPhase conProto = consolidateSegment(calcSegList);
                                System.err.println("skip " + conProto.phaseNameForSegments()
                                        + " " + (calcSegList.calcInteractionNumber() <= maxInteractions)
                                        + " " + (calcendSeg.minRayParam < calcendSeg.maxRayParam)
                                );
                            }
                        }
                    }
                }
            }
        }
        nextSegmentTree = overlapsRayParam(nextSegmentTree, minRayParam, maxRayParam);
        if (walkedAStep ) {
            nextSegmentTree = walkPhases(tMod, nextSegmentTree, maxInteractions);
        }
        return nextSegmentTree;
    }

    public List<ProtoSeismicPhase> overlapsRayParam(List<ProtoSeismicPhase> segTree,
                                                            Double minRayParam, Double maxRayParam) {
        List<ProtoSeismicPhase> out = new ArrayList<>();
        for (ProtoSeismicPhase segList : segTree) {
            SeismicPhaseSegment endSeg = segList.get(segList.size()-1);
            if ((minRayParam == null || endSeg.maxRayParam >= minRayParam)
                    && (maxRayParam == null || endSeg.minRayParam <= maxRayParam)) {
                out.add(segList);
            }
        }
        return out;
    }

    public ProtoSeismicPhase walkToSurface(TauModel tMod, ProtoSeismicPhase proto, boolean isPWave, PhaseInteraction action) throws TauModelException {
        ProtoSeismicPhase out;
        ProtoSeismicPhase prev;
        if (proto.endSegment().layerPropogationType == LayerPropogationType.DIFF) {
            out = nextLegWithAction(tMod, proto, isPWave, DIFFRACTTURN);
        } else {
            out = proto;
        }
        while ( out != null && PhaseInteraction.isDowngoingActionAfter(out.getEndAction())) {
            prev = out;
            out = nextLegWithAction(tMod, out, isPWave, TURN);
            if (out == null) {
                out = nextLegWithAction(tMod, prev, isPWave, TRANSDOWN);
            }
        }
        prev = out;
        while (out != null && out.endSegment().endBranch > 0) {
            prev = out;
            out = nextLegWithAction(tMod, out, isPWave, TRANSUP);
            if (out == null) {
                out = nextLegWithAction(tMod, prev, isPWave, action);
            }
        }
        if (out != null && out.endSegment().endAction != action) {
            // back up one step and go to surface with end = action
            out = nextLegWithAction(tMod, prev, isPWave, action);
        }
        return out;
    }

    /**
     * Calculates all next legs, then returns the first (only?) protophase with the given action at the end. Used to
     * walk a path one step at a time.
     * @param tMod the model
     * @param proto starting proto phase
     * @param isPWave true for P, false for S
     * @param action desired end action
     * @return protophase one step further, null if not possible
     * @throws TauModelException
     */
    public ProtoSeismicPhase nextLegWithAction(TauModel tMod, ProtoSeismicPhase proto, boolean isPWave, PhaseInteraction action) throws TauModelException {
        List<ProtoSeismicPhase> nextLegs = nextLegs(tMod, proto, isPWave);
        for (ProtoSeismicPhase p : nextLegs) {
            if (p.endSegment().endAction == action) {
                return p;
            }
        }
        return null;
    }

    public List<ProtoSeismicPhase> nextLegs(TauModel tMod, ProtoSeismicPhase proto, boolean isPWave) throws TauModelException {
        List<ProtoSeismicPhase> outTree = new ArrayList<>();
        if (proto.getEndAction() == FAIL) {
            return outTree;
        }
        SeismicPhaseSegment prevEndSeg = proto.endSegment();

        if (isPWave != prevEndSeg.isPWave) {
            // don't do phase change at excluded boundary
            if (prevEndSeg.layerPropogationType==LayerPropogationType.DOWN && excludeBranch.contains(prevEndSeg.endBranch + 1)) {
                return outTree;
            }
            if ( prevEndSeg.layerPropogationType==LayerPropogationType.UP && excludeBranch.contains(prevEndSeg.endBranch)) {
                return outTree;
            }
        }
        int startBranchNum = proto.nextStartBranch();

        if (isPWave == SeismicPhase.SWAVE
                && tMod.getSlownessModel().depthInFluid(tMod.getTauBranch(startBranchNum, true).getTopDepth())) {
            // no s wave in fluid layer
            return outTree;
        }

        int endDisconBranchNum = ProtoSeismicPhase.findEndDiscon(tMod, startBranchNum, isPWave,
                layerPropogationTypeAfter(prevEndSeg.endAction));

        // check if endBranchNum is past receiver, in which case ending is possible
        switch (prevEndSeg.endAction) {
            case HEADTURN:
            case TRANSUP:
                if (receiverBranch < prevEndSeg.endBranch && receiverBranch >= endDisconBranchNum) {
                    outTreeAdd(outTree, proto.nextSegment(isPWave, receiverBranch, END));
                }
                break;
            case DIFFRACTTURN:
                if (receiverBranch <= prevEndSeg.endBranch && receiverBranch >= endDisconBranchNum && isPWave == prevEndSeg.isPWave) {
                    // diff cannot phase convert
                    outTreeAdd(outTree, proto.nextSegment( prevEndSeg.isPWave, receiverBranch, END));
                }
                break;
            case DIFFRACTDOWN:
                // seems weird???
                if (receiverBranch > prevEndSeg.endBranch && receiverBranch <= endDisconBranchNum+2) {  // maybe +1???
                    outTreeAdd(outTree, proto.nextSegment( isPWave, END));
                }
                break;
            case TURN:
                if (receiverBranch <= prevEndSeg.endBranch && receiverBranch >= endDisconBranchNum && isPWave == prevEndSeg.isPWave) {
                    // turn cannot phase convert
                    outTreeAdd(outTree, proto.nextSegment( prevEndSeg.isPWave, endDisconBranchNum, END));
                }
                break;
            case REFLECT_TOPSIDE:
                if (receiverBranch <= prevEndSeg.endBranch && receiverBranch >= endDisconBranchNum ) {
                    outTreeAdd(outTree, proto.nextSegment( isPWave, END));
                }
                break;
            case REFLECT_UNDERSIDE:
                if (receiverBranch > prevEndSeg.endBranch && receiverBranch <= endDisconBranchNum+1) {
                    outTreeAdd(outTree, proto.nextSegment( isPWave, END));
                }
                break;
            case TRANSDOWN:
                // seems weird???
                if (receiverBranch > prevEndSeg.endBranch && receiverBranch <= endDisconBranchNum+2) {  // maybe +1???
                    outTreeAdd(outTree, proto.nextSegment( isPWave, END));
                }
                break;
            case DIFFRACT:
                if (receiverBranch == prevEndSeg.endBranch && receiverDepth == prevEndSeg.getBotDepth()) {
                    outTreeAdd(outTree, proto.nextSegment( isPWave, END));
                }
                break;
            case HEAD:
                if (receiverBranch == prevEndSeg.endBranch && receiverDepth == prevEndSeg.getTopDepth()) {
                    outTreeAdd(outTree, proto.nextSegment( isPWave, END));
                }
                break;
            case TRANSUPDIFFRACT:
                if (receiverBranch == prevEndSeg.endBranch-1) {
                    outTreeAdd(outTree, proto.nextSegment( isPWave, END));
                }
                break;
            default:
                throw new TauModelException("Unknown endAction: "+prevEndSeg.endAction);
        }
        switch (prevEndSeg.endAction) {
            case END:
            case FAIL:
                outTreeAdd(outTree, proto);
                break;
            case REFLECT_UNDERSIDE:
            case TRANSDOWN:
            case DIFFRACTDOWN:
                ProtoSeismicPhase turnPhase = proto.nextSegment(isPWave, TURN);
                outTreeAdd(outTree, turnPhase);
                if (startBranchNum < tMod.getNumBranches()-1) {
                    outTreeAdd(outTree, proto.nextSegment(isPWave, TRANSDOWN));
                    int endDiscon = ProtoSeismicPhase.findEndDiscon(tMod, startBranchNum, isPWave, LayerPropogationType.DOWN);
                    if (!excludeBranch.contains(endDiscon)) {
                        if (endDiscon == tMod.getMohoBranch()) {
                            ProtoSeismicPhase ref = proto.nextSegment(isPWave, endDiscon, REFLECT_TOPSIDE);
                        }
                        outTreeAdd(outTree, proto.nextSegment(isPWave, endDiscon, REFLECT_TOPSIDE));
                        if (tMod.isDiffractionBranch(endDiscon, isPWave)) {
                            outTreeAdd(outTree, proto.nextSegment(isPWave, endDiscon, DIFFRACT));
                        }
                        if (tMod.isHeadWaveBranch(endDiscon, prevEndSeg.isPWave, isPWave)) {
                            outTreeAdd(outTree, proto.nextSegment(isPWave, endDiscon, HEAD));
                        }
                    }
                }
                break;
            case HEAD:
                if (startBranchNum > 0) {
                    if (isPWave || ! tMod.isFluidBranch(startBranchNum)) {
                        outTreeAdd(outTree, proto.nextSegment(isPWave, startBranchNum, HEADTURN));
                    }
                }
                break;
            case DIFFRACT:
            case TRANSUPDIFFRACT:
                if (isPWave == prevEndSeg.isPWave && startBranchNum > 0) {
                    if (isPWave || ! tMod.isFluidBranch(startBranchNum)) {
                        // discon is one below start for flat diff leg
                        outTreeAdd(outTree, proto.nextSegment(isPWave, startBranchNum+1, DIFFRACTTURN));
                        outTreeAdd(outTree, proto.nextSegment(isPWave, startBranchNum+1, DIFFRACTDOWN));
                    }
                }
                break;
            case REFLECT_TOPSIDE:
            case HEADTURN:
            case TRANSUP:
                if (startBranchNum > 0) {
                    if (isPWave || ! tMod.isFluidBranch(startBranchNum)) {
                        outTreeAdd(outTree, proto.nextSegment(isPWave, TRANSUP));
                    }
                }
                int endDiscon = ProtoSeismicPhase.findEndDiscon(tMod, startBranchNum, isPWave, LayerPropogationType.UP);
                if ( ! excludeBranch.contains(endDiscon)) {
                    outTreeAdd(outTree, proto.nextSegment(isPWave, endDiscon, REFLECT_UNDERSIDE));
                    if (tMod.isDiffractionBranch(endDiscon, isPWave)) {
                        // should allow up to diffract???
                        //outTreeAdd(outTree, proto.nextSegment(isPWave, endDiscon, TRANSUPDIFFRACT));
                    }
                }
                break;
            case TURN:
            case DIFFRACTTURN:
                if (isPWave == prevEndSeg.isPWave) {
                    // turn cannot phase convert
                    int endDisconAfterTurn = ProtoSeismicPhase.findEndDiscon(tMod, startBranchNum, isPWave, LayerPropogationType.UP);
                    if (prevEndSeg.endBranch > 0) {
                            // exclude phase convertion if fluid
                        if (isPWave || ! tMod.isFluidBranch(startBranchNum)) {
                            outTreeAdd(outTree, proto.nextSegment(prevEndSeg.isPWave, endDisconAfterTurn, TRANSUP));
                        }

                    }
                    if ( ! excludeBranch.contains(endDisconAfterTurn) ) {
                        outTreeAdd(outTree, proto.nextSegment(prevEndSeg.isPWave, endDisconAfterTurn, REFLECT_UNDERSIDE));
                    }
                }
                break;
            default:
                throw new TauModelException("Unknown endAction: "+prevEndSeg.endAction);
        }

        return outTree;
    }

    public static void outTreeAdd(List<ProtoSeismicPhase> outTree, ProtoSeismicPhase proto) {
        if (proto != null) {
            SeismicPhaseSegment flatSeg = proto.getFlatSegment();
            if (flatSeg!= null) {
                for (SeismicPhaseSegment seg : proto.segmentList) {
                    if (seg.isPWave == flatSeg.isPWave && seg != flatSeg
                            && !LayerPropogationType.isFlat(seg.layerPropogationType)
                            && seg.getEndDepth()==flatSeg.getEndDepth()
                            && (seg.endAction == TURN || seg.endAction == REFLECT_TOPSIDE || seg.endAction == REFLECT_TOPSIDE_CRITICAL)
                    ) {
                        if(TauPConfig.DEBUG) {
                            System.err.println("Not adding "+proto.getPuristName()+" as "+seg.endAction+" at same depth as "+flatSeg.endAction+", "+flatSeg.getEndDepth());
                        }
                        return;
                    }
                }
            }
            SeismicPhaseSegment endSeg = proto.get(proto.size()-1);
            // only phases with a head/diff leg can exist with degenerate ray parameter range
            if (flatSeg != null || endSeg.minRayParam < endSeg.maxRayParam) {
                outTree.add(proto);
            }
        }
    }

    public static String legNameForTauBranch(TauModel tMod, int branchNum, boolean isPWave, LayerPropogationType layerPropogationType) {
        if (branchNum < 0 || branchNum >= tMod.getNumBranches()) {
            return "unknown";
        }
        TauBranch tauBranch = tMod.getTauBranch(branchNum, isPWave);
        if (branchNum >= tMod.getIocbBranch()) {
            if (tauBranch.isPWave) {
                if (layerPropogationType == LayerPropogationType.HEAD || layerPropogationType == LayerPropogationType.DIFF) {
                    return "I";
                } else if (layerPropogationType == LayerPropogationType.DOWN) {
                    return "Ied";
                } else {
                    // UP
                    return "y";
                }
            } else {
                if (layerPropogationType == LayerPropogationType.HEAD || layerPropogationType == LayerPropogationType.DIFF) {
                    return "J";
                } else if (layerPropogationType == LayerPropogationType.DOWN) {
                    return "Jed";
                } else {
                    // UP
                    return "j";
                }
            }
        }
        if (branchNum >= tMod.getCmbBranch()) {
            if (tauBranch.isPWave) {
                if (layerPropogationType == LayerPropogationType.HEAD || layerPropogationType == LayerPropogationType.DIFF) {
                    return "K";
                } else if (layerPropogationType == LayerPropogationType.DOWN) {
                    return "Ked";
                } else {
                    // UP
                    return "k";
                }
            } else {
                return "[outercore_S]";
            }
        }
        if (tauBranch.isPWave) {
            if (layerPropogationType == LayerPropogationType.HEAD || layerPropogationType == LayerPropogationType.DIFF) {
                return "P";
            } else if (layerPropogationType == LayerPropogationType.DOWN) {
                return "Ped";
            } else {
                // UP
                return "p";
            }
        } else {
            if (layerPropogationType == LayerPropogationType.HEAD || layerPropogationType == LayerPropogationType.DIFF) {
                return "S";
            } else if (layerPropogationType == LayerPropogationType.DOWN) {
                return "Sed";
            } else {
                // UP
                return "s";
            }
        }
    }

    public ProtoSeismicPhase consolidateSegment(ProtoSeismicPhase proto) throws TauModelException {
        proto.validateSegList();

        ProtoSeismicPhase outSegmentList = consolidateTrans(proto);
        outSegmentList.validateSegList();
        return outSegmentList;
        //return proto;
    }

    public ProtoSeismicPhase consolidateTrans(ProtoSeismicPhase proto) throws TauModelException {
        List<SeismicPhaseSegment> out = new ArrayList<>();
        SeismicPhaseSegment prev = null;
        for (SeismicPhaseSegment seg : proto.segmentList) {
            if (prev != null && !prev.legName.isEmpty() && !seg.legName.isEmpty()
                    && (prev.endAction == TRANSDOWN || prev.endAction == TRANSUP)
                    && prev.isPWave == seg.isPWave
                    && Objects.equals(prev.legName.charAt(0), seg.legName.charAt(0))) {
                SeismicPhaseSegment conSeg = new SeismicPhaseSegment(prev.tMod,
                        prev.startBranch, seg.endBranch, prev.isPWave, seg.endAction, prev.layerPropogationType,
                        prev.legName,
                        Math.max(prev.minRayParam, seg.minRayParam),
                        Math.min(prev.maxRayParam, seg.maxRayParam),
                        prev.prevEndAction);
                out.remove(prev);
                out.add(conSeg);
                prev = conSeg;
            } else {
                out.add(seg);
                prev = seg;
            }
        }
        ProtoSeismicPhase conProto = new ProtoSeismicPhase(out, 0);
        conProto.validateSegList();
        return conProto;
    }

    public boolean isAllowSWave() {
        return allowSWave;
    }

    public void setAllowSWave(boolean allowSWave) {
        this.allowSWave = allowSWave;
    }

    public boolean isAllowPWave() {
        return allowPWave;
    }

    public void setAllowPWave(boolean allowPWave) {
        this.allowPWave = allowPWave;
    }

    public TauModel gettMod() {
        return tMod;
    }

    public List<Integer> getExcludeBranch() {
        return excludeBranch;
    }

    boolean allowSWave = true;
    boolean allowPWave = true;

}
