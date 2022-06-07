/*
 * <pre> The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
 * Copyright (C) 1998-2000 University of South Carolina This program is free
 * software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place -
 * Suite 330, Boston, MA 02111-1307, USA. The current version can be found at <A
 * HREF="www.seis.sc.edu">http://www.seis.sc.edu </A> Bug reports and comments
 * should be directed to H. Philip Crotwell, crotwell@seis.sc.edu or Tom Owens,
 * owens@seis.sc.edu </pre>
 */
package edu.sc.seis.TauP;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.sc.seis.TauP.PhaseInteraction.*;

/**
 * Stores and transforms seismic phase names to and from their corresponding
 * sequence of branches.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * @author H. Philip Crotwell
 * 
 * Modified to add "expert" mode wherein paths may start in the core. Principal
 * use is to calculate leg contributions for scattered phases. Nomenclature: "K" -
 * downgoing wave from source in core; "k" - upgoing wave from source in core.
 * 
 * G. Helffrich/U. Bristol 24 Feb. 2007
 */
public class SeismicPhase implements Serializable, Cloneable {

    /** Enables debugging output. */
    public transient boolean DEBUG = ToolRun.DEBUG;

    /** Enables verbose output. */
    public transient boolean verbose = false;

    /** TauModel to generate phase for. */
    protected TauModel tMod;

    /**
     * The source depth within the TauModel that was used to generate this
     * phase.
     */
    protected double sourceDepth;

    /**
     * The receiver depth within the TauModel that was used to generate this
     * phase. Normally this is 0.0 for a surface stations, but can be different
     * for borehole or scattering calculations.
     */
    protected double receiverDepth = 0.0;

    /**
     * Array of distances corresponding to the ray parameters stored in
     * rayParams.
     */
    protected double[] dist = new double[0];

    /**
     * Array of times corresponding to the ray parameters stored in rayParams.
     */
    protected double[] time = new double[0];

    /** Array of possible ray parameters for this phase. */
    protected double[] rayParams = new double[0];

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
     * Array of branch numbers for the given phase. Note that this depends upon
     * both the earth model and the source depth.
     */
    protected List<Integer> branchSeq = new ArrayList<Integer>();


    /**
     * Array of branchSeq positions where a head or diffracted segment occurs.
     */
    protected List<Integer> headOrDiffractSeq = new ArrayList<Integer>();

    /** The phase name, ie PKiKP. */
    protected String name;

    /**
     * name with depths corrected to be actuall discontinuities in the model.
     */
    protected String puristName;

    /** ArrayList containing Strings for each leg. */
    protected ArrayList<String> legs = new ArrayList<String>();

    /** Description of segments of the phase. */
    protected List<SeismicPhaseSegment> segmentList = new ArrayList<SeismicPhaseSegment>();

    /**
     * records the end action for the current leg. Will be one of
     * SeismicPhase.TURN, SeismicPhase.TRANSDOWN, SeismicPhase.TRANSUP,
     * SeismicPhase.REFLECTBOT, or SeismicPhase.REFLECTTOP. This allows a check
     * to make sure the path is correct. Used in addToBranch() and parseName().
     */
    protected List<PhaseInteraction> legAction = new ArrayList<PhaseInteraction>();

    /**
     * true if the current leg of the phase is down going. This allows a check
     * to make sure the path is correct. Used in addToBranch() and parseName().
     */
    protected List<Boolean> downGoing = new ArrayList<Boolean>();

    /**
     * ArrayList of wave types corresponding to each leg of the phase.
     *
     */
    protected List<Boolean> waveType = new ArrayList<Boolean>();
    
    protected double refineDistToleranceRadian = 0.0049*Math.PI/180;
    
    protected int maxRecursion = 5;

    public static final boolean PWAVE = true;

    public static final boolean SWAVE = false;

    /**
     *
     *  @deprecated use SeismicPhaseFactory.create()
     * @param name
     * @param modelName
     * @param depth
     * @throws TauModelException
     */
    @Deprecated
    public SeismicPhase(String name, String modelName, double depth) throws TauModelException {
        this(name, TauModelLoader.load(modelName).depthCorrect(depth));
    }
    /**
     *  @deprecated use SeismicPhaseFactory.create()
     * @param name
     *            String containing a name of the phase.
     * @param tMod
     *            Tau model to be used to construct the phase. This should be corrected for the source
     *            depth.
     * @throws TauModelException 
     */
    @Deprecated
    public SeismicPhase(String name, TauModel tMod) throws TauModelException {
        this(name, tMod, 0.0); //surface receiver
    }

    /**
     *
     *  @deprecated use SeismicPhaseFactory.create()
     * @param name
     * @param tMod
     * @param receiverDepth
     * @throws TauModelException
     */
    @Deprecated
    public SeismicPhase(String name, TauModel tMod, double receiverDepth) throws TauModelException {
    	this(name, tMod, receiverDepth, ToolRun.DEBUG);
    }

    /**
     *  @deprecated use SeismicPhaseFactory.create()
     * @param name
     * @param tMod
     * @param receiverDepth
     * @param debug
     * @throws TauModelException
     */
    @Deprecated
    public SeismicPhase(String name, TauModel tMod, double receiverDepth, boolean debug) throws TauModelException {
        SeismicPhase phase = SeismicPhaseFactory.createPhase(name, tMod, tMod.sourceDepth, receiverDepth, debug);

        this.DEBUG = phase.DEBUG ;
        this.verbose = phase.verbose;
    	this.name = phase.name;
    	this.tMod = phase.tMod;
        this.sourceDepth = phase.sourceDepth;
        this.receiverDepth = phase.receiverDepth;
        this.legs = phase.legs;
        this.puristName = phase.puristName;

        this.minRayParam = phase.minRayParam;
        this.maxRayParam = phase.maxRayParam;
        this.minRayParamIndex = phase.minRayParamIndex;
        this.maxRayParamIndex = phase.maxRayParamIndex;
        this.branchSeq = phase.branchSeq;
        this.headOrDiffractSeq = phase.headOrDiffractSeq;
        this.segmentList = phase.segmentList;
        this.legAction = phase.legAction;
        this.downGoing = phase.downGoing;
        this.waveType = phase.waveType;
        this.minDistance = phase.minDistance;
        this.maxDistance = phase.maxDistance;
        this.dist = phase.dist;
        this.time = phase.time;
        this.rayParams = phase.rayParams;
    }

