package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static edu.sc.seis.TauP.PhaseInteraction.*;
import static edu.sc.seis.TauP.PhaseInteraction.REFLECT_TOPSIDE_CRITICAL;

public class SeismicPhaseFactory {

    boolean DEBUG;
    String name;
    double sourceDepth;
    double receiverDepth;
    TauModel tMod;
    ArrayList<String> legs;
    String puristName;

    // temp vars used in calculation of phase
    int upgoingRecBranch;
    int downgoingRecBranch;
    double nextLegDepth = 0.0;
    boolean isLegDepth, isNextLegDepth = false;
    PhaseInteraction prevEndAction = START;
    double[] dist;
    double[] time;
    double[] rayParams;

    /** Minimum ray parameter that exists for this phase. */
    protected double minRayParam;

    /** Maximum ray parameter that exists for this phase. */
    protected double maxRayParam;

    /**
     * Index within TauModel.rayParams that corresponds to maxRayParam. Note
     * that maxRayParamIndex &lt; minRayParamIndex as ray parameter decreases with
     * increasing index.
     */
    protected int maxRayParamIndex = -1;

    /**
     * Index within TauModel.rayParams that corresponds to minRayParam. Note
     * that maxRayParamIndex &lt; minRayParamIndex as ray parameter decreases with
     * increasing index.
     */
    protected int minRayParamIndex = -1;

    /** The minimum distance that this phase can be theoretically observed. */
    protected double minDistance = 0.0;

    /** The maximum distance that this phase can be theoretically observed. */
    protected double maxDistance = Double.MAX_VALUE;

    /**
     * temporary branch number so we know where to start add to the branch
     * sequence. Used in addToBranch() and parseName().
     */
    protected transient int currBranch;

    /**
     * Array of branch numbers for the given phase. Note that this depends upon
     * both the earth model and the source depth.
     */
    protected List<Integer> branchSeq = new ArrayList<Integer>();

    /**
     * Array of branchSeq positions where a head or diffracted segment occurs.
     */
    protected List<Integer> headOrDiffractSeq = new ArrayList<Integer>();

    /** Description of segments of the phase. */
    protected List<SeismicPhaseSegment> segmentList = new ArrayList<SeismicPhaseSegment>();

    /**
     * records the end action for the current leg. Will be one of
     * SeismicPhase.TURN, SeismicPhase.TRANSDOWN, SeismicPhase.TRANSUP,
     * SeismicPhase.REFLECTBOT, or SeismicPhase.REFLECTTOP. This allows a check
     * to make sure the path is correct. Used in addToBranch() and parseName().
     */
    protected ArrayList<PhaseInteraction> legAction = new ArrayList<PhaseInteraction>();

    /**
     * true if the current leg of the phase is down going. This allows a check
     * to make sure the path is correct. Used in addToBranch() and parseName().
     */
    protected ArrayList<Boolean> downGoing = new ArrayList<Boolean>();

    /**
     * ArrayList of wave types corresponding to each leg of the phase.
     *
     */
    protected ArrayList<Boolean> waveType = new ArrayList<Boolean>();

    public static final boolean PWAVE = SimpleSeismicPhase.PWAVE;

    public static final boolean SWAVE = SimpleSeismicPhase.SWAVE;

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
        if (name == null || name.length() == 0) {
            throw new TauModelException("Phase name cannot be empty to null: " + name);
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
        this.sourceDepth = sourceDepth;
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
        return createPhase(name, tMod, sourceDepth, receiverDepth, ToolRun.DEBUG);
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
                                                         double scattererDepth,
                                                         double scattererDistanceDeg,
                                                         boolean debug) throws TauModelException {
        List<SeismicPhase> phaseList = new ArrayList<>();
        if (name.contains(""+LegPuller.SCATTER_CODE)
                || name.contains(""+LegPuller.BACKSCATTER_CODE)) {
            String[] in_scat = name.split("("+LegPuller.SCATTER_CODE+"|"+LegPuller.BACKSCATTER_CODE+")");
            if (in_scat.length != 2) {
                throw new TauModelException("Scatter phase doesn't have two segments: "+name);
            }
            if (scattererDistanceDeg == 0.0) {
                throw new ScatterArrivalFailException("Attempt to use scatter phase but scatter distance is zero: "+name);
            }
            boolean isBackscatter = false;
            if( name.contains(""+LegPuller.BACKSCATTER_CODE)) {
                isBackscatter = true;
            }
            TauModel tModDepthCorrected = tMod.depthCorrect(sourceDepth);
            tModDepthCorrected = tModDepthCorrected.splitBranch(receiverDepth);
            SeismicPhase inPhase = SeismicPhaseFactory.createPhase(in_scat[0],
                    tModDepthCorrected, sourceDepth, scattererDepth, debug);
            SeismicPhaseSegment lastSegment = inPhase.getPhaseSegments().get(inPhase.getPhaseSegments().size()-1);
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
            TauModel scatTMod = tModDepthCorrected.depthRecorrect(scattererDepth);
            SimpleSeismicPhase scatPhase = SeismicPhaseFactory.createPhase(in_scat[1],
                    scatTMod, scattererDepth, receiverDepth, debug);

            List<Arrival> inArrivals = inPhase.calcTime(scattererDistanceDeg);
            if (inArrivals.size() == 0) {
                throw new ScatterArrivalFailException("No inbound arrivals to the scatterer for "+name
                        +" at "+scattererDepth+" km depth and "+scattererDistanceDeg+" deg. Distance range for scatterer at this depth is "+inPhase.getMinDistanceDeg()+" "+inPhase.getMaxDistanceDeg()+" deg.");
            }
            for (Arrival inArr : inArrivals) {
                ScatteredSeismicPhase seismicPhase = new ScatteredSeismicPhase(
                        inArr,
                        scatPhase,
                        scattererDepth,
                        scattererDistanceDeg,
                        isBackscatter);
                phaseList.add(seismicPhase);
            }
        } else {
            TauModel tModDepthCorrected = tMod.depthCorrect(sourceDepth);
            tModDepthCorrected = tModDepthCorrected.splitBranch(receiverDepth);
            SimpleSeismicPhase seismicPhase = SeismicPhaseFactory.createPhase(name,
                    tModDepthCorrected, sourceDepth, receiverDepth, debug);
            phaseList.add(seismicPhase);
        }
        return phaseList;
    }

    SimpleSeismicPhase internalCreatePhase() throws TauModelException {
        legs = LegPuller.legPuller(name);
        this.puristName = LegPuller.createPuristName(tMod, legs);

        parseName(tMod);
        sumBranches(tMod);
        SimpleSeismicPhase phase = new SimpleSeismicPhase(name,
                tMod,
                receiverDepth,
                legs,
                puristName,
                rayParams,
                time,
                dist,
                minRayParam,
                maxRayParam,
                minRayParamIndex,
                maxRayParamIndex,
                minDistance,
                maxDistance,
                branchSeq,
                headOrDiffractSeq,
                segmentList,
                legAction,
                downGoing,
                waveType,
                DEBUG);
        return phase;
    }

    public String getName() {
        return name;
    }


