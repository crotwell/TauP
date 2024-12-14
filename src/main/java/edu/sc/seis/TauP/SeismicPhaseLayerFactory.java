package edu.sc.seis.TauP;

import java.util.List;

import static edu.sc.seis.TauP.LegPuller.extractBoundaryId;
import static edu.sc.seis.TauP.PhaseInteraction.*;
import static edu.sc.seis.TauP.PhaseSymbols.*;
import static edu.sc.seis.TauP.PhaseSymbols.isDiffractedDown;

public class SeismicPhaseLayerFactory {

    public SeismicPhaseLayerFactory(SeismicPhaseFactory baseFactory, String layerName, int topBranchNum, int botBranchNum) {
        this.baseFactory = baseFactory;
        this.tMod = baseFactory.tMod;
        this.layerName = layerName;
        this.topBranchNum = topBranchNum;
        if (topBranchNum == -1) {
            this.topDepth = 0;
        } else if (topBranchNum == tMod.getNumBranches()) {
            this.topDepth = tMod.getRadiusOfEarth();
        } else {
            this.topDepth = tMod.getTauBranch(topBranchNum, true).getTopDepth();
        }
        this.botBranchNum = botBranchNum;

        if (botBranchNum == -1) {
            this.botDepth = 0;
        } else if (botBranchNum == tMod.getNumBranches()) {
            this.botDepth = tMod.getRadiusOfEarth();
        } else {
            this.botDepth = tMod.getTauBranch(botBranchNum, true).getBotDepth();
        }
    }

    public static SeismicPhaseLayerFactory crustMantleFactory(SeismicPhaseFactory baseFactory) {
        String layerName = "crust/mantle";
        if (baseFactory.tMod.isDegenerateCrustMantle()) {
            // no crust or mantle, so no P or S
            String reason = "Model with no mantle, cmb at surface";
            return new SeismicPhaseLayerFactoryAllFail(baseFactory, layerName,
                    -1,
                    -1,
                    reason);
        }
        SeismicPhaseLayerFactory factory = new SeismicPhaseLayerFactory(baseFactory,
                layerName,
                0,
                baseFactory.tMod.getCmbBranch()-1);
        factory.p_leg = PhaseSymbols.P;
        factory.up_p_leg = PhaseSymbols.p;
        factory.s_leg = PhaseSymbols.S;
        factory.up_s_leg = PhaseSymbols.s;
        return factory;
    }

    public static SeismicPhaseLayerFactory outerCoreFactory(SeismicPhaseFactory baseFactory) {
        String layerName = "outer core";
        if (baseFactory.tMod.isDegenerateOuterCore()) {
            // cmb is center of earth, no core
            String reason = "Model with no core, cmb at iocb";
            return new SeismicPhaseLayerFactoryAllFail(baseFactory, layerName,
                    baseFactory.tMod.getCmbBranch(),
                    baseFactory.tMod.getIocbBranch()-1, reason);
        }
        SeismicPhaseLayerFactory factory = new SeismicPhaseLayerFactory(baseFactory,
                layerName,
                baseFactory.tMod.getCmbBranch(),
                baseFactory.tMod.getIocbBranch()-1);
        factory.p_leg = PhaseSymbols.K;
        factory.up_p_leg = PhaseSymbols.k;
        factory.s_leg = '.';
        factory.up_s_leg = '.';
        return factory;
    }

    public static SeismicPhaseLayerFactory innerCoreFactory(SeismicPhaseFactory baseFactory) {
        String layerName = "inner core";
        if (baseFactory.tMod.isDegenerateInnerCore()) {
            String reason = "Model with no inner core, iocb at center of earth";
            return new SeismicPhaseLayerFactoryAllFail(baseFactory, layerName,
                    baseFactory.tMod.getIocbBranch(),
                    baseFactory.tMod.getNumBranches()-1,
                    reason);
        }
        SeismicPhaseLayerFactory factory = new SeismicPhaseLayerFactory(baseFactory,
                layerName,
                baseFactory.tMod.getIocbBranch(),
                baseFactory.tMod.getNumBranches()-1);
        factory.p_leg = PhaseSymbols.I;
        factory.up_p_leg = PhaseSymbols.y;
        factory.s_leg = PhaseSymbols.J;
        factory.up_s_leg = PhaseSymbols.j;
        return factory;
    }

    public static List<SeismicPhaseLayerFactory> createFactory(SeismicPhaseFactory baseFactory) {
        SeismicPhaseLayerFactory crustMantle = crustMantleFactory(baseFactory);
        SeismicPhaseLayerFactory outerCore = outerCoreFactory(baseFactory);
        SeismicPhaseLayerFactory innerCore = innerCoreFactory(baseFactory);
        List<SeismicPhaseLayerFactory> factoryList = List.of(crustMantle, outerCore, innerCore);
        crustMantle.aboveLayerFactory = new SeismicPhaseLayerFactoryAllFail(baseFactory, "surface of earth",
                0, 0, "No layers above surface");
        crustMantle.belowLayerFactory = outerCore;
        outerCore.aboveLayerFactory = crustMantle;
        outerCore.belowLayerFactory = innerCore;
        innerCore.aboveLayerFactory = outerCore;
        innerCore.belowLayerFactory = new SeismicPhaseLayerFactoryAllFail(baseFactory, "center of earth",
                baseFactory.tMod.getNumBranches(), baseFactory.tMod.getNumBranches(), "No layers below inner core");
        return List.of(crustMantle, outerCore, innerCore);
    }

