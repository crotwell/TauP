package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static edu.sc.seis.TauP.PhaseInteraction.*;
import static edu.sc.seis.TauP.PhaseInteraction.REFLECT_TOPSIDE_CRITICAL;
import static edu.sc.seis.TauP.PhaseSymbols.*;
import static edu.sc.seis.TauP.SimpleSeismicPhase.PWAVE;
import static edu.sc.seis.TauP.SimpleSeismicPhase.SWAVE;
import static edu.sc.seis.TauP.ProtoSeismicPhase.failNewPhase;

public class SeismicPhaseFactory {

    boolean DEBUG;
    String name;
    double receiverDepth;
    TauModel tMod;

    // temp vars used in calculation of phase
    int upgoingRecBranch;
    int downgoingRecBranch;
    PhaseInteraction prevEndAction = START;

    /**
     * The maximum degrees that a Pn or Sn can refract along the moho. Note this
     * is not the total distance, only the segment along the moho. The default
     * is 20 degrees.
     */
    protected static double maxRefraction = 20;

    /**
     * The maximum degrees that a Pdiff or Sdiff can diffract along the CMB.
     * Note this is not the total distance, only the segment along the CMB. The
     * default is 60 degrees.
     */
    protected static double maxDiffraction = 60;

    public static double getMaxRefraction() {
        return maxRefraction;
    }

    public static void setMaxRefraction(double max) {
        maxRefraction = max;
    }

    public static double getMaxDiffraction() {
        return maxDiffraction;
    }

    public static void setMaxDiffraction(double max) {
        maxDiffraction = max;
    }

    static double maxKmpsLaps = 1;

    public static double getMaxKmpsLaps() {
        return maxKmpsLaps;
    }

    public static void setMaxKmpsLaps(double max) {
        maxKmpsLaps = max;
    }


    SeismicPhaseFactory(String name, TauModel tMod, double sourceDepth, double receiverDepth, boolean debug) throws TauModelException {
        this.DEBUG = debug;
        if (name == null || name.isEmpty()) {
            throw new TauModelException("Phase name cannot be empty or null: '" + name+"'");
        }
        // make sure we have layer boundary at source and receiver
        // this does nothing if already split
        TauModel sourceDepthTMod;
        if (sourceDepth == tMod.getSourceDepth()) {
            sourceDepthTMod = tMod;
        } else {
            sourceDepthTMod = tMod.depthCorrect(sourceDepth);
        }
        this.tMod = sourceDepthTMod.splitBranch(receiverDepth);
        this.name = name;
        this.receiverDepth = receiverDepth;

        // where we end up, depending on if we end going down or up
        this.upgoingRecBranch = this.tMod.findBranch(receiverDepth);
        this.downgoingRecBranch = upgoingRecBranch - 1; // one branch shallower
    }
    public static SimpleSeismicPhase createPhase(String name, TauModel tMod) throws TauModelException {
        return createPhase(name, tMod, tMod.getSourceDepth());
    }
    public static SimpleSeismicPhase createPhase(String name, TauModel tMod, double sourceDepth) throws TauModelException {
        return createPhase(name, tMod, sourceDepth, 0.0);
    }
    public static SimpleSeismicPhase createPhase(String name, TauModel tMod, double sourceDepth, double receiverDepth) throws TauModelException {
        return createPhase(name, tMod, sourceDepth, receiverDepth, TauPConfig.DEBUG);
    }
    public static SimpleSeismicPhase createPhase(String name, TauModel tMod, double sourceDepth, double receiverDepth, boolean debug) throws TauModelException {
        SeismicPhaseFactory factory = new SeismicPhaseFactory(name, tMod, sourceDepth, receiverDepth, debug);
        return factory.internalCreatePhase();
    }

    public static void configure(Properties toolProps) {
        if (toolProps.containsKey("taup.maxRefraction")) {
            SeismicPhaseFactory.setMaxRefraction(Double.parseDouble(toolProps.getProperty("taup.maxRefraction")));
        }
        if (toolProps.containsKey("taup.maxDiffraction")) {
            SeismicPhaseFactory.setMaxDiffraction(Double.parseDouble(toolProps.getProperty("taup.maxDiffraction")));
        }
        if (toolProps.containsKey("taup.maxKmpsLaps")) {
            SeismicPhaseFactory.setMaxKmpsLaps(Double.parseDouble(toolProps.getProperty("taup.maxKmpsLaps")));
        }
    }

    public static List<SeismicPhase> createSeismicPhases(String name,
                                                         TauModel tMod,
                                                         double sourceDepth,
                                                         double receiverDepth,
                                                         Scatterer scat,
                                                         boolean debug) throws TauModelException {
        List<SeismicPhase> phaseList = new ArrayList<>();
        if (name.contains(""+ SCATTER_CODE)
                || name.contains(""+ BACKSCATTER_CODE)) {
            String[] in_scat = name.split("(["+ SCATTER_CODE+ BACKSCATTER_CODE+"])");
            if (in_scat.length > 2) {
                FailedSeismicPhase fail = FailedSeismicPhase.failForReason(name, tMod, receiverDepth,
                        "Scatter phase cannot have multiple scatter symbols, oO, in "+name+", repeated scattering not supported");
                phaseList.add(fail);
                return phaseList;
            }
            if (scat == null ) {
                FailedSeismicPhase fail = FailedSeismicPhase.failForReason(name, tMod, receiverDepth,
                        "Attempt to use scatter phase but scatter is missing: "+name);
                phaseList.add(fail);
                return phaseList;
            }
            String prescatterPhaseName = in_scat[0];
            String postscatterPhaseName = in_scat[1];
            boolean isBackscatter = name.contains("" + BACKSCATTER_CODE);
            TauModel tModDepthCorrected = tMod;
            if (tModDepthCorrected.getSourceDepth()!= sourceDepth) {
                tModDepthCorrected= tMod.depthCorrect(sourceDepth);
            }
            tModDepthCorrected = tModDepthCorrected.splitBranch(receiverDepth);
            TauModel scatTMod = tModDepthCorrected.depthRecorrect(scat.depth);
            SimpleSeismicPhase inPhase = SeismicPhaseFactory.createPhase(prescatterPhaseName,
                    tModDepthCorrected, sourceDepth, scat.depth, debug);

            SeismicPhaseSegment lastSegment = inPhase.getPhaseSegments().get(inPhase.getPhaseSegments().size() - 1);
            PhaseInteraction endAction = lastSegment.endAction;
            if (endAction == END_DOWN) {
                if (isBackscatter) {
                    lastSegment.endAction = BACKSCATTER_DOWN;
                } else {
                    lastSegment.endAction = SCATTER_DOWN;
                }
            } else if (endAction == END) {
                if (isBackscatter) {
                    lastSegment.endAction = BACKSCATTER;
                } else {
                    lastSegment.endAction = SCATTER;
                }
            }
            List<Arrival> inArrivals = scat.dist.calculate(inPhase);
            if (inArrivals.isEmpty()) {
                StringBuffer failReason = new StringBuffer("No inbound arrivals to the scatterer for "+name
                        +" at "+scat.depth+" km depth and "+scat.dist.getDegrees(tMod.getRadiusOfEarth())
                        +" deg. Distance range for scatterer at this depth is"
                        +" "+inPhase.getMinDistanceDeg()+" "+inPhase.getMaxDistanceDeg()+" deg");

                FailedSeismicPhase fail = FailedSeismicPhase.failForReason(name, tMod, receiverDepth,
                                failReason.toString());
                phaseList.add(fail);
                return phaseList;
            }
            SimpleSeismicPhase scatPhase = SeismicPhaseFactory.createPhase(postscatterPhaseName,
                    scatTMod, scat.depth, receiverDepth, debug);
            for (Arrival inArr : inArrivals) {
                Arrival flipInArr = inArr;
                if (inArr.getDistDeg() == -1 * scat.dist.getDegrees(scatPhase.getTauModel().getRadiusOfEarth())) {
                    flipInArr = inArr.negateDistance();
                }
                ScatteredSeismicPhase seismicPhase = new ScatteredSeismicPhase(
                        flipInArr,
                        scatPhase,
                        scat.depth,
                        scat.dist.getDegrees(tMod.getRadiusOfEarth()),
                        isBackscatter);
                phaseList.add(seismicPhase);
            }
        } else {

            TauModel tModDepthCorrected = tMod;
            if (tModDepthCorrected.getSourceDepth()!= sourceDepth) {
                tModDepthCorrected= tMod.depthCorrect(sourceDepth);
            }
            tModDepthCorrected = tModDepthCorrected.splitBranch(receiverDepth);
            SimpleSeismicPhase seismicPhase = SeismicPhaseFactory.createPhase(name,
                    tModDepthCorrected, sourceDepth, receiverDepth, debug);
            phaseList.add(seismicPhase);
        }
        return phaseList;
    }

    public static List<SeismicPhase> calculateSeismicPhases(TauModel tMod,
                                                            List<PhaseName> phaseNameList,
                                                            double sourceDepth,
                                                            List<Double> receiverDepths,
                                                            Scatterer scatterer) throws TauModelException {
        List<SeismicPhase> newPhases = new ArrayList<>();
        TauModel tModDepth = tMod.depthCorrect(sourceDepth);
        if (receiverDepths.isEmpty()) { throw new RuntimeException("receiverDepths should not be empty");}
        for (Double receiverDepth : receiverDepths) {
            TauModel tModRecDepth = tModDepth.splitBranch(receiverDepth);
            for (PhaseName phaseName : phaseNameList) {
                String tempPhaseName = phaseName.getName();
                try {
                    List<SeismicPhase> calcPhaseList = createSeismicPhases(
                            phaseName.getName(),
                            tModRecDepth,
                            sourceDepth,
                            receiverDepth,
                            scatterer,
                            TauPConfig.DEBUG);
                    newPhases.addAll(calcPhaseList);
                    for (SeismicPhase seismicPhase : newPhases) {
                        if (TauPConfig.VERBOSE) {
                            Alert.info(seismicPhase.toString());
                        }
                    }
                } catch (ScatterArrivalFailException e) {
                    Alert.warning(e.getMessage() + ", skipping this phase");
                    if (TauPConfig.VERBOSE || TauPConfig.DEBUG) {
                        e.printStackTrace();
                    }
                } catch (TauModelException e) {
                    Alert.warning("Error with phase=" + tempPhaseName,
                            e.getMessage() + "\nSkipping this phase");
                    if (TauPConfig.VERBOSE || TauPConfig.DEBUG) {
                        e.printStackTrace();
                    }
                } finally {
                    if (TauPConfig.VERBOSE) {
                        Alert.info("-----------------");
                    }
                }
            }
        }
        return newPhases;
    }

    SimpleSeismicPhase internalCreatePhase() throws TauModelException {
        ArrayList<String> legs = LegPuller.legPuller(name);

        ProtoSeismicPhase proto = parseName(tMod, legs);
        proto.phaseName = name;
        if ( ! proto.isFail) {
            proto.validateSegList();
        }

        if (proto.isSuccessful()) {
            return sumBranches(tMod, proto);
        } else {
            return new FailedSeismicPhase(proto);
        }
    }

    public String getName() {
        return name;
    }


    public static Boolean legIsPWave(String currLeg) {
        if (PhaseSymbols.isCompressionalWaveSymbol(currLeg, 0)) {
                return PWAVE;
        } else if(PhaseSymbols.isTransverseWaveSymbol(currLeg, 0)) {
            return SWAVE;
        } else {
            // otherwise, curr leg is same as prev, like for reflections
            return null;
        }
    }

    /*
     * Figures out which legs are P and S. Non-travel legs, like c or v410 are the same wave type as the
     * next leg.
     */
    public static boolean[] legsArePWave(ArrayList<String> legs) {
        boolean[] isPWaveForLegs = new boolean[legs.size()];
        int legNum = 0;
        boolean prevWaveType = true;
        for (String currLeg : legs) {
            Boolean currWaveType;
            if (currLeg.equals(END_CODE)) {
                // end just use prev
                currWaveType = prevWaveType;
            } else {
                currWaveType = legIsPWave(currLeg);
                if (currWaveType == null) {
                    if (legNum == legs.size()-2) {
                    // null just before end??
                    currWaveType = prevWaveType;
                    } else {
                        // null means non travel leg like reflection, use next leg
                        if (legNum < legs.size() - 1) {
                            currWaveType = legIsPWave(legs.get(legNum + 1));
                            if (currWaveType == null) {
                                throw new RuntimeException("next wavetype is null: "+currLeg+" in " + legs + " " + legNum + " of " + legs.size());
                            }
                        } else {
                            throw new RuntimeException("SHould never happen: "+currLeg+" of "+legs+" "+legNum);
                        }
                    }
                }
            }
            isPWaveForLegs[legNum] =currWaveType;
            legNum++;
            prevWaveType = currWaveType;
        }
        return isPWaveForLegs;
    }