    public SeismicPhase(String name,
                 TauModel tMod,
                 double receiverDepth,
                 ArrayList<String> legs,
                 String puristName,
                 double[] rayParams,
                 double[] time,
                 double[] dist,
                 double minRayParam,
                 double maxRayParam,
                 int minRayParamIndex,
                 int maxRayParamIndex,
                 double minDistance,
                 double maxDistance,
                 List<Integer> branchSeq,
                 List<Integer> headOrDiffractSeq,
                 List<SeismicPhaseSegment> segmentList,
                 List<PhaseInteraction> legAction,
                 List<Boolean> downGoing,
                 List<Boolean> waveType,
                 boolean debug) throws TauModelException {
        this.DEBUG = debug ;
        this.name = name;
        this.tMod = tMod;
        this.puristName = puristName;
        this.sourceDepth = tMod.getSourceDepth();
        this.receiverDepth = receiverDepth;
        this.legs = legs;
        this.rayParams = rayParams;
        this.time = time;
        this.dist = dist;
        this.minRayParam = minRayParam;
        this.maxRayParam = maxRayParam;
        this.minRayParamIndex = minRayParamIndex;
        this.maxRayParamIndex = maxRayParamIndex;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.branchSeq = branchSeq;
        this.headOrDiffractSeq = headOrDiffractSeq;
        this.segmentList = segmentList;
        this.legAction = legAction;
        this.downGoing = downGoing;
        this.waveType = waveType;

    }


    public boolean phasesExistsInModel() {
        return getMaxRayParam() >= 0;
    }
    
    public Arrival getEarliestArrival(double degrees) {
        double soonest = Double.MAX_VALUE;
        Arrival soonestArrival = null;
        List<Arrival> arrivals = calcTime(degrees);
        for (Arrival a : arrivals) {
            if (a.getTime() < soonest) {
                soonestArrival = a;
                soonest = a.getTime();
            }
        }
        return soonestArrival;
    }

    public TauModel getTauModel() {
        return tMod;
    }

    public double getMinDistanceDeg() {
        return getMinDistance() * 180.0 / Math.PI;
    }
    
    public double getMinDistance() {
        return minDistance;
    }

    public double getMaxDistanceDeg() {
        return getMaxDistance() * 180.0 / Math.PI;
    }
    
    public double getMaxDistance() {
        return maxDistance;
    }

    public double getMaxRayParam() {
        return maxRayParam;
    }

    public double getMinRayParam() {
        return minRayParam;
    }

    public int getMaxRayParamIndex() {
        return maxRayParamIndex;
    }

    public int getMinRayParamIndex() {
        return minRayParamIndex;
    }

    /**
     * @deprecated see SeismicPhaseFactory
     * @return max refractions distance for head waves
     */
    public static double getMaxRefraction() {
        return SeismicPhaseFactory.getMaxRefraction();
    }

    /**
     * set max refractions distance for head waves
     * @deprecated see SeismicPhaseFactory
     */
    public static void setMaxRefraction(double max) {
        SeismicPhaseFactory.setMaxRefraction(max);
    }

    /**
     * @deprecated see SeismicPhaseFactory
     * @return max diffraction distance for diff waves
     */
    public static double getMaxDiffraction() {
        return SeismicPhaseFactory.getMaxDiffraction();
    }

    /**
     * set max diffraction distance for diff waves
     * @deprecated see SeismicPhaseFactory
     */
    public static void setMaxDiffraction(double max) {
        SeismicPhaseFactory.setMaxDiffraction(max);
    }

    public String getName() {
        return name;
    }

    public String getPuristName() {
        return name;
    }

    public List<String> getLegs() {
        return Collections.unmodifiableList(legs);
    }
    
    public List<SeismicPhaseSegment> getPhaseSegments() {
    	return Collections.unmodifiableList(segmentList);
    }

    public double getRayParams(int i) {
        return rayParams[i];
    }

    public double[] getRayParams() {
        return (double[])rayParams.clone();
    }

    public double getDist(int i) {
        return dist[i];
    }
    
    public double[] getDist() {
        return (double[])dist.clone();
    }

    public double getTime(int i) {
        return time[i];
    }
    
    public double[] getTime() {
        return (double[])time.clone();
    }

    public double getTau(int i) {
        return time[i] - rayParams[i] * dist[i];
    }
    
    public double[] getTau() {
        double[] tau = new double[dist.length];
        for(int i = 0; i < dist.length; i++) {
            tau[i] = time[i] - rayParams[i] * dist[i];
        }
        return tau;
    }

    /**
     * Direction of the leg between pierce point i and i+1, true is downgoing,
     * false if upgoing
     */
    public boolean[] getDownGoing() {
        Boolean[] b = (Boolean[])downGoing.toArray(new Boolean[0]);
        boolean[] out = new boolean[b.length];
        for(int i = 0; i < b.length; i++) {
            out[i] = b[i].booleanValue();
        }
        return out;
    }

    /**
     * Wave type of the leg between pierce point i and i+1, true is P, false if
     * S
     */
    public boolean[] getWaveType() {
        Boolean[] b = (Boolean[])waveType.toArray(new Boolean[0]);
        boolean[] out = new boolean[b.length];
        for(int i = 0; i < b.length; i++) {
            out[i] = b[i].booleanValue();
        }
        return out;
    }

    /**
     * Leg type i layer interaction, one of TURN, REFLECTTOP, REFLECTBOT,
     * TRANSUP, TRANSDOWN
     */
    public int[] getLegAction() {
        Integer[] b = (Integer[])legAction.toArray(new Integer[0]);
        int[] out = new int[b.length];
        for(int i = 0; i < b.length; i++) {
            out[i] = b[i].intValue();
        }
        return out;
    }
    
