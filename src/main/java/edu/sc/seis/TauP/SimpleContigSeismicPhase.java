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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static edu.sc.seis.TauP.PhaseInteraction.TRANSUPDIFFRACT;
import static edu.sc.seis.TauP.SphericalCoords.DtoR;
import static edu.sc.seis.TauP.SphericalCoords.RtoD;

/**
 * Stores and transforms seismic phase names to and from their corresponding
 * sequence of branches.
 *
 */
public class SimpleContigSeismicPhase extends SimpleSeismicPhase {

    ProtoSeismicPhase proto;

    /**
     * Enables debugging output.
     */
    public transient boolean DEBUG;

    /**
     * Enables verbose output.
     */
    public transient boolean verbose = false;

    /**
     * TauModel to generate phase for.
     */
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

    /**
     * Array of possible ray parameters for this phase.
     */
    protected double[] rayParams;

    /**
     * Minimum ray parameter that exists for this phase.
     */
    protected double minRayParam;

    /**
     * Maximum ray parameter that exists for this phase.
     */
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

    /**
     * The minimum distance that this phase can be theoretically observed.
     */
    protected double minDistance;

    /**
     * The maximum distance that this phase can be theoretically observed.
     */
    protected double maxDistance;

    /**
     * The phase name, ie PKiKP.
     */
    protected String name;

    /**
     * name with depths corrected to be actuall discontinuities in the model.
     */
    protected String puristName;

    protected double refineDistToleranceRadian = 0.0049 * Math.PI / 180;

    /**
     * Maximum recursion for refine arrival.
     */
    protected int maxRecursion = 5;


    public SimpleContigSeismicPhase(ProtoSeismicPhase proto,
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
        if (proto == null) {
            throw new IllegalArgumentException("proto cannot be null");
        }
        try {
            if (proto.isSuccessful()) {
                proto.validateSegList();
            }
        } catch (TauModelException e) {
            throw new RuntimeException(getName() + " fail validation:", e);
        }
        this.proto = proto;
        this.DEBUG = debug;
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
        if (time.length == 0) {
            return -1;
        }
        double v = time[0];
        for (double d : time) {
            v = Math.min(v, d);
        }
        return v;
    }

    public double getMaxTime() {
        if (time.length == 0) {
            return -1;
        }
        double v = time[0];
        for (double d : time) {
            v = Math.max(v, d);
        }
        return v;
    }

    public ProtoSeismicPhase getProto() {
        return proto;
    }

    @Override
    public boolean isFail() {
        return proto.isFail;
    }

    @Override
    public String failReason() {
        return proto.failReason;
    }

    public TauModel gettMod() {
        return tMod;
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

    @Override
    public List<List<SeismicPhaseSegment>> getListPhaseSegments() {
        return List.of(getPhaseSegments());
    }

    /**
     * Description of segments of the phase.
     */
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
        for (int i = 0; i < dist.length; i++) {
            tau[i] = time[i] - rayParams[i] * dist[i];
        }
        return tau;
    }

    @Override
    public boolean hasArrivals() {
        return dist != null && dist.length != 0;
    }

    public List<ShadowZone> getShadowZones() {
        return new ArrayList<>();
    }

    // Normal methods

    /**
     * Calculates arrivals for this phase, but only for the exact distance in radians. This does not check multiple
     * laps nor going the long way around.
     */
    public List<Arrival> calcTimeExactDistance(double searchDist) {
        List<Arrival> arrivals = new ArrayList<>();
        for (int rayNum = 0; rayNum < (dist.length - 1); rayNum++) {
            if (searchDist == dist[rayNum + 1]
                    && rayNum + 1 != dist.length - 1) {
                /* So we don't get 2 arrivals for the same ray. */
                continue;
            } else if ((dist[rayNum] - searchDist)
                    * (searchDist - dist[rayNum + 1]) >= 0.0) {
                /* look for distances that bracket the search distance */
                if ((rayParams[rayNum] == rayParams[rayNum + 1])
                        && getMaxRayParam() > getMinRayParam()) {
                    /*
                     * Here we have a shadow zone, so it is not really an
                     * arrival.
                     */
                    continue;
                }
                if (DEBUG) {
                    Alert.debug("SeismicPhase " + name
                            + ", found arrival:\n" + "dist "
                            + (float) (180 / Math.PI * dist[rayNum]) + " "
                            + (float) (180 / Math.PI * searchDist) + " "
                            + (float) (180 / Math.PI * dist[rayNum + 1]));
                    Alert.debug("time "
                            + time[rayNum] + " --  "
                            + time[rayNum + 1]);
                }
                arrivals.add(refineArrival(rayNum, searchDist, refineDistToleranceRadian, maxRecursion));

            }
        }
        return arrivals;
    }