    /**
     * Constructs a branch sequence from the given phase name and tau model.
     */
    protected ProtoSeismicPhase parseName(TauModel tMod, ArrayList<String> legs) throws TauModelException {
        String prevLeg;
        String currLeg = legs.get(0);
        String nextLeg = currLeg;
        boolean isPWave = true;
        boolean prevIsPWave;

        /*
         * Deal with surface wave velocities first, since they are a special
         * case.
         */
        if(legs.size() == 2 && currLeg.endsWith(KMPS_CODE)) {
            try {
                Double.parseDouble(currLeg.substring(0, name.length() - 4));
            } catch (NumberFormatException e) {
                return ProtoSeismicPhase.failNewPhase(tMod, false, true,
                        receiverDepth, getName()," Illegal surface wave velocity "+name.substring(0, name.length() - 4));
            }
            // KMPS fake with a head wave
            ProtoSeismicPhase proto = ProtoSeismicPhase.startEmpty(name, tMod, receiverDepth);
            proto.addFlatBranch(false, KMPS, END, currLeg);
            return proto;
        }
        /* Make a check for J legs if the model doesn not allow J */
        if((name.indexOf(J) != -1 || name.indexOf(j) != -1)
                && !tMod.getSlownessModel().isAllowInnerCoreS()) {
            return ProtoSeismicPhase.failNewPhase(tMod, false, true,
                    receiverDepth, getName()," 'J' phases were not created for this model: "
                    + tMod.getModelName());
        }
        /* check for outer core if K */
        for (String leg : legs) {
            if (tMod.getCmbBranch() == tMod.getNumBranches() && (isOuterCoreLeg(leg)
                    || (leg.length()==1 && leg.charAt(0) == c))) {
                String reason = "Cannot have K leg in model with no outer core";
                return failNewPhase(tMod, isCompressionalWaveSymbol(legs.get(0)), isDowngoingSymbol(legs.get(0)), receiverDepth, name, reason);
            }
            if (tMod.getIocbBranch() == tMod.getNumBranches() && (isInnerCoreLeg(leg) ||
                    (leg.length()==1 && leg.charAt(0) == i))) {
                String reason = "Cannot have I,J,y,j,i leg in model with no inner core";
                return failNewPhase(tMod, isCompressionalWaveSymbol(legs.get(0)), isDowngoingSymbol(legs.get(0)),
                        receiverDepth, name, reason);
            }
        }
        /* set currWave to be the wave type for this leg, 'P' or 'S'. */
        if(isCompressionalWaveSymbol(currLeg)) {
            isPWave = PWAVE;
            prevIsPWave = isPWave;
        } else if(isTransverseWaveSymbol(currLeg)) {
            isPWave = SWAVE;
            prevIsPWave = isPWave;
        } else {
            return ProtoSeismicPhase.failNewPhase(tMod, false, true,
                    receiverDepth, getName(), getName()+" Unknown starting phase: "+currLeg);
        }
        /*
         * First, decide whether the ray is up going or downgoing from the
         * source. If it is up going then the first branch number would be
         * tMod.getSourceBranch()-1 and downgoing would be
         * tMod.getSourceBranch().
         */
        if(isTransverseWaveSymbol(currLeg)) {
            // Exclude S sources in fluids
            double sdep = tMod.getSourceDepth();
            if(tMod.getSlownessModel().depthInFluid(sdep, new DepthRange())) {
                String reason = "Cannot have S wave with starting depth in fluid layer " + currLeg;
                return failNewPhase(tMod, isCompressionalWaveSymbol(legs.get(0)), isDowngoingSymbol(legs.get(0)),
                        receiverDepth, name, reason);
            }
        }
        /*
         * Set maxRayParam to be a horizontal ray leaving the source and set
         * minRayParam to be a vertical (p=0) ray.
         */
        if(isDowngoingSymbol(currLeg)) {
            // Downgoing from source
            if ((currLeg.startsWith("P") || currLeg.startsWith("S")) && tMod.getSourceDepth() > tMod.getCmbDepth()  ) {
                // not possible
                String reason = "Source must be in crust/mantle for "+ currLeg;
                return failNewPhase(tMod, isCompressionalWaveSymbol(legs.get(0)), isDowngoingSymbol(legs.get(0)),
                        receiverDepth, name, reason);
            } else if ((currLeg.startsWith("K")) && (tMod.getSourceDepth() < tMod.getCmbDepth() || tMod.getSourceDepth() > tMod.getIocbDepth() )) {
                // not possible
                String reason = "Source must be in outer core for "+ currLeg;
                return failNewPhase(tMod, isCompressionalWaveSymbol(legs.get(0)), isDowngoingSymbol(legs.get(0)),
                        receiverDepth, name, reason);
            } else if ((currLeg.startsWith("I") || currLeg.startsWith("J")) && (tMod.getSourceDepth() < tMod.getIocbDepth() )) {
                // not possible
                String reason = "Source must be in inner core for "+currLeg;
                return failNewPhase(tMod, isCompressionalWaveSymbol(legs.get(0)), isDowngoingSymbol(legs.get(0)),
                        receiverDepth, name, reason);
            }
        } else if(isUpgoingSymbol(currLeg)) {
            // Up going from source
            if (isCrustMantleLeg(currLeg) && tMod.getSourceDepth() > tMod.getCmbDepth()  ) {
                // not possible as in core
                String reason = "Source must be in crust/mantle for "+ currLeg;
                return failNewPhase(tMod, isCompressionalWaveSymbol(legs.get(0)), isDowngoingSymbol(legs.get(0)),
                        receiverDepth, name, reason);
            } else if (isOuterCoreLeg(currLeg)
                    && (tMod.getSourceDepth() < tMod.getCmbDepth()
                        || tMod.getSourceDepth() > tMod.getIocbDepth() )) {
                // not possible source not in outer core
                String reason = "Source must be in outer core for "+ currLeg;
                return failNewPhase(tMod, isCompressionalWaveSymbol(legs.get(0)), isDowngoingSymbol(legs.get(0)),
                        receiverDepth, name, reason);
            } else if ((isInnerCoreLeg(currLeg)) && (tMod.getSourceDepth() < tMod.getIocbDepth() )) {
                // not possible
                String reason = "Source must be in inner core for " + currLeg;
                return failNewPhase(tMod, isCompressionalWaveSymbol(legs.get(0)), isDowngoingSymbol(legs.get(0)),
                        receiverDepth, name, reason);
            }

            if(tMod.getSourceBranch() == 0) {
                /*
                 * p and s for zero source depth are only at zero distance and
                 * then can be called P or S.
                 */
                String reason = " Upgoing initial leg but already at surface, so no ray parameters satisfy path.";
                return failNewPhase(tMod, isCompressionalWaveSymbol(legs.get(0)), isDowngoingSymbol(legs.get(0)),
                        receiverDepth, name, reason);
            }
        } else {
            return ProtoSeismicPhase.failNewPhase(tMod, false, true,
                    receiverDepth, getName()," First phase leg not recognized: "
                    +currLeg
                    + " Must be one of P, Pg, Pn, Pdiff, p, Ped or the S equivalents in crust/mantle, "
                    + "or k, K, I, y, J, j for core sources.");
        }
        /*
         * Figure out which legs are P and S
         */
        boolean[] isLegPWave = legsArePWave(legs);
        /*
         * Now loop over all of the phase legs and construct the proper branch
         * sequence.
         */
        currLeg = "START"; // So the prevLeg isn't wrong on the first pass

        ProtoSeismicPhase proto = ProtoSeismicPhase.startEmpty(name, tMod, receiverDepth);
        for(int legNum = 0; legNum < legs.size(); legNum++) {
            prevLeg = currLeg;
            currLeg = nextLeg;
            if (legNum < legs.size() - 1) {
                nextLeg = legs.get(legNum + 1);
            } else {
                nextLeg = END_CODE;
            }

            if(DEBUG) {
                System.err.println("Iterate legs: "+legNum + "  " + prevLeg + "  cur=" + currLeg
                        + "  " + nextLeg);
            }
            if (currLeg.contentEquals(END_CODE)) {
                if (!proto.segmentList.isEmpty()) {
                    proto.endSegment().endAction = END;
                    continue;
                }
            }
            /* set currWave to be the wave type for this leg, 'P' or 'S'. */
            prevIsPWave = isPWave;
            isPWave = isLegPWave[legNum];
            boolean nextIsPWave = isPWave;
            if (legNum < isLegPWave.length-1) {
                nextIsPWave = isLegPWave[legNum + 1];
            }

            if (isCrustMantleLeg(currLeg)) {
                if (currLeg.equals("Ped") || currLeg.equals("Sed")) {
                    /* Deal with P and S exclusively downgoing case . */
                    proto = currLegIs_Ped_Sed(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                } else if (currLeg.equals("p") || currLeg.equals("s")) {
                    /* Deal with p and s case . */
                    proto = currLegIs_p_s(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                } else if (currLeg.equals("P") || currLeg.equals("S")) {
                    /* Now deal with P and S case. */
                    //special, need nextnextleg too
                    String nextNextLeg = legNum < legs.size()-2 ? legs.get(legNum+2) : END_CODE;
                    proto = currLegIs_P_S(proto, prevLeg, currLeg, nextLeg, nextNextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                } else if ((currLeg.startsWith("P") || currLeg.startsWith("S")) && isDiffracted(currLeg)) {
                    proto = currLegIs_Pdiff_Sdiff(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                } else if ((currLeg.startsWith("P") || currLeg.startsWith("S")) &&
                        (currLeg.endsWith(HEAD_CODE) || currLeg.endsWith("g"))) {
                    proto = currLegIs_Pn_Sn(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                }
            } else if (isOuterCoreLeg(currLeg)) {
                if (currLeg.equals("K")) {
                    /* Now deal with K. */
                    if (checkDegenerateOuterCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                        proto = currLegIs_K(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                    } else {
                        String reason = "DegenerateOuterCore";
                        proto.failNext(reason);
                        return proto;
                    }
                } else if (currLeg.equals("Ked")) {
                    /* Now deal with Ked. */
                    if (checkDegenerateOuterCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                        proto = currLegIs_Ked(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                    } else {
                        String reason = "DegenerateOuterCore";
                        proto.failNext(reason);
                        return proto;
                    }
                } else if (currLeg.startsWith("K") && isDiffracted(currLeg)) {
                    /* Now deal with Kdiff. */
                    if (checkDegenerateOuterCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                        proto = currLegIs_Kdiff(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                    } else {
                        String reason = "DegenerateOuterCore";
                        proto.failNext(reason);
                        return proto;
                    }
                } else if (currLeg.equals("k")) {
                    /* Deal with k case . */
                    if (checkDegenerateOuterCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                        proto = currLegIs_k(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                    } else {
                        String reason = "DegenerateOuterCore";
                        proto.failNext(reason);
                        return proto;
                    }
                }
            } else if (isInnerCoreLeg(currLeg)) {
                if (currLeg.equals("I") || currLeg.equals("J")) {
                    /* And now consider inner core, I and J. */
                    if (checkDegenerateInnerCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                        String nextNextLeg = legNum < legs.size()-2 ? legs.get(legNum+2) : END_CODE;
                        proto = currLegIs_I_J(proto, prevLeg, currLeg, nextLeg, nextNextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                    } else {
                        String reason = "DegenerateInnerCore";
                        proto.failNext(reason);
                        return proto;
                    }
                } else if ((currLeg.startsWith("I") || currLeg.startsWith("J")) && isDiffracted(currLeg)) {
                    /* Now deal with I5500diff. */
                    if (checkDegenerateInnerCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                        proto = currLegIs_I_Jdiff(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                    } else {
                        String reason = "DegenerateInnerCore";
                        proto.failNext(reason);
                        return proto;
                    }

                } else if (currLeg.equals("Ied") || currLeg.equals("Jed")) {
                    /* And now consider inner core, Ied and Jed. */
                    if (checkDegenerateInnerCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                        String nextNextLeg = legNum < legs.size()-2 ? legs.get(legNum+2) : END_CODE;
                        proto = currLegIs_Ied_Jed(proto, prevLeg, currLeg, nextLeg, nextNextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                    } else {
                        String reason = "DegenerateInnerCore";
                        proto.failNext(reason);
                        return proto;
                    }
                } else if (currLeg.equals("y") || currLeg.equals("j")) {
                    /* And now consider upgoing inner core, y and j. */
                    if (checkDegenerateInnerCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                        proto = currLegIs_y_j(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                    } else {
                        String reason = "DegenerateInnerCore";
                        proto.failNext(reason);
                        return proto;
                    }
                }
            } else if (currLeg.equals("m")
                    || currLeg.equals("c") || currLeg.equals("cx")
                    || currLeg.equals("i") || currLeg.equals("ix")) {
                if (currLeg.equals("c") || currLeg.equals("cx")) {
                    if (!checkDegenerateOuterCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                        String reason = "DegenerateOuterCore";
                        proto.failNext(reason);
                        return proto;
                    }
                }
                if (currLeg.equals("i") || currLeg.equals("ix")) {
                    if (!checkDegenerateInnerCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                        String reason = "DegenerateInnerCore";
                        proto.failNext(reason);
                        return proto;
                    }
                }
                if (nextLeg.equals(END_CODE)) {
                    return failWithMessage(proto," Phase not recognized (12): "
                            + currLeg + " as last leg, then " + nextLeg);
                }
            } else if (currLeg.startsWith("^")) {
                if (nextLeg.equals(END_CODE)) {
                    return failWithMessage(proto," Phase not recognized (12): "
                            + currLeg + " as last leg, then " + nextLeg);
                }
                // nothing to do as will have been handled by previous leg
            } else if (currLeg.startsWith("v") || currLeg.startsWith("V")) {
                if (nextLeg.equals(END_CODE)) {
                    return failWithMessage(proto," Phase not recognized (12): "
                            + currLeg + " as last leg, then " + nextLeg);
                }
                String depthString;
                depthString = currLeg.substring(1);
                int b = LegPuller.closestBranchToDepth(tMod, depthString);
                if (b == 0) {
                    return failWithMessage(proto," Phase not recognized: " + currLeg + " looks like a top side reflection at the free surface.");
                }
            } else if (LegPuller.isBoundary(currLeg)) {
                // check for phase like P0s, but could also be P2s if first discon is deeper
                int b = LegPuller.closestBranchToDepth(tMod, currLeg);
                if (b == 0 && (nextLeg.equals("p") || nextLeg.equals("s"))) {
                    return failWithMessage(proto," Phase not recognized: " + currLeg
                            + " followed by " + nextLeg + " looks like a upgoing wave from the free surface as closest discontinuity to " + currLeg + " is zero depth.");
                }
            } else if (currLeg.endsWith(HEAD_CODE)) {
                // non-standard head wave
                proto = currLegIs_OtherHead(proto, prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
            } else {
                return failWithMessage(proto," Phase not recognized (10): " + currLeg
                        + " followed by " + nextLeg);
            }
            if (proto.isFail ) {
                // phase has no arrivals, so stop looping over legs
                break;
            } else {
                prevEndAction = proto.endSegment().endAction;
            }
        }
        if (proto.isSuccessful() ) {
            if ((proto.endSegment().isDownGoing && proto.endSegment().endBranch != downgoingRecBranch)
                || (!proto.endSegment().isDownGoing && proto.endSegment().endBranch != upgoingRecBranch)) {
                return failWithMessage(proto," Phase does not end at the receiver branch, last: "+proto.endSegment());
            }

            if (DEBUG) {
                System.err.println("Last action is: "+proto.endSegment());
            }

        }

        // need to divide distance for any flat segments, which don't have a natural propogation distance, as a fraction
        // of the entire path
        double fractionOfPath = 1.0 / proto.countFlatLegs();
        for (SeismicPhaseSegment seg : proto.segmentList) {
            if (seg.isFlat) {
                seg.flatFractionOfPath = fractionOfPath;
            }
        }
        if (proto.segmentList.isEmpty()) {
            throw new TauModelException("seg list is zero??? "+name);
        }
        return proto;
    }

    ProtoSeismicPhase currLegIs_p_s(ProtoSeismicPhase proto,
                                   String prevLeg, String currLeg, String nextLeg,
                                   boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = calcStartBranch(proto, currLeg);
        if (tMod.getVelocityModel().cmbDepth == 0) {
            String reason = "no crust or mantle, so no P or p";
            return failWithMessage(proto, reason);
        }
        if(nextLeg.startsWith("v") || nextLeg.startsWith("V")) {
            return failWithMessage(proto," p and s and k must always be up going "
                    + " and cannot come immediately before a top-side reflection."
                    + " currLeg=" + currLeg + " nextLeg=" + nextLeg);
        } else if(nextLeg.equals("p") || nextLeg.equals("s")) {
            return failWithMessage(proto," Phase not recognized (2): "
                    + currLeg + " followed by " + nextLeg);
        } else if(nextLeg.startsWith("^")) {
            String depthString;
            depthString = nextLeg.substring(1);
            endAction = REFLECT_UNDERSIDE;
            int disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
            if(currBranch >= disconBranch) {
                proto.addToBranch(
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                return failWithMessage(proto," Phase not recognized (2): "
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
        } else if(nextLeg.startsWith("P") || nextLeg.startsWith("S")
                || nextLeg.equals(END_CODE)) {
            int disconBranch;
            if (nextLeg.equals(END_CODE)) {
                disconBranch = upgoingRecBranch;
                if (currBranch < upgoingRecBranch) {
                    String reason = " (currBranch "+currBranch+" < receiverBranch() "
                            + upgoingRecBranch
                            + ", so there cannot be a upgoing "
                            + currLeg
                            + " phase for this sourceDepth, receiverDepth and/or path.";
                    return failWithMessage(proto, reason);
                }
            } else {
                disconBranch = 0;
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

        } else if(nextLeg.equals("c") || nextLeg.equals("i")
                || nextLeg.equals("I") || nextLeg.equals("J") || nextLeg.equals("j")) {
            return failWithMessage(proto," Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", p and s must be upgoing in mantle and so cannot hit core.");
        } else if(isLegDepth(nextLeg)) {
            double nextLegDepth = Double.parseDouble(nextLeg);
            if (nextLegDepth >= tMod.getCmbDepth()) {
                return failWithMessage(proto," Phase not recognized (3): "
                        + currLeg + " followed by " + nextLeg+", p and s must be upgoing in mantle and so cannot hit core.");
            }
            int disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
            endAction = TRANSUP;
            proto.addToBranch(
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else {
            return failWithMessage(proto," Phase not recognized (3 else): "+legNum+" "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }

    ProtoSeismicPhase currLegIs_P_S(ProtoSeismicPhase proto,
                                   String prevLeg, String currLeg, String nextLeg, String nextNextLeg,
                                   boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = calcStartBranch(proto, currLeg);
        if (tMod.getVelocityModel().cmbDepth == 0) {
            String reason = "no crust or mantle, so no P or S";
            return failWithMessage(proto, reason);
        }
        if(nextLeg.equals("P") || nextLeg.equals("S")
                || nextLeg.equals("Ped") || nextLeg.equals("Sed")
                || nextLeg.equals("Pn") || nextLeg.equals("Sn")
                || nextLeg.equals("Pg") || nextLeg.equals("Sg")
                || nextLeg.equals("Pb") || nextLeg.equals("Sb")
                || nextLeg.equals("Pdiff") || nextLeg.equals("Sdiff")
                || nextLeg.equals(END_CODE)) {
            if(prevEndAction == START || prevEndAction == TRANSDOWN || prevEndAction == REFLECT_UNDERSIDE|| prevEndAction == REFLECT_UNDERSIDE_CRITICAL) {
                // was downgoing, so must first turn in mantle
                endAction = TURN;
                proto.addToBranch(
                        tMod.getCmbBranch() - 1,
                        isPWave,
                        isPWave, //next same as curr for turn
                        endAction,
                        currLeg);
            }
            if (nextLeg.equals(END_CODE)) {
                endAction = END;
                proto.addToBranch(upgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
            } else {
                endAction = REFLECT_UNDERSIDE;
                proto.addToBranch(0, isPWave, nextIsPWave, endAction, currLeg);
            }
        } else if(nextLeg.startsWith("v") || nextLeg.startsWith("V") ) {
            if (nextLeg.startsWith("V")) {
                endAction = REFLECT_TOPSIDE_CRITICAL;
            } else {
                endAction = REFLECT_TOPSIDE;
            }
            int disconBranch = LegPuller.closestBranchToDepth(tMod,
                    nextLeg.substring(1));
            if(currBranch <= disconBranch - 1) {
                proto.addToBranch(
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                // can't topside reflect if already below, setting maxRayParam forces no arrivals
                String reason = "can't topside reflect if already below";
                return failWithMessage(proto, reason);
            }
        } else if(nextLeg.startsWith("^")) {
            String depthString;
            depthString = nextLeg.substring(1);
            endAction = REFLECT_UNDERSIDE;
            int disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
            if (disconBranch == tMod.getNumBranches()) {
                String reason = "Attempt to underside reflect from center of earth: "+nextLeg;
                return failWithMessage(proto, reason);
            }
            if(prevLeg.equals("K")) {
                proto.addToBranch(
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if(prevLeg.startsWith("^") || prevLeg.equals("P")
                    || prevLeg.equals("S") || prevLeg.equals("p")
                    || prevLeg.equals("s") || prevLeg.equals("m")
                    || isLegDepth(prevLeg)
                    || prevLeg.equals("START")) {
                proto.addToBranch(
                        tMod.getCmbBranch() - 1,
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
            } else if(((prevLeg.startsWith("v") || prevLeg.startsWith("V"))
                    && disconBranch < LegPuller.closestBranchToDepth(tMod, prevLeg.substring(1)))
                    || (prevLeg.equals("m") && disconBranch < tMod.getMohoBranch())
                    || (prevLeg.equals("c") && disconBranch < tMod.getCmbBranch())) {
                if (disconBranch == tMod.getNumBranches()) {
                    String reason = "Attempt to reflect from center of earth: "+nextLeg;
                    return failWithMessage(proto, reason);
                }
                proto.addToBranch(
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                return failWithMessage(proto," Phase not recognized (5): "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch
                        + " > disconBranch=" + disconBranch+" , prev="+prevLeg);
            }
        } else if(nextLeg.equals("c")) {
            if (tMod.getCmbBranch() == tMod.getNumBranches()) {
                String reason = "Attempt to reflect from center of earth: "+nextLeg;
                return failWithMessage(proto, reason);
            }
            endAction = REFLECT_TOPSIDE;
            proto.addToBranch(
                    tMod.getCmbBranch() - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.equals("K") && prevLeg.equals("K")) {
            return failWithMessage(proto," Phase not recognized (5.5): "
                    + currLeg + " followed by " + nextLeg
                    + " and preceeded by "+prevLeg
                    + " when currBranch=" + currBranch
                    );
        } else if(nextLeg.startsWith("K") ) {
            endAction = TRANSDOWN;
            proto.addToBranch(
                    tMod.getCmbBranch() - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if( nextLeg.equals("I") || nextLeg.equals("J")) {
            if(tMod.getCmbDepth() == tMod.getIocbDepth()) {
                // degenerate case of no fluid outer core, so allow phases like PIP or SJS
                endAction = TRANSDOWN;
                proto.addToBranch(
                        tMod.getCmbBranch() - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                return failWithMessage(proto," P or S followed by I or J can only exist if model has no outer core: "
                        + currLeg
                        + " followed by "
                        + nextLeg);
            }
        } else if( LegPuller.isBoundary(nextLeg) && ( nextLeg.equals("m")
                || (0 < LegPuller.legAsDepthBoundary(tMod, nextLeg) && LegPuller.legAsDepthBoundary(tMod, nextLeg) < tMod.getCmbDepth()))) {
            // treat the moho in the same wasy as 410 type
            // discontinuities
            int disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
            if (DEBUG) {
                System.err.println("DisconBranch=" + disconBranch
                        + " for " + nextLeg);
                System.err.println(tMod.getTauBranch(disconBranch,
                                isPWave)
                        .getTopDepth());
            }
            if (prevEndAction == TURN || prevEndAction == REFLECT_TOPSIDE
                    || prevEndAction == REFLECT_TOPSIDE_CRITICAL || prevEndAction == TRANSUP) {
                // upgoing section
                if (disconBranch > currBranch) {
                    // check for discontinuity below the current branch
                    // when the ray should be upgoing
                    return failWithMessage(proto," Phase not recognized (6): "
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
                if (nextNextLeg.equals("p") || nextNextLeg.equals("s")) {
                    // convert on upgoing section
                    endAction = TURN;
                    proto.addToBranch(
                            tMod.getCmbBranch() - 1,
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
                } else if (nextNextLeg.equals("P")
                        || nextNextLeg.equals("S")) {
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
                                + ", so this phase, "+ getName()
                                + " is illegal for this sourceDepth.";
                        return failWithMessage(proto, reason);
                    }
                } else {
                    return failWithMessage(proto," Phase not recognized (7): "
                            + currLeg
                            + " followed by "
                            + nextLeg
                            + " followed by " + nextNextLeg);
                }
            }
        } else if (nextLeg.endsWith(HEAD_CODE) && nextLeg.length() > 1) {
            String numString = nextLeg.substring(0, nextLeg.length() - 1);
            try {
                int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
                if ( ! tMod.isHeadWaveBranch(disconBranch, isPWave)) {
                    return failWithMessage(proto,"Unable to head wave, "+ currLeg+", "
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
                return failWithMessage(proto," Phase not recognized (7): "
                        + currLeg
                        + " followed by "
                        + nextLeg + " expected number but was `" + numString + "`");
            }

        } else if (nextLeg.endsWith("diff") && nextLeg.length() > 4) {
            // diff but not Pdiff or Sdiff
            String numString = nextLeg.substring(0, nextLeg.length()-4);
            try {
                int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);

                if(prevEndAction == START || prevEndAction == TRANSDOWN || prevEndAction == REFLECT_UNDERSIDE|| prevEndAction == REFLECT_UNDERSIDE_CRITICAL) {
                    // was downgoing, so must first turn in mantle
                    endAction = TURN;
                    proto.addToBranch(
                            tMod.getCmbBranch() - 1,
                            isPWave,
                            isPWave,
                            endAction,
                            currLeg);
                }
                endAction = REFLECT_UNDERSIDE;
                proto.addToBranch(0, isPWave, nextIsPWave, endAction, currLeg);

            } catch(NumberFormatException e) {
                return failWithMessage(proto," Phase not recognized (7): "
                        + currLeg
                        + " followed by "
                        + nextLeg+" expected number but was `"+numString+"`");
            }
        } else {
            return failWithMessage(proto," Phase not recognized (8): "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }

    ProtoSeismicPhase currLegIs_Ped_Sed(ProtoSeismicPhase proto,
                                       String prevLeg, String currLeg, String nextLeg,
                                       boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = calcStartBranch(proto, currLeg);
        if (tMod.getVelocityModel().cmbDepth == 0) {
            // no crust or mantle, so no P or S
            String reason = "no crust or mantle, so no P or S";
            return failWithMessage(proto, reason);
        }
        if(nextLeg.equals(END_CODE)) {
            if (receiverDepth > 0) {
                endAction = END_DOWN;
                proto.addToBranch(downgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
            } else {
                String reason = "impossible except for 0 dist 0 source depth which can be called p or P";
                return failWithMessage(proto, reason);
            }

        } else if(nextLeg.equals("Pdiff") || nextLeg.equals("Sdiff")) {
            endAction = DIFFRACT;
            proto.addToBranch(
                    tMod.getCmbBranch() - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if (nextLeg.endsWith(HEAD_CODE) && nextLeg.length() > 1) {
            String numString = nextLeg.substring(0, nextLeg.length()-1);
            double headDepth = Double.parseDouble(numString);
            int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);

            if ( ! tMod.isHeadWaveBranch(disconBranch, isPWave)) {
                return failWithMessage(proto,"Unable to head wave, "+ currLeg+", "
                        + disconBranch +", "+headDepth+ " is not positive velocity discontinuity.");
            }
            endAction = HEAD;
            proto.addToBranch(
                    disconBranch-1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if (nextLeg.endsWith("diff") && nextLeg.length() > 4) {
            String numString = nextLeg.substring(0, nextLeg.length()-4);
            double diffDepth = Double.parseDouble(numString);
            int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
            endAction = DIFFRACT;
            proto.addToBranch(
                    disconBranch - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.equals("K") || nextLeg.equals("Ked") || nextLeg.equals("Kdiff")) {
            endAction = TRANSDOWN;
            proto.addToBranch(
                    tMod.getCmbBranch() - 1,
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
            int disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
            endAction = REFLECT_TOPSIDE;
            proto.addToBranch(
                    disconBranch - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if( LegPuller.isBoundary(nextLeg)) {
            // but not m, c or i
            int disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
            endAction = TRANSDOWN;
            proto.addToBranch(
                    disconBranch - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.startsWith("v") || nextLeg.startsWith("V") ) {
            if (nextLeg.startsWith("V")) {
                endAction = REFLECT_TOPSIDE_CRITICAL;
            } else {
                endAction = REFLECT_TOPSIDE;
            }
            int disconBranch = LegPuller.closestBranchToDepth(tMod,
                    nextLeg.substring(1));
            if(currBranch <= disconBranch - 1) {
                proto.addToBranch(
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                return failWithMessage(proto," Phase not recognized (4): "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch
                        + " < disconBranch=" + disconBranch);
            }
        } else if( nextLeg.equals("I") || nextLeg.equals("J")) {
            if(tMod.getCmbDepth() == tMod.getIocbDepth()) {
                // degenerate case of no fluid outer core, so allow phases like PIP or SJS
                endAction = TRANSDOWN;
                proto.addToBranch(
                        tMod.getCmbBranch() - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                String reason = "P or S followed by I or J can only exist if model has no outer core";
                return failWithMessage(proto, reason);
            }
        } else {
            return failWithMessage(proto," Phase not recognized (1): "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }

    ProtoSeismicPhase currLegIs_Pdiff_Sdiff(ProtoSeismicPhase proto,
                                           String prevLeg, String currLeg, String nextLeg,
                                       boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = calcStartBranch(proto, currLeg);
        if (tMod.getVelocityModel().cmbDepth == 0) {
            // no crust or mantle, so no P or S
            String reason = "no crust or mantle, so no P or S";
            return failWithMessage(proto, reason);
        }
        if (currLeg.equals("Pdiff") || currLeg.equals("Sdiff")) {
            endAction = DIFFRACT;


                SeismicPhaseSegment prevSegment = !proto.segmentList.isEmpty() ? proto.endSegment() : null;

                if (currBranch < tMod.getCmbBranch() - 1 || prevEndAction == START ||
                        (currBranch == tMod.getCmbBranch()-1 && prevSegment != null && prevSegment.endsAtTop())
                ) {
                    proto.addToBranch(
                            tMod.getCmbBranch() - 1,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                } else if (currBranch == tMod.getCmbBranch() - 1
                        && (prevSegment.endAction == DIFFRACT || prevSegment.endAction == TRANSUPDIFFRACT
                        || prevSegment.endAction == TRANSUP)) {
                    // already at correct depth ?
                } else {
                    // we are below at the right branch to diffract???
                    return failWithMessage(proto,"Unable to diffract, " + currBranch + " to cmb " + (tMod.getCmbBranch() - 1) + " " + endActionString(prevEndAction) + " " + prevSegment);
                }
                if ( ! tMod.isDiffractionBranch(tMod.getCmbBranch(), isPWave)) {
                    return failWithMessage(proto,"Unable to diffract, " + currBranch + " to cmb "
                            + (tMod.getCmbBranch() - 1)+", CMB is not velocity discontinuity.");
                }
                if (nextLeg.startsWith("K") || nextLeg.equals("I") || nextLeg.equals("J")) {
                    // down into inner core
                    proto.addFlatBranch(isPWave, endAction, TRANSDOWN, currLeg);
                } else {
                    // normal case
                    proto.addFlatBranch(isPWave, endAction, DIFFRACTTURN, currLeg);
                }
                if (nextLeg.equals(END_CODE)) {
                    endAction = END;
                    proto.addToBranch(
                            upgoingRecBranch,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                } else if (nextLeg.equals("K") || nextLeg.equals("Ked")) {
                    endAction = TRANSDOWN;
                    currBranch++;
                } else if (nextLeg.startsWith("^")) {
                    String depthString;
                    depthString = nextLeg.substring(1);
                    endAction = REFLECT_UNDERSIDE;
                    int disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
                    if (disconBranch >= tMod.getCmbBranch()) {
                        String reason = "Attempt to underside reflect " + currLeg
                                + " from deeper layer: " + nextLeg;
                        return failWithMessage(proto, reason);
                    }
                    proto.addToBranch(
                            disconBranch,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);

                } else if (nextLeg.startsWith("P") || nextLeg.startsWith("S")) {
                    endAction = REFLECT_UNDERSIDE;
                    proto.addToBranch(
                            0,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                } else if (nextLeg.equals("p") || nextLeg.equals("s")) {
                    // upgoing
                } else {
                    return failWithMessage(proto, " Phase not recognized (12): "
                            + currLeg + " followed by " + nextLeg
                            + " when currBranch=" + currBranch);
                }

        } else if (currLeg.endsWith("diff") && currLeg.length()>5) {
            int depthIdx = 0;
            if (currLeg.startsWith("P") || currLeg.startsWith("S")) {
                depthIdx = 1;
            }
            String numString = currLeg.substring(depthIdx, currLeg.length()-4);
            double diffDepth = Double.parseDouble(numString);
            int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
            SeismicPhaseSegment prevSegment = !proto.segmentList.isEmpty() ? proto.endSegment() : null;

            endAction = DIFFRACT;
            if (currBranch < disconBranch - 1 || prevEndAction == START ||
                    (currBranch == disconBranch-1 && prevSegment != null && prevSegment.endsAtTop())
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
                return failWithMessage(proto,"Unable to diffract, " + currBranch +" of "+proto.phaseName+" "+ (disconBranch - 1) + " " + endActionString(prevEndAction) + " " + prevSegment);
            }

            if ( ! tMod.isDiffractionBranch(disconBranch, isPWave)) {
                return failWithMessage(proto,"Unable to diffract " + currLeg + ", "
                        + disconBranch+", "+numString+" is not velocity discontinuity.");
            }
            // is possible to diffract downward? maybe if low velocity zone??
            // normal case
            proto.addFlatBranch(isPWave, endAction, DIFFRACTTURN, currLeg);


            if(nextLeg.equals(END_CODE)) {
                endAction = END;
                proto.addToBranch(
                        upgoingRecBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if (nextLeg.startsWith("^")) {
                String depthString;
                depthString = nextLeg.substring(1);
                endAction = REFLECT_UNDERSIDE;
                int reflectDisconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
                if (reflectDisconBranch >= disconBranch ) {
                    String reason = "Attempt to underside reflect " + currLeg
                            + " from deeper layer: " + nextLeg;
                    return failWithMessage(proto, reason);
                }
                proto.addToBranch(
                        reflectDisconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if (nextLeg.startsWith("P") || nextLeg.startsWith("S")) {
                endAction = REFLECT_UNDERSIDE;
                proto.addToBranch(
                        0,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            }

        } else {
            return failWithMessage(proto, " Phase not recognized for P,S: "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }

    ProtoSeismicPhase currLegIs_Pn_Sn(ProtoSeismicPhase proto,
                                     String prevLeg, String currLeg, String nextLeg,
                                       boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = calcStartBranch(proto, currLeg);
        if (tMod.getVelocityModel().cmbDepth == 0) {
            String reason ="no crust or mantle, so no P or S";
            return failWithMessage(proto, reason);
        }
        if (currLeg.endsWith(HEAD_CODE) && currLeg.length()>2) {
            int depthIdx = 0;
            if (currLeg.startsWith("P") || currLeg.startsWith("S")) {
                depthIdx = 1;
            }
            String numString = currLeg.substring(depthIdx, currLeg.length()-1);
            double headDepth = Double.parseDouble(numString);
            int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);

            if ( ! tMod.isHeadWaveBranch(disconBranch, isPWave)) {
                return failWithMessage(proto,"Unable to head wave, "+ currLeg+", "
                        + disconBranch +", "+headDepth+ " is not positive velocity discontinuity.");
            }
            endAction = HEAD;
            proto.addToBranch(
                    disconBranch-1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
            if (nextLeg.startsWith("K") || nextLeg.equals("I") || nextLeg.equals("J")) {
                // down into  core
                proto.addFlatBranch(isPWave, endAction, TRANSDOWN, currLeg);
            } else {
                // normal case
                proto.addFlatBranch(isPWave, endAction, TRANSUP, currLeg);
            }
            currBranch=disconBranch;

            if(nextLeg.equals(END_CODE)) {
                endAction = END;
                proto.addToBranch(
                        upgoingRecBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            }
        } else if(currLeg.equals("Pg") || currLeg.equals("Sg")
                || currLeg.equals("Pn") || currLeg.equals("Sn")) {
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
                return failWithMessage(proto, reason);
            }
            if(currLeg.equals("Pg") || currLeg.equals("Sg")) {
                endAction = TURN;
                proto.addToBranch(
                        tMod.getMohoBranch() - 1,
                        isPWave,
                        isPWave,
                        endAction,
                        currLeg);
                if(nextLeg.equals(END_CODE)) {
                    endAction = END;
                    proto.addToBranch(upgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
                } else if(nextLeg.startsWith("P") || nextLeg.startsWith("S")) {
                    endAction = REFLECT_UNDERSIDE;
                    proto.addToBranch(0, isPWave, nextIsPWave, endAction, currLeg);
                } else if(nextLeg.startsWith("^")) {
                    String depthString;
                    depthString = nextLeg.substring(1);
                    endAction = REFLECT_UNDERSIDE;
                    int disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
                    if (disconBranch >= tMod.getMohoBranch()) {
                        String reason = "Attempt to underside reflect "+currLeg+" from deeper layer: "+nextLeg;
                        return failWithMessage(proto, reason);
                    }
                    proto.addToBranch(
                            disconBranch,
                                isPWave,
                                nextIsPWave,
                                endAction,
                                currLeg);

                } else {
                    return failWithMessage(proto, " Phase not recognized (12): "
                            + currLeg + " followed by " + nextLeg);
                }
            } else if(currLeg.equals("Pn") || currLeg.equals("Sn")) {
                    endAction = HEAD;
                    proto.addToBranch(
                            tMod.getMohoBranch()-1,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                    if (nextLeg.equals("Ped") || nextLeg.equals("Sed")) {
                        // down into  core
                        proto.addFlatBranch(isPWave, endAction, TRANSDOWN, currLeg);
                    } else {
                        // normal case
                        proto.addFlatBranch(isPWave, endAction, TRANSUP, currLeg);
                    }

                    if(nextLeg.equals(END_CODE)) {
                        endAction = END;
                        if (currBranch >= upgoingRecBranch) {
                            proto.addToBranch(
                                    upgoingRecBranch,
                                    isPWave,
                                    nextIsPWave,
                                    endAction,
                                    currLeg);
                        } else {
                            String reason = "Cannot have the moho head wave "
                                    + currLeg + " within phase " + name
                                    + " for this sourceDepth, receiverDepth and/or path.";
                            return failWithMessage(proto, reason);
                        }
                    } else if ( nextLeg.startsWith("P") || nextLeg.startsWith("S")) {
                        endAction = REFLECT_UNDERSIDE;
                        proto.addToBranch(
                                0,
                                isPWave,
                                nextIsPWave,
                                endAction,
                                currLeg);
                    } else if(nextLeg.startsWith("^")) {
                        String depthString;
                        depthString = nextLeg.substring(1);
                        endAction = REFLECT_UNDERSIDE;
                        int disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
                        if (disconBranch >= tMod.getMohoBranch()) {
                            String reason = " Attempt to underside reflect "+currLeg
                                    +" from deeper layer: "+nextLeg;
                            return failWithMessage(proto, reason);
                        }
                        proto.addToBranch(
                                disconBranch,
                                isPWave,
                                nextIsPWave,
                                endAction,
                                currLeg);
                    } else {
                        return failWithMessage(proto, " Phase not recognized (12): "
                                + currLeg + " followed by " + nextLeg);
                    }
            } else {
                return failWithMessage(proto, " Phase not recognized for P,S: "
                        + currLeg + " followed by " + nextLeg);
            }
        } else {
            return failWithMessage(proto, " Phase not recognized for P,S: "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }

    ProtoSeismicPhase currLegIs_OtherHead(ProtoSeismicPhase proto,
                                         String prevLeg, String currLeg, String nextLeg,
                                         boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = calcStartBranch(proto, currLeg);
        if (currLeg.endsWith(HEAD_CODE) && currLeg.length()>2) {
            int depthIdx = 0;
            if (currLeg.startsWith("P") || currLeg.startsWith("S") || currLeg.startsWith("K")
                    || currLeg.startsWith("I") || currLeg.startsWith("J")) {
                depthIdx = 1;
            }
            String numString = currLeg.substring(depthIdx, currLeg.length()-1);
            double headDepth = Double.parseDouble(numString);
            int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);

            if ( ! tMod.isHeadWaveBranch(disconBranch, isPWave)) {
                return failWithMessage(proto,"Unable to head wave, "+ currLeg+", "
                        + disconBranch +", "+headDepth+ " is not positive velocity discontinuity.");
            }
            if(nextLeg.equals(END_CODE)) {
                endAction = END;
                proto.addToBranch(
                        upgoingRecBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
                // should handle other nextLeg besides END ???
            } else {
                return failWithMessage(proto, " Phase not recognized for non-standard diffraction: "
                        + currLeg + " followed by " + nextLeg);
            }
        } else {
            return failWithMessage(proto,  " Phase not recognized for non-standard diffraction: "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }

    boolean checkDegenerateOuterCore(String prevLeg, String currLeg, String nextLeg,
                                 boolean isPWave, boolean isPWavePrev, int legNum)
            throws TauModelException {
        if (tMod.getCmbDepth() == tMod.getRadiusOfEarth()) {
            // degenerate case, CMB is at center, so model without a core
            if(DEBUG) {
                System.err.println("Cannot have K phase "
                        + currLeg + " within phase " + name
                        + " for this model as it has no core, cmb depth = radius of Earth.");
            }
            return false;
        }
        if (tMod.getCmbDepth() == tMod.getIocbDepth()) {
            // degenerate case, CMB is same as IOCB, so model without an outer core
            if(DEBUG) {
                System.err.println("Cannot have K phase "
                        + currLeg + " within phase " + name
                        + " for this model as it has no outer core, cmb depth = iocb depth, "+tMod.getCmbDepth());
            }
            return false;
        }
        return true;
    }

    ProtoSeismicPhase currLegIs_Kdiff(ProtoSeismicPhase proto,
                                     String prevLeg, String currLeg, String nextLeg,
                                     boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = calcStartBranch(proto, currLeg);

        if (tMod.getCmbBranch() == tMod.tauBranches[0].length || tMod.getIocbBranch() == tMod.tauBranches[0].length) {
            // cmb or iocb is center of earth, no core or inner core to diffract
            String reason = "mb or iocb is center of earth, no core or inner core to diffract";
            return failWithMessage(proto, reason);
        }
        int disconBranch = tMod.getIocbBranch();

        if (currLeg.equals("Kdiff")) {
            disconBranch = tMod.getIocbBranch();
        } else if (currLeg.endsWith("diff") && currLeg.length() > 5) {
            int depthIdx = 0;
            if (currLeg.startsWith("K")) {
                depthIdx = 1;
            }
            String numString = currLeg.substring(depthIdx, currLeg.length() - 4);
            double diffDepth = Double.parseDouble(numString);
            disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
        } else {
            throw new TauModelException("Not Kdiff " + currLeg + " in currLegIs_Kdiff " + getName());
        }

        if ( ! tMod.isDiffractionBranch(disconBranch, isPWave)) {
            return failWithMessage(proto,"Unable to diffract " + currLeg + ", "
                    + disconBranch+" is not negative velocity discontinuity.");
        }
        endAction = DIFFRACT;
        proto.addToBranch(
                disconBranch - 1,
                PWAVE,
                nextIsPWave,
                endAction,
                currLeg);
        if (nextLeg.equals("I") || nextLeg.equals("J")) {
            // down into inner core
            proto.addFlatBranch(isPWave, endAction, TRANSDOWN, currLeg);
        } else {
            // normal case
            proto.addFlatBranch(isPWave, endAction, DIFFRACTTURN, currLeg);
        }

        if (nextLeg.startsWith("P") || nextLeg.startsWith("S") || nextLeg.equals("p") || nextLeg.equals("s")) {
            endAction = TRANSUP;
            proto.addToBranch(
                    tMod.getCmbBranch(),
                    PWAVE,
                    nextIsPWave,
                    endAction,
                    currLeg);

        } else if (nextLeg.equals("K") || (nextLeg.startsWith("K") && nextLeg.endsWith(DIFF))) {
            endAction = REFLECT_UNDERSIDE;
            proto.addToBranch(
                    tMod.getCmbBranch(),
                    PWAVE,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if (nextLeg.startsWith("^")) {
            String reflectdepthString = nextLeg.substring(1);
            endAction = REFLECT_UNDERSIDE;
            int reflectBranch = LegPuller.closestBranchToDepth(tMod, reflectdepthString);
            if (reflectBranch < tMod.getCmbBranch()) {
                return failWithMessage(proto, " Phase not recognized (5a): "
                        + currLeg + " followed by " + nextLeg
                        + " when reflectBranch=" + reflectBranch
                        + " < cmbBranch=" + tMod.getCmbBranch() + ", likely need P or S leg , prev=" + prevLeg);
            }
            if (reflectBranch >= disconBranch) {
                return failWithMessage(proto,  " Phase not recognized (5b): "
                        + currLeg + " followed by " + nextLeg
                        + " when reflectBranch=" + reflectBranch
                        + " > disconBranch=" + disconBranch + ", likely need K, I or J leg , prev=" + prevLeg);
            }
            if (reflectBranch == tMod.getNumBranches()) {
                String reason = "Attempt to underside reflect from center of earth: " + nextLeg;
                return failWithMessage(proto, reason);
            }
            proto.addToBranch(
                    reflectBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else {
            throw new TauModelException("Should not allow " + currLeg + " followed by " + nextLeg);
        }
        return proto;

    }

    ProtoSeismicPhase currLegIs_K(ProtoSeismicPhase proto,
                                 String prevLeg, String currLeg, String nextLeg,
                                 boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = calcStartBranch(proto, currLeg);
        if (currLeg.equals("K")) {
            if (tMod.getCmbBranch() == tMod.tauBranches[0].length) {
                // cmb is center of earth, no core
                String reason = "cmb is center of earth, no core";
                return failWithMessage(proto, reason);
            }
            if(nextLeg.equals("P") || nextLeg.equals("S")
                || nextLeg.equals("p") || nextLeg.equals("s")
                || nextLeg.equals("Pdiff") || nextLeg.equals("Sdiff")) {
                if (prevLeg.equals("P") || prevLeg.equals("S")
                        || prevLeg.equals("Ped") || prevLeg.equals("Sed")
                        || prevLeg.equals("Pdiff") || prevLeg.equals("Sdiff")
                        || prevLeg.equals("K") || prevLeg.equals("k")
                        || prevLeg.startsWith("^")
                        || prevLeg.equals("START")) {
                    endAction = TURN;
                    proto.addToBranch(
                            tMod.getIocbBranch() - 1,
                            isPWave,
                            isPWave,
                            endAction,
                            currLeg);
                }
                if (nextLeg.equals("Pdiff") || nextLeg.equals("Sdiff")) {
                    endAction = TRANSUPDIFFRACT;
                } else {
                    endAction = TRANSUP;
                }

                proto.addToBranch(
                        tMod.getCmbBranch(),
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if(nextLeg.equals("K") || nextLeg.equals("Ked") || nextLeg.equals(END_CODE)) {
                if(prevLeg.equals("P") || prevLeg.equals("S")
                        || prevLeg.equals("Ped") || prevLeg.equals("Sed")
                        || prevLeg.equals("Pdiff") || prevLeg.equals("Sdiff")
                        || prevLeg.equals("K") || prevLeg.equals("k")
                        || prevLeg.startsWith("^")
                        || prevLeg.equals("START")) {
                    endAction = TURN;
                    proto.addToBranch(
                            tMod.getIocbBranch() - 1,
                            isPWave,
                            isPWave,
                            endAction,
                            currLeg);
                }
                if (nextLeg.equals(END_CODE)) {
                    endAction = END;
                    proto.addToBranch(upgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
                } else {
                    endAction = REFLECT_UNDERSIDE;
                    proto.addToBranch(
                            tMod.getCmbBranch(),
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                }
            } else if(nextLeg.startsWith("I") || nextLeg.startsWith("J")) {
                endAction = TRANSDOWN;
                proto.addToBranch(
                        tMod.getIocbBranch() - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if(nextLeg.equals("i")) {
                if (tMod.getIocbBranch() == tMod.getNumBranches()) {
                    String reason = "Attempt to reflect from center of earth: " + nextLeg;
                    return failWithMessage(proto, reason);
                }
                endAction = REFLECT_TOPSIDE;
                proto.addToBranch(
                        tMod.getIocbBranch() - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if(nextLeg.startsWith("v") || nextLeg.startsWith("V") ) {
                if (nextLeg.startsWith("V")) {
                    endAction = REFLECT_TOPSIDE_CRITICAL;
                } else {
                    endAction = REFLECT_TOPSIDE;
                }
                int disconBranch = LegPuller.closestBranchToDepth(tMod,
                        nextLeg.substring(1));
                if(currBranch <= disconBranch - 1) {
                    proto.addToBranch(
                            disconBranch - 1,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                } else {
                    return failWithMessage(proto, " Phase not recognized (4): "
                            + currLeg + " followed by " + nextLeg
                            + " when currBranch=" + currBranch
                            + " < disconBranch=" + disconBranch);
                }
            } else if(nextLeg.startsWith("^")) {
                String depthString;
                depthString = nextLeg.substring(1);
                endAction = REFLECT_UNDERSIDE;
                int disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
                if (disconBranch < tMod.getCmbBranch()) {
                    return failWithMessage(proto, " Phase not recognized (5a): "
                            + currLeg + " followed by " + nextLeg
                            + " when disconBranch=" + disconBranch
                            + " < cmbBranch=" + tMod.getCmbBranch() + ", likely need P or S leg , prev=" + prevLeg);
                }
                if (disconBranch >= tMod.getIocbBranch()) {
                    return failWithMessage(proto, " Phase not recognized (5b): "
                            + currLeg + " followed by " + nextLeg
                            + " when disconBranch=" + disconBranch
                            + " > iocbBranch=" + tMod.getIocbBranch() + ", likely need Ior J leg , prev=" + prevLeg);
                }
                if (disconBranch == tMod.getNumBranches()) {
                    String reason = "Attempt to underside reflect from center of earth: " + nextLeg;
                    return failWithMessage(proto, reason);
                }
                if (prevLeg.startsWith("I") || prevLeg.startsWith("J")
                        || prevLeg.equals("i") || prevLeg.equals("j")
                        || prevLeg.equals("k")) {
                    // upgoind K leg
                    proto.addToBranch(
                            disconBranch,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                } else if (prevLeg.equals("P") || prevLeg.equals("S") ||
                        prevLeg.startsWith("^") ||
                        prevLeg.equals("K") || prevLeg.equals("START")) {
                    proto.addToBranch(
                            tMod.getIocbBranch() - 1,
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
                } else if (prevLeg.startsWith("v") || prevLeg.startsWith("V")) {
                    String prevDepthString = prevLeg.substring(1);
                    int prevdisconBranch = LegPuller.closestBranchToDepth(tMod, prevDepthString);
                    if (disconBranch < prevdisconBranch) {
                        // upgoind K leg
                        proto.addToBranch(
                                disconBranch,
                                isPWave,
                                nextIsPWave,
                                endAction,
                                currLeg);
                    } else {
                        // down-turn-up
                        proto.addToBranch(
                                tMod.getIocbBranch() - 1,
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
                    }
                } else {
                    return failWithMessage(proto, " Phase not recognized (5): "
                            + currLeg + " followed by " + nextLeg
                            + " when currBranch=" + currBranch
                            + " > disconBranch=" + disconBranch + " , prev=" + prevLeg);
                }
            } else if (nextLeg.endsWith(HEAD_CODE) && nextLeg.length() > 1) {
                String numString = nextLeg.substring(0, nextLeg.length()-1);
                double headDepth = Double.parseDouble(numString);
                int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
                if (disconBranch < tMod.cmbBranch) {
                    return failWithMessage(proto, " Phase not recognized (5): "
                            + currLeg + " followed by " + nextLeg
                            + " when cmbBranch < disconBranch=" + disconBranch + " , prev=" + prevLeg);
                }

                if ( ! tMod.isHeadWaveBranch(disconBranch, isPWave)) {
                    return failWithMessage(proto,"Unable to head wave, "+ currLeg+", "
                            + disconBranch +", "+headDepth+ " is not positive velocity discontinuity.");
                }
                endAction = HEAD;
                proto.addToBranch(
                        disconBranch-1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if (nextLeg.endsWith("diff") && nextLeg.length() > 4) {
                String numString = nextLeg.substring(0, nextLeg.length()-4);
                double diffDepth = Double.parseDouble(numString);
                int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
                if (disconBranch < tMod.cmbBranch) {
                    return failWithMessage(proto, " Phase not recognized (5): "
                            + currLeg + " followed by " + nextLeg
                            + " when cmbBranch < disconBranch=" + disconBranch + " , prev=" + prevLeg);
                }
                endAction = DIFFRACT;
                proto.addToBranch(
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                return failWithMessage(proto, " Phase not recognized (9b): "
                        + currLeg + " followed by " + nextLeg);
            }
        } else {
            return failWithMessage(proto, " Phase not recognized (9): "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }

    ProtoSeismicPhase currLegIs_Ked(ProtoSeismicPhase proto,
                                   String prevLeg, String currLeg, String nextLeg,
                                   boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = calcStartBranch(proto, currLeg);
        if (currLeg.equals("Ked")) {
            if (tMod.getCmbBranch() == tMod.tauBranches[0].length) {
                // cmb is center of earth, no core
                String reason = "cmb is center of earth, no core";
                return failWithMessage(proto, reason);
            }
            if(nextLeg.equals(END_CODE)) {
                endAction = END_DOWN;
                proto.addToBranch(downgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
            } else if(nextLeg.equals("I") || nextLeg.equals("J")) {
                endAction = TRANSDOWN;
                proto.addToBranch(
                        tMod.getIocbBranch() - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if(nextLeg.equals("i")) {
                if (tMod.getIocbBranch() == tMod.getNumBranches()) {
                    String reason = "Attempt to reflect from center of earth: " + nextLeg;
                    return failWithMessage(proto, reason);
                }
                endAction = REFLECT_TOPSIDE;
                proto.addToBranch(
                        tMod.getIocbBranch() - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if(nextLeg.startsWith("v") || nextLeg.startsWith("V") ) {
                if (nextLeg.startsWith("V")) {
                    endAction = REFLECT_TOPSIDE_CRITICAL;
                } else {
                    endAction = REFLECT_TOPSIDE;
                }
                int disconBranch = LegPuller.closestBranchToDepth(tMod,
                        nextLeg.substring(1));
                if(currBranch <= disconBranch - 1) {
                    proto.addToBranch(
                            disconBranch - 1,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                } else {
                    return failWithMessage(proto, " Phase not recognized (4): "
                            + currLeg + " followed by " + nextLeg
                            + " when currBranch=" + currBranch
                            + " < disconBranch=" + disconBranch);
                }
            } else if (nextLeg.endsWith(HEAD_CODE) && nextLeg.length() > 1) {
                String numString = nextLeg.substring(0, nextLeg.length()-1);
                double headDepth = Double.parseDouble(numString);
                int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
                if (disconBranch < tMod.cmbBranch) {
                    return failWithMessage(proto,  " Phase not recognized (5): "
                            + currLeg + " followed by " + nextLeg
                            + " when cmbBranch < disconBranch=" + disconBranch + " , prev=" + prevLeg);
                }
                if ( ! tMod.isHeadWaveBranch(disconBranch, isPWave)) {
                    return failWithMessage(proto,"Unable to head wave, "+ currLeg+", "
                            + disconBranch +", "+headDepth+ " is not positive velocity discontinuity.");
                }
                endAction = HEAD;
                proto.addToBranch(
                        disconBranch-1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if (nextLeg.endsWith("diff") && nextLeg.length() > 4) {
                String numString = nextLeg.substring(0, nextLeg.length()-4);
                double diffDepth = Double.parseDouble(numString);
                int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
                if (disconBranch < tMod.cmbBranch) {
                    return failWithMessage(proto,  " Phase not recognized (5): "
                            + currLeg + " followed by " + nextLeg
                            + " when cmbBranch < disconBranch=" + disconBranch + " , prev=" + prevLeg);
                }
                endAction = DIFFRACT;
                proto.addToBranch(
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                return failWithMessage(proto, " Phase not recognized (9b): "
                        + currLeg + " followed by " + nextLeg);
            }
        } else {
            return failWithMessage(proto, " Phase not recognized (9): "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }

    ProtoSeismicPhase currLegIs_k(ProtoSeismicPhase proto,
                                 String prevLeg, String currLeg, String nextLeg,
                                 boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = calcStartBranch(proto, currLeg);
        if(nextLeg.startsWith("v") || nextLeg.startsWith("V")) {
            return failWithMessage(proto, " k must always be up going "
                    + " and cannot come immediately before a top-side reflection."
                    + " currLeg=" + currLeg + " nextLeg=" + nextLeg);
        } else if(nextLeg.startsWith("^")) {
            String depthString;
            depthString = nextLeg.substring(1);
            endAction = REFLECT_UNDERSIDE;
            int disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
            if (disconBranch < tMod.getCmbBranch() || disconBranch >= tMod.getIocbBranch()) {
                return failWithMessage(proto, " Phase not recognized (2): "
                        + currLeg + " followed by " + nextLeg
                        + ", discon not in outer core.");
            }
            if(currBranch >= disconBranch) {
                proto.addToBranch(
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                return failWithMessage(proto, " Phase not recognized (2): "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch
                        + " > disconBranch=" + disconBranch);
            }
        } else if(nextLeg.startsWith("P") || nextLeg.startsWith("S")
                || nextLeg.equals("p") || nextLeg.equals("s")
                || nextLeg.equals("c")
                || nextLeg.equals("K") || nextLeg.equals("Ked")
                || nextLeg.equals(END_CODE)) {
            int disconBranch;
            if (nextLeg.equals(END_CODE)) {
                disconBranch = upgoingRecBranch;
                if (currBranch < upgoingRecBranch) {
                    String reason  = name+" (currBranch >= receiverBranch() "
                            + currBranch
                            + " "
                            + upgoingRecBranch
                            + " so there cannot be a "
                            + currLeg
                            + " phase for this sourceDepth, receiverDepth and/or path.";
                    return failWithMessage(proto, reason);
                }
            } else  {
                disconBranch = tMod.getCmbBranch();
            }
            if (nextLeg.startsWith("P") || nextLeg.startsWith("S")
               || nextLeg.startsWith("p") || nextLeg.startsWith("s")
                    || nextLeg.equals("c")) {
                endAction = TRANSUP;
            } else if (nextLeg.equals(END_CODE)) {
                endAction = END;
            } else if (nextLeg.equals("K") || nextLeg.equals("Ked")) {
                endAction = REFLECT_UNDERSIDE;
            } else {
                return failWithMessage(proto, " Phase not recognized (3): "
                        + currLeg + " followed by " + nextLeg);
            }
            proto.addToBranch(
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);

        } else if(nextLeg.equals("c") ) {
            return failWithMessage(proto, " Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", k must be upgoing in outer core and so cannot hit cmb from above.");
        } else if(nextLeg.equals("i")
                || nextLeg.equals("I") || nextLeg.equals("J") || nextLeg.equals("j")) {
            return failWithMessage(proto, " Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", k must be upgoing in outer core and so cannot hit inner core.");
        } else if(nextLeg.equals("k")) {
            return failWithMessage(proto, " Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", k must be upgoing in outer core and so repeat.");
        } else if(isLegDepth(currLeg)) {
            double nextLegDepth = Double.parseDouble(currLeg);
            if (nextLegDepth >= tMod.getCmbDepth()) {
                return failWithMessage(proto, " Phase not recognized (3): "
                        + currLeg + " followed by " + nextLeg+", p and s must be upgoing in mantle and so cannot hit core.");
            }
            int disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
            if (disconBranch < tMod.getCmbBranch() || disconBranch >= tMod.getIocbBranch()) {
                return failWithMessage(proto, " Phase not recognized (2): "
                        + currLeg + " followed by " + nextLeg
                        + ", discon not in outer core.");
            }
            endAction = TRANSUP;
            proto.addToBranch(
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else {
            return failWithMessage(proto, " Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }

    boolean checkDegenerateInnerCore(String prevLeg, String currLeg, String nextLeg,
                                   boolean isPWave, boolean isPWavePrev, int legNum) {
        if (tMod.getIocbDepth() == tMod.getRadiusOfEarth()) {
            // degenerate case, IOCB is at center, so model without an inner core
            if (DEBUG) {
                System.err.println("Cannot have I or J phase "
                        + currLeg + " within phase " + name
                        + " for this model as it has no inner core, iocb depth = radius of Earth.");
            }
            return false;
        }
        return true;
    }

    ProtoSeismicPhase currLegIs_I_J(ProtoSeismicPhase proto,
                                   String prevLeg, String currLeg, String nextLeg, String nextNextLeg,
                                   boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = calcStartBranch(proto, currLeg);
        if( ! nextLeg.startsWith("v") && ! nextLeg.startsWith("V") && ! LegPuller.isBoundary(nextLeg)
                && (prevEndAction == START || prevEndAction == TRANSDOWN || prevEndAction == REFLECT_UNDERSIDE || prevEndAction == REFLECT_UNDERSIDE_CRITICAL)) {
            // was downgoing, not reflecting, so must first turn in inner core
            endAction = TURN;
            proto.addToBranch(
                    tMod.getNumBranches() - 1,
                    isPWave,
                    isPWave,
                    endAction,
                    currLeg);

        }
        // have already TURNed
        if(nextLeg.equals(END_CODE)) {
            endAction = END;
            proto.addToBranch(upgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
        } else if(nextLeg.equals("I") || nextLeg.equals("J")) {
            endAction = REFLECT_UNDERSIDE;
            proto.addToBranch(
                    tMod.getIocbBranch(),
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.equals("K") || nextLeg.equals("k")) {
            endAction = TRANSUP;
            proto.addToBranch(
                    tMod.getIocbBranch(),
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.startsWith("v") || nextLeg.startsWith("V") ) {
            if (nextLeg.startsWith("V")) {
                endAction = REFLECT_TOPSIDE_CRITICAL;
            } else {
                endAction = REFLECT_TOPSIDE;
            }
            int disconBranch = LegPuller.closestBranchToDepth(tMod,
                    nextLeg.substring(1));
            if(currBranch <= disconBranch - 1) {
                proto.addToBranch(
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                return failWithMessage(proto, " Phase not recognized (4): "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch
                        + " < disconBranch=" + disconBranch);
            }
        } else if(nextLeg.startsWith("^")) {
            String depthString;
            depthString = nextLeg.substring(1);
            endAction = REFLECT_UNDERSIDE;
            int disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
            if (disconBranch < tMod.getIocbBranch()) {
                return failWithMessage(proto, " Phase not recognized (6a): "
                        + currLeg + " followed by " + nextLeg
                        + " when disconBranch=" + disconBranch
                        +" < iocbBranch="+tMod.getIocbBranch()+", likely need K leg , prev="+prevLeg);
            }
            if (disconBranch == tMod.getNumBranches()) {
                String reason = "Attempt to underside reflect from center of earth: "+nextLeg;
                return failWithMessage(proto, reason);
            }
            proto.addToBranch(
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.equalsIgnoreCase("P") || nextLeg.equalsIgnoreCase("S")) {
            if (tMod.getCmbDepth() == tMod.getIocbDepth()) {
                // degenerate case of no fluid outer core, so allow phases like PIP or SJS
                endAction = TRANSUP;
                proto.addToBranch(
                        tMod.getIocbBranch(),
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                String reason = "Cannot have I or J phase "
                        + currLeg
                        + " followed by "+nextLeg
                        + " within phase " + name
                        + " for this model as it has an outer core so need K,k in between.";
                return failWithMessage(proto, reason);
            }
        } else if (nextLeg.endsWith(HEAD_CODE) && nextLeg.length() > 1) {
            String numString = nextLeg.substring(0, nextLeg.length()-1);
            double headDepth = Double.parseDouble(numString);
            int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
            if (disconBranch < tMod.iocbBranch) {
                return failWithMessage(proto,  " Phase not recognized (5): "
                        + currLeg + " followed by " + nextLeg
                        + " when iocbBranch < disconBranch=" + disconBranch + " , prev=" + prevLeg);
            }
            if ( ! tMod.isHeadWaveBranch(disconBranch, isPWave)) {
                return failWithMessage(proto,"Unable to head wave, "+ currLeg+", "
                        + disconBranch +", "+headDepth+ " is not positive velocity discontinuity.");
            }
            endAction = HEAD;
            proto.addToBranch(
                    disconBranch-1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if (nextLeg.endsWith("diff") && nextLeg.length() > 4) {
            String numString = nextLeg.substring(0, nextLeg.length()-4);
            double diffDepth = Double.parseDouble(numString);
            int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
            if (disconBranch < tMod.iocbBranch) {
                return failWithMessage(proto, " Phase not recognized (5): "
                        + currLeg + " followed by " + nextLeg
                        + " when iocbBranch="+tMod.getIocbBranch()+" < disconBranch=" + disconBranch + " , prev=" + prevLeg);
            }
            endAction = DIFFRACT;
            proto.addToBranch(
                    disconBranch - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if( LegPuller.isBoundary(nextLeg) &&
                 (tMod.getIocbDepth() < LegPuller.legAsDepthBoundary(tMod, nextLeg) )) {
            // conversion at inner core discontinuity
            int disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
            if (DEBUG) {
                System.err.println("DisconBranch=" + disconBranch
                        + " for " + nextLeg);
                System.err.println(tMod.getTauBranch(disconBranch,
                                isPWave)
                        .getTopDepth());
            }
            if (prevEndAction == TURN || prevEndAction == REFLECT_TOPSIDE
                    || prevEndAction == REFLECT_TOPSIDE_CRITICAL || prevEndAction == TRANSUP) {
                // upgoing section
                if (disconBranch > currBranch) {
                    // check for discontinuity below the current branch
                    // when the ray should be upgoing
                    return failWithMessage(proto," Phase not recognized (6): "
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
                if (nextNextLeg.equals("y") || nextNextLeg.equals("j")) {
                    // convert on upgoing section
                    endAction = TURN;
                    proto.addToBranch(
                            tMod.getNumBranches()-1,
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
                } else if (nextNextLeg.equals("I") || nextNextLeg.equals("Ied")
                        || nextNextLeg.equals("J")
                        || nextNextLeg.equals("Jed")) {
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
                                + ", so this phase, "+ getName()
                                + " is illegal for this sourceDepth.";
                        return failWithMessage(proto, reason);
                    }
                } else {
                    return failWithMessage(proto," Phase not recognized (7): "
                            + currLeg
                            + " followed by "
                            + nextLeg
                            + " followed by " + nextNextLeg);
                }
            }
        } else {
            return failWithMessage(proto, " Phase not recognized (6a): "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }


    ProtoSeismicPhase currLegIs_Ied_Jed(ProtoSeismicPhase proto,
                                       String prevLeg, String currLeg, String nextLeg, String nextNextLeg,
                                       boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = calcStartBranch(proto, currLeg);
        if (currLeg.equals("Ied") || currLeg.equals("Jed")) {
            if(nextLeg.equals(END_CODE)) {
                endAction = END_DOWN;
                proto.addToBranch(downgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
            } else if(nextLeg.startsWith("v") || nextLeg.startsWith("V") ) {
                if (nextLeg.startsWith("V")) {
                    endAction = REFLECT_TOPSIDE_CRITICAL;
                } else {
                    endAction = REFLECT_TOPSIDE;
                }
                int disconBranch = LegPuller.closestBranchToDepth(tMod,
                        nextLeg.substring(1));
                if(currBranch <= disconBranch - 1) {
                    proto.addToBranch(
                            disconBranch - 1,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                } else {
                    return failWithMessage(proto, " Phase not recognized (4): "
                            + currLeg + " followed by " + nextLeg
                            + " when currBranch=" + currBranch
                            + " < disconBranch=" + disconBranch);
                }
            } else if (nextLeg.endsWith(HEAD_CODE) && nextLeg.length() > 1) {
                String numString = nextLeg.substring(0, nextLeg.length()-1);
                double headDepth = Double.parseDouble(numString);
                int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
                if (disconBranch < tMod.iocbBranch) {
                    return failWithMessage(proto,  " Phase not recognized (5): "
                            + currLeg + " followed by " + nextLeg
                            + " when iocbBranch < disconBranch=" + disconBranch + " , prev=" + prevLeg);
                }
                if ( ! tMod.isHeadWaveBranch(disconBranch, isPWave)) {
                    return failWithMessage(proto,"Unable to head wave, "+ currLeg+", "
                            + disconBranch +", "+headDepth+ " is not positive velocity discontinuity.");
                }
                endAction = HEAD;
                proto.addToBranch(
                        disconBranch-1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if (nextLeg.endsWith("diff") && nextLeg.length() > 4) {
                String numString = nextLeg.substring(0, nextLeg.length()-4);
                double diffDepth = Double.parseDouble(numString);
                int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
                if (disconBranch < tMod.iocbBranch) {
                    return failWithMessage(proto,  " Phase not recognized (5): "
                            + currLeg + " followed by " + nextLeg
                            + " when iocbBranch < disconBranch=" + disconBranch + " , prev=" + prevLeg);
                }
                endAction = DIFFRACT;
                proto.addToBranch(
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if( LegPuller.isBoundary(nextLeg) &&
                    (tMod.getIocbDepth() < LegPuller.legAsDepthBoundary(tMod, nextLeg) )) {
                // conversion at inner core discontinuity
                int disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
                if (DEBUG) {
                    System.err.println("DisconBranch=" + disconBranch
                            + " for " + nextLeg);
                    System.err.println(tMod.getTauBranch(disconBranch,
                                    isPWave)
                            .getTopDepth());
                }
                if (prevEndAction == TURN || prevEndAction == REFLECT_TOPSIDE
                        || prevEndAction == REFLECT_TOPSIDE_CRITICAL || prevEndAction == TRANSUP) {
                    // upgoing section
                    if (disconBranch > currBranch) {
                        // check for discontinuity below the current branch
                        // when the ray should be upgoing
                        return failWithMessage(proto," Phase not recognized (6): "
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
                    if (nextNextLeg.equals("y") || nextNextLeg.equals("j")) {
                        // convert on upgoing section
                        endAction = TURN;
                        proto.addToBranch(
                                tMod.getNumBranches()-1,
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
                    } else if (nextNextLeg.equals("I") || nextNextLeg.equals("Ied")
                            || nextNextLeg.equals("J")
                            || nextNextLeg.equals("Jed")) {
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
                                    + ", so this phase, "+ getName()
                                    + " is illegal for this sourceDepth.";
                            return failWithMessage(proto, reason);
                        }
                    } else {
                        return failWithMessage(proto," Phase not recognized (7): "
                                + currLeg
                                + " followed by "
                                + nextLeg
                                + " followed by " + nextNextLeg);
                    }
                }            } else {
                return failWithMessage(proto, " Phase not recognized (9b): "
                        + currLeg + " followed by " + nextLeg);
            }
        } else {
            return failWithMessage(proto, " Phase not recognized (9): "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }

    ProtoSeismicPhase currLegIs_I_Jdiff(ProtoSeismicPhase proto,
                                       String prevLeg, String currLeg, String nextLeg,
                                     boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = calcStartBranch(proto, currLeg);

        if (tMod.getIocbBranch() == tMod.tauBranches[0].length ) {
            String reason = "cmb or iocb is center of earth, no core or inner core to diffract";
            return failWithMessage(proto, reason);
        }
        int disconBranch = tMod.getIocbBranch();

        if (currLeg.endsWith("diff") && currLeg.length() > 5) {
            int depthIdx = 0;
            if (currLeg.startsWith("I") || currLeg.startsWith("J")) {
                depthIdx = 1;
            }
            String numString = currLeg.substring(depthIdx, currLeg.length() - 4);
            double diffDepth = Double.parseDouble(numString);
            disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
        } else {
            throw new TauModelException("Not I or Jdiff " + currLeg + " in currLegIs_I_Jdiff " + getName());
        }

        endAction = DIFFRACT;
        proto.addToBranch(
                disconBranch - 1,
                PWAVE,
                nextIsPWave,
                endAction,
                currLeg);

        if ( ! tMod.isDiffractionBranch(disconBranch, isPWave)) {
            return failWithMessage(proto,"Unable to diffract " + currLeg + ", "
                    + disconBranch+" is not negative velocity discontinuity.");
        }
        if (nextLeg.equals("I") || nextLeg.equals("J")) {
            // down into inner core
            proto.addFlatBranch(isPWave, endAction, TRANSDOWN, currLeg);
        } else {
            // normal case
            proto.addFlatBranch(isPWave, endAction, DIFFRACTTURN, currLeg);
        }

        if (nextLeg.startsWith("K")  || nextLeg.equals("k")) {
            endAction = TRANSUP;
            proto.addToBranch(
                    tMod.getIocbBranch(),
                    PWAVE,
                    nextIsPWave,
                    endAction,
                    currLeg);

        } else if (nextLeg.equals("I") || nextLeg.equals("I")
                || ((nextLeg.startsWith("I") || nextLeg.startsWith("J")) && nextLeg.endsWith("diff"))) {
            endAction = REFLECT_UNDERSIDE;
            proto.addToBranch(
                    tMod.getIocbBranch(),
                    PWAVE,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if (nextLeg.startsWith("^")) {
            String reflectdepthString = nextLeg.substring(1);
            endAction = REFLECT_UNDERSIDE;
            int reflectBranch = LegPuller.closestBranchToDepth(tMod, reflectdepthString);
            if (reflectBranch < tMod.getIocbBranch()) {
                return failWithMessage(proto,  " Phase not recognized (5a): "
                        + currLeg + " followed by " + nextLeg
                        + " when reflectBranch=" + reflectBranch
                        + " < iocbBranch=" + tMod.getIocbBranch() + ", likely need P or S leg , prev=" + prevLeg);
            }
            if (reflectBranch >= disconBranch) {
                return failWithMessage(proto,  " Phase not recognized (5b): "
                        + currLeg + " followed by " + nextLeg
                        + " when reflectBranch=" + reflectBranch
                        + " > disconBranch=" + disconBranch + ", likely need K, I or J leg , prev=" + prevLeg);
            }
            if (reflectBranch == tMod.getNumBranches()) {
                String reason = "Attempt to underside reflect from center of earth: " + nextLeg;
                return failWithMessage(proto, reason);
            }
            proto.addToBranch(
                    reflectBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else {
            throw new TauModelException("Should not allow " + currLeg + " followed by " + nextLeg);
        }
        return proto;

    }


    ProtoSeismicPhase currLegIs_y_j(ProtoSeismicPhase proto,
                                   String prevLeg, String currLeg, String nextLeg,
                                   boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        int currBranch = calcStartBranch(proto, currLeg);
        if(nextLeg.startsWith("v") || nextLeg.startsWith("V")) {
            return failWithMessage(proto, " y,j must always be up going "
                    + " and cannot come immediately before a top-side reflection."
                    + " currLeg=" + currLeg + " nextLeg=" + nextLeg);
        } else if(nextLeg.startsWith("^")) {
            String depthString;
            depthString = nextLeg.substring(1);
            endAction = REFLECT_UNDERSIDE;
            int disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
            if (disconBranch < tMod.getIocbBranch() ) {
                return failWithMessage(proto, " Phase not recognized (2): "
                        + currLeg + " followed by " + nextLeg
                        + ", discon not in inner core.");
            }
            if(currBranch >= disconBranch) {
                proto.addToBranch(
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                return failWithMessage(proto, " Phase not recognized (2): "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch
                        + " > disconBranch=" + disconBranch);
            }
        } else if(nextLeg.startsWith("P") || nextLeg.startsWith("S")
                || nextLeg.equals("p") || nextLeg.equals("s")
                || nextLeg.equals("c")
                || nextLeg.equals("K") || nextLeg.equals("Ked")
                || nextLeg.equals("k") || nextLeg.equals("Ked")
                || nextLeg.equals("I") || nextLeg.equals("Ied")
                || nextLeg.equals("J") || nextLeg.equals("Jed")
                || nextLeg.equals(END_CODE)) {
            int disconBranch;
            if (nextLeg.equals(END_CODE)) {
                disconBranch = upgoingRecBranch;
                if (currBranch < upgoingRecBranch) {
                    String reason = name+" (currBranch >= receiverBranch() "
                            + currBranch
                            + " "
                            + upgoingRecBranch
                            + " so there cannot be a "
                            + currLeg
                            + " phase for this sourceDepth, receiverDepth and/or path.";
                    return failWithMessage(proto, reason);
                }
            } else  {
                disconBranch = tMod.getIocbBranch();
            }
            if (nextLeg.startsWith("K") || nextLeg.startsWith("k")) {
                endAction = TRANSUP;

            } else if (nextLeg.startsWith("P") || nextLeg.startsWith("S")
                    || nextLeg.startsWith("p") || nextLeg.startsWith("s")
                    || nextLeg.equals("c")) {
                if (tMod.getCmbBranch() == tMod.getIocbBranch()) {
                    // no outer core
                    endAction = TRANSUP;
                } else {
                    return failWithMessage(proto, " Phase not recognized (3): "
                            + currLeg + " followed by " + nextLeg+", outer core leg would be K or k");
                }
            } else if (nextLeg.equals(END_CODE)) {
                endAction = END;
            } else if (nextLeg.equals("I") || nextLeg.equals("Ied")
                || nextLeg.equals("J") || nextLeg.equals("Jed")) {
                endAction = REFLECT_UNDERSIDE;
            } else {
                return failWithMessage(proto, " Phase not recognized (3): "
                        + currLeg + " followed by " + nextLeg);
            }
            proto.addToBranch(
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);

        } else if(nextLeg.equals("i") ) {
            return failWithMessage(proto, " Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", y,j must be upgoing in inner core and so cannot hit iomb from above.");
        } else if(nextLeg.equals("I") || nextLeg.equals("J") ) {
            return failWithMessage(proto, " Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", y,j must be upgoing in outer core and so cannot hit inner core.");
        } else if(nextLeg.equals("y") || nextLeg.equals("j")) {
            return failWithMessage(proto, " Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", y,j must be upgoing in inner core and so cannot repeat.");
        } else if(isLegDepth(currLeg)) {
            double nextLegDepth = Double.parseDouble(currLeg);
            if (nextLegDepth >= tMod.getCmbDepth()) {
                return failWithMessage(proto, " Phase not recognized (3): "
                        + currLeg + " followed by " + nextLeg+", y and j must be upgoing in inner core and so cannot hit depth "+nextLegDepth);
            }
            int disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
            if (disconBranch < tMod.getIocbBranch() ) {
                return failWithMessage(proto, " Phase not recognized (2): "
                        + currLeg + " followed by " + nextLeg
                        + ", discon not in inner core.");
            }
            endAction = TRANSUP;
            proto.addToBranch(
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else {
            return failWithMessage(proto, " Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg);
        }
        return proto;
    }

    ProtoSeismicPhase failWithMessage(ProtoSeismicPhase proto, String reason) {
        if (DEBUG) {
            System.err.println("FAIL: "+name+" "+reason);
        }
        proto.failNext(reason);
        return proto;
    }

    /**
     * Calculates how many times the phase passes through a branch, up or down,
     * so that we can just multiply instead of doing the ray calc for each time.
     * @return
     */
    protected static int[][] calcBranchMultiplier(TauModel tMod, List<SeismicPhaseSegment> segmentList) {
        /* initialize the counter for each branch to 0. 0 is P and 1 is S. */
        int[][] timesBranches = new int[2][tMod.getNumBranches()];
        for(int i = 0; i < timesBranches[0].length; i++) {
            timesBranches[0][i] = 0;
            timesBranches[1][i] = 0;
        }
        /* Count how many times each branch appears in the path. */
        for (SeismicPhaseSegment seg : segmentList) {
            if (seg.isFlat) {
                // head/diff waves will be inserted after regular paths are calculated
                continue;
            }
            int begin = Math.min(seg.startBranch, seg.endBranch);
            int finish = Math.max(seg.startBranch, seg.endBranch);
            int isPIdx = seg.isPWave ? 0 : 1;
            for (int i = begin; i <= finish; i++) {
                timesBranches[isPIdx][i]++;
            }
        }
        return timesBranches;
    }

    /**
     * Sums the appropriate branches for this phase.
     *
     * @throws TauModelException
     *             if the topDepth of the high slowness zone is not contained
     *             within the TauModel. This should never happen and would
     *             indicate an invalid TauModel.
     */
    protected static SimpleSeismicPhase sumBranches(TauModel tMod, ProtoSeismicPhase proto) throws TauModelException {
        SeismicPhaseSegment endSeg = proto.endSegment();
        double minRayParam = endSeg.minRayParam;
        double maxRayParam = endSeg.maxRayParam;
        if (endSeg.endAction == FAIL) {
            throw new RuntimeException("Cannot sum failed phase");
        }
        double[] rayParams;
        double[] dist;
        double[] time;
        double minDistance;
        double maxDistance;
        int minRayParamIndex = 0;
        int maxRayParamIndex = tMod.rayParams.length;
        String name = proto.getName();
        if(endSeg.maxRayParam < 0.0 || endSeg.minRayParam > endSeg.maxRayParam) {
            /* Phase has no arrivals, possibly due to source depth. */
            proto.failNext("Phase has no arrivals, possibly due to source depth");
            return new FailedSeismicPhase(proto);
        }
        /* Special case for surface waves. */
        if(name.endsWith("kmps")) {
            try {
            dist = new double[2];
            time = new double[2];
            rayParams = new double[2];
            dist[0] = 0.0;
            time[0] = 0.0;
            double velocity = Double.valueOf(name.substring(0, name.length() - 4))
                    .doubleValue();
            rayParams[0] = tMod.radiusOfEarth / velocity;
            dist[1] = getMaxKmpsLaps() * 2 * Math.PI;
            time[1] = getMaxKmpsLaps() * 2 * Math.PI * tMod.radiusOfEarth / velocity;
            rayParams[1] = rayParams[0];
            minDistance = dist[0];
            maxDistance = dist[1];
            minRayParam = rayParams[0];
            maxRayParam = rayParams[0];
            maxRayParamIndex = 1;
            return new SimpleSeismicPhase(proto, rayParams, time, dist,
                    minRayParam, maxRayParam, minRayParamIndex, maxRayParamIndex, minDistance, maxDistance, TauPConfig.DEBUG);
            } catch (NumberFormatException e) {
                proto.failNext(" Illegal surface wave velocity "+name.substring(0, name.length() - 4));
                return new FailedSeismicPhase(proto);
            }
        }
        /*
         * Find the ray parameter index that corresponds to the minRayParam and
         * maxRayParam.
         */
        for(int i = 0; i < tMod.rayParams.length; i++) {
            if(tMod.rayParams[i] >= minRayParam) {
                minRayParamIndex = i;
            }
            if(tMod.rayParams[i] >= maxRayParam) {
                maxRayParamIndex = i;
            }
        }
        if(maxRayParamIndex < 0) {
            throw new RuntimeException(proto.getName()+" Should not happen, did not find max ray param"+maxRayParam);
        }

        if(minRayParamIndex < 0) {
            throw new RuntimeException(proto.getName()+" Should not happen, did not find min ray param"+minRayParam);
        }

        if(maxRayParamIndex == 0
                && minRayParamIndex == tMod.rayParams.length - 1) {
            // all ray parameters are valid so just copy
            rayParams = new double[tMod.rayParams.length];
            System.arraycopy(tMod.rayParams,
                    0,
                    rayParams,
                    0,
                    tMod.rayParams.length);
        } else if(maxRayParamIndex == minRayParamIndex) {
            // only single valid ray param, like head or diffracted
            rayParams = new double[2];
            rayParams[0] = minRayParam;
            rayParams[1] = minRayParam;
        } else {
            if(TauPConfig.DEBUG) {
                System.err.println("SumBranches() maxRayParamIndex=" + maxRayParamIndex
                        + " minRayParamIndex=" + minRayParamIndex
                        + " tMod.rayParams.length=" + tMod.rayParams.length
                        + " tMod.rayParams[0]=" + tMod.rayParams[0]
                        +"\n"
                        + " tMod.rayParams["+minRayParamIndex+"]=" + tMod.rayParams[minRayParamIndex]
                        +"\n"
                        + " tMod.rayParams["+maxRayParamIndex+"]=" + tMod.rayParams[maxRayParamIndex]
                        + " maxRayParam=" + maxRayParam);
            }
            // only a subset of ray parameters are valid so only use those
            rayParams = new double[minRayParamIndex - maxRayParamIndex + 1];
            System.arraycopy(tMod.rayParams,
                    maxRayParamIndex,
                    rayParams,
                    0,
                    minRayParamIndex - maxRayParamIndex + 1);
        }
        dist = new double[rayParams.length];
        time = new double[rayParams.length];
        /* counter for passes through each branch. 0 is P and 1 is S. */
        int[][] timesBranches = calcBranchMultiplier(tMod, proto.segmentList);
        /* Sum the branches with the appropriate multiplier. */
        for(int j = 0; j < tMod.getNumBranches(); j++) {
            if(timesBranches[0][j] != 0) {
                for(int i = maxRayParamIndex; i < minRayParamIndex + 1; i++) {
                    dist[i - maxRayParamIndex] += timesBranches[0][j]
                            * tMod.getTauBranch(j, PWAVE).getDist(i);
                    time[i - maxRayParamIndex] += timesBranches[0][j]
                            * tMod.getTauBranch(j, PWAVE).time[i];
                }
            }
            if(timesBranches[1][j] != 0) {
                for(int i = maxRayParamIndex; i < minRayParamIndex + 1; i++) {
                    dist[i - maxRayParamIndex] += timesBranches[1][j]
                            * tMod.getTauBranch(j, SWAVE).getDist(i);
                    time[i - maxRayParamIndex] += timesBranches[1][j]
                            * tMod.getTauBranch(j, SWAVE).time[i];
                }
            }
        }
        int numHead = proto.countHeadLegs();
        int numDiff = proto.countDiffLegs();
        if (numDiff>0 || numHead>0) {
            // proportionally share head/diff, although this probably can't actually happen in a single ray
            // and will usually be either refraction or diffraction
            double horizontalDistDeg = numHead/(numHead+numDiff) * getMaxRefraction() + numDiff/(numHead+numDiff)*getMaxDiffraction();
            dist[1] = dist[0] + horizontalDistDeg * Math.PI / 180.0;
            time[1] = time[0] + horizontalDistDeg * Math.PI / 180.0 * minRayParam;
        } else if(maxRayParamIndex == minRayParamIndex) {
            // one ray param but no head/diff phases ???
            dist[1] = dist[0];
            time[1] = time[0];
        }
        minDistance = Double.MAX_VALUE;
        maxDistance = 0.0;
        for(int j = 0; j < dist.length; j++) {
            if(dist[j] < minDistance) {
                minDistance = dist[j];
            }
            if(dist[j] > maxDistance) {
                maxDistance = dist[j];
            }
        }
        /*
         * Now check to see if our ray parameter range includes any ray
         * parameters that are associated with high slowness zones. If so, then
         * we will need to insert a "shadow zone" into our time and distance
         * arrays. It is represented by a repeated ray parameter.
         */
        DepthRange[] hsz;
        int hSZIndex;
        int indexOffset;
        boolean foundOverlap = false;
        boolean isPWave;
        int branchNum;
        int dummy;
        for(dummy = 0, isPWave = true; dummy < 2; dummy++, isPWave = false) {
            hsz = tMod.getSlownessModel().getHighSlowness(isPWave);
            hSZIndex = 0;
            indexOffset = 0;
            for(int i = 0; i < hsz.length; i++) {
                if(maxRayParam > hsz[i].rayParam
                        && hsz[i].rayParam > minRayParam) {
                    /*
                     * There is a high slowness zone within our ray parameter
                     * range so we might need to add a shadow zone. We need to
                     * check to see if this wave type, P or S, is part of the
                     * phase at this depth/ray parameter.
                     */
                    branchNum = tMod.findBranch(hsz[i].topDepth);
                    foundOverlap = false;
                    SeismicPhaseSegment prev = null;
                    for (SeismicPhaseSegment seg : proto.segmentList) {
                        // check for downgoing legs that cross the high slowness
                        // zone with the same wave type
                        if (seg.isDownGoing
                                && seg.isPWave == isPWave
                                && (seg.startBranch < branchNum && seg.endBranch >= branchNum)
                                || (prev != null && seg.startBranch == branchNum && prev.endBranch == branchNum-1
                                && prev.isDownGoing
                                && prev.isPWave == isPWave)) {
                            foundOverlap = true;
                            break;
                        }
                        prev = seg;
                    }
                    if(foundOverlap) {
                        double[] newdist = new double[dist.length + 1];
                        double[] newtime = new double[time.length + 1];
                        double[] newrayParams = new double[rayParams.length + 1];
                        for(int j = 0; j < rayParams.length; j++) {
                            if(rayParams[j] == hsz[i].rayParam) {
                                hSZIndex = j;
                                break;
                            }
                        }
                        System.arraycopy(dist, 0, newdist, 0, hSZIndex);
                        System.arraycopy(time, 0, newtime, 0, hSZIndex);
                        System.arraycopy(rayParams,
                                0,
                                newrayParams,
                                0,
                                hSZIndex);
                        newrayParams[hSZIndex] = hsz[i].rayParam;
                        /* Sum the branches with the appropriate multiplier. */
                        newdist[hSZIndex] = 0.0;
                        newtime[hSZIndex] = 0.0;
                        for(int j = 0; j < tMod.getNumBranches(); j++) {
                            if(timesBranches[0][j] != 0
                                    && tMod.getTauBranch(j, PWAVE)
                                    .getTopDepth() < hsz[i].topDepth) {
                                newdist[hSZIndex] += timesBranches[0][j]
                                        * tMod.getTauBranch(j, PWAVE).dist[maxRayParamIndex
                                        + hSZIndex - indexOffset];
                                newtime[hSZIndex] += timesBranches[0][j]
                                        * tMod.getTauBranch(j, PWAVE).time[maxRayParamIndex
                                        + hSZIndex - indexOffset];
                            }
                            if(timesBranches[1][j] != 0
                                    && tMod.getTauBranch(j, SWAVE)
                                    .getTopDepth() < hsz[i].topDepth) {
                                newdist[hSZIndex] += timesBranches[1][j]
                                        * tMod.getTauBranch(j, SWAVE).dist[maxRayParamIndex
                                        + hSZIndex - indexOffset];
                                newtime[hSZIndex] += timesBranches[1][j]
                                        * tMod.getTauBranch(j, SWAVE).time[maxRayParamIndex
                                        + hSZIndex - indexOffset];
                            }
                        }
                        System.arraycopy(dist,
                                hSZIndex,
                                newdist,
                                hSZIndex + 1,
                                dist.length - hSZIndex);
                        System.arraycopy(time,
                                hSZIndex,
                                newtime,
                                hSZIndex + 1,
                                time.length - hSZIndex);
                        System.arraycopy(rayParams,
                                hSZIndex,
                                newrayParams,
                                hSZIndex + 1,
                                rayParams.length - hSZIndex);
                        indexOffset++;
                        dist = newdist;
                        time = newtime;
                        rayParams = newrayParams;
                    }
                }
            }
        }

        return new SimpleSeismicPhase(proto,
                rayParams,
                time,
                dist,
                minRayParam,
                maxRayParam,
                minRayParamIndex,
                maxRayParamIndex,
                minDistance,
                maxDistance,
                TauPConfig.DEBUG);
    }

    /**
     * find out if the next leg represents a phase conversion depth
     * @param leg
     * @return
     */
    public boolean isLegDepth(String leg) {
        boolean isNextLegDepth;
        try {
            double nextLegDepth = Double.parseDouble(leg);
            isNextLegDepth = true;
        } catch(NumberFormatException e) {
            isNextLegDepth = false;
        }
        return isNextLegDepth;
    }

    public int calcStartBranch(ProtoSeismicPhase proto, String currLeg) {
        int currBranch;
        if (! proto.isEmpty()) {
            currBranch = proto.endSegment().endBranch + PhaseInteraction.endOffset(proto.endSegment().endAction);
        } else if(isDowngoingSymbol(currLeg)) {
            // initial downgoing leg, like P
            currBranch = tMod.getSourceBranch();
        } else {
            // initial upgoing leg, like p
            currBranch = tMod.getSourceBranch()-1;
        }
        return currBranch;
    }

    public static String endActionString(PhaseInteraction endAction) {
        if(endAction == START) {
            return "START";
        } else if(endAction == TURN) {
            return "TURN";
        } else if(endAction == DIFFRACTTURN) {
            return "DIFFRACT_TURN";
        } else if(endAction == REFLECT_UNDERSIDE) {
            return "REFLECT_UNDERSIDE";
        } else if(endAction == REFLECT_UNDERSIDE_CRITICAL) {
            return "REFLECT_UNDERSIDE_CRITICAL";
        } else if(endAction == SCATTER ) {
            return "SCATTER";
        } else if(endAction == SCATTER_DOWN) {
            return "SCATTER_DOWN";
        } else if(endAction == BACKSCATTER ) {
            return "BACKSCATTER";
        } else if(endAction == BACKSCATTER_DOWN) {
            return "BACKSCATTER_DOWN";
        } else if(endAction == END ) {
            return END_CODE;
        } else if(endAction == END_DOWN) {
            return "END_DOWN";
        } else if(endAction == REFLECT_TOPSIDE) {
            return "REFLECT_TOPSIDE";
        } else if(endAction == REFLECT_TOPSIDE_CRITICAL) {
            return "REFLECT_TOPSIDE_CRITICAL";
        } else if(endAction == TRANSUP) {
            return "TRANSUP";
        } else if(endAction == TRANSDOWN) {
            return "TRANSDOWN";
        } else if(endAction == DIFFRACT) {
            return "DIFFRACT";
        } else if(endAction == TRANSUPDIFFRACT) {
            return "TRANS UP DIFFRACT";
        } else if(endAction == HEAD) {
            return "HEAD WAVE";
        } else if(endAction == FAIL) {
            return "FAIL";
        } else {
            throw new RuntimeException("UNKNOWN Action: "+endAction);
        }
    }


}