    public boolean hasArrivals() {
        return dist != null && dist.length != 0;
    }

    // Normal methods

    /** calculates arrival times for this phase, sorted by time. 
     *  */
    public List<Arrival> calcTime(double deg) {
        double tempDeg = deg;
        if(tempDeg < 0.0) {
            tempDeg *= -1.0;
        } // make sure deg is positive
        while(tempDeg > 360.0) {
            tempDeg -= 360.0;
        } // make sure it is less than 360
        if(tempDeg > 180.0) {
            tempDeg = 360.0 - tempDeg;
        } // make sure less than or equal to 180
        // now we have 0.0 <= deg <= 180
        double radDist = tempDeg * Math.PI / 180.0;
        List<Arrival> arrivals = new ArrayList<Arrival>();
        /*
         * Search all distances 2n*PI+radDist and 2(n+1)*PI-radDist that are
         * less than the maximum distance for this phase. This insures that we
         * get the time for phases that accumulate more than 180 degrees of
         * distance, for instance PKKKKP might wrap all of the way around. A
         * special case exists at 180, so we skip the second case if
         * tempDeg==180.
         */
        int n = 0;
        double searchDist;
        while(n * 2.0 * Math.PI + radDist <= maxDistance) {
            /*
             * Look for arrivals that are radDist + 2nPi, ie rays that have done
             * more than n laps.
             */
            searchDist = n * 2.0 * Math.PI + radDist;
            for(int rayNum = 0; rayNum < (dist.length - 1); rayNum++) {
                if(searchDist == dist[rayNum + 1]
                        && rayNum + 1 != dist.length - 1) {
                    /* So we don't get 2 arrivals for the same ray. */
                    continue;
                } else if((dist[rayNum] - searchDist)
                        * (searchDist - dist[rayNum + 1]) >= 0.0) {
                    /* look for distances that bracket the search distance */
                    if((rayParams[rayNum] == rayParams[rayNum + 1])
                            && rayParams.length > 2) {
                        /*
                         * Here we have a shadow zone, so it is not really an
                         * arrival.
                         */
                        continue;
                    }
                    if(DEBUG) {
                        System.err.println("SeismicPhase " + name
                                + ", found arrival:\n" + "dist "
                                + (float)(180 / Math.PI * dist[rayNum]) + " "
                                + (float)(180 / Math.PI * searchDist) + " "
                                + (float)(180 / Math.PI * dist[rayNum + 1]));
                    }
                    arrivals.add(refineArrival(rayNum, searchDist, refineDistToleranceRadian, maxRecursion));
                }
            }
            /*
             * Look for arrivals that are 2(n+1)Pi-radDist, ie rays that have
             * done more than one half lap plus some number of whole laps.
             */
            searchDist = (n + 1) * 2.0 * Math.PI - radDist;
            if(tempDeg != 180) {
                for(int rayNum = 0; rayNum < (dist.length - 1); rayNum++) {
                    if(searchDist == dist[rayNum + 1]
                            && rayNum + 1 != dist.length - 1) {
                        /* So we don't get 2 arrivals for the same ray. */
                        continue;
                    } else if((dist[rayNum] - searchDist)
                            * (searchDist - dist[rayNum + 1]) >= 0.0) {
                        if((rayParams[rayNum] == rayParams[rayNum + 1])
                                && rayParams.length > 2) {
                            /*
                             * Here we have a shadow zone, so it is not really
                             * an arrival.
                             */
                            continue;
                        }
                        if(DEBUG) {
                            System.err.println("SeismicPhase " + name
                                    + ", found arrival:\n" + "dist "
                                    + (float)(180 / Math.PI * dist[rayNum])
                                    + " " + (float)(180 / Math.PI * searchDist)
                                    + " "
                                    + (float)(180 / Math.PI * dist[rayNum + 1]));
                        }
                        arrivals.add(refineArrival(rayNum, searchDist, refineDistToleranceRadian, maxRecursion));
                        
                    }
                }
            }
            n++;
        }
        Collections.sort(arrivals, new Comparator<Arrival>() {
            public int compare(Arrival o1, Arrival o2) {
                return Double.compare(o1.getTime(), o2.getTime());
            }});
        return arrivals;
    }
    
    public Arrival refineArrival(int rayNum, double distRadian, double distTolRadian, int maxRecursion) {
        Arrival left = new Arrival(this,
                                   getTime(rayNum),
                                   getDist(rayNum),
                                   getRayParams(rayNum),
                                   rayNum,
                                   name,
                                   puristName,
                                   sourceDepth);
        Arrival right = new Arrival(this,
                                   getTime(rayNum+1),
                                   getDist(rayNum+1),
                                   getRayParams(rayNum+1),
                                   rayNum, // use rayNum as know dist is between rayNum and rayNum+1
                                   name,
                                   puristName,
                                   sourceDepth);
        return refineArrival(left, right, distRadian, distTolRadian, maxRecursion);
    }
        