    /**
     * Creates an Arrival for a sampled ray parameter from the model. No interpolation between rays as this is a sample.
     *
     * @param rayNum
     * @return
     */
    public Arrival createArrivalAtIndex(int rayNum) {
        double dRPdDist = 0;
        if (rayParams.length > 1) {
            if (rayNum == 0) {
                dRPdDist = (getRayParams(rayNum) - getRayParams(rayNum + 1)) / (getDist(rayNum) - getDist(rayNum + 1));
            } else if (rayNum == rayParams.length - 1) {
                dRPdDist = (getRayParams(rayNum) - getRayParams(rayNum - 1)) / (getDist(rayNum) - getDist(rayNum - 1));
            } else {
                // average left and right ray params
                dRPdDist = ((getRayParams(rayNum) - getRayParams(rayNum - 1)) / (getDist(rayNum) - getDist(rayNum - 1))
                        + (getRayParams(rayNum) - getRayParams(rayNum + 1)) / (getDist(rayNum) - getDist(rayNum + 1)))
                        / 2.0;
            }

        }
        return new Arrival(this,
                this, getTime(rayNum),
                getDist(rayNum),
                getRayParams(rayNum),
                rayNum,
                dRPdDist
        );
    }

    public Arrival refineArrival(int rayNum, double distRadian, double distTolRadian, int maxRecursion) {
        Arrival left = createArrivalAtIndex(rayNum);
        Arrival right = createArrivalAtIndex(rayNum + 1);
        return refineArrival(left, right, distRadian, distTolRadian, maxRecursion);
    }

    public Arrival refineArrival(Arrival leftEstimate, Arrival rightEstimate, double searchDist, double distTolRadian, int maxRecursion) {
        Arrival linInterp = linearInterpArrival(searchDist, leftEstimate, rightEstimate);
        if (maxRecursion <= 0 || countFlatLegs() > 0) {
            // can't shoot/refine for diffracted, head and non-body waves
            return linInterp;
        }
        if (leftEstimate.getRayParam() == rightEstimate.getRayParam()) {
            return linInterp;
        }
        if (linInterp.getRayParam() == leftEstimate.getRayParam()) {
            return leftEstimate;
        }
        if (linInterp.getRayParam() == rightEstimate.getRayParam()) {
            return rightEstimate;
        }

        if (DEBUG) {
            Alert.debug("Phase: " + this);
            Alert.debug("Refine: " + maxRecursion + "\nleft:  " + leftEstimate + "\nright: " + rightEstimate + "\nlinInterp: " + linInterp);
        }

        if (leftEstimate.getRayParam() < minRayParam || maxRayParam < leftEstimate.getRayParam()) {
            throw new RuntimeException("Left Ray param " + leftEstimate.getRayParam() + " is outside range for this phase: " + getName() + " min=" + minRayParam + " max=" + maxRayParam);
        }
        if (rightEstimate.getRayParam() < minRayParam || maxRayParam < rightEstimate.getRayParam()) {
            throw new RuntimeException("Right Ray param " + rightEstimate.getRayParam() + " is outside range for this phase: " + getName() + " min=" + minRayParam + " max=" + maxRayParam);
        }

        try {
            Arrival shoot = shootRay(linInterp.getRayParam());
            if ((leftEstimate.getDist() - searchDist)
                    * (searchDist - shoot.getDist()) > 0) {
                // search between left and shoot
                if (Math.abs(shoot.getDist() - linInterp.getDist()) < distTolRadian) {
                    return linearInterpArrival(searchDist, leftEstimate, shoot);
                } else {
                    return refineArrival(leftEstimate, shoot, searchDist, distTolRadian, maxRecursion - 1);
                }
            } else {
                // search between shoot and right
                if (Math.abs(shoot.getDist() - linInterp.getDist()) < distTolRadian) {
                    return linearInterpArrival(searchDist, shoot, rightEstimate);
                } else {
                    return refineArrival(shoot, rightEstimate, searchDist, distTolRadian, maxRecursion - 1);
                }
            }
        } catch (TauModelException | SlownessModelException e) {
            throw new RuntimeException("Should not happen: " + getName(), e);
        }
    }

