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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static edu.sc.seis.TauP.Arrival.RtoD;

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

    ProtoSeismicPhase proto = null;

    /** Enables debugging output. */
    public transient boolean DEBUG;

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
    protected double receiverDepth;

    /**
     * Array of distances corresponding to the ray parameters stored in
     * rayParams.
     */
    protected double[] dist;

    /**
     * Array of times corresponding to the ray parameters stored in rayParams.
     */
    protected double[] time;

    /** Array of possible ray parameters for this phase. */
    protected double[] rayParams;

    /** Minimum ray parameter that exists for this phase. */
    protected double minRayParam;

    /** Maximum ray parameter that exists for this phase. */
    protected double maxRayParam;

    /**
     * Index within TauModel.rayParams that corresponds to maxRayParam. Note
     * that maxRayParamIndex &lt; minRayParamIndex as ray parameter decreases with
     * increasing index.
     */
    protected int maxRayParamIndex;

    /**
     * Index within TauModel.rayParams that corresponds to minRayParam. Note
     * that maxRayParamIndex &lt; minRayParamIndex as ray parameter decreases with
     * increasing index.
     */
    protected int minRayParamIndex;

    /** The minimum distance that this phase can be theoretically observed. */
    protected double minDistance;

    /** The maximum distance that this phase can be theoretically observed. */
    protected double maxDistance;

    /** The phase name, ie PKiKP. */
    protected String name;

    /**
     * name with depths corrected to be actuall discontinuities in the model.
     */
    protected String puristName;

    protected double refineDistToleranceRadian = 0.0049*Math.PI/180;

    protected int maxRecursion = 5;

    public static final boolean PWAVE = true;

    public static final boolean SWAVE = false;


    public SimpleSeismicPhase(ProtoSeismicPhase proto,
                              double[] rayParams,
                              double[] time,
                              double[] dist,
                              double minRayParam,
                              double maxRayParam,
                              int minRayParamIndex,
                              int maxRayParamIndex,
                              double minDistance,
                              double maxDistance,
                              boolean debug) {
        if (proto == null) {throw new IllegalArgumentException("proto cannot be null");}
        try {
            proto.validateSegList();
        } catch (TauModelException e) {
            throw new RuntimeException(getName()+" fail validation:", e);
        }
        this.proto = proto;
        this.DEBUG = debug ;
        this.name = proto.getName();
        this.tMod = proto.tMod;
        this.puristName = proto.getPuristName();
        this.sourceDepth = tMod != null ? tMod.getSourceDepth() : -1;
        this.receiverDepth = proto.receiverDepth;
        this.rayParams = rayParams;
        this.time = time;
        this.dist = dist;
        this.minRayParam = minRayParam;
        this.maxRayParam = maxRayParam;
        this.minRayParamIndex = minRayParamIndex;
        this.maxRayParamIndex = maxRayParamIndex;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
    }

    @Override
    public boolean phasesExistsInModel() {
        return getMaxRayParam() >= 0;
    }

    @Override
    public Arrival getEarliestArrival(double degrees) {
        return Arrival.getEarliestArrival(DistanceRay.ofDegrees(degrees).calcSimplePhase(this));
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

    public double getMinTime() {
        double v = time[0];
        for (double d : time) {
            v = Math.min(v, d);
        }
        return v;
    }

    public double getMaxTime() {
        double v = time[0];
        for (double d : time) {
            v = Math.max(v, d);
        }
        return v;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPuristName() {
        return puristName;
    }

    @Override
    public double getSourceDepth() {
        return sourceDepth;
    }

    @Override
    public double getReceiverDepth() {
        return receiverDepth;
    }

    /** Description of segments of the phase. */
    @Override
    public List<SeismicPhaseSegment> getPhaseSegments() {
    	return Collections.unmodifiableList(proto.segmentList);
    }

    @Override
    public int countFlatLegs() {
        return proto.countFlatLegs();
    }

    @Override
    public double getRayParams(int i) {
        return rayParams[i];
    }

    @Override
    public double[] getRayParams() {
        return rayParams.clone();
    }

    @Override
    public double getDist(int i) {
        return dist[i];
    }

    @Override
    public double[] getDist() {
        return dist.clone();
    }

    @Override
    public double getTime(int i) {
        return time[i];
    }

    @Override
    public double[] getTime() {
        return time.clone();
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

    @Override
    public boolean hasArrivals() {
        return dist != null && dist.length != 0;
    }

    // Normal methods

    @Deprecated
    public List<Arrival> calcTime(double deg) {
        return DistanceRay.ofDegrees(deg).calcSimplePhase(this);
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

    /**
     * Creates an Arrival for a sampled ray parameter from the model. No interpolation between rays as this is a sample.
     * @param rayNum
     * @return
     */
    public Arrival createArrivalAtIndex(int rayNum) {
        int adjacentRayNum = 0;
        double dRPdDist = 0;
        if (rayParams.length > 1) {
            if (rayNum == 0) {
                dRPdDist = (getRayParams(rayNum)-getRayParams(rayNum+1))/ (getDist(rayNum)-getDist(rayNum+1));
            } else if (rayNum == rayParams.length-1) {
                dRPdDist = (getRayParams(rayNum)-getRayParams(rayNum-1))/ (getDist(rayNum)-getDist(rayNum-1));
            } else {
                // average left and right ray params
                dRPdDist = ((getRayParams(rayNum)-getRayParams(rayNum-1))/ (getDist(rayNum)-getDist(rayNum-1))
                        + (getRayParams(rayNum)-getRayParams(rayNum+1))/ (getDist(rayNum)-getDist(rayNum+1)))
                        /2.0;
            }

        }
        return new Arrival(this,
                getTime(rayNum),
                getDist(rayNum),
                getRayParams(rayNum),
                rayNum,
                dRPdDist);
    }

    public Arrival refineArrival(int rayNum, double distRadian, double distTolRadian, int maxRecursion) {
        Arrival left = createArrivalAtIndex(rayNum);
        Arrival right = createArrivalAtIndex(rayNum+1);
        return refineArrival(left, right, distRadian, distTolRadian, maxRecursion);
    }

    public Arrival refineArrival(Arrival leftEstimate, Arrival rightEstimate, double searchDist, double distTolRadian, int maxRecursion) {
        Arrival linInterp = linearInterpArrival(searchDist, leftEstimate, rightEstimate);
        if(maxRecursion <= 0 || countFlatLegs() > 0) {
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
        if(countFlatLegs() > 0) {
            throw new SlownessModelException("Unable to shoot ray in non-body, head, diffracted waves");
        }
        if (rayParam < minRayParam || maxRayParam < rayParam) {
            throw new SlownessModelException("Ray param "+rayParam+" is outside range for this phase: "+getName()+" min="+minRayParam+" max="+maxRayParam);
        }
        // looks like a body wave and can ray param can propagate
        int rayParamIndex = -1;
        for (rayParamIndex = 0; rayParamIndex < rayParams.length-1 && rayParams[rayParamIndex+1] >= rayParam; rayParamIndex++) {
            // find index for ray param, done in for-loop check
        }
        /* counter for passes through each branch. 0 is P and 1 is S. */
        int[][] timesBranches = SeismicPhaseFactory.calcBranchMultiplier(tMod, getPhaseSegments());
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
        double dRPdDist = (rayParam-rayParams[rayParamIndex])/(sum.getDistRadian()-dist[rayParamIndex]);
        return new Arrival(this,
                           sum.getTime(),
                           sum.getDistRadian(),
                           rayParam,
                           rayParamIndex,
                dRPdDist);
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
        double dRPdDist;
        if (Math.abs(searchDist - left.getDist()) < Math.abs(searchDist - right.getDist())) {
            arrivalTime = left.getTime() + arrivalRayParam * (searchDist - left.getDist());
            dRPdDist = (left.getRayParam()-arrivalRayParam)/ (left.getDist()-searchDist);
        } else {
            arrivalTime = right.getTime() + arrivalRayParam * (searchDist - right.getDist());
            dRPdDist = (right.getRayParam()-arrivalRayParam)/ (right.getDist()-searchDist);
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
                dRPdDist);
    }

    @Override
    public double calcRayParamForTakeoffAngle(double takeoffDegree) throws NoArrivalException {
        if (getPhaseSegments().size() == 0) {
            throw new NoArrivalException("No phase segments for "+getName());
        }
        double takeoffVelocity;
        VelocityModel vMod = getTauModel().getVelocityModel();
        VelocityModelMaterial material = getPhaseSegments().get(0).isPWave ? VelocityModelMaterial.P_VELOCITY : VelocityModelMaterial.S_VELOCITY;
        try {
            if (getPhaseSegments().get(0).isDownGoing) {
                takeoffVelocity = vMod.evaluateBelow(sourceDepth, material);
            } else {
                takeoffVelocity = vMod.evaluateAbove(sourceDepth, material);
            }
            double rayParam = (getTauModel().getRadiusOfEarth()-sourceDepth)*Math.sin(takeoffDegree*Math.PI/180)/takeoffVelocity;
            return rayParam;
        } catch(NoSuchLayerException e) {
            throw new RuntimeException("Should not happen", e);
        }
    }

    @Override
    public double velocityAtSource() {
        try {
            double takeoffVelocity;
            VelocityModelMaterial firstLeg;
            VelocityModel vMod = getTauModel().getVelocityModel();
            if (getPhaseSegments().get(0).isPWave) {
                firstLeg = VelocityModelMaterial.P_VELOCITY;
            } else {
                firstLeg = VelocityModelMaterial.S_VELOCITY;
            }
            if (getPhaseSegments().get(0).isDownGoing) {
                takeoffVelocity = vMod.evaluateBelow(sourceDepth, firstLeg);
            } else {
                takeoffVelocity = vMod.evaluateAbove(sourceDepth, firstLeg);
            }
            return takeoffVelocity;
        } catch(NoSuchLayerException e) {
            throw new RuntimeException("Should not happen", e);
        }
    }

    @Override
    public double velocityAtReceiver() {
        try {
            double incidentVelocity;
            VelocityModel vMod = getTauModel().getVelocityModel();
            VelocityModelMaterial lastLeg;
            if (getPhaseSegments().get(getPhaseSegments().size()-1).isPWave) {
                lastLeg = VelocityModelMaterial.P_VELOCITY;
            } else {
                lastLeg = VelocityModelMaterial.S_VELOCITY;
            }
            if (getPhaseSegments().get(getPhaseSegments().size()-1).isDownGoing) {
                incidentVelocity = vMod.evaluateAbove(receiverDepth, lastLeg);
            } else {
                incidentVelocity = vMod.evaluateBelow(receiverDepth, lastLeg);
            }
            return incidentVelocity;
        } catch(NoSuchLayerException e) {
            throw new RuntimeException("Should not happen", e);
        }
    }

    @Override
    public double densityAtSource() {
        try {
            double rho;
            VelocityModel vMod = getTauModel().getVelocityModel();
            if (getInitialPhaseSegment().isDownGoing) {
                rho = vMod.evaluateAbove(sourceDepth, VelocityModelMaterial.DENSITY);
            } else {
                rho = vMod.evaluateBelow(sourceDepth, VelocityModelMaterial.DENSITY);
            }
            return rho;
        } catch(NoSuchLayerException e) {
            throw new RuntimeException("Should not happen", e);
        }
    }

    @Override
    public double densityAtReceiver() {
        try {
            double rho;
            VelocityModel vMod = getTauModel().getVelocityModel();
            if (getFinalPhaseSegment().isDownGoing) {
                rho = vMod.evaluateAbove(receiverDepth, VelocityModelMaterial.DENSITY);
            } else {
                rho = vMod.evaluateBelow(receiverDepth, VelocityModelMaterial.DENSITY);
            }
            return rho;
        } catch(NoSuchLayerException e) {
            throw new RuntimeException("Should not happen", e);
        }
    }

    @Override
    public double calcTakeoffAngleDegree(double arrivalRayParam) {
        return calcTakeoffAngle(arrivalRayParam)*RtoD;
    }
    @Override
    public double calcTakeoffAngle(double arrivalRayParam) {
        if (name.endsWith("kmps")) {
            return 0;
        }
        double takeoffAngle = Math.asin(velocityAtSource()*arrivalRayParam/(getTauModel().getRadiusOfEarth()-sourceDepth));
        if (! Double.isFinite(takeoffAngle)
                && Math.abs(velocityAtSource()*arrivalRayParam - (getTauModel().getRadiusOfEarth()-sourceDepth)) < 0.05) {
            // due to rounding/interpolation, arg for asin for horizontal ray can be ever so slightly greater than one
            // just set takeoffAngle to 90 in this case
            takeoffAngle = Math.PI/2;
        }
        if ( !getInitialPhaseSegment().isDownGoing) {
            // upgoing, so angle is in 90-180 range
            takeoffAngle = Math.PI-takeoffAngle;
        }
        return takeoffAngle;
    }

    @Override
    public double calcIncidentAngleDegree(double arrivalRayParam) {
        return calcIncidentAngle(arrivalRayParam) * RtoD;
    }

    @Override
    public double calcIncidentAngle(double arrivalRayParam) {
        if (name.endsWith("kmps")) {
            return 0;
        }
        double incidentAngle = Math.asin(velocityAtReceiver()*arrivalRayParam/(getTauModel().getRadiusOfEarth()-receiverDepth));
        if (! Double.isFinite(incidentAngle)
                && Math.abs(velocityAtSource()*arrivalRayParam - (getTauModel().getRadiusOfEarth()-receiverDepth)) < 0.05) {
            // due to rounding/interpolation, arg for asin for horizontal ray can be ever so slightly greater than one
            // just set incidentAngle to 90 in this case
            incidentAngle = Math.PI/2;
        }
        if (getFinalPhaseSegment().isDownGoing) {
            incidentAngle = Math.PI - incidentAngle;
        }
        return incidentAngle;
    }

    @Override
    public boolean sourceSegmentIsPWave() {
        return getPhaseSegments().get(0).isPWave;
    }


    public SeismicPhaseSegment getInitialPhaseSegment() {
        return getPhaseSegments().get(0);
    }

    public SeismicPhaseSegment getFinalPhaseSegment() {
        return getPhaseSegments().get(getPhaseSegments().size()-1);
    }

    @Override
    public boolean finalSegmentIsPWave() {
        return getPhaseSegments().get(getPhaseSegments().size()-1).isPWave;
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
        if (getName().endsWith("kmps")) {
            pierce.add(new TimeDist(distRayParam,
                    0.0,
                    0.0,
                    0.0));
        } else {
            /* First pierce point is always 0 distance at the source depth. */
            pierce.add(new TimeDist(distRayParam,
                    0.0,
                    0.0,
                    tMod.getSourceDepth()));
        }
        /*
         * Loop from 0 but already done 0, so the pierce point when the ray
         * leaves branch i is stored in i+1. Use linear interpolation
         * between rays that we know.
         */
        for (SeismicPhaseSegment seg : getPhaseSegments()) {
            boolean isPWave = seg.isPWave;
            int indexIncr = seg.isDownGoing ? 1 : -1;
            int finish = seg.endBranch + indexIncr;
            for (int branchNum = seg.startBranch; branchNum != finish; branchNum += indexIncr) {
                if (DEBUG) {
                    System.err.println(seg);
                }

                if (seg.isFlat) {
                    double refractDist = (currArrival.getDist() - dist[0]) / countFlatLegs();
                    double refractTime = refractDist * currArrival.getRayParam();
                    pierce.add(new TimeDist(distRayParam,
                            branchTime + refractTime,
                            negMulDist * (branchDist + refractDist),
                            seg.getDepthRange()[0]));
                    branchDist += refractDist;
                    prevBranchTime = branchTime;
                    branchTime += refractTime;
                } else {
                    /*
                     * Save the turning depths for the ray parameter for both P and
                     * S waves. This way we get the depth correct for any rays that
                     * turn within a layer. We have to do this on a per branch basis
                     * because of converted phases, e.g. SKS.
                     */
                    double turnDepth;
                    try {
                        if (distRayParam > tMod.getTauBranch(branchNum, isPWave)
                                .getMaxRayParam()) {
                            turnDepth = tMod.getTauBranch(branchNum, isPWave)
                                    .getTopDepth();
                        } else if (distRayParam <= tMod.getTauBranch(branchNum,
                                        isPWave)
                                .getMinRayParam()) {
                            turnDepth = tMod.getTauBranch(branchNum, isPWave)
                                    .getBotDepth();
                        } else {
                            if (isPWave
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
                    } catch (SlownessModelException e) {
                        // shouldn't happen but...
                        throw new RuntimeException("SeismicPhase.calcPierce: Caught SlownessModelException. "
                                , e);
                    }
                    double timeA, timeB;
                    if (countFlatLegs() > 0) {
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
                    if (seg.isDownGoing) {
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
                    if (Math.abs(prevBranchTime - branchTime) > 1e-10) {
                        pierce.add(new TimeDist(distRayParam,
                                branchTime,
                                negMulDist * branchDist,
                                branchDepth));
                        if (DEBUG) {
                            System.err.println("------->  add pierce " + branchDepth);
                            System.err.println(" branchTime=" + branchTime
                                    + " branchDist=" + branchDist + " branchDepth="
                                    + branchDepth);
                            System.err.println("incrementTime = "
                                    + (distRatio * (timeB - timeA)) + " timeB="
                                    + timeB + " timeA=" + timeA);
                        }
                    } else {
                        if (DEBUG) {
                            System.err.println("Time inc in branch tiny: " + " branchTime=" + branchTime
                                    + " branchDist=" + branchDist + " branchDepth="
                                    + branchDepth);
                        }
                    }
                    if (seg.isFlat) {
                        double refractDist = (currArrival.getDist() - dist[0]) / countFlatLegs();
                        double refractTime = refractDist * currArrival.getRayParam();
                        pierce.add(new TimeDist(distRayParam,
                                branchTime + refractTime,
                                negMulDist * (branchDist + refractDist),
                                branchDepth));
                        branchDist += refractDist;
                        branchTime += refractTime;
                    }
                }
            }
        }
        return pierce;
    }

    @Override
    public int getNumRays() {
        return getRayParams().length;
    }

    /** True is all segments of this path are only P waves.
     *
     * @return
     */
    @Override
    public boolean isAllPWave() {
        for (SeismicPhaseSegment seg: getPhaseSegments()) {
            if ( ! seg.isPWave) { return false; }
        }
        return true;
    }
    /** True is all segments of this path are only S waves.
     *
     * @return
     */
    @Override
    public boolean isAllSWave() {
        for (SeismicPhaseSegment seg: getPhaseSegments()) {
            if (seg.isPWave) { return false; }
        }
        return true;
    }

    @Override
    public double calcReflTranPSV(Arrival arrival) throws VelocityModelException, SlownessModelException {
        double reflTranValue = 1;
        boolean calcSH = false;
        SeismicPhaseSegment prevSeg = getPhaseSegments().get(0);
        for (SeismicPhaseSegment seg : getPhaseSegments().subList(1, getPhaseSegments().size())) {
            reflTranValue *= prevSeg.calcReflTran(arrival, seg.isPWave, calcSH);
            prevSeg = seg;
        }
        reflTranValue *= prevSeg.calcReflTran(arrival, prevSeg.isPWave, calcSH); // last seg can't change phase at end
        return reflTranValue;
    }

    @Override
    public double calcReflTranSH(Arrival arrival) throws VelocityModelException, SlownessModelException {
        double reflTranValue = 1;

        boolean isAllS = isAllSWave();
        if ( ! isAllS) { return 0; }
        boolean calcSH = true;
        SeismicPhaseSegment prevSeg = getPhaseSegments().get(0);
        for (SeismicPhaseSegment seg : getPhaseSegments().subList(1, getPhaseSegments().size())) {
            reflTranValue *= prevSeg.calcReflTran(arrival, seg.isPWave, calcSH);
            prevSeg = seg;
        }
        reflTranValue *= prevSeg.calcReflTran(arrival, prevSeg.isPWave, calcSH); // last seg can't change phase at end
        return reflTranValue;
    }

    @Override
    public List<ArrivalPathSegment> calcSegmentPaths(Arrival currArrival) {
        return calcSegmentPaths(currArrival, null, 0);
    }

    /**
     * Calc path with a starting time-distance possibly not zero. Used when this simple phase
     * is the outbound phase of a scattered phase and so the path needs to start at the
     * scatterer distance.
     *
     * @param currArrival
     * @param prevEnd
     * @return
     */
    public List<ArrivalPathSegment> calcSegmentPaths(Arrival currArrival, TimeDist prevEnd, int prevIdx) {
        int idx = prevIdx+1;
        List<ArrivalPathSegment> segmentPaths = new ArrayList<>();
        for (SeismicPhaseSegment seg : getPhaseSegments()) {
            ArrivalPathSegment segPath = seg.calcPathTimeDist(currArrival, prevEnd, idx++, prevIdx+ getPhaseSegments().size());

            if (segPath.path.size() == 0) {
                System.err.println("segPath.size() is 0 "+seg);
                continue;
            }
            segmentPaths.add(segPath);
            prevEnd = segPath.getPathEnd();
        }
        return ArrivalPathSegment.adjustPath(segmentPaths, currArrival);
    }

    public static List<TimeDist> removeDuplicatePathPoints(List<TimeDist> inPath) {
        List<TimeDist> outPath = new ArrayList<TimeDist>();
        if (inPath.size() != 0) {
            TimeDist prev = inPath.get(0);
            outPath.add(prev);
            for (TimeDist td : inPath) {
                if (!td.equals(prev)) {
                    outPath.add(td);
                    prev = td;
                }
            }
        }
        return outPath;
    }

    @Override
    public String describe() {
        String desc = getName() +(getName().equals(getPuristName()) ? "" : (" ("+getPuristName()+")"))+ ":\n";
        return desc+ SeismicPhase.baseDescribe(this)+"\n"+ SeismicPhase.segmentDescribe(this);
    }

    @Override
    public String describeJson() {
        String Q = ""+'"';
        String COMMA = ",";
        String QCOMMA = Q+COMMA;
        String COLON = ": "; // plus space
        String S = "  ";
        String QC = Q+COLON;
        String QCQ = QC+Q;
        String SS = S+S;
        String SQ = S+Q;
        String SSQ = S+SQ;
        String SSSQ = S+SSQ;
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println("{");
        out.println(SQ+"name"+QCQ+getName()+QCOMMA);
        out.println(SQ+"puristname"+QCQ+getPuristName()+QCOMMA);
        out.println(SeismicPhase.baseDescribeJSON(this));
        out.println(",");
        out.println(SeismicPhase.segmentDescribeJSON(this));
        out.println("}");
        return sw.toString();
    }

    @Override
    public String toString() {
        String desc = name + ": ";
        for (SeismicPhaseSegment seg : getPhaseSegments()) {
            desc += seg.legName+" ";
        }
        desc += "\n";
        desc += proto.branchNumSeqStr();
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
}