    public Arrival refineArrival(Arrival leftEstimate, Arrival rightEstimate, double searchDist, double distTolRadian, int maxRecursion) {
        Arrival linInterp = linearInterpArrival(searchDist, leftEstimate, rightEstimate);
        if(maxRecursion <= 0 || headOrDiffractSeq.size() > 0
                || name.endsWith("kmps")) {
            // can't shoot/refine for diffracted, head and non-body waves
            return linInterp;
        }
        if (linInterp.getRayParam() == leftEstimate.getRayParam()) { return leftEstimate;}
        if (linInterp.getRayParam() == rightEstimate.getRayParam()) { return rightEstimate;}

        if(DEBUG) {
            System.err.println("Phase: "+this);
            System.err.println("Refine: "+maxRecursion+"\nleft:  "+leftEstimate+"\nright: "+rightEstimate+"\nlinInterp: "+linInterp);
        }

        if (leftEstimate.getRayParam() < minRayParam || maxRayParam < leftEstimate.getRayParam()) {
            throw new RuntimeException("Left Ray param "+leftEstimate.getRayParam()+" is outside range for this phase: "+getName()+" min="+minRayParam+" max="+maxRayParam);
        }
        if (rightEstimate.getRayParam() < minRayParam || maxRayParam < rightEstimate.getRayParam()) {
            throw new RuntimeException("Right Ray param "+rightEstimate.getRayParam()+" is outside range for this phase: "+getName()+" min="+minRayParam+" max="+maxRayParam);
        }
        
        try {
            Arrival shoot = shootRay(linInterp.getRayParam());
            if ((leftEstimate.getDist() - searchDist)
                    * (searchDist - shoot.getDist()) > 0) {
                // search between left and shoot
                if (Math.abs(shoot.getDist()-linInterp.getDist()) < distTolRadian) {
                    return linearInterpArrival(searchDist, leftEstimate, shoot);
                } else {
                    return refineArrival(leftEstimate, shoot, searchDist, distTolRadian, maxRecursion-1);
                }
            } else {
                // search between shoot and right
                if (Math.abs(shoot.getDist()-linInterp.getDist()) < distTolRadian) {
                    return linearInterpArrival(searchDist, shoot, rightEstimate);
                } else {
                    return refineArrival(shoot, rightEstimate, searchDist, distTolRadian, maxRecursion-1);
                }
            }
        } catch(NoSuchLayerException e) {
            throw new RuntimeException("Should not happen: "+getName(), e);
        } catch(SlownessModelException e) {
            throw new RuntimeException("Should not happen: "+getName(), e);
        }
    }
    
    public Arrival shootRay(double rayParam) throws SlownessModelException, NoSuchLayerException {
        if(name.indexOf("Sdiff") != -1 
                || name.indexOf("Pdiff") != -1 
                || name.indexOf("Pn") != -1 
                || name.indexOf("Sn") != -1
                || name.endsWith("kmps")) {
            throw new SlownessModelException("Unable to shoot ray in non-body waves");
        }
        if (rayParam < minRayParam || maxRayParam < rayParam) {
            throw new SlownessModelException("Ray param "+rayParam+" is outside range for this phase: "+getName()+" min="+minRayParam+" max="+maxRayParam);
        }
        // looks like a body wave and can ray param can propagate
        int rayParamIndex = -1;
        for (rayParamIndex = 0; rayParamIndex < rayParams.length-1 && rayParams[rayParamIndex+1] >= rayParam; rayParamIndex++) {}
        /* counter for passes through each branch. 0 is P and 1 is S. */
        int[][] timesBranches = SeismicPhaseFactory.calcBranchMultiplier(tMod, branchSeq, waveType);
        TimeDist sum = new TimeDist(rayParam);
        /* Sum the branches with the appropriate multiplier. */
        for(int j = 0; j < tMod.getNumBranches(); j++) {
            if(timesBranches[0][j] != 0) {
                int topLayerNum = tMod.getSlownessModel().layerNumberBelow(tMod.getTauBranch(j, PWAVE).getTopDepth(), PWAVE);
                int botLayerNum = tMod.getSlownessModel().layerNumberAbove(tMod.getTauBranch(j, PWAVE).getBotDepth(), PWAVE);
                TimeDist td = tMod.getTauBranch(j, PWAVE).calcTimeDist(tMod.getSlownessModel(),
                                                                       topLayerNum,
                                                                       botLayerNum,
                                                                       rayParam,
                                                                       true);
                td = new TimeDist(rayParam,
                                  timesBranches[0][j]*td.getTime(),
                                  timesBranches[0][j]*td.getDistRadian(),
                                  td.getDepth());
                sum = sum.add(td);
            }
            if(timesBranches[1][j] != 0) {
                int topLayerNum = tMod.getSlownessModel().layerNumberBelow(tMod.getTauBranch(j, SWAVE).getTopDepth(), SWAVE);
                int botLayerNum = tMod.getSlownessModel().layerNumberAbove(tMod.getTauBranch(j, SWAVE).getBotDepth(), SWAVE);
                TimeDist td = tMod.getTauBranch(j, SWAVE).calcTimeDist(tMod.getSlownessModel(),
                                                                       topLayerNum,
                                                                       botLayerNum,
                                                                       rayParam,
                                                                       true);
                td = new TimeDist(rayParam,
                                  timesBranches[1][j]*td.getTime(),
                                  timesBranches[1][j]*td.getDistRadian(),
                                  td.getDepth());
                sum = sum.add(td);
            }
        }
        return new Arrival(this,
                           sum.getTime(),
                           sum.getDistRadian(),
                           rayParam,
                           rayParamIndex,
                           name,
                           puristName,
                           sourceDepth);
    }

    /**
     * Interprets between two arrivals to find new arrival at given distance.
     * @param searchDist new arrival distance
     * @param left known arrival to left
     * @param right known arrival to right
     * @return Arrival at searchDist
     */
    private Arrival linearInterpArrival(double searchDist,
                                        Arrival left,
                                        Arrival right) {
        if (left.getDist() == searchDist) {
            return left;
        }
        if (right.getDist() == searchDist) {
            return right;
        }
        double arrivalRayParam = (searchDist - right.getDist())
                * (left.getRayParam() - right.getRayParam())
                / (left.getDist() - right.getDist())
                + right.getRayParam();

        // use closest edge to interpolate time
        double arrivalTime;
        if (Math.abs(searchDist - left.getDist()) < Math.abs(searchDist - right.getDist())) {
            arrivalTime = left.getTime() + arrivalRayParam * (searchDist - left.getDist());
        } else {
            arrivalTime = right.getTime() + arrivalRayParam * (searchDist - right.getDist());
        }
        if (Double.isNaN(arrivalTime)) {
            throw new RuntimeException("Time is NaN, search "+searchDist +" leftDist "+ left.getDist()+ " leftTime "+left.getTime()
                               +"  rightDist "+right.getDist()+"  rightTime "+right.getTime());
        }

        return new Arrival(this,
                           arrivalTime,
                           searchDist,
                           arrivalRayParam,
                           left.getRayParamIndex(),
                           name,
                           puristName,
                           sourceDepth);
    }
    
