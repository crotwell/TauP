package edu.sc.seis.TauP;

import java.util.*;
import java.util.stream.Collectors;

import static edu.sc.seis.TauP.PhaseInteraction.*;
import static edu.sc.seis.TauP.SeismicPhase.PWAVE;
import static edu.sc.seis.TauP.SeismicPhase.SWAVE;

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
        seismicNamingLayers = tMod.getNamingLayers();
    }

    public void excludeBoundaries(double[] layerDepths) throws TauModelException {
        List<Double> depths = new ArrayList<>();
        for (double d : layerDepths) {
            depths.add(d);
        }
        excludeBoundaries(depths);
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
        segmentTree = cleanDuplicates(segmentTree); // remove duplicate phases
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
            segmentTree.addAll( createSourceSegments(tMod, SWAVE, receiverDepth));
        }
        if (allowPWave) {
            segmentTree.addAll( createSourceSegments(tMod, PWAVE, receiverDepth));
        }
        segmentTree = overlapsRayParam(segmentTree, minRayParam, maxRayParam);
        segmentTree = walkPhases(tMod, segmentTree, maxAction);
        return segmentTree;
    }

    public List<ProtoSeismicPhase> createSourceSegments(TauModel tMod, boolean isPWave, double receiverDepth) {
        List<ProtoSeismicPhase> segmentTree =  new ArrayList<>();
        // Upgoing phases:
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
                        seismicNamingLayers.legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.UP, END),
                        0, aboveSourceBranchP.getMinTurnRayParam(), receiverDepth);
                segmentTree.add(upProto);
            }
            if ( ! excludeBranch.contains(aboveDisconBranch) && tMod.isDiscontinuityBranch(aboveDisconBranch, isPWave) ) {
                ProtoSeismicPhase reflProto = ProtoSeismicPhase.start(tMod,
                        startUpBranchNum, aboveDisconBranch,
                        isPWave, REFLECT_UNDERSIDE, LayerPropogationType.UP,
                        seismicNamingLayers.legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.UP, REFLECT_UNDERSIDE),
                        0, aboveSourceBranchP.getMinTurnRayParam(), receiverDepth);
                segmentTree.add(reflProto);
            }
            if (aboveDisconBranch > 0) {
                ProtoSeismicPhase upProto = ProtoSeismicPhase.start(tMod,
                        startUpBranchNum, aboveDisconBranch,
                        isPWave, TRANSUP, LayerPropogationType.UP,
                        seismicNamingLayers.legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.UP, TRANSUP),
                        0, aboveSourceBranchP.getMinTurnRayParam(), receiverDepth);
                segmentTree.add(upProto);
                /*
                if (tMod.isDiffractionBranch(aboveDisconBranch, isPWave) && ! excludeBranch.contains(aboveDisconBranch)) {
                    ProtoSeismicPhase upDiff = ProtoSeismicPhase.start(tMod,
                            startUpBranchNum, aboveDisconBranch,
                            isPWave, TRANSUPDIFFRACT, LayerPropogationType.UP,
                            seismicNamingLayers.legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.UP, TRANSUPDIFFRACT),
                            0, aboveSourceBranchP.getMinTurnRayParam(), receiverDepth);
                    segmentTree.add(upDiff);
                }
                */
                if (tMod.isDiffractionBranch(startUpBranchNum+1, isPWave) && ! excludeBranch.contains(startUpBranchNum+1)) {
                    ProtoSeismicPhase diffProto = ProtoSeismicPhase.start( tMod,
                            startUpBranchNum, startUpBranchNum,
                            isPWave, DIFFRACTTURN, LayerPropogationType.DIFF,
                            seismicNamingLayers.legNameForTauBranch(tMod.getSourceBranch()-1, isPWave, LayerPropogationType.DOWN, DIFFRACTTURN),
                            aboveSourceBranchP.getMinTurnRayParam(),
                            aboveSourceBranchP.getMinTurnRayParam(), receiverDepth);
                    segmentTree.add(diffProto);
                }
            }
        }

        // downgoing phases:
        int startBranch = tMod.getSourceBranch();

        TauBranch sourceBranchP = tMod.getTauBranch(tMod.getSourceBranch(), isPWave);
        int endDisconBranchNum = ProtoSeismicPhase.findEndDiscon(tMod, tMod.getSourceBranch(), isPWave,
                LayerPropogationType.DOWN);
        int endDownBranchNum = endDisconBranchNum-1;

        // downgoing options are END, TURN, HEAD, REFLECT_TOPSIDE or TRANSDOWN

        int legNameChangeDiscon = seismicNamingLayers.disconBranchBelow(endDisconBranchNum);
        int endTurnBranchNum = legNameChangeDiscon-1; // end above discon

        double minTurnInSegRayParam = sourceBranchP.getMinTurnRayParam();
        for (int bnum = startBranch; bnum <= endTurnBranchNum; bnum++) {
            minTurnInSegRayParam = Math.min(minTurnInSegRayParam, tMod.getTauBranch(bnum,
                    isPWave).getMinTurnRayParam()); // should be getMinRayParam???
        }

        ProtoSeismicPhase turnProto = ProtoSeismicPhase.start(tMod,
                startBranch, endTurnBranchNum,
                isPWave, TURN, LayerPropogationType.DOWN,
                seismicNamingLayers.legNameForTauBranch(startBranch, isPWave, LayerPropogationType.DOWN, TURN),
                minTurnInSegRayParam,
                sourceBranchP.getMaxRayParam(), receiverDepth);
        segmentTree.add(turnProto);
        try {
            if (tMod.isHeadWaveBranch(endDisconBranchNum, isPWave, isPWave) && ! excludeBranch.contains(endDisconBranchNum)) {
                ProtoSeismicPhase headProto = ProtoSeismicPhase.start(tMod,
                        startBranch, endDownBranchNum,
                        isPWave, HEAD, LayerPropogationType.DOWN,
                        seismicNamingLayers.legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN, HEAD),
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
                    seismicNamingLayers.legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN, DIFFRACT),
                    sourceBranchP.getMinRayParam(),
                    sourceBranchP.getMaxRayParam(), receiverDepth);
            segmentTree.add(headProto);
        }

        if (receiverBranch <= endDisconBranchNum && receiverBranch > startBranch) {
            ProtoSeismicPhase endProto = ProtoSeismicPhase.start( tMod,
                    startBranch, receiverBranch,
                    isPWave, END_DOWN, LayerPropogationType.DOWN,
                    seismicNamingLayers.legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN, END_DOWN),
                    sourceBranchP.getMinRayParam(),
                    sourceBranchP.getMaxRayParam(), receiverDepth);
            segmentTree.add(endProto);
        }
        if (tMod.getSourceBranch() < tMod.getNumBranches() - 1) {
            // source not center of earth
            double maxRP = sourceBranchP.getTopRayParam();
            for (int bnum = startBranch; bnum < endDisconBranchNum; bnum++) {
                maxRP = Math.min(maxRP, tMod.getTauBranch(bnum, isPWave).getMinTurnRayParam());
            }
            if ( ! excludeBranch.contains(endDisconBranchNum) && tMod.isDiscontinuityBranch(endDisconBranchNum, isPWave)) {
                ProtoSeismicPhase reflProto = ProtoSeismicPhase.start(tMod,
                        startBranch, endDownBranchNum,
                        isPWave, REFLECT_TOPSIDE, LayerPropogationType.DOWN,
                        seismicNamingLayers.legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN, REFLECT_TOPSIDE),
                        0, maxRP, receiverDepth);
                segmentTree.add(reflProto);
            }
            ProtoSeismicPhase transDProto = ProtoSeismicPhase.start( tMod,
                    startBranch, endDownBranchNum,
                    isPWave, TRANSDOWN, LayerPropogationType.DOWN,
                    seismicNamingLayers.legNameForTauBranch(tMod.getSourceBranch(), isPWave, LayerPropogationType.DOWN, TRANSDOWN),
                    0, maxRP, receiverDepth);
            segmentTree.add(transDProto);

            if (! excludeBranch.contains(endDisconBranchNum) && tMod.isDiffractionBranch(endDisconBranchNum, isPWave) ) {
                ProtoSeismicPhase diffProto = ProtoSeismicPhase.start( tMod,
                        startBranch, endDownBranchNum,
                        isPWave, DIFFRACT, LayerPropogationType.DOWN,
                        seismicNamingLayers.legNameForTauBranch(endDisconBranchNum, isPWave, LayerPropogationType.DOWN, DIFFRACT),
                        sourceBranchP.getMinTurnRayParam(),
                        maxRP, receiverDepth);
                segmentTree.add(diffProto);
            }
        }
        return segmentTree;
    }

    public static List<ProtoSeismicPhase> cleanDuplicates(List<ProtoSeismicPhase> in) {
        List<ProtoSeismicPhase> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            ProtoSeismicPhase p = in.get(i);
            boolean foundMatch=false;
            for (ProtoSeismicPhase pp : in.subList(i+1, in.size())) {
                if ( identicalPhases(p, pp)) {
                    foundMatch=true;
                    break;
                }
            }
            if ( ! foundMatch) {
                out.add(p);
            }
        }
        return out;
    }

    public static boolean identicalPhases(ProtoSeismicPhase curr, ProtoSeismicPhase other) {
        if (curr.size() != other.size()) {
            return false;
        }
        for (int s = 0; s < curr.size(); s++) {
            SeismicPhaseSegment cS = curr.get(s);
            SeismicPhaseSegment oS = other.get(s);
            if (cS.startBranch != oS.startBranch
                    || cS.endBranch != oS.endBranch
                    || cS.isPWave != oS.isPWave
                    || cS.endAction != oS.endAction
            ) {
                return false;
            }
        }
        return true;
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
                        || !Objects.equals(cS.legName.substring(0,1), oS.legName.substring(0,1))) {
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
                    || !Objects.equals(cS.legName.charAt(0), oS.legName.charAt(0))) {
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
                    // use segment with wider ray param range, like a TURN vs TRANSDOWN followed by TURN
                    if (cS.getMaxRayParam()>=oS.getMaxRayParam() ) {
                        out.add(cS);
                    } else {
                        out.add(oS);
                    }
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
        return new ProtoSeismicPhase(out, curr.receiverDepth);
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
                        calcedNext.addAll(nextLegs(tMod, segList, PWAVE));
                    }
                    if (allowSWave) {
                        calcedNext.addAll(nextLegs(tMod, segList, SWAVE));
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
            if (out == null || out.isFail) {
                out = nextLegWithAction(tMod, prev, isPWave, TRANSDOWN);
            }
        }
        prev = out;
        while (out != null && out.endSegment().endBranch > 0) {
            prev = out;
            out = null;
            // see if we can just do it
            out = nextLegWithAction(tMod, prev, isPWave, action);
            if (out != null && out.getEndAction()==action && out.endSegment().endBranch == 0) {
                return out;
            }
            // nope, need to go up first
            out = null;
            List<ProtoSeismicPhase> upList = transAcrossExcludedBoundaries(tMod, prev, isPWave, TRANSUP);
            for (ProtoSeismicPhase p : upList) {
                if (p.endSegment().endBranch == 0 && p.getEndAction() == action) {
                    return p;
                } else if (p.endSegment().endBranch > 0 && p.getEndAction() == TRANSUP
                        && (out == null || out.endSegment().endBranch>p.endSegment().endBranch)) {
                    // grab a transup and keep going
                    out = p;
                }
            }
        }
        if (out != null && out.getEndAction()==action && out.endSegment().endBranch == 0) {
            return out;
        } else {
            System.err.println("Can't walkToSurface to surface for "+action+" prev: "+prev.branchNumSeqStrWithSegBreaks());
            System.err.println(out);
            return nextLegWithAction(tMod, prev, isPWave, action);
        }
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

        if (isPWave == SWAVE
                && tMod.getSlownessModel().depthInFluid(tMod.getTauBranch(startBranchNum, true).getTopDepth())) {
            // no s wave in fluid layer
            return outTree;
        }

        int endDisconBranchNum = ProtoSeismicPhase.findEndDiscon(tMod, startBranchNum, isPWave,
                layerPropogationTypeAfter(prevEndSeg.endAction));


        switch (prevEndSeg.endAction) {
            case END:
            case FAIL:
                outTreeAdd(outTree, proto);
                break;
            case REFLECT_UNDERSIDE:
            case TRANSDOWN:
            case DIFFRACTDOWN:
                // transdown should not become turn unless legname changes
                if (prevEndSeg.endAction!=TRANSDOWN || prevEndSeg.isPWave!=isPWave
                        || startBranchNum==seismicNamingLayers.rangeForBranchNum(startBranchNum).get(0)) {
                    int legNameChangeDiscon = seismicNamingLayers.disconBranchBelow(startBranchNum);
                    ProtoSeismicPhase turnPhase = proto.nextSegment(isPWave, legNameChangeDiscon, TURN);
                    outTreeAdd(outTree, turnPhase);
                }
                if (startBranchNum < tMod.getNumBranches()-1) {
                    if (isPWave || ! tMod.isFluidBranch(startBranchNum)) {
                        for (ProtoSeismicPhase p : transAcrossExcludedBoundaries(tMod, proto, isPWave, TRANSDOWN)) {
                            outTreeAdd(outTree, p);
                        }
                    }
                    int endDiscon = ProtoSeismicPhase.findEndDiscon(tMod, startBranchNum, isPWave, LayerPropogationType.DOWN);
                    if (!excludeBranch.contains(endDiscon) && tMod.isDiscontinuityBranch(endDiscon, isPWave)) {
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
                    if (receiverBranch == prevEndSeg.endBranch && receiverDepth == prevEndSeg.getTopDepth()) {
                        outTreeAdd(outTree, proto.nextSegment( isPWave, END));
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
                        if (receiverBranch == prevEndSeg.endBranch && receiverDepth == prevEndSeg.getBotDepth() ) {
                            outTreeAdd(outTree, proto.nextSegment( isPWave, END));
                        }
                    }
                }
                break;
            case REFLECT_TOPSIDE:
            case HEADTURN:
            case TRANSUP:
                if (startBranchNum > 0) {
                    if (isPWave || ! tMod.isFluidBranch(startBranchNum)) {
                        for (ProtoSeismicPhase p : transAcrossExcludedBoundaries(tMod, proto, isPWave, TRANSUP)) {
                            outTreeAdd(outTree, p);
                        }
                    }
                }
                int endDiscon = ProtoSeismicPhase.findEndDiscon(tMod, startBranchNum, isPWave, LayerPropogationType.UP);

                if (receiverBranch >= endDiscon ) {
                    outTreeAdd(outTree, proto.nextSegment(isPWave, receiverBranch, END));
                }
                if ( ! excludeBranch.contains(endDiscon) && tMod.isDiscontinuityBranch(endDiscon, isPWave)) {
                    outTreeAdd(outTree, proto.nextSegment(isPWave, endDiscon, REFLECT_UNDERSIDE));
                    /*
                    if (tMod.isDiffractionBranch(endDiscon, isPWave)) {
                        // should allow up to diffract???
                        outTreeAdd(outTree, proto.nextSegment(isPWave, endDiscon, TRANSUPDIFFRACT));
                    }
                    */
                }
                break;
            case TURN:
                if (isPWave == prevEndSeg.isPWave) {
                    for (ProtoSeismicPhase p : transUpAfterTurn(tMod, proto, isPWave, TRANSUP)) {
                        outTreeAdd(outTree, p);
                    }
                }
                break;
            case DIFFRACTTURN:
                if (isPWave == prevEndSeg.isPWave) {
                    // turn cannot phase convert
                    int endDisconAfterTurn = ProtoSeismicPhase.findEndDiscon(tMod, startBranchNum, isPWave, LayerPropogationType.UP);
                    if (receiverBranch <= prevEndSeg.endBranch && receiverBranch >= endDisconAfterTurn && isPWave == prevEndSeg.isPWave) {
                        // diff cannot phase convert
                        //outTreeAdd(outTree, proto.nextSegment( prevEndSeg.isPWave, receiverBranch, END));
                    }
                    if (prevEndSeg.endBranch > 0) {
                            // exclude phase convertion if fluid
                        if (isPWave || ! tMod.isFluidBranch(startBranchNum)) {
                            for (ProtoSeismicPhase p : transAcrossExcludedBoundaries(tMod, proto, isPWave, TRANSUP)) {
                                outTreeAdd(outTree, p);
                            }
                        }

                    }
                    if ( ! excludeBranch.contains(endDisconAfterTurn) ) {
                        if (  tMod.isDiscontinuityBranch(endDisconAfterTurn, isPWave)) {
                            outTreeAdd(outTree, proto.nextSegment(prevEndSeg.isPWave, endDisconAfterTurn, REFLECT_UNDERSIDE));
                        }
                    }
                }
                break;
            default:
                throw new TauModelException("Unknown endAction: "+prevEndSeg.endAction);
        }
        return outTree;
    }

    private List<ProtoSeismicPhase> transUpAfterTurn(TauModel tMod, ProtoSeismicPhase proto, boolean isPWave, PhaseInteraction phaseInteraction) throws TauModelException {
        if (proto.getEndAction() != TURN) {
            throw new RuntimeException("Proto must endAction==TURN, but "+proto.getEndAction());
        }
        if (isPWave != proto.endSegment().isPWave) {
            throw new RuntimeException("Cannot phase change at point of turn: p="+isPWave+" but "+proto.branchNumSeqStrWithSegBreaks());
        }
        int legNameChangeDiscon = seismicNamingLayers.disconBranchAbove(proto.endSegment().endBranch);
        List<ProtoSeismicPhase> outTree = new ArrayList<>();
        // might have to go above turn's startBranch, ex if turn came from source so not a real discontinuity
        int topBranch = proto.endSegment().startBranch;
        while (topBranch > legNameChangeDiscon && (excludeBranch.contains(topBranch) || ! tMod.isDiscontinuityBranch(topBranch, isPWave))) {
            topBranch--;
        }
        outTree.add(proto.nextSegment(isPWave, topBranch, TRANSUP));
        for (int endDiscon = proto.endSegment().endBranch; endDiscon >= topBranch; endDiscon--) {
            if ( ! excludeBranch.contains(endDiscon) && tMod.isDiscontinuityBranch(endDiscon, isPWave)) {
                outTreeAdd(outTree, proto.nextSegment(isPWave, endDiscon, TRANSUP));
                outTreeAdd(outTree, proto.nextSegment(isPWave, endDiscon, REFLECT_UNDERSIDE));
                if (tMod.isDiffractionBranch(endDiscon, isPWave)) {
                    // should allow up to diffract???
                    //outTreeAdd(outTree, proto.nextSegment(isPWave, endDiscon, TRANSUPDIFFRACT));
                }
            }
            if (receiverBranch == endDiscon) {
                outTreeAdd(outTree, proto.nextSegment(isPWave, endDiscon, END));
            }
        }
        return outTree;
    }

    public List<ProtoSeismicPhase> transAcrossExcludedBoundaries(TauModel tMod, ProtoSeismicPhase proto, boolean isPWave, PhaseInteraction transAction)
            throws TauModelException {
        if (!(transAction == TRANSUP || transAction == TRANSDOWN)) {
            throw new IllegalArgumentException("End action must be TRANSUP or TRANSDOWN, but was "+transAction);
        }
        if (proto.isFail) {
            return List.of(proto);
        }
        ProtoSeismicPhase prevTransPhase = proto;
        ProtoSeismicPhase transPhase = proto.nextSegment(isPWave, transAction);
        // try ending directly
        ProtoSeismicPhase endingPhase = null;
        List<ProtoSeismicPhase> out = new ArrayList<>();
        int startBranchNum = proto.nextStartBranch();

        int legNameChangeDiscon;
        if (transAction == TRANSDOWN) {
            legNameChangeDiscon = seismicNamingLayers.disconBranchBelow(startBranchNum);
        } else {
            legNameChangeDiscon = seismicNamingLayers.disconBranchAbove(startBranchNum);
        }
        int endDiscon = ProtoSeismicPhase.findEndDiscon(tMod, startBranchNum, isPWave,
                transAction==TRANSDOWN?LayerPropogationType.DOWN:LayerPropogationType.UP);

        if (transAction == TRANSDOWN
                && receiverBranch > startBranchNum && receiverBranch <= endDiscon
                && endDiscon <= legNameChangeDiscon ) {
            endingPhase =  proto.nextSegment(isPWave, receiverBranch, END_DOWN);
        } else if (transAction == TRANSUP
                && receiverBranch <= startBranchNum  && receiverBranch >= endDiscon
                && endDiscon >= legNameChangeDiscon) {
            endingPhase =  proto.nextSegment(isPWave, receiverBranch, END);
        }
        if (endingPhase!=null && endingPhase.isSuccessful()) {
            out.add(endingPhase);
        }

        while (transPhase.isSuccessful()
                && ( (transAction==TRANSUP && transPhase.endSegment().getEndBranch() > legNameChangeDiscon)
                    || (transAction==TRANSDOWN && transPhase.endSegment().getEndBranch() < legNameChangeDiscon-1)
                ) && (isPWave || ! tMod.isFluidBranch(transPhase.nextStartBranch()))
                && excludeBranch.contains(transPhase.endSegment().getEndBranch())) {
            // keep going trans up if discon is in exclude list
            // but check to see if can end at receiver on the way
            endingPhase=null;
            startBranchNum = transPhase.nextStartBranch();
            endDiscon = ProtoSeismicPhase.findEndDiscon(tMod, startBranchNum, isPWave,
                    transAction==TRANSDOWN?LayerPropogationType.DOWN:LayerPropogationType.UP);
            prevTransPhase = transPhase;
            transPhase = transPhase.nextSegment(isPWave, transAction);

            if (transAction == TRANSDOWN) {
                if ( receiverBranch >= startBranchNum && receiverBranch <= endDiscon
                        && endDiscon <= legNameChangeDiscon  ) {
                    endingPhase = prevTransPhase.nextSegment(isPWave, receiverBranch, END_DOWN);
                }
            } else {
                // TRANSUP
                if (receiverBranch <= startBranchNum  && receiverBranch >= endDiscon
                        && endDiscon >= legNameChangeDiscon ) {
                    endingPhase = prevTransPhase.nextSegment(isPWave, receiverBranch, END);
                }
            }
            if (endingPhase!=null && endingPhase.isSuccessful()) {
                out.add(endingPhase);

            }
            if (transAction == TRANSDOWN) {
                if (transPhase.endSegment().endBranch >= legNameChangeDiscon-1
                        || !(isPWave ||  tMod.isFluidBranch(transPhase.endSegment().getEndBranch()-1))
                ) {
                    // don't go past a leg name change discon, like CMB or IOCB
                    // or into fluid if S
                    break;
                }
            } else {
                // TRANSUP
                if (transPhase.endSegment().endBranch <= legNameChangeDiscon
                        || !(isPWave ||  tMod.isFluidBranch(transPhase.endSegment().getEndBranch()+1))
                ) {
                    // don't go past a leg name change discon, like CMB or IOCB
                    // or into fluid if S
                    break;
                }
            }
        }
        if (prevTransPhase != proto ) {
            out.add(prevTransPhase);
        }
        if (transPhase!=null && transPhase.isSuccessful()) {
            out.add(transPhase);
        }
        if (prevTransPhase.isSuccessful()) {
            if (transAction==TRANSUP) {
                if (!excludeBranch.contains(transPhase.endSegment().endBranch)) {
                    out.add(prevTransPhase.nextSegment(isPWave, REFLECT_UNDERSIDE));
                }
            } else {
                if (!excludeBranch.contains(transPhase.endSegment().endBranch+1)) {
                    out.add(prevTransPhase.nextSegment(isPWave, REFLECT_TOPSIDE));
                }
            }
        }
        for (ProtoSeismicPhase p : out) {
            for (SeismicPhaseSegment seg : p.segmentList) {
                String start = seismicNamingLayers.legNameForTauBranch(seg.startBranch, seg.isPWave, seg.layerPropogationType, seg.endAction);
                String end = seismicNamingLayers.legNameForTauBranch(seg.endBranch, seg.isPWave, seg.layerPropogationType, seg.endAction);
                assert start.equals(end)
                        : p.getPuristName()+" "+p.branchNumSeqStrWithSegBreaks()+" but "+start+" == "+end;
            }
        }
        return out;
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

            List<ProtoSeismicPhase> cleaned = cleanDuplicates(outTree);
            if (outTree.size() != cleaned.size()) {
                outTree.clear();
                outTree.addAll(cleaned);
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
                    && prev.layerPropogationType == seg.layerPropogationType
                    && Objects.equals(prev.legName.charAt(0), seg.legName.charAt(0))) {
                SeismicPhaseSegment conSeg = new SeismicPhaseSegment(prev.tMod,
                        prev.startBranch, seg.endBranch, prev.isPWave, seg.endAction, prev.layerPropogationType,
                        seg.legName,
                        Math.max(prev.minRayParam, seg.minRayParam),
                        Math.min(prev.maxRayParam, seg.maxRayParam),
                        prev.prevEndAction);
                out.remove(prev);
                out.add(conSeg);
                assert conSeg.endAction == seg.endAction : ("consolidated end not same as seg end: "+conSeg.endAction+" "+seg.endAction);

                prev = conSeg;
            } else {
                out.add(seg);
                prev = seg;
            }
        }
        ProtoSeismicPhase conProto = new ProtoSeismicPhase(out, proto.receiverDepth);
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

    private final SeismicNamingLayers seismicNamingLayers;

}
