package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.PhaseInteraction.*;
import static edu.sc.seis.TauP.PhaseSymbols.*;
import static edu.sc.seis.TauP.SeismicPhaseFactory.endActionString;

/**
 * Represents a partial seismic phase, appended to as a name is parsed.
 * May also fail if part way if phase is not compatible with the model.
 */
public class ProtoSeismicPhase implements Comparable<ProtoSeismicPhase> {

    public ProtoSeismicPhase(List<SeismicPhaseSegment> segmentList, double receiverDepth) {
        this(segmentList, receiverDepth, null);
        this.tMod = segmentList.get(0).tMod;
        SeismicPhaseSegment prev = null;
        for (SeismicPhaseSegment seg : segmentList) {
            if (prev != null) {
                seg.prevEndAction = prev.endAction;
            }
            prev = seg;
        }
        try {
            validateSegList();
        } catch (TauModelException e) {
            throw new RuntimeException(e);
        }
    }

    ProtoSeismicPhase(List<SeismicPhaseSegment> segmentList, double receiverDepth, String phaseName) {
        this.segmentList = segmentList;
        this.receiverDepth = receiverDepth;
        this.phaseName = phaseName; // possible overwrite later
        if ( ! segmentList.isEmpty()) {
            this.tMod = segmentList.get(0).tMod;
        }
    }

    public static ProtoSeismicPhase startEmpty(String phaseName, TauModel tMod, double receiverDepth) {
        ProtoSeismicPhase proto = new ProtoSeismicPhase(new ArrayList<>(), receiverDepth, phaseName);
        proto.phaseName = phaseName;
        proto.tMod = tMod;
        if (tMod == null) {throw new IllegalArgumentException("TauModel cannot be null");}
        return proto;
    }

    public static ProtoSeismicPhase start(SeismicPhaseSegment startSeg, double receiverDepth) {
        if (startSeg == null) {throw new RuntimeException("Start Segment cannot be null");}
        return new ProtoSeismicPhase(new ArrayList<>(List.of(startSeg)), receiverDepth);
    }

    public static ProtoSeismicPhase failNewPhase(TauModel tMod,
                                                 boolean isPWave,
                                                 boolean isDownGoing,
                                                 double receiverDepth,
                                                 String phaseName,
                                                 String reason) {
        ProtoSeismicPhase failed = startEmpty(phaseName, tMod, receiverDepth);
        int startBranchNum = tMod.getSourceBranch();
        failed.add(SeismicPhaseSegment.failSegment(tMod, startBranchNum, startBranchNum,
                        isPWave, isDownGoing, phaseName));
        failed.isFail = true;
        failed.failReason = reason;
        if(TauPConfig.DEBUG) {
            Alert.debug("FAIL: "+reason+" within phase " + phaseName);
        }
        return failed;
    }

    public static ProtoSeismicPhase startNewPhase(TauModel tMod,
                                                  boolean isPWave,
                                                  PhaseInteraction endAction,
                                                  boolean isDownGoing,
                                                  double receiverDepth) throws TauPException {
        int startBranchNum = tMod.getSourceBranch();
        if ( ! isDownGoing) {
            startBranchNum = startBranchNum-1;
        }
        String legName = legNameForSegment(tMod, startBranchNum, isPWave, isDownGoing, false, endAction);
        ProtoSeismicPhase proto = startEmpty(legName, tMod, receiverDepth);
        TauBranch startBranch = tMod.getTauBranch(startBranchNum, isPWave);
        double minRayParam = 0.0;
        double maxRayParam;
        SlownessModel sMod = tMod.getSlownessModel();
        if (isDownGoing) {
            if (tMod.getSourceDepth() == tMod.getRadiusOfEarth()) {
                throw new TauPException("Cannot be downgoing for source at center of earth: "+tMod.getSourceDepth());
            }
            SlownessLayer slownessLayer = sMod.getSlownessLayer(sMod.layerNumberBelow(tMod.getSourceDepth(), isPWave), isPWave);
            maxRayParam = slownessLayer.getTopP();
            switch (endAction) {
                case TURN:
                    minRayParam = startBranch.getMinRayParam();
                    break;
                case REFLECT_TOPSIDE:
                case TRANSDOWN:
                    maxRayParam = startBranch.getMinTurnRayParam();
                    break;
                case REFLECT_TOPSIDE_CRITICAL:
                    maxRayParam = startBranch.getMinTurnRayParam();
                    minRayParam = startBranch.getMinRayParam();
                    break;
                default:
                    throw new TauPException("Don't understand endAction "+endAction+" when downgoing");
            }
        } else {
            // upgoing
            if (tMod.getSourceDepth() == 0) {throw new TauPException("Cannot be upgoing for zero depth source");}
            SlownessLayer slownessLayer = sMod.getSlownessLayer(sMod.layerNumberAbove(tMod.getSourceDepth(), isPWave), isPWave);
            maxRayParam = slownessLayer.getBotP();
            switch (endAction) {
                case REFLECT_UNDERSIDE:
                case TRANSUP:
                    maxRayParam = Math.max(maxRayParam, startBranch.getMinTurnRayParam());
                    break;
                default:
                    throw new TauPException("Don't understand endAction "+endAction+" when upgoing");
            }
        }
        proto.add(new SeismicPhaseSegment(tMod, startBranchNum, startBranchNum,
                        isPWave, endAction, isDownGoing, legName, minRayParam, maxRayParam));
        return proto;
    }


    public SeismicPhaseSegment failNext(String reason) {
        if (TauPConfig.DEBUG){
            Alert.debug("Fail: " + reason + " empty: " + segmentList.isEmpty());
        }
        SeismicPhaseSegment failSeg = SeismicPhaseSegment.failSegment(tMod);
        segmentList.add(failSeg);
        isFail = true;
        failReason = reason;
        return failSeg;
    }