    public double calcRayParamForTakeoffAngle(double takeoffDegree) {
        double takeoffVelocity;
        VelocityModel vMod = getTauModel().getVelocityModel();
        try {
            if (getDownGoing()[0]) {
                takeoffVelocity = vMod.evaluateBelow(sourceDepth, name.charAt(0));
            } else { 
                takeoffVelocity = vMod.evaluateAbove(sourceDepth, name.charAt(0));
            }
            double rayParam = (getTauModel().getRadiusOfEarth()-sourceDepth)*Math.sin(takeoffDegree*Math.PI/180)/takeoffVelocity;
            return rayParam;
        } catch(NoSuchLayerException e) {
            throw new RuntimeException("Should not happen", e);
        } catch(NoSuchMatPropException e) {
            throw new RuntimeException("Should not happen", e);
        }
    }
    
    public double calcTakeoffAngle(double arrivalRayParam) {
        double takeoffVelocity;
        if (name.endsWith("kmps")) {
            return 0;
        }
        VelocityModel vMod = getTauModel().getVelocityModel();
        try {
            char firstLeg = name.charAt(0);
            if (firstLeg == 'P' || firstLeg == 'p' || firstLeg == 'K' || firstLeg == 'k' || firstLeg == 'I') {
                firstLeg = 'P';
            } else if (firstLeg == 'S' || firstLeg == 's' || firstLeg == 'J' ) {
                firstLeg = 'S';
            }
            if (getDownGoing()[0]) {
                takeoffVelocity = vMod.evaluateBelow(sourceDepth, firstLeg);
            } else { 
                takeoffVelocity = vMod.evaluateAbove(sourceDepth, firstLeg);
            }
            double takeoffAngle = 180/Math.PI*Math.asin(takeoffVelocity*arrivalRayParam/(getTauModel().getRadiusOfEarth()-sourceDepth));
            if ( ! getDownGoing()[0]) {
                // upgoing, so angle is in 90-180 range
                takeoffAngle = 180-takeoffAngle;
            }
            return takeoffAngle;
        } catch(NoSuchLayerException e) {
            throw new RuntimeException("Should not happen", e);
        } catch(NoSuchMatPropException e) {
            throw new RuntimeException("Should not happen", e);
        }
    }
    
    public double calcIncidentAngle(double arrivalRayParam) {
        if (name.endsWith("kmps")) {
            return 0;
        }
        double incidentVelocity;
        VelocityModel vMod = getTauModel().getVelocityModel();
        try {
            char lastLeg = getLegs().get(getLegs().size()-2).charAt(0); // last item is "END", assume first char is P or S
            if (lastLeg == 'P' || lastLeg == 'p' || lastLeg == 'K' || lastLeg == 'k' || lastLeg == 'I') {
                lastLeg = 'P';
            } else if (lastLeg == 'S' || lastLeg == 's' || lastLeg == 'J' ) {
                lastLeg = 'S';
            }
            if (getDownGoing()[getDownGoing().length-1]) {
                incidentVelocity = vMod.evaluateAbove(receiverDepth, lastLeg);
            } else {
                incidentVelocity = vMod.evaluateBelow(receiverDepth, lastLeg);
            }
            double incidentAngle = 180/Math.PI*Math.asin(incidentVelocity*arrivalRayParam/(getTauModel().getRadiusOfEarth()-receiverDepth));
            if (getDownGoing()[getDownGoing().length-1]) {
                incidentAngle = 180 - incidentAngle;
            }
            return incidentAngle;
        } catch(NoSuchLayerException e) {
            throw new RuntimeException("Should not happen", e);
        } catch(NoSuchMatPropException e) {
            throw new RuntimeException("Should not happen", e);
        }
    }
        


    /**
     * Calculates the "pierce points" for the arrivals stored in arrivals. The
     * pierce points are stored within each arrival object.
     * @deprecated  Use the getPierce() method on each Arrival from calcTime()
     */
    @Deprecated
    public List<Arrival> calcPierce(double deg) throws TauModelException {
        List<Arrival> arrivals = calcTime(deg);
        for (Arrival a : arrivals) {
            a.getPierce(); // side effect calc pierce
        }
        return arrivals;
    }
    
    /** Calculates the pierce points for a particular arrival. The returned arrival is the same
     * as the input arguement but now has the pierce points filled in.
     * @param currArrival
     * @return same arrival with pierce points
     * @deprecated  Use the getPierce() method on each Arrival from calcTime()
     */
    @Deprecated
    public Arrival calcPierce(Arrival currArrival) {
        currArrival.getPierce();
        return currArrival;
    }
    
