package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.PhaseInteraction.*;
import static edu.sc.seis.TauP.PhaseInteraction.REFLECT_TOPSIDE_CRITICAL;

public class SeismicPhaseFactory {

    /** Enables phases originating in core. */
    public static transient boolean expert = TauP_Time.expert;

    boolean DEBUG;
    String name;
    double sourceDepth;
    double receiverDepth;
    TauModel tMod;
    ArrayList<String> legs;
    String puristName;


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

    public static final boolean PWAVE = SeismicPhase.PWAVE;

    public static final boolean SWAVE = SeismicPhase.SWAVE;

    public SeismicPhaseFactory() {

    }

    public SeismicPhase createPhase(String name, TauModel tMod, double receiverDepth, boolean debug) throws TauModelException {
        this.DEBUG = debug ;
        if (name == null || name.length() == 0) {
            throw new TauModelException("Phase name cannot be empty to null: "+name);
        }
        this.name = name;
        this.sourceDepth = tMod.getSourceDepth();
        this.receiverDepth = receiverDepth;
        // make sure we have layer boundary at receiver
        // this does nothing if already split at receiver
        this.tMod = tMod.splitBranch(receiverDepth);
        legs = LegPuller.legPuller(name);
        this.puristName = LegPuller.createPuristName(tMod, legs);
        parseName(tMod);
        SeismicPhase phase = new SeismicPhase(name, tMod, receiverDepth, legs, puristName, debug);
        phase.minRayParam = minRayParam;
        phase.maxRayParam = maxRayParam;
        phase.minRayParamIndex = minRayParamIndex;
        phase.maxRayParamIndex = maxRayParamIndex;
        phase.branchSeq = branchSeq;
        phase.headOrDiffractSeq = headOrDiffractSeq;
        phase.segmentList = segmentList;
        phase.legAction = legAction;
        phase.downGoing = downGoing;
        phase.waveType = waveType;
        phase.sumBranches(tMod);
        return phase;
    }

