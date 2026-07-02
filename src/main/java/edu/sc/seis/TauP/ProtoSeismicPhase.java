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
        if (!segmentList.isEmpty() && segmentList.get(0).prevEndAction == null) {
            segmentList.get(0).prevEndAction = SeismicPhaseSegment.prevEndActionForStart(segmentList.get(0).layerPropogationType);
        }
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

    /**
     * Starts a phase with no legs.
     *
     * @param phaseName name of phase
     * @param tMod tau model, which includes the source depth
     * @param receiverDepth depth of the receiver
     * @return proto phase ready for adding legs
     */
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

    public static ProtoSeismicPhase start(TauModel tMod,
                                          int startBranch,
                                          int endBranch,
                                          boolean isPWave,
                                          PhaseInteraction endAction,
                                          LayerPropogationType layerPropogationType,
                                          String legName,
                                          double minRayParam,
                                          double maxRayParam,
                                          double receiverDepth) {
        SeismicPhaseSegment startSeg =
                SeismicPhaseSegment.startingSegment(tMod,startBranch, endBranch, isPWave,
                        endAction, layerPropogationType, legName, minRayParam, maxRayParam);
        return new ProtoSeismicPhase(new ArrayList<>(List.of(startSeg)), receiverDepth);
    }

    public static ProtoSeismicPhase failNewPhase(TauModel tMod,
                                                 boolean isPWave,
                                                 LayerPropogationType layerPropogationType,
                                                 double receiverDepth,
                                                 String phaseName,
                                                 String reason) {
        ProtoSeismicPhase failed = startEmpty(phaseName, tMod, receiverDepth);
        int startBranchNum = tMod.getSourceBranch();
        failed.add(SeismicPhaseSegment.failSegment(tMod, startBranchNum, startBranchNum,
                        isPWave, layerPropogationType, phaseName));
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
                                                  LayerPropogationType layerPropogationType,
                                                  double receiverDepth) throws TauPException {
        int startBranchNum = tMod.getSourceBranch();
        if ( layerPropogationType == LayerPropogationType.UP || layerPropogationType == LayerPropogationType.DIFF) {
            startBranchNum = startBranchNum-1;
        }
        String legName = legNameForSegment(tMod, startBranchNum, isPWave, layerPropogationType, endAction);
        TauBranch startBranch = tMod.getTauBranch(startBranchNum, isPWave);
        double minRayParam = 0.0;
        double maxRayParam;
        SlownessModel sMod = tMod.getSlownessModel();
        switch (layerPropogationType) {
            case HEAD -> {
                if (tMod.isHeadWaveBranch(startBranchNum, isPWave, isPWave)) {
                    SlownessLayer slownessLayer = sMod.getSlownessLayer(sMod.layerNumberBelow(tMod.getSourceDepth(), isPWave), isPWave);
                    maxRayParam = slownessLayer.getTopP();
                    minRayParam = maxRayParam;
                } else {
                    throw new TauModelException("Boundary at "+tMod.getSourceDepth()+" cannot be a head wave");
                }
            }
            case DIFF -> {
                if (tMod.isDiffractionBranch(startBranchNum, isPWave)) {
                    SlownessLayer slownessLayer = sMod.getSlownessLayer(sMod.layerNumberAbove(tMod.getSourceDepth(), isPWave), isPWave);
                    maxRayParam = slownessLayer.getBotP();
                    minRayParam = maxRayParam;
                } else {
                    throw new TauModelException("Boundary at "+tMod.getSourceDepth()+" cannot be a diff wave");
                }
            }
            case DOWN ->  {
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
            }
            case UP -> {
                // upgoing
                if (tMod.getSourceDepth() == 0) {
                    throw new TauPException("Cannot be upgoing for zero depth source");
                }
                SlownessLayer slownessLayer = sMod.getSlownessLayer(sMod.layerNumberAbove(tMod.getSourceDepth(), isPWave), isPWave);
                maxRayParam = slownessLayer.getBotP();
                switch (endAction) {
                    case REFLECT_UNDERSIDE:
                    case TRANSUP:
                        maxRayParam = Math.max(maxRayParam, startBranch.getMinTurnRayParam());
                        break;
                    default:
                        throw new TauPException("Don't understand endAction " + endAction + " when upgoing");
                }
            }
            default -> throw new TauPException("Not imple type: "+layerPropogationType);
        }
        return start(tMod, startBranchNum, startBranchNum,
                    isPWave, endAction, layerPropogationType, legName, minRayParam, maxRayParam, receiverDepth);
    }


    public SeismicPhaseSegment failNext(String reason) {
        if (TauPConfig.DEBUG){
            Alert.debug("Fail: " + reason + " empty: " + segmentList.isEmpty());
        }
        SeismicPhaseSegment failSeg = SeismicPhaseSegment.failSegment(tMod);
        if (segmentList.isEmpty()) {
            failSeg.prevEndAction = START_DOWN;
        } else {
            failSeg.prevEndAction = segmentList.get(segmentList.size() - 1).endAction;
        }
        segmentList.add(failSeg);
        isFail = true;
        failReason = reason;
        return failSeg;
    }

    public ProtoSeismicPhase nextSegment(boolean isPWave,
                                         PhaseInteraction endAction) throws TauModelException {
        SeismicPhaseSegment endSeg = segmentList.isEmpty() ? null : segmentList.get(segmentList.size()-1);
        TauModel tMod = endSeg != null ? endSeg.getTauModel() : null;
        int priorEndBranchNum = endSeg != null ? endSeg.endBranch : -1;
        LayerPropogationType propTypeBeforeEndAction;
        switch (endAction) {
            case HEAD, DIFFRACT, TRANSDOWN, TURN,
                 REFLECT_TOPSIDE, REFLECT_TOPSIDE_CRITICAL, END_DOWN -> propTypeBeforeEndAction=LayerPropogationType.DOWN;

            case TRANSUP, REFLECT_UNDERSIDE, REFLECT_UNDERSIDE_CRITICAL, END, TRANSUPDIFFRACT -> propTypeBeforeEndAction=LayerPropogationType.UP;
            case DIFFRACTTURN, DIFFRACTDOWN -> propTypeBeforeEndAction=LayerPropogationType.DIFF;
            case HEADTURN -> propTypeBeforeEndAction=LayerPropogationType.HEAD;
            case FAIL -> {
                SeismicPhaseSegment nextSeg = SeismicPhaseSegment.failSegment(tMod, priorEndBranchNum, priorEndBranchNum,
                        isPWave, LayerPropogationType.DOWN, "");
                List<SeismicPhaseSegment> out = new ArrayList<>(segmentList);
                out.add(nextSeg);
                if (endSeg != null) {
                    nextSeg.prevEndAction = endSeg.endAction;
                }
                ProtoSeismicPhase failProto = new ProtoSeismicPhase(out, receiverDepth);
                failProto.isFail = true;
                return failProto;
            }
            case START_UP, START_DOWN, START_FLAT ->
                    throw new IllegalArgumentException("End action cannot be START: "+endAction);
            default ->
                    throw new IllegalArgumentException("End action case not yet impl: "+endAction);
        }
        int startBranchNum = nextStartBranch();
        // usually same as start unless cross source depth that is not a discon
        int endDisconBranchNum = findEndDiscon(tMod, startBranchNum, isPWave, propTypeBeforeEndAction);
        return nextSegment(isPWave, endDisconBranchNum, endAction);
    }

    public int nextStartBranch() throws TauModelException {
        if (segmentList.isEmpty()) {
            throw new TauModelException("Unable to calc next start for empty proto phase");
        }
        if (segmentList.isEmpty()) {
            throw new TauModelException("Unable to calc next start for empty proto phase");
        }
        SeismicPhaseSegment endSeg = segmentList.get(segmentList.size()-1);
        if (endSeg.endAction == FAIL) {
            throw new TauModelException("Unable to calc next start for FAIL proto phase");
        }

        return switch (getEndAction()) {
            case TRANSUP, HEADTURN, TRANSUPDIFFRACT -> {
                if (endSeg.endBranch == 0) {
                    throw new TauModelException(getName()+" TransUp when prev end is zero, prev: "
                            +endSeg.endBranch+" "+endSeg.endAction);
                }
                yield endSeg.endBranch-1;
            }
            case TRANSDOWN, HEAD, DIFFRACTDOWN -> endSeg.endBranch+1;
            default -> endSeg.endBranch;
        };
    }


    /**
     * Propogates a ray to the discontinuity given.
     * @param isPWave true for P, false for S
     * @param endDisconBranchNum branch number for the layer below the discontinuity
     * @param endAction end action when hitting the discontinuity
     * @return phase with one additional segment
     * @throws TauModelException
     */
    public ProtoSeismicPhase nextSegment(boolean isPWave,
                                         int endDisconBranchNum,
                                         PhaseInteraction endAction) throws TauModelException {
        if (segmentList.isEmpty()) {
            throw new IllegalArgumentException("Can't add segment to empty proto phase");
        }
        List<SeismicPhaseSegment> out = new ArrayList<>(segmentList);
        SeismicPhaseSegment endSeg = segmentList.isEmpty() ? null : segmentList.get(segmentList.size()-1);
        TauModel tMod = endSeg != null ? endSeg.getTauModel() : null;
        int priorEndBranchNum = endSeg != null ? endSeg.endBranch : -1;
        if (endAction == FAIL) {
            SeismicPhaseSegment nextSeg = SeismicPhaseSegment.failSegment(tMod, priorEndBranchNum, priorEndBranchNum,
                    isPWave, LayerPropogationType.DOWN, "");
            out.add(nextSeg);
            if (endSeg != null) {
                nextSeg.prevEndAction = endSeg.endAction;
            }
            ProtoSeismicPhase failProto = new ProtoSeismicPhase(out, receiverDepth);
            failProto.isFail = true;
            failProto.failReason = "no reason, end action was FAIL?";
            return failProto;
        }
        LayerPropogationType propTypeBeforeEndAction = switch (endAction) {
            case HEAD, DIFFRACT, TRANSDOWN, TURN,
                 REFLECT_TOPSIDE, REFLECT_TOPSIDE_CRITICAL, END_DOWN -> LayerPropogationType.DOWN;

            case TRANSUP, REFLECT_UNDERSIDE, REFLECT_UNDERSIDE_CRITICAL, END, TRANSUPDIFFRACT -> LayerPropogationType.UP;
            case DIFFRACTTURN, DIFFRACTDOWN -> LayerPropogationType.DIFF;
            case HEADTURN -> LayerPropogationType.HEAD;
            case START_UP, START_DOWN, START_FLAT ->
                throw new IllegalArgumentException("End action cannot be START: "+endAction);
            default ->
                throw new IllegalArgumentException("End action case not yet impl: "+endAction);
        };
        if (propTypeBeforeEndAction== LayerPropogationType.UP && endSeg != null && (endSeg.endBranch == 0 && (endSeg.endAction != TURN))) {
            SeismicPhaseSegment nextSeg = SeismicPhaseSegment.failSegment(tMod, priorEndBranchNum, priorEndBranchNum,
                    isPWave, propTypeBeforeEndAction, "");
            out.add(nextSeg);
            nextSeg.prevEndAction = endSeg.endAction;
            ProtoSeismicPhase outProto =  new ProtoSeismicPhase(out, receiverDepth);
            outProto.isFail = true;
            outProto.failReason = "phase upgoing at surface";
            return outProto;
        }
        int startBranchNum = nextStartBranch();
        if (endDisconBranchNum == 0 && (endAction == TRANSUP || endAction == HEADTURN)) {
            SeismicPhaseSegment nextSeg = SeismicPhaseSegment.failSegment(tMod, startBranchNum, endDisconBranchNum,
                    isPWave, propTypeBeforeEndAction, "");
            out.add(nextSeg);
            nextSeg.prevEndAction = endSeg.endAction;
            ProtoSeismicPhase outProto = new ProtoSeismicPhase(out, receiverDepth);
            outProto.isFail = true;
            outProto.failReason = "phase transup at surface";
            return outProto;
        }
        if ((propTypeBeforeEndAction==LayerPropogationType.DOWN&& endDisconBranchNum<startBranchNum)
            || propTypeBeforeEndAction==LayerPropogationType.UP&& endDisconBranchNum>startBranchNum) {
            SeismicPhaseSegment nextSeg = SeismicPhaseSegment.failSegment(tMod, startBranchNum, endDisconBranchNum,
                    isPWave, propTypeBeforeEndAction, "");
            out.add(nextSeg);
            nextSeg.prevEndAction = endSeg.endAction;
            ProtoSeismicPhase outProto = new ProtoSeismicPhase(out, receiverDepth);
            outProto.isFail = true;
            outProto.failReason = "end not compatible with direction: "+startBranchNum+" to "+endDisconBranchNum+" "+propTypeBeforeEndAction;
            return outProto;
        }

        if (endDisconBranchNum==0 && propTypeBeforeEndAction == LayerPropogationType.DOWN) {
            throw new RuntimeException("End discon cannot be 0 with prop is "+propTypeBeforeEndAction+" endDisconBranchNum="+endDisconBranchNum);
        }
        int endBranchNum = switch (propTypeBeforeEndAction) {
            case DOWN, DIFF -> endDisconBranchNum-1;
            default -> endDisconBranchNum;

        };

        String nextLegName = SeismicPhaseWalk.legNameForTauBranch(tMod, startBranchNum, isPWave, propTypeBeforeEndAction);
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
            case HEAD:
                maxRayParam = Math.min(maxRayParam, startBranch.getTopRayParam());
                break;
            case REFLECT_TOPSIDE:
            case REFLECT_TOPSIDE_CRITICAL:
            case TRANSUP:
            case HEADTURN:
            case DIFFRACTTURN:
            case DIFFRACTDOWN:
                maxRayParam = Math.min(maxRayParam, startBranch.getBotRayParam());
            default:
        }

        switch (endAction) {
            case REFLECT_TOPSIDE_CRITICAL:
                minRayParam = Math.max(minRayParam, endBranch.getMinRayParam());
            case TRANSDOWN:
            case DIFFRACT:
            case REFLECT_TOPSIDE:
            case HEAD:
            case END_DOWN:
                maxRayParam = Math.min(maxRayParam, endBranch.getMinTurnRayParam());
                maxRayParam = Math.min(maxRayParam, endBranch.getBotRayParam());
                break;
            case DIFFRACTTURN:
            case DIFFRACTDOWN:
                minRayParam = Math.max(minRayParam, endBranch.getBotRayParam());
                maxRayParam = Math.min(maxRayParam, endBranch.getBotRayParam());
                break;

            case TURN:
                minRayParam = Math.max(minRayParam, endBranch.getBotRayParam());
                maxRayParam = Math.min(maxRayParam, startBranch.getTopRayParam());
                break;
            case TRANSUP:
                maxRayParam = Math.min(maxRayParam, endBranch.getTopRayParam());
                break;
            case HEADTURN:
                minRayParam = Math.max(minRayParam, endBranch.getTopRayParam());
                maxRayParam = Math.min(maxRayParam, endBranch.getTopRayParam());
                break;
            case REFLECT_UNDERSIDE:
            case REFLECT_UNDERSIDE_CRITICAL:
            case END:
                maxRayParam = Math.min(maxRayParam, endBranch.getMaxRayParam());
                break;

        }

        SeismicPhaseSegment nextSeg;
        if (maxRayParam < minRayParam || (maxRayParam==minRayParam && countFlatLegs()==0 && !LayerPropogationType.isFlat(propTypeBeforeEndAction))) {
            // either ray param range is not single value, or there must be a flat (head, diff) leg somewhere in the path
            nextSeg = SeismicPhaseSegment.failSegment(tMod, startBranchNum, endBranchNum, isPWave, propTypeBeforeEndAction, nextLegName);
            out.add(nextSeg);
            nextSeg.prevEndAction = endSeg.endAction;
            ProtoSeismicPhase outProto = new ProtoSeismicPhase(out, receiverDepth);
            outProto.isFail = true;
            outProto.failReason = "no ray param compatible with boundary: maxRayParam < minRayParam: "+startBranchNum+" to "+endDisconBranchNum+" "+propTypeBeforeEndAction;
            return outProto;
        }
        nextSeg = new SeismicPhaseSegment(tMod,
                startBranchNum, endBranchNum, isPWave, endAction, propTypeBeforeEndAction, nextLegName,
                minRayParam, maxRayParam, endSeg.endAction);
        out.add(nextSeg);
        ProtoSeismicPhase proto = new ProtoSeismicPhase(out, receiverDepth);
        try {
            proto.validateSegList();
        } catch (Exception e) {
            proto.isFail= true;
            proto.failReason = "validation failed: "+e.getMessage();
        }
        return proto;
    }

    /**
     * Finds discontinuity branch in direction given. Branch number is below the discontinuity.
     * @param tMod the model
     * @param startBranchNum starting branch
     * @param isPWave true for P
     * @param layerPropogationType up or down
     * @return branch number below the discontinuity
     */
    public static int findEndDiscon(TauModel tMod, int startBranchNum, boolean isPWave, LayerPropogationType layerPropogationType) {
        int endBranchNum = startBranchNum;
        if (layerPropogationType == LayerPropogationType.DOWN) {
            while (endBranchNum < tMod.getNumBranches()-1
                    && tMod.isNoDisconDepth(tMod.getTauBranch(endBranchNum, isPWave).getBotDepth())) {
                endBranchNum += 1;
            }
            endBranchNum+=1; // discon is labeled by branch below discon depth
        } if (layerPropogationType == LayerPropogationType.UP) {
            // upgoing
            while (endBranchNum > 0 && tMod.isNoDisconDepth(tMod.getTauBranch(endBranchNum, isPWave).getTopDepth())) {
                endBranchNum -= 1;
            }
        }
        // head and diff no change start to end
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
        if (segmentList.get(0).prevEndAction==null) {
            throw new TauModelException("start segment prevEndAction is null: "+phaseNameForSegments());

        }
        SeismicPhaseSegment prev = null;
        for (SeismicPhaseSegment seg : segmentList) {
            if (seg.maxRayParam < 0) {
                throw new TauModelException("maxRayParam is zero: "+phaseNameForSegments());
            }
            if (seg.endBranch == seg.tMod.getNumBranches()-1 && seg.layerPropogationType==LayerPropogationType.DOWN && seg.endAction != TURN) {
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
                                || seg.endAction == HEADTURN || seg.endAction == DIFFRACTDOWN
                                || seg.endAction == END_DOWN || seg.endAction == REFLECT_TOPSIDE)) {
                    throw new TauModelException("prev is TURN, but seg is "+phaseNameForSegments()+" "+seg.endAction);
                }
                if (prev.layerPropogationType==LayerPropogationType.DOWN && seg.layerPropogationType==LayerPropogationType.DOWN && prev.endBranch +1 != seg.startBranch) {
                    throw new TauModelException("Prev and Curr both downgoing but prev.endBranch+1 != seg.startBranch "
                            +phaseNameForSegments()+" "
                            +" pdown: "+prev.layerPropogationType+" currdown "+seg.layerPropogationType
                            +" && "+prev.endBranch+" +1 != "+seg.startBranch);
                }
                if (prev.layerPropogationType==LayerPropogationType.UP && seg.layerPropogationType==LayerPropogationType.UP && prev.endBranch  != seg.startBranch+1) {
                    throw new TauModelException("Prev and Curr both upgoing but prev.endBranch != seg.startBranch+1 "
                            +phaseNameForSegments()+" "
                            +" pdown: "+prev.layerPropogationType+" currdown "+seg.layerPropogationType
                            +" && "+prev.endBranch+" != "+seg.startBranch+" +1");
                }
                if (LayerPropogationType.isFlat(prev.layerPropogationType)) {
                    if (seg.layerPropogationType==LayerPropogationType.DOWN) {
                        if (prev.endsAtTop() && prev.endBranch != seg.startBranch) {
                            throw new TauModelException(getName()
                                    + ": Flat Segment is ends at top, but start is not current branch: " + currLeg);
                        } else if (!prev.endsAtTop() && prev.endBranch != seg.startBranch - 1) {
                            throw new TauModelException(getName()
                                    + ": Flat Segment is ends at bottom, but start is not next deeper branch: " + currLeg+"\n"+prev.describe()+"\n"+seg.describe());
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
                } else if (seg.layerPropogationType==LayerPropogationType.DOWN) {
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
                    if (prev.endBranch == seg.startBranch && prev.layerPropogationType==LayerPropogationType.UP &&
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
                    if (prev.endAction == DIFFRACTDOWN) {
                        throw new TauModelException(getName()
                                +": Segment is upgoing, but previous action was  to diffract down: "+currLeg);
                    }
                    if (prev.endBranch == seg.startBranch && prev.layerPropogationType==LayerPropogationType.DOWN
                            && ! ( prev.endAction == TURN || prev.endAction == DIFFRACTTURN
                            || prev.endAction == DIFFRACT || prev.endAction == HEAD || prev.endAction == HEADTURN
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
                        + endSeg.endBranch + " != " + receiverBranch + " rec depth: " + receiverDepth+" "+branchNumSeqStrWithSegBreaks());
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
                        seg.layerPropogationType,
                        seg.legName,
                        hszRayParam,
                        seg.maxRayParam,
                        seg.prevEndAction
                );
                preShadowSegList.add(downSplitSeg);
                SeismicPhaseSegment upSplitSeg = new SeismicPhaseSegment(
                        next.tMod,
                        hszBranchNum-1,
                        next.endBranch,
                        next.isPWave,
                        next.endAction,
                        next.layerPropogationType,
                        next.legName,
                        hszRayParam,
                        seg.maxRayParam,
                        next.prevEndAction
                );
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
                        seg.layerPropogationType,
                        seg.legName,
                        seg.minRayParam,
                        Math.min(hszRayParam, transBranch.getBotRayParam()),
                        seg.prevEndAction
                );
                postShadowSegList.add(downTransSeg);

                // phase that turns below HSZ
                SeismicPhaseSegment downBelowSeg = new SeismicPhaseSegment(
                        seg.tMod,
                        hszBranchNum,
                        seg.endBranch,
                        seg.isPWave,
                        seg.endAction,
                        seg.layerPropogationType,
                        seg.legName,
                        seg.minRayParam,
                        hszRayParam,
                        downTransSeg.endAction
                );
                if (downBelowSeg.maxRayParam < downBelowSeg.minRayParam) {throw new RuntimeException("downBelowSeg max rp < min rp");}

                postShadowSegList.add(downBelowSeg);
                SeismicPhaseSegment upBelowSeg = new SeismicPhaseSegment(
                        next.tMod,
                        next.startBranch,
                        next.endBranch,
                        next.isPWave,
                        next.endAction,
                        next.layerPropogationType,
                        next.legName,
                        seg.minRayParam,
                        hszRayParam,
                        next.prevEndAction
                );
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
                        seg.layerPropogationType,
                        seg.legName,
                        hszRayParam,
                        seg.maxRayParam,
                        seg.prevEndAction
                );
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
                        next.layerPropogationType,
                        next.legName,
                        hszRayParam,
                        seg.maxRayParam,
                        next.prevEndAction
                );
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
                        seg.layerPropogationType,
                        seg.legName,
                        seg.minRayParam,
                        hszRayParam,
                        seg.prevEndAction
                );
                postShadowSegList.add(downTransSeg);

                // phase that turns below HSZ
                SeismicPhaseSegment downBelowSeg = new SeismicPhaseSegment(
                        seg.tMod,
                        hszBranchNum+1,
                        seg.endBranch,
                        seg.isPWave,
                        seg.endAction,
                        seg.layerPropogationType,
                        seg.legName,
                        seg.minRayParam,
                        hszRayParam,
                        downTransSeg.endAction
                );
                if (downBelowSeg.maxRayParam < downBelowSeg.minRayParam) {throw new RuntimeException("downBelowSeg max rp < min rp");}

                postShadowSegList.add(downBelowSeg);
                SeismicPhaseSegment upBelowSeg = new SeismicPhaseSegment(
                        next.tMod,
                        next.startBranch,
                        next.endBranch,
                        next.isPWave,
                        next.endAction,
                        next.layerPropogationType,
                        next.legName,
                        seg.minRayParam,
                        hszRayParam,
                        next.prevEndAction
                );
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

    public SeismicPhaseSegment getFlatSegment() {
        for (SeismicPhaseSegment seg : segmentList) {
            if (LayerPropogationType.isFlat(seg.layerPropogationType)) {
                return seg;
            }
        }
        return null;
    }

    public final PhaseInteraction getEndAction() {
        if (isEmpty()) {
            return START_DOWN;
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
        if (segmentList.get(0).layerPropogationType==LayerPropogationType.SURFACE) {
            return 1;
        }
        return countHeadLegs()+countDiffLegs();
    }

    public int countHeadLegs() {
        int countHeadLegs = 0;
        for (SeismicPhaseSegment seg : segmentList) {
            if (seg.layerPropogationType==LayerPropogationType.HEAD) {
                countHeadLegs++;
            }
        }
        return countHeadLegs;
    }

    public int countDiffLegs() {
        int countDiffLegs = 0;
        for (SeismicPhaseSegment seg : segmentList) {
            if (seg.layerPropogationType==LayerPropogationType.DIFF ) {
                countDiffLegs++;
            }
        }
        return countDiffLegs;
    }

    public final void add(SeismicPhaseSegment seg) {
        if (seg.prevEndAction == KMPS || seg.endAction == FAIL) {
            // keep KMPS and failures as is
        } else if (segmentList.isEmpty()) {
            if (seg.layerPropogationType==LayerPropogationType.HEAD) {
                seg.prevEndAction = HEAD;
            } else if (seg.layerPropogationType==LayerPropogationType.DIFF) {
                seg.prevEndAction = DIFFRACT;
            } else if (seg.layerPropogationType==LayerPropogationType.DIFF) {
                seg.prevEndAction = DIFFRACT;
            } else if (seg.layerPropogationType==LayerPropogationType.DOWN) {
                seg.prevEndAction = START_DOWN;
            } else if (seg.layerPropogationType==LayerPropogationType.UP) {
                seg.prevEndAction = START_UP;
            } else {
                throw new IllegalArgumentException("SURFACE not allowed as propogation type to add: "+
                        seg.layerPropogationType);
            }
        } else {
            seg.prevEndAction = endSegment().endAction;
        }
        segmentList.add(seg);
    }

    /**
     * Calculate the starting branch number for the current leg. This is the same as the
     * previous end branch number for reflections, and offset by one for transmissions.
     * @param currLeg current leg name
     * @return branch number
     */
    public int calcStartBranch(String currLeg) throws TauModelException {
        int currBranch;
        if (isFail) {
            throw new TauModelException("Phase has failed, cannot calc next branch");
        }
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
            return failNext("cannot TRANSUP with end branch zero, already at surface: "+endBranch);
        }
        if( ! isPWave && tMod.isFluidBranch(startBranch)) {
            // S wave in fluid
            return failNext("Attempt to have S wave in fluid layer in "+getName()+" "+startBranch+" to "+endBranch+" "+endActionString(endAction));
        }
        int endOffset;
        PhaseInteraction prevEndAction = isEmpty() ? PhaseInteraction.START_DOWN : endSegment().endAction;
        if (isEmpty()) {
            if (endAction == HEADTURN || endAction == DIFFRACTTURN || endAction == DIFFRACTDOWN) {
                prevEndAction = START_FLAT;
            } else if (PhaseInteraction.isUpgoingActionBefore(endAction)) {
                prevEndAction = START_UP;
            } else {
                prevEndAction = START_DOWN;
            }
        } else {
            prevEndAction = getEndAction();
        }
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
        LayerPropogationType layerPropogationType;
        if(endAction == TURN || endAction == DIFFRACTTURN) {
            if (isPWave != nextIsPWave && endAction == TURN) {
                throw new TauModelException(getName()+" phase conversion not allowed for TURN");
            }
            endOffset = 0;
            layerPropogationType= (endAction==DIFFRACTTURN) ? LayerPropogationType.DIFF : LayerPropogationType.DOWN;

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
            layerPropogationType = LayerPropogationType.UP;

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
            layerPropogationType = LayerPropogationType.UP;
            maxRayParam = calcMaxTransitRP(startBranch, endBranch, isPWave, prevEndAction, maxRayParam);
            // also must be less than ending slowness
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch, isPWave).getTopRayParam());

        } else if (endAction == END_DOWN) {
            endOffset = 0;
            layerPropogationType = LayerPropogationType.DOWN;

            maxRayParam = calcMaxTransitRP(startBranch, endBranch, isPWave, prevEndAction, maxRayParam);
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch, isPWave).getBotRayParam());

        } else if(endAction == REFLECT_TOPSIDE || endAction == REFLECT_TOPSIDE_CRITICAL) {
            endOffset = 0;
            layerPropogationType = LayerPropogationType.DOWN;

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
        } else if(endAction == TRANSUP || endAction == HEADTURN) {
            endOffset = -1;
            layerPropogationType = LayerPropogationType.UP;
            maxRayParam = calcMaxTransitRP(startBranch, endBranch, isPWave, prevEndAction, maxRayParam);
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch, isPWave).getTopRayParam());
            maxRayParam = Math.min(maxRayParam,
                    tMod.getTauBranch(endBranch-1, nextIsPWave).getBotRayParam());
        } else if(endAction == TRANSDOWN || endAction == DIFFRACTDOWN) {
            endOffset = 1;
            layerPropogationType= (endAction==DIFFRACTDOWN) ? LayerPropogationType.DIFF : LayerPropogationType.DOWN;
            // ray must reach discon
            maxRayParam = calcMaxTransitRP(startBranch, endBranch, isPWave, prevEndAction, maxRayParam);
            // and cross into lower
            if (endBranch == tMod.getNumBranches()-1) {
                return failNext(" Cannot TRANSDOWN center of earth, endBranch: "+endBranch+" == numBranchs: "+tMod.getNumBranches());
            }
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch+1, nextIsPWave).getTopRayParam());

        } else if(endAction == HEAD) {
            if (endBranch == tMod.getNumBranches()-1) {
                return failNext(" Cannot head wave at center of earth, endBranch: "+endBranch+" == numBranchs: "+tMod.getNumBranches());
            }
            endOffset = 0;
            layerPropogationType = LayerPropogationType.DOWN;
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
                return failNext("No diffraction if diffraction is at center of earth.");
            }
            endOffset = 0;
            layerPropogationType = LayerPropogationType.DOWN;
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
                return failNext("No diffraction as above branch is a high slowness gradient");
            }
        } else if (endAction == TRANSUPDIFFRACT) {
            endOffset = -1;
            layerPropogationType = LayerPropogationType.UP;

            maxRayParam = calcMaxTransitRP(startBranch, endBranch, isPWave, prevEndAction, maxRayParam);
            maxRayParam = Math.min(maxRayParam,
                    tMod.getTauBranch(endBranch-1, nextIsPWave).getMinTurnRayParam());
            minRayParam = Math.max(minRayParam, maxRayParam);
            if (tMod.getTauBranch(endBranch-1, nextIsPWave).isHighSlowness()) {
                /*
                 * No diffraction if above branch is a high slowness gradient.
                 */
                return failNext("No transup diffraction as above branch is a high slowness gradient");
            }
        } else {
            throw new TauModelException(getName()+": Illegal endAction: endAction="
                    + endAction);
        }
        SeismicPhaseSegment segment = new SeismicPhaseSegment(tMod, startBranch, endBranch,
                isPWave, endAction, layerPropogationType, currLeg, minRayParam, maxRayParam, prevEndAction);
        if ( ! isPWave &&  ! (currLeg.startsWith("K") || currLeg.equals("k"))) {
            // outer core K is treated as S wave as special case
            for(int i = Math.min(startBranch, endBranch); i <= Math.max(startBranch,endBranch); i++) {
                TauBranch tb = tMod.getTauBranch(i, isPWave);
                for (DepthRange fluidDR : tMod.getSlownessModel().fluidLayerDepths) {
                    if (tb.getTopDepth() >= fluidDR.topDepth && tb.getTopDepth() < fluidDR.botDepth
                            || tb.getBotDepth() > fluidDR.topDepth && tb.getBotDepth() <= fluidDR.botDepth) {
                        return failNext("S wave branch "+currLeg+"("+isPWave+")"+" in "+getName()
                                +" is in fluid: "+tb+" "+fluidDR+" "+startBranch+" "+endBranch+" "+layerPropogationType);
                    }
                }
            }
        }
        if(layerPropogationType == LayerPropogationType.DOWN) {
            if (startBranch > endBranch) {
                // can't be downgoing as we are already below
                return failNext("can't be downgoing as we are already below: "+startBranch+" "+endBranch+" in "+getName());
            } else {
                if(TauPConfig.DEBUG) {
                    for(int i = startBranch; i <= endBranch; i++) {
                        Alert.debug("i=" + i + " isDownGoing=" + layerPropogationType
                                + " isPWave=" + isPWave + " startBranch="
                                + startBranch + " endBranch=" + endBranch + " "
                                + endActionString(endAction));
                    }
                }
            }
        } else if(layerPropogationType == LayerPropogationType.UP) {
            if (startBranch < endBranch) {
                // can't be upgoing as we are already above
                return failNext("can't be upgoing as we are already above: "+startBranch+" "+endBranch+" "+currLeg+" in "+getName()+" "+layerPropogationType);
            } else {
                if(TauPConfig.DEBUG) {
                    for(int i = startBranch; i >= endBranch; i--) {
                        Alert.debug("i=" + i + " isDownGoing=" + layerPropogationType
                                + " isPWave=" + isPWave + " startBranch="
                                + startBranch + " endBranch=" + endBranch + " "
                                + endActionString(endAction));
                    }
                }
            }
        } else {
            if (startBranch != endBranch) {
                return failNext("can't be flat as start != end: "+startBranch+" "+endBranch+" "+currLeg+" in "+getName()+" "+layerPropogationType);
            }
        }
        if(TauPConfig.DEBUG) {
            Alert.debug("after addToBranch: minRP="+minRayParam+"  maxRP="+maxRayParam+" endOffset="+endOffset+" propogation="+layerPropogationType);
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
        if (isFail) {
            // phase has already failed, don't add more segments, return last (failed) segment.
            return endSegment();
        }
        switch (endAction) {
            case END:
            case END_DOWN:
            case FAIL:
            case DIFFRACTTURN:
            case DIFFRACTDOWN:
            case HEADTURN:
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
                throw new TauModelException("End action "+prevEndAction+" before flat branch not allowed: endAction: "+endAction+" in "+getName()+" "+branchNumSeqStrWithSegBreaks());
        }
        int branch = calcStartBranch(currLeg);
        double minRayParam;
        double maxRayParam;
        LayerPropogationType layerPropogationType;
        SeismicPhaseSegment flatSegment;
        if (prevEndAction == KMPS) {
            // dummy case for surface wave velocity
            layerPropogationType = LayerPropogationType.SURFACE;
            double velocity = Double.parseDouble(currLeg.substring(0, currLeg.length() - 4));
            minRayParam = tMod.radiusOfEarth / velocity;
            maxRayParam = minRayParam;
            flatSegment = new SeismicPhaseSegment(tMod, branch, branch,
                    isPWave, endAction, layerPropogationType, currLeg, minRayParam, maxRayParam, prevEndAction);

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
                layerPropogationType = LayerPropogationType.HEAD;
                double headRP = tMod.getTauBranch(branch,isPWave).getMaxRayParam();
                if (minRayParam > headRP || maxRayParam < headRP) {
                    // can't do head wave, no rp match
                    return failNext("Head wave ray parameter, "+headRP
                            +", outside of min,max rayparameter for phase "+minRayParam+" "+maxRayParam);
                } else {
                    minRayParam = headRP;
                    maxRayParam = headRP;
                }
                flatSegment = new SeismicPhaseSegment(tMod, branch, branch,
                        isPWave, endAction, layerPropogationType, currLeg, minRayParam, maxRayParam, prevEndAction);
            } else if (prevEndAction == DIFFRACT || prevEndAction == TRANSUPDIFFRACT){
                layerPropogationType = LayerPropogationType.DIFF;
                double diffRP = tMod.getTauBranch(branch,isPWave).getMinTurnRayParam();
                if (minRayParam > diffRP || maxRayParam < diffRP) {
                    // can't do diff wave, no rp match
                    return failNext("Diffraction ray parameter, "+diffRP
                            +", outside of min,max rayparameter for phase "+minRayParam+" "+maxRayParam);
                } else {
                    minRayParam = diffRP;
                    maxRayParam = diffRP;
                }
                flatSegment = new SeismicPhaseSegment(tMod, branch, branch,
                        isPWave, endAction, layerPropogationType, currLeg, minRayParam, maxRayParam, prevEndAction);
            } else {
                throw new TauModelException("Cannot addFlatBranch for prevEndAction: "+prevEndAction+" for "+currLeg);
            }
        }

        if(TauPConfig.DEBUG) {
            Alert.debug("after addFlatBranch: minRP="+minRayParam+"  maxRP="+maxRayParam);
        }
        add(flatSegment);
        return flatSegment;
    }


    public int calcInteractionNumber() {
        int count = 0;
        SeismicPhaseSegment prev = null;
        boolean hasFlatLegs = countFlatLegs()>0;
        boolean isFlatLegPWave = false;

        for (SeismicPhaseSegment seg : segmentList) {
            if (seg.getIsFlat()) {
                isFlatLegPWave = seg.isPWave;
            }
        }
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
                case DIFFRACT:
                case HEAD:
                    count++;
                    break;
                case TURN:
                    if (hasFlatLegs && seg.isPWave == isFlatLegPWave) {
                        // a turn for a degenerate phase is a diff or head if wavetype is same, ie PPdiff
                        count++;
                    }
                    break;
            }
            if (prev != null && prev.isPWave != seg.isPWave) {
                count++;
            }
            prev = seg;
        }
        return count;
    }

    public String zapEDIfPossible(SeismicPhaseSegment seg, SeismicPhaseSegment next, String legName) {
        String nextLegName = next.getLegName();
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
                (seg.endAction == TRANSDOWN && (seg.getEndDepth() == tMod.cmbDepth || seg.getEndDepth() == tMod.iocbDepth)))) {
            // ed not needed as legname changes
            legName = legName.substring(0, 1);
        }
        return legName;
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

        SeismicPhaseSegment flatSeg = getFlatSegment();

        boolean prevAddedDepthToName = false;
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
            //name += " "+seg.startBranch+","+seg.endBranch+" "
            String legName = legNameForSegment(tMod, seg);;
            legName = zapED ? zapEDIfPossible(seg, next, legName) : legName;

            if (prev == null
                    || prevAddedDepthToName
                    || prev.isPWave != seg.isPWave
            ) {
                // add legname for inital leg, or on phase change, or on legname change
            } else if ((prev.endAction==REFLECT_TOPSIDE|| prev.endAction==REFLECT_TOPSIDE_CRITICAL)
                    && name.endsWith("diff") && flatSeg!= null
                    && prev.isPWave==flatSeg.isPWave&&prev.getEndDepth()==flatSeg.getEndDepth()
                    && !LayerPropogationType.isFlat(prev.layerPropogationType)
            ) {
                // like PcpPdiff changed to PdiffPdiff, so no legname
                // previous leg converted reflection to diff
                legName = "";
            } else if (prev.legName.substring(0,1).equals("I") && seg.legName.substring(0,1).equals("y") && prev.getEndAction()==TURN) {
                // special case, I TURN y should be just I
                legName = "";
            } else if (!seg.legName.substring(0,1).equalsIgnoreCase(prev.legName.substring(0,1))) {
                // leg change name, so keep
            } else if (seg.endAction == END
                    && (prev.endAction == DIFFRACTTURN || prev.endAction == HEADTURN)
                    && (seg.isPWave == prev.isPWave)
            ) {
                // no leg if up leg after head like Pn or diff like Sdiff
                legName = "";
            } else if (prev.endAction == TURN || prev.endAction == DIFFRACTTURN) {
                // no leg addition to name after TURN, as no interaction and no phase change allowed
                legName = "";
            } else if (prev.endAction == HEADTURN && prev.isPWave == seg.isPWave
                    && seg.legName.substring(0,1).equalsIgnoreCase(prev.legName.substring(0,1))) {
                // no legname change after head wave, as long as no leg name change like PK2889np
                legName = "";
            } else if (prev.endAction == REFLECT_TOPSIDE || prev.endAction == REFLECT_TOPSIDE_CRITICAL) {
            } else if (prev.endAction == REFLECT_UNDERSIDE || prev.endAction == REFLECT_UNDERSIDE_CRITICAL) {
                // keep legname after reflection
            } else if (prev.endAction == DIFFRACTDOWN) {
                // keep after diffdn
            } else {
                // other cases, I don't think we need to keep the legname
                legName = "";
            }
            if (!legName.isEmpty() && seg.layerPropogationType == LayerPropogationType.UP) {
                // upgoing change P to p, S to s, etc
                if (legName.substring(0,1).equals("I")) {
                    legName = "y"+legName.substring(1);
                } else {
                    legName = legName.substring(0,1).toLowerCase()+legName.substring(1);
                }
            }

            String prevDepthToName = "";
            prevAddedDepthToName = false;
            switch (seg.endAction) {
                case REFLECT_TOPSIDE:
                    if (flatSeg!= null && flatSeg!=seg && seg.isPWave==flatSeg.isPWave&&seg.getEndDepth()==flatSeg.getEndDepth()) {
                        // looks like PcpPdiff, so purist is PdiffPdiff
                        prevDepthToName += diffStringForSeg(seg, prev, botDepth);
                    } else if (botDepth == tMod.cmbDepth) {
                        prevDepthToName += "c";
                    } else if (botDepth == tMod.iocbDepth) {
                        prevDepthToName += "i";
                    } else if (botDepth == tMod.mohoDepth) {
                        prevDepthToName += "vm";
                    } else {
                        prevDepthToName += "v" + (int) (Math.round(botDepth));
                    }
                    break;
                case REFLECT_TOPSIDE_CRITICAL:
                    if (flatSeg!= null && flatSeg!=seg && seg.isPWave==flatSeg.isPWave&&seg.getEndDepth()==flatSeg.getEndDepth()) {
                        // looks like PVcpPdiff, so purist is PdiffPdiff
                        prevDepthToName += diffStringForSeg(seg, prev, botDepth);
                    } else {
                        prevDepthToName += "V";
                        if (botDepth == tMod.cmbDepth) {
                            prevDepthToName += "c";
                        } else if (botDepth == tMod.iocbDepth) {
                            prevDepthToName += "i";
                        } else if (botDepth == tMod.mohoDepth) {
                            prevDepthToName += m;
                        } else {
                            prevDepthToName += (int) (botDepth);
                        }
                    }
                    break;
                case REFLECT_UNDERSIDE:
                    if (topDepth == 0 || topDepth == tMod.cmbDepth || topDepth == tMod.iocbDepth) {
                        // no char as PP or KK or II
                    } else if (topDepth == tMod.mohoDepth) {
                        prevDepthToName += "^m";
                    } else {
                        prevDepthToName += "^" + (int) (Math.round(topDepth));
                    }
                    break;
                case TURN:
                    if (flatSeg!= null && seg.isPWave==flatSeg.isPWave&&seg.getEndDepth()==flatSeg.getEndDepth()) {
                        // looks like PPdiff, so purist is PdiffPdiff
                        prevDepthToName += diffStringForSeg(seg, prev, botDepth);
                    }
                    break;
                case DIFFRACTTURN:
                case DIFFRACTDOWN:
                    prevDepthToName += diffStringForSeg(seg, prev, botDepth);
                    break;
                case HEADTURN:
                    //name += "U";
                    if (topDepth == tMod.mohoDepth) {
                        prevDepthToName += "n";
                    } else {
                        prevDepthToName += (int) (Math.round(topDepth)) +"n";
                    }
                    break;
                case TRANSDOWN:
                    if (LayerPropogationType.isFlat(seg.layerPropogationType) || botDepth == tMod.cmbDepth || botDepth == tMod.iocbDepth) {
                        // flat no char as already at depth
                        // no char as P,S -> K -> I,J
                    } else if (botDepth == tMod.mohoDepth) {
                        prevDepthToName += m;
                    } else {
                        prevDepthToName += (int)(Math.round(botDepth));
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
                        prevDepthToName += m;
                    } else {
                        prevDepthToName += (int) (Math.round(topDepth));
                    }
                    break;
                case HEAD:
                    break;
                case DIFFRACT:
                case TRANSUPDIFFRACT:

                    break;
                case END:
                case END_DOWN:
                    break;
                default:
                    prevDepthToName += seg.endAction.name();
            }
            switch (seg.endAction) {
                case HEADTURN -> {
                    if (topDepth == tMod.mohoDepth) {
                        prevAddedDepthToName = false;
                    }
                }
                case DIFFRACTTURN, DIFFRACTDOWN -> {
                    if (botDepth == tMod.cmbDepth || botDepth == tMod.iocbDepth) {
                        prevAddedDepthToName = false;
                    }
                }
                case REFLECT_TOPSIDE, REFLECT_TOPSIDE_CRITICAL -> {
                    if (flatSeg != null && flatSeg != seg && seg.isPWave == flatSeg.isPWave && seg.getEndDepth() == flatSeg.getEndDepth()) {
                        // looks like PVcpPdiff, so purist is PdiffPdiff
                        prevAddedDepthToName = false;
                    }
                }
                case TURN -> {
                    if (flatSeg != null && seg.isPWave == flatSeg.isPWave && seg.getEndDepth() == flatSeg.getEndDepth()) {
                        // looks like PPdiff, so purist is PdiffPdiff
                        prevAddedDepthToName = false;
                    }
                }
                default -> {
                    prevAddedDepthToName = !prevDepthToName.isEmpty();
                }
            }
            name += legName + prevDepthToName;
            idx++;
        }
        return name;
    }

    private String diffStringForSeg(SeismicPhaseSegment seg, SeismicPhaseSegment prev, double botDepth) {
        String diff = "diff";
        if (seg.endAction == DIFFRACTDOWN) {
            diff = "diffdn";
        }
        String out = "";
        if ( botDepth == tMod.cmbDepth || botDepth == tMod.iocbDepth) {
            out = diff;
        } else {
            out = (int) (Math.round(botDepth))+diff;
        }
        return out;
    }

    public static String legNameForSegment(TauModel tMod, SeismicPhaseSegment seg) {
        return legNameForSegment(tMod, seg.endBranch, seg.isPWave, seg.layerPropogationType, seg.endAction);
    }
    public static String legNameForSegment(TauModel tMod, int endBranch, boolean isPWave, LayerPropogationType layerPropogationType, PhaseInteraction endAction) {
        String name = SeismicPhaseWalk.legNameForTauBranch(tMod, endBranch, isPWave, layerPropogationType);
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
            switch (seg.layerPropogationType) {
                case HEAD, SURFACE, DIFF ->  branchSeq.add(seg.startBranch);
                case UP, DOWN -> {
                    int indexIncr = seg.layerPropogationType==LayerPropogationType.DOWN ? 1 : -1;
                    int finish = seg.endBranch + indexIncr;
                    for (int branchNum = seg.startBranch; branchNum != finish; branchNum += indexIncr) {
                        branchSeq.add(branchNum);
                    }
                }
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
                out.append(" FAIL");
                break;
            }
            int indexIncr = seg.layerPropogationType==LayerPropogationType.DOWN ? 1 : -1;
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