    @Override
    public SimpleContigSeismicPhase interpolatePhase(double maxDeltaDeg) {
        return interpolateSimplePhase(maxDeltaDeg);
    }

    /**
     * Interpolates the time-dist arrays so adjacent rays are no more than maxDeltaDeg apart.
     *
     * @param maxDeltaDeg max separation in degrees
     * @return new phase interpolated
     */
    public SimpleContigSeismicPhase interpolateSimplePhase(double maxDeltaDeg) {
        int numToAdd = 0;
        double maxDeltaRadian = maxDeltaDeg * DtoR;
        for (int i = 0; i < rayParams.length - 1; i++) {
            if (Math.abs(dist[i] - dist[i + 1]) > maxDeltaRadian) {

                numToAdd += (int) Math.ceil(Math.abs(dist[i + 1] - dist[i]) / maxDeltaRadian) - 1;
            }
        }
        double[] out_rayParams = new double[rayParams.length + numToAdd];
        double[] out_dist = new double[rayParams.length + numToAdd];
        double[] out_time = new double[rayParams.length + numToAdd];
        int shift = 0;
        for (int i = 0; i < rayParams.length - 1; i++) {
            out_dist[i + shift] = dist[i];
            out_rayParams[i + shift] = rayParams[i];
            out_time[i + shift] = time[i];

            int numRPs = (int) Math.ceil(Math.abs(dist[i + 1] - dist[i]) / maxDeltaRadian);
            double deltaDist = (dist[i + 1] - dist[i]) / numRPs;
            for (int j = 1; j < numRPs; j++) {
                List<Arrival> aList = DistanceRay.ofExactRadians(dist[i] + j * deltaDist).calcSimplePhase(this);

                for (Arrival a : aList) {
                    if (rayParams[i + 1] <= a.getRayParam() && a.getRayParam() <= rayParams[i]) {
                        shift++;
                        out_dist[i + shift] = a.getDist();
                        out_time[i + shift] = a.getTime();
                        out_rayParams[i + shift] = a.getRayParam();
                        break;
                    }
                }
                if (aList.isEmpty()) {
                    throw new RuntimeException("Bad calc for interp " + j + " " + (dist[i] + j * deltaDist) * RtoD + " for " + getName());
                }
            }

        }
        // add last sample
        out_dist[rayParams.length - 1 + shift] = dist[rayParams.length - 1];
        out_rayParams[rayParams.length - 1 + shift] = rayParams[rayParams.length - 1];
        out_time[rayParams.length - 1 + shift] = time[rayParams.length - 1];
        SimpleContigSeismicPhase out = new SimpleContigSeismicPhase(proto, out_rayParams, out_time, out_dist,
                minRayParam, maxRayParam, minRayParamIndex, maxRayParamIndex, minDistance, maxDistance, false);
        if (shift != numToAdd) {
            throw new RuntimeException("shifty not numAdd " + shift + " " + numToAdd);
        }
        return out;
    }

