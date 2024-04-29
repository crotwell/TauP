package edu.sc.seis.TauP;

import java.util.*;

import static edu.sc.seis.TauP.PhaseInteraction.*;

public class SeismicPhaseWalk {

    /**
     * Temporary assume receiver is at surface.
     */
    public static final int receiverBranch = 0;
    public List<List<SeismicPhaseSegment>> walkPhases(TauModel tMod, int maxLegs) {

        List<List<SeismicPhaseSegment>> segmentTree = new ArrayList<>();
        if (allowSWave) {
            segmentTree.addAll( createSourceSegments(tMod, SimpleSeismicPhase.SWAVE));
        }
        if (allowPWave) {
            segmentTree.addAll( createSourceSegments(tMod, SimpleSeismicPhase.PWAVE));
        }
        segmentTree = walkPhases(tMod, segmentTree, maxLegs);
        List<List<SeismicPhaseSegment>> endingSegments = new ArrayList<>();
        for (List<SeismicPhaseSegment> segList : segmentTree) {
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

    public List<List<SeismicPhaseSegment>> createSourceSegments(TauModel tMod, boolean isPWave) {
        List<List<SeismicPhaseSegment>> segmentTree =  new ArrayList<>();
        if (tMod.getSourceBranch() > 0) {
            List<SeismicPhaseSegment> upSegList = new ArrayList<>();
            int aboveStartBranch = tMod.getSourceBranch()-1;
            TauBranch aboveSourceBranchP = tMod.getTauBranch(tMod.getSourceBranch()-1, SimpleSeismicPhase.PWAVE);
            TauBranch aboveSourceBranchS = tMod.getTauBranch(tMod.getSourceBranch()-1, SimpleSeismicPhase.SWAVE);
            SeismicPhaseSegment upSeg;
            if (receiverBranch == tMod.sourceBranch-1) {
                // one branch away from receiver, so can just go direct and END
                upSeg = new SeismicPhaseSegment(tMod,
                        aboveStartBranch, aboveStartBranch,
                        isPWave, END, false,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, false),
                        0, aboveSourceBranchP.getMinTurnRayParam());
                upSegList.add(upSeg);
                segmentTree.add(upSegList);
                upSegList = new ArrayList<>();
            }
            upSeg = new SeismicPhaseSegment(tMod,
                    aboveStartBranch, aboveStartBranch,
                    isPWave, REFLECT_UNDERSIDE, false,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, false),
                    0, aboveSourceBranchP.getMinTurnRayParam());
            upSegList.add(upSeg);
            segmentTree.add(upSegList);
            upSegList = new ArrayList<>();
            if (tMod.getSourceBranch() > 1) {
                upSeg = new SeismicPhaseSegment(tMod,
                        aboveStartBranch, aboveStartBranch,
                        isPWave, TRANSUP, false,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), isPWave, false),
                        0, aboveSourceBranchP.getMinTurnRayParam());
                upSegList.add(upSeg);
                segmentTree.add(upSegList);
            }
        }

        List<SeismicPhaseSegment> downSegList = new ArrayList<>();
        int startBranch = tMod.getSourceBranch();

        TauBranch sourceBranchP = tMod.getTauBranch(tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE);
        TauBranch sourceBranchS = tMod.getTauBranch(tMod.getSourceBranch(), SimpleSeismicPhase.SWAVE);

        // downgoing options are END, TURN, REFLECT_TOPSIDE or TRANSDOWN
        SeismicPhaseSegment downSeg = new SeismicPhaseSegment(tMod,
                startBranch, startBranch,
                SimpleSeismicPhase.PWAVE, TURN, true,
                legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE, true),
                sourceBranchP.getMinRayParam(),
                sourceBranchP.getMaxRayParam());
        downSegList.add(downSeg);
        segmentTree.add(downSegList);
        downSegList = new ArrayList<>();
        downSeg = new SeismicPhaseSegment(tMod,
                startBranch, startBranch,
                SimpleSeismicPhase.SWAVE, TURN, true,
                legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.SWAVE, true),
                sourceBranchS.getMinRayParam(),
                sourceBranchS.getMaxRayParam());
        downSegList.add(downSeg);
        segmentTree.add(downSegList);
        if (receiverBranch == startBranch+1) {
            downSegList = new ArrayList<>();
            downSeg = new SeismicPhaseSegment(tMod,
                    startBranch, startBranch,
                    SimpleSeismicPhase.PWAVE, END, true,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE, true),
                    sourceBranchP.getMinRayParam(),
                    sourceBranchP.getMaxRayParam());
            downSegList.add(downSeg);
            segmentTree.add(downSegList);
            downSegList = new ArrayList<>();
            downSeg = new SeismicPhaseSegment(tMod,
                    startBranch, startBranch,
                    SimpleSeismicPhase.SWAVE, END, true,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.SWAVE, true),
                    sourceBranchS.getMinRayParam(),
                    sourceBranchS.getMaxRayParam());
            downSegList.add(downSeg);
            segmentTree.add(downSegList);
        }
        if (tMod.getSourceBranch() < tMod.getNumBranches() - 1) {
            downSegList = new ArrayList<>();
            downSeg = new SeismicPhaseSegment(tMod,
                    startBranch, startBranch,
                    SimpleSeismicPhase.PWAVE, REFLECT_TOPSIDE, true,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE, true),
                    0, sourceBranchP.getMinTurnRayParam());
            downSegList.add(downSeg);
            segmentTree.add(downSegList);
            downSegList = new ArrayList<>();
            downSeg = new SeismicPhaseSegment(tMod,
                    startBranch, startBranch,
                    SimpleSeismicPhase.SWAVE, REFLECT_TOPSIDE, true,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.SWAVE, true),
                    0, sourceBranchS.getMinTurnRayParam());
            downSegList.add(downSeg);
            segmentTree.add(downSegList);
            downSegList = new ArrayList<>();
            downSeg = new SeismicPhaseSegment(tMod,
                    startBranch, startBranch,
                    SimpleSeismicPhase.PWAVE, TRANSDOWN, true,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE, true),
                    0, sourceBranchP.getMinRayParam());
            downSegList.add(downSeg);
            segmentTree.add(downSegList);
            downSegList = new ArrayList<>();
            downSeg = new SeismicPhaseSegment(tMod,
                    startBranch, startBranch,
                    SimpleSeismicPhase.SWAVE, TRANSDOWN, true,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.SWAVE, true),
                    0, sourceBranchS.getMinRayParam());
            downSegList.add(downSeg);
            segmentTree.add(downSegList);
        }
        return segmentTree;
    }

    public List<List<SeismicPhaseSegment>> cleanDuplicates(List<List<SeismicPhaseSegment>> in) {
        List<List<SeismicPhaseSegment>> out = new ArrayList<>();
        List<List<SeismicPhaseSegment>> sameSize = new ArrayList<>();

        int currSize = in.get(0).size();
        for (int i = 0; i < in.size(); i++) {
            List<SeismicPhaseSegment> next = in.get(i);
            if (currSize == next.size()) {
                List<List<SeismicPhaseSegment>> merged = new ArrayList<>();
                for (List<SeismicPhaseSegment> p : sameSize) {
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

    public boolean canMergePhases(List<SeismicPhaseSegment> curr, List<SeismicPhaseSegment> other) {
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
    public List<SeismicPhaseSegment> mergePhases(List<SeismicPhaseSegment> curr, List<SeismicPhaseSegment> other) {
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
        return out;
    }

    public List<List<SeismicPhaseSegment>> walkPhases(TauModel tMod, List<List<SeismicPhaseSegment>> segmentTree, int maxLegs) {
        List<List<SeismicPhaseSegment>> nextSegmentTree = new ArrayList<>();
        boolean walkedAStep = false;
        for (List<SeismicPhaseSegment> segList : segmentTree) {
            SeismicPhaseSegment endSeg = segList.get(segList.size() - 1);
            if (endSeg.endAction == END || endSeg.endAction == END_DOWN) {
                nextSegmentTree.add(segList);
            } else {
                int interactionNum = calcInteractionNumber(segList);
                if (interactionNum <= maxLegs) {
                    validateSegList(segList);
                    List<List<SeismicPhaseSegment>> calcedNext = new ArrayList<>();
                    if (allowPWave) {
                        calcedNext.addAll(nextLegs(tMod, segList, SimpleSeismicPhase.PWAVE));
                    }
                    if (allowSWave) {
                        calcedNext.addAll(nextLegs(tMod, segList, SimpleSeismicPhase.SWAVE));
                    }
                    for (List<SeismicPhaseSegment> calcSegList : calcedNext) {
                        SeismicPhaseSegment calcendSeg = calcSegList.get(calcSegList.size()-1);
                        if (calcInteractionNumber(calcSegList) <= maxLegs
                                && calcendSeg.minRayParam < calcendSeg.maxRayParam
                        ) {
                            nextSegmentTree.add(consolidateSegment(calcSegList));
                            walkedAStep = true;
                        } else {
                            if (ToolRun.VERBOSE) {
                                System.out.println("skip " + phaseNameForSegments(consolidateSegment(calcSegList))
                                        + " " + (calcInteractionNumber(calcSegList) <= maxLegs)
                                        + " " + (calcendSeg.minRayParam < calcendSeg.maxRayParam)
                                );
                            }
                        }
                    }
                }
            }
        }
        int endCount = 0;
        for (List<SeismicPhaseSegment> segList : nextSegmentTree) {
            SeismicPhaseSegment endSeg = segList.get(segList.size()-1);
            if (endSeg.endAction == END || endSeg.endAction == END_DOWN) {
                endCount++;
            }
        }
        if (walkedAStep ) {
            nextSegmentTree = walkPhases(tMod, nextSegmentTree, maxLegs);
        }
        return nextSegmentTree;
    }

    public List<List<SeismicPhaseSegment>> overlapsRayParam(List<List<SeismicPhaseSegment>> segTree, double minRayParam, double maxRayParam) {
        List<List<SeismicPhaseSegment>> out = new ArrayList<>();
        for (List<SeismicPhaseSegment> segList : segTree) {
            SeismicPhaseSegment endSeg = segList.get(segList.size()-1);
            if (endSeg.maxRayParam >= minRayParam && endSeg.minRayParam <= maxRayParam) {
                out.add(segList);
            }
        }
        return out;
    }

    public String phaseNameForSegments(List<SeismicPhaseSegment> segList) {
        return phaseNameForSegments(segList, true);
    }
    public String phaseNameForSegments(List<SeismicPhaseSegment> segList, boolean zapED) {
        String name = "";
        TauModel tMod = segList.get(0).tMod;
        int idx = 0;
        SeismicPhaseSegment prev;
        SeismicPhaseSegment seg = null;
        SeismicPhaseSegment next = segList.get(0);
        while (idx < segList.size()) {
            prev = seg;
            seg = next;
            if (idx < segList.size()-1) {
                next = segList.get(idx+1);
            }
            double botDepth = tMod.getTauBranch(seg.endBranch, seg.isPWave).getBotDepth();
            double topDepth = tMod.getTauBranch(seg.endBranch, seg.isPWave).getTopDepth();
            //name += " "+seg.startBranch+","+seg.endBranch+" ";
            if ( prev == null || prev.endAction != TURN
                    || (! prev.legName.equalsIgnoreCase(seg.legName) && (prev.legName.equals("I") && seg.legName.equals("y")))) {
                String legName = legNameForSegment(tMod, seg);
                if (zapED) {
                    if (seg.endAction == TRANSDOWN && legName.endsWith("ed")
                            && seg.isPWave == next.isPWave
                            && !legName.startsWith(next.legName.substring(0, 1))) {
                        legName = legName.substring(0, 1);
                    } else if (seg.endAction == REFLECT_TOPSIDE
                            && (botDepth == tMod.cmbDepth || botDepth == tMod.iocbDepth)) {
                        legName = legName.substring(0, 1);
                    }
                }
                name += legName;
            } else {
                //name += "("+seg.legName+")";
            }
            switch (seg.endAction) {
                case REFLECT_TOPSIDE:
                    if (botDepth == tMod.cmbDepth) {
                        name += "c";
                    } else if (botDepth == tMod.iocbDepth) {
                        name += "i";
                    } else {
                        name += "v" + (int) (botDepth);
                    }
                    break;
                case REFLECT_TOPSIDE_CRITICAL:
                    name += "V";
                    if (botDepth == tMod.cmbDepth) {
                        name += "c";
                    } else if (botDepth == tMod.iocbDepth) {
                        name += "i";
                    } else {
                        name += (int) (botDepth);
                    }
                    break;
                case REFLECT_UNDERSIDE:
                    if (topDepth == 0 || topDepth == tMod.cmbDepth || topDepth == tMod.iocbDepth) {
                        // no char as PP or KK or II
                    } else {
                        name += "^" + (int) (topDepth);
                    }
                    break;
                case TURN:
                    //name += "U";
                    break;
                case TRANSDOWN:
                    if (botDepth == tMod.cmbDepth || botDepth == tMod.iocbDepth) {
                        // no char as P,S -> K -> I,J
                    } else {
                        name += (int)(botDepth);
                    }
                    break;
                case TRANSUP:
                    if (topDepth == tMod.cmbDepth || topDepth == tMod.iocbDepth) {
                        // no char as P,S -> K -> I,J
                    } else {
                        name += (int) (topDepth);
                    }
                    break;
                case END:
                case END_DOWN:
                    break;
                default:
                    name += seg.endAction.name();
            }
            idx++;
        }
        return name;
    }

    public String branchNumSeq(List<SeismicPhaseSegment> segmentList) {
        String out = "";
        for (SeismicPhaseSegment seg : segmentList) {
            out += seg.startBranch;
            if (seg.endBranch != seg.startBranch) {
                out += seg.endBranch;
            }
        }
        return out;
    }

    public List<List<SeismicPhaseSegment>> nextLegs(TauModel tMod, List<SeismicPhaseSegment> segmentList, boolean isPWave) {
        List<List<SeismicPhaseSegment>> outTree = new ArrayList<>();
        SeismicPhaseSegment prevEndSeg = segmentList.get(segmentList.size()-1);
        TauBranch prevEndBranch = tMod.getTauBranch(prevEndSeg.endBranch, prevEndSeg.isPWave);
        int startBranchNum;
        TauBranch startBranch;

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
                    outTree.add(nextSegment(segmentList, isPWave, END));
                }
                break;
            case TURN:
                if (receiverBranch == prevEndSeg.endBranch && isPWave == prevEndSeg.isPWave) {
                    // turn cannot phase convert
                    outTree.add(nextSegment(segmentList, prevEndSeg.isPWave, END));
                }
                break;
            case REFLECT_TOPSIDE:
                if (receiverBranch == prevEndSeg.endBranch) {
                    outTree.add(nextSegment(segmentList, isPWave, END));
                }
                break;
            case REFLECT_UNDERSIDE:
                if (receiverBranch == prevEndSeg.endBranch+1) {
                    outTree.add(nextSegment(segmentList, isPWave, END));
                }
                break;
            case TRANSDOWN:
                if (receiverBranch == prevEndSeg.endBranch+2) {
                    outTree.add(nextSegment(segmentList, isPWave, END));
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
                outTree.add(nextSegment(segmentList, isPWave, TURN));
                if (prevEndSeg.endBranch < tMod.getNumBranches()-2) {
                    outTree.add(nextSegment(segmentList, isPWave, TRANSDOWN));
                    outTree.add(nextSegment(segmentList, isPWave, REFLECT_TOPSIDE));
                }
                break;
            case REFLECT_TOPSIDE:
            case TRANSUP:
                if (prevEndSeg.endBranch > 1) {
                    outTree.add(nextSegment(segmentList, isPWave, TRANSUP));
                }
                outTree.add(nextSegment(segmentList, isPWave, REFLECT_UNDERSIDE));
                break;
            case TURN:
                if (isPWave == prevEndSeg.isPWave) {
                    // turn cannot phase convert
                    if (prevEndSeg.endBranch > 0) {
                        outTree.add(nextSegment(segmentList, prevEndSeg.isPWave, TRANSUP));
                    }
                    outTree.add(nextSegment(segmentList, prevEndSeg.isPWave, REFLECT_UNDERSIDE));
                }
                break;
        }
        return outTree;
    }

    public List<SeismicPhaseSegment> nextSegment(List<SeismicPhaseSegment> segmentList,
                                                 boolean isPWave,
                                                 PhaseInteraction endAction) {
        SeismicPhaseSegment endSeg = segmentList.get(segmentList.size()-1);
        List<SeismicPhaseSegment> out = new ArrayList<>(segmentList);
        TauModel tMod = endSeg.getTauModel();
        int priorEndBranchNum = endSeg.endBranch;
        boolean isDowngoing;
        switch (endAction) {
            case TRANSUP:
            case REFLECT_UNDERSIDE:
            case REFLECT_UNDERSIDE_CRITICAL:
            case END:
                isDowngoing = false;
                break;
            case TURN:
            case TRANSDOWN:
            case REFLECT_TOPSIDE:
            case REFLECT_TOPSIDE_CRITICAL:
            case END_DOWN:
                isDowngoing = true;
                break;
            case FAIL:
            case START:
                throw new IllegalArgumentException("End action cannot be FAIL or START: "+endAction);
            default:
                throw new IllegalArgumentException("End action case not yet impl: "+endAction);
        }
        int startBranchNum;
        switch (endSeg.endAction) {
            case TRANSUP:
                startBranchNum = priorEndBranchNum-1;
                break;
            case TRANSDOWN:
                startBranchNum = priorEndBranchNum+1;
                break;
            default:
                // some reversal of direction
                startBranchNum = priorEndBranchNum;
        }
        int endBranchNum = startBranchNum;
        if (isDowngoing) {
            while (endBranchNum < tMod.getNumBranches()-1
                    && tMod.isNoDisconDepth(tMod.getTauBranch(endBranchNum, isPWave).getBotDepth())) {
                endBranchNum += 1;
            }
        } else {
            // upgoing
            while (endBranchNum > 0 && tMod.isNoDisconDepth(tMod.getTauBranch(endBranchNum, isPWave).getTopDepth())) {
                endBranchNum -= 1;
            }
        }
        String nextLegName = legNameForTauBranch(tMod, startBranchNum, isPWave, isDowngoing);
        TauBranch nextBranch = tMod.getTauBranch(startBranchNum, isPWave);

        double minRayParam = endSeg.minRayParam;
        double maxRayParam = endSeg.maxRayParam;
        switch (endAction) {
            case REFLECT_TOPSIDE_CRITICAL:
                minRayParam = Math.max(minRayParam, nextBranch.getMinRayParam());
            case TRANSDOWN:
            case REFLECT_TOPSIDE:
            case END_DOWN:
                maxRayParam = Math.min(maxRayParam, nextBranch.getMinTurnRayParam());
                break;

            case TURN:
                minRayParam = Math.max(minRayParam, nextBranch.getMinRayParam());
                maxRayParam = Math.min(maxRayParam, nextBranch.getMaxRayParam());
                break;
            case TRANSUP:
            case REFLECT_UNDERSIDE:
            case REFLECT_UNDERSIDE_CRITICAL:
            case END:
                maxRayParam = Math.min(maxRayParam, nextBranch.getMaxRayParam());
                break;

        }

        out.add(new SeismicPhaseSegment(tMod,
                startBranchNum, endBranchNum, isPWave, endAction, isDowngoing, nextLegName,
                minRayParam, maxRayParam));
        validateSegList(out);
        return out;
    }

    public String legNameForTauBranch(TauModel tMod, int branchNum, boolean isPWave, boolean isDowngoing) {
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

    public String legNameForSegment(TauModel tMod, SeismicPhaseSegment seg) {
        String name = legNameForTauBranch(tMod, seg.endBranch, seg.isPWave, seg.isDownGoing);
        if (seg.endAction == TURN && name.endsWith("ed")) {
            name = name.substring(0, name.length()-2);
        }
        return name;
    }

    public List<SeismicPhaseSegment> consolidateSegment(List<SeismicPhaseSegment> segmentList) {
        validateSegList(segmentList);

        List<SeismicPhaseSegment> outSegmentList = consolidateTrans(segmentList);
        validateSegList(outSegmentList);
        return outSegmentList;
        //return segmentList;
    }

    public double[] minMaxRayParam(List<SeismicPhaseSegment> segmentList) {
        double[] minmax = new double[2];
        for (SeismicPhaseSegment seg : segmentList) {
            minmax[0] = Math.max(minmax[0], seg.minRayParam);
            minmax[1] = Math.min(minmax[1], seg.maxRayParam);
        }
        return minmax;
    }

    public List<SeismicPhaseSegment> consolidateTrans(List<SeismicPhaseSegment> segmentList) {
        List<SeismicPhaseSegment> out = new ArrayList<>();
        SeismicPhaseSegment prev = null;
        for (SeismicPhaseSegment seg : segmentList) {
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
                validateSegList(out);
                prev = conSeg;
            } else {
                out.add(seg);
                prev = seg;
            }
        }
        return out;
    }

    public void validateSegList(List<SeismicPhaseSegment> segmentList) {
        SeismicPhaseSegment prev = null;
        for (SeismicPhaseSegment seg : segmentList) {
            if (seg.maxRayParam == 0) {
                throw new RuntimeException("maxRayParam is zero: "+phaseNameForSegments(segmentList));
            }
            if (seg.endBranch == seg.tMod.getNumBranches()-1 && seg.isDownGoing && seg.endAction != TURN) {
                throw new RuntimeException("down not turn in innermost core layer: "
                        +phaseNameForSegments(segmentList)+" "+seg.endBranch+" "+ seg.tMod.getNumBranches()+" "+seg.endAction);
            }
            if (prev != null) {
                if (prev.endAction == TRANSDOWN && prev.endBranch != seg.startBranch-1) {
                    throw new RuntimeException("prev is TRANSDOWN, but seg is not +1\n"+prev.endAction+"  "+seg.startBranch+"\n"+phaseNameForSegments(segmentList));
                }
                if (prev.endAction == TURN &&
                        ( seg.endAction == TURN || seg.endAction == TRANSDOWN
                                || seg.endAction == END_DOWN || seg.endAction == REFLECT_TOPSIDE)) {
                    throw new RuntimeException("prev is TURN, but seg is "+seg.endAction);
                }
            }
            prev = seg;
        }
    }

    public int calcInteractionNumber(List<SeismicPhaseSegment> segmentList) {
        int count = 0;
        SeismicPhaseSegment prev = null;
        if (segmentList.size()>30) { return 9999999;}
        for (SeismicPhaseSegment seg : segmentList) {
            switch (seg.endAction) {
                case SCATTER:
                case BACKSCATTER:
                case SCATTER_DOWN:
                case BACKSCATTER_DOWN:
                case REFLECT_TOPSIDE:
                case REFLECT_UNDERSIDE:
                case REFLECT_UNDERSIDE_CRITICAL:
                case REFLECT_TOPSIDE_CRITICAL:
                    count++;
                    break;
            }
            if (prev != null && prev.isPWave != seg.isPWave) {
                count++;
            }
            prev = seg;
        }
        return count;
    }

    boolean allowSWave = true;
    boolean allowPWave = true;
}