    public Boolean legIsPWave(String currLeg) {
            if(currLeg.equals("p") || currLeg.startsWith("P")
                    || currLeg.startsWith("I") || currLeg.equals("y")
                    ||  currLeg.startsWith("K") || currLeg.equals("k")) {
                return PWAVE;
            } else if(currLeg.equals("s") || currLeg.startsWith("S")
                    || currLeg.equals("J")|| currLeg.equals("j")) {
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
    public boolean[] legsArePWave() {
        boolean[] isPWaveForLegs = new boolean[legs.size()];
        int legNum = 0;
        boolean prevWaveType = true;
        for (String currLeg : legs) {
            Boolean currWaveType;
            if (currLeg.equals("END")) {
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
                                throw new RuntimeException("next wavetype is null: "+currLeg+" in " + name + " " + legNum + " of " + legs.size());
                            }
                        } else {
                            throw new RuntimeException("SHould never happen: "+currLeg+" of "+name+" "+legNum);
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
    protected void parseName(TauModel tMod) throws TauModelException {
        String prevLeg;
        String currLeg = (String)legs.get(0);
        String nextLeg = currLeg;
        branchSeq.clear();
        boolean isPWave = PWAVE;
        boolean prevIsPWave = isPWave;
        PhaseInteraction endAction = TRANSDOWN;
        /*
         * Deal with surface wave velocities first, since they are a special
         * case.
         */
        if(legs.size() == 2 && currLeg.endsWith("kmps")) {
            try {
                double velocity = Double.valueOf(currLeg.substring(0, name.length() - 4))
                    .doubleValue();
            } catch (NumberFormatException e) {
                throw new TauModelException(getName()+" Illegal surface wave velocity "+name.substring(0, name.length() - 4), e);
            }
            // KMPS fake with a head wave
            SeismicPhaseSegment flatSegment = addFlatBranch(tMod, 0, false, KMPS, END, currLeg);
            return;
        }
        /* Make a check for J legs if the model doesn not allow J */
        if((name.indexOf('J') != -1 || name.indexOf('j') != -1)
                && !tMod.getSlownessModel().isAllowInnerCoreS()) {
            throw new TauModelException(getName()+" 'J' phases were not created for this model: "
                    + tMod.getModelName());
        }
        /* check for outer core if K */
        for (String leg : legs) {
            if (tMod.getCmbBranch() == tMod.getNumBranches() && (leg.startsWith("K") || leg.equals("k") || leg.equals("c"))) {
                maxRayParam = -1;
                minRayParam = -1;
                if(DEBUG) {
                    System.out.println("Cannot have K leg in model with no outer core within phase " + name);
                }
                return;
            }
            if (tMod.getIocbBranch() == tMod.getNumBranches() && (
                    leg.startsWith("I") || leg.startsWith("J") || leg.equals("j") || leg.equals("y") || leg.equals("i"))) {
                maxRayParam = -1;
                minRayParam = -1;
                if(DEBUG) {
                    System.out.println("Cannot have I,J,y,j,i leg in model with no inner core within phase " + name);
                }
                return;
            }
        }
        /* set currWave to be the wave type for this leg, 'P' or 'S'. */
        if(currLeg.equals("p") || currLeg.startsWith("P")
                || currLeg.startsWith("K")
                || currLeg.equals("k")
                || currLeg.startsWith("I") || currLeg.equals("y")) {
            isPWave = PWAVE;
            prevIsPWave = isPWave;
        } else if(currLeg.equals("s") || currLeg.startsWith("S")
                || currLeg.startsWith("J") || currLeg.equals("j")) {
            isPWave = SWAVE;
            prevIsPWave = isPWave;
        } else {
            throw new TauModelException(getName()+" Unknown starting phase: "+currLeg);
        }
        /*
         * First, decide whether the ray is up going or downgoing from the
         * source. If it is up going then the first branch number would be
         * tMod.getSourceBranch()-1 and downgoing would be
         * tMod.getSourceBranch().
         */
        if(currLeg.startsWith("s") || currLeg.startsWith("S")) {
            // Exclude S sources in fluids
            double sdep = tMod.getSourceDepth();
            if(tMod.getSlownessModel().depthInFluid(sdep, new DepthRange())) {
                maxRayParam = -1;
                minRayParam = -1;
                if(DEBUG) {
                    System.out.println("Cannot have S wave with starting depth in fluid layer"
                            + currLeg + " within phase " + name);
                }
                return;
            }
        }
        /*
         * Set maxRayParam to be a horizontal ray leaving the source and set
         * minRayParam to be a vertical (p=0) ray.
         */
        if(currLeg.startsWith("P")
                || currLeg.startsWith("S")
                || currLeg.startsWith("K")
                || currLeg.startsWith("I")
                || currLeg.startsWith("J")) {
            // Downgoing from source
            if ((currLeg.startsWith("P") || currLeg.startsWith("S")) && tMod.getSourceDepth() > tMod.getCmbDepth()  ) {
                // not possible
                maxRayParam = -1;
                minRayParam = -1;
                if(DEBUG) {
                    System.out.println("Source must be in crust/mantle for "
                            + currLeg + " within phase " + name);
                }
                return;
            } else if ((currLeg.startsWith("K")) && (tMod.getSourceDepth() < tMod.getCmbDepth() || tMod.getSourceDepth() > tMod.getIocbDepth() )) {
                // not possible
                maxRayParam = -1;
                minRayParam = -1;
                if(DEBUG) {
                    System.out.println("Source must be in outer core for "
                            + currLeg + " within phase " + name);
                }
                return;
            } else if ((currLeg.startsWith("I") || currLeg.startsWith("J")) && (tMod.getSourceDepth() < tMod.getIocbDepth() )) {
                // not possible
                maxRayParam = -1;
                minRayParam = -1;
                if(DEBUG) {
                    System.out.println("Source must be in inner core for "
                            + currLeg + " within phase " + name);
                }
                return;
            }
            currBranch = tMod.getSourceBranch();
            endAction = REFLECT_UNDERSIDE; // treat initial downgoing as if it were a
            // underside reflection
            try {
                int sLayerNum = tMod.getSlownessModel().layerNumberBelow(tMod.getSourceDepth(), prevIsPWave);
                maxRayParam = tMod.getSlownessModel().getSlownessLayer(sLayerNum, prevIsPWave).getTopP();
            } catch(NoSuchLayerException e) {
                throw new RuntimeException("Should not happen", e);
            }
            maxRayParam = tMod.getTauBranch(tMod.getSourceBranch(),
                    isPWave).getMaxRayParam();
        } else if(currLeg.equals("p") || currLeg.equals("s")
                || currLeg.equals("y") || currLeg.equals("j")
                || currLeg.startsWith("k")) {
            // Up going from source
            if ((currLeg.startsWith("p") || currLeg.startsWith("s")) && tMod.getSourceDepth() > tMod.getCmbDepth()  ) {
                // not possible
                maxRayParam = -1;
                minRayParam = -1;
                if(DEBUG) {
                    System.out.println("Source must be in crust/mantle for "
                            + currLeg + " within phase " + name);
                }
                return;
            } else if ((currLeg.startsWith("k"))
                    && (tMod.getSourceDepth() < tMod.getCmbDepth()
                        || tMod.getSourceDepth() > tMod.getIocbDepth() )) {
                // not possible
                maxRayParam = -1;
                minRayParam = -1;
                if(DEBUG) {
                    System.out.println("Source must be in outer core for "
                            + currLeg + " within phase " + name);
                }
                return;
            } else if ((currLeg.startsWith("y") || currLeg.startsWith("j")) && (tMod.getSourceDepth() < tMod.getIocbDepth() )) {
                // not possible
                maxRayParam = -1;
                minRayParam = -1;
                if(DEBUG) {
                    System.out.println("Source must be in inner core for "
                            + currLeg + " within phase " + name);
                }
                return;
            }

            endAction = REFLECT_TOPSIDE; // treat initial upgoing as if it were a topside reflection
            try {
                int sLayerNum = tMod.getSlownessModel().layerNumberAbove(tMod.getSourceDepth(), prevIsPWave);
                maxRayParam = tMod.getSlownessModel().getSlownessLayer(sLayerNum, prevIsPWave).getBotP();
                // check if source is in high slowness zone
                DepthRange highSZoneDepth = new DepthRange();
                if (tMod.getSlownessModel().depthInHighSlowness(tMod.getSourceDepth(), maxRayParam, highSZoneDepth, prevIsPWave)) {
                    // need to reduce maxRayParam until it can propagate out of high slowness zone
                    maxRayParam = Math.min(maxRayParam, highSZoneDepth.rayParam);
                }
            } catch(NoSuchLayerException e) {
                throw new RuntimeException("Should not happen", e);
            }
            if(tMod.getSourceBranch() != 0) {
                currBranch = tMod.getSourceBranch() - 1;
            } else {
                /*
                 * p and s for zero source depth are only at zero distance and
                 * then can be called P or S.
                 */
                maxRayParam = -1;
                minRayParam = -1;
                if (DEBUG) {
                    System.out.println(getName()+" Upgoing initial leg but already at surface, so no ray parameters satisfy path.");
                }
                return;
            }
        } else {
            throw new TauModelException(getName()+" First phase not recognized: "
                    +currLeg
                    + " Must be one of P, Pg, Pn, Pdiff, p, Ped or the S equivalents in crust/mantle, "
                    + "or k, K, I, J, j for core sources.");
        }
        if (receiverDepth != 0) {
            if (legs.get(legs.size()-2).equals("Ped") || legs.get(legs.size()-2).equals("Sed")
                    || legs.get(legs.size()-2).equals("Ked")
                    || legs.get(legs.size()-2).equals("Ied")
                    || legs.get(legs.size()-2).equals("Jed")) {
                // downgoing at receiver
                maxRayParam = Math.min(tMod.getTauBranch(downgoingRecBranch,
                                        isPWave)
                                .getMinTurnRayParam(),
                        maxRayParam);
            } else {
                // upgoing at receiver
                maxRayParam = Math.min(tMod.getTauBranch(upgoingRecBranch,
                                        isPWave)
                                .getMaxRayParam(),
                        maxRayParam);
            }

        }
        minRayParam = 0.0;
        if (maxRayParam < 0) {
            minRayParam = maxRayParam;
        }
        /*
         * Figure out which legs are P and S
         */
        boolean[] isLegPWave = legsArePWave();
        /*
         * Now loop over all of the phase legs and construct the proper branch
         * sequence.
         */
        currLeg = "START"; // So the prevLeg isn't wrong on the first pass
        for(int legNum = 0; legNum < legs.size(); legNum++) {
            prevLeg = currLeg;
            currLeg = nextLeg;
            if (legNum < legs.size() - 1) {
                nextLeg = legs.get(legNum + 1);
            } else {
                nextLeg = "END";
            }
            if(DEBUG) {
                System.out.println("Iterate legs: "+legNum + "  " + prevLeg + "  cur=" + currLeg
                        + "  " + nextLeg);
            }
            if (currLeg.contentEquals("END")) {
                if (segmentList.size() > 0) {
                    segmentList.get(segmentList.size()-1).endAction = END;
                    continue;
                }
            }
            isLegDepth = isNextLegDepth;
            // find out if the next leg represents a phase conversion depth or head/diff wave at discon
            try {
                nextLegDepth = Double.parseDouble(nextLeg);
                isNextLegDepth = true;
            } catch(NumberFormatException e) {
                nextLegDepth = -1;
                isNextLegDepth = false;
            }
            /* set currWave to be the wave type for this leg, 'P' or 'S'. */
            prevIsPWave = isPWave;
            isPWave = isLegPWave[legNum];
            boolean nextIsPWave = isPWave;
            if (legNum < isLegPWave.length-1) {
                nextIsPWave = isLegPWave[legNum + 1];
            }

            if (currLeg.equals("Ped") || currLeg.equals("Sed")) {
                /* Deal with P and S exclusively downgoing case . */
                endAction = currLegIs_Ped_Sed(prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
            } else if(currLeg.equals("p") || currLeg.equals("s")) {
                /* Deal with p and s case . */
                endAction = currLegIs_p_s(prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
            } else if(currLeg.equals("P") || currLeg.equals("S")) {
                /* Now deal with P and S case. */
                endAction = currLegIs_P_S(prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
            } else if((currLeg.startsWith("P") || currLeg.startsWith("S")) && currLeg.endsWith("diff")) {
                endAction = currLegIs_Pdiff_Sdiff(prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
            } else if((currLeg.startsWith("P") || currLeg.startsWith("S")) && (currLeg.endsWith("n") || currLeg.endsWith("g"))) {
                endAction = currLegIs_Pn_Sn(prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
            } else if(currLeg.equals("K")) {
                /* Now deal with K. */
                if ( checkDegenerateOuterCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum) ) {
                    endAction = currLegIs_K(prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                } else {
                    endAction = FAIL;
                }
            } else if(currLeg.equals("Ked")) {
                /* Now deal with Ked. */
                if ( checkDegenerateOuterCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                    endAction = currLegIs_Ked(prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                } else {
                    endAction = FAIL;
                }
            } else if(currLeg.startsWith("K") && currLeg.endsWith("diff")) {
                /* Now deal with Kdiff. */
                if ( checkDegenerateOuterCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                    endAction = currLegIs_Kdiff(prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                } else {
                    endAction = FAIL;
                }
            } else if(currLeg.equals("k")) {
                /* Deal with k case . */
                if ( checkDegenerateOuterCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                    endAction = currLegIs_k(prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                } else {
                    endAction = FAIL;
                }
            } else if(currLeg.equals("I") || currLeg.equals("J")) {
                /* And now consider inner core, I and J. */
                if ( checkDegenerateInnerCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                    endAction = currLegIs_I_J(prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                } else {
                    endAction = FAIL;
                }
            } else if((currLeg.startsWith("I") || currLeg.startsWith("J")) && currLeg.endsWith("diff")) {
                /* Now deal with I5500diff. */
                if ( checkDegenerateInnerCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                    endAction = currLegIs_I_Jdiff(prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                } else {
                    endAction = FAIL;
                }

            } else if(currLeg.equals("Ied") || currLeg.equals("Jed")) {
                /* And now consider inner core, Ied and Jed. */
                if ( checkDegenerateInnerCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                    endAction = currLegIs_Ied_Jed(prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                } else {
                    endAction = FAIL;
                }
            } else if(currLeg.equals("y") || currLeg.equals("j")) {
                /* And now consider upgoing inner core, y and j. */
                if ( checkDegenerateInnerCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                    endAction = currLegIs_y_j(prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
                } else {
                    endAction = FAIL;
                }
            } else if(currLeg.equals("m")
                    || currLeg.equals("c") || currLeg.equals("cx")
                    || currLeg.equals("i") || currLeg.equals("ix")) {
                if (currLeg.equals("c") || currLeg.equals("cx")) {
                    if ( ! checkDegenerateOuterCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                        endAction = FAIL;
                    }
                }
                if (currLeg.equals("i") || currLeg.equals("ix")) {
                    if ( ! checkDegenerateInnerCore(prevLeg, currLeg, nextLeg, isPWave, prevIsPWave, legNum)) {
                        endAction = FAIL;
                    }
                }
                if (nextLeg.equals("END")) {
                    throw new TauModelException(getName()+" Phase not recognized (12): "
                            + currLeg + " as last leg, then " + nextLeg);
                }
            } else if(currLeg.startsWith("^")) {
                endAction = REFLECT_UNDERSIDE;
                if (nextLeg.equals("END")) {
                    throw new TauModelException(getName()+" Phase not recognized (12): "
                            + currLeg + " as last leg, then " + nextLeg);
                }
                // nothing to do as will have been handled by previous leg
            } else if(currLeg.startsWith("v") || currLeg.startsWith("V")) {
                if (nextLeg.equals("END")) {
                    throw new TauModelException(getName()+" Phase not recognized (12): "
                            + currLeg + " as last leg, then " + nextLeg);
                }
                String depthString;
                depthString = currLeg.substring(1);
                int b = LegPuller.closestBranchToDepth(tMod, depthString);
                if (b == 0) {
                    throw new TauModelException(getName()+" Phase not recognized: "+currLeg+" looks like a top side reflection at the free surface.");
                }
            } else if(isLegDepth) {
                // check for phase like P0s, but could also be P2s if first discon is deeper
                int b = LegPuller.closestBranchToDepth(tMod, currLeg);
                if (b == 0 && (nextLeg.equals("p") || nextLeg.equals("s"))) {
                    throw new TauModelException(getName() + " Phase not recognized: " + currLeg
                            + " followed by " + nextLeg + " looks like a upgoing wave from the free surface as closest discontinuity to " + currLeg + " is zero depth.");
                }
            } else if (currLeg.endsWith("n")) {
                // non-standard head wave
                currLegIs_OtherHead(prevLeg, currLeg, nextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
            } else {
                throw new TauModelException(getName()+" Phase not recognized (10): " + currLeg
                        + " followed by " + nextLeg);
            }
            if (endAction == FAIL || maxRayParam < 0) {
                // phase has no arrivals, so stop looping over legs
                break;
            } else {
                prevEndAction = endAction;
            }
        }
        if (endAction != FAIL && maxRayParam != -1) {
            if (branchSeq.size() > 0 &&
                    branchSeq.get(branchSeq.size()-1) != upgoingRecBranch &&
                    branchSeq.get(branchSeq.size()-1) != downgoingRecBranch) {
                throw new TauModelException(getName()+" Phase does not end at the receiver branch, last: "+branchSeq.get(branchSeq.size()-1)
                        +" down Rec: "+downgoingRecBranch+" up Rec: "+upgoingRecBranch);
            }
            if ((endAction == REFLECT_UNDERSIDE || endAction == REFLECT_UNDERSIDE) && downgoingRecBranch == branchSeq.get(branchSeq.size()-1) ) {
                // last action was upgoing, so last branch should be upgoingRecBranch
                if (DEBUG) {
                    System.out.println("Phase ends upgoing, but receiver is not on upgoing end of last branch");
                }
                minRayParam = -1;
                maxRayParam = -1;
            } else if ((endAction == REFLECT_TOPSIDE || endAction == REFLECT_TOPSIDE_CRITICAL)
                    && upgoingRecBranch == branchSeq.get(branchSeq.size()-1) ) {
                // last action was downgoing, so last branch should be downgoingRecBranch
                if (DEBUG) {
                    System.out.println("Phase ends downgoing, but receiver is not on downgoing end of last branch");
                    System.out.println(endActionString(endAction)+" upgoingRecBranch="+upgoingRecBranch+"  bs="+branchSeq.get(branchSeq.size()-1));
                }
                minRayParam = -1;
                maxRayParam = -1;
            } else {
                if (DEBUG) {
                    System.out.println("Last action is: "+endActionString(endAction)+" upR="+upgoingRecBranch+" downR="+downgoingRecBranch+" last="+branchSeq.get(branchSeq.size()-1));
                }
            }
        }
    }

    PhaseInteraction currLegIs_p_s(String prevLeg, String currLeg, String nextLeg,
                                   boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        if (tMod.getVelocityModel().cmbDepth == 0) {
            // no crust or mantle, so no P or p
            maxRayParam = -1;
            return FAIL;
        }
        if(nextLeg.startsWith("v") || nextLeg.startsWith("V")) {
            throw new TauModelException(getName()+" p and s and k must always be up going "
                    + " and cannot come immediately before a top-side reflection."
                    + " currLeg=" + currLeg + " nextLeg=" + nextLeg);
        } else if(nextLeg.equals("p") || nextLeg.equals("s")) {
            throw new TauModelException(getName()+" Phase not recognized (2): "
                    + currLeg + " followed by " + nextLeg);
        } else if(nextLeg.startsWith("^")) {
            String depthString;
            depthString = nextLeg.substring(1);
            endAction = REFLECT_UNDERSIDE;
            int disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
            if(currBranch >= disconBranch) {
                addToBranch(tMod,
                        currBranch,
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                throw new TauModelException(getName()+" Phase not recognized (2): "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch
                        + " > disconBranch=" + disconBranch);
            }
        } else if(nextLeg.equals("m")
                && currBranch >= tMod.getMohoBranch()) {
            endAction = TRANSUP;
            addToBranch(tMod,
                    currBranch,
                    tMod.getMohoBranch(),
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.startsWith("P") || nextLeg.startsWith("S")
                || nextLeg.equals("END")) {
            int disconBranch;
            if (nextLeg.equals("END")) {
                disconBranch = upgoingRecBranch;
                if (currBranch < upgoingRecBranch) {
                    maxRayParam = -1;
                    if (DEBUG) {
                        System.out.println(name+" (currBranch >= receiverBranch() "
                                + currBranch
                                + " "
                                + upgoingRecBranch
                                + " so there cannot be a "
                                + currLeg
                                + " phase for this sourceDepth, receiverDepth and/or path.");
                    }
                    return FAIL;
                }
            } else {
                disconBranch = 0;
            }
            if (nextLeg.equals("END")) {
                endAction = END;
            } else {
                endAction = REFLECT_UNDERSIDE;
            }
            addToBranch(tMod,
                    currBranch,
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);

        } else if(nextLeg.equals("c") || nextLeg.equals("i")
                || nextLeg.equals("I") || nextLeg.equals("J") || nextLeg.equals("j")) {
            throw new TauModelException(getName()+" Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", p and s must be upgoing in mantle and so cannot hit core.");
        } else if(isLegDepth(currLeg)) {
            double nextLegDepth = Double.parseDouble(currLeg);
            if (nextLegDepth >= tMod.getCmbDepth()) {
                throw new TauModelException(getName()+" Phase not recognized (3): "
                        + currLeg + " followed by " + nextLeg+", p and s must be upgoing in mantle and so cannot hit core.");
            }
            int disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
            endAction = TRANSUP;
            addToBranch(tMod,
                    currBranch,
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else {
            throw new TauModelException(getName()+" Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg);
        }
        return endAction;
    }

    PhaseInteraction currLegIs_P_S(String prevLeg, String currLeg, String nextLeg,
                                   boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        SeismicPhaseSegment prevSegment = null;
        if (segmentList.size()>0) {segmentList.get(segmentList.size()-1);}
        if (tMod.getVelocityModel().cmbDepth == 0) {
            // no crust or mantle, so no P or P
            maxRayParam = -1;
            return FAIL;
        }
        if(nextLeg.equals("P") || nextLeg.equals("S")
                || nextLeg.equals("Pn") || nextLeg.equals("Sn")
                || nextLeg.equals("Pdiff") || nextLeg.equals("Sdiff")
                || nextLeg.equals("END")) {
            if(prevEndAction == START || prevEndAction == TRANSDOWN || prevEndAction == REFLECT_UNDERSIDE|| prevEndAction == REFLECT_UNDERSIDE_CRITICAL) {
                // was downgoing, so must first turn in mantle
                endAction = TURN;
                addToBranch(tMod,
                        currBranch,
                        tMod.getCmbBranch() - 1,
                        isPWave,
                        isPWave,
                        endAction,
                        currLeg);
            }
            if (nextLeg.equals("END")) {
                endAction = END;
                addToBranch(tMod, currBranch, upgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
            } else {
                endAction = REFLECT_UNDERSIDE;
                addToBranch(tMod, currBranch, 0, isPWave, nextIsPWave, endAction, currLeg);
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
                addToBranch(tMod,
                        currBranch,
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                // can't topside reflect if already below, setting maxRayParam forces no arrivals
                maxRayParam = -1;
                return FAIL;
            }
        } else if(nextLeg.startsWith("^")) {
            String depthString;
            depthString = nextLeg.substring(1);
            endAction = REFLECT_UNDERSIDE;
            int disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
            if (disconBranch == tMod.getNumBranches()) {
                maxRayParam = -1;
                if(DEBUG) {System.out.println("Attempt to underside reflect from center of earth: "+nextLeg);}
                return FAIL;
            }
            if(prevLeg.equals("K")) {
                addToBranch(tMod,
                        currBranch,
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if(prevLeg.startsWith("^") || prevLeg.equals("P")
                    || prevLeg.equals("S") || prevLeg.equals("p")
                    || prevLeg.equals("s") || prevLeg.equals("m")
                    || prevLeg.equals("START")) {
                addToBranch(tMod,
                        currBranch,
                        tMod.getCmbBranch() - 1,
                        isPWave,
                        isPWave,
                        TURN,
                        currLeg);
                addToBranch(tMod,
                        currBranch,
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
                    maxRayParam = -1;
                    if(DEBUG) {System.out.println("Attempt to reflect from center of earth: "+nextLeg);}
                    return FAIL;
                }
                addToBranch(tMod,
                        currBranch,
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                throw new TauModelException(getName()+" Phase not recognized (5): "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch
                        + " > disconBranch=" + disconBranch+" , prev="+prevLeg);
            }
        } else if(nextLeg.equals("c")) {
            if (tMod.getCmbBranch() == tMod.getNumBranches()) {
                maxRayParam = -1;
                if(DEBUG) {System.out.println("Attempt to reflect from center of earth: "+nextLeg);}
                return FAIL;
            }
            endAction = REFLECT_TOPSIDE;
            addToBranch(tMod,
                    currBranch,
                    tMod.getCmbBranch() - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.equals("K") && prevLeg.equals("K")) {
            throw new TauModelException(getName()+" Phase not recognized (5.5): "
                    + currLeg + " followed by " + nextLeg
                    + " and preceeded by "+prevLeg
                    + " when currBranch=" + currBranch
                    );
        } else if(nextLeg.startsWith("K") ) {
            endAction = TRANSDOWN;
            addToBranch(tMod,
                    currBranch,
                    tMod.getCmbBranch() - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if( nextLeg.equals("I") || nextLeg.equals("J")) {
            if(tMod.getCmbDepth() == tMod.getIocbDepth()) {
                // degenerate case of no fluid outer core, so allow phases like PIP or SJS
                endAction = TRANSDOWN;
                addToBranch(tMod,
                        currBranch,
                        tMod.getCmbBranch() - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                throw new TauModelException(getName()+" P or S followed by I or J can only exist if model has no outer core: "
                        + currLeg
                        + " followed by "
                        + nextLeg);
            }
        } else if(nextLeg.equals("m")
                || (isNextLegDepth && nextLegDepth < tMod.getCmbDepth())) {
            // treat the moho in the same wasy as 410 type
            // discontinuities
            int disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
            if (DEBUG) {
                System.out.println("DisconBranch=" + disconBranch
                        + " for " + nextLeg);
                System.out.println(tMod.getTauBranch(disconBranch,
                                isPWave)
                        .getTopDepth());
            }
            if (prevEndAction == TURN || prevEndAction == REFLECT_TOPSIDE
                    || prevEndAction == REFLECT_TOPSIDE_CRITICAL || prevEndAction == TRANSUP) {
                // upgoing section
                if (disconBranch > currBranch) {
                    // check for discontinuity below the current branch
                    // when the ray should be upgoing
                    throw new TauModelException(getName() + " Phase not recognized (6): "
                            + currLeg
                            + " followed by "
                            + nextLeg
                            + " when currBranch="
                            + currBranch
                            + " > disconBranch=" + disconBranch);
                }
                endAction = TRANSUP;
                addToBranch(tMod,
                        currBranch,
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
                String nextNextLeg = (String) legs.get(legNum + 2);
                if (nextNextLeg.equals("p") || nextNextLeg.equals("s")) {
                    // convert on upgoing section
                    endAction = TURN;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getCmbBranch() - 1,
                            isPWave,
                            isPWave,
                            endAction,
                            currLeg);
                    endAction = TRANSUP;
                    addToBranch(tMod,
                            currBranch,
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
                        addToBranch(tMod,
                                currBranch,
                                disconBranch - 1,
                                isPWave,
                                nextIsPWave,
                                endAction,
                                currLeg);
                    } else {
                        // discon is above current loc, but we have a
                        // downgoing ray, so this is an illegal ray for
                        // this source depth
                        maxRayParam = -1;
                        if (DEBUG) {
                            System.out.println("Cannot phase convert on the "
                                    + "downgoing side if the discontinuity is above "
                                    + "the phase leg starting point, "
                                    + currLeg
                                    + " "
                                    + nextLeg
                                    + " "
                                    + nextNextLeg
                                    + ", so this phase, "
                                    + getName()
                                    + " is illegal for this sourceDepth.");
                        }
                        return FAIL;
                    }
                } else {
                    throw new TauModelException(getName() + " Phase not recognized (7): "
                            + currLeg
                            + " followed by "
                            + nextLeg
                            + " followed by " + nextNextLeg);
                }
            }
        } else if (nextLeg.endsWith("n") && nextLeg.length() > 1) {
            String numString = nextLeg.substring(0, nextLeg.length() - 1);
            try {
                double headDepth = Double.parseDouble(numString);
                int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
                endAction = HEAD;
                addToBranch(tMod,
                        currBranch,
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } catch (NumberFormatException e) {
                throw new TauModelException(getName() + " Phase not recognized (7): "
                        + currLeg
                        + " followed by "
                        + nextLeg + " expected number but was `" + numString + "`", e);
            }

        } else if (nextLeg.endsWith("diff") && nextLeg.length() > 4) {
            // diff but not Pdiff or Sdiff
            String numString = nextLeg.substring(0, nextLeg.length()-4);
            try {
                double diffDepth = Double.parseDouble(numString);
                int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);

                if(prevEndAction == START || prevEndAction == TRANSDOWN || prevEndAction == REFLECT_UNDERSIDE|| prevEndAction == REFLECT_UNDERSIDE_CRITICAL) {
                    // was downgoing, so must first turn in mantle
                    endAction = TURN;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getCmbBranch() - 1,
                            isPWave,
                            isPWave,
                            endAction,
                            currLeg);
                }
                endAction = REFLECT_UNDERSIDE;
                addToBranch(tMod, currBranch, 0, isPWave, nextIsPWave, endAction, currLeg);

            } catch(NumberFormatException e) {
                throw new TauModelException(getName() + " Phase not recognized (7): "
                        + currLeg
                        + " followed by "
                        + nextLeg+" expected number but was `"+numString+"`", e);
            }
        } else {
            throw new TauModelException(getName()+" Phase not recognized (8): "
                    + currLeg + " followed by " + nextLeg);
        }
        return endAction;
    }

    PhaseInteraction currLegIs_Ped_Sed(String prevLeg, String currLeg, String nextLeg,
                                       boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        if (tMod.getVelocityModel().cmbDepth == 0) {
            // no crust or mantle, so no P or P
            maxRayParam = -1;
            return FAIL;
        }
        if(nextLeg.equals("END")) {
            if (receiverDepth > 0) {
                endAction = END_DOWN;
                addToBranch(tMod, currBranch, downgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
            } else {
                //this should be impossible except for 0 dist 0 source depth which can be called p or P
                maxRayParam = -1;
                minRayParam = -1;
                return FAIL;
            }

        } else if(nextLeg.equals("Pdiff") || nextLeg.equals("Sdiff")) {
            endAction = DIFFRACT;
            addToBranch(tMod,
                    currBranch,
                    tMod.getCmbBranch() - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if (nextLeg.endsWith("n") && nextLeg.length() > 1) {
            String numString = nextLeg.substring(0, nextLeg.length()-1);
            double headDepth = Double.parseDouble(numString);
            int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
            endAction = HEAD;
            addToBranch(tMod,
                    currBranch,
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
            addToBranch(tMod,
                    currBranch,
                    disconBranch - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.equals("K") || nextLeg.equals("Ked") || nextLeg.equals("Kdiff")) {
            endAction = TRANSDOWN;
            addToBranch(tMod,
                    currBranch,
                    tMod.getCmbBranch() - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.equals("m")) {
            endAction = TRANSDOWN;
            addToBranch(tMod,
                    currBranch,
                    tMod.getMohoBranch() - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if( isLegDepth) {
            int disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
            endAction = TRANSDOWN;
            addToBranch(tMod,
                    currBranch,
                    disconBranch - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.equals("c") || nextLeg.equals("i")) {
            int disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
            endAction = REFLECT_TOPSIDE;
            addToBranch(tMod,
                    currBranch,
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
                addToBranch(tMod,
                        currBranch,
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                throw new TauModelException(getName()+" Phase not recognized (4): "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch
                        + " < disconBranch=" + disconBranch);
            }
        } else if( nextLeg.equals("I") || nextLeg.equals("J")) {
            if(tMod.getCmbDepth() == tMod.getIocbDepth()) {
                // degenerate case of no fluid outer core, so allow phases like PIP or SJS
                endAction = TRANSDOWN;
                addToBranch(tMod,
                        currBranch,
                        tMod.getCmbBranch() - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                maxRayParam = -1;
                if(DEBUG) {System.out.println("P or S followed by I or J can only exist if model has no outer core");}
                return FAIL;
            }
        } else {
            throw new TauModelException(getName()+" Phase not recognized (1): "
                    + currLeg + " followed by " + nextLeg);
        }
        return endAction;
    }

    PhaseInteraction currLegIs_Pdiff_Sdiff(String prevLeg, String currLeg, String nextLeg,
                                       boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        if (tMod.getVelocityModel().cmbDepth == 0) {
            // no crust or mantle, so no P or P
            maxRayParam = -1;
            return FAIL;
        }
        if (currLeg.equals("Pdiff") || currLeg.equals("Sdiff")) {
            endAction = DIFFRACT;
            /*
             * in the diffracted case we trick addToBranch into thinking
             * we are turning, but then make the maxRayParam equal to
             * minRayParam, which is the deepest turning ray.
             */
            if (maxRayParam >= tMod.getTauBranch(tMod.getCmbBranch() - 1,
                            isPWave)
                    .getMinTurnRayParam()
                    && minRayParam <= tMod.getTauBranch(tMod.getCmbBranch() - 1,
                            isPWave)
                    .getMinTurnRayParam()) {

                SeismicPhaseSegment prevSegment = segmentList.size() > 0 ? segmentList.get(segmentList.size() - 1) : null;
                if (currBranch < tMod.getCmbBranch() - 1 || prevEndAction == START ||
                        (currBranch == tMod.getCmbBranch() && prevSegment != null && prevSegment.endsAtTop())
                ) {
                    endAction = DIFFRACT;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getCmbBranch() - 1,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                } else if (currBranch == tMod.getCmbBranch() - 1
                        && (prevSegment.endAction == DIFFRACT || prevSegment.endAction == TRANSUP)) {
                    // already at correct depth ?
                } else {
                    // we are below at the right branch to diffract???
                    throw new TauModelException("Unable to diffract, " + currBranch + " to cmb " + (tMod.getCmbBranch() - 1) + " " + endActionString(prevEndAction) + " " + prevSegment);
                }
                if (nextLeg.startsWith("K") || nextLeg.equals("I") || nextLeg.equals("J")) {
                    // down into inner core
                    addFlatBranch(tMod, tMod.getCmbBranch() - 1, isPWave, endAction, TRANSDOWN, currLeg);
                } else {
                    // normal case
                    addFlatBranch(tMod, tMod.getCmbBranch() - 1, isPWave, endAction, TURN, currLeg);
                }
                if (nextLeg.equals("END")) {
                    endAction = END;
                    addToBranch(tMod,
                            currBranch,
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
                        maxRayParam = -1;
                        if (DEBUG) {
                            System.out.println(getName() + " Attempt to underside reflect " + currLeg
                                    + " from deeper layer: " + nextLeg);
                        }
                        return FAIL;
                    }
                    addToBranch(tMod,
                            currBranch,
                            disconBranch,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);

                } else if (nextLeg.startsWith("P") || nextLeg.startsWith("S")) {
                    endAction = REFLECT_UNDERSIDE;
                    addToBranch(tMod,
                            currBranch,
                            0,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                } else if (nextLeg.equals("p") || nextLeg.equals("s")) {
                    // upgoing
                } else {
                    throw new TauModelException(getName() + " Phase not recognized (12): "
                            + currLeg + " followed by " + nextLeg
                            + " when currBranch=" + currBranch);
                }
            } else {
                // can't have head wave as ray param is not within range
                if (DEBUG) {
                    System.out.println("Cannot have the head wave "
                            + currLeg + " within phase " + name
                            + " for this sourceDepth and/or path.");
                    System.out.println(maxRayParam + " >= " + tMod.getTauBranch(tMod.getCmbBranch() - 1,
                                    isPWave)
                            .getMinTurnRayParam() + " " +
                            "&& " + minRayParam + " <= " + tMod.getTauBranch(tMod.getCmbBranch() - 1,
                                    isPWave)
                            .getMinTurnRayParam());
                }
                maxRayParam = -1;
                return FAIL;
            }

        } else if (currLeg.endsWith("diff") && currLeg.length()>5) {
            int depthIdx = 0;
            if (currLeg.startsWith("P") || currLeg.startsWith("S")) {
                depthIdx = 1;
            }
            String numString = currLeg.substring(depthIdx, currLeg.length()-4);
            double diffDepth = Double.parseDouble(numString);
            int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
            endAction = DIFFRACT;
            addToBranch(tMod,
                    currBranch,
                    disconBranch - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
            if (nextLeg.startsWith("K") || nextLeg.equals("I") || nextLeg.equals("J")) {
                // down into  core
                addFlatBranch(tMod, disconBranch-1, isPWave, endAction, TRANSDOWN, currLeg);
            } else {
                // normal case
                addFlatBranch(tMod, disconBranch-1, isPWave, endAction, TURN, currLeg);
            }

            if(nextLeg.equals("END")) {
                endAction = END;
                addToBranch(tMod,
                        currBranch,
                        upgoingRecBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            }

        } else {
            throw new TauModelException(getName()+" Phase not recognized for P,S: "
                    + currLeg + " followed by " + nextLeg);
        }
        return endAction;
    }

    PhaseInteraction currLegIs_Pn_Sn(String prevLeg, String currLeg, String nextLeg,
                                       boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        if (tMod.getVelocityModel().cmbDepth == 0) {
            // no crust or mantle, so no P or P
            maxRayParam = -1;
            return FAIL;
        }
        if (currLeg.endsWith("n") && currLeg.length()>2) {
            int depthIdx = 0;
            if (currLeg.startsWith("P") || currLeg.startsWith("S")) {
                depthIdx = 1;
            }
            String numString = currLeg.substring(depthIdx, currLeg.length()-1);
            double diffDepth = Double.parseDouble(numString);
            int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
            endAction = HEAD;
            addToBranch(tMod,
                    currBranch,
                    disconBranch-1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
            if (nextLeg.startsWith("K") || nextLeg.equals("I") || nextLeg.equals("J")) {
                // down into  core
                addFlatBranch(tMod, disconBranch, isPWave, endAction, TRANSDOWN, currLeg);
            } else {
                // normal case
                addFlatBranch(tMod, disconBranch, isPWave, endAction, TRANSUP, currLeg);
            }
            currBranch=disconBranch;

            if(nextLeg.equals("END")) {
                endAction = END;
                addToBranch(tMod,
                        currBranch-1,
                        upgoingRecBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            }
        } else if(currLeg.equals("Pg") || currLeg.equals("Sg")
                || currLeg.equals("Pn") || currLeg.equals("Sn")) {
            endAction = TURN;
            if(currBranch >= tMod.getMohoBranch()) {
                /*
                 * Pg, Pn, Sg and Sn must be above the moho and so is
                 * not valid for rays coming upwards from below,
                 * possibly due to the source depth. Setting maxRayParam =
                 * -1 effectively disallows this phase.
                 */
                maxRayParam = -1;
                if(DEBUG) {
                    System.out.println("(currBranch >= tMod.getMohoBranch() "
                            + currBranch
                            + " "
                            + tMod.getMohoBranch()
                            + " so there cannot be a "
                            + currLeg
                            + " phase for this sourceDepth and/or path.");
                }
                return FAIL;
            }
            if(currLeg.equals("Pg") || currLeg.equals("Sg")) {
                endAction = TURN;
                addToBranch(tMod,
                        currBranch,
                        tMod.getMohoBranch() - 1,
                        isPWave,
                        isPWave,
                        endAction,
                        currLeg);
                if(nextLeg.equals("END")) {
                    endAction = END;
                    addToBranch(tMod, currBranch, upgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
                } else if(nextLeg.startsWith("P") || nextLeg.startsWith("S")) {
                    endAction = REFLECT_UNDERSIDE;
                    addToBranch(tMod, currBranch, 0, isPWave, nextIsPWave, endAction, currLeg);
                } else if(nextLeg.startsWith("^")) {
                    String depthString;
                    depthString = nextLeg.substring(1);
                    endAction = REFLECT_UNDERSIDE;
                    int disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
                    if (disconBranch >= tMod.getMohoBranch()) {
                        maxRayParam = -1;
                        if(DEBUG) {
                            System.out.println(getName()+" Attempt to underside reflect "+currLeg
                                    +" from deeper layer: "+nextLeg);
                        }
                        return FAIL;
                    }
                    addToBranch(tMod,
                                currBranch,
                                disconBranch,
                                isPWave,
                                nextIsPWave,
                                endAction,
                                currLeg);

                } else {
                    throw new TauModelException(getName()+" Phase not recognized (12): "
                            + currLeg + " followed by " + nextLeg);
                }
            } else if(currLeg.equals("Pn") || currLeg.equals("Sn")) {
                /*
                 * in the refracted case we trick addToBranch into
                 * thinking we are turning below the moho, but then make
                 * the minRayParam equal to maxRayParam, which is the
                 * head wave ray.
                 */
                if(maxRayParam >= tMod.getTauBranch(tMod.getMohoBranch(),
                                isPWave)
                        .getMaxRayParam()
                        && minRayParam <= tMod.getTauBranch(tMod.getMohoBranch(),
                                isPWave)
                        .getMaxRayParam()) {
                    endAction = HEAD;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getMohoBranch()-1,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                    minRayParam = maxRayParam;
                    if (nextLeg.equals("Ped") || nextLeg.equals("Sed")) {
                        // down into  core
                        addFlatBranch(tMod, tMod.getMohoBranch(), isPWave, endAction, TRANSDOWN, currLeg);
                    } else {
                        // normal case
                        addFlatBranch(tMod, tMod.getMohoBranch(), isPWave, endAction, TRANSUP, currLeg);
                    }

                    if(nextLeg.equals("END")) {
                        endAction = END;
                        if (currBranch >= upgoingRecBranch) {
                            addToBranch(tMod,
                                    currBranch,
                                    upgoingRecBranch,
                                    isPWave,
                                    nextIsPWave,
                                    endAction,
                                    currLeg);
                        } else {
                            maxRayParam = -1;
                            if(DEBUG) {
                                System.out.println("Cannot have the head wave "
                                        + currLeg + " within phase " + name
                                        + " for this sourceDepth, receiverDepth and/or path.");
                            }
                            return FAIL;
                        }
                    } else if ( nextLeg.startsWith("P") || nextLeg.startsWith("S")) {
                        endAction = REFLECT_UNDERSIDE;
                        addToBranch(tMod,
                                currBranch,
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
                            maxRayParam = -1;
                            if(DEBUG) {
                                System.out.println(getName()+" Attempt to underside reflect "+currLeg
                                        +" from deeper layer: "+nextLeg);
                            }
                            return FAIL;
                        }
                        addToBranch(tMod,
                                currBranch,
                                disconBranch,
                                isPWave,
                                nextIsPWave,
                                endAction,
                                currLeg);
                    } else {
                        throw new TauModelException(getName()+" Phase not recognized (12): "
                                + currLeg + " followed by " + nextLeg);
                    }
                } else {
                    // can't have head wave as ray param is not within
                    // range
                    maxRayParam = -1;
                    if(DEBUG) {
                        System.out.println("Cannot have the head wave "
                                + currLeg + " within phase " + name
                                + " for this sourceDepth and/or path.");
                    }
                    return FAIL;
                }
            }
        } else {
            throw new TauModelException(getName()+" Phase not recognized for P,S: "
                    + currLeg + " followed by " + nextLeg);
        }
        return endAction;
    }

    PhaseInteraction currLegIs_OtherHead(String prevLeg, String currLeg, String nextLeg,
                                         boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        if (currLeg.endsWith("n")) {
            if(nextLeg.equals("END")) {
                endAction = END;
                addToBranch(tMod,
                        currBranch,
                        upgoingRecBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
                // should handle other nextLeg besides END ???
            } else {
                throw new TauModelException(getName() + " Phase not recognized for non-standard diffraction: "
                        + currLeg + " followed by " + nextLeg);
            }
        } else {
            throw new TauModelException(getName() + " Phase not recognized for non-standard diffraction: "
                    + currLeg + " followed by " + nextLeg);
        }
        return endAction;
    }

    boolean checkDegenerateOuterCore(String prevLeg, String currLeg, String nextLeg,
                                 boolean isPWave, boolean isPWavePrev, int legNum)
            throws TauModelException {
        if (tMod.getCmbDepth() == tMod.getRadiusOfEarth()) {
            // degenerate case, CMB is at center, so model without a core
            maxRayParam = -1;
            if(DEBUG) {
                System.out.println("Cannot have K phase "
                        + currLeg + " within phase " + name
                        + " for this model as it has no core, cmb depth = radius of Earth.");
            }
            return false;
        }
        if (tMod.getCmbDepth() == tMod.getIocbDepth()) {
            // degenerate case, CMB is same as IOCB, so model without an outer core
            maxRayParam = -1;
            if(DEBUG) {
                System.out.println("Cannot have K phase "
                        + currLeg + " within phase " + name
                        + " for this model as it has no outer core, cmb depth = iocb depth, "+tMod.getCmbDepth());
            }
            return false;
        }
        return true;
    }

    PhaseInteraction currLegIs_Kdiff(String prevLeg, String currLeg, String nextLeg,
                                     boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;

        if (tMod.getCmbBranch() == tMod.tauBranches[0].length || tMod.getIocbBranch() == tMod.tauBranches[0].length) {
            // cmb or iocb is center of earth, no core or inner core to diffract
            maxRayParam = -1;
            return FAIL;
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

        endAction = DIFFRACT;
        addToBranch(tMod,
                currBranch,
                disconBranch - 1,
                PWAVE,
                nextIsPWave,
                endAction,
                currLeg);
        if (nextLeg.equals("I") || nextLeg.equals("J")) {
            // down into inner core
            addFlatBranch(tMod, disconBranch-1, isPWave, endAction, TRANSDOWN, currLeg);
        } else {
            // normal case
            addFlatBranch(tMod, disconBranch-1, isPWave, endAction, TURN, currLeg);
        }

        if (nextLeg.startsWith("P") || nextLeg.startsWith("S") || nextLeg.equals("p") || nextLeg.equals("s")) {
            endAction = TRANSUP;
            addToBranch(tMod,
                    currBranch,
                    tMod.getCmbBranch(),
                    PWAVE,
                    nextIsPWave,
                    endAction,
                    currLeg);

        } else if (nextLeg.equals("K") || (nextLeg.startsWith("K") && nextLeg.endsWith("diff"))) {
            endAction = REFLECT_UNDERSIDE;
            addToBranch(tMod,
                    currBranch,
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
                throw new TauModelException(getName() + " Phase not recognized (5a): "
                        + currLeg + " followed by " + nextLeg
                        + " when reflectBranch=" + reflectBranch
                        + " < cmbBranch=" + tMod.getCmbBranch() + ", likely need P or S leg , prev=" + prevLeg);
            }
            if (reflectBranch >= disconBranch) {
                throw new TauModelException(getName() + " Phase not recognized (5b): "
                        + currLeg + " followed by " + nextLeg
                        + " when reflectBranch=" + reflectBranch
                        + " > disconBranch=" + disconBranch + ", likely need K, I or J leg , prev=" + prevLeg);
            }
            if (reflectBranch == tMod.getNumBranches()) {
                maxRayParam = -1;
                if (DEBUG) {
                    System.out.println("Attempt to underside reflect from center of earth: " + nextLeg);
                }
                return FAIL;
            }
            addToBranch(tMod,
                    currBranch,
                    reflectBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else {
            throw new TauModelException("Should not allow " + currLeg + " followed by " + nextLeg);
        }
        return endAction;

    }

    PhaseInteraction currLegIs_K(String prevLeg, String currLeg, String nextLeg,
                                 boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        if (currLeg.equals("K")) {
            if (tMod.getCmbBranch() == tMod.tauBranches[0].length) {
                // cmb is center of earth, no core
                maxRayParam = -1;
                return FAIL;
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
                    addToBranch(tMod,
                            currBranch,
                            tMod.getIocbBranch() - 1,
                            isPWave,
                            isPWave,
                            endAction,
                            currLeg);
                }
                endAction = TRANSUP;
                addToBranch(tMod,
                        currBranch,
                        tMod.getCmbBranch(),
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if(nextLeg.equals("K") || nextLeg.equals("Ked") || nextLeg.equals("END")) {
                if(prevLeg.equals("P") || prevLeg.equals("S")
                        || prevLeg.equals("K")) {
                    endAction = TURN;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getIocbBranch() - 1,
                            isPWave,
                            isPWave,
                            endAction,
                            currLeg);
                }
                if (nextLeg.equals("END")) {
                    endAction = END;
                    addToBranch(tMod, currBranch, upgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
                } else {
                    endAction = REFLECT_UNDERSIDE;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getCmbBranch(),
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                }
            } else if(nextLeg.startsWith("I") || nextLeg.startsWith("J")) {
                endAction = TRANSDOWN;
                addToBranch(tMod,
                        currBranch,
                        tMod.getIocbBranch() - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if(nextLeg.equals("i")) {
                if (tMod.getIocbBranch() == tMod.getNumBranches()) {
                    maxRayParam = -1;
                    if (DEBUG) {
                        System.out.println("Attempt to reflect from center of earth: " + nextLeg);
                    }
                    return FAIL;
                }
                endAction = REFLECT_TOPSIDE;
                addToBranch(tMod,
                        currBranch,
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
                    addToBranch(tMod,
                            currBranch,
                            disconBranch - 1,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                } else {
                    throw new TauModelException(getName()+" Phase not recognized (4): "
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
                    throw new TauModelException(getName() + " Phase not recognized (5a): "
                            + currLeg + " followed by " + nextLeg
                            + " when disconBranch=" + disconBranch
                            + " < cmbBranch=" + tMod.getCmbBranch() + ", likely need P or S leg , prev=" + prevLeg);
                }
                if (disconBranch >= tMod.getIocbBranch()) {
                    throw new TauModelException(getName() + " Phase not recognized (5b): "
                            + currLeg + " followed by " + nextLeg
                            + " when disconBranch=" + disconBranch
                            + " > iocbBranch=" + tMod.getIocbBranch() + ", likely need Ior J leg , prev=" + prevLeg);
                }
                if (disconBranch == tMod.getNumBranches()) {
                    maxRayParam = -1;
                    if (DEBUG) {
                        System.out.println("Attempt to underside reflect from center of earth: " + nextLeg);
                    }
                    return FAIL;
                }
                if (prevLeg.startsWith("I") || prevLeg.startsWith("J")
                        || prevLeg.equals("i") || prevLeg.equals("j")
                        || prevLeg.equals("k")) {
                    // upgoind K leg
                    addToBranch(tMod,
                            currBranch,
                            disconBranch,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                } else if (prevLeg.equals("P") || prevLeg.equals("S") ||
                        prevLeg.startsWith("^") ||
                        prevLeg.equals("K") || prevLeg.equals("START")) {
                    addToBranch(tMod,
                            currBranch,
                            tMod.getIocbBranch() - 1,
                            isPWave,
                            isPWave,
                            TURN,
                            currLeg);
                    addToBranch(tMod,
                            currBranch,
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
                        addToBranch(tMod,
                                currBranch,
                                disconBranch,
                                isPWave,
                                nextIsPWave,
                                endAction,
                                currLeg);
                    } else {
                        // down-turn-up
                        addToBranch(tMod,
                                currBranch,
                                tMod.getIocbBranch() - 1,
                                isPWave,
                                isPWave,
                                TURN,
                                currLeg);
                        addToBranch(tMod,
                                currBranch,
                                disconBranch,
                                isPWave,
                                nextIsPWave,
                                endAction,
                                currLeg);
                    }
                } else {
                    throw new TauModelException(getName() + " Phase not recognized (5): "
                            + currLeg + " followed by " + nextLeg
                            + " when currBranch=" + currBranch
                            + " > disconBranch=" + disconBranch + " , prev=" + prevLeg);
                }
            } else if (nextLeg.endsWith("n") && nextLeg.length() > 1) {
                String numString = nextLeg.substring(0, nextLeg.length()-1);
                double headDepth = Double.parseDouble(numString);
                int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
                if (disconBranch < tMod.cmbBranch) {
                    throw new TauModelException(getName() + " Phase not recognized (5): "
                            + currLeg + " followed by " + nextLeg
                            + " when cmbBranch < disconBranch=" + disconBranch + " , prev=" + prevLeg);
                }
                endAction = HEAD;
                addToBranch(tMod,
                        currBranch,
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
                    throw new TauModelException(getName() + " Phase not recognized (5): "
                            + currLeg + " followed by " + nextLeg
                            + " when cmbBranch < disconBranch=" + disconBranch + " , prev=" + prevLeg);
                }
                endAction = DIFFRACT;
                addToBranch(tMod,
                        currBranch,
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                throw new TauModelException(getName()+" Phase not recognized (9b): "
                        + currLeg + " followed by " + nextLeg);
            }
        } else {
            throw new TauModelException(getName()+" Phase not recognized (9): "
                    + currLeg + " followed by " + nextLeg);
        }
        return endAction;
    }

    PhaseInteraction currLegIs_Ked(String prevLeg, String currLeg, String nextLeg,
                                   boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        if (currLeg.equals("Ked")) {
            if (tMod.getCmbBranch() == tMod.tauBranches[0].length) {
                // cmb is center of earth, no core
                maxRayParam = -1;
                return FAIL;
            }
            if(nextLeg.equals("END")) {
                endAction = END_DOWN;
                addToBranch(tMod, currBranch, downgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
            } else if(nextLeg.equals("I") || nextLeg.equals("J")) {
                endAction = TRANSDOWN;
                addToBranch(tMod,
                        currBranch,
                        tMod.getIocbBranch() - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else if(nextLeg.equals("i")) {
                if (tMod.getIocbBranch() == tMod.getNumBranches()) {
                    maxRayParam = -1;
                    if (DEBUG) {
                        System.out.println("Attempt to reflect from center of earth: " + nextLeg);
                    }
                    return FAIL;
                }
                endAction = REFLECT_TOPSIDE;
                addToBranch(tMod,
                        currBranch,
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
                    addToBranch(tMod,
                            currBranch,
                            disconBranch - 1,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                } else {
                    throw new TauModelException(getName()+" Phase not recognized (4): "
                            + currLeg + " followed by " + nextLeg
                            + " when currBranch=" + currBranch
                            + " < disconBranch=" + disconBranch);
                }
            } else if (nextLeg.endsWith("n") && nextLeg.length() > 1) {
                String numString = nextLeg.substring(0, nextLeg.length()-1);
                double headDepth = Double.parseDouble(numString);
                int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
                if (disconBranch < tMod.cmbBranch) {
                    throw new TauModelException(getName() + " Phase not recognized (5): "
                            + currLeg + " followed by " + nextLeg
                            + " when cmbBranch < disconBranch=" + disconBranch + " , prev=" + prevLeg);
                }
                endAction = HEAD;
                addToBranch(tMod,
                        currBranch,
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
                    throw new TauModelException(getName() + " Phase not recognized (5): "
                            + currLeg + " followed by " + nextLeg
                            + " when cmbBranch < disconBranch=" + disconBranch + " , prev=" + prevLeg);
                }
                endAction = DIFFRACT;
                addToBranch(tMod,
                        currBranch,
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                throw new TauModelException(getName()+" Phase not recognized (9b): "
                        + currLeg + " followed by " + nextLeg);
            }
        } else {
            throw new TauModelException(getName()+" Phase not recognized (9): "
                    + currLeg + " followed by " + nextLeg);
        }
        return endAction;
    }

    PhaseInteraction currLegIs_k(String prevLeg, String currLeg, String nextLeg,
                                 boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        if(nextLeg.startsWith("v") || nextLeg.startsWith("V")) {
            throw new TauModelException(getName()+" k must always be up going "
                    + " and cannot come immediately before a top-side reflection."
                    + " currLeg=" + currLeg + " nextLeg=" + nextLeg);
        } else if(nextLeg.startsWith("^")) {
            String depthString;
            depthString = nextLeg.substring(1);
            endAction = REFLECT_UNDERSIDE;
            int disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
            if (disconBranch < tMod.getCmbBranch() || disconBranch >= tMod.getIocbBranch()) {
                throw new TauModelException(getName()+" Phase not recognized (2): "
                        + currLeg + " followed by " + nextLeg
                        + ", discon not in outer core.");
            }
            if(currBranch >= disconBranch) {
                addToBranch(tMod,
                        currBranch,
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                throw new TauModelException(getName()+" Phase not recognized (2): "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch
                        + " > disconBranch=" + disconBranch);
            }
        } else if(nextLeg.startsWith("P") || nextLeg.startsWith("S")
                || nextLeg.equals("p") || nextLeg.equals("s")
                || nextLeg.equals("c")
                || nextLeg.equals("K") || nextLeg.equals("Ked")
                || nextLeg.equals("END")) {
            int disconBranch;
            if (nextLeg.equals("END")) {
                disconBranch = upgoingRecBranch;
                if (currBranch < upgoingRecBranch) {
                    maxRayParam = -1;
                    if (DEBUG) {
                        System.out.println(name+" (currBranch >= receiverBranch() "
                                + currBranch
                                + " "
                                + upgoingRecBranch
                                + " so there cannot be a "
                                + currLeg
                                + " phase for this sourceDepth, receiverDepth and/or path.");
                    }
                    return FAIL;
                }
            } else  {
                disconBranch = tMod.getCmbBranch();
            }
            if (nextLeg.startsWith("P") || nextLeg.startsWith("S")
               || nextLeg.startsWith("p") || nextLeg.startsWith("s")
                    || nextLeg.equals("c")) {
                endAction = TRANSUP;
            } else if (nextLeg.equals("END")) {
                endAction = END;
            } else if (nextLeg.equals("K") || nextLeg.equals("Ked")) {
                endAction = REFLECT_UNDERSIDE;
            } else {
                throw new TauModelException(getName()+" Phase not recognized (3): "
                        + currLeg + " followed by " + nextLeg);
            }
            addToBranch(tMod,
                    currBranch,
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);

        } else if(nextLeg.equals("c") ) {
            throw new TauModelException(getName()+" Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", k must be upgoing in outer core and so cannot hit cmb from above.");
        } else if(nextLeg.equals("i")
                || nextLeg.equals("I") || nextLeg.equals("J") || nextLeg.equals("j")) {
            throw new TauModelException(getName()+" Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", k must be upgoing in outer core and so cannot hit inner core.");
        } else if(nextLeg.equals("k")) {
            throw new TauModelException(getName()+" Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", k must be upgoing in outer core and so repeat.");
        } else if(isLegDepth(currLeg)) {
            double nextLegDepth = Double.parseDouble(currLeg);
            if (nextLegDepth >= tMod.getCmbDepth()) {
                throw new TauModelException(getName()+" Phase not recognized (3): "
                        + currLeg + " followed by " + nextLeg+", p and s must be upgoing in mantle and so cannot hit core.");
            }
            int disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
            if (disconBranch < tMod.getCmbBranch() || disconBranch >= tMod.getIocbBranch()) {
                throw new TauModelException(getName()+" Phase not recognized (2): "
                        + currLeg + " followed by " + nextLeg
                        + ", discon not in outer core.");
            }
            endAction = TRANSUP;
            addToBranch(tMod,
                    currBranch,
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else {
            throw new TauModelException(getName()+" Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg);
        }
        return endAction;
    }

    boolean checkDegenerateInnerCore(String prevLeg, String currLeg, String nextLeg,
                                   boolean isPWave, boolean isPWavePrev, int legNum)
            throws TauModelException {
        if (tMod.getIocbDepth() == tMod.getRadiusOfEarth()) {
            // degenerate case, IOCB is at center, so model without a inner core
            maxRayParam = -1;
            if (DEBUG) {
                System.out.println("Cannot have I or J phase "
                        + currLeg + " within phase " + name
                        + " for this model as it has no inner core, iocb depth = radius of Earth.");
            }
            return false;
        }
        return true;
    }

    PhaseInteraction currLegIs_I_J(String prevLeg, String currLeg, String nextLeg,
                                   boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        if( ! nextLeg.startsWith("v") && ! nextLeg.startsWith("V")
                && (prevEndAction == START || prevEndAction == TRANSDOWN || prevEndAction == REFLECT_UNDERSIDE || prevEndAction == REFLECT_UNDERSIDE_CRITICAL)) {
            // was downgoing, not reflecting, so must first turn in inner core
            endAction = TURN;
            addToBranch(tMod,
                    currBranch,
                    tMod.getNumBranches() - 1,
                    isPWave,
                    isPWave,
                    endAction,
                    currLeg);

        }
        // have already TURNed
        if(nextLeg.equals("END")) {
            endAction = END;
            addToBranch(tMod, currBranch, upgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
        } else if(nextLeg.equals("I") || nextLeg.equals("J")) {
            endAction = REFLECT_UNDERSIDE;
            addToBranch(tMod,
                    currBranch,
                    tMod.getIocbBranch(),
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.equals("K") || nextLeg.equals("k")) {
            endAction = TRANSUP;
            addToBranch(tMod,
                    currBranch,
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
                addToBranch(tMod,
                        currBranch,
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                throw new TauModelException(getName()+" Phase not recognized (4): "
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
                throw new TauModelException(getName()+" Phase not recognized (6a): "
                        + currLeg + " followed by " + nextLeg
                        + " when disconBranch=" + disconBranch
                        +" < iocbBranch="+tMod.getIocbBranch()+", likely need K leg , prev="+prevLeg);
            }
            if (disconBranch == tMod.getNumBranches()) {
                maxRayParam = -1;
                if(DEBUG) {System.out.println("Attempt to underside reflect from center of earth: "+nextLeg);}
                return FAIL;
            }
            addToBranch(tMod,
                    currBranch,
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else if(nextLeg.equalsIgnoreCase("P") || nextLeg.equalsIgnoreCase("S")) {
            if (tMod.getCmbDepth() == tMod.getIocbDepth()) {
                // degenerate case of no fluid outer core, so allow phases like PIP or SJS
                endAction = TRANSUP;
                addToBranch(tMod,
                        currBranch,
                        tMod.getIocbBranch(),
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                maxRayParam = -1;
                endAction = FAIL;
                throw new TauModelException(getName()+" Cannot have I or J phase "
                        + currLeg
                        + " followed by "+nextLeg
                        + " within phase " + name
                        + " for this model as it has an outer core so need K,k in between.");
            }
        } else if (nextLeg.endsWith("n") && nextLeg.length() > 1) {
            String numString = nextLeg.substring(0, nextLeg.length()-1);
            double headDepth = Double.parseDouble(numString);
            int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
            if (disconBranch < tMod.iocbBranch) {
                throw new TauModelException(getName() + " Phase not recognized (5): "
                        + currLeg + " followed by " + nextLeg
                        + " when iocbBranch < disconBranch=" + disconBranch + " , prev=" + prevLeg);
            }
            endAction = HEAD;
            addToBranch(tMod,
                    currBranch,
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
                throw new TauModelException(getName() + " Phase not recognized (5): "
                        + currLeg + " followed by " + nextLeg
                        + " when iocbBranch="+tMod.getIocbBranch()+" < disconBranch=" + disconBranch + " , prev=" + prevLeg);
            }
            endAction = DIFFRACT;
            addToBranch(tMod,
                    currBranch,
                    disconBranch - 1,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);

        } else {
            throw new TauModelException(getName()+" Phase not recognized (6a): "
                    + currLeg + " followed by " + nextLeg);
        }
        return endAction;
    }


    PhaseInteraction currLegIs_Ied_Jed(String prevLeg, String currLeg, String nextLeg,
                                       boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        if (currLeg.equals("Ied") || currLeg.equals("Jed")) {
            if(nextLeg.equals("END")) {
                endAction = END_DOWN;
                addToBranch(tMod, currBranch, downgoingRecBranch, isPWave, nextIsPWave, endAction, currLeg);
            } else if(nextLeg.startsWith("v") || nextLeg.startsWith("V") ) {
                if (nextLeg.startsWith("V")) {
                    endAction = REFLECT_TOPSIDE_CRITICAL;
                } else {
                    endAction = REFLECT_TOPSIDE;
                }
                int disconBranch = LegPuller.closestBranchToDepth(tMod,
                        nextLeg.substring(1));
                if(currBranch <= disconBranch - 1) {
                    addToBranch(tMod,
                            currBranch,
                            disconBranch - 1,
                            isPWave,
                            nextIsPWave,
                            endAction,
                            currLeg);
                } else {
                    throw new TauModelException(getName()+" Phase not recognized (4): "
                            + currLeg + " followed by " + nextLeg
                            + " when currBranch=" + currBranch
                            + " < disconBranch=" + disconBranch);
                }
            } else if (nextLeg.endsWith("n") && nextLeg.length() > 1) {
                String numString = nextLeg.substring(0, nextLeg.length()-1);
                double headDepth = Double.parseDouble(numString);
                int disconBranch = LegPuller.closestBranchToDepth(tMod, numString);
                if (disconBranch < tMod.iocbBranch) {
                    throw new TauModelException(getName() + " Phase not recognized (5): "
                            + currLeg + " followed by " + nextLeg
                            + " when iocbBranch < disconBranch=" + disconBranch + " , prev=" + prevLeg);
                }
                endAction = HEAD;
                addToBranch(tMod,
                        currBranch,
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
                    throw new TauModelException(getName() + " Phase not recognized (5): "
                            + currLeg + " followed by " + nextLeg
                            + " when iocbBranch < disconBranch=" + disconBranch + " , prev=" + prevLeg);
                }
                endAction = DIFFRACT;
                addToBranch(tMod,
                        currBranch,
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                throw new TauModelException(getName()+" Phase not recognized (9b): "
                        + currLeg + " followed by " + nextLeg);
            }
        } else {
            throw new TauModelException(getName()+" Phase not recognized (9): "
                    + currLeg + " followed by " + nextLeg);
        }
        return endAction;
    }

    PhaseInteraction currLegIs_I_Jdiff(String prevLeg, String currLeg, String nextLeg,
                                     boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;

        if (tMod.getIocbBranch() == tMod.tauBranches[0].length ) {
            // cmb or iocb is center of earth, no core or inner core to diffract
            maxRayParam = -1;
            return FAIL;
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
        addToBranch(tMod,
                currBranch,
                disconBranch - 1,
                PWAVE,
                nextIsPWave,
                endAction,
                currLeg);
        if (nextLeg.equals("I") || nextLeg.equals("J")) {
            // down into inner core
            addFlatBranch(tMod, disconBranch-1, isPWave, endAction, TRANSDOWN, currLeg);
        } else {
            // normal case
            addFlatBranch(tMod, disconBranch-1, isPWave, endAction, TURN, currLeg);
        }

        if (nextLeg.startsWith("K")  || nextLeg.equals("k")) {
            endAction = TRANSUP;
            addToBranch(tMod,
                    currBranch,
                    tMod.getIocbBranch(),
                    PWAVE,
                    nextIsPWave,
                    endAction,
                    currLeg);

        } else if (nextLeg.equals("I") || nextLeg.equals("I")
                || ((nextLeg.startsWith("I") || nextLeg.startsWith("J")) && nextLeg.endsWith("diff"))) {
            endAction = REFLECT_UNDERSIDE;
            addToBranch(tMod,
                    currBranch,
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
                throw new TauModelException(getName() + " Phase not recognized (5a): "
                        + currLeg + " followed by " + nextLeg
                        + " when reflectBranch=" + reflectBranch
                        + " < iocbBranch=" + tMod.getIocbBranch() + ", likely need P or S leg , prev=" + prevLeg);
            }
            if (reflectBranch >= disconBranch) {
                throw new TauModelException(getName() + " Phase not recognized (5b): "
                        + currLeg + " followed by " + nextLeg
                        + " when reflectBranch=" + reflectBranch
                        + " > disconBranch=" + disconBranch + ", likely need K, I or J leg , prev=" + prevLeg);
            }
            if (reflectBranch == tMod.getNumBranches()) {
                maxRayParam = -1;
                if (DEBUG) {
                    System.out.println("Attempt to underside reflect from center of earth: " + nextLeg);
                }
                return FAIL;
            }
            addToBranch(tMod,
                    currBranch,
                    reflectBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else {
            throw new TauModelException("Should not allow " + currLeg + " followed by " + nextLeg);
        }
        return endAction;

    }


    PhaseInteraction currLegIs_y_j(String prevLeg, String currLeg, String nextLeg,
                                   boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum)
            throws TauModelException {
        PhaseInteraction endAction;
        if(nextLeg.startsWith("v") || nextLeg.startsWith("V")) {
            throw new TauModelException(getName()+" y,j must always be up going "
                    + " and cannot come immediately before a top-side reflection."
                    + " currLeg=" + currLeg + " nextLeg=" + nextLeg);
        } else if(nextLeg.startsWith("^")) {
            String depthString;
            depthString = nextLeg.substring(1);
            endAction = REFLECT_UNDERSIDE;
            int disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
            if (disconBranch < tMod.getIocbBranch() ) {
                throw new TauModelException(getName()+" Phase not recognized (2): "
                        + currLeg + " followed by " + nextLeg
                        + ", discon not in inner core.");
            }
            if(currBranch >= disconBranch) {
                addToBranch(tMod,
                        currBranch,
                        disconBranch,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                throw new TauModelException(getName()+" Phase not recognized (2): "
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
                || nextLeg.equals("END")) {
            int disconBranch;
            if (nextLeg.equals("END")) {
                disconBranch = upgoingRecBranch;
                if (currBranch < upgoingRecBranch) {
                    maxRayParam = -1;
                    if (DEBUG) {
                        System.out.println(name+" (currBranch >= receiverBranch() "
                                + currBranch
                                + " "
                                + upgoingRecBranch
                                + " so there cannot be a "
                                + currLeg
                                + " phase for this sourceDepth, receiverDepth and/or path.");
                    }
                    return FAIL;
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
                    throw new TauModelException(getName()+" Phase not recognized (3): "
                            + currLeg + " followed by " + nextLeg+", outer core leg would be K or k");
                }
            } else if (nextLeg.equals("END")) {
                endAction = END;
            } else if (nextLeg.equals("I") || nextLeg.equals("Ied")
                || nextLeg.equals("J") || nextLeg.equals("Jed")) {
                endAction = REFLECT_UNDERSIDE;
            } else {
                throw new TauModelException(getName()+" Phase not recognized (3): "
                        + currLeg + " followed by " + nextLeg);
            }
            addToBranch(tMod,
                    currBranch,
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);

        } else if(nextLeg.equals("i") ) {
            throw new TauModelException(getName()+" Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", y,j must be upgoing in inner core and so cannot hit iomb from above.");
        } else if(nextLeg.equals("I") || nextLeg.equals("J") ) {
            throw new TauModelException(getName()+" Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", y,j must be upgoing in outer core and so cannot hit inner core.");
        } else if(nextLeg.equals("y") || nextLeg.equals("j")) {
            throw new TauModelException(getName()+" Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg+", y,j must be upgoing in inner core and so cannot repeat.");
        } else if(isLegDepth(currLeg)) {
            double nextLegDepth = Double.parseDouble(currLeg);
            if (nextLegDepth >= tMod.getCmbDepth()) {
                throw new TauModelException(getName()+" Phase not recognized (3): "
                        + currLeg + " followed by " + nextLeg+", y and j must be upgoing in inner core and so cannot hit depth "+nextLegDepth);
            }
            int disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
            if (disconBranch < tMod.getIocbBranch() ) {
                throw new TauModelException(getName()+" Phase not recognized (2): "
                        + currLeg + " followed by " + nextLeg
                        + ", discon not in inner core.");
            }
            endAction = TRANSUP;
            addToBranch(tMod,
                    currBranch,
                    disconBranch,
                    isPWave,
                    nextIsPWave,
                    endAction,
                    currLeg);
        } else {
            throw new TauModelException(getName()+" Phase not recognized (3): "
                    + currLeg + " followed by " + nextLeg);
        }
        return endAction;
    }

    /*
     * Adds the branch numbers from startBranch to endBranch, inclusive, to
     * branchSeq, in order. Also, currBranch is set correctly based on the value
     * of endAction. endAction can be one of PhaseInteraction like
     * TRANSUP, TRANSDOWN, REFLECTTOP, REFLECTBOT, or TURN.
     */
    protected SeismicPhaseSegment addToBranch(TauModel tMod,
                               int startBranch,
                               int endBranch,
                               boolean isPWave,
                               boolean nextIsPWave,
                               PhaseInteraction endAction,
                               String currLeg) throws TauModelException {
        if (startBranch < 0 || startBranch > tMod.getNumBranches()) {
            throw new IllegalArgumentException(getName()+": start branch outside range: (0-"+tMod.getNumBranches()+") "+startBranch);
        }
        if (endBranch < 0 || endBranch > tMod.getNumBranches()) {
            throw new IllegalArgumentException(getName()+": end branch outside range: "+endBranch);
        }
        if(endAction == TRANSUP && endBranch == 0) {
            throw new IllegalArgumentException(getName()+": cannot TRANSUP with end branch zero: "+endBranch);
        }
        if( ! isPWave && tMod.getSlownessModel().depthInFluid(tMod.getTauBranch(startBranch, isPWave).getTopDepth())) {
            // S wave in fluid
            throw new TauModelException("Attempt to have S wave in fluid layer in "+name+" "+startBranch+" to "+endBranch+" "+endActionString(endAction));
        }
        int endOffset;
        boolean isDownGoing;
        if(DEBUG) {
            System.out.println("before addToBranch: minRP="+minRayParam+"  maxRP="+maxRayParam);
            System.out.println("addToBranch( start=" + startBranch + " end=" + endBranch
                    + " endAction="+endActionString(endAction)+" "+currLeg+") isP:"+(isPWave?"P":"S"));

        }
        if(endAction == TURN) {
            if (isPWave != nextIsPWave) {
                throw new TauModelException(name+" phase conversion not allowed for TURN");
            }
            endOffset = 0;
            isDownGoing = true;
            minRayParam = Math.max(minRayParam, tMod.getTauBranch(endBranch,
                            isPWave)
                    .getMinTurnRayParam());
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
                    if (DEBUG) {
                        System.out.println("Warn, ray cannot turn in layer "+bNum+" due to high slowness layer at bottom depth "+tMod.getTauBranch(bNum, isPWave).getBotDepth());
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
                    throw new TauModelException(e);
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
                    throw new TauModelException(e);
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
                throw new TauModelException(name+" Cannot TRANSDOWN if endBranch: "+endBranch+" == numBranchs: "+tMod.getNumBranches());
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
            if (prevEndAction == TRANSUP) {
                // diffract on upgoing leg from below
                isDownGoing = false;
            } else {
                isDownGoing = true;
            }
            // ray must reach discon
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch, isPWave).getMinTurnRayParam());
            // and propagate at the smallest turning ray param, may be different if phase conversion, ie SedPdiff
            if (isPWave != nextIsPWave) {
                maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch, nextIsPWave).getMinTurnRayParam());
            }
            // min rp same as max
            minRayParam = Math.max(minRayParam, maxRayParam);
            double depth = tMod.getTauBranch(endBranch, isPWave).getBotDepth();
            if(depth == tMod.radiusOfEarth ||
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
        SeismicPhaseSegment segment = new SeismicPhaseSegment(tMod, startBranch, endBranch, isPWave, endAction, isDownGoing, currLeg);
        if (segmentList.size() == 0) {
            segment.prevEndAction = START;
        } else {
            SeismicPhaseSegment prevSegment = segmentList.get(segmentList.size()-1);
            segment.prevEndAction = prevSegment.endAction;
            if (prevSegment.isFlat) {
                if (isDownGoing) {
                    if (prevSegment.endsAtTop() && prevSegment.endBranch != startBranch) {
                        throw new TauModelException(getName() + ": Flat Segment is ends at top, but start is not current branch: " + currLeg);
                    } else if (!prevSegment.endsAtTop() && prevSegment.endBranch != startBranch - 1) {
                        throw new TauModelException(getName() + ": Flat Segment is ends at bottom, but start is not next deeper branch: " + currLeg);
                    }
                } else {
                    if (prevSegment.endsAtTop() && prevSegment.endBranch != startBranch +1) {
                        System.out.println(prevSegment.toString());
                        throw new TauModelException(getName() + ": Flat Segment is ends at top, but start is not next shallower branch: " + currLeg+" "+prevSegment.endBranch +"!= "+startBranch+"+1");
                    } else if (!prevSegment.endsAtTop() && prevSegment.endBranch != startBranch) {
                        throw new TauModelException(getName() + ": Flat Segment is ends at bottom, but start is not current branch: " + currLeg+" "+prevSegment.endBranch +"!= "+startBranch);
                    }
                }
            } else if (isDownGoing) {
                if (prevSegment.endBranch > startBranch) {
                    throw new TauModelException(getName()+": Segment is downgoing, but we are already below the start: "+currLeg);
                }
                if (prevSegment.endAction == REFLECT_TOPSIDE || prevSegment.endAction == REFLECT_TOPSIDE_CRITICAL) {
                    throw new TauModelException(getName()+": Segment is downgoing, but previous action was to reflect up: "+currLeg);
                }
                if (prevSegment.endAction == TURN) {
                    throw new TauModelException(getName()+": Segment is downgoing, but previous action was to turn: "+currLeg);
                }
                if (prevSegment.endAction == TRANSUP) {
                    throw new TauModelException(getName()+": Segment is downgoing, but previous action was to transmit up: "+currLeg);
                }
                if (prevSegment.endBranch == startBranch && prevSegment.isDownGoing == false &&
                        ! (prevSegment.endAction == REFLECT_UNDERSIDE || prevSegment.endAction == REFLECT_UNDERSIDE_CRITICAL)) {
                    throw new TauModelException(getName()+": Segment "+currLeg+" is downgoing, but previous action was not to reflect underside: "+currLeg+" "+endActionString(prevSegment.endAction));
                }
            } else {
                if (prevSegment.endBranch < startBranch) {
                    throw new TauModelException(getName()+": Segment is upgoing, but we are already above the start: "+currLeg);
                }
                if (prevSegment.endAction == REFLECT_UNDERSIDE || prevSegment.endAction == REFLECT_UNDERSIDE_CRITICAL) {
                    throw new TauModelException(getName()+": Segment is upgoing, but previous action was to underside reflect down: "+currLeg);
                }
                if (prevSegment.endAction == TRANSDOWN) {
                    throw new TauModelException(getName()+": Segment is upgoing, but previous action was  to trans down: "+currLeg);
                }
                if (prevSegment.endBranch == startBranch && prevSegment.isDownGoing == true
                        && ! ( prevSegment.endAction == TURN || prevSegment.endAction == DIFFRACT || prevSegment.endAction == HEAD || prevSegment.endAction == REFLECT_TOPSIDE || prevSegment.endAction == REFLECT_TOPSIDE_CRITICAL)) {
                    throw new TauModelException(getName()+": Segment is upgoing, but previous action was not to reflect topside: "+currLeg+" "+endActionString(prevSegment.endAction));
                }
            }
        }
        if ( ! isPWave &&  ! (currLeg.startsWith("K") || currLeg.equals("k"))) {
            // outer core K is treated as S wave as special case
            for(int i = Math.min(startBranch, endBranch); i <= Math.max(startBranch,endBranch); i++) {
                TauBranch tb = tMod.getTauBranch(i, isPWave);
                for (DepthRange fluidDR : tMod.getSlownessModel().fluidLayerDepths) {
                    if (tb.getTopDepth() >= fluidDR.topDepth && tb.getTopDepth() < fluidDR.botDepth
                            || tb.getBotDepth() > fluidDR.topDepth && tb.getBotDepth() <= fluidDR.botDepth) {
                        throw new TauModelException("S wave branch in "+getName()+" is in fluid: "+tb+" "+fluidDR);
                    }
                }
            }
        }
        segmentList.add(segment);

        if(isDownGoing) {
            if (startBranch > endBranch) {
                // can't be downgoing as we are already below
                minRayParam = -1;
                maxRayParam = -1;
                throw new TauModelException("can't be downgoing as we are already below: "+startBranch+" "+endBranch+" in "+getName());
            } else {
                /* Must be downgoing, so use i++. */
                for(int i = startBranch; i <= endBranch; i++) {
                    branchSeq.add(i);
                    downGoing.add(isDownGoing);
                    waveType.add(isPWave);
                    legAction.add(endAction);
                }
                if(DEBUG) {
                    for(int i = startBranch; i <= endBranch; i++) {
                        System.out.println("i=" + i + " isDownGoing=" + isDownGoing
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
                throw new TauModelException("can't be upgoing as we are already above: "+startBranch+" "+endBranch+" "+currLeg+" in "+getName());
            } else {
                /* Must be up going so use i--. */
                for(int i = startBranch; i >= endBranch; i--) {
                    branchSeq.add(i);
                    downGoing.add(isDownGoing);
                    waveType.add(isPWave);
                    legAction.add(endAction);
                }
                if(DEBUG) {
                    for(int i = startBranch; i >= endBranch; i--) {
                        System.out.println("i=" + i + " isDownGoing=" + isDownGoing
                                + " isPWave=" + isPWave + " startBranch="
                                + startBranch + " endBranch=" + endBranch + " "
                                + endActionString(endAction));
                    }
                }
            }
        }
        currBranch = endBranch + endOffset;
        if(DEBUG) {
            System.out.println("after addToBranch: minRP="+minRayParam+"  maxRP="+maxRayParam+" endOffset="+endOffset+" isDownGoing="+isDownGoing);
        }
        return segment;
    }


    protected SeismicPhaseSegment addFlatBranch(TauModel tMod,
                                                int branch,
                                                boolean isPWave,
                                                PhaseInteraction prevEndAction,
                                                PhaseInteraction endAction,
                                                String currLeg) throws TauModelException {
        // special case, add "flat" segment along bounday

        if(DEBUG) {
            System.out.println("before addFlatBranch: minRP="+minRayParam+"  maxRP="+maxRayParam);
            System.out.println("addFlatBranch( " + branch
                    + " endAction="+endActionString(endAction)+" "+currLeg+") isP:"+(isPWave?"P":"S"));

        }
        boolean flatIsDownGoing = false;
        SeismicPhaseSegment flatSegment;
        if (prevEndAction == HEAD) {
            flatSegment = new SeismicPhaseSegment(tMod, branch, branch, isPWave, endAction, flatIsDownGoing, currLeg);
            double headRP = tMod.getTauBranch(branch,isPWave).getMaxRayParam();
            if (minRayParam > headRP || maxRayParam < headRP) {
                // can't do head wave, no rp match
                minRayParam = -1;
                maxRayParam = -1;
            } else {
                minRayParam = headRP;
                maxRayParam = headRP;
            }
        } else if (prevEndAction == DIFFRACT){
            flatSegment = new SeismicPhaseSegment(tMod, branch, branch, isPWave, endAction, flatIsDownGoing, currLeg);
            double diffRP = tMod.getTauBranch(branch,isPWave).getMinTurnRayParam();
            if (minRayParam > diffRP || maxRayParam < diffRP) {
                // can't do diff wave, no rp match
                minRayParam = -1;
                maxRayParam = -1;
            } else {
                minRayParam = diffRP;
                maxRayParam = diffRP;
            }
        } else if (prevEndAction == KMPS && currLeg.endsWith("kmps")){
            // dummy case for surface wave velocity
            flatSegment = new SeismicPhaseSegment(tMod, branch, branch, isPWave, endAction, flatIsDownGoing, currLeg);
            double velocity = Double.valueOf(currLeg.substring(0, currLeg.length() - 4))
                    .doubleValue();
            minRayParam = tMod.radiusOfEarth / velocity;
            maxRayParam = minRayParam;
        } else {
            throw new TauModelException("Cannot addFlatBranch for prevEndAction: "+prevEndAction+" for "+currLeg);
        }
        flatSegment.isFlat = true;
        flatSegment.prevEndAction = prevEndAction;
        segmentList.add(flatSegment);
        headOrDiffractSeq.add(branchSeq.size() - 1);
        return flatSegment;
    }


    /**
     * Calculates how many times the phase passes through a branch, up or down,
     * so that we can just multiply instead of doing the ray calc for each time.
     * @return
     */
    protected static int[][] calcBranchMultiplier(TauModel tMod, List<Integer> branchSeq, List<Boolean> waveType) {
        /* initialize the counter for each branch to 0. 0 is P and 1 is S. */
        int[][] timesBranches = new int[2][tMod.getNumBranches()];
        for(int i = 0; i < timesBranches[0].length; i++) {
            timesBranches[0][i] = 0;
            timesBranches[1][i] = 0;
        }
        /* Count how many times each branch appears in the path. */
        for(int i = 0; i < branchSeq.size(); i++) {
            if(((Boolean)waveType.get(i)).booleanValue()) {
                timesBranches[0][((Integer)branchSeq.get(i)).intValue()]++;
            } else {
                timesBranches[1][((Integer)branchSeq.get(i)).intValue()]++;
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
    protected void sumBranches(TauModel tMod) throws TauModelException {
        if(maxRayParam < 0.0 || minRayParam > maxRayParam) {
            /* Phase has no arrivals, possibly due to source depth. */
            rayParams = new double[0];
            minRayParam = -1;
            maxRayParam = -1;
            dist = new double[0];
            time = new double[0];
            maxDistance = -1;
            return;
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
            downGoing.add(true);
            return;
            } catch (NumberFormatException e) {
                throw new TauModelException(getName()+" Illegal surface wave velocity "+name.substring(0, name.length() - 4), e);
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
            throw new RuntimeException(getName()+" Should not happen, did not find max ray param"+maxRayParam);
        }

        if(minRayParamIndex < 0) {
            throw new RuntimeException(getName()+" Should not happen, did not find min ray param"+minRayParam);
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
            if(DEBUG) {
                System.out.println("SumBranches() maxRayParamIndex=" + maxRayParamIndex
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
        int[][] timesBranches = calcBranchMultiplier(tMod, branchSeq, waveType);
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
        int numHead = 0;
        int numDiff = 0;
        for (SeismicPhaseSegment seg : segmentList) {
            if (seg.prevEndAction.equals(DIFFRACT)) {
                numDiff++;
            } else if (seg.prevEndAction.equals(HEAD)) {
                numHead++;
            }
        }
        if (numDiff>0 || numHead>0) {
            // proportionally share head/diff, although this probably can't actually happen in a single ray
            // and will usually be either refraction or diffraction
            double horizontalDistDeg = numHead/(numHead+numDiff) * getMaxRefraction() + numDiff/(numHead+numDiff)*getMaxDiffraction();
            dist[1] = dist[0] + horizontalDistDeg * Math.PI / 180.0;
            time[1] = time[0] + horizontalDistDeg * Math.PI / 180.0 * minRayParam;
        } else if(maxRayParamIndex == minRayParamIndex) {
            System.out.println("one ray param but no head/diff phases");
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
                    for(int legNum = 0; legNum < branchSeq.size(); legNum++) {
                        // check for downgoing legs that cross the high slowness
                        // zone
                        // with the same wave type
                        if(((Integer)branchSeq.get(legNum)).intValue() == branchNum
                                && ((Boolean)waveType.get(legNum)).booleanValue() == isPWave
                                && ((Boolean)downGoing.get(legNum)).booleanValue() == true
                                && ((Integer)branchSeq.get(legNum - 1)).intValue() == branchNum - 1
                                && ((Boolean)waveType.get(legNum - 1)).booleanValue() == isPWave
                                && ((Boolean)downGoing.get(legNum - 1)).booleanValue() == true) {
                            foundOverlap = true;
                            break;
                        }
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

    public static final String endActionString(PhaseInteraction endAction) {
        if(endAction == START) {
            return "START";
        } else if(endAction == TURN) {
            return "TURN";
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
            return "END";
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
        } else if(endAction == HEAD) {
            return "HEAD WAVE";
        } else if(endAction == FAIL) {
            return "FAIL";
        } else {
            throw new RuntimeException("UNKNOWN Action: "+endAction);
        }
    }


}