    @Override
    public Arrival shootRay(double rayParam) throws SlownessModelException, TauModelException {
        System.err.println("SimpleContig shootRay "+rayParam);
        if (countFlatLegs() > 0 && rayParam != getMinRayParam()) {
            throw new TauModelException("Unable to shoot ray in non-body, head, diffracted waves: " + getName());
        }
        if (rayParam < minRayParam || maxRayParam < rayParam) {
            throw new TauModelException("Ray param " + rayParam + " is outside range for this phase: " + getName() + " min=" + minRayParam + " max=" + maxRayParam);
        }
        // looks like a body wave and can ray param can propagate
        int rayParamIndex = -1;
        for (rayParamIndex = 0; rayParamIndex < rayParams.length - 1 && rayParams[rayParamIndex + 1] >= rayParam; rayParamIndex++) {
            // find index for ray param, done in for-loop check
        }

        List<TauBranch> branchList = SeismicPhaseFactory.calcBranchSeqForRayparam(proto, rayParam);
        TimeDist sum = new TimeDist(rayParam, 0, 0, getSourceDepth());
        List<TimeDist> pierce = new ArrayList<>();
        pierce.add(sum);
        for (TauBranch tb : branchList) {
            TimeDist td = tb.calcTimeDist(rayParam, true);
            sum = sum.add(td);
            pierce.add(sum);
        }
        double dRPdDist = (rayParam - rayParams[rayParamIndex]) / (sum.getDistRadian() - dist[rayParamIndex]);
        Arrival a = new Arrival(this,
                this,
                pierce,
                rayParamIndex,
                dRPdDist
        );
        return a;
    }

