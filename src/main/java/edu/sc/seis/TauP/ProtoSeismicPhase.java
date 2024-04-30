package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.PhaseInteraction.*;

public class ProtoSeismicPhase implements Comparable<ProtoSeismicPhase> {

    public ProtoSeismicPhase(List<SeismicPhaseSegment> segmentList) {
        this.segmentList = segmentList;
        phaseName = phaseNameForSegments();
    }

    public static ProtoSeismicPhase start(SeismicPhaseSegment startSeg) {
        return new ProtoSeismicPhase(new ArrayList<>(List.of(startSeg)));
    }


    public ProtoSeismicPhase nextSegment(boolean isPWave,
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
        String nextLegName = SeismicPhaseWalk.legNameForTauBranch(tMod, startBranchNum, isPWave, isDowngoing);
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
        ProtoSeismicPhase proto = new ProtoSeismicPhase(out);
        proto.validateSegList();
        return proto;
    }


    public void validateSegList() {
        SeismicPhaseSegment prev = null;
        for (SeismicPhaseSegment seg : segmentList) {
            if (seg.maxRayParam == 0) {
                throw new RuntimeException("maxRayParam is zero: "+phaseNameForSegments());
            }
            if (seg.endBranch == seg.tMod.getNumBranches()-1 && seg.isDownGoing && seg.endAction != TURN) {
                throw new RuntimeException("down not turn in innermost core layer: "
                        +phaseNameForSegments()+" "+seg.endBranch+" "+ seg.tMod.getNumBranches()+" "+seg.endAction);
            }
            if (prev != null) {
                if (prev.endAction == TRANSDOWN && prev.endBranch != seg.startBranch-1) {
                    throw new RuntimeException("prev is TRANSDOWN, but seg is not +1\n"+prev.endAction+"  "+seg.startBranch+"\n"+phaseNameForSegments());
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

    public final SeismicPhaseSegment get(int i) {
        return segmentList.get(i);
    }

    public final int size() {
        return segmentList.size();
    }

    public final void add(SeismicPhaseSegment seg) {
        segmentList.add(seg);
    }

    public int calcInteractionNumber() {
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


    public String phaseNameForSegments() {
        return phaseNameForSegments(true);
    }
    public String phaseNameForSegments(boolean zapED) {
        String name = "";
        TauModel tMod = segmentList.get(0).tMod;
        int idx = 0;
        SeismicPhaseSegment prev;
        SeismicPhaseSegment seg = null;
        SeismicPhaseSegment next = segmentList.get(0);
        while (idx < segmentList.size()) {
            prev = seg;
            seg = next;
            if (idx < segmentList.size()-1) {
                next = segmentList.get(idx+1);
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

    public static String legNameForSegment(TauModel tMod, SeismicPhaseSegment seg) {
        String name = SeismicPhaseWalk.legNameForTauBranch(tMod, seg.endBranch, seg.isPWave, seg.isDownGoing);
        if (seg.endAction == TURN && name.endsWith("ed")) {
            name = name.substring(0, name.length()-2);
        }
        return name;
    }

    public String branchNumSeq() {
        String out = "";
        for (SeismicPhaseSegment seg : segmentList) {
            out += seg.startBranch;
            if (seg.endBranch != seg.startBranch) {
                out += seg.endBranch;
            }
        }
        return out;
    }

    List<SeismicPhaseSegment> segmentList;

    String phaseName;

    @Override
    public int compareTo(ProtoSeismicPhase o) {
        return phaseName.compareTo(o.phaseName);
    }
}