    protected List<TimeDist> calcPierceTimeDist(Arrival currArrival) {
        double branchDist = 0.0;
        double branchTime = 0.0;
        double prevBranchTime = 0.0;
        List<TimeDist> pierce = new ArrayList<TimeDist>();
        /*
         * Find the ray parameter index that corresponds to the arrival ray
         * parameter in the TauModel, ie it is between rayNum and rayNum+1,
         * We know that it must be <tMod.rayParams.length-1 since the last
         * ray parameter sample is 0, at least in a spherical model...
         */
        int rayNum = 0;
        for(int i = 0; i < tMod.rayParams.length - 1; i++) {
            if(tMod.rayParams[i] >= currArrival.getRayParam()) {
                rayNum = i;
            } else {
                break;
            }
        }
        // here we use ray parameter and dist info stored within the
        // SeismicPhase so we can use currArrival.rayParamIndex, which
        // may not correspond to rayNum (for tMod.rayParams).
        double distRayParam;
        double distA;
        double distB;
        double distRatio;
        if (currArrival.getRayParamIndex() == rayParams.length-1) {
            // special case for exactly matching last ray param (often rayparam==0)
            distRayParam = rayParams[currArrival.getRayParamIndex()];
            distA = dist[currArrival.getRayParamIndex()];
            distB = dist[currArrival.getRayParamIndex()];
            distRatio = 1;
        } else {
            // normal case, in middle of ray param space
            double rayParamA = rayParams[currArrival.getRayParamIndex()];
            double rayParamB = rayParams[currArrival.getRayParamIndex() + 1];
            distA = dist[currArrival.getRayParamIndex()];
            distB = dist[currArrival.getRayParamIndex() + 1];
            distRatio = (currArrival.getDist() - distA) / (distB - distA);
            distRayParam = distRatio * (rayParamB - rayParamA) + rayParamA;
        }
        /* First pierce point is always 0 distance at the source depth. */
        pierce.add(new TimeDist(distRayParam,
                                             0.0,
                                             0.0,
                                             tMod.getSourceDepth()));
        /*
         * Loop from 0 but already done 0, so the pierce point when the ray
         * leaves branch i is stored in i+1. Use linear interpolation
         * between rays that we know.
         */
        for(int i = 0; i < branchSeq.size(); i++) {
            int branchNum = ((Integer)branchSeq.get(i)).intValue();
            boolean isPWave = ((Boolean)waveType.get(i)).booleanValue();
            if(DEBUG) {
                System.out.println(i + " branchNum =" + branchNum
                        + " downGoing=" + (Boolean)downGoing.get(i)
                        + "  isPWave=" + isPWave);
            }
            /*
             * Save the turning depths for the ray parameter for both P and
             * S waves. This way we get the depth correct for any rays that
             * turn within a layer. We have to do this on a per branch basis
             * because of converted phases, e.g. SKS.
             */
            double turnDepth;
            try {
                if(distRayParam > tMod.getTauBranch(branchNum, isPWave)
                        .getMaxRayParam()) {
                    turnDepth = tMod.getTauBranch(branchNum, isPWave)
                            .getTopDepth();
                } else if(distRayParam <= tMod.getTauBranch(branchNum,
                                                            isPWave)
                        .getMinRayParam()) {
                    turnDepth = tMod.getTauBranch(branchNum, isPWave)
                            .getBotDepth();
                } else {
                    if(isPWave
                            || tMod.getSlownessModel()
                                    .depthInFluid((tMod.getTauBranch(branchNum,
                                                                     isPWave)
                                            .getTopDepth() + tMod.getTauBranch(branchNum,
                                                                               isPWave)
                                            .getBotDepth()) / 2.0)) {
                        turnDepth = tMod.getSlownessModel()
                                .findDepth(distRayParam,
                                           tMod.getTauBranch(branchNum,
                                                             isPWave)
                                                   .getTopDepth(),
                                           tMod.getTauBranch(branchNum,
                                                             isPWave)
                                                   .getBotDepth(),
                                           PWAVE);
                    } else {
                        turnDepth = tMod.getSlownessModel()
                                .findDepth(distRayParam,
                                           tMod.getTauBranch(branchNum,
                                                             isPWave)
                                                   .getTopDepth(),
                                           tMod.getTauBranch(branchNum,
                                                             isPWave)
                                                   .getBotDepth(),
                                           isPWave);
                    }
                }
            } catch(SlownessModelException e) {
                // shouldn't happen but...
                throw new RuntimeException("SeismicPhase.calcPierce: Caught SlownessModelException. "
                        , e);
            }
            double timeA, timeB;
            if(name.indexOf("Pdiff") != -1 || name.indexOf("Pn") != -1
                    || name.indexOf("Sdiff") != -1
                    || name.indexOf("Sn") != -1) {
                /* head waves and diffracted waves are a special case. */
                distA = tMod.getTauBranch(branchNum, isPWave)
                        .getDist(rayNum);
                timeA = tMod.getTauBranch(branchNum, isPWave).time[rayNum];
                distB = tMod.getTauBranch(branchNum, isPWave)
                        .getDist(rayNum);
                timeB = tMod.getTauBranch(branchNum, isPWave).time[rayNum];
            } else {
                distA = tMod.getTauBranch(branchNum, isPWave)
                        .getDist(rayNum);
                timeA = tMod.getTauBranch(branchNum, isPWave).time[rayNum];
                distB = tMod.getTauBranch(branchNum, isPWave)
                        .getDist(rayNum + 1);
                timeB = tMod.getTauBranch(branchNum, isPWave).time[rayNum + 1];
            }
            branchDist += distRatio * (distB - distA) + distA;
            prevBranchTime = branchTime;
            branchTime += distRatio * (timeB - timeA) + timeA;
            double branchDepth;
            if(((Boolean)downGoing.get(i)).booleanValue()) {
                branchDepth = Math.min(tMod.getTauBranch(branchNum, isPWave)
                                               .getBotDepth(),
                                       turnDepth);
            } else {
                branchDepth = Math.min(tMod.getTauBranch(branchNum, isPWave)
                                               .getTopDepth(),
                                       turnDepth);
            }
            // make sure ray actually propagates in this branch, leave
            // a little room for numerical "chatter"
            if(Math.abs(prevBranchTime - branchTime) > 1e-10) {
                pierce.add(new TimeDist(distRayParam,
                                                              branchTime,
                                                              branchDist,
                                                              branchDepth));
                if(DEBUG) {
                    System.out.println(" branchTime=" + branchTime
                            + " branchDist=" + branchDist + " branchDepth="
                            + branchDepth);
                    System.out.println("incrementTime = "
                            + (distRatio * (timeB - timeA)) + " timeB="
                            + timeB + " timeA=" + timeA);
                }
            }
            for(int diffBranchIdx = 0; diffBranchIdx < headOrDiffractSeq.size(); diffBranchIdx++) {
                int diffBranchNum = headOrDiffractSeq.get(diffBranchIdx);
                if (DEBUG) {
                    System.out.println("diff check: "+diffBranchNum+" "+i + " branchNum =" + branchNum
                                       + " downGoing=" + (Boolean)downGoing.get(i)
                                       + "  isPWave=" + isPWave);
                }
                if (i == diffBranchNum) {
                    double refractDist = (currArrival.getDist() - dist[0]) / headOrDiffractSeq.size();
                    double refractTime = refractDist*currArrival.getRayParam();
                    pierce.add(new TimeDist(distRayParam,
                                            branchTime + refractTime,
                                            branchDist + refractDist,
                                            branchDepth));
                    branchDist += refractDist;
                    prevBranchTime = branchTime;
                    branchTime += refractTime;
                }
            }
        }
        if(name.indexOf("kmps") != -1) {
            // just so kmps waves can be indentified as special
            headOrDiffractSeq.add(0);
            pierce.add(new TimeDist(distRayParam,
                                                 currArrival.getTime(),
                                                 currArrival.getDist(),
                                                 0));
        }
        return pierce;
    }

