package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.sc.seis.TauP.PhaseInteraction.*;
import static edu.sc.seis.TauP.PhaseInteraction.REFLECT_TOPSIDE_CRITICAL;
import static edu.sc.seis.TauP.PhaseSymbols.*;
import static edu.sc.seis.TauP.ProtoSeismicPhase.failNewPhase;

/**
 * Factory for calculating seismic phase from a phase name within a model.
 */
public class SeismicPhaseFactory {

    boolean DEBUG;
    String name;
    double receiverDepth;
    TauModel tMod;

    // temp vars used in calculation of phase
    int upgoingRecBranch;
    int downgoingRecBranch;
    PhaseInteraction prevEndAction = START;

    public static final int CRUST_MANTLE_FACTORY = 0;
    public static final int OUTER_CORE_FACTOR = 1;
    public static final int INNER_CORE_FACTORY = 2;

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

        Pattern scatPattern = Pattern.compile(LegPuller.scatterWave);
        Matcher m = scatPattern.matcher(name);
        if (m.matches()) {
            String inbound = m.group("inscat");
            String outbound = m.group("outscat");
            if (inbound.isEmpty() || name.charAt(0) == SCATTER_CODE || name.charAt(0) == BACKSCATTER_CODE) {
                // this probably can't pass the RE, but...
                FailedSeismicPhase fail = FailedSeismicPhase.failForReason(name, tMod, receiverDepth,
                        "Scatter phase cannot start with symbols, oO, in "+name+", must have phase from source to scatterer.");
                phaseList.add(fail);
                return phaseList;
            }
            if (outbound.charAt(0) == SCATTER_CODE || outbound.charAt(0) == BACKSCATTER_CODE) {
                // this probably can't pass the RE, but...
                FailedSeismicPhase fail = FailedSeismicPhase.failForReason(name, tMod, receiverDepth,
                        "Scatter phase cannot have repeat symbols, oO, in "+name+", must have only one scatterer.");
                phaseList.add(fail);
                return phaseList;
            }
            if (outbound.isEmpty()) {
                // this probably can't pass the RE, but...
                FailedSeismicPhase fail = FailedSeismicPhase.failForReason(name, tMod, receiverDepth,
                        "Scatter phase cannot end with symbols, oO, in "+name+", must have phase from scatterer to receiver.");
                phaseList.add(fail);
                return phaseList;
            }
            // check second scatterer in outbound
            Matcher outM = scatPattern.matcher(outbound);
            if (outM.matches()) {
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
            String prescatterPhaseName = inbound;
            String postscatterPhaseName = outbound;
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
                return SeismicPhase.PWAVE;
        } else if(PhaseSymbols.isTransverseWaveSymbol(currLeg, 0)) {
            return SeismicPhase.SWAVE;
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
            isPWave = SeismicPhase.PWAVE;
            prevIsPWave = isPWave;
        } else if(isTransverseWaveSymbol(currLeg)) {
            isPWave = SeismicPhase.SWAVE;
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
                String reason = " Upgoing initial leg but already at surface, so no ray parameters satisfy path."+tMod.getSourceBranch();
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
                Alert.debug("Iterate legs: "+legNum + "  " + prevLeg + "  cur=" + currLeg
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

            // crust/mantle, outer core, inner core
            List<SeismicPhaseLayerFactory> layerFactories = SeismicPhaseLayerFactory.createFactory(this);
            String nextNextLeg = legNum < legs.size()-2 ? legs.get(legNum+2) : END_CODE;

            if (isCrustMantleLeg(currLeg)) {
                proto = layerFactories.get(CRUST_MANTLE_FACTORY).parse(proto, prevLeg, currLeg, nextLeg, nextNextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
            } else if (isOuterCoreLeg(currLeg)) {
                proto = layerFactories.get(OUTER_CORE_FACTOR).parse(proto, prevLeg, currLeg, nextLeg, nextNextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
            } else if (isInnerCoreLeg(currLeg)) {
                proto = layerFactories.get(INNER_CORE_FACTORY).parse(proto, prevLeg, currLeg, nextLeg, nextNextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
            } else if (isLegDepth(currLeg) || isReflectSymbol(currLeg) || currLeg.equals(""+ m)) {
                // depth interaction leg, no action
            } else {
                return failWithMessage(proto,"Unknown leg: "+currLeg);
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
                Alert.debug("Last action is: "+proto.endSegment());
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

    ProtoSeismicPhase failWithMessage(ProtoSeismicPhase proto, String reason) {
        if (DEBUG) {
            Alert.debug("FAIL: "+name+" "+reason);
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
    protected static SimpleSeismicPhase sumBranches(TauModel tModA, ProtoSeismicPhase proto) throws TauModelException {
        SeismicPhaseSegment endSeg = proto.endSegment();
        TauModel tMod = proto.gettMod();
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
        int maxRayParamIndex;
        String name = proto.getName();
        if (endSeg.maxRayParam < 0.0 || endSeg.minRayParam > endSeg.maxRayParam) {
            /* Phase has no arrivals, possibly due to source depth. */
            proto.failNext("Phase has no arrivals, possibly due to source depth");
            return new FailedSeismicPhase(proto);
        }
        if (endSeg.maxRayParam == endSeg.minRayParam && proto.countFlatLegs()==0) {
            proto.failNext("Phase has singleton ray parameter, but no flat legs (head or diff).");
            return new FailedSeismicPhase(proto);
        }
        /* Special case for surface waves. */
        if (name.endsWith(KMPS_CODE)) {
            double velocity;
            try {
                velocity = Double.valueOf(name.substring(0, name.length() - 4));
            } catch (NumberFormatException e) {
                proto.failNext(" Illegal surface wave velocity " + name.substring(0, name.length() - 4));
                return new FailedSeismicPhase(proto);
            }
            dist = new double[2];
            time = new double[2];
            rayParams = new double[2];
            dist[0] = 0.0;
            time[0] = 0.0;
            rayParams[0] = tMod.radiusOfEarth / velocity;
            dist[1] = getMaxKmpsLaps() * 2 * Math.PI;
            time[1] = getMaxKmpsLaps() * 2 * Math.PI * tMod.radiusOfEarth / velocity;
            rayParams[1] = rayParams[0];
            minDistance = dist[0];
            maxDistance = dist[1];
            minRayParam = rayParams[0];
            maxRayParam = rayParams[0];
            maxRayParamIndex = 1;
            SimpleSeismicPhase sp = new SimpleContigSeismicPhase(proto, rayParams, time, dist,
                    minRayParam, maxRayParam, minRayParamIndex, maxRayParamIndex, minDistance, maxDistance, TauPConfig.DEBUG);
            return sp;
        }
        // split for any high slowness zones
        List<ProtoSeismicPhase> hszSplitProtoList = proto.splitForAllHighSlowness();
        List<SimpleContigSeismicPhase> contigPhaseList = new ArrayList<>();
        for (ProtoSeismicPhase hszProto : hszSplitProtoList) {
            contigPhaseList.add(internalSumContigPhase(hszProto));
        }
        if (contigPhaseList.size()==1) {
            return contigPhaseList.get(0);
        }
        return new CompositeSeismicPhase(contigPhaseList);
    }

    protected static SimpleContigSeismicPhase internalSumContigPhase(ProtoSeismicPhase proto) throws TauModelException {
        TauModel tMod = proto.tMod;
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

            if (proto.countFlatLegs() == 0) {
                throw new TauModelException("min and max rp index are same, only one ray but not flat? "+
                        proto.getName()+" "+minRayParamIndex+" "+maxRayParamIndex
                        +"  rp "+minRayParam+" "+maxRayParam+" "+proto.branchNumSeqStr());
            }
        } else {
            if(TauPConfig.DEBUG) {
                Alert.debug("SumBranches() maxRayParamIndex=" + maxRayParamIndex
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
        for(int i = 0; i < rayParams.length; i++) {
            TimeDist td = calcForIndex(proto, i, maxRayParamIndex, rayParams);
            dist[i] = td.getDistRadian();
            time[i] = td.getTime();
        }

        int numHead = proto.countHeadLegs();
        int numDiff = proto.countDiffLegs();
        if (numDiff>0 || numHead>0) {
            // proportionally share head/diff, although this probably can't actually happen in a single ray
            // and will usually be either refraction or diffraction
            double horizontalDistDeg = 1.0*numHead/(numHead+numDiff) * getMaxRefraction() + 1.0*numDiff/(numHead+numDiff)*getMaxDiffraction();
            dist[1] = dist[0] + horizontalDistDeg * Math.PI / 180.0;
            time[1] = time[0] + horizontalDistDeg * Math.PI / 180.0 * minRayParam;
        } else if(rayParams.length == 2 && maxRayParamIndex == minRayParamIndex) {
            if (!proto.sourceSegment().isDownGoing && tMod.getSourceDepth() == tMod.getRadiusOfEarth()) {
                // special case for source at center of earth, weird but sometimes useful for testing
                dist[0] = 0;
                dist[1] = 2*Math.PI;
                time[1] = time[0];
            } else {
                // one ray param but no head/diff phases, weird degenerate case of single ray working ???
                dist[1] = dist[0];
                time[1] = time[0];
            }
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

        return new SimpleContigSeismicPhase(proto,
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

    public static TimeDist calcForIndex(ProtoSeismicPhase proto, int idx, int maxRayParamIndex, double[] rayParams){
        double rp = rayParams[idx];
        double dist = 0;
        double time = 0;
        for (SeismicPhaseSegment seg : proto.segmentList) {
            if (seg.isFlat) {continue;}
            int add = seg.isDownGoing ? 1 : -1;
            for (int b = seg.startBranch; (seg.isDownGoing && b <= seg.endBranch) || (!seg.isDownGoing && b >= seg.endBranch); b+=add) {
                TauBranch tauBranch = proto.tMod.getTauBranch(b, seg.isPWave);
                if (rp <= tauBranch.getMaxRayParam()) {
                    dist += tauBranch.getDist(idx+maxRayParamIndex);
                    time += tauBranch.getTime(idx+maxRayParamIndex);
                }
            }
        }
        return new TimeDist(rp, time, dist);
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
