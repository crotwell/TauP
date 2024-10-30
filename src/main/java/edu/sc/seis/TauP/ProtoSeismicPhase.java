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
        ProtoSeismicPhase proto = new ProtoSeismicPhase(new ArrayList<SeismicPhaseSegment>(), receiverDepth, phaseName);
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
            System.err.println("FAIL: "+reason+" within phase " + phaseName);
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
        String legName = legNameForSegment(tMod, startBranchNum, isPWave, isDownGoing, endAction);
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
            System.err.println("Fail: " + reason + " empty: " + segmentList.isEmpty());
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
                throw new IllegalArgumentException("End action cannot be FAIL or START: "+endAction);
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
        int endBranchNum = findEndDiscon(tMod, startBranchNum, isPWave, isDowngoing); // usually same as start
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
            if (seg.maxRayParam == 0) {
                throw new RuntimeException("maxRayParam is zero: "+phaseNameForSegments());
            }
            if (seg.endBranch == seg.tMod.getNumBranches()-1 && seg.isDownGoing && seg.endAction != TURN) {
                throw new RuntimeException("down not turn in innermost core layer: "
                        +phaseNameForSegments()+" "+seg.endBranch+" "+ seg.tMod.getNumBranches()+" "+seg.endAction);
            }
            if (prev != null) {
                String currLeg = seg.legName;
                if (seg.prevEndAction != prev.endAction) {
                    throw new RuntimeException("segment prevEndAction is not prev segment endAction: "+seg.prevEndAction+" "+prev.endAction);
                }
                if (prev.endAction == TRANSDOWN && prev.endBranch != seg.startBranch-1) {
                    throw new RuntimeException("prev is TRANSDOWN, but seg is not +1\n"+prev.endAction+"  "+seg.startBranch+"\n"+phaseNameForSegments());
                }
                if (prev.endAction == TURN &&
                        ( seg.endAction == TURN || seg.endAction == TRANSDOWN || seg.endAction == DIFFRACTTURN
                                || seg.endAction == END_DOWN || seg.endAction == REFLECT_TOPSIDE)) {
                    throw new RuntimeException("prev is TURN, but seg is "+seg.endAction);
                }
                if (prev.isDownGoing && seg.isDownGoing && prev.endBranch +1 != seg.startBranch) {
                    throw new TauModelException("Prev and Curr both downgoing but prev.endBranch+1 != seg.startBranch"
                    +" pdown: "+prev.isDownGoing+" currdown "+seg.isDownGoing+" && "+prev.endBranch+" +1 != "+seg.startBranch);
                }
                if (prev.isFlat) {
                    if (seg.isDownGoing) {
                        if (prev.endsAtTop() && prev.endBranch != seg.startBranch) {
                            throw new TauModelException(getName() + ": Flat Segment is ends at top, but start is not current branch: " + currLeg);
                        } else if (!prev.endsAtTop() && prev.endBranch != seg.startBranch - 1) {
                            throw new TauModelException(getName() + ": Flat Segment is ends at bottom, but start is not next deeper branch: " + currLeg);
                        }
                    } else {
                        if (prev.endsAtTop() && prev.endBranch != seg.startBranch +1) {
                            throw new TauModelException(getName() + ": Flat Segment is ends at top, but start is not next shallower branch: " + currLeg+" "+prev.endBranch +"!= "+seg.startBranch+"+1");
                        } else if (!prev.endsAtTop() && prev.endBranch != seg.startBranch) {
                            throw new TauModelException(getName() + ": Flat Segment is ends at bottom, but start is not current branch: " + currLeg+" "+prev.endBranch +"!= "+seg.startBranch);
                        }
                    }
                } else if (seg.isDownGoing) {
                    if (prev.endBranch > seg.startBranch) {
                        throw new TauModelException(getName()+": Segment is downgoing, but we are already below the start: "+currLeg);
                    }
                    if (prev.endAction == REFLECT_TOPSIDE || prev.endAction == REFLECT_TOPSIDE_CRITICAL) {
                        throw new TauModelException(getName()+": Segment is downgoing, but previous action was to reflect up: "+currLeg+" "+prev.endAction+" "+seg);
                    }
                    if (prev.endAction == TURN) {
                        throw new TauModelException(getName()+": Segment is downgoing, but previous action was to turn: "+currLeg);
                    }
                    if (prev.endAction == DIFFRACTTURN) {
                        throw new TauModelException(getName()+": Segment is downgoing, but previous action was to diff turn: "+currLeg);
                    }
                    if (prev.endAction == TRANSUP) {
                        throw new TauModelException(getName()+": Segment is downgoing, but previous action was to transmit up: "+currLeg);
                    }
                    if (prev.endBranch == seg.startBranch && prev.isDownGoing == false &&
                            ! (prev.endAction == REFLECT_UNDERSIDE || prev.endAction == REFLECT_UNDERSIDE_CRITICAL)) {
                        throw new TauModelException(getName()+": Segment "+currLeg+" is downgoing, but previous action was not to reflect underside: "+currLeg+" "+endActionString(prev.endAction));
                    }
                } else {
                    if (prev.endAction == REFLECT_UNDERSIDE || prev.endAction == REFLECT_UNDERSIDE_CRITICAL) {
                        throw new TauModelException(getName()+": Segment is upgoing, but previous action was to underside reflect down: "+currLeg);
                    }
                    if (prev.endAction == TRANSDOWN) {
                        throw new TauModelException(getName()+": Segment is upgoing, but previous action was  to trans down: "+currLeg);
                    }
                    if (prev.endBranch == seg.startBranch && prev.isDownGoing == true
                            && ! ( prev.endAction == TURN || prev.endAction == DIFFRACTTURN
                            || prev.endAction == DIFFRACT || prev.endAction == HEAD
                            || prev.endAction == REFLECT_TOPSIDE || prev.endAction == REFLECT_TOPSIDE_CRITICAL)) {
                        throw new TauModelException(getName()+": Segment is upgoing, but previous action was not to reflect topside: "+currLeg+" "+endActionString(prev.endAction));
                    }
                }
            }
            prev = seg;
        }
        if (TauPConfig.VERBOSE) {
            System.err.println("#### VALIDATE OK " + getName());
        }
    }

    public final SeismicPhaseSegment get(int i) {
        return segmentList.get(i);
    }

    public final boolean isEmpty() {
        return segmentList.isEmpty();
    }

    public final SeismicPhaseSegment endSegment() {
        if (isEmpty()) {throw new RuntimeException("Segment list is empty");}
        return segmentList.get(segmentList.size()-1);
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
            throw new IllegalArgumentException(getName()+": start branch outside range: (0-"+tMod.getNumBranches()+") "+startBranch);
        }
        if (endBranch < 0 || endBranch > tMod.getNumBranches()) {
            throw new IllegalArgumentException(getName()+": end branch outside range: "+endBranch);
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
        double minRayParam = isEmpty() ? 0 : endSegment().minRayParam;
        double maxRayParam;
        if (isEmpty()) {
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
                    throw new RuntimeException("Should not happen", e);
                }
            } else {
                throw new RuntimeException("Unknown starting max ray param for "+currLeg+" in "+getName()+" at "+tMod.getSourceDepth());
            }
        } else {
            maxRayParam = endSegment().maxRayParam;
        }
        if(TauPConfig.DEBUG) {
            System.err.println("before addToBranch: minRP="+minRayParam+"  maxRP="+maxRayParam);
            System.err.println("addToBranch( start=" + startBranch + " end=" + endBranch
                    + " endAction="+endActionString(endAction)+" "+currLeg+") isP:"+(isPWave?"P":"S"));

        }
        if(endAction == TURN || endAction == DIFFRACTTURN) {
            if (isPWave != nextIsPWave && endAction == TURN) {
                throw new TauModelException(getName()+" phase conversion not allowed for TURN");
            }
            endOffset = 0;
            isDownGoing = true;
            minRayParam = Math.max(minRayParam, tMod.getTauBranch(endBranch,
                            isPWave)
                    .getMinTurnRayParam());


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
                if (tMod.getSlownessModel().depthInHighSlowness(tMod.getTauBranch(bNum, isPWave).getTopDepth(),
                        minRayParam, isPWave) && (
                        bNum+1>=tMod.getNumBranches()
                                || minRayParam <= tMod.getTauBranch(bNum+1, isPWave).getMaxRayParam())) {
                    // tau branch is in high slowness, so turn is not possible, only
                    // non-critical reflect, so do not add these branches
                    if (TauPConfig.DEBUG) {
                        System.err.println("Warn, ray cannot turn in layer "+bNum+" due to high slowness layer at bottom depth "+tMod.getTauBranch(bNum, isPWave).getBotDepth());
                    }
                    endBranch = bNum-1;
                    bNum--;
                } else {
                    // can turn in bNum layer, so don't worry about shallower high slowness layers
                    break;
                }
            }
        } else if(endAction == REFLECT_UNDERSIDE || endAction == REFLECT_UNDERSIDE_CRITICAL) {
            endOffset = 0;
            isDownGoing = false;
            maxRayParam = Math.min(maxRayParam,
                    tMod.getTauBranch(endBranch, isPWave).getMaxRayParam());

            if (isPWave != nextIsPWave) {
                maxRayParam = Math.min(maxRayParam,
                        tMod.getTauBranch(endBranch, nextIsPWave).getMaxRayParam());
            }
            if (endAction == REFLECT_UNDERSIDE_CRITICAL) {
                try {
                    TauBranch endTauBranch = tMod.getTauBranch(endBranch, isPWave);
                    int slayAbove = tMod.getSlownessModel().layerNumberAbove(endTauBranch.getTopDepth(), isPWave);
                    SlownessLayer sLayer = tMod.getSlownessModel().getSlownessLayer(slayAbove, isPWave);
                    minRayParam = Math.max(minRayParam, sLayer.getBotP());
                } catch (NoSuchLayerException e) {
                    failNext("Unable to find layer for underside reflection: "+e.getMessage());
                }
            }
        } else if(endAction == END) {
            endOffset = 0;
            isDownGoing = false;
            maxRayParam = Math.min(maxRayParam,
                    tMod.getTauBranch(endBranch, isPWave).getMaxRayParam());
        } else if (endAction == END_DOWN) {
            endOffset = 0;
            isDownGoing = true;
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch,
                            isPWave)
                    .getMinTurnRayParam());
        } else if(endAction == REFLECT_TOPSIDE || endAction == REFLECT_TOPSIDE_CRITICAL) {
            endOffset = 0;
            isDownGoing = true;
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch,
                    isPWave).getMinTurnRayParam());
            if (isPWave != nextIsPWave) {
                maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch,
                        nextIsPWave).getMinTurnRayParam());
            }
            if (endAction == REFLECT_TOPSIDE_CRITICAL) {
                try {
                    TauBranch endTauBranch = tMod.getTauBranch(endBranch, isPWave);
                    int slayBelow = tMod.getSlownessModel().layerNumberBelow(endTauBranch.getBotDepth(), isPWave);
                    SlownessLayer sLayer = tMod.getSlownessModel().getSlownessLayer(slayBelow,isPWave);
                    minRayParam = Math.max(minRayParam,
                            sLayer.getTopP());

                } catch (NoSuchLayerException e) {
                    failNext("Unable to find layer for topside reflection: "+e.getMessage());
                }
            }
        } else if(endAction == TRANSUP) {
            endOffset = -1;
            isDownGoing = false;
            maxRayParam = Math.min(maxRayParam,
                    tMod.getTauBranch(endBranch, isPWave).getMaxRayParam());
            maxRayParam = Math.min(maxRayParam,
                    tMod.getTauBranch(endBranch-1, nextIsPWave).getMinTurnRayParam());
        } else if(endAction == TRANSDOWN) {
            endOffset = 1;
            isDownGoing = true;
            // ray must reach discon
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch, isPWave).getMinTurnRayParam());
            // and cross into lower
            if (endBranch == tMod.getNumBranches()-1) {
                failNext(" Cannot TRANSDOWN if endBranch: "+endBranch+" == numBranchs: "+tMod.getNumBranches());
            }
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch+1, nextIsPWave).getMaxRayParam());
        } else if(endAction == HEAD) {
            endOffset = 0;
            isDownGoing = true;
            // ray must reach discon, at turn/critical ray parameter
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch,
                            isPWave)
                    .getMinTurnRayParam());
            // and cross into lower layer, possible phase change
            maxRayParam = Math.min(maxRayParam,
                    tMod.getTauBranch(endBranch+1, nextIsPWave).getMaxRayParam());
            minRayParam = Math.max(minRayParam, maxRayParam);
        } else if(endAction == DIFFRACT) {
            endOffset = 0;
            isDownGoing = true;
            // ray must reach discon
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch, isPWave).getMinTurnRayParam());
            // and propagate at the smallest turning ray param, may be different if phase conversion, ie SedPdiff
            if (isPWave != nextIsPWave) {
                maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch, nextIsPWave).getMinTurnRayParam());
            }
            // min rp same as max
            minRayParam = Math.max(minRayParam, maxRayParam);
            double depth = tMod.getTauBranch(endBranch, isPWave).getBotDepth();
            if (depth == tMod.radiusOfEarth ||
                    tMod.getSlownessModel().depthInHighSlowness(depth - 1e-10, minRayParam, isPWave)) {
                /*
                 * No diffraction if diffraction is at zero radius or there is a high slowness zone.
                 */
                minRayParam = -1;
                maxRayParam = -1;
            }
        } else if (endAction == TRANSUPDIFFRACT) {
            endOffset = -1;
            isDownGoing = false;
            maxRayParam = Math.min(maxRayParam,
                    tMod.getTauBranch(endBranch, isPWave).getMaxRayParam());
            maxRayParam = Math.min(maxRayParam,
                    tMod.getTauBranch(endBranch-1, nextIsPWave).getMinTurnRayParam());
            minRayParam = Math.max(minRayParam, maxRayParam);
            double depth = tMod.getTauBranch(endBranch, isPWave).getBotDepth();
            if (depth == tMod.radiusOfEarth ||
                    tMod.getSlownessModel().depthInHighSlowness(depth - 1e-10, minRayParam, isPWave)) {
                /*
                 * No diffraction if diffraction is at zero radius or there is a high slowness zone.
                 */
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
                        System.err.println("i=" + i + " isDownGoing=" + isDownGoing
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
                        System.err.println("i=" + i + " isDownGoing=" + isDownGoing
                                + " isPWave=" + isPWave + " startBranch="
                                + startBranch + " endBranch=" + endBranch + " "
                                + endActionString(endAction));
                    }
                }
            }
        }
        if(TauPConfig.DEBUG) {
            System.err.println("after addToBranch: minRP="+minRayParam+"  maxRP="+maxRayParam+" endOffset="+endOffset+" isDownGoing="+isDownGoing);
        }
        add(segment);
        return segment;
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
                System.err.println("before addFlatBranch: minRP="+minRayParam+"  maxRP="+maxRayParam);
                System.err.println("addFlatBranch( " + branch
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
            System.err.println("after addFlatBranch: minRP="+minRayParam+"  maxRP="+maxRayParam);
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
                if (zapED) {
                    if ((seg.endAction == TRANSDOWN || seg.endAction == DIFFRACT|| seg.endAction == HEAD)
                            && legName.endsWith("ed")
                            && seg.isPWave == next.isPWave || ( seg.legName.equals("Sed") && nextLegName.equals("K"))
                            && !legName.startsWith(nextLegName.substring(0, 1))) {
                        legName = legName.substring(0, 1);
                    } else if ((seg.endAction == REFLECT_TOPSIDE || seg.endAction == REFLECT_TOPSIDE_CRITICAL ||
                            (seg.endAction == TRANSDOWN && (botDepth == tMod.cmbDepth || botDepth == tMod.iocbDepth)))
                            && legName.endsWith("ed")) {
                        // ed not needed as legname changes
                        legName = legName.substring(0, 1);
                    } else if ((seg.endAction == END
                            || (seg.endAction == REFLECT_UNDERSIDE && seg.endBranch == 0)
                            || ((seg.endAction == TURN || seg.endAction == DIFFRACTTURN
                                || seg.endAction == TRANSUP || seg.endAction == REFLECT_UNDERSIDE)
                                && next.endBranch == 0))
                            && (prev != null
                                && (prev.endAction == DIFFRACT || prev.endAction == DIFFRACTTURN
                                    || prev.endAction == HEAD || prev.endAction ==TRANSUP)
                                && seg.isPWave == prev.isPWave && seg.legName.equals(prev.legName))
                    ) {
                        legName = "";
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
                        name += "m";
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
                    if (botDepth == tMod.cmbDepth || botDepth == tMod.iocbDepth) {
                        // no char as P,S -> K -> I,J
                    } else if (botDepth == tMod.mohoDepth
                            && seg.isPWave != next.isPWave) {
                        name += "m";
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
                    } else if (topDepth == tMod.mohoDepth && seg.endAction == TRANSUP
                            && seg.isPWave != next.isPWave) {
                        name += "m";
                    } else {
                        name += (int) (topDepth);
                    }
                    break;
                case HEAD:
                    name += "n";
                    break;
                case DIFFRACT:
                    if (botDepth == tMod.cmbDepth || botDepth == tMod.iocbDepth) {
                        name += "diff";
                    } else {
                        name += (int) (botDepth)+"diff";
                    }
                    break;
                case TRANSUPDIFFRACT:
                    if (topDepth == tMod.cmbDepth || topDepth == tMod.iocbDepth) {
                        name += "diff";
                    } else {
                        name += (int) (topDepth)+"diff";
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
        return legNameForSegment(tMod, seg.endBranch, seg.isPWave, seg.isDownGoing, seg.endAction);
    }
    public static String legNameForSegment(TauModel tMod, int endBranch, boolean isPWave, boolean isDownGoing, PhaseInteraction endAction) {
        String name = SeismicPhaseWalk.legNameForTauBranch(tMod, endBranch, isPWave, isDownGoing);
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
        return out;
    }

    public SeismicPhase asSeismicPhase() throws TauModelException {
        return SeismicPhaseFactory.sumBranches(tMod, this);

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
            sb.append(", "+seg.startBranch+" "+seg.endBranch+" then "+seg.endAction);
        }
        return sb.substring(2).toString();
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