    /**
     * Here we worry about the special case for head and diffracted
     * waves. It is assumed that a phase can be a diffracted wave or a
     * head wave, but not both. Nor can it be a head wave or diffracted
     * wave for both P and S.
     */
    List<TimeDist> handleHeadOrDiffractedWave(Arrival currArrival, List<TimeDist> orig) {
        String[] phaseSegments = new String[] {"Pn", "Sn", "Pdiff", "Sdiff"};
        String phaseSeg = "";
        for (int i = 0; i < phaseSegments.length; i++) {
            if (name.indexOf(phaseSegments[i]) != -1) {
                phaseSeg = phaseSegments[i];
                break;
            }
        }
        if (phaseSeg.equals("")) {throw new RuntimeException("no head/diff segment in "+name); }
        double headDepth;
        if (phaseSeg.equals("Pn") || phaseSeg.equals("Sn")) {
            headDepth = tMod.getMohoDepth();
        } else {
            headDepth = tMod.getCmbDepth();
        }
        int numFound = 0;
        int indexInString = -1;
        // can't have both Pxxx and Sxxx in a head wave phase, so one of these
        // should do nothing
        while((indexInString = name.indexOf(phaseSeg, indexInString + 1)) != -1) {
            numFound++;
        }
        double refractDist = currArrival.getDist() - dist[0];
        double refractTime = refractDist*currArrival.getRayParam();
        List<TimeDist> out = new ArrayList<TimeDist>();
        int j = 0;
        for (TimeDist td : orig) {
            // this is a little weird as we are not checking where we are in the phase name, but simply
            // if the depth matches. This likely works in most cases, but may not for head/diffracted
            // waves that undergo a phase change, if that type of phase can even exist
            out.add(new TimeDist(td.getP(), td.getTime()+j * refractTime / numFound, td.getDistRadian() + j * refractDist / numFound, td.getDepth()));
            if (td.getDepth() == headDepth) {
                j++;
                out.add(new TimeDist(td.getP(), td.getTime()+j * refractTime / numFound, td.getDistRadian() + j * refractDist / numFound, td.getDepth()));
            }
        }
        return out;
    }

    /** calculates the paths this phase takes through the earth model. 
     * @deprecated  Use the getPath() method on each Arrival from calcTime()
     */
    @Deprecated
    public List<Arrival> calcPath(double deg) {
        List<Arrival> arrivals = calcTime(deg);
        for (Arrival a : arrivals) {
            a.getPath(); // side effect calculates path
        }
        return arrivals;
    }
    
    /**
     * 
     * @param currArrival
     * @return
     * @deprecated use the getPath() method on the arrival.
     */
    @Deprecated
    public Arrival calcPath(Arrival currArrival) {
        currArrival.getPath(); // side effect calculates path
        return currArrival;
    }
    
    protected List<TimeDist> calcPathTimeDist(Arrival currArrival) {
        ArrayList<TimeDist[]> pathList = new ArrayList<TimeDist[]>();
        /*
         * Find the ray parameter index that corresponds to the arrival ray
         * parameter in the TauModel, ie it is between rayNum and rayNum+1.
         */
        TimeDist[] tempTimeDist = new TimeDist[1];
        tempTimeDist[0] = new TimeDist(currArrival.getRayParam(),
                                       0.0,
                                       0.0,
                                       tMod.getSourceDepth());
        pathList.add(tempTimeDist);
            for(int i = 0; i < branchSeq.size(); i++) {
                int branchNum = ((Integer)branchSeq.get(i)).intValue();
                boolean isPWave = ((Boolean)waveType.get(i)).booleanValue();
                if(DEBUG) {
                    System.out.println("i=" + i + " branchNum=" + branchNum
                            + " isPWave=" + isPWave + " downgoing="
                            + ((Boolean)downGoing.get(i)).booleanValue());
                }
                try {
                    tempTimeDist = tMod.getTauBranch(branchNum, isPWave)
                            .path(currArrival.getRayParam(),
                                  ((Boolean)downGoing.get(i)).booleanValue(),
                                  tMod.getSlownessModel());
                } catch(SlownessModelException e) {
                    // shouldn't happen but...
                    throw new RuntimeException("SeismicPhase.calcPath: Caught SlownessModelException. "
                            , e);
                }
                if(tempTimeDist != null) {
                    pathList.add(tempTimeDist);
                    for (int j = 0; j < tempTimeDist.length; j++) {
                        if (tempTimeDist[j].getDistDeg() < 0) {
                            throw new RuntimeException("Path is backtracking, no possible: "+j+" ("+tempTimeDist[j]+")");
                        }
                    }
                }
                /*
                 * Here we worry about the special case for head and
                 * diffracted waves.
                 */

                for(int diffBranchIdx = 0; diffBranchIdx < headOrDiffractSeq.size(); diffBranchIdx++) {
                    int diffBranchNum = headOrDiffractSeq.get(diffBranchIdx);
                    if (DEBUG) {
                        System.out.println("diff check: "+diffBranchNum+" "+i + " branchNum =" + branchNum
                                           + " downGoing=" + (Boolean)downGoing.get(i)
                                           + "  isPWave=" + isPWave);
                    }
                    if (i == diffBranchNum) {
                        double refractDist = (currArrival.getDist() - dist[0]) / headOrDiffractSeq.size();
                        double refractTime = refractDist*currArrival.getRayParam();
                        TimeDist[] diffTD = new TimeDist[1];
                        if (name.indexOf("Pdiff") != -1 || name.indexOf("Sdiff") != -1) {
                            diffTD[0] = new TimeDist(currArrival.getRayParam(),
                                                    refractTime,
                                                    refractDist,
                                                    tMod.cmbDepth);
                        } else if (name.indexOf("Pn") != -1 || name.indexOf("Sn") != -1) {
                            diffTD[0] = new TimeDist(currArrival.getRayParam(),
                                                     refractTime,
                                                     refractDist,
                                                     tMod.mohoDepth);
                        } else {
                            throw new RuntimeException("Path adding head and diffracted wave, but did not find P/Sdiff or P/Sn, expected: "+headOrDiffractSeq.size());
                        }
                        pathList.add(diffTD);
                    }
                }
            }
            if (name.indexOf("kmps") != -1) {
                // kmps phases have no branches, so need to end them at the arrival distance
                TimeDist[] headTD = new TimeDist[1];
                headTD[0] = new TimeDist(currArrival.getRayParam(),
                                         currArrival.getDist()
                                         * currArrival.getRayParam(),
                                         currArrival.getDist(),
                                         0);
                pathList.add(headTD);
            }
            List<TimeDist> outPath = new ArrayList<TimeDist>();
            TimeDist cummulative = new TimeDist(currArrival.getRayParam(),
                                                0.0,
                                                0.0,
                                                currArrival.getSourceDepth());
            TimeDist prev = cummulative;
            TimeDist[] branchPath;
            int numAdded = 0;
            for(int i = 0; i < pathList.size(); i++) {
                branchPath = (TimeDist[])pathList.get(i);
                for(int j = 0; j < branchPath.length; j++) {
                    prev = cummulative;
                    cummulative = new TimeDist(cummulative.getP(),
                                               cummulative.getTime()+branchPath[j].getTime(),
                                               cummulative.getDistRadian()+branchPath[j].getDistRadian(),
                                               branchPath[j].getDepth());
                    outPath.add(cummulative);
                    if (numAdded > 0 && cummulative.getDistRadian() < prev.getDistRadian()) {
                        throw new RuntimeException("Backtracking ray, not possible: "+numAdded+" "+cummulative+") < ("+prev+")");
                    }
                    numAdded++;
                }
            }
        return outPath;
    }

