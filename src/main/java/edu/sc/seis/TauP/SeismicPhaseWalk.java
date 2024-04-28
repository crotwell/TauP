package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static edu.sc.seis.TauP.PhaseInteraction.*;

public class SeismicPhaseWalk {

    /**
     * Temporary assume receiver is at surface.
     */
    public static final int receiverBranch = 0;
    public List<List<SeismicPhaseSegment>> walkPhases(TauModel tMod, int maxLegs) {
        List<List<SeismicPhaseSegment>> segmentTree = createSourceSegments(tMod);
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
        System.err.println("Found "+endingSegments.size()+" segments < "+maxLegs);
        return endingSegments;
    }
    public List<List<SeismicPhaseSegment>> createSourceSegments(TauModel tMod) {

        List<List<SeismicPhaseSegment>> segmentTree =  new ArrayList<>();
        List<SeismicPhaseSegment> downSegList = new ArrayList<>();
        SeismicPhaseSegment downSeg = new SeismicPhaseSegment(tMod,
                tMod.getSourceBranch(), tMod.getSourceBranch(),
                SimpleSeismicPhase.PWAVE, TURN, true,
                legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE, true),
                0, tMod.getTauBranch(tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE).getMinTurnRayParam());
        downSegList.add(downSeg);
        segmentTree.add(downSegList);
        downSegList = new ArrayList<>();
        downSeg = new SeismicPhaseSegment(tMod,
                tMod.getSourceBranch(), tMod.getSourceBranch(),
                SimpleSeismicPhase.PWAVE, TRANSDOWN, true,
                legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE, true),
                0, tMod.getTauBranch(tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE).getMinRayParam());
        downSegList.add(downSeg);
        segmentTree.add(downSegList);
        return segmentTree;
    }

    public List<List<SeismicPhaseSegment>> createSourceSegmentsAll(TauModel tMod) {
        List<List<SeismicPhaseSegment>> segmentTree =  new ArrayList<>();
        if (tMod.getSourceBranch() > 0) {
            List<SeismicPhaseSegment> upSegList = new ArrayList<>();
            SeismicPhaseSegment upSeg;
            if (receiverBranch == tMod.sourceBranch-1) {
                upSeg = new SeismicPhaseSegment(tMod,
                        tMod.getSourceBranch()-1, tMod.getSourceBranch()-1,
                        SimpleSeismicPhase.PWAVE, END, false,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE, false),
                        0, tMod.getTauBranch(tMod.getSourceBranch()-1, SimpleSeismicPhase.PWAVE).getMinTurnRayParam());
                upSegList.add(upSeg);
                segmentTree.add(upSegList);
                upSegList = new ArrayList<>();
                upSeg = new SeismicPhaseSegment(tMod,
                        tMod.getSourceBranch()-1, tMod.getSourceBranch()-1,
                        SimpleSeismicPhase.SWAVE, END, false,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.SWAVE, false),
                        0, tMod.getTauBranch(tMod.getSourceBranch()-1, SimpleSeismicPhase.SWAVE).getMinTurnRayParam());
                upSegList.add(upSeg);
                segmentTree.add(upSegList);
                upSegList = new ArrayList<>();
            }
            upSeg = new SeismicPhaseSegment(tMod,
                    tMod.getSourceBranch()-1, tMod.getSourceBranch()-1,
                    SimpleSeismicPhase.PWAVE, REFLECT_UNDERSIDE, false,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE, false),
                    0, tMod.getTauBranch(tMod.getSourceBranch()-1, SimpleSeismicPhase.PWAVE).getMinTurnRayParam());
            upSegList.add(upSeg);
            segmentTree.add(upSegList);
            upSegList = new ArrayList<>();
            upSeg = new SeismicPhaseSegment(tMod,
                    tMod.getSourceBranch()-1, tMod.getSourceBranch()-1,
                    SimpleSeismicPhase.SWAVE, REFLECT_UNDERSIDE, false,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.SWAVE, false),
                    0, tMod.getTauBranch(tMod.getSourceBranch()-1, SimpleSeismicPhase.SWAVE).getMinTurnRayParam());
            upSegList.add(upSeg);
            segmentTree.add(upSegList);
            upSegList = new ArrayList<>();
            if (tMod.getSourceBranch() > 1) {
                upSeg = new SeismicPhaseSegment(tMod,
                        tMod.getSourceBranch()-1, tMod.getSourceBranch()-1,
                        SimpleSeismicPhase.PWAVE, TRANSUP, false,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE, false),
                        0, tMod.getTauBranch(tMod.getSourceBranch()-1, SimpleSeismicPhase.PWAVE).getMinTurnRayParam());
                upSegList.add(upSeg);
                segmentTree.add(upSegList);
                upSegList = new ArrayList<>();
                upSeg = new SeismicPhaseSegment(tMod,
                        tMod.getSourceBranch()-1, tMod.getSourceBranch()-1,
                        SimpleSeismicPhase.SWAVE, TRANSUP, false,
                        legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.SWAVE, false),
                        0, tMod.getTauBranch(tMod.getSourceBranch()-1, SimpleSeismicPhase.SWAVE).getMinTurnRayParam());
                upSegList.add(upSeg);
                segmentTree.add(upSegList);
            }
        }

        List<SeismicPhaseSegment> downSegList = new ArrayList<>();
        SeismicPhaseSegment downSeg = new SeismicPhaseSegment(tMod,
                tMod.getSourceBranch(), tMod.getSourceBranch(),
                SimpleSeismicPhase.PWAVE, TURN, true,
                legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE, true),
                0, tMod.getTauBranch(tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE).getMinTurnRayParam());
        downSegList.add(downSeg);
        segmentTree.add(downSegList);
        downSegList = new ArrayList<>();
        downSeg = new SeismicPhaseSegment(tMod,
                tMod.getSourceBranch(), tMod.getSourceBranch(),
                SimpleSeismicPhase.SWAVE, TURN, true,
                legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.SWAVE, true),
                0, tMod.getTauBranch(tMod.getSourceBranch(), SimpleSeismicPhase.SWAVE).getMinTurnRayParam());
        downSegList.add(downSeg);
        segmentTree.add(downSegList);
        if (tMod.getSourceBranch() < tMod.getNumBranches() - 1) {
            downSegList = new ArrayList<>();
            downSeg = new SeismicPhaseSegment(tMod,
                    tMod.getSourceBranch(), tMod.getSourceBranch(),
                    SimpleSeismicPhase.PWAVE, REFLECT_TOPSIDE, true,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE, true),
                    0, tMod.getTauBranch(tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE).getMinRayParam());
            downSegList.add(downSeg);
            segmentTree.add(downSegList);
            downSegList = new ArrayList<>();
            downSeg = new SeismicPhaseSegment(tMod,
                    tMod.getSourceBranch(), tMod.getSourceBranch(),
                    SimpleSeismicPhase.SWAVE, REFLECT_TOPSIDE, true,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.SWAVE, true),
                    0, tMod.getTauBranch(tMod.getSourceBranch(), SimpleSeismicPhase.SWAVE).getMinRayParam());
            downSegList.add(downSeg);
            segmentTree.add(downSegList);
            downSegList = new ArrayList<>();
            downSeg = new SeismicPhaseSegment(tMod,
                    tMod.getSourceBranch(), tMod.getSourceBranch(),
                    SimpleSeismicPhase.PWAVE, TRANSDOWN, true,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE, true),
                    0, tMod.getTauBranch(tMod.getSourceBranch(), SimpleSeismicPhase.PWAVE).getMinRayParam());
            downSegList.add(downSeg);
            segmentTree.add(downSegList);
            downSegList = new ArrayList<>();
            downSeg = new SeismicPhaseSegment(tMod,
                    tMod.getSourceBranch(), tMod.getSourceBranch(),
                    SimpleSeismicPhase.SWAVE, TRANSDOWN, true,
                    legNameForTauBranch(tMod, tMod.getSourceBranch(), SimpleSeismicPhase.SWAVE, true),
                    0, tMod.getTauBranch(tMod.getSourceBranch(), SimpleSeismicPhase.SWAVE).getMinRayParam());
            downSegList.add(downSeg);
            segmentTree.add(downSegList);
        }
        return segmentTree;
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
                if (interactionNum < maxLegs) {
                    validateSegList(segList);
                    List<List<SeismicPhaseSegment>> calced = nextLegs(tMod, segList);
                    if (calced.size() > 0) {
                        walkedAStep = true;
                        for (List<SeismicPhaseSegment> calcSegList : calced) {
                            nextSegmentTree.add(consolidateSegment(calcSegList));
                        }
                    }
                } else {
                    System.err.println("Skip: " + phaseNameForSegments(segList)+" num: "+interactionNum+" > "+maxLegs);
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
            System.err.println("recur walkPhases: "+nextSegmentTree.size());
            nextSegmentTree = walkPhases(tMod, nextSegmentTree, maxLegs);
        }
        return nextSegmentTree;
    }

    public String phaseNameForSegments(List<SeismicPhaseSegment> segList) {
        String name = "";
        TauModel tMod = segList.get(0).tMod;
        SeismicPhaseSegment prev = null;
        int idx = 0;
        for (SeismicPhaseSegment seg : segList) {
            name += " "+seg.startBranch+","+seg.endBranch+" ";
            if ( true || prev == null || prev.endAction != TURN) {
                name += legNameForSegment(tMod, seg);
            }
            switch (seg.endAction) {
                case REFLECT_TOPSIDE:
                    name += "v"+tMod.getTauBranch(seg.endBranch, seg.isPWave).getBotDepth();
                    break;
                case REFLECT_TOPSIDE_CRITICAL:
                    name += "V"+tMod.getTauBranch(seg.endBranch, seg.isPWave).getBotDepth();
                    break;
                case REFLECT_UNDERSIDE:
                    name += "^"+tMod.getTauBranch(seg.endBranch, seg.isPWave).getTopDepth();
                    break;
                case TURN:
                    name += "U";
                    break;
                case TRANSDOWN:
                    name += "d"+seg.endBranch+" ";
                    break;
                case TRANSUP:
                    name += "u"+seg.endBranch+" ";
                    break;
                default:
                    name += seg.endAction.name();
            }
            name += " ";
            prev = seg;
            idx++;
        }
        return name;
    }

    public List<List<SeismicPhaseSegment>> nextLegs(TauModel tMod, List<SeismicPhaseSegment> segmentList) {
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
        boolean nextAllowSWave = ! tMod.getSlownessModel().depthInFluid(tMod.getTauBranch(startBranchNum, true).getTopDepth());
        //nextAllowSWave = false;

        switch (prevEndSeg.endAction) {
            case TRANSUP:
                if (receiverBranch == prevEndSeg.endBranch-1) {
                    outTree.add(nextSegment(segmentList, SimpleSeismicPhase.PWAVE, END));
                    if (nextAllowSWave) {
                        outTree.add(nextSegment(segmentList, SimpleSeismicPhase.SWAVE, END));
                    }
                }
                break;
            case TURN:
            case REFLECT_TOPSIDE:
                if (receiverBranch == prevEndSeg.endBranch) {
                    outTree.add(nextSegment(segmentList, SimpleSeismicPhase.PWAVE, END));
                    if (nextAllowSWave) {
                        outTree.add(nextSegment(segmentList, SimpleSeismicPhase.SWAVE, END));
                    }
                }
                break;
            case REFLECT_UNDERSIDE:
                if (receiverBranch == prevEndSeg.endBranch+1) {
                    outTree.add(nextSegment(segmentList, SimpleSeismicPhase.PWAVE, END));
                    if (nextAllowSWave) {
                        outTree.add(nextSegment(segmentList, SimpleSeismicPhase.SWAVE, END));
                    }
                }
                break;
            case TRANSDOWN:
                if (receiverBranch == prevEndSeg.endBranch+2) {
                    outTree.add(nextSegment(segmentList, SimpleSeismicPhase.PWAVE, END));
                    if (nextAllowSWave) {
                        outTree.add(nextSegment(segmentList, SimpleSeismicPhase.SWAVE, END));
                    }
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
                outTree.add(nextSegment(segmentList, SimpleSeismicPhase.PWAVE, TURN));
                if (nextAllowSWave) outTree.add(nextSegment(segmentList, SimpleSeismicPhase.SWAVE, TURN));
                if (prevEndSeg.endBranch < tMod.getNumBranches()-2) {
                    outTree.add(nextSegment(segmentList, SimpleSeismicPhase.PWAVE, TRANSDOWN));
                    outTree.add(nextSegment(segmentList, SimpleSeismicPhase.PWAVE, REFLECT_TOPSIDE));
                    if (nextAllowSWave)  {
                        outTree.add(nextSegment(segmentList, SimpleSeismicPhase.SWAVE, TRANSDOWN));
                        outTree.add(nextSegment(segmentList, SimpleSeismicPhase.SWAVE, REFLECT_TOPSIDE));
                    }
                }
                break;
            case REFLECT_TOPSIDE:
            case TRANSUP:
                if (prevEndSeg.endBranch > 1) {
                    outTree.add(nextSegment(segmentList, SimpleSeismicPhase.PWAVE, TRANSUP));
                    if (nextAllowSWave)  {
                        outTree.add(nextSegment(segmentList, SimpleSeismicPhase.SWAVE, TRANSUP));
                    }
                }
                outTree.add(nextSegment(segmentList, SimpleSeismicPhase.PWAVE, REFLECT_UNDERSIDE));
                if (nextAllowSWave)  {
                    outTree.add(nextSegment(segmentList, SimpleSeismicPhase.SWAVE, REFLECT_UNDERSIDE));
                }
                break;
            case TURN:
                if (prevEndSeg.endBranch > 0) {
                    outTree.add(nextSegment(segmentList, prevEndSeg.isPWave, TRANSUP));
                }
                outTree.add(nextSegment(segmentList, prevEndSeg.isPWave, REFLECT_UNDERSIDE));
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
            case TRANSUP:
            case REFLECT_UNDERSIDE:
            case REFLECT_UNDERSIDE_CRITICAL:
            case TURN:
            case END:
                maxRayParam = Math.min(maxRayParam, nextBranch.getMaxRayParam());
                break;

        }

        out.add(new SeismicPhaseSegment(tMod,
                startBranchNum, startBranchNum, isPWave, endAction, isDowngoing, nextLegName,
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
                        Math.min(prev.maxRayParam, seg.maxRayParam), Math.max(prev.minRayParam, seg.minRayParam));
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
        if (segmentList.size()>15) { return 9999999;}
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
                default:

            }
            if (prev != null && prev.isPWave != seg.isPWave) {
                count++;
            }
            prev = seg;
        }
        return count;
    }
}
