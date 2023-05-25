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
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
public class SimpleSeismicPhase implements SeismicPhase {

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
    public SimpleSeismicPhase(String name, String modelName, double depth) throws TauModelException {
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
    public SimpleSeismicPhase(String name, TauModel tMod) throws TauModelException {
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
    public SimpleSeismicPhase(String name, TauModel tMod, double receiverDepth) throws TauModelException {
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
    public SimpleSeismicPhase(String name, TauModel tMod, double receiverDepth, boolean debug) throws TauModelException {
        SimpleSeismicPhase phase = SeismicPhaseFactory.createPhase(name, tMod, tMod.sourceDepth, receiverDepth, debug);

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

    public SimpleSeismicPhase(String name,
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


    @Override
    public boolean phasesExistsInModel() {
        return getMaxRayParam() >= 0;
    }

    @Override
    public Arrival getEarliestArrival(double degrees) {
        return Arrival.getEarliestArrival(calcTime(degrees));
    }

    @Override
    public TauModel getTauModel() {
        return tMod;
    }

    @Override
    public double getMinDistanceDeg() {
        return getMinDistance() * 180.0 / Math.PI;
    }

    @Override
    public double getMinDistance() {
        return minDistance;
    }

    @Override
    public double getMaxDistanceDeg() {
        return getMaxDistance() * 180.0 / Math.PI;
    }

    @Override
    public double getMaxDistance() {
        return maxDistance;
    }

    @Override
    public double getMaxRayParam() {
        return maxRayParam;
    }

    @Override
    public double getMinRayParam() {
        return minRayParam;
    }

    @Override
    public int getMaxRayParamIndex() {
        return maxRayParamIndex;
    }

    @Override
    public int getMinRayParamIndex() {
        return minRayParamIndex;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPuristName() {
        return name;
    }

    @Override
    public double getSourceDepth() {
        return sourceDepth;
    }

    @Override
    public double getReceiverDepth() {
        return receiverDepth;
    }

    @Override
    public List<String> getLegs() {
        return Collections.unmodifiableList(legs);
    }

    @Override
    public List<SeismicPhaseSegment> getPhaseSegments() {
    	return Collections.unmodifiableList(segmentList);
    }

    @Override
    public double getRayParams(int i) {
        return rayParams[i];
    }

    @Override
    public double[] getRayParams() {
        return (double[])rayParams.clone();
    }

    @Override
    public double getDist(int i) {
        return dist[i];
    }

    @Override
    public double[] getDist() {
        return (double[])dist.clone();
    }

    @Override
    public double getTime(int i) {
        return time[i];
    }

    @Override
    public double[] getTime() {
        return (double[])time.clone();
    }

    @Override
    public double getTau(int i) {
        return time[i] - rayParams[i] * dist[i];
    }

    @Override
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
    @Override
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
    @Override
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
    @Override
    public int[] getLegAction() {
        Integer[] b = (Integer[])legAction.toArray(new Integer[0]);
        int[] out = new int[b.length];
        for(int i = 0; i < b.length; i++) {
            out[i] = b[i].intValue();
        }
        return out;
    }

    @Override
    public boolean hasArrivals() {
        return dist != null && dist.length != 0;
    }

    // Normal methods

    /** calculates arrival times for this phase, sorted by time.
     *  */
    @Override
    public List<Arrival> calcTime(double deg) {
        double tempDeg = SeismicPhase.distanceTrim180(deg);
        if (TauP_Tool.DEBUG) {
            System.out.println("Calculation distance: "+tempDeg+" deg");
        }
        double deltaTemp = Math.abs((deg - tempDeg + 180) % 360 - 180);
        double deltaTempLongWay = Math.abs(((360-deg)-tempDeg + 180) % 360 - 180);
        int trimDistIsPos = deltaTemp < deltaTempLongWay ? 1 : -1;
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
            List<Arrival> forwardArrivals = calcTimeExactDistance(searchDist);
            for (Arrival a : forwardArrivals) {
                a.setSearchDistDeg(deg );
            }
            arrivals.addAll(forwardArrivals);
            /*
             * Look for arrivals that are 2(n+1)Pi-radDist, ie rays that have
             * done more than one half lap plus some number of whole laps.
             */
            searchDist = (n + 1) * 2.0 * Math.PI - radDist;
            if(tempDeg != 180 && radDist != 0 && searchDist <= maxDistance) {
                List<Arrival> backwardsArrivals = calcTimeExactDistance(searchDist);
                for (Arrival a : backwardsArrivals) {
                    a.setSearchDistDeg(deg);
                }
                arrivals.addAll(backwardsArrivals);
            }
            n++;
        }
        Collections.sort(arrivals, new Comparator<Arrival>() {
            public int compare(Arrival o1, Arrival o2) {
                return Double.compare(o1.getTime(), o2.getTime());
            }});
        return arrivals;
    }

    /**
     * Calculates arrivals for this phase, but only for the exact distance in radians. This does not check multiple
     * laps nor going the long way around.
     *  */
    public List<Arrival> calcTimeExactDistance(double searchDist) {
        List<Arrival> arrivals = new ArrayList<Arrival>();
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
                    System.err.println("time "
                            +  time[rayNum] + " --  "
                            + time[rayNum + 1]);
                }
                arrivals.add(refineArrival(rayNum, searchDist, refineDistToleranceRadian, maxRecursion));
            }
        }
        return arrivals;
    }

    public Arrival refineArrival(int rayNum, double distRadian, double distTolRadian, int maxRecursion) {
        Arrival left = new Arrival(this,
                                   getTime(rayNum),
                                   getDist(rayNum),
                                   getRayParams(rayNum),
                                   rayNum);
        Arrival right = new Arrival(this,
                                   getTime(rayNum+1),
                                   getDist(rayNum+1),
                                   getRayParams(rayNum+1),
                                   rayNum // use rayNum as know dist is between rayNum and rayNum+1
                                    );
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

    @Override
    public Arrival shootRay(double rayParam) throws SlownessModelException, NoSuchLayerException {
        if(headOrDiffractSeq.size() > 0) {
            throw new SlownessModelException("Unable to shoot ray in non-body, head, diffracted waves");
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
                           rayParamIndex);
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
                           left.getRayParamIndex());
    }

    @Override
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

    @Override
    public double calcTakeoffAngle(double arrivalRayParam) {
        double takeoffVelocity;
        if (name.endsWith("kmps")) {
            return 0;
        }
        VelocityModel vMod = getTauModel().getVelocityModel();
        try {
            char firstLeg;
            if (segmentList.get(0).isPWave) {
                firstLeg = 'P';
            } else {
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

    @Override
    public double calcIncidentAngle(double arrivalRayParam) {
        if (name.endsWith("kmps")) {
            return 0;
        }
        double incidentVelocity;
        VelocityModel vMod = getTauModel().getVelocityModel();
        try {
            char lastLeg;
            if (segmentList.get(segmentList.size()-1).isPWave) {
                lastLeg = 'P';
            } else {
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

    public List<TimeDist> calcPierceTimeDist(Arrival currArrival) {
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
        // check in case going opposite way around from request distance
        double negMulDist = 1;
        if (currArrival.isLongWayAround()) {
            negMulDist = -1;
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
            if(headOrDiffractSeq.size() > 0) {
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
                        negMulDist*branchDist,
                                                              branchDepth));
                if(DEBUG) {
                    System.out.println("------->  add pierce "+branchDepth);
                    System.out.println(" branchTime=" + branchTime
                            + " branchDist=" + branchDist + " branchDepth="
                            + branchDepth);
                    System.out.println("incrementTime = "
                            + (distRatio * (timeB - timeA)) + " timeB="
                            + timeB + " timeA=" + timeA);
                }
            } else {
                if(DEBUG) {
                    System.out.println("Time inc in branch tiny: "+" branchTime=" + branchTime
                            + " branchDist=" + branchDist + " branchDepth="
                            + branchDepth);
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
                            negMulDist*(branchDist + refractDist),
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
                    negMulDist*currArrival.getDist(),
                                                 0));
        }
        return pierce;
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

    public List<TimeDist> calcPathTimeDist(Arrival currArrival) {
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

                        TauBranch branch = tMod.getTauBranch(branchNum, isPWave);
                        TimeDist[] diffTD = new TimeDist[1];
                        if (legAction.get(i).equals(DIFFRACT)) {
                            // diffraction happens at bottom of layer, like Pdiff bottom of mantle
                            diffTD[0] = new TimeDist(currArrival.getRayParam(),
                                    refractTime,
                                    refractDist,
                                    branch.getBotDepth());
                        } else if (legAction.get(i).equals(HEAD)) {
                            // head wave happens at top of layer, like Pn top of mantle
                            diffTD[0] = new TimeDist(currArrival.getRayParam(),
                                    refractTime,
                                    refractDist,
                                    branch.getTopDepth());
                        } else {
                            throw new RuntimeException("Should be one of DIFFRACT or HEAD: "+legAction.get(i));
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

                    if (currArrival.isLongWayAround()) {
                        outPath.add(cummulative.negateDistance());
                    } else {
                        outPath.add(cummulative);
                    }
                    numAdded++;
                }
            }
        return adjustPath(outPath, currArrival);
    }

    /**
     * Adjust path so the end point lines up.
     * Because we are shooting a ray parameter through the model, and that ray parameter came from an
     * interpolation, it can happen for long paths that the output path doesn't quite end at the requested
     * distance. We do a simple scaling of all path distances so it hits the output.
     * @param inPath
     * @param arrival
     * @return
     */
    public static List<TimeDist> adjustPath(List<TimeDist> inPath, Arrival arrival) {
        double distRadian = arrival.getDist();
        double finalPathDist = inPath.get(inPath.size()-1).getDistRadian();
        if (distRadian != 0 && finalPathDist != 0) {
            double shifty = distRadian/finalPathDist;
            if (arrival.isLongWayAround()) {
                shifty *= -1;
            }
            if (Math.abs(1.0-shifty) > .02 ) {
                System.err.println("Path error is greater than 2%, correction may cause errors. "+shifty+" "+arrival);
            }
            ArrayList<TimeDist> out = new ArrayList<TimeDist>();
            for (TimeDist td : inPath) {
                out.add(new TimeDist(td.getP(),
                        td.getTime(),
                        td.getDistRadian() * shifty,
                        td.getDepth()));
            }
            return out;
        } else {
            return inPath;
        }
    }

    @Override
    public String describe() {
        String desc = name + ":\n";
        return desc+ SeismicPhase.baseDescribe(this)+"\n"+ SeismicPhase.segmentDescribe(this);
    }


    @Override
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

    @Override
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
                System.out.println("Usage: SimpleSeismicPhase modelfile depth phasename [phasename ...]");
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