    /**
     * Interprets between two arrivals to find new arrival at given distance.
     *
     * @param searchDist new arrival distance
     * @param left       known arrival to left
     * @param right      known arrival to right
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
        if (maxRayParam == minRayParam) {
            // degenerate phase, all ray parameters are the same, just interpolate time
            arrivalTime = LinearInterpolation.linearInterp(left.getDist(), left.getTime(),
                    right.getDist(), right.getTime(), searchDist);
            dRPdDist = 0;
        } else if (Math.abs(searchDist - left.getDist()) < Math.abs(searchDist - right.getDist())) {
            arrivalTime = left.getTime() + arrivalRayParam * (searchDist - left.getDist());
            dRPdDist = (left.getRayParam() - arrivalRayParam) / (left.getDist() - searchDist);
        } else {
            arrivalTime = right.getTime() + arrivalRayParam * (searchDist - right.getDist());
            dRPdDist = (right.getRayParam() - arrivalRayParam) / (right.getDist() - searchDist);
        }
        if (Double.isNaN(arrivalTime)) {
            throw new RuntimeException("Time is NaN, search " + searchDist + " leftDist " + left.getDist() + " leftTime " + left.getTime()
                    + "  rightDist " + right.getDist() + "  rightTime " + right.getTime());
        }

        return new Arrival(this,
                this, arrivalTime,
                searchDist,
                arrivalRayParam,
                left.getRayParamIndex(),
                dRPdDist
        );
    }

    @Override
    public double calcRayParamForTakeoffAngle(double takeoffDegree) throws NoArrivalException {
        if (takeoffDegree < 0 || takeoffDegree > 180) {
            throw new IllegalArgumentException("Takeoff angle should be 0 to 180, but was " + takeoffDegree);
        }
        if (getPhaseSegments().isEmpty()) {
            throw new NoArrivalException("No phase segments for " + getName());
        }

        boolean firstIsPWave = getInitialPhaseSegment().isPWave;
        double rayParam;
        try {
            rayParam = calcRayParamForTakeoffAngleInModel(takeoffDegree, firstIsPWave, tMod,
                    getInitialPhaseSegment().isDownGoing);
        } catch (NoSuchLayerException | SlownessModelException e) {
            throw new RuntimeException("Should not happen", e);
        }
        return rayParam;
    }

    public static double calcRayParamForTakeoffAngleInModel(double takeoffDegree,
                                                            boolean isPWave,
                                                            TauModel tMod,
                                                            boolean isDownGoing)
            throws NoSuchLayerException, SlownessModelException {
        if ((isDownGoing && (takeoffDegree > 90))
                || (!isDownGoing && (takeoffDegree < 90))
        ) {
            throw new SlownessModelException("Phase downgoing and takeoff different up/down " + isDownGoing + " " + takeoffDegree);
        }
        SlownessLayer sLayer;
        if (isDownGoing) {
            int layerNum = tMod.getSlownessModel().layerNumberBelow(tMod.getSourceDepth(), isPWave);
            sLayer = tMod.getSlownessModel().getSlownessLayer(layerNum, isPWave);
        } else {
            int layerNum = tMod.getSlownessModel().layerNumberAbove(tMod.getSourceDepth(), isPWave);
            sLayer = tMod.getSlownessModel().getSlownessLayer(layerNum, isPWave);
        }
        double rayParam = sLayer.evaluateAt_bullen(tMod.getSourceDepth(), tMod.radiusOfEarth)
                * Math.sin(takeoffDegree * SphericalCoords.DtoR);
        return rayParam;
    }


    @Override
    public double calcRayParamForIncidentAngle(double incidentDegree) throws NoArrivalException {
        if (incidentDegree < 0 || incidentDegree > 180) {
            throw new IllegalArgumentException("Takeoff angle should be 0 to 180, but was " + incidentDegree);
        }
        if (getPhaseSegments().isEmpty()) {
            throw new NoArrivalException("No phase segments for " + getName());
        }

        boolean firstIsPWave = getFinalPhaseSegment().isPWave;
        double rayParam;
        try {
            rayParam = calcRayParamForIncidentAngleInModel(incidentDegree, firstIsPWave, getProto(),
                    getFinalPhaseSegment().isDownGoing);
        } catch (NoSuchLayerException | SlownessModelException e) {
            throw new RuntimeException("Should not happen", e);
        }
        return rayParam;
    }

    public static double calcRayParamForIncidentAngleInModel(double incidentDegree,
                                                             boolean isPWave,
                                                             ProtoSeismicPhase proto,
                                                             boolean isDownGoing)
            throws NoSuchLayerException, SlownessModelException {
        if ((isDownGoing && (incidentDegree < 90))
                || (!isDownGoing && (incidentDegree > 90))
        ) {
            throw new SlownessModelException("Phase ends downgoing and incident different up/down " + isDownGoing + " " + incidentDegree);
        }
        SlownessLayer sLayer;
        if (!isDownGoing) {
            int layerNum = proto.tMod.getSlownessModel().layerNumberBelow(proto.receiverDepth, isPWave);
            sLayer = proto.tMod.getSlownessModel().getSlownessLayer(layerNum, isPWave);
        } else {
            int layerNum = proto.tMod.getSlownessModel().layerNumberAbove(proto.receiverDepth, isPWave);
            sLayer = proto.tMod.getSlownessModel().getSlownessLayer(layerNum, isPWave);
        }
        double rayParam = sLayer.evaluateAt_bullen(proto.receiverDepth, proto.tMod.radiusOfEarth)
                * Math.sin(incidentDegree * SphericalCoords.DtoR);
        return rayParam;
    }

    @Override
    public double velocityAtSource() {
        try {
            double takeoffVelocity;
            VelocityModelMaterial firstLeg;
            VelocityModel vMod = getTauModel().getVelocityModel();
            if (getInitialPhaseSegment().isPWave) {
                firstLeg = VelocityModelMaterial.P_VELOCITY;
            } else {
                firstLeg = VelocityModelMaterial.S_VELOCITY;
            }
            if (getInitialPhaseSegment().isDownGoing) {
                takeoffVelocity = vMod.evaluateBelow(sourceDepth, firstLeg);
            } else {
                takeoffVelocity = vMod.evaluateAbove(sourceDepth, firstLeg);
            }
            return takeoffVelocity;
        } catch (NoSuchLayerException e) {
            throw new RuntimeException("Should not happen", e);
        }
    }

    @Override
    public double velocityAtReceiver() {
        try {
            double incidentVelocity;
            VelocityModel vMod = getTauModel().getVelocityModel();
            VelocityModelMaterial lastLeg;
            if (getPhaseSegments().get(getPhaseSegments().size() - 1).isPWave) {
                lastLeg = VelocityModelMaterial.P_VELOCITY;
            } else {
                lastLeg = VelocityModelMaterial.S_VELOCITY;
            }
            if (getPhaseSegments().get(getPhaseSegments().size() - 1).isDownGoing) {
                incidentVelocity = vMod.evaluateAbove(receiverDepth, lastLeg);
            } else {
                incidentVelocity = vMod.evaluateBelow(receiverDepth, lastLeg);
            }
            return incidentVelocity;
        } catch (NoSuchLayerException e) {
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
        } catch (NoSuchLayerException e) {
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
        } catch (NoSuchLayerException e) {
            throw new RuntimeException("Should not happen", e);
        }
    }

    @Override
    public double calcTakeoffAngleDegree(double arrivalRayParam) {
        return calcTakeoffAngle(arrivalRayParam) * RtoD;
    }

    @Override
    public double calcTakeoffAngle(double arrivalRayParam) {
        if (name.endsWith("kmps")) {
            return 0;
        }
        double takeoffAngle = Math.asin(velocityAtSource() * arrivalRayParam / (getTauModel().getRadiusOfEarth() - sourceDepth));
        if (!Double.isFinite(takeoffAngle)
                && Math.abs(velocityAtSource() * arrivalRayParam - (getTauModel().getRadiusOfEarth() - sourceDepth)) < 0.05) {
            // due to rounding/interpolation, arg for asin for horizontal ray can be ever so slightly greater than one
            // just set takeoffAngle to 90 in this case
            takeoffAngle = Math.PI / 2;
        }
        if (!getInitialPhaseSegment().isDownGoing) {
            // upgoing, so angle is in 90-180 range
            takeoffAngle = Math.PI - takeoffAngle;
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
        double incidentAngle = Math.asin(velocityAtReceiver() * arrivalRayParam / (getTauModel().getRadiusOfEarth() - receiverDepth));
        if (!Double.isFinite(incidentAngle)
                && Math.abs(velocityAtSource() * arrivalRayParam - (getTauModel().getRadiusOfEarth() - receiverDepth)) < 0.05) {
            // due to rounding/interpolation, arg for asin for horizontal ray can be ever so slightly greater than one
            // just set incidentAngle to 90 in this case
            incidentAngle = Math.PI / 2;
        }
        if (getFinalPhaseSegment().isDownGoing) {
            incidentAngle = Math.PI - incidentAngle;
        }
        return incidentAngle;
    }

    @Override
    public boolean sourceSegmentIsPWave() {
        return getInitialPhaseSegment().isPWave;
    }


    public SeismicPhaseSegment getInitialPhaseSegment() {
        return getPhaseSegments().get(0);
    }

    public SeismicPhaseSegment getFinalPhaseSegment() {
        return getPhaseSegments().get(getPhaseSegments().size() - 1);
    }

    @Override
    public boolean finalSegmentIsPWave() {
        return getPhaseSegments().get(getPhaseSegments().size() - 1).isPWave;
    }


    public List<TimeDist> calcPierceTimeDist(Arrival currArrival) {
        double branchDist = 0.0;
        double branchTime = 0.0;
        double prevBranchTime;
        List<TimeDist> pierce = new ArrayList<>();
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
                                                SeismicPhase.PWAVE);
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
                        if (true || DEBUG) {
                            Alert.debug("------->  add pierce " + branchDepth+"  bnum="+branchNum);
                            Alert.debug(" branchTime=" + branchTime
                                    + " branchDist=" + branchDist + " branchDepth="
                                    + branchDepth);
                            Alert.debug("incrementTime = "
                                    + (distRatio * (timeB - timeA)) + " timeB="
                                    + timeB + " timeA=" + timeA);
                        }
                    } else {
                        if (DEBUG) {
                            Alert.debug("Time inc in branch tiny: " + " branchTime=" + branchTime
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

    public double calcTstar(Arrival currArrival) {
        // path tstar
        double tstar = 0;
        TauModel tMod = getTauModel();
        VelocityModel vMod = tMod.getVelocityModel();
        List<ArrivalPathSegment> pathSegList = currArrival.getPathSegments();
        TimeDist prev = new TimeDist();
        try {
            for (ArrivalPathSegment pseg : pathSegList) {
                for (TimeDist td : pseg.getPath()) {
                    double timeInc = td.getTime()-prev.getTime();
                    double Q;
                    if (pseg.getPhaseSegment().isFlat) {
                        // does tstar make sense for flat ray???
                        if (pseg.getPhaseSegment().prevEndAction == PhaseInteraction.DIFFRACT
                                || pseg.getPhaseSegment().prevEndAction == TRANSUPDIFFRACT) {
                            Q = vMod.evaluateAbove(td.getDepth(), pseg.isPWave ? VelocityModelMaterial.Q_P : VelocityModelMaterial.Q_S);
                        } else if (pseg.getPhaseSegment().prevEndAction == PhaseInteraction.HEAD) {
                            Q = vMod.evaluateBelow(td.getDepth(), pseg.isPWave ? VelocityModelMaterial.Q_P : VelocityModelMaterial.Q_S);
                        } else {
                            throw new RuntimeException("tstar unknown for flat for prevendaction= "+pseg.getPhaseSegment().prevEndAction);
                        }
                    } else if (td.getDepth() == pseg.getPhaseSegment().getBotDepth()) {
                        // careful of turning at bottom, use above istead of below
                        Q = vMod.evaluateAbove(td.getDepth(), pseg.isPWave ? VelocityModelMaterial.Q_P : VelocityModelMaterial.Q_S);
                    } else {
                        Q = vMod.evaluateBelow(td.getDepth(), pseg.isPWave ? VelocityModelMaterial.Q_P : VelocityModelMaterial.Q_S);
                    }
                    if (Q <= 0) {
                        throw new RuntimeException("Q <= 0 for "+getName()+" "+td+"   depthrange: "
                                +pseg.getPhaseSegment().getDepthRange()[0]+" "+(pseg.getPhaseSegment().getDepthRange()[1]));
                    }
                    tstar += timeInc / Q;
                    prev = td;
                }
            }

        } catch (NoSuchLayerException e) {
            // should never happen...
            throw new RuntimeException("Can't find vel layer for tau branch? depth", e);
        }
        return tstar;
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
    public double calcEnergyFluxFactorReflTranPSV(Arrival arrival) throws VelocityModelException {
        double reflTranValue = 1;
        boolean calcSH = false;
        SeismicPhaseSegment prevSeg = getPhaseSegments().get(0);
        for (SeismicPhaseSegment seg : getPhaseSegments().subList(1, getPhaseSegments().size())) {

            reflTranValue *= prevSeg.calcEnergyFluxFactorReflTran(arrival, seg.isPWave, calcSH);
            prevSeg = seg;
        }
        reflTranValue *= prevSeg.calcEnergyFluxFactorReflTran(arrival, prevSeg.isPWave, calcSH); // last seg can't change phase at end
        return reflTranValue;
    }

    @Override
    public double calcEnergyFluxFactorReflTranSH(Arrival arrival) throws VelocityModelException {
        double reflTranValue = 1;

        boolean isAllS = isAllSWave();
        if ( ! isAllS) { return 0; }
        boolean calcSH = true;
        SeismicPhaseSegment prevSeg = getPhaseSegments().get(0);
        for (SeismicPhaseSegment seg : getPhaseSegments().subList(1, getPhaseSegments().size())) {
            reflTranValue *= prevSeg.calcEnergyFluxFactorReflTran(arrival, seg.isPWave, calcSH);
            prevSeg = seg;
        }
        reflTranValue *= prevSeg.calcEnergyFluxFactorReflTran(arrival, prevSeg.isPWave, calcSH); // last seg can't change phase at end
        return reflTranValue;
    }

    public static List<TimeDist> removeDuplicatePathPoints(List<TimeDist> inPath) {
        List<TimeDist> outPath = new ArrayList<>();
        if (!inPath.isEmpty()) {
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
        return desc+ SeismicPhase.baseDescribe(this)+"\n"+
                SeismicPhaseSegment.segmentDescribe(this.getPhaseSegments());
    }

    @Override
    public String describeShort() {
        String desc = getName() +(getName().equals(getPuristName()) ? "" : (" ("+getPuristName()+")"))
                + " source: "+getSourceDepth()+" km, receiver: "+getReceiverDepth()+" km";
        return desc;
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
