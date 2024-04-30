package edu.sc.seis.TauP;

import java.util.*;

import static edu.sc.seis.TauP.PhaseInteraction.*;

public class SeismicPhaseWalk {


    public SeismicPhaseWalk(TauModel tMod) throws TauModelException {
        this(tMod, null, null, 0);
    }

    public SeismicPhaseWalk(TauModel tMod, Double minRayParam, Double maxRayParam, double receiverDepth) throws TauModelException {
        this.tMod = tMod;
        this.minRayParam = minRayParam;
        this.maxRayParam = maxRayParam;
        this.receiverBranch = tMod.findBranch(receiverDepth);
    }

    public void excludeBoundaries(List<Double> layerDepths) throws TauModelException {
        double tol = 10;
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
                System.err.println("Unable to find discontinuity within "+tol+" km of "+d+" in "+tMod.getModelName()+", ignoring.");
            }
        }
    }


    /**
     * Temporary assume receiver is at surface.
     */
    public int receiverBranch;
    Double minRayParam;
    Double maxRayParam;
    TauModel tMod;

    List<Integer> excludeBranch = new ArrayList<>();


    public List<ProtoSeismicPhase> findEndingPaths(int maxAction) {
        List<ProtoSeismicPhase> segmentTree = walkPhases(maxAction);

        List<ProtoSeismicPhase> endingSegments = new ArrayList<>();
        for (ProtoSeismicPhase segList : segmentTree) {
            SeismicPhaseSegment endSeg = segList.get(segList.size()-1);
            if (endSeg.endAction == END || endSeg.endAction == END_DOWN) {
                endingSegments.add(consolidateSegment(segList));
            } else {
                // System.err.println("seg does not end: "+phaseNameForSegments(segList));
            }
        }
        endingSegments.sort(Comparator.comparingInt(s -> s.size()));
        endingSegments = cleanDuplicates(endingSegments);
        return endingSegments;
    }

    public List<ProtoSeismicPhase> walkPhases(int maxAction) {

        List<ProtoSeismicPhase> segmentTree = new ArrayList<>();
        if (allowSWave) {
            segmentTree.addAll( createSourceSegments(tMod, SimpleSeismicPhase.SWAVE));
        }
        if (allowPWave) {
            segmentTree.addAll( createSourceSegments(tMod, SimpleSeismicPhase.PWAVE));
        }
        segmentTree = overlapsRayParam(segmentTree, minRayParam, maxRayParam);
        segmentTree = walkPhases(tMod, segmentTree, maxAction);
        return segmentTree;
    }

    public List<ProtoSeismicPhase> createSourceSegments(TauModel tMod, boolean isPWave) {
        List<ProtoSeismicPhase> segmentTree =  new ArrayList<>();
        if (tMod.getSourceBranch() > 0) {
            int aboveStartBranch = tMod.getSourceBranch()-1;
            TauBranch aboveSourceBranchP = tMod.getTauBranch(tMod.getSourceBranch()-1, isPWave);
            TauBranch aboveSourceBranchS = tMod.getTauBranch(tMod.getSourceBranch()-1, SimpleSeismicPhase.SWAVE);
            SeismicPhaseSegment upSeg;
            if (receiverBranch == tMod.sourceBranch-1) {
                // one branch away from receiver, so can just go direct and END
                ProtoSeismicPhase upProto = ProtoSeismicPhase.start( new SeismicPhaseSegment(tMod,
                        aboveStartBranch, aboveStartBranch,
                        isPWave, END, false,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, false),
                        0, aboveSourceBranchP.getMinTurnRayParam()));
                segmentTree.add(upProto);
            }
            if ( ! excludeBranch.contains(aboveStartBranch) ) {
                ProtoSeismicPhase reflProto = ProtoSeismicPhase.start(new SeismicPhaseSegment(tMod,
                        aboveStartBranch, aboveStartBranch,
                        isPWave, REFLECT_UNDERSIDE, false,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, false),
                        0, aboveSourceBranchP.getMinTurnRayParam()));
                segmentTree.add(reflProto);
            }
            if (tMod.getSourceBranch() > 1) {
                ProtoSeismicPhase upProto = ProtoSeismicPhase.start(new SeismicPhaseSegment(tMod,
                        aboveStartBranch, aboveStartBranch,
                        isPWave, TRANSUP, false,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, false),
                        0, aboveSourceBranchP.getMinTurnRayParam()));
                segmentTree.add(upProto);
            }
        }

        int startBranch = tMod.getSourceBranch();

        TauBranch sourceBranchP = tMod.getTauBranch(tMod.getSourceBranch(), isPWave);

        // downgoing options are END, TURN, REFLECT_TOPSIDE or TRANSDOWN
        ProtoSeismicPhase turnProto = ProtoSeismicPhase.start( new SeismicPhaseSegment(tMod,
                startBranch, startBranch,
                isPWave, TURN, true,
                legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, true),
                sourceBranchP.getMinRayParam(),
                sourceBranchP.getMaxRayParam()));
        segmentTree.add(turnProto);
        if (receiverBranch == startBranch+1) {
            ProtoSeismicPhase endProto = ProtoSeismicPhase.start( new SeismicPhaseSegment(tMod,
                    startBranch, startBranch,
                    isPWave, END, true,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, true),
                    sourceBranchP.getMinRayParam(),
                    sourceBranchP.getMaxRayParam()));
            segmentTree.add(endProto);
        }
        if (tMod.getSourceBranch() < tMod.getNumBranches() - 1) {
            if ( ! excludeBranch.contains(startBranch+1) ) {
                ProtoSeismicPhase reflProto = ProtoSeismicPhase.start(new SeismicPhaseSegment(tMod,
                        startBranch, startBranch,
                        isPWave, REFLECT_TOPSIDE, true,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, true),
                        0, sourceBranchP.getMinTurnRayParam()));
                segmentTree.add(reflProto);
            }
            ProtoSeismicPhase transDProto = ProtoSeismicPhase.start( new SeismicPhaseSegment(tMod,
                    startBranch, startBranch,
                    isPWave, TRANSDOWN, true,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, true),
                    0, sourceBranchP.getMinRayParam()));
            segmentTree.add(transDProto);
        }
        return segmentTree;
    }

    public List<ProtoSeismicPhase> cleanDuplicates(List<ProtoSeismicPhase> in) {
        List<ProtoSeismicPhase> out = new ArrayList<>();
        List<ProtoSeismicPhase> sameSize = new ArrayList<>();

        int currSize = in.get(0).size();
        for (int i = 0; i < in.size(); i++) {
            ProtoSeismicPhase next = in.get(i);
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
        if (!sameSize.isEmpty()) {
            out.addAll(sameSize);
        }
        return out;
    }

    public boolean canMergePhases(ProtoSeismicPhase curr, ProtoSeismicPhase other) {
        if (curr.size() != other.size()) {
            return false;
        }
        SeismicPhaseSegment pS = null;
        for (int s = 0; s < curr.size(); s++) {
            SeismicPhaseSegment cS = curr.get(s);
            SeismicPhaseSegment oS = other.get(s);
            if (cS.isPWave != oS.isPWave
                    || cS.isDownGoing != oS.isDownGoing
                    || cS.endAction != oS.endAction
                    || cS.legName != oS.legName) {
                return false;
            }
            if (cS.isDownGoing) {
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
                    SeismicPhaseSegment m = new SeismicPhaseSegment(cS.tMod, cS.startBranch, oS.endBranch, cS.isPWave, cS.endAction, cS.isDownGoing, cS.legName, oS.minRayParam, cS.maxRayParam);
                    out.add(m);
                } else if (cS.endBranch > oS.endBranch) {
                    SeismicPhaseSegment m = new SeismicPhaseSegment(cS.tMod, cS.startBranch, oS.endBranch, cS.isPWave, cS.endAction, cS.isDownGoing, cS.legName, cS.minRayParam, oS.maxRayParam);
                    out.add(m);
                }
            } else if (prevS != null && prevS.endAction == TURN) {
                if (cS.startBranch == oS.startBranch) {
                    out.add(cS);
                } else if (cS.startBranch < oS.startBranch) {
                    SeismicPhaseSegment m = new SeismicPhaseSegment(cS.tMod, oS.startBranch, oS.endBranch, cS.isPWave, cS.endAction, cS.isDownGoing, cS.legName, oS.minRayParam, cS.maxRayParam);
                    out.add(m);
                } else if (cS.startBranch > oS.startBranch) {
                    SeismicPhaseSegment m = new SeismicPhaseSegment(cS.tMod, oS.startBranch, oS.endBranch, cS.isPWave, cS.endAction, cS.isDownGoing, cS.legName, cS.minRayParam, oS.maxRayParam);
                    out.add(m);
                }
            } else {
                out.add(cS);
            }
            prevS = cS;
        }
        return new ProtoSeismicPhase(out);
    }

    public List<ProtoSeismicPhase> walkPhases(TauModel tMod, List<ProtoSeismicPhase> segmentTree, int maxLegs) {
        List<ProtoSeismicPhase> nextSegmentTree = new ArrayList<>();
        boolean walkedAStep = false;
        for (ProtoSeismicPhase segList : segmentTree) {
            SeismicPhaseSegment endSeg = segList.get(segList.size() - 1);
            if (endSeg.endAction == END || endSeg.endAction == END_DOWN) {
                nextSegmentTree.add(segList);
            } else {
                int interactionNum = segList.calcInteractionNumber();
                if (interactionNum <= maxLegs) {
                    segList.validateSegList();
                    List<ProtoSeismicPhase> calcedNext = new ArrayList<>();
                    if (allowPWave) {
                        calcedNext.addAll(nextLegs(tMod, segList, SimpleSeismicPhase.PWAVE));
                    }
                    if (allowSWave) {
                        calcedNext.addAll(nextLegs(tMod, segList, SimpleSeismicPhase.SWAVE));
                    }
                    for (ProtoSeismicPhase calcSegList : calcedNext) {
                        SeismicPhaseSegment calcendSeg = calcSegList.get(calcSegList.size()-1);
                        if (calcSegList.calcInteractionNumber() <= maxLegs
                                && calcendSeg.minRayParam < calcendSeg.maxRayParam
                        ) {
                            ProtoSeismicPhase conProto = consolidateSegment(calcSegList);
                            nextSegmentTree.add(conProto);
                            walkedAStep = true;
                        } else {
                            if (ToolRun.VERBOSE) {
                                ProtoSeismicPhase conProto = consolidateSegment(calcSegList);
                                System.out.println("skip " + conProto.phaseNameForSegments()
                                        + " " + (calcSegList.calcInteractionNumber() <= maxLegs)
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
            nextSegmentTree = walkPhases(tMod, nextSegmentTree, maxLegs);
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


    public List<ProtoSeismicPhase> nextLegs(TauModel tMod, ProtoSeismicPhase segmentList, boolean isPWave) {
        List<ProtoSeismicPhase> outTree = new ArrayList<>();
        SeismicPhaseSegment prevEndSeg = segmentList.get(segmentList.size()-1);
        TauBranch prevEndBranch = tMod.getTauBranch(prevEndSeg.endBranch, prevEndSeg.isPWave);
        int startBranchNum;
        TauBranch startBranch;

        if (isPWave != prevEndSeg.isPWave) {
            if (prevEndSeg.isDownGoing && excludeBranch.contains(prevEndSeg.endBranch + 1)) {
                return outTree;
            }
            if ( ! prevEndSeg.isDownGoing && excludeBranch.contains(prevEndSeg.endBranch)) {
                return outTree;
            }
        }

        switch (prevEndSeg.endAction) {
            case TRANSUP:
                startBranchNum = prevEndSeg.endBranch-1;
                break;
            case TRANSDOWN:
                startBranchNum = prevEndSeg.endBranch+1;
                break;
            default:
                startBranchNum = prevEndSeg.endBranch;
        }
        if (isPWave == SimpleSeismicPhase.SWAVE
                && tMod.getSlownessModel().depthInFluid(tMod.getTauBranch(startBranchNum, true).getTopDepth())) {
            // no s wave in fluid layer
            return outTree;
        }

        switch (prevEndSeg.endAction) {
            case TRANSUP:
                if (receiverBranch == prevEndSeg.endBranch-1) {
                    outTree.add(segmentList.nextSegment(isPWave, END));
                }
                break;
            case TURN:
                if (receiverBranch == prevEndSeg.endBranch && isPWave == prevEndSeg.isPWave) {
                    // turn cannot phase convert
                    outTree.add(segmentList.nextSegment( prevEndSeg.isPWave, END));
                }
                break;
            case REFLECT_TOPSIDE:
                if (receiverBranch == prevEndSeg.endBranch ) {
                    outTree.add(segmentList.nextSegment( isPWave, END));
                }
                break;
            case REFLECT_UNDERSIDE:
                if (receiverBranch == prevEndSeg.endBranch+1) {
                    outTree.add(segmentList.nextSegment( isPWave, END));
                }
                break;
            case TRANSDOWN:
                if (receiverBranch == prevEndSeg.endBranch+2) {
                    outTree.add(segmentList.nextSegment( isPWave, END));
                }
                break;
        }
        switch (prevEndSeg.endAction) {
            case END:
            case FAIL:
                outTree.add(segmentList);
                break;
            case REFLECT_UNDERSIDE:
            case TRANSDOWN:
                outTree.add(segmentList.nextSegment(isPWave, TURN));
                if (prevEndSeg.endBranch < tMod.getNumBranches()-2) {
                    outTree.add(segmentList.nextSegment(isPWave, TRANSDOWN));
                    if ( ! excludeBranch.contains(startBranchNum+1) ) {
                        outTree.add(segmentList.nextSegment(isPWave, REFLECT_TOPSIDE));
                    }
                }
                break;
            case REFLECT_TOPSIDE:
            case TRANSUP:
                    if (prevEndSeg.endBranch > 1) {
                            // exclude phase converstion
                            outTree.add(segmentList.nextSegment(isPWave, TRANSUP));
                    }
                if ( ! excludeBranch.contains(startBranchNum)) {
                    outTree.add(segmentList.nextSegment(isPWave, REFLECT_UNDERSIDE));

                }
                break;
            case TURN:
                if (isPWave == prevEndSeg.isPWave) {
                    // turn cannot phase convert
                    if (prevEndSeg.endBranch > 0) {
                            // exclude phase converstion
                            outTree.add(segmentList.nextSegment(prevEndSeg.isPWave, TRANSUP));

                    }
                    if ( ! excludeBranch.contains(startBranchNum) ) {
                        outTree.add(segmentList.nextSegment(prevEndSeg.isPWave, REFLECT_UNDERSIDE));
                    }
                }
                break;
        }
        return outTree;
    }


    public static String legNameForTauBranch(TauModel tMod, int branchNum, boolean isPWave, boolean isDowngoing) {
        TauBranch tauBranch = tMod.getTauBranch(branchNum, isPWave);
        if (branchNum >= tMod.getIocbBranch()) {
            if (tauBranch.isPWave) {
                if (isDowngoing) {
                    return "Ied";
                } else {
                    return "y";
                }
            } else {
                if (isDowngoing) {
                    return "Jed";
                } else {
                    return "j";
                }
            }
        }
        if (branchNum >= tMod.getCmbBranch()) {
            if (tauBranch.isPWave) {
                if (isDowngoing) {
                    return "Ked";
                } else {
                    return "k";
                }
            } else {
                throw new IllegalArgumentException("Cannot have S wave in outer core");
            }
        }
        if (tauBranch.isPWave) {
            if (isDowngoing) {
                return "Ped";
            } else {
                return "p";
            }
        } else {
            if (isDowngoing) {
                return "Sed";
            } else {
                return "s";
            }
        }
    }

    public ProtoSeismicPhase consolidateSegment(ProtoSeismicPhase segmentList) {
        segmentList.validateSegList();

        ProtoSeismicPhase outSegmentList = consolidateTrans(segmentList);
        outSegmentList.validateSegList();
        return outSegmentList;
        //return segmentList;
    }

    public ProtoSeismicPhase consolidateTrans(ProtoSeismicPhase proto) {
        List<SeismicPhaseSegment> out = new ArrayList<>();
        SeismicPhaseSegment prev = null;
        for (SeismicPhaseSegment seg : proto.segmentList) {
            if (prev != null
                    && (prev.endAction == TRANSDOWN || prev.endAction == TRANSUP)
                    && prev.isPWave == seg.isPWave
                    && Objects.equals(prev.legName, seg.legName)) {
                SeismicPhaseSegment conSeg = new SeismicPhaseSegment(prev.tMod,
                        prev.startBranch, seg.endBranch, prev.isPWave, seg.endAction, prev.isDownGoing,
                        prev.legName,
                        Math.max(prev.minRayParam, seg.minRayParam),
                        Math.min(prev.maxRayParam, seg.maxRayParam));
                out.remove(prev);
                out.add(conSeg);
                prev = conSeg;
            } else {
                out.add(seg);
                prev = seg;
            }
        }
        ProtoSeismicPhase conProto = new ProtoSeismicPhase(out);
        conProto.validateSegList();
        return conProto;
    }



    boolean allowSWave = true;
    boolean allowPWave = true;
    
}