    public String getName() {
        return name;
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
        boolean isPWavePrev = isPWave;
        PhaseInteraction endAction = TRANSDOWN;
        /*
         * Deal with surface wave velocities first, since they are a special
         * case.
         */
        if(legs.size() == 2 && currLeg.endsWith("kmps")) {
            return;
        }
        /* Make a check for J legs if the model doesn not allow J */
        if(name.indexOf('J') != -1
                && !tMod.getSlownessModel().isAllowInnerCoreS()) {
            throw new TauModelException(getName()+" 'J' phases were not created for this model: "
                    + tMod.getModelName());
        }
        /* set currWave to be the wave type for this leg, 'P' or 'S'. */
        if(currLeg.equals("p") || currLeg.startsWith("P")
                || currLeg.equals("K") || currLeg.equals("k")
                || currLeg.equals("I")) {
            isPWave = PWAVE;
            isPWavePrev = isPWave;
        } else if(currLeg.equals("s") || currLeg.startsWith("S")
                || currLeg.equals("J") || currLeg.equals("j")) {
            isPWave = SWAVE;
            isPWavePrev = isPWave;
        } else {
            throw new TauModelException(getName()+" Unknown starting phase: "+currLeg);
        }
        // where we end up, depending on if we end going down or up
        int upgoingRecBranch = tMod.findBranch(receiverDepth);
        int downgoingRecBranch = upgoingRecBranch-1; // one branch shallower
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
                maxRayParam = minRayParam = -1;
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
                || (expert && (currLeg.startsWith("K") || currLeg.startsWith("I") || currLeg.startsWith("J")))) {
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
                int sLayerNum = tMod.getSlownessModel().layerNumberBelow(tMod.getSourceDepth(), isPWavePrev);
                maxRayParam = tMod.getSlownessModel().getSlownessLayer(sLayerNum, isPWavePrev).getTopP();
            } catch(NoSuchLayerException e) {
                throw new RuntimeException("Should not happen", e);
            }
            maxRayParam = tMod.getTauBranch(tMod.getSourceBranch(),
                    isPWave).getMaxRayParam();
        } else if(currLeg.equals("p") || currLeg.equals("s")
                || (expert && currLeg.startsWith("k"))) {
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
            } else if ((currLeg.startsWith("k")) && (tMod.getSourceDepth() < tMod.getCmbDepth() || tMod.getSourceDepth() > tMod.getIocbDepth() )) {
                // not possible
                maxRayParam = -1;
                minRayParam = -1;
                if(DEBUG) {
                    System.out.println("Source must be in outer core for "
                            + currLeg + " within phase " + name);
                }
                return;
            } else if ((currLeg.startsWith("Ieu") || currLeg.startsWith("j")) && (tMod.getSourceDepth() < tMod.getIocbDepth() )) {
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
                int sLayerNum = tMod.getSlownessModel().layerNumberAbove(tMod.getSourceDepth(), isPWavePrev);
                maxRayParam = tMod.getSlownessModel().getSlownessLayer(sLayerNum, isPWavePrev).getBotP();
                // check if source is in high slowness zone
                DepthRange highSZoneDepth = new DepthRange();
                if (tMod.getSlownessModel().depthInHighSlowness(tMod.getSourceDepth(), maxRayParam, highSZoneDepth, isPWavePrev)) {
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
                return;
            }
        } else {
            throw new TauModelException(getName()+" First phase not recognized: "
                    +currLeg
                    + " Must be one of P, Pg, Pn, Pdiff, p, Ped or the S equivalents");
        }
        if (receiverDepth != 0) {
            if (legs.get(legs.size()-2).equals("Ped") || legs.get(legs.size()-2).equals("Sed")) {
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
        int disconBranch = 0;
        double nextLegDepth = 0.0;
        boolean isLegDepth, isNextLegDepth = false;
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
                System.out.println(legNum + "  " + prevLeg + "  cur=" + currLeg
                        + "  " + nextLeg);
            }
            if (currLeg.contentEquals("END")) {
                if (segmentList.size() > 0) {
                    segmentList.get(segmentList.size()-1).endAction = END;
                    continue;
                }
            }
            isLegDepth = isNextLegDepth;
            // find out if the next leg represents a phase conversion depth
            try {
                nextLegDepth = Double.parseDouble(nextLeg);
                isNextLegDepth = true;
            } catch(NumberFormatException e) {
                nextLegDepth = -1;
                isNextLegDepth = false;
            }
            /* set currWave to be the wave type for this leg, 'P' or 'S'. */
            isPWavePrev = isPWave;
            if(currLeg.equals("p") || currLeg.startsWith("P")
                    || currLeg.equals("k") || currLeg.equals("I")) {
                isPWave = PWAVE;
            } else if(currLeg.equals("s") || currLeg.startsWith("S")
                    || currLeg.equals("J")) {
                isPWave = SWAVE;
            } else if(currLeg.equals("K")) {
                /*
                 * here we want to use whatever isPWave was on the last leg so
                 * do nothing. This makes sure we us the correct maxRayParam
                 * from the correct TauBranch within the outer core. In other
                 * words K has a high slowness zone if it entered the outer core
                 * as a mantle P wave, but doesn't if it entered as a mantle S
                 * wave. It shouldn't matter for inner core to outer core type
                 * legs.
                 */
            }
            // check to see if there has been a phase conversion
            if(branchSeq.size() > 0 && isPWavePrev != isPWave) {
                phaseConversion(tMod,
                        ((Integer)branchSeq.get(branchSeq.size() - 1)).intValue(),
                        endAction,
                        isPWavePrev);
            }
            if (currLeg.equals("Ped") || currLeg.equals("Sed")) {
                if(nextLeg.equals("END")) {
                    if (receiverDepth > 0) {
                        endAction = END_DOWN;
                        addToBranch(tMod, currBranch, downgoingRecBranch, isPWave, endAction, currLeg);
                    } else {
                        //this should be impossible except for 0 dist 0 source depth which can be called p or P
                        maxRayParam = -1;
                        minRayParam = -1;
                        return;
                    }

                } else if(nextLeg.equals("Pdiff") || nextLeg.equals("Sdiff")) {
                    endAction = DIFFRACT;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getCmbBranch() - 1,
                            isPWave,
                            endAction,
                            currLeg);
                    if (currLeg.charAt(0) != nextLeg.charAt(0)) {
                        // like Sed Pdiff conversion
                        isPWave = ! isPWave;
                    }
                } else if(nextLeg.equals("K")) {
                    endAction = TRANSDOWN;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getCmbBranch() - 1,
                            isPWave,
                            endAction,
                            currLeg);
                } else if(nextLeg.equals("m")) {
                    endAction = TRANSDOWN;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getMohoBranch() - 1,
                            isPWave,
                            endAction,
                            currLeg);
                } else if( isLegDepth) {
                    disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
                    endAction = TRANSDOWN;
                    addToBranch(tMod,
                            currBranch,
                            disconBranch - 1,
                            isPWave,
                            endAction,
                            currLeg);
                } else if(nextLeg.equals("c") || nextLeg.equals("i")) {
                    disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
                    endAction = REFLECT_TOPSIDE;
                    addToBranch(tMod,
                            currBranch,
                            disconBranch - 1,
                            isPWave,
                            endAction,
                            currLeg);
                } else if(nextLeg.startsWith("v") || nextLeg.startsWith("V") ) {
                    if (nextLeg.startsWith("V")) {
                        endAction = REFLECT_TOPSIDE_CRITICAL;
                    } else {
                        endAction = REFLECT_TOPSIDE;
                    }
                    disconBranch = LegPuller.closestBranchToDepth(tMod,
                            nextLeg.substring(1));
                    if(currBranch <= disconBranch - 1) {
                        addToBranch(tMod,
                                currBranch,
                                disconBranch - 1,
                                isPWave,
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
                                endAction,
                                currLeg);
                    } else {
                        maxRayParam = -1;
                        if(DEBUG) {System.out.println("P or S followed by I or J can only exist if model has no outer core");}
                        return;
                    }
                } else {
                    throw new TauModelException(getName()+" Phase not recognized (1): "
                            + currLeg + " followed by " + nextLeg);
                }
                /* Deal with p and s case . */
            } else if(currLeg.equals("p") || currLeg.equals("s")
                    || currLeg.equals("k")) {
                if(nextLeg.startsWith("v") || nextLeg.startsWith("V")) {
                    throw new TauModelException(getName()+" p and s and k must always be up going "
                            + " and cannot come immediately before a top-side reflection."
                            + " currLeg=" + currLeg + " nextLeg=" + nextLeg);
                } else if(nextLeg.startsWith("^")) {
                    String depthString;
                    depthString = currLeg.substring(1);
                    endAction = REFLECT_UNDERSIDE;
                    disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
                    if(currBranch >= disconBranch) {
                        addToBranch(tMod,
                                currBranch,
                                disconBranch,
                                isPWave,
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
                            endAction,
                            currLeg);
                } else if(nextLeg.startsWith("P") || nextLeg.startsWith("S")
                        || nextLeg.equals("K") || nextLeg.equals("END")) {
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
                            return;
                        }
                    } else if (nextLeg.equals("K")) {
                        disconBranch = tMod.getCmbBranch();
                    } else {
                        disconBranch = 0;
                    }
                    if (currLeg.equals("k") && ! nextLeg.equals("K")) {
                        endAction = TRANSUP;
                    } else if (nextLeg.equals("END")) {
                        endAction = END;
                    } else {
                        endAction = REFLECT_UNDERSIDE;
                    }
                    addToBranch(tMod,
                            currBranch,
                            disconBranch,
                            isPWave,
                            endAction,
                            currLeg);
                } else if(isNextLegDepth) {
                    disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
                    endAction = TRANSUP;
                    addToBranch(tMod,
                            currBranch,
                            disconBranch,
                            isPWave,
                            endAction,
                            currLeg);
                } else {
                    throw new TauModelException(getName()+" Phase not recognized (3): "
                            + currLeg + " followed by " + nextLeg);
                }
                /* Now deal with P and S case. */
            } else if(currLeg.equals("P") || currLeg.equals("S")) {
                if(nextLeg.equals("P") || nextLeg.equals("S")
                        || nextLeg.equals("Pn") || nextLeg.equals("Sn")
                        || nextLeg.equals("END")) {
                    if(endAction == TRANSDOWN || endAction == REFLECT_UNDERSIDE|| endAction == REFLECT_UNDERSIDE_CRITICAL) {
                        // was downgoing, so must first turn in mantle
                        addToBranch(tMod,
                                currBranch,
                                tMod.getCmbBranch() - 1,
                                isPWave,
                                TURN,
                                currLeg);
                    }
                    if (nextLeg.equals("END")) {
                        endAction = END;
                        addToBranch(tMod, currBranch, upgoingRecBranch, isPWave, END, currLeg);
                    } else {
                        endAction = REFLECT_UNDERSIDE;
                        addToBranch(tMod, currBranch, 0, isPWave, endAction, currLeg);
                    }
                } else if(nextLeg.startsWith("v") || nextLeg.startsWith("V") ) {
                    if (nextLeg.startsWith("V")) {
                        endAction = REFLECT_TOPSIDE_CRITICAL;
                    } else {
                        endAction = REFLECT_TOPSIDE;
                    }
                    disconBranch = LegPuller.closestBranchToDepth(tMod,
                            nextLeg.substring(1));
                    if(currBranch <= disconBranch - 1) {
                        addToBranch(tMod,
                                currBranch,
                                disconBranch - 1,
                                isPWave,
                                endAction,
                                currLeg);
                    } else {
                        // can't topside reflect if already below, setting maxRayParam forces no arrivals
                        maxRayParam = -1;
                    }
                } else if(nextLeg.startsWith("^")) {
                    String depthString;
                    depthString = nextLeg.substring(1);
                    endAction = REFLECT_UNDERSIDE;
                    disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
                    if (disconBranch == tMod.getNumBranches()) {
                        maxRayParam = -1;
                        if(DEBUG) {System.out.println("Attempt to underside reflect from center of earth: "+nextLeg);}
                        return;
                    }
                    if(prevLeg.equals("K")) {
                        addToBranch(tMod,
                                currBranch,
                                disconBranch,
                                isPWave,
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
                                TURN,
                                currLeg);
                        addToBranch(tMod,
                                currBranch,
                                disconBranch,
                                isPWave,
                                endAction,
                                currLeg);
                    } else if(((prevLeg.startsWith("v") || prevLeg.startsWith("V"))
                            && disconBranch < LegPuller.closestBranchToDepth(tMod, prevLeg.substring(1)))
                            || (prevLeg.equals("m") && disconBranch < tMod.getMohoBranch())
                            || (prevLeg.equals("c") && disconBranch < tMod.getCmbBranch())) {
                        if (disconBranch == tMod.getNumBranches()) {
                            maxRayParam = -1;
                            if(DEBUG) {System.out.println("Attempt to reflect from center of earth: "+nextLeg);}
                            return;
                        }
                        addToBranch(tMod,
                                currBranch,
                                disconBranch,
                                isPWave,
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
                        return;
                    }
                    endAction = REFLECT_TOPSIDE;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getCmbBranch() - 1,
                            isPWave,
                            endAction,
                            currLeg);
                } else if(nextLeg.equals("K") && prevLeg.equals("K")) {
                    throw new TauModelException(getName()+" Phase not recognized (5.5): "
                            + currLeg + " followed by " + nextLeg
                            + " and preceeded by "+prevLeg
                            + " when currBranch=" + currBranch
                            + " > disconBranch=" + disconBranch);
                } else if(nextLeg.equals("K")) {
                    endAction = TRANSDOWN;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getCmbBranch() - 1,
                            isPWave,
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
                    disconBranch = LegPuller.closestBranchToDepth(tMod, nextLeg);
                    if(DEBUG) {
                        System.out.println("DisconBranch=" + disconBranch
                                + " for " + nextLeg);
                        System.out.println(tMod.getTauBranch(disconBranch,
                                        isPWave)
                                .getTopDepth());
                    }
                    if(endAction == TURN || endAction == REFLECT_TOPSIDE
                            || endAction == REFLECT_TOPSIDE_CRITICAL || endAction == TRANSUP) {
                        // upgoing section
                        if(disconBranch > currBranch) {
                            // check for discontinuity below the current branch
                            // when the ray should be upgoing
                            throw new TauModelException(getName()+" Phase not recognized (6): "
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
                                endAction,
                                currLeg);
                    } else {
                        // downgoing section, must look at the leg after the
                        // next
                        // leg to determine whether to convert on the downgoing
                        // or
                        // upgoing part of the path
                        String nextNextLeg = (String)legs.get(legNum + 2);
                        if(nextNextLeg.equals("p") || nextNextLeg.equals("s")) {
                            // convert on upgoing section
                            endAction = TURN;
                            addToBranch(tMod,
                                    currBranch,
                                    tMod.getCmbBranch() - 1,
                                    isPWave,
                                    endAction,
                                    currLeg);
                            endAction = TRANSUP;
                            addToBranch(tMod,
                                    currBranch,
                                    disconBranch,
                                    isPWave,
                                    endAction,
                                    currLeg);
                        } else if(nextNextLeg.equals("P")
                                || nextNextLeg.equals("S")) {
                            if(disconBranch > currBranch) {
                                // discon is below current loc
                                endAction = TRANSDOWN;
                                addToBranch(tMod,
                                        currBranch,
                                        disconBranch - 1,
                                        isPWave,
                                        endAction,
                                        currLeg);
                            } else {
                                // discon is above current loc, but we have a
                                // downgoing ray, so this is an illegal ray for
                                // this source depth
                                maxRayParam = -1;
                                if(DEBUG) {
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
                                return;
                            }
                        } else {
                            throw new TauModelException(getName()+" Phase not recognized (7): "
                                    + currLeg
                                    + " followed by "
                                    + nextLeg
                                    + " followed by " + nextNextLeg);
                        }
                    }
                } else {
                    throw new TauModelException(getName()+" Phase not recognized (8): "
                            + currLeg + " followed by " + nextLeg);
                }
            } else if(currLeg.startsWith("P") || currLeg.startsWith("S")) {
                if(currLeg.equals("Pdiff") || currLeg.equals("Sdiff")) {
                    /*
                     * in the diffracted case we trick addToBranch into thinking
                     * we are turning, but then make the maxRayParam equal to
                     * minRayParam, which is the deepest turning ray.
                     */
                    if(maxRayParam >= tMod.getTauBranch(tMod.getCmbBranch() - 1,
                                    isPWave)
                            .getMinTurnRayParam()
                            && minRayParam <= tMod.getTauBranch(tMod.getCmbBranch() - 1,
                                    isPWave)
                            .getMinTurnRayParam()) {

                        if (currBranch < tMod.getCmbBranch() - 1
                                || (currBranch == tMod.getCmbBranch() - 1 && endAction != DIFFRACT)
                                || (currBranch == tMod.getCmbBranch() && endAction != TRANSUP)) {
                            endAction = DIFFRACT;
                            addToBranch(tMod,
                                    currBranch,
                                    tMod.getCmbBranch() - 1,
                                    isPWave,
                                    endAction,
                                    currLeg);
                        } // otherwise we are already at the right branch to diffract
                        // remember where the diff or head happened (one less than size)
                        headOrDiffractSeq.add(branchSeq.size() - 1);
                        maxRayParam = tMod.getTauBranch(tMod.getCmbBranch() - 1,
                                        isPWave)
                                .getMinTurnRayParam();
                        minRayParam = maxRayParam;
                        if(nextLeg.equals("END")) {
                            endAction = END;
                            addToBranch(tMod,
                                    currBranch,
                                    upgoingRecBranch,
                                    isPWave,
                                    endAction,
                                    currLeg);
                        } else if (nextLeg.equals("K")) {
                            endAction = TRANSDOWN;
                            currBranch++;
                        } else if (nextLeg.startsWith("P") || nextLeg.startsWith("S")) {
                            endAction = REFLECT_UNDERSIDE;
                            addToBranch(tMod,
                                    currBranch,
                                    0,
                                    isPWave,
                                    endAction,
                                    currLeg);
                        } else {
                            throw new TauModelException(getName()+" Phase not recognized (12): "
                                    + currLeg + " followed by " + nextLeg
                                    + " when currBranch=" + currBranch);
                        }
                    } else {
                        // can't have head wave as ray param is not within range
                        if(DEBUG) {
                            System.out.println("Cannot have the head wave "
                                    + currLeg + " within phase " + name
                                    + " for this sourceDepth and/or path.");
                            System.out.println(maxRayParam+" >= "+tMod.getTauBranch(tMod.getCmbBranch() - 1,
                                            isPWave)
                                    .getMinTurnRayParam()+" "+
                                    "&& "+minRayParam+" <= "+tMod.getTauBranch(tMod.getCmbBranch() - 1,
                                            isPWave)
                                    .getMinTurnRayParam());
                        }
                        maxRayParam = -1;
                        return;
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
                        return;
                    }
                    if(currLeg.equals("Pg") || currLeg.equals("Sg")) {
                        endAction = TURN;
                        addToBranch(tMod,
                                currBranch,
                                tMod.getMohoBranch() - 1,
                                isPWave,
                                endAction,
                                currLeg);
                        if(nextLeg.equals("END")) {
                            endAction = END;
                        } else {
                            endAction = REFLECT_UNDERSIDE;
                        }
                        addToBranch(tMod, currBranch, upgoingRecBranch, isPWave, endAction, currLeg);
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
                            endAction = TURN;
                            addToBranch(tMod,
                                    currBranch,
                                    tMod.getMohoBranch(),
                                    isPWave,
                                    endAction,
                                    currLeg);
                            headOrDiffractSeq.add(branchSeq.size() - 1);
                            endAction = TRANSUP;
                            addToBranch(tMod,
                                    currBranch,
                                    tMod.getMohoBranch(),
                                    isPWave,
                                    endAction,
                                    currLeg);
                            minRayParam = maxRayParam;
                            if(nextLeg.equals("END")) {
                                endAction = END;
                                if (currBranch >= upgoingRecBranch) {
                                    addToBranch(tMod,
                                            currBranch,
                                            upgoingRecBranch,
                                            isPWave,
                                            endAction,
                                            currLeg);
                                } else {
                                    maxRayParam = -1;
                                    if(DEBUG) {
                                        System.out.println("Cannot have the head wave "
                                                + currLeg + " within phase " + name
                                                + " for this sourceDepth, receiverDepth and/or path.");
                                    }
                                    return;
                                }
                            } else if ( nextLeg.startsWith("P") || nextLeg.startsWith("S")) {
                                endAction = REFLECT_UNDERSIDE;
                                addToBranch(tMod,
                                        currBranch,
                                        0,
                                        isPWave,
                                        endAction,
                                        currLeg);
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
                            return;
                        }
                    }
                } else {
                    throw new TauModelException(getName()+" Phase not recognized for P,S: "
                            + currLeg + " followed by " + nextLeg);
                }
            } else if(currLeg.equals("K")) {
                /* Now deal with K. */
                if (tMod.getCmbDepth() == tMod.getRadiusOfEarth()) {
                    // degenerate case, CMB is at center, so model without a core
                    maxRayParam = -1;
                    if(DEBUG) {
                        System.out.println("Cannot have K phase "
                                + currLeg + " within phase " + name
                                + " for this model as it has no core, cmb depth = radius of Earth.");
                    }
                    return;
                }
                if (tMod.getCmbDepth() == tMod.getIocbDepth()) {
                    // degenerate case, CMB is same as IOCB, so model without an outer core
                    maxRayParam = -1;
                    if(DEBUG) {
                        System.out.println("Cannot have K phase "
                                + currLeg + " within phase " + name
                                + " for this model as it has no outer core, cmb depth = iocb depth, "+tMod.getCmbDepth());
                    }
                    return;
                }
                if(nextLeg.equals("P") || nextLeg.equals("S")
                        || nextLeg.equals("p") || nextLeg.equals("s")
                        || nextLeg.equals("Pdiff") || nextLeg.equals("Sdiff")) {
                    if(prevLeg.equals("P") || prevLeg.equals("S")
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
                                endAction,
                                currLeg);
                    }
                    endAction = TRANSUP;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getCmbBranch(),
                            isPWave,
                            endAction,
                            currLeg);
                } else if(nextLeg.equals("K")) {
                    if(prevLeg.equals("P") || prevLeg.equals("S")
                            || prevLeg.equals("K")) {
                        endAction = TURN;
                        addToBranch(tMod,
                                currBranch,
                                tMod.getIocbBranch() - 1,
                                isPWave,
                                endAction,
                                currLeg);
                    }
                    endAction = REFLECT_UNDERSIDE;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getCmbBranch(),
                            isPWave,
                            endAction,
                            currLeg);
                } else if(nextLeg.equals("I") || nextLeg.equals("J")) {
                    endAction = TRANSDOWN;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getIocbBranch() - 1,
                            isPWave,
                            endAction,
                            currLeg);
                } else if(nextLeg.equals("i")) {
                    if (tMod.getIocbBranch() == tMod.getNumBranches()) {
                        maxRayParam = -1;
                        if (DEBUG) {
                            System.out.println("Attempt to reflect from center of earth: " + nextLeg);
                        }
                        return;
                    }
                    endAction = REFLECT_TOPSIDE;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getIocbBranch() - 1,
                            isPWave,
                            endAction,
                            currLeg);
                } else if(nextLeg.startsWith("v") || nextLeg.startsWith("V") ) {
                    if (nextLeg.startsWith("V")) {
                        endAction = REFLECT_TOPSIDE_CRITICAL;
                    } else {
                        endAction = REFLECT_TOPSIDE;
                    }
                    disconBranch = LegPuller.closestBranchToDepth(tMod,
                            nextLeg.substring(1));
                    if(currBranch <= disconBranch - 1) {
                        addToBranch(tMod,
                                currBranch,
                                disconBranch - 1,
                                isPWave,
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
                    disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
                    if (disconBranch < tMod.getCmbBranch()) {
                        throw new TauModelException(getName()+" Phase not recognized (5a): "
                                + currLeg + " followed by " + nextLeg
                                + " when disconBranch=" + disconBranch
                                +" < cmbBranch="+tMod.getCmbBranch()+", likely need P or S leg , prev="+prevLeg);
                    }
                    if (disconBranch >= tMod.getIocbBranch()) {
                        throw new TauModelException(getName()+" Phase not recognized (5b): "
                                + currLeg + " followed by " + nextLeg
                                + " when disconBranch=" + disconBranch
                                +" > iocbBranch="+tMod.getIocbBranch()+", likely need Ior J leg , prev="+prevLeg);
                    }
                    if (disconBranch == tMod.getNumBranches()) {
                        maxRayParam = -1;
                        if(DEBUG) {System.out.println("Attempt to underside reflect from center of earth: "+nextLeg);}
                        return;
                    }
                    if(prevLeg.equals("I") || prevLeg.equals("J")
                            || prevLeg.equals("i") || prevLeg.equals("j")
                            || prevLeg.equals("k")) {
                        // upgoind K leg
                        addToBranch(tMod,
                                currBranch,
                                disconBranch,
                                isPWave,
                                endAction,
                                currLeg);
                    } else if(prevLeg.equals("P") || prevLeg.equals("S") ||
                            prevLeg.startsWith("^") ||
                            prevLeg.equals("K") || prevLeg.equals("START")) {
                        addToBranch(tMod,
                                currBranch,
                                tMod.getIocbBranch() - 1,
                                isPWave,
                                TURN,
                                currLeg);
                        addToBranch(tMod,
                                currBranch,
                                disconBranch,
                                isPWave,
                                endAction,
                                currLeg);
                    } else if(prevLeg.startsWith("v") || prevLeg.startsWith("V") ) {
                        String prevDepthString = prevLeg.substring(1);
                        int prevdisconBranch = LegPuller.closestBranchToDepth(tMod, prevDepthString);
                        if (disconBranch < prevdisconBranch) {
                            // upgoind K leg
                            addToBranch(tMod,
                                    currBranch,
                                    disconBranch,
                                    isPWave,
                                    endAction,
                                    currLeg);
                        } else {
                            // down-turn-up
                            addToBranch(tMod,
                                    currBranch,
                                    tMod.getIocbBranch() - 1,
                                    isPWave,
                                    TURN,
                                    currLeg);
                            addToBranch(tMod,
                                    currBranch,
                                    disconBranch,
                                    isPWave,
                                    endAction,
                                    currLeg);
                        }
                    } else {
                        throw new TauModelException(getName()+" Phase not recognized (5): "
                                + currLeg + " followed by " + nextLeg
                                + " when currBranch=" + currBranch
                                + " > disconBranch=" + disconBranch+" , prev="+prevLeg);
                    }
                } else {
                    throw new TauModelException(getName()+" Phase not recognized (9): "
                            + currLeg + " followed by " + nextLeg);
                }
            } else if(currLeg.equals("I") || currLeg.equals("J")) {
                /* And now consider inner core, I and J. */
                if (tMod.getIocbDepth() == tMod.getRadiusOfEarth()) {
                    // degenerate case, IOCB is at center, so model without a inner core
                    maxRayParam = -1;
                    if(DEBUG) {
                        System.out.println("Cannot have I or J phase "
                                + currLeg + " within phase " + name
                                + " for this model as it has no inner core, iocb depth = radius of Earth.");
                    }
                    return;
                }
                endAction = TURN;
                addToBranch(tMod,
                        currBranch,
                        tMod.getNumBranches() - 1,
                        isPWave,
                        endAction,
                        currLeg);
                if(nextLeg.equals("I") || nextLeg.equals("J")) {
                    endAction = REFLECT_UNDERSIDE;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getIocbBranch(),
                            isPWave,
                            endAction,
                            currLeg);
                } else if(nextLeg.equals("K")) {
                    endAction = TRANSUP;
                    addToBranch(tMod,
                            currBranch,
                            tMod.getIocbBranch(),
                            isPWave,
                            endAction,
                            currLeg);
                } else if(nextLeg.startsWith("^")) {
                    String depthString;
                    depthString = nextLeg.substring(1);
                    endAction = REFLECT_UNDERSIDE;
                    disconBranch = LegPuller.closestBranchToDepth(tMod, depthString);
                    if (disconBranch < tMod.getIocbBranch()) {
                        throw new TauModelException(getName()+" Phase not recognized (6a): "
                                + currLeg + " followed by " + nextLeg
                                + " when disconBranch=" + disconBranch
                                +" < iocbBranch="+tMod.getIocbBranch()+", likely need K leg , prev="+prevLeg);
                    }
                    if (disconBranch == tMod.getNumBranches()) {
                        maxRayParam = -1;
                        if(DEBUG) {System.out.println("Attempt to underside reflect from center of earth: "+nextLeg);}
                        return;
                    }
                    if(prevLeg.equals("K") || prevLeg.equals("I") || prevLeg.equals("J")) {
                        addToBranch(tMod,
                                currBranch,
                                tMod.getNumBranches() - 1,
                                isPWave,
                                TURN,
                                currLeg);
                    } else {
                        throw new TauModelException(getName()+" Phase not recognized (5c): "
                                + currLeg + " followed by " + nextLeg
                                + " when disconBranch=" + disconBranch+" , prev="+prevLeg);
                    }
                } else if(nextLeg.equalsIgnoreCase("P") || nextLeg.equalsIgnoreCase("S")) {
                    if (tMod.getCmbDepth() == tMod.getIocbDepth()) {
                        // degenerate case of no fluid outer core, so allow phases like PIP or SJS
                        endAction = TRANSUP;
                        addToBranch(tMod,
                                currBranch,
                                tMod.getIocbBranch(),
                                isPWave,
                                endAction,
                                currLeg);
                    } else {
                        maxRayParam = -1;
                        throw new TauModelException(getName()+" Cannot have I or J phase "
                                + currLeg
                                + " followed by "+nextLeg
                                + " within phase " + name
                                + " for this model as it has an outer core so need K in between.");
                    }
                }
            } else if(currLeg.equals("m")) {

            } else if(currLeg.equals("c") || currLeg.equals("cx")) {

            } else if(currLeg.equals("i") || currLeg.equals("ix")) {

            } else if(currLeg.startsWith("^")) {

            } else if(currLeg.startsWith("v") || currLeg.startsWith("V")) {
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
                    throw new TauModelException(getName()+" Phase not recognized: "+currLeg
                            + " followed by " + nextLeg+" looks like a upgoing wave from the free surface as closest discontinuity to "+currLeg+" is zero depth.");
                }
            } else {
                throw new TauModelException(getName()+" Phase not recognized (10): " + currLeg
                        + " followed by " + nextLeg);
            }
        }
        if (maxRayParam != -1) {
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
                    System.out.println(SeismicPhase.endActionString(endAction)+" upgoingRecBranch="+upgoingRecBranch+"  bs="+branchSeq.get(branchSeq.size()-1));
                }
                minRayParam = -1;
                maxRayParam = -1;
            } else {
                if (DEBUG) {
                    System.out.println("Last action is: "+SeismicPhase.endActionString(endAction)+" upR="+upgoingRecBranch+" downR="+downgoingRecBranch+" last="+branchSeq.get(branchSeq.size()-1));
                }
            }
        }
    }

    /**
     * changes maxRayParam and minRayParam whenever there is a phase conversion.
     * For instance, SKP needs to change the maxRayParam because there are SKS
     * ray parameters that cannot propagate from the cmb into the mantle as a p
     * wave.
     */
    protected void phaseConversion(TauModel tMod,
                                   int fromBranch,
                                   PhaseInteraction endAction,
                                   boolean isPtoS) throws TauModelException {
        if(endAction == TURN) {
            // can't phase convert for just a turn point
            throw new TauModelException("Illegal endAction: endAction="
                    + endAction
                    + "\nphase conversion are not allowed at turn points.");
        } else if(endAction == REFLECT_UNDERSIDE || endAction == REFLECT_UNDERSIDE_CRITICAL) {
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(fromBranch,
                            isPtoS)
                    .getMaxRayParam());
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(fromBranch,
                            !isPtoS)
                    .getMaxRayParam());
        } else if(endAction == REFLECT_TOPSIDE || endAction == REFLECT_TOPSIDE_CRITICAL) {
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(fromBranch,
                            isPtoS)
                    .getMinTurnRayParam());
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(fromBranch,
                            !isPtoS)
                    .getMinTurnRayParam());
        } else if(endAction == TRANSUP) {
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(fromBranch,
                            isPtoS)
                    .getMaxRayParam());
            maxRayParam = Math.min(maxRayParam,
                    tMod.getTauBranch(fromBranch - 1, !isPtoS)
                            .getMinTurnRayParam());
        } else if(endAction == TRANSDOWN) {
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(fromBranch,
                            isPtoS)
                    .getMinRayParam());
            maxRayParam = Math.min(maxRayParam,
                    tMod.getTauBranch(fromBranch + 1, !isPtoS)
                            .getMaxRayParam());
        } else {
            throw new TauModelException("Illegal endAction: endAction="
                    + endAction);
        }
    }

    /*
     * Adds the branch numbers from startBranch to endBranch, inclusive, to
     * branchSeq, in order. Also, currBranch is set correctly based on the value
     * of endAction. endAction can be one of TRANSUP, TRANSDOWN, REFLECTTOP,
     * REFLECTBOT, or TURN.
     */
    protected void addToBranch(TauModel tMod,
                               int startBranch,
                               int endBranch,
                               boolean isPWave,
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
        int endOffset;
        boolean isDownGoing;
        if(DEBUG) {
            System.out.println("before addToBranch: minRP="+minRayParam+"  maxRP="+maxRayParam);
            System.out.println("addToBranch( start=" + startBranch + " end=" + endBranch
                    + " endAction="+SeismicPhase.endActionString(endAction)+" "+currLeg+")");

        }
        if(endAction == TURN) {
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
                            isPWave)
                    .getMinTurnRayParam());
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
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch,
                            isPWave)
                    .getMaxRayParam());
        } else if(endAction == TRANSDOWN) {
            endOffset = 1;
            isDownGoing = true;
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch,
                            isPWave)
                    .getMinRayParam());
        } else if(endAction == DIFFRACT) {
            endOffset = 0;
            isDownGoing = true;
            maxRayParam = Math.min(maxRayParam, tMod.getTauBranch(endBranch,
                            isPWave)
                    .getMinTurnRayParam());
        } else {
            throw new TauModelException(getName()+": Illegal endAction: endAction="
                    + endAction);
        }
        SeismicPhaseSegment segment = new SeismicPhaseSegment(tMod, startBranch, endBranch, isPWave, endAction, isDownGoing, currLeg);
        if (segmentList.size() > 0) {
            SeismicPhaseSegment prevSegment = segmentList.get(segmentList.size()-1);
            if (isDownGoing) {
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
                    throw new TauModelException(getName()+": Segment "+currLeg+" is downgoing, but previous action was not to reflect underside: "+currLeg+" "+SeismicPhase.endActionString(prevSegment.endAction));
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
                        && ! ( prevSegment.endAction == TURN || prevSegment.endAction == DIFFRACT || prevSegment.endAction == REFLECT_TOPSIDE || prevSegment.endAction == REFLECT_TOPSIDE_CRITICAL)) {
                    throw new TauModelException(getName()+": Segment is upgoing, but previous action was not to reflect topside: "+currLeg+" "+SeismicPhase.endActionString(prevSegment.endAction));
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
                                + SeismicPhase.endActionString(endAction));
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
                                + SeismicPhase.endActionString(endAction));
                    }
                }
            }
        }
        currBranch = endBranch + endOffset;

        if(DEBUG) {
            System.out.println("after addToBranch: minRP="+minRayParam+"  maxRP="+maxRayParam+" endOffset="+endOffset+" isDownGoing="+isDownGoing);
        }

    }

}