    public ProtoSeismicPhase nextSegment(boolean isPWave,
                                         PhaseInteraction endAction) throws TauModelException {
        List<SeismicPhaseSegment> out = new ArrayList<>(segmentList);
        SeismicPhaseSegment endSeg = segmentList.isEmpty() ? null : segmentList.get(segmentList.size()-1);
        TauModel tMod = endSeg != null ? endSeg.getTauModel() : null;
        int priorEndBranchNum = endSeg != null ? endSeg.endBranch : -1;
        boolean isDowngoing;
        switch (endAction) {
            case TRANSUP:
            case REFLECT_UNDERSIDE:
            case REFLECT_UNDERSIDE_CRITICAL:
            case END:
            case TURN:
            case TRANSDOWN:
            case REFLECT_TOPSIDE:
            case REFLECT_TOPSIDE_CRITICAL:
            case END_DOWN:
                isDowngoing = PhaseInteraction.isDowngoingActionBefore(endAction);
                break;
            case FAIL:
                SeismicPhaseSegment nextSeg = SeismicPhaseSegment.failSegment(tMod, priorEndBranchNum, priorEndBranchNum, isPWave, true, "");
                out.add(nextSeg);
                if (endSeg != null) {
                    nextSeg.prevEndAction = endSeg.endAction;
                }
                ProtoSeismicPhase failProto =  new ProtoSeismicPhase(out, receiverDepth);
                failProto.isFail = true;
                return failProto;
            case START:
                throw new IllegalArgumentException("End action cannot be START: "+endAction);
            default:
                throw new IllegalArgumentException("End action case not yet impl: "+endAction);
        }
        if (! isDowngoing && endSeg != null && (endSeg.endBranch == 0 && endSeg.endAction != TURN)) {
            SeismicPhaseSegment nextSeg = SeismicPhaseSegment.failSegment(tMod, priorEndBranchNum, priorEndBranchNum, isPWave, true, "");
            isFail = true;
            failReason = "phase upgoing at surface";
            out.add(nextSeg);
            nextSeg.prevEndAction = endSeg.endAction;
            return new ProtoSeismicPhase(out, receiverDepth);
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
        // usually same as start unless cross source depth that is not a discon
        int endBranchNum = findEndDiscon(tMod, startBranchNum, isPWave, isDowngoing);
        if (endBranchNum == 0 && endAction == TRANSUP) {
            SeismicPhaseSegment nextSeg = SeismicPhaseSegment.failSegment(tMod, startBranchNum, endBranchNum, isPWave, false, "");
            isFail = true;
            failReason = "phase transup at surface";
            out.add(nextSeg);
            nextSeg.prevEndAction = endSeg.endAction;
            return new ProtoSeismicPhase(out, receiverDepth);
        }
        boolean isFlat = false;
        String nextLegName = SeismicPhaseWalk.legNameForTauBranch(tMod, startBranchNum, isPWave, isFlat, isDowngoing);
        TauBranch startBranch = tMod.getTauBranch(startBranchNum, isPWave);
        TauBranch endBranch = tMod.getTauBranch(endBranchNum, isPWave);


        double minRayParam = endSeg.minRayParam;
        double maxRayParam = endSeg.maxRayParam;
        TauBranch priorEndBranch = tMod.getTauBranch(priorEndBranchNum, endSeg.isPWave);

        // check to make sure RP compatible with boundary when entering from below or above
        // or when reflecting with phase change
        switch (endSeg.endAction) {
            case REFLECT_UNDERSIDE:
            case REFLECT_UNDERSIDE_CRITICAL:
            case TRANSDOWN:
                maxRayParam = Math.min(maxRayParam, startBranch.getTopRayParam());
                break;
            case REFLECT_TOPSIDE:
            case REFLECT_TOPSIDE_CRITICAL:
            case TRANSUP:
                maxRayParam = Math.min(maxRayParam, startBranch.getBotRayParam());
            default:
        }
        switch (endAction) {
            case REFLECT_TOPSIDE_CRITICAL:
                minRayParam = Math.max(minRayParam, endBranch.getMinRayParam());
            case TRANSDOWN:
            case REFLECT_TOPSIDE:
            case END_DOWN:
                maxRayParam = Math.min(maxRayParam, endBranch.getMinTurnRayParam());
                maxRayParam = Math.min(maxRayParam, endBranch.getBotRayParam());
                break;

            case TURN:
                minRayParam = Math.max(minRayParam, endBranch.getBotRayParam());
                maxRayParam = Math.min(maxRayParam, startBranch.getTopRayParam());
                break;
            case TRANSUP:
                maxRayParam = Math.min(maxRayParam, endBranch.getTopRayParam());
            case REFLECT_UNDERSIDE:
            case REFLECT_UNDERSIDE_CRITICAL:
            case END:
                maxRayParam = Math.min(maxRayParam, endBranch.getMaxRayParam());
                break;

        }

        SeismicPhaseSegment nextSeg;
        if (maxRayParam < minRayParam) {
            nextSeg = SeismicPhaseSegment.failSegment(tMod, startBranchNum, endBranchNum, isPWave, isDowngoing, nextLegName);
        } else {
            nextSeg = new SeismicPhaseSegment(tMod,
                    startBranchNum, endBranchNum, isPWave, endAction, isDowngoing, nextLegName,
                    minRayParam, maxRayParam);
        }
        nextSeg.prevEndAction = endSeg.endAction;
        validateSegList();
        out.add(nextSeg);
        ProtoSeismicPhase proto = new ProtoSeismicPhase(out, receiverDepth);
        proto.validateSegList();
        return proto;
    }

    public static int findEndDiscon(TauModel tMod, int startBranchNum, boolean isPWave, boolean isDowngoing) {
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
        return endBranchNum;
    }

    public void validateSegList() throws TauModelException {
        if (segmentList.isEmpty()) {
            return;
        }
        if (endSegment().endAction == FAIL) {
            // failing is failing, no need to validate
            return;
        }
        SeismicPhaseSegment prev = null;
        for (SeismicPhaseSegment seg : segmentList) {
            if (seg.maxRayParam < 0) {
                throw new TauModelException("maxRayParam is zero: "+phaseNameForSegments());
            }
            if (seg.endBranch == seg.tMod.getNumBranches()-1 && seg.isDownGoing && seg.endAction != TURN) {
                throw new TauModelException("down not turn in innermost core layer: "
                        +phaseNameForSegments()+" "+seg.endBranch+" "+ seg.tMod.getNumBranches()+" "+seg.endAction);
            }
            if (prev != null) {
                String currLeg = seg.legName;
                if (seg.prevEndAction != prev.endAction) {
                    throw new TauModelException("segment prevEndAction is not prev segment endAction: "
                            +phaseNameForSegments()+" "+seg.prevEndAction+" "+prev.endAction);
                }
                if (prev.endAction == TRANSDOWN && prev.endBranch != seg.startBranch-1) {
                    throw new TauModelException("prev is TRANSDOWN, but seg is not +1\n"
                            +phaseNameForSegments()+" "
                            +prev.endAction+"  "+seg.startBranch+"\n"+phaseNameForSegments());
                }
                if (prev.endAction == TURN &&
                        ( seg.endAction == TURN || seg.endAction == TRANSDOWN || seg.endAction == DIFFRACTTURN
                                || seg.endAction == END_DOWN || seg.endAction == REFLECT_TOPSIDE)) {
                    throw new TauModelException("prev is TURN, but seg is "+phaseNameForSegments()+" "+seg.endAction);
                }
                if (prev.isDownGoing && seg.isDownGoing && prev.endBranch +1 != seg.startBranch) {
                    throw new TauModelException("Prev and Curr both downgoing but prev.endBranch+1 != seg.startBranch"
                            +phaseNameForSegments()+" "
                            +" pdown: "+prev.isDownGoing+" currdown "+seg.isDownGoing
                            +" && "+prev.endBranch+" +1 != "+seg.startBranch);
                }
                if (prev.isFlat) {
                    if (seg.isDownGoing) {
                        if (prev.endsAtTop() && prev.endBranch != seg.startBranch) {
                            throw new TauModelException(getName()
                                    + ": Flat Segment is ends at top, but start is not current branch: " + currLeg);
                        } else if (!prev.endsAtTop() && prev.endBranch != seg.startBranch - 1) {
                            throw new TauModelException(getName()
                                    + ": Flat Segment is ends at bottom, but start is not next deeper branch: " + currLeg);
                        }
                    } else {
                        if (prev.endsAtTop() && prev.endBranch != seg.startBranch +1) {
                            throw new TauModelException(getName()
                                    + ": Flat Segment is ends at top, but upgoing start is not next shallower branch: " + currLeg+" "+prev.endBranch +"!= "+seg.startBranch+"+1");
                        } else if (!prev.endsAtTop() && prev.endBranch != seg.startBranch) {
                            throw new TauModelException(getName()
                                    + ": Flat Segment is ends at bottom, but upgoing start is not current branch: " + currLeg+" "+prev.endBranch +"!= "+seg.startBranch);
                        }
                    }
                } else if (seg.isDownGoing) {
                    if (prev.endBranch > seg.startBranch) {
                        throw new TauModelException(getName()
                                +": Segment is downgoing, but we are already below the start: "+currLeg);
                    }
                    if (prev.endAction == REFLECT_TOPSIDE || prev.endAction == REFLECT_TOPSIDE_CRITICAL) {
                        throw new TauModelException(getName()
                                +": Segment is downgoing, but previous action was to reflect up: "+currLeg+" "+prev.endAction+" "+seg);
                    }
                    if (prev.endAction == TURN) {
                        throw new TauModelException(getName()
                                +": Segment is downgoing, but previous action was to turn: "+currLeg);
                    }
                    if (prev.endAction == DIFFRACTTURN) {
                        throw new TauModelException(getName()
                                +": Segment is downgoing, but previous action was to diff turn: "+currLeg);
                    }
                    if (prev.endAction == TRANSUP) {
                        throw new TauModelException(getName()
                                +": Segment is downgoing, but previous action was to transmit up: "+currLeg);
                    }
                    if (prev.endBranch == seg.startBranch && prev.isDownGoing == false &&
                            ! (prev.endAction == REFLECT_UNDERSIDE || prev.endAction == REFLECT_UNDERSIDE_CRITICAL)) {
                        throw new TauModelException(getName()
                                +": Segment "+currLeg
                                +" is downgoing, but previous action was not to reflect underside: "
                                +currLeg+" "+endActionString(prev.endAction));
                    }
                } else {
                    if (prev.endAction == REFLECT_UNDERSIDE || prev.endAction == REFLECT_UNDERSIDE_CRITICAL) {
                        throw new TauModelException(getName()
                                +": Segment is upgoing, but previous action was to underside reflect down: "+currLeg);
                    }
                    if (prev.endAction == TRANSDOWN) {
                        throw new TauModelException(getName()
                                +": Segment is upgoing, but previous action was  to trans down: "+currLeg);
                    }
                    if (prev.endBranch == seg.startBranch && prev.isDownGoing == true
                            && ! ( prev.endAction == TURN || prev.endAction == DIFFRACTTURN
                            || prev.endAction == DIFFRACT || prev.endAction == HEAD
                            || prev.endAction == REFLECT_TOPSIDE || prev.endAction == REFLECT_TOPSIDE_CRITICAL)) {
                        throw new TauModelException(getName()
                                +": Segment is upgoing, but previous action was not to reflect topside: "
                                +currLeg+" "+endActionString(prev.endAction));
                    }
                }
            }
            prev = seg;
        }
        SeismicPhaseSegment endSeg = endSegment();
        int receiverBranch = tMod.findBranch(receiverDepth);
        if (endSeg.prevEndAction != KMPS) {
            if (endSeg.endAction == END && endSeg.endBranch != receiverBranch) {
                throw new TauModelException(getName()
                        + " End is upgoing, but last branch num is not receiver branch: "
                        + endSeg.endBranch + " != " + receiverBranch + " rec depth: " + receiverDepth);
            } else if (endSeg.endAction == END_DOWN && endSeg.endBranch != receiverBranch - 1) {
                throw new TauModelException(getName()
                        + " End is downgoing, but last branch num is not receiver branch-1: "
                        + endSeg.endBranch + " != " + receiverBranch + " rec depth: " + receiverDepth);
            }
        }
        if (TauPConfig.VERBOSE) {
            Alert.debug("#### VALIDATE OK " + getName());
        }
    }

    public List<ShadowOrProto> splitForAllHighSlowness() throws TauModelException {
        List<ShadowOrProto> shadowSplits = List.of(new ShadowOrProto(this));
        for (int psIdx = 0; psIdx < 2; psIdx++) {
            TauBranch prevTB = null;
            boolean isPWave = psIdx==0;

            for (int tbNum = 0; tbNum < tMod.getNumBranches(); tbNum++) {
                TauBranch tb = tMod.getTauBranch(tbNum, isPWave);
                if (tb.isHighSlowness() && tb.getTopRayParam() <= endSegment().maxRayParam
                        && tb.getTopRayParam() > endSegment().minRayParam) {
                    // ray param range overlaps shadow ray param for HSZ, split proto
                    List<ShadowOrProto> outList = new ArrayList<>();
                    for (ShadowOrProto inVal : shadowSplits) {
                        if (inVal.isProto()) {
                            List<ShadowOrProto> splitList = inVal.getProto().splitForHighSlowness(tb);
                            outList.addAll(splitList);
                        } else {
                            outList.add(inVal);
                        }
                    }
                    shadowSplits = outList;
                }
                if (prevTB != null && prevTB.getBotRayParam() < tb.getTopRayParam()
                        && endSegment().minRayParam < prevTB.getBotRayParam()
                        &&  prevTB.getBotRayParam() < endSegment().maxRayParam ) {

                    // LVZ discon
                    List<ShadowOrProto> outList = new ArrayList<>();
                    for (ShadowOrProto inVal : shadowSplits) {
                        if (inVal.isProto()) {
                            List<ShadowOrProto> splitList = inVal.getProto().splitForHighSlownessDiscon(tbNum, isPWave);
                            outList.addAll(splitList);
                        } else {
                            outList.add(inVal);
                        }
                    }
                    shadowSplits = outList;
                }
                prevTB = tb;
            }
        }

        return shadowSplits;
    }

    public List<ShadowOrProto> splitForHighSlowness(TauBranch hszBranch) throws TauModelException {
        boolean found = false;
        int hszBranchNum = -1;
        for (int bNum = 0; bNum < tMod.getNumBranches(); bNum++) {
            if (tMod.getTauBranch(bNum, hszBranch.isPWave) == hszBranch) {
                hszBranchNum = bNum;
            }
        }
        if (hszBranchNum == -1) {throw new TauModelException("Unable to find TauBranch in TauModel: "+hszBranch);}
        SeismicPhaseSegment endSeg = endSegment();
        double minRayParam = endSeg.minRayParam;
        double maxRayParam = endSeg.maxRayParam;
        double hszRayParam = hszBranch.getTopRayParam();

        if (!hszBranch.isHighSlowness() || hszRayParam < minRayParam ||  maxRayParam < hszRayParam) {
            // phase doesn't strictly contain HSZ ray param, so no shadow zone
            return List.of(new ShadowOrProto(this));
        }
        List<SeismicPhaseSegment> preShadowSegList = new ArrayList<>();
        List<SeismicPhaseSegment> postShadowSegList = new ArrayList<>();
        SeismicPhaseSegment seg = null;
        for (SeismicPhaseSegment next : segmentList) {
            if (seg != null && seg.endAction == TURN
                    && seg.isPWave == hszBranch.isPWave
                    && hszRayParam < seg.maxRayParam
                    && seg.startBranch <= hszBranchNum
                    && seg.endBranch >= hszBranchNum) {
                found = true;

                // phase that turns above HSZ
                SeismicPhaseSegment downSplitSeg = new SeismicPhaseSegment(
                        seg.tMod,
                        seg.startBranch,
                        hszBranchNum - 1,
                        seg.isPWave,
                        seg.endAction,
                        seg.isDownGoing,
                        seg.legName,
                        hszRayParam,
                        seg.maxRayParam
                );
                downSplitSeg.prevEndAction = seg.prevEndAction;
                preShadowSegList.add(downSplitSeg);
                SeismicPhaseSegment upSplitSeg = new SeismicPhaseSegment(
                        next.tMod,
                        hszBranchNum-1,
                        next.endBranch,
                        next.isPWave,
                        next.endAction,
                        next.isDownGoing,
                        next.legName,
                        hszRayParam,
                        seg.maxRayParam
                );
                upSplitSeg.prevEndAction = next.prevEndAction;
                preShadowSegList.add(upSplitSeg);
                if (hszBranchNum == seg.startBranch) {
                    // high slowness at top, so only need turn below phase, so fail the above phase
                    downSplitSeg.maxRayParam = -1;
                    downSplitSeg.minRayParam = -1;
                    upSplitSeg.maxRayParam = -1;
                    upSplitSeg.minRayParam = -1;
                }
                if (downSplitSeg.maxRayParam < downSplitSeg.minRayParam) {throw new RuntimeException("downSplitSeg max rp < min rp");}
                if (upSplitSeg.maxRayParam < upSplitSeg.minRayParam) {throw new RuntimeException("upSplitSeg max rp < min rp");}

                // now go below HSZ

                // phase that transmits HSZ
                TauBranch transBranch = tMod.getTauBranch(hszBranchNum - 1, seg.isPWave);
                SeismicPhaseSegment downTransSeg = new SeismicPhaseSegment(
                        seg.tMod,
                        seg.startBranch,
                        hszBranchNum - 1,
                        seg.isPWave,
                        TRANSDOWN,
                        seg.isDownGoing,
                        seg.legName,
                        seg.minRayParam,
                        Math.min(hszRayParam, transBranch.getBotRayParam())
                );
                downTransSeg.prevEndAction = seg.prevEndAction;
                postShadowSegList.add(downTransSeg);

                // phase that turns below HSZ
                SeismicPhaseSegment downBelowSeg = new SeismicPhaseSegment(
                        seg.tMod,
                        hszBranchNum,
                        seg.endBranch,
                        seg.isPWave,
                        seg.endAction,
                        seg.isDownGoing,
                        seg.legName,
                        seg.minRayParam,
                        hszRayParam
                );
                downBelowSeg.prevEndAction = downTransSeg.endAction;
                if (downBelowSeg.maxRayParam < downBelowSeg.minRayParam) {throw new RuntimeException("downBelowSeg max rp < min rp");}

                postShadowSegList.add(downBelowSeg);
                SeismicPhaseSegment upBelowSeg = new SeismicPhaseSegment(
                        next.tMod,
                        next.startBranch,
                        next.endBranch,
                        next.isPWave,
                        next.endAction,
                        next.isDownGoing,
                        next.legName,
                        seg.minRayParam,
                        hszRayParam
                );
                upBelowSeg.prevEndAction = next.prevEndAction;
                if (upBelowSeg.maxRayParam < upBelowSeg.minRayParam) {throw new RuntimeException("upBelowSeg max rp < min rp");}
                postShadowSegList.add(upBelowSeg);

                seg = null;
            } else {
                if (seg != null) {
                    preShadowSegList.add(seg);
                    postShadowSegList.add(seg);
                }
                seg = next;
            }
        }
        if (found) {
            if (seg != null ) {
                preShadowSegList.add(seg);
                postShadowSegList.add(seg);
            }
            ProtoSeismicPhase preShadow = new ProtoSeismicPhase(preShadowSegList, receiverDepth, phaseName);
            preShadow.calcEndSegRayParam();
            if (preShadow.countFlatLegs()==0 && preShadow.endSegment().maxRayParam==preShadow.endSegment().minRayParam) {
                preShadow.failNext("Single ray parameter with no flat segments");
            }
            ProtoSeismicPhase postShadow = new ProtoSeismicPhase(postShadowSegList, receiverDepth, phaseName);
            postShadow.calcEndSegRayParam();
            if (postShadow.countFlatLegs()==0 && postShadow.endSegment().maxRayParam==postShadow.endSegment().minRayParam) {
                postShadow.failNext("Single ray parameter with no flat segments");
            }
            if (preShadow.isFail) {
                return List.of(new ShadowOrProto(postShadow));
            }
            if (postShadow.isFail) {
                return List.of(new ShadowOrProto(preShadow));
            }
            if (preShadow.endSegment().minRayParam != postShadow.endSegment().maxRayParam) {
                throw new TauModelException("Shadow ray params don't match: "+preShadow.endSegment().maxRayParam+" != "+postShadow.endSegment().minRayParam);
            }
            ShadowZone shadow = new ShadowZone(preShadow.endSegment().minRayParam, hszBranch);
            return List.of(new ShadowOrProto(preShadow), new ShadowOrProto(shadow), new ShadowOrProto(postShadow));
        } else {
            return List.of(new ShadowOrProto(this));
        }
    }

    /**
     * Split for a discontinuity where above slowness is smaller than below. Usually
     * a low velocity zone. Only splits for discontinuities internal to a
     * SeismicPhaseSegment as discontinuities at the boundary are handled
     * via normal phase generation and min,max ray param and cannot generate
     * an internal shadow zone.
     * @param hszBranchNum TauBranch number with the discon at its top
     * @param isPWave true for P waves, false for S waves
     * @return phase split for shadow zone
     * @throws TauModelException
     */
    public List<ShadowOrProto> splitForHighSlownessDiscon(int hszBranchNum, boolean isPWave)
            throws TauModelException {
        if (hszBranchNum == 0) {
            // discon at free surface?
            return List.of(new ShadowOrProto(this));
        }
        TauBranch abovehszBranch = tMod.getTauBranch(hszBranchNum-1, isPWave);
        TauBranch hszBranch = tMod.getTauBranch(hszBranchNum, isPWave);
        if (abovehszBranch.getBotRayParam() >= hszBranch.getTopRayParam()) {
            // normal discon or not a discon at all, so no negative jump in velocity
            return List.of(new ShadowOrProto(this));
        }
        SeismicPhaseSegment endSeg = endSegment();
        double minRayParam = endSeg.minRayParam;
        double maxRayParam = endSeg.maxRayParam;
        // only concerned with high slowness discontinuities strictly contained within
        // the SeismicPhaseSegment. Discontinuities at the boundary of the SeismicPhaseSegment
        // are handled by min/max ray param and can't create shadow zones inside of a phase.
        if (minRayParam > hszBranch.getTopRayParam()  || maxRayParam <= abovehszBranch.getBotRayParam()) {
            // ray parameters don't overlap the discon ray paramters
            return List.of(new ShadowOrProto(this));
        }
        // find max ray param that makes it to the discontinuity

        double hszRayParam = abovehszBranch.getBotRayParam();
        boolean foundSegRPOverlap = false;
        for (SeismicPhaseSegment seg : segmentList) {
            if (seg.maxRayParam < hszRayParam) {
                hszRayParam = seg.maxRayParam;
            }
            if (seg != null
                    && seg.isPWave == hszBranch.isPWave
                    && seg.startBranch <= hszBranchNum
                    && seg.endBranch >= hszBranchNum) {
                // found the seg containing the hsz discon,
                foundSegRPOverlap = true;
            }
        }

        if (!foundSegRPOverlap || hszRayParam < minRayParam ||  maxRayParam < hszRayParam) {
            // phase doesn't strictly contain HSZ ray param, so no shadow zone
            return List.of(new ShadowOrProto(this));
        }
        List<SeismicPhaseSegment> preShadowSegList = new ArrayList<>();
        List<SeismicPhaseSegment> postShadowSegList = new ArrayList<>();
        SeismicPhaseSegment seg = null;
        boolean found = false;
        for (SeismicPhaseSegment next : segmentList) {
            if (seg != null && seg.endAction == TURN
                    && seg.isPWave == hszBranch.isPWave
                    && hszRayParam < seg.maxRayParam
                    && seg.minRayParam < hszRayParam
                    && seg.startBranch < hszBranchNum
                    && seg.endBranch >= hszBranchNum) {
                found = true;
                // phase that turns above HSZ
                SeismicPhaseSegment downSplitSeg = new SeismicPhaseSegment(
                        seg.tMod,
                        seg.startBranch,
                        hszBranchNum - 1,
                        seg.isPWave,
                        seg.endAction,
                        seg.isDownGoing,
                        seg.legName,
                        hszRayParam,
                        seg.maxRayParam
                );
                downSplitSeg.prevEndAction = seg.prevEndAction;
                preShadowSegList.add(downSplitSeg);
                if (next.endBranch > hszBranchNum-1) {
                    throw new RuntimeException("next seg ends before HSZ: "+next.endBranch+" > "+(hszBranchNum-1));
                }
                SeismicPhaseSegment upSplitSeg = new SeismicPhaseSegment(
                        next.tMod,
                        hszBranchNum-1,
                        next.endBranch,
                        next.isPWave,
                        next.endAction,
                        next.isDownGoing,
                        next.legName,
                        hszRayParam,
                        seg.maxRayParam
                );
                upSplitSeg.prevEndAction = next.prevEndAction;
                preShadowSegList.add(upSplitSeg);
                if (hszBranchNum == seg.startBranch) {
                    // high slowness at top, so only need turn below phase, so fail the above phase
                    downSplitSeg.maxRayParam = -1;
                    downSplitSeg.minRayParam = -1;
                    upSplitSeg.maxRayParam = -1;
                    upSplitSeg.minRayParam = -1;
                }
                if (downSplitSeg.maxRayParam < downSplitSeg.minRayParam) {throw new RuntimeException("downSplitSeg max rp < min rp");}
                if (upSplitSeg.maxRayParam < upSplitSeg.minRayParam) {throw new RuntimeException("upSplitSeg max rp < min rp");}

                // phase that transmits HSZ
                SeismicPhaseSegment downTransSeg = new SeismicPhaseSegment(
                        seg.tMod,
                        seg.startBranch,
                        hszBranchNum,
                        seg.isPWave,
                        TRANSDOWN,
                        seg.isDownGoing,
                        seg.legName,
                        seg.minRayParam,
                        hszRayParam
                );
                downTransSeg.prevEndAction = seg.prevEndAction;
                postShadowSegList.add(downTransSeg);

                // phase that turns below HSZ
                SeismicPhaseSegment downBelowSeg = new SeismicPhaseSegment(
                        seg.tMod,
                        hszBranchNum+1,
                        seg.endBranch,
                        seg.isPWave,
                        seg.endAction,
                        seg.isDownGoing,
                        seg.legName,
                        seg.minRayParam,
                        hszRayParam
                );
                downBelowSeg.prevEndAction = downTransSeg.endAction;
                if (downBelowSeg.maxRayParam < downBelowSeg.minRayParam) {throw new RuntimeException("downBelowSeg max rp < min rp");}

                postShadowSegList.add(downBelowSeg);
                SeismicPhaseSegment upBelowSeg = new SeismicPhaseSegment(
                        next.tMod,
                        next.startBranch,
                        next.endBranch,
                        next.isPWave,
                        next.endAction,
                        next.isDownGoing,
                        next.legName,
                        seg.minRayParam,
                        hszRayParam
                );
                upBelowSeg.prevEndAction = next.prevEndAction;
                if (upBelowSeg.maxRayParam < upBelowSeg.minRayParam) {throw new RuntimeException("upBelowSeg max rp < min rp");}
                postShadowSegList.add(upBelowSeg);

                seg = null;
            } else {
                if (seg != null) {
                    preShadowSegList.add(seg);
                    postShadowSegList.add(seg);
                }
                seg = next;
            }
        }
        if (found) {
            if (seg != null ) {
                preShadowSegList.add(seg);
                postShadowSegList.add(seg);
            }
            ProtoSeismicPhase preShadow = new ProtoSeismicPhase(preShadowSegList, receiverDepth, phaseName);
            preShadow.calcEndSegRayParam();
            if (preShadow.countFlatLegs()==0 && preShadow.endSegment().maxRayParam==preShadow.endSegment().minRayParam) {
                preShadow.failNext("Single ray parameter with no flat segments");
            }
            ProtoSeismicPhase postShadow = new ProtoSeismicPhase(postShadowSegList, receiverDepth, phaseName);
            postShadow.calcEndSegRayParam();
            if (postShadow.countFlatLegs()==0 && postShadow.endSegment().maxRayParam==postShadow.endSegment().minRayParam) {
                postShadow.failNext("Single ray parameter with no flat segments");
            }
            if (preShadow.isFail) {
                return List.of(new ShadowOrProto(postShadow));
            }
            if (postShadow.isFail) {
                return List.of(new ShadowOrProto(preShadow));
            }
            if (preShadow.endSegment().minRayParam != postShadow.endSegment().maxRayParam) {
                throw new TauModelException("Shadow ray params don't match: "+preShadow.endSegment().maxRayParam+" != "+postShadow.endSegment().minRayParam);
            }
            ShadowZone shadow = new ShadowZone(preShadow.endSegment().minRayParam, hszBranch);
            return List.of(new ShadowOrProto(preShadow),
                    new ShadowOrProto(shadow),
                    new ShadowOrProto(postShadow));
        } else {
            return List.of(new ShadowOrProto(this));
        }
    }

    public void calcEndSegRayParam() throws TauModelException {
        double maxRP = segmentList.get(0).maxRayParam;
        double minRP = segmentList.get(0).minRayParam;
        for (SeismicPhaseSegment s : segmentList) {
            maxRP = Math.min(maxRP, s.maxRayParam);
            minRP = Math.max(minRP, s.minRayParam);
        }
        SeismicPhaseSegment end = endSegment();
        if (end.maxRayParam != maxRP || end.minRayParam != minRP) {
            segmentList.remove(end);
            if (maxRP < minRP || maxRP<0 || (maxRP == minRP && countFlatLegs() == 0)) {
                failNext("No ray parameters exists for phase");
            } else {
                addToBranch(end.endBranch, end.isPWave, end.isPWave, end.endAction, end.legName);
                endSegment().maxRayParam = maxRP;
                endSegment().minRayParam = minRP;
            }
        }
    }

    public final List<SeismicPhaseSegment> getSegmentList() {
        return segmentList;
    }

    public final SeismicPhaseSegment get(int i) {
        return segmentList.get(i);
    }

    public final boolean isEmpty() {
        return segmentList.isEmpty();
    }

    public final PhaseInteraction getEndAction() {
        if (isEmpty()) {
            return START;
        }
        return endSegment().endAction;
    }
    public final SeismicPhaseSegment endSegment() {
        if (isEmpty()) {throw new RuntimeException("Segment list is empty");}
        return segmentList.get(segmentList.size()-1);
    }

    public final SeismicPhaseSegment sourceSegment() {
        if (isEmpty()) {throw new RuntimeException("Segment list is empty");}
        return segmentList.get(0);
    }


    public boolean isSuccessful() {
        return( !isFail) && endSegment().endAction != FAIL;
    }

    public final int size() {
        return segmentList.size();
    }

    public int countFlatLegs() {
        int countHeadLegs = 0;
        for (SeismicPhaseSegment seg : segmentList) {
            if (seg.isFlat) { countHeadLegs++;}
        }
        return countHeadLegs;
    }

    public int countHeadLegs() {
        int countHeadLegs = 0;
        for (SeismicPhaseSegment seg : segmentList) {
            if (seg.isFlat && seg.prevEndAction == HEAD) { countHeadLegs++;}
        }
        return countHeadLegs;
    }

    public int countDiffLegs() {
        int countHeadLegs = 0;
        for (SeismicPhaseSegment seg : segmentList) {
            if (seg.isFlat && (seg.prevEndAction == DIFFRACT || seg.prevEndAction == TRANSUPDIFFRACT)) {
                countHeadLegs++;
            }
        }
        return countHeadLegs;
    }

    public final void add(SeismicPhaseSegment seg) {
        if (seg.prevEndAction == KMPS) {
            // keep KMPS
        } else if (segmentList.isEmpty()) {
            seg.prevEndAction = START;
        } else {
            seg.prevEndAction = endSegment().endAction;
        }
        segmentList.add(seg);
    }


    public int calcStartBranch(String currLeg) {
        int currBranch;
        if (currLeg.endsWith(KMPS_CODE)) {
            // surface wave, zero
            currBranch = 0;
        } else if (! isEmpty()) {
            currBranch = endSegment().endBranch + PhaseInteraction.endOffset(endSegment().endAction);
        } else if(isDowngoingSymbol(currLeg)) {
            // initial downgoing leg, like P
            currBranch = tMod.getSourceBranch();
        } else {
            // initial upgoing leg, like p
            currBranch = tMod.getSourceBranch()-1;
        }
        return currBranch;
    }

    /**
     * Adds a segment to a path of a seismic phase. Generally this corresponds to a character in a phase name,
     * like K in PKP.
     * @param endBranch ending branch number, start is calculated from the end branch and end action of the prior leg.
     * @param isPWave current leg phase type, true for P, false for S
     * @param nextIsPWave next leg phase type, true for P, false for S, determines if a phase conversion at the end
     * @param endAction action the phase takes at the end, like TURN or REFLECT_TOPSIDE
     * @param currLeg name of current leg
     * @return The segment added by this call
     * @throws TauModelException if arguments not possible in the model, but not thrown for a simple failure to exist
     */
    public SeismicPhaseSegment addToBranch(int endBranch,
                                           boolean isPWave,
                                           boolean nextIsPWave,
                                           PhaseInteraction endAction,
                                           String currLeg) throws TauModelException {
        if (isFail) {
            // phase has already failed, don't add more segments, return last (failed) segment.
            return segmentList.get(segmentList.size()-1);
        }
        int startBranch = calcStartBranch(currLeg);

        if (startBranch < 0 || startBranch > tMod.getNumBranches()) {
            throw new TauModelException(getName()+": start branch outside range: (0-"+tMod.getNumBranches()+") "+startBranch);
        }
        if (endBranch < 0 || endBranch > tMod.getNumBranches()) {
            throw new TauModelException(getName()+": end branch outside range: "+endBranch);
        }
        if(endAction == TRANSUP && endBranch == 0) {
            failNext("cannot TRANSUP with end branch zero, already at surface: "+endBranch);
        }
        if( ! isPWave && tMod.isFluidBranch(startBranch)) {
            // S wave in fluid
            failNext("Attempt to have S wave in fluid layer in "+getName()+" "+startBranch+" to "+endBranch+" "+endActionString(endAction));
        }
        int endOffset;
        boolean isDownGoing;
        PhaseInteraction prevEndAction = isEmpty() ? PhaseInteraction.START : endSegment().endAction;
        double minRayParam = isEmpty() ? 0 : endSegment().minRayParam;
        double maxRayParam;
        if (isEmpty()) {
            // max ray param from source
            if(isDowngoingSymbol(currLeg) || tMod.getSourceDepth() == 0) {
                maxRayParam = tMod.getTauBranch(tMod.getSourceBranch(),
                        isPWave).getMaxRayParam();
            } else if (isUpgoingSymbol(currLeg)) {
                try {
                    int sLayerNum = tMod.getSlownessModel().layerNumberAbove(tMod.getSourceDepth(), isPWave);
                    maxRayParam = tMod.getSlownessModel().getSlownessLayer(sLayerNum, isPWave).getBotP();
                    // check if source is in high slowness zone
                    DepthRange highSZoneDepth = new DepthRange();
                    if (tMod.getSlownessModel().depthInHighSlowness(tMod.getSourceDepth(), maxRayParam, highSZoneDepth, isPWave)) {
                        // need to reduce maxRayParam until it can propagate out of high slowness zone
                        maxRayParam = Math.min(maxRayParam, highSZoneDepth.rayParam);
                    }
                } catch(NoSuchLayerException e) {
                    throw new TauModelException("Should not happen", e);
                }
            } else {
                throw new TauModelException("Unknown starting max ray param for "+currLeg+" in "+getName()+" at "+tMod.getSourceDepth());
            }
        } else {
            maxRayParam = endSegment().maxRayParam;
        }
        if(TauPConfig.DEBUG) {
            Alert.debug("before addToBranch: minRP="+minRayParam+"  maxRP="+maxRayParam);
            Alert.debug("  addToBranch( start=" + startBranch + " end=" + endBranch
                    + " endAction="+endActionString(endAction)+" "+currLeg+") isP:"+(isPWave?"P":"S"));

        }
        if(endAction == TURN || endAction == DIFFRACTTURN) {
            if (isPWave != nextIsPWave && endAction == TURN) {
                throw new TauModelException(getName()+" phase conversion not allowed for TURN");
            }
            endOffset = 0;
            isDownGoing = true;

            double maxTurnInSegRayParam = tMod.getTauBranch(startBranch,
                            isPWave).getTopRayParam(); // at least penetrate the layer
            double minTurnInSegRayParam = tMod.getTauBranch(startBranch,
                    isPWave).getMinTurnRayParam();

            for (int bnum = startBranch; bnum <= endBranch; bnum++) {
                minTurnInSegRayParam = Math.min(minTurnInSegRayParam, tMod.getTauBranch(bnum,
                                isPWave).getMinTurnRayParam()); // should be getMinRayParam???
            }
            minRayParam = Math.max(minRayParam, minTurnInSegRayParam);
            maxRayParam = Math.min(maxRayParam, maxTurnInSegRayParam);

            // careful S wave and fluid layers, know at least startBranch is not fluid from above check,
            // stop path at first fluid layer
            if ( !isPWave) {
                for (int bNum = startBranch+1; bNum <= endBranch; bNum++) {
                    if (tMod.isFluidBranch(bNum)) {
                        // fluid at bottom, just trim these layers from turn segment
                        endBranch = bNum - 1;
                        minRayParam = Math.max(minRayParam, tMod.getTauBranch(endBranch,
                                        isPWave)
                                .getMinTurnRayParam());
                        break;
                    }
                }
            }
            // careful if the ray param cannot turn due to high slowness. Do not use these
            // layers if their top is in high slowness for the given ray parameter
            // and the bottom is not a critical reflection, rp > max rp in next branch
            int bNum = endBranch;
            while (bNum >= startBranch) {
                TauBranch tauBranch = tMod.getTauBranch(bNum, isPWave);
                if (tauBranch.isHighSlowness() && (
                        bNum+1>=tMod.getNumBranches()
                                ||(
                                        bNum > startBranch &&
                                        tauBranch.getMinTurnRayParam() > tMod.getTauBranch(bNum-1, isPWave).getBotRayParam())
                                || tauBranch.getMinTurnRayParam() >= tMod.getTauBranch(bNum+1, isPWave).getTopRayParam())) {
                    // tau branch is high slowness, so turn is not possible, and
                    // no critical reflect, so do not add these branches
                    if (TauPConfig.DEBUG) {
                        Alert.debug("Warn, ray cannot turn in layer "+bNum+" due to high slowness layer "+tauBranch.getBotDepth());
                    }
                    endBranch = bNum-1;
                    bNum--;
                } else {
                    // can turn in bNum layer, so don't worry about shallower high slowness layers
                    // will split phase for shadow zones after finish proto phase
                    break;
                }
            }
            if(TauPConfig.DEBUG) {
                Alert.debug("after addToBranch: minRP="+minRayParam+"  maxRP="+maxRayParam);

            }
        } else if(endAction == REFLECT_UNDERSIDE || endAction == REFLECT_UNDERSIDE_CRITICAL) {
            endOffset = 0;
            isDownGoing = false;

            maxRayParam = calcMaxTransitRP(startBranch, endBranch, isPWave, prevEndAction, maxRayParam);
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch, isPWave).getTopRayParam());

            if (isPWave != nextIsPWave) {
                maxRayParam = Math.min(maxRayParam,
                        tMod.getTauBranch(endBranch, nextIsPWave).getMaxRayParam());
            }
            if (endAction == REFLECT_UNDERSIDE_CRITICAL) {
                minRayParam = Math.max(minRayParam, tMod.getTauBranch(endBranch-1, isPWave).getBotRayParam());
            }
        } else if(endAction == END) {
            endOffset = 0;
            isDownGoing = false;
            maxRayParam = calcMaxTransitRP(startBranch, endBranch, isPWave, prevEndAction, maxRayParam);
            // also must be less than ending slowness
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch, isPWave).getTopRayParam());

        } else if (endAction == END_DOWN) {
            endOffset = 0;
            isDownGoing = true;

            maxRayParam = calcMaxTransitRP(startBranch, endBranch, isPWave, prevEndAction, maxRayParam);
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch, isPWave).getBotRayParam());

        } else if(endAction == REFLECT_TOPSIDE || endAction == REFLECT_TOPSIDE_CRITICAL) {
            endOffset = 0;
            isDownGoing = true;

            maxRayParam = calcMaxTransitRP(startBranch, endBranch, isPWave, prevEndAction, maxRayParam);
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch, isPWave).getBotRayParam());
            if (isPWave != nextIsPWave) {
                maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch,
                        nextIsPWave).getMinTurnRayParam());
            }
            if (endAction == REFLECT_TOPSIDE_CRITICAL) {
                minRayParam = Math.max(minRayParam,
                        tMod.getTauBranch(endBranch+1, isPWave).getTopRayParam());
            }
        } else if(endAction == TRANSUP) {
            endOffset = -1;
            isDownGoing = false;
            maxRayParam = calcMaxTransitRP(startBranch, endBranch, isPWave, prevEndAction, maxRayParam);
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch, isPWave).getTopRayParam());
            maxRayParam = Math.min(maxRayParam,
                    tMod.getTauBranch(endBranch-1, nextIsPWave).getBotRayParam());
        } else if(endAction == TRANSDOWN) {
            endOffset = 1;
            isDownGoing = true;
            // ray must reach discon
            maxRayParam = calcMaxTransitRP(startBranch, endBranch, isPWave, prevEndAction, maxRayParam);
            // and cross into lower
            if (endBranch == tMod.getNumBranches()-1) {
                failNext(" Cannot TRANSDOWN center of earth, endBranch: "+endBranch+" == numBranchs: "+tMod.getNumBranches());
            }
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch+1, nextIsPWave).getTopRayParam());

        } else if(endAction == HEAD) {
            if (endBranch == tMod.getNumBranches()-1) {
                failNext(" Cannot head wave at center of earth, endBranch: "+endBranch+" == numBranchs: "+tMod.getNumBranches());
            }
            endOffset = 0;
            isDownGoing = true;
            // ray must reach discon, at turn/critical ray parameter
            maxRayParam = calcMaxTransitRP(startBranch, endBranch, isPWave, prevEndAction, maxRayParam);
            // and cross into lower layer, possible phase change
            maxRayParam = Math.min(maxRayParam,
                    tMod.getTauBranch(endBranch+1, nextIsPWave).getTopRayParam());
            minRayParam = Math.max(minRayParam, maxRayParam);
        } else if(endAction == DIFFRACT) {
            if (endBranch == tMod.getNumBranches()-1 ) {
                /*
                 * No diffraction if diffraction is at center of earth.
                 */
                failNext("No diffraction if diffraction is at center of earth.");
                minRayParam = -1;
                maxRayParam = -1;
            }
            endOffset = 0;
            isDownGoing = true;
            // ray must reach discon
            maxRayParam = calcMaxTransitRP(startBranch, endBranch, isPWave, prevEndAction, maxRayParam);
            // and propagate at the smallest turning ray param, may be different if phase conversion, ie SedPdiff
            if (isPWave != nextIsPWave) {
                maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch, nextIsPWave).getMinTurnRayParam());
            }
            // min rp same as max
            minRayParam = Math.max(minRayParam, maxRayParam);
            if (tMod.getTauBranch(endBranch, isPWave).isHighSlowness()) {
                // should diff be allowed if in neg slowness gradient at boundary???
                failNext("No diffraction as above branch is a high slowness gradient");
                minRayParam = -1;
                maxRayParam = -1;
            }
        } else if (endAction == TRANSUPDIFFRACT) {
            endOffset = -1;
            isDownGoing = false;

            maxRayParam = calcMaxTransitRP(startBranch, endBranch, isPWave, prevEndAction, maxRayParam);
            maxRayParam = Math.min(maxRayParam,
                    tMod.getTauBranch(endBranch-1, nextIsPWave).getMinTurnRayParam());
            minRayParam = Math.max(minRayParam, maxRayParam);
            if (tMod.getTauBranch(endBranch-1, nextIsPWave).isHighSlowness()) {
                /*
                 * No diffraction if above branch is a high slowness gradient.
                 */
                failNext("No transup diffraction as above branch is a high slowness gradient");
                minRayParam = -1;
                maxRayParam = -1;
            }
        } else {
            throw new TauModelException(getName()+": Illegal endAction: endAction="
                    + endAction);
        }
        SeismicPhaseSegment segment = new SeismicPhaseSegment(tMod, startBranch, endBranch,
                isPWave, endAction, isDownGoing, currLeg, minRayParam, maxRayParam);
        if ( ! isPWave &&  ! (currLeg.startsWith("K") || currLeg.equals("k"))) {
            // outer core K is treated as S wave as special case
            for(int i = Math.min(startBranch, endBranch); i <= Math.max(startBranch,endBranch); i++) {
                TauBranch tb = tMod.getTauBranch(i, isPWave);
                for (DepthRange fluidDR : tMod.getSlownessModel().fluidLayerDepths) {
                    if (tb.getTopDepth() >= fluidDR.topDepth && tb.getTopDepth() < fluidDR.botDepth
                            || tb.getBotDepth() > fluidDR.topDepth && tb.getBotDepth() <= fluidDR.botDepth) {
                        failNext("S wave branch "+currLeg+"("+isPWave+")"+" in "+getName()
                                +" is in fluid: "+tb+" "+fluidDR+" "+startBranch+" "+endBranch+" "+isDownGoing);
                    }
                }
            }
        }
        if(isDownGoing) {
            if (startBranch > endBranch) {
                // can't be downgoing as we are already below
                minRayParam = -1;
                maxRayParam = -1;
                failNext("can't be downgoing as we are already below: "+startBranch+" "+endBranch+" in "+getName());
            } else {
                if(TauPConfig.DEBUG) {
                    for(int i = startBranch; i <= endBranch; i++) {
                        Alert.debug("i=" + i + " isDownGoing=" + isDownGoing
                                + " isPWave=" + isPWave + " startBranch="
                                + startBranch + " endBranch=" + endBranch + " "
                                + endActionString(endAction));
                    }
                }
            }
        } else {
            if (startBranch < endBranch) {
                // can't be upgoing as we are already above
                minRayParam = -1;
                maxRayParam = -1;
                failNext("can't be upgoing as we are already above: "+startBranch+" "+endBranch+" "+currLeg+" in "+getName());
            } else {
                if(TauPConfig.DEBUG) {
                    for(int i = startBranch; i >= endBranch; i--) {
                        Alert.debug("i=" + i + " isDownGoing=" + isDownGoing
                                + " isPWave=" + isPWave + " startBranch="
                                + startBranch + " endBranch=" + endBranch + " "
                                + endActionString(endAction));
                    }
                }
            }
        }
        if(TauPConfig.DEBUG) {
            Alert.debug("after addToBranch: minRP="+minRayParam+"  maxRP="+maxRayParam+" endOffset="+endOffset+" isDownGoing="+isDownGoing);
        }
        add(segment);
        return segment;
    }

    protected double calcMaxTransitRP(int startBranch, int endBranch, boolean isPWave, PhaseInteraction prevEndAction, double maxRayParam) {
        if (prevEndAction != TURN) {
            // must make it all way from start to end
            for (int bnum = startBranch; bnum <= endBranch; bnum++) {
                maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(bnum, isPWave).getTopRayParam());
                maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(bnum, isPWave).getBotRayParam());
            }
        }
        return maxRayParam;
    }

    protected SeismicPhaseSegment addFlatBranch(boolean isPWave,
                                                PhaseInteraction prevEndAction,
                                                PhaseInteraction endAction,
                                                String currLeg) throws TauModelException {
        // special case, add "flat" segment along bounday
        switch (endAction) {
            case END:
            case END_DOWN:
            case FAIL:
            case DIFFRACTTURN:
            case TRANSDOWN:
            case TRANSUP:
                break;
            default:
                throw new TauModelException("End action for flat branch not allowed: "+endAction);
        }
        switch (prevEndAction) {
            case DIFFRACT:
            case TRANSUPDIFFRACT:
            case HEAD:
            case KMPS:
                break;
            case END:
            case END_DOWN:
            case FAIL:
                throw new TauModelException("Phase already finished: "+prevEndAction);
            default:
                throw new TauModelException("End action before flat branch not allowed: "+endAction);
        }
        int branch = calcStartBranch(currLeg);
        double minRayParam;
        double maxRayParam;
        boolean flatIsDownGoing = false;
        SeismicPhaseSegment flatSegment;
        if (prevEndAction == KMPS) {
            // dummy case for surface wave velocity
            double velocity = Double.parseDouble(currLeg.substring(0, currLeg.length() - 4));
            minRayParam = tMod.radiusOfEarth / velocity;
            maxRayParam = minRayParam;
            flatSegment = new SeismicPhaseSegment(tMod, branch, branch,
                    isPWave, endAction, flatIsDownGoing, currLeg, minRayParam, maxRayParam);

        } else {
            minRayParam = isEmpty() ? 0 : endSegment().minRayParam;
            if (isEmpty()) {
                throw new TauModelException("Cannot have flat leg as starting leg in phase: " + currLeg + " " + getName());
            }
            maxRayParam = endSegment().maxRayParam;
            if(TauPConfig.DEBUG) {
                Alert.debug("before addFlatBranch: minRP="+minRayParam+"  maxRP="+maxRayParam);
                Alert.debug("addFlatBranch( " + branch
                        + " endAction="+endActionString(endAction)+" "+currLeg+") isP:"+(isPWave?"P":"S"));

            }
            if (prevEndAction == HEAD) {
                double headRP = tMod.getTauBranch(branch,isPWave).getMaxRayParam();
                if (minRayParam > headRP || maxRayParam < headRP) {
                    // can't do head wave, no rp match
                    minRayParam = -1;
                    maxRayParam = -1;
                    return failNext("Head wave ray parameter, "+headRP
                            +", outside of min,max rayparameter for phase "+minRayParam+" "+maxRayParam);
                } else {
                    minRayParam = headRP;
                    maxRayParam = headRP;
                }
                flatSegment = new SeismicPhaseSegment(tMod, branch, branch,
                        isPWave, endAction, flatIsDownGoing, currLeg, minRayParam, maxRayParam);
            } else if (prevEndAction == DIFFRACT || prevEndAction == TRANSUPDIFFRACT){
                double diffRP = tMod.getTauBranch(branch,isPWave).getMinTurnRayParam();
                if (minRayParam > diffRP || maxRayParam < diffRP) {
                    // can't do diff wave, no rp match
                    minRayParam = -1;
                    maxRayParam = -1;
                    return failNext("Diffraction ray parameter, "+diffRP
                            +", outside of min,max rayparameter for phase "+minRayParam+" "+maxRayParam);
                } else {
                    minRayParam = diffRP;
                    maxRayParam = diffRP;
                }
                flatSegment = new SeismicPhaseSegment(tMod, branch, branch,
                        isPWave, endAction, flatIsDownGoing, currLeg, minRayParam, maxRayParam);
            } else {
                throw new TauModelException("Cannot addFlatBranch for prevEndAction: "+prevEndAction+" for "+currLeg);
            }
        }
        flatSegment.isFlat = true;
        flatSegment.prevEndAction = prevEndAction;


        if(TauPConfig.DEBUG) {
            Alert.debug("after addFlatBranch: minRP="+minRayParam+"  maxRP="+maxRayParam);
        }
        add(flatSegment);
        return flatSegment;
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
        if (segmentList.isEmpty()) {
            return name;
        } else if (segmentList.size() == 1 && segmentList.get(0).legName.endsWith(KMPS_CODE)) {
            return segmentList.get(0).legName;
        }
        TauModel tMod = segmentList.get(0).tMod;
        int idx = 0;
        SeismicPhaseSegment prev;
        SeismicPhaseSegment seg = null;
        SeismicPhaseSegment next = segmentList.get(0);
        while (idx < segmentList.size()) {
            prev = seg;
            seg = next;
            if (seg.endAction == FAIL) {
                name += "FAIL";
                return name;
            }
            if (idx < segmentList.size()-1) {
                next = segmentList.get(idx+1);
            }
            double botDepth = tMod.getTauBranch(seg.endBranch, seg.isPWave).getBotDepth();
            double topDepth = tMod.getTauBranch(seg.endBranch, seg.isPWave).getTopDepth();
            //name += " "+seg.startBranch+","+seg.endBranch+" ";
            if ( prev == null || prev.endAction != TURN
                    || prev.isPWave != seg.isPWave
                    || (! prev.legName.equalsIgnoreCase(seg.legName) && (prev.legName.equals("I") && seg.legName.equals("y")))) {
                String legName = legNameForSegment(tMod, seg);
                String nextLegName = legNameForSegment(tMod, next);
                if (zapED && legName.endsWith("ed")) {
                    if ((seg.endAction == DIFFRACT || seg.endAction == HEAD) && seg.isPWave == next.isPWave) {
                        legName = legName.substring(0, 1);
                    } else if ((seg.endAction == TRANSDOWN)
                            && (legName.startsWith("P") || legName.startsWith("S")) && !(nextLegName.startsWith("P") || nextLegName.startsWith("S"))
                            && (legName.startsWith("K")) && !(nextLegName.startsWith("K"))
                            && (legName.startsWith("I") || legName.startsWith("J")) && !(nextLegName.startsWith("I") || nextLegName.startsWith("J"))
                        //&& seg.isPWave == next.isPWave || ( seg.legName.equals("Sed") && nextLegName.equals("K"))
                        //&& !legName.startsWith(nextLegName.substring(0, 1))
                    ) {
                        legName = legName.substring(0, 1);
                    } else if ((seg.endAction == REFLECT_TOPSIDE || seg.endAction == REFLECT_TOPSIDE_CRITICAL ||
                            (seg.endAction == TRANSDOWN && (botDepth == tMod.cmbDepth || botDepth == tMod.iocbDepth)))) {
                        // ed not needed as legname changes
                        legName = legName.substring(0, 1);
                    }
                }
                if ((seg.endAction == END
                        || (seg.endAction == REFLECT_UNDERSIDE )
                        || ((seg.endAction == TURN || seg.endAction == DIFFRACTTURN
                            || seg.endAction == TRANSUP || seg.endAction == REFLECT_UNDERSIDE)
                            && next.endBranch == 0))
                        && (prev != null
                            && (prev.endAction == DIFFRACT || prev.endAction == DIFFRACTTURN
                                || prev.endAction == HEAD || prev.endAction ==TRANSUP)
                            && seg.isPWave == prev.isPWave && seg.legName.equals(prev.legName))
                ) {
                    legName = "";
                } else if (prev != null && prev.endAction == TURN && (prev.isPWave == seg.isPWave) && !seg.isDownGoing){
                    legName="";
                }
                if (seg.isFlat && (prev.isPWave == seg.isPWave)) {
                    // no phase change and flat leg, so no leg symbol needed
                    legName = "";
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
                    } else if (botDepth == tMod.mohoDepth) {
                        name += "vm";
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
                    } else if (botDepth == tMod.mohoDepth) {
                        name += m;
                    } else {
                        name += (int) (botDepth);
                    }
                    break;
                case REFLECT_UNDERSIDE:
                    if (topDepth == 0 || topDepth == tMod.cmbDepth || topDepth == tMod.iocbDepth) {
                        // no char as PP or KK or II
                    } else if (topDepth == tMod.mohoDepth) {
                        name += "^m";
                    } else {
                        name += "^" + (int) (topDepth);
                    }
                    break;
                case TURN:
                case DIFFRACTTURN:
                    //name += "U";
                    break;
                case TRANSDOWN:
                    if (seg.isFlat || botDepth == tMod.cmbDepth || botDepth == tMod.iocbDepth) {
                        // flat no char as already at depth
                        // no char as P,S -> K -> I,J
                    } else if (botDepth == tMod.mohoDepth) {
                        name += m;
                    } else {
                        name += (int)(botDepth);
                    }
                    break;
                case TRANSUP:
                    if (topDepth == tMod.cmbDepth || topDepth == tMod.iocbDepth || topDepth == tMod.surfaceDepth) {
                        // no char as P,S -> K -> I,J
                    } else if (topDepth == tMod.mohoDepth &&
                            (next.endAction == END || next.endAction == REFLECT_UNDERSIDE && next.endBranch == 0)
                            && seg.isPWave == next.isPWave
                    ) {
                        // no char finish at surface
                    } else if (topDepth == tMod.mohoDepth ) {
                        name += m;
                    } else {
                        name += (int) (topDepth);
                    }
                    break;
                case HEAD:
                    name += "n";
                    break;
                case DIFFRACT:
                case TRANSUPDIFFRACT:
                    String diff = "diff";
                    if (next != null && next.endAction == TRANSDOWN) {
                        diff = "diffdn";
                    }
                    if (seg.endAction==DIFFRACT) {
                        if ( botDepth == tMod.cmbDepth || botDepth == tMod.iocbDepth) {
                            name += diff;
                        } else {
                            name += (int) (botDepth)+diff;
                        }
                    } else if( seg.endAction==TRANSUPDIFFRACT) {
                        if (topDepth == tMod.cmbDepth || topDepth == tMod.iocbDepth) {
                            name += diff;
                        } else {
                            name += (int) (topDepth) + diff;
                        }
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
        return legNameForSegment(tMod, seg.endBranch, seg.isPWave, seg.isDownGoing, seg.isFlat, seg.endAction);
    }
    public static String legNameForSegment(TauModel tMod, int endBranch, boolean isPWave, boolean isDownGoing, boolean isFlat, PhaseInteraction endAction) {
        String name = SeismicPhaseWalk.legNameForTauBranch(tMod, endBranch, isPWave, isDownGoing, isFlat);
        if (endAction == TURN && name.endsWith("ed")) {
            name = name.substring(0, name.length()-2);
        }
        return name;
    }

    public List<Integer> branchNumSeg() {
        List<Integer> branchSeq = new ArrayList<>();
        for (SeismicPhaseSegment seg : segmentList) {
            if (seg.endAction == FAIL) {
                break;
            }
            int indexIncr = seg.isDownGoing ? 1 : -1;
            int finish = seg.endBranch + indexIncr;
            for (int branchNum = seg.startBranch; branchNum != finish; branchNum += indexIncr) {
                branchSeq.add(branchNum);
            }
        }
        return branchSeq;
    }

    public String branchNumSeqStr() {
        String out = "";
        for (Integer i : branchNumSeg()) {
            out += i+" ";
        }
        return out.trim();
    }

    public String branchNumSeqStrWithSegBreaks() {
        StringBuilder out = new StringBuilder();
        for (SeismicPhaseSegment seg : segmentList) {
            out.append(seg.legName);
            if (seg.endAction == FAIL) {
                break;
            }
            int indexIncr = seg.isDownGoing ? 1 : -1;
            int finish = seg.endBranch + indexIncr;
            for (int branchNum = seg.startBranch; branchNum != finish; branchNum += indexIncr) {
                out.append(" ").append(branchNum);
            }
            out.append(" "+seg.endAction).append(",");
        }
        return out.toString();
    }

    public SimpleSeismicPhase asSeismicPhase() throws TauModelException {
        return SeismicPhaseFactory.sumBranches(this);

    }

    public String getName() {
        if (phaseName != null ) {
            return phaseName;
        }
        return getPuristName();
    }

    public String getPuristName() {
        String pure = phaseNameForSegments();
        return pure;
    }

    public TauModel gettMod() {
        return tMod;
    }

    public String segmentListAsString() {
        StringBuffer sb = new StringBuffer();
        for (SeismicPhaseSegment seg : segmentList) {
            sb.append(", "+seg.startBranch+" as "+(seg.isPWave?"P":"S")+" "+seg.endBranch+" then "+seg.endAction);
        }
        return sb.substring(2);
    }
    final List<SeismicPhaseSegment> segmentList;

    TauModel tMod;

    boolean isFail = false;

    String failReason = null;

    String phaseName;

    final double receiverDepth;

    @Override
    public int compareTo(ProtoSeismicPhase o) {
        return phaseName.compareTo(o.phaseName);
    }
}