    public static Arrival getEarliestArrival(List<SeismicPhase> phases, double degrees) {
        Arrival minArrival = null;
        for (SeismicPhase seismicPhase : phases) {
            seismicPhase.calcTime(degrees);
            Arrival currArrival = seismicPhase.getEarliestArrival(degrees);
            if (currArrival != null && ( minArrival == null || minArrival.getTime() > currArrival.getTime())) {
                minArrival = currArrival;
            }
        }
        return minArrival;
    }
    
    public String describe( ) {
        String desc = name + ":\n";
        if (phasesExistsInModel()) {
        	desc += "  exists from "+Outputs.formatDistance(getMinDistanceDeg())+" to "+Outputs.formatDistance(getMaxDistanceDeg())+" degrees.\n";
        	if (getMaxRayParam() > getMinRayParam()) {
        		desc += "  with ray parameter from "+Outputs.formatRayParam(getMaxRayParam())+" down to "+Outputs.formatRayParam(getMinRayParam())+" sec/rad.\n";
        	} else {
        		desc += "  with degenerate ray parameter of "+Outputs.formatRayParam(getMaxRayParam())+"\n";
        	}
        	desc += "  travel times from "+Outputs.formatTime(time[0])+" to "+Outputs.formatTime(time[time.length-1])+" sec";
            for(int i = 0; i < dist.length; i++) {
                if (i < dist.length - 1 && (rayParams[i] == rayParams[i + 1])
                        && rayParams.length > 2) {
                    /* Here we have a shadow zone, so output a warning of break in curve. */
                    desc += "\n  with shadow zone between " + Outputs.formatDistance(Arrival.RtoD*dist[i])
                            + " and " + Outputs.formatDistance(Arrival.RtoD*dist[i + 1]) + " deg";
                }
            }
            desc += ".\n";
        } else {
        	desc += "  FAILS to exist, because no ray parameters satisfy the path.\n";
        }
        for(SeismicPhaseSegment segment : getPhaseSegments()) {
        	desc += segment.toString()+"\n";
        }
        
        return desc;
    }

    public String toString() {
        String desc = name + ": ";
        for(int i = 0; i < legs.size(); i++) {
            desc += legs.get(i) + " ";
        }
        desc += "\n";
        for(int i = 0; i < branchSeq.size(); i++) {
            desc += (Integer)branchSeq.get(i) + " ";
        }
        desc += "\n";
        desc += "minRayParam=" + minRayParam + " maxRayParam=" + maxRayParam;
        desc += "\n";
        desc += "minDistance=" + (minDistance * 180.0 / Math.PI)
                + " maxDistance=" + (maxDistance * 180.0 / Math.PI);
        return desc;
    }

    public void dump() {
        for(int j = 0; j < dist.length; j++) {
            System.out.println(j + "  " + dist[j] + "  " + rayParams[j]);
        }
    }

    public static void main(String args[]) {
        TauModel tMod;
        TauModel tModDepth;
        try {
            if(args.length < 3) {
                System.out.println("Usage: SeismicPhase modelfile depth phasename [phasename ...]");
            }
            tMod = TauModel.readModel(args[0]);
            tModDepth = tMod.depthCorrect(Double.valueOf(args[1]).doubleValue());
            for(int i = 2; i < args.length; i++) {
                System.out.println("-----");
                SeismicPhase sp = SeismicPhaseFactory.createPhase(args[i], tModDepth);
                System.out.println(sp);
                sp.dump();
            }
            System.out.println("-----");
        } catch(FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch(OptionalDataException e) {
            System.out.println(e.getMessage());
        } catch(StreamCorruptedException e) {
            System.out.println(e.getMessage());
        } catch(IOException e) {
            System.out.println(e.getMessage());
        } catch(ClassNotFoundException e) {
            System.out.println(e.getMessage());
        } catch(TauModelException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