    public ProtoSeismicPhase parse(ProtoSeismicPhase proto,
                                   String prevLeg, String currLeg, String nextLeg, String nextNextLeg,
                                   boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum) throws TauModelException {
        if (isExclusiveDowngoingSymbol(currLeg)) {
            /* Deal with P and S exclusively downgoing case . */
            proto = currLegIsExclusiveDowngoing(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
        } else if (isUpgoingSymbol(currLeg)) {
            /* Deal with p and s case . */
            proto = currLegIsUpLeg(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
        } else if (currLeg.equals(""+p_leg) || currLeg.equals(""+s_leg)) {
            /* Now deal with plain P and S case. */
            //special, need nextnextleg too
            proto = currLegIsDownLeg(proto, prevLeg, currLeg, nextLeg, nextNextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
        } else if (isDiffracted(currLeg) || isDiffractedDown(currLeg)) {
            proto = currLegIsDiffracted(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
        } else if (isHead(currLeg)) {
            currLegIsHead(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
        } else if ((currLeg.equals("Pg") || currLeg.equals("Sg"))) {
            // dumb special case crustal P and S
            proto = currLegIs_Pg_Sg(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
        } else {
            String reason = "parse() failed, Unknown phase in "+layerName+": "+currLeg;
            proto.failNext(reason);
            return proto;
        }
        return proto;
    }

    ProtoSeismicPhase currLegIsExclusiveDowngoing(ProtoSeismicPhase proto,
                                                  String prevLeg, String currLeg, String nextLeg,
                                                  boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = baseFactory.calcStartBranch(proto, currLeg);
        if(nextLeg.equals(END_CODE)) {
            if (baseFactory.receiverDepth > 0) {
                endAction = END_DOWN;
                proto.addToBranch(baseFactory.downgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
            } else {
                String reason = "impossible except for 0 dist 0 source depth which can be called p or P";
                return baseFactory.failWithMessage(proto, reason);
            }

        } else if(PhaseSymbols.isDiffracted(nextLeg) && (nextLeg.charAt(0) == p_leg || nextLeg.charAt(0) == s_leg)) {
            endAction = DIFFRACT;
            proto.addToBranch(
                    botBranchNum,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if (nextLeg.endsWith(HEAD_CODE) && nextLeg.length() > 1) {
            String numString = extractBoundaryId(nextLeg, 0, false);
            double headDepth = Double.parseDouble(numString);
            int disconBranch = LegPuller.closestDisconBranchToDepth(tMod, numString, depthTolerance);
            if ( ! tMod.isHeadWaveBranch(disconBranch, isPWave)) {
                return baseFactory.failWithMessage(proto,"Unable to head wave, "+ currLeg+", "
                        + disconBranch +", "+headDepth+ " is not positive velocity discontinuity.");
            }

            if (!validateDisconWithinLayers(proto, disconBranch-1, currLeg)) {
                return proto;
            }
            endAction = HEAD;
            proto.addToBranch(
                    disconBranch-1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if (isDiffracted(nextLeg) || isDiffractedDown(nextLeg)) {
            String numString = extractBoundaryId(nextLeg, 0, false);
            int disconBranch = LegPuller.closestDisconBranchToDepth(tMod, numString, depthTolerance);
            if (!validateDisconWithinLayers(proto, disconBranch-1, currLeg)) {
                return proto;
            }
            endAction = DIFFRACT;
            proto.addToBranch(
                    disconBranch - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.charAt(0) == getBelowPLegSymbol() || nextLeg.charAt(0) == getBelowSLegSymbol()) {
            endAction = TRANSDOWN;
            proto.addToBranch(
                    botBranchNum,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.equals("m")) {
            endAction = TRANSDOWN;
            proto.addToBranch(
                    tMod.getMohoBranch() - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.equals("c") || nextLeg.equals("i")) {
            int disconBranch = LegPuller.closestDisconBranchToDepth(tMod, nextLeg, depthTolerance);
            if (!validateDisconWithinLayers(proto, disconBranch-1, currLeg)) {
                return proto;
            }
            endAction = REFLECT_TOPSIDE;
            proto.addToBranch(
                    disconBranch - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if( LegPuller.isBoundary(nextLeg)) {
            // but not m, c or i
            int disconBranch = LegPuller.closestDisconBranchToDepth(tMod, nextLeg, depthTolerance);

            if (!validateDisconWithinLayers(proto, disconBranch-1, currLeg)) {
                return proto;
            }
            endAction = TRANSDOWN;
            proto.addToBranch(
                    disconBranch - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(PhaseSymbols.isReflectSymbol(nextLeg)) {
            if (isCriticalReflectSymbol(nextLeg)) {
                endAction = REFLECT_TOPSIDE_CRITICAL;
            } else {
                endAction = REFLECT_TOPSIDE;
            }
            int disconBranch = LegPuller.closestDisconBranchToDepth(tMod,
                    nextLeg.substring(1), depthTolerance);
            if(currBranch <= disconBranch - 1) {
                if(!validateDisconWithinLayers(proto, disconBranch-1, currLeg)) {
                    return proto;
                }
                proto.addToBranch(
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                return baseFactory.failWithMessage(proto," Phase not recognized in "+layerName+": "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch
                        + " < disconBranch=" + disconBranch);
            }
        } else {
            return baseFactory.failWithMessage(proto," Phase not recognized (1): "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }


    ProtoSeismicPhase currLegIsUpLeg(ProtoSeismicPhase proto,
                                     String prevLeg, String currLeg, String nextLeg,
                                     boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = baseFactory.calcStartBranch(proto, currLeg);
        if(PhaseSymbols.isTopsideReflectSymbol(nextLeg, 0)) {
            return baseFactory.failWithMessage(proto," p and s and k must always be up going "
                    + " and cannot come immediately before a top-side reflection."
                    + " currLeg=" + currLeg + " nextLeg=" + nextLeg);
        } else if(nextLeg.equals(up_p_leg) || nextLeg.equals(up_s_leg)) {
            return baseFactory.failWithMessage(proto, " Phase not recognized (2): "
                    + currLeg + " followed by " + nextLeg);
        } else if (isUpDiffracted(currLeg, 0)){
            String depthString = extractBoundaryId(currLeg, 1, false);
            int disconBranch = LegPuller.closestDisconBranchToDepth(tMod, depthString, depthTolerance);
            if(currBranch >= disconBranch) {
                if (!validateDisconWithinLayers(proto, disconBranch, currLeg)) {
                    return proto;
                }
                endAction = TRANSUPDIFFRACT;
                proto.addToBranch(
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            }
            proto.addFlatBranch(isPWave, TRANSUPDIFFRACT, TRANSUP, currLeg);

            // diff acts kind of like turn, so may need to add more
            if(nextLeg.equals(END_CODE)) {
                endAction = END;
                proto.addToBranch(
                        baseFactory.upgoingRecBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if (nextLeg.startsWith("^")) {
                String nextdepthString;
                nextdepthString = nextLeg.substring(1);
                endAction = REFLECT_UNDERSIDE;
                int reflectDisconBranch = LegPuller.closestDisconBranchToDepth(tMod, nextdepthString, depthTolerance);
                if (reflectDisconBranch >= disconBranch ) {
                    String reason = "Attempt to underside reflect " + currLeg
                            + " from deeper layer: " + nextLeg;
                    return baseFactory.failWithMessage(proto, reason);
                }
                if (!validateDisconWithinLayers(proto, reflectDisconBranch, currLeg)) {
                    return proto;
                }
                proto.addToBranch(
                        reflectDisconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if (nextLeg.charAt(0) == p_leg || nextLeg.charAt(0) == s_leg) {
                endAction = REFLECT_UNDERSIDE;
                proto.addToBranch(
                        0,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if (nextLeg.charAt(0) == up_p_leg || nextLeg.charAt(0) == up_s_leg) {
                // upgoing
            } else {
                return baseFactory.failWithMessage(proto, " Phase not recognized (p12): "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch);
            }
        } else if(nextLeg.startsWith("^")) {
            String depthString;
            depthString = nextLeg.substring(1);
            endAction = REFLECT_UNDERSIDE;
            int disconBranch = LegPuller.closestDisconBranchToDepth(tMod, depthString, depthTolerance);
            if(currBranch >= disconBranch) {
                if (!validateDisconWithinLayers(proto, disconBranch, currLeg)) {
                    return proto;
                }
                proto.addToBranch(
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                return baseFactory.failWithMessage(proto," Phase not recognized (2): "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch
                        + " > disconBranch=" + disconBranch);
            }
        } else if(nextLeg.equals("m")
                && currBranch >= tMod.getMohoBranch()) {
            endAction = TRANSUP;
            proto.addToBranch(
                    tMod.getMohoBranch(),
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.charAt(0) == p_leg || nextLeg.charAt(0) == s_leg
                || nextLeg.equals(END_CODE)) {
            int disconBranch;
            if (nextLeg.equals(END_CODE)) {
                disconBranch = baseFactory.upgoingRecBranch;
                if (currBranch < baseFactory.upgoingRecBranch) {
                    String reason = " (currBranch "+currBranch+" < receiverBranch() "
                            + baseFactory.upgoingRecBranch
                            + ", so there cannot be a upgoing "
                            + currLeg
                            + " phase for this sourceDepth, receiverDepth and/or path.";
                    return baseFactory.failWithMessage(proto, reason);
                }
            } else {
                disconBranch = topBranchNum;
            }
            if (nextLeg.equals(END_CODE)) {
                endAction = END;
            } else {
                endAction = REFLECT_UNDERSIDE;
            }
            proto.addToBranch(
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);

        } else if(belowLayerFactory.isLayerLeg(nextLeg)) {
            return baseFactory.failWithMessage(proto," Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", must be upgoing and so cannot hit lower layers.");
        } else if(baseFactory.isLegDepth(nextLeg)) {
            double nextLegDepth = Double.parseDouble(nextLeg);
            if (nextLegDepth >= botDepth) {
                return baseFactory.failWithMessage(proto," Phase not recognized (3): "
                        + currLeg + " followed by " + nextLeg+",  must be upgoing and so cannot hit lower depth.");
            }
            int disconBranch = LegPuller.closestDisconBranchToDepth(tMod, nextLeg, depthTolerance);
            if (!validateDisconWithinLayers(proto, disconBranch, currLeg)) {
                return proto;
            }
            endAction = TRANSUP;
            proto.addToBranch(
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(aboveLayerFactory.isLayerLeg(nextLeg)) {
            endAction = TRANSUP;
            proto.addToBranch(
                    topBranchNum,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else {
            return baseFactory.failWithMessage(proto," Phase not recognized (3 else): "+legNum+" "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }


    ProtoSeismicPhase currLegIsDownLeg(ProtoSeismicPhase proto,
                                       String prevLeg, String currLeg, String nextLeg, String nextNextLeg,
                                       boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        PhaseInteraction prevEndAction = proto.getEndAction();
        int currBranch = baseFactory.calcStartBranch(proto, currLeg);
        if(nextLeg.charAt(0) == p_leg || nextLeg.charAt(0) == s_leg
                || nextLeg.equals(END_CODE)) {
            if(prevEndAction == START || prevEndAction == TRANSDOWN || prevEndAction == REFLECT_UNDERSIDE|| prevEndAction == REFLECT_UNDERSIDE_CRITICAL) {
                // was downgoing, so must first turn in layers
                endAction = TURN;
                proto.addToBranch(
                        botBranchNum,
                        isPWave,
                        isPWave, //next same as curr for turn
                        endAction,
                        currLeg);
            }
            if (nextLeg.equals(END_CODE) && possibleToEnd(nextLeg)) {
                endAction = END;
                proto.addToBranch(baseFactory.upgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
            } else {
                endAction = REFLECT_UNDERSIDE;
                proto.addToBranch(topBranchNum, isPWave, nextIsPWave, endAction, currLeg);
            }
        } else if(isTopsideReflectSymbol(nextLeg, 0) ) {
            if (isCriticalReflectSymbol(nextLeg)) {
                endAction = REFLECT_TOPSIDE_CRITICAL;
            } else {
                endAction = REFLECT_TOPSIDE;
            }
            int disconBranch;
            if (nextLeg.equals(""+i)) {
                disconBranch = tMod.getIocbBranch();
            } else if (nextLeg.equals(""+c)) {
                disconBranch = tMod.getCmbBranch();
            } else {
                disconBranch = LegPuller.closestDisconBranchToDepth(tMod,
                        nextLeg.substring(1), depthTolerance);
            }
            if(currBranch <= disconBranch - 1) {
                if (!validateDisconWithinLayers(proto, disconBranch-1, currLeg)) {
                    return proto;
                }
                proto.addToBranch(
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                // can't topside reflect if already below, setting maxRayParam forces no arrivals
                String reason = "can't topside reflect if already below";
                return baseFactory.failWithMessage(proto, reason);
            }
        } else if( isUndersideReflectSymbol(nextLeg, 0)) {
            String depthString;
            depthString = nextLeg.substring(1);
            endAction = REFLECT_UNDERSIDE;
            int disconBranch = LegPuller.closestDisconBranchToDepth(tMod, depthString, depthTolerance);
            if (disconBranch == tMod.getNumBranches()) {
                String reason = "Attempt to underside reflect from center of earth: "+nextLeg;
                return baseFactory.failWithMessage(proto, reason);
            }
            if (!validateDisconWithinLayers(proto, disconBranch, currLeg)) {
                return proto;
            }
            if(getBelowFactory()!= null && getBelowFactory().isLayerLeg(prevLeg)) {
                proto.addToBranch(
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if(isUndersideReflectSymbol(prevLeg, 0)
                    || isLayerLeg(prevLeg)
                    || isDowngoingActionAfter(prevEndAction)
                    || baseFactory.isLegDepth(prevLeg)
                    || prevLeg.equals(PhaseSymbols.START_CODE)) {
                proto.addToBranch(
                        botBranchNum,
                        isPWave,
                        isPWave,
                        TURN,
                        currLeg);
                proto.addToBranch(
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if((isTopsideReflectSymbol(prevLeg, 0)
                    && disconBranch < LegPuller.closestDisconBranchToDepth(tMod, prevLeg.substring(1), depthTolerance))
                    || (prevLeg.equals("m") && disconBranch < tMod.getMohoBranch())
                    || (prevLeg.equals("c") && disconBranch < tMod.getCmbBranch())) {
                if (disconBranch == tMod.getNumBranches()) {
                    String reason = "Attempt to reflect from center of earth: "+nextLeg;
                    return baseFactory.failWithMessage(proto, reason);
                }
                proto.addToBranch(
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                return baseFactory.failWithMessage(proto," Phase not recognized (5): "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch
                        + " > disconBranch=" + disconBranch+" , prev="+prevLeg);
            }
        } else if(nextLeg.equals("c")) {
            if (tMod.getCmbBranch() == tMod.getNumBranches()) {
                String reason = "Attempt to reflect from center of earth: "+nextLeg;
                return baseFactory.failWithMessage(proto, reason);
            }
            endAction = REFLECT_TOPSIDE;
            proto.addToBranch(
                    tMod.getCmbBranch() - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(getBelowFactory()!= null && getBelowFactory().isLayerLeg(nextLeg)
                && getBelowFactory().isLayerLeg(prevLeg)) {
            return baseFactory.failWithMessage(proto," Phase not recognized (5.5): "
                    + currLeg + " followed by " + nextLeg
                    + " and preceeded by "+prevLeg
                    + " when currBranch=" + currBranch
            );
        } else if(getBelowFactory()!= null && getBelowFactory().isLayerLeg(nextLeg)) {
            endAction = TRANSDOWN;
            proto.addToBranch(
                    botBranchNum,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(getAboveFactory()!= null &&   getAboveFactory().isLayerLeg(nextLeg)) {
            if ((proto.getEndAction()== START && PhaseSymbols.isDowngoingSymbol(currLeg) ) || isDowngoingActionAfter(proto.getEndAction())) {
                endAction = TURN;
                proto.addToBranch(
                        botBranchNum,
                        isPWave,
                        isPWave,
                        endAction,
                        currLeg);
            }
            endAction = TRANSUP;
            if (isDiffracted(nextLeg)) {
                endAction = TRANSUPDIFFRACT;
            }
            proto.addToBranch(
                    topBranchNum,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if( LegPuller.isBoundary(nextLeg) && ( nextLeg.equals("m")
                || (topDepth < LegPuller.legAsDepthBoundary(tMod, nextLeg) && LegPuller.legAsDepthBoundary(tMod, nextLeg) < botDepth))) {
            // treat the moho in the same wasy as 410 type discontinuities

            int disconBranch = LegPuller.closestDisconBranchToDepth(tMod, nextLeg, depthTolerance);
            if (!validateDisconWithinLayers(proto, disconBranch, currLeg)) {
                return proto;
            }
            if (baseFactory.DEBUG) {
                Alert.debug("DisconBranch=" + disconBranch + " for " + nextLeg);
                Alert.debug("  "+tMod.getTauBranch(disconBranch, isPWave).getTopDepth());
            }
            if (prevEndAction == TURN || prevEndAction == REFLECT_TOPSIDE
                    || prevEndAction == REFLECT_TOPSIDE_CRITICAL || prevEndAction == TRANSUP) {
                // upgoing section
                if (disconBranch > currBranch) {
                    // check for discontinuity below the current branch
                    // when the ray should be upgoing
                    return baseFactory.failWithMessage(proto," Phase not recognized (6): "
                            + currLeg
                            + " followed by "
                            + nextLeg
                            + " when currBranch="
                            + currBranch
                            + " > disconBranch=" + disconBranch);
                }
                endAction = TRANSUP;
                proto.addToBranch(
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                // downgoing section, must look at the leg after the
                // next
                // leg to determine whether to convert on the downgoing
                // or
                // upgoing part of the path
                //String nextNextLeg = (String) legs.get(legNum + 2);
                if (nextNextLeg.charAt(0) == up_p_leg || nextNextLeg.charAt(0) == up_s_leg) {
                    // convert on upgoing section
                    endAction = TURN;
                    proto.addToBranch(
                            botBranchNum,
                            isPWave,
                            isPWave,
                            endAction,
                            currLeg);
                    endAction = TRANSUP;
                    proto.addToBranch(
                            disconBranch,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                } else if (nextNextLeg.equals(""+p_leg)
                        || nextNextLeg.equals(""+s_leg)) {
                    if (disconBranch > currBranch) {
                        // discon is below current loc
                        endAction = TRANSDOWN;
                        proto.addToBranch(
                                disconBranch - 1,
                                isPWave,
                                nextIsPWave,
                                endAction,
                                currLeg);
                    } else {
                        // discon is above current loc, but we have a
                        // downgoing ray, so this is an illegal ray for
                        // this source depth
                        String reason = "Cannot phase convert on the "
                                + "downgoing side if the discontinuity is above "
                                + "the phase leg starting point, "
                                + currLeg+ " "+ nextLeg+ " "+ nextNextLeg
                                + ", so this phase, "+ baseFactory.getName()
                                + " is illegal for this sourceDepth.";
                        return baseFactory.failWithMessage(proto, reason);
                    }
                } else {
                    return baseFactory.failWithMessage(proto," Phase not recognized (7): "
                            + currLeg
                            + " followed by "
                            + nextLeg
                            + " followed by " + nextNextLeg);
                }
            }
        } else if (nextLeg.endsWith(HEAD_CODE) && nextLeg.length() > 1) {
            String numString = extractBoundaryId(nextLeg, 0, false);
            try {
                int disconBranch = LegPuller.closestDisconBranchToDepth(tMod, numString, depthTolerance);
                if (!validateDisconWithinLayers(proto, disconBranch-1, currLeg)) {
                    return proto;
                }
                if ( ! tMod.isHeadWaveBranch(disconBranch, isPWave)) {
                    return baseFactory.failWithMessage(proto,"Unable to head wave, "+ currLeg+", "
                            + disconBranch +", "+numString+ " is not positive velocity discontinuity.");
                }
                endAction = HEAD;
                proto.addToBranch(
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } catch (NumberFormatException e) {
                return baseFactory.failWithMessage(proto," Phase not recognized (7): "
                        + currLeg
                        + " followed by "
                        + nextLeg + " expected number but was `" + numString + "`");
            }

        } else if (isDiffracted(nextLeg) || isDiffractedDown(nextLeg)) {
            // diff but not Pdiff or Sdiff
            String numString = extractBoundaryId(nextLeg, 0, false);
            try {
                int disconBranch = LegPuller.closestDisconBranchToDepth(tMod, numString, depthTolerance);
                if (!validateDisconWithinLayers(proto, disconBranch, nextLeg)) {
                    return proto;
                }

                if(prevEndAction == START || prevEndAction == TRANSDOWN || prevEndAction == REFLECT_UNDERSIDE|| prevEndAction == REFLECT_UNDERSIDE_CRITICAL) {
                    // was downgoing, so must first turn in layer
                    endAction = TURN;
                    proto.addToBranch(
                            botBranchNum,
                            isPWave,
                            isPWave,
                            endAction,
                            currLeg);
                }
                endAction = REFLECT_UNDERSIDE;
                proto.addToBranch(topBranchNum, isPWave, nextIsPWave, endAction, currLeg);

            } catch(NumberFormatException e) {
                return baseFactory.failWithMessage(proto," Phase not recognized (7): "
                        + currLeg
                        + " followed by "
                        + nextLeg+" expected number but was `"+numString+"`");
            }
        } else {
            return baseFactory.failWithMessage(proto," Phase not recognized (8): "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }

    ProtoSeismicPhase currLegIsDiffracted(ProtoSeismicPhase proto,
                                          String prevLeg, String currLeg, String nextLeg,
                                          boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        PhaseInteraction prevEndAction = proto.getEndAction();
        int prevEndBranch = proto.isEmpty() ? -1 : proto.endSegment().endBranch;
        int currBranch = baseFactory.calcStartBranch(proto, currLeg);
        int depthIdx = 0;
        if (currLeg.startsWith(""+p_leg) || currLeg.startsWith(""+s_leg)) {
            depthIdx = 1;
        }
        String numString="";
        int disconBranch;
        if ((isDiffracted(currLeg) && currLeg.length()==DIFF.length()+1)
                || (isDiffractedDown(currLeg) && currLeg.length()==DIFFDOWN.length()+1)) {
            disconBranch = botBranchNum+1; // diff at bottom
        } else {
            numString = extractBoundaryId(currLeg, depthIdx, false);
            disconBranch = LegPuller.closestDisconBranchToDepth(tMod, numString, depthTolerance);
        }
        if (!validateDisconWithinLayers(proto, disconBranch-1, currLeg)) {
            return proto;
        }
        SeismicPhaseSegment prevSegment = !proto.segmentList.isEmpty() ? proto.endSegment() : null;

        endAction = DIFFRACT;
        if (prevEndBranch < disconBranch - 1 || prevEndAction == START ||
                (prevEndBranch == disconBranch-1 && prevSegment != null && prevSegment.endsAtTop())
        ) {
            proto.addToBranch(
                    disconBranch - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if (currBranch == disconBranch - 1
                && (prevSegment.endAction == DIFFRACT || prevSegment.endAction == TRANSUPDIFFRACT
                || prevSegment.endAction == TRANSUP)) {
            // already at correct depth ?
        } else {
            // we are below at the right branch to diffract???
            return baseFactory.failWithMessage(proto,"Unable to diffract, below the right branch to diffract " + currBranch +" of "+proto.phaseName
                    +" "+ (disconBranch - 1) + " " + baseFactory.endActionString(prevEndAction) + " " + prevSegment+" "+prevSegment.endsAtTop());
        }

        if ( ! tMod.isDiffractionBranch(disconBranch, isPWave)) {
            return baseFactory.failWithMessage(proto,"Unable to diffract, not diffraction depth " + currLeg + ", "+disconBranch+" at "+
                    + tMod.getTauBranch(disconBranch, isPWave).getTopDepth()+" km, "+numString+" is not velocity discontinuity.");
        }
        // is possible to diffract downward? maybe if low velocity zone??
        if (currLeg.endsWith(DIFFDOWN)
                || (currBranch == botBranchNum
                && (nextLeg.charAt(0) == getBelowPLegSymbol()|| nextLeg.charAt(0) == getBelowSLegSymbol() ))) {
            // down after diffract
            proto.addFlatBranch(isPWave, endAction, TRANSDOWN, currLeg);
            endAction = TRANSDOWN;
            currBranch++;
            // next must be some kind of downgoing leg, so let next step handle
        } else {
            // normal case
            proto.addFlatBranch(isPWave, endAction, DIFFRACTTURN, currLeg);

            // diff acts kind of like turn, so may need to add more
            if(nextLeg.equals(END_CODE)) {
                endAction = END;
                proto.addToBranch(
                        baseFactory.upgoingRecBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if (nextLeg.startsWith("^")) {
                String depthString;
                depthString = nextLeg.substring(1);
                endAction = REFLECT_UNDERSIDE;
                int reflectDisconBranch = LegPuller.closestDisconBranchToDepth(tMod, depthString, depthTolerance);
                if (reflectDisconBranch >= disconBranch ) {
                    String reason = "Attempt to underside reflect " + currLeg
                            + " from deeper layer: " + nextLeg;
                    return baseFactory.failWithMessage(proto, reason);
                }
                if (!validateDisconWithinLayers(proto, reflectDisconBranch, currLeg)) {
                    return proto;
                }
                proto.addToBranch(
                        reflectDisconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if (nextLeg.startsWith(""+p_leg) || nextLeg.startsWith(""+s_leg)) {
                endAction = REFLECT_UNDERSIDE;
                proto.addToBranch(
                        topBranchNum,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if (nextLeg.charAt(0) == getAbovePLegSymbol() || nextLeg.charAt(0) == getAboveSLegSymbol()
                    || nextLeg.charAt(0) == getAboveUpPLegSymbol() || nextLeg.charAt(0) == getAboveUpSLegSymbol()) {
                endAction = TRANSUP;
                proto.addToBranch(
                        topBranchNum,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if (nextLeg.charAt(0) == up_p_leg || nextLeg.charAt(0) == up_s_leg) {
                // upgoing
            } else {
                return baseFactory.failWithMessage(proto, " Phase not recognized (12): "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch);
            }
        }
        return proto;
    }

    ProtoSeismicPhase currLegIs_Pg_Sg(ProtoSeismicPhase proto,
                                      String prevLeg, String currLeg, String nextLeg,
                                      boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = baseFactory.calcStartBranch(proto, currLeg);
        if(currLeg.equals(p_leg+"g") || currLeg.equals(s_leg+"g")) {
            if(currBranch >= tMod.getMohoBranch()) {
                /*
                 * Pg, Pn, Sg and Sn must be above the moho and so is
                 * not valid for rays coming upwards from below,
                 * possibly due to the source depth. Setting maxRayParam =
                 * -1 effectively disallows this phase.
                 */
                String reason = "(currBranch >= tMod.getMohoBranch() "
                        + currBranch
                        + " "
                        + tMod.getMohoBranch()
                        + " so there cannot be a "
                        + currLeg
                        + " phase for this sourceDepth and/or path.";
                return baseFactory.failWithMessage(proto, reason);
            }
            endAction = TURN;
            proto.addToBranch(
                    tMod.getMohoBranch() - 1,
                    isPWave,
                    isPWave,
                    endAction,
                    currLeg);
            if(nextLeg.equals(END_CODE)) {
                endAction = END;
                proto.addToBranch(baseFactory.upgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
            } else if(nextLeg.startsWith("P") || nextLeg.startsWith("S")) {
                endAction = REFLECT_UNDERSIDE;
                proto.addToBranch(0, isPWave, nextIsPWave, endAction, currLeg);
            } else if(nextLeg.startsWith("^")) {
                String depthString;
                depthString = nextLeg.substring(1);
                endAction = REFLECT_UNDERSIDE;
                int disconBranch = LegPuller.closestDisconBranchToDepth(tMod, depthString, depthTolerance);
                if (disconBranch >= tMod.getMohoBranch()) {
                    String reason = "Attempt to underside reflect "+currLeg+" from deeper layer: "+nextLeg;
                    return baseFactory.failWithMessage(proto, reason);
                }
                if (!validateDisconWithinLayers(proto, disconBranch, currLeg)) {
                    return proto;
                }
                proto.addToBranch(
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);

            } else {
                return baseFactory.failWithMessage(proto, " Phase not recognized (12): "
                        + currLeg + " followed by " + nextLeg);
            }

        } else {
            return baseFactory.failWithMessage(proto, " Phase not recognized for P,S: "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }

    ProtoSeismicPhase currLegIsHead(ProtoSeismicPhase proto,
                                    String prevLeg, String currLeg, String nextLeg,
                                    boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        String numString;
        int disconBranch;

        int currBranch = baseFactory.calcStartBranch(proto, currLeg);
        if (currLeg.endsWith(HEAD_CODE) && currLeg.length() >= 2) {
            if (currLeg.length() == 2 && ( currLeg.charAt(0)==P || currLeg.charAt(0)==S)) {
                // special case, Pn or Sn, use moho
                numString = "m";
                disconBranch = tMod.getMohoBranch();
            } else {
                int depthIdx = 0;
                if (currLeg.startsWith("P") || currLeg.startsWith("S") || currLeg.startsWith("K")
                        || currLeg.startsWith("I") || currLeg.startsWith("J")) {
                    depthIdx = 1;
                }
                numString = extractBoundaryId(currLeg, depthIdx, false);
                disconBranch = LegPuller.closestDisconBranchToDepth(tMod, numString, depthTolerance);
            }
            if (!validateDisconWithinLayers(proto, disconBranch-1, currLeg)) {
                return proto;
            }
            if ( ! tMod.isHeadWaveBranch(disconBranch, isPWave)) {
                return baseFactory.failWithMessage(proto,"Unable to head wave, "+ currLeg+", "
                        + disconBranch +", "+numString+ " is not positive velocity discontinuity.");
            }
            endAction = HEAD;
            proto.addToBranch(
                    disconBranch-1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
            if (nextLeg.startsWith(""+getBelowPLegSymbol()) || nextLeg.startsWith(""+getBelowSLegSymbol()) ) {
                // down into  below layers, like core
                proto.addFlatBranch(isPWave, endAction, TRANSDOWN, currLeg);
            } else {
                // normal case
                proto.addFlatBranch(isPWave, endAction, TRANSUP, currLeg);
            }
            currBranch=disconBranch;
            if(nextLeg.equals(END_CODE)) {
                endAction = END;
                proto.addToBranch(
                        baseFactory.upgoingRecBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
                // should handle other nextLeg besides END ???
            } else if (topBranchNum==0 && (nextLeg.charAt(0) == p_leg || nextLeg.charAt(0)==s_leg)) {
                // crust mantle surface reflect
                endAction = REFLECT_UNDERSIDE;
                proto.addToBranch(
                        0,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);

            } else {
                return baseFactory.failWithMessage(proto, " Phase not recognized for non-standard diffraction: "
                        + currLeg + " followed by " + nextLeg);
            }
        } else {
            return baseFactory.failWithMessage(proto,  " Phase not recognized for non-standard diffraction: "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }


    public SeismicPhaseLayerFactory getAboveFactory() {
        SeismicPhaseLayerFactory b = aboveLayerFactory;
        while (b != null && b instanceof SeismicPhaseLayerFactoryAllFail) {
            b = b.aboveLayerFactory;
        }
        return b;
    }

    public SeismicPhaseLayerFactory getBelowFactory() {
        SeismicPhaseLayerFactory b = belowLayerFactory;
        while (b != null && b instanceof SeismicPhaseLayerFactoryAllFail) {
            b = b.belowLayerFactory;
        }
        return b;
    }

    public Character getPLegSymbol() {
        return p_leg;
    }

    public Character getBelowPLegSymbol() {
        SeismicPhaseLayerFactory b = getBelowFactory();
        if (b == null) {
            return ' ';
        }
        return b.getPLegSymbol();
    }

    public Character getAbovePLegSymbol() {
        SeismicPhaseLayerFactory b = getAboveFactory();
        if (b == null) {
            return ' ';
        }
        return b.getPLegSymbol();
    }

    public Character getAboveUpPLegSymbol() {
        SeismicPhaseLayerFactory b = getAboveFactory();
        if (b == null) {
            return ' ';
        }
        return b.up_p_leg;
    }

    public Character getSLegSymbol() {
        return s_leg;
    }

    public Character getBelowSLegSymbol() {
        SeismicPhaseLayerFactory b = getBelowFactory();
        if (b == null) {
            return ' ';
        }
        return b.getSLegSymbol();
    }

    public Character getAboveSLegSymbol() {
        SeismicPhaseLayerFactory b = getAboveFactory();
        if (b == null) {
            return ' ';
        }
        return b.getSLegSymbol();
    }

    public Character getAboveUpSLegSymbol() {
        SeismicPhaseLayerFactory b = getAboveFactory();
        if (b == null) {
            return ' ';
        }
        return b.up_s_leg;
    }

    public boolean isLayerLeg(String leg) {
        char c = leg.charAt(0);
        return c == p_leg || c == s_leg || c == up_p_leg || c == up_s_leg;
    }

    public boolean possibleToEnd(String endLeg) {
        if (endLeg.equals(END_CODE) && baseFactory.upgoingRecBranch >= topBranchNum) {
            return true;
        }
        if (endLeg.equals(END_DOWN) && baseFactory.downgoingRecBranch <= botBranchNum) {
            return true;
        }
        return false;
    }

    public boolean validateDisconWithinLayers(ProtoSeismicPhase proto, int disconNum, String currLeg) throws TauModelException {
        if (disconNum <= -1) {
            baseFactory.failWithMessage(proto, "No boundary in model within "+depthTolerance+" km of "+currLeg);
            return false;
        }
        if (topBranchNum <= disconNum && disconNum <= botBranchNum) {
            return true;
        }
        baseFactory.failWithMessage(proto, "Illegal phase, cannot reach discontinuity "+disconNum
                +" at depth "+tMod.getTauBranch(disconNum,true).getTopDepth()+" for phase symbol "+currLeg);
        return false;
    }

    String layerName;
    public static final char EMPTY = ' ';
    char p_leg = EMPTY;
    char up_p_leg = EMPTY;
    char s_leg = EMPTY;
    char up_s_leg = EMPTY;
    TauModel tMod;
    int topBranchNum;
    int botBranchNum;
    double topDepth;
    double botDepth;
    double depthTolerance = 10;
    SeismicPhaseFactory baseFactory;
    SeismicPhaseLayerFactory aboveLayerFactory;
    SeismicPhaseLayerFactory belowLayerFactory;
}
