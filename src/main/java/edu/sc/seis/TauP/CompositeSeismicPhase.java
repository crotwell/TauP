package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

/**
 * Seismic Phase that is simple, but may contain shadow zones due to high slowness layers (low velocity zones) in the model.
 */
public class CompositeSeismicPhase extends SimpleSeismicPhase {

    private final List<SimpleContigSeismicPhase> simplePhaseList;

    private final List<ShadowZone> shadowZones = new ArrayList<>();

    public CompositeSeismicPhase(List<SimpleContigSeismicPhase> subphaseList) {
        this.simplePhaseList = subphaseList;
        if (simplePhaseList.size() < 2) {
            throw new IllegalArgumentException("Subphase list must not be empty or single: "+simplePhaseList.size());
        }
        SimpleContigSeismicPhase prev = null;
        for (SimpleContigSeismicPhase subphase : subphaseList) {
            if (prev != null ) {
                if (prev.tMod != subphase.tMod) {
                    throw new IllegalArgumentException("subphases must all have same TauModel "+prev.tMod.getModelName()+" "+subphase.tMod.getModelName());
                }
                if (prev.getName() != subphase.getName()) {
                    throw new IllegalArgumentException("subphases must all have same name "+prev.getName()+" "+subphase.getName());
                }
                if (prev.getMinRayParam() != subphase.getMaxRayParam()) {
                    throw new IllegalArgumentException("subphase are not adjacent in ray param: "+
                            prev.getMinRayParam()+" "+subphase.getMaxRayParam());
                }
                shadowZones.add(new ShadowZone(this, prev.getMinRayParam(),
                        prev.createArrivalAtIndex(prev.getNumRays()-1),
                        subphase.createArrivalAtIndex(0)
                ));

            }
            prev = subphase;
        }
    }

    public List<SimpleContigSeismicPhase> getSubPhaseList() {
        return simplePhaseList;
    }

    @Override
    public List<ShadowZone> getShadowZones() {
        return shadowZones;
    }

    @Override
    public SimpleSeismicPhase interpolateSimplePhase(double maxDeltaDeg) {
        List<SimpleContigSeismicPhase> interpList = new ArrayList<>();
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            interpList.add(sp.interpolateSimplePhase(maxDeltaDeg));
        }
        return new CompositeSeismicPhase(interpList);
    }

    @Override
    public boolean isFail() {
        return !phasesExistsInModel();
    }

    @Override
    public String failReason() {
        if (!isFail()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            sb.append(sp.failReason()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public boolean phasesExistsInModel() {
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            if (sp.phasesExistsInModel()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Arrival getEarliestArrival(double degrees) {
        Arrival early = null;
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            Arrival spArrival = sp.getEarliestArrival(degrees);
            if (early == null || spArrival.getTime() < early.getTime()) {
                early = spArrival;
            }
        }
        return early;
    }

    @Override
    public TauModel getTauModel() {
        return simplePhaseList.get(0).tMod;
    }

    @Override
    public double getMinDistanceDeg() {
        return getMinDistance() * 180.0 / Math.PI;
    }

    @Override
    public double getMinDistance() {
        double minDist = Double.MAX_VALUE;
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            minDist = Math.min(minDist, sp.getMinDistance());
        }
        return minDist;
    }

    @Override
    public double getMaxDistanceDeg() {
        return getMaxDistance() * 180.0 / Math.PI;
    }

    @Override
    public double getMaxDistance() {
        double maxDist = -1*Double.MAX_VALUE;
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            maxDist = Math.max(maxDist, sp.getMaxDistance());
        }
        return maxDist;
    }

    @Override
    public double getMaxRayParam() {
        double maxRP = -1*Double.MAX_VALUE;
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            maxRP = Math.max(maxRP, sp.getMaxRayParam());
        }
        return maxRP;
    }

    @Override
    public double getMinRayParam() {
        double minRP = Double.MAX_VALUE;
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            minRP = Math.min(minRP, sp.getMinRayParam());
        }
        return minRP;
    }

    @Override
    public double getMinTime() {
        double minTime = Double.MAX_VALUE;
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            minTime = Math.min(minTime, sp.getMinTime());
        }
        return minTime;
    }

    @Override
    public double getMaxTime() {
        double maxTime = -1*Double.MAX_VALUE;
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            maxTime = Math.max(maxTime, sp.getMaxTime());
        }
        return maxTime;
    }

    @Override
    public String getName() {
        return simplePhaseList.get(0).getName();
    }

    @Override
    public String getPuristName() {
        return simplePhaseList.get(0).getPuristName();
    }

    @Override
    public double getSourceDepth() {
        return simplePhaseList.get(0).getSourceDepth();
    }

    @Override
    public double getReceiverDepth() {
        return simplePhaseList.get(0).getReceiverDepth();
    }


    @Override
    public boolean hasArrivals() {
        for (SimpleSeismicPhase sp : simplePhaseList) {
            if (sp.hasArrivals()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getMaxRayParamIndex() {
        int idx = Integer.MAX_VALUE;
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            // min as index of minRayParam > index of maxRayParam
            idx = Integer.min(idx, sp.getMaxRayParamIndex());
        }
        return idx;
    }

    @Override
    public int getMinRayParamIndex() {
        int idx = -1*Integer.MAX_VALUE;
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            // max as index of minRayParam > index of maxRayParam
            idx = Integer.max(idx, sp.getMinRayParamIndex());
        }
        return idx;
    }

    @Override
    public List<SeismicPhaseSegment> getPhaseSegments() {
        throw new RuntimeException("getPhaseSegments no impl for CompositeSeismicPhase");
    }

    @Override
    public SeismicPhaseSegment getInitialPhaseSegment() {
        return getSubPhaseList().get(0).getInitialPhaseSegment();
    }

    @Override
    public SeismicPhaseSegment getFinalPhaseSegment() {
        return getSubPhaseList().get(0).getFinalPhaseSegment();
    }

    @Override
    public int countFlatLegs() {
        throw new RuntimeException("getPhaseSegments no impl for CompositeSeismicPhase");
    }

    @Override
    public double getRayParams(int i) {
        throw new RuntimeException("getPhaseSegments no impl for CompositeSeismicPhase");
    }

    @Override
    public double[] getRayParams() {
        throw new RuntimeException("getPhaseSegments no impl for CompositeSeismicPhase");
    }

    @Override
    public double getDist(int i) {
        throw new RuntimeException("getPhaseSegments no impl for CompositeSeismicPhase");
    }

    @Override
    public double[] getDist() {
        throw new RuntimeException("getPhaseSegments no impl for CompositeSeismicPhase");
    }

    @Override
    public double getTime(int i) {
        throw new RuntimeException("getPhaseSegments no impl for CompositeSeismicPhase");
    }

    @Override
    public double[] getTime() {
        throw new RuntimeException("getPhaseSegments no impl for CompositeSeismicPhase");
    }

    @Override
    public double getTau(int i) {
        throw new RuntimeException("getPhaseSegments no impl for CompositeSeismicPhase");
    }

    @Override
    public double[] getTau() {
        throw new RuntimeException("getPhaseSegments no impl for CompositeSeismicPhase");
    }

    /**
     * Creates an Arrival for a sampled ray parameter from the model. No interpolation between rays as this is a sample.
     *
     * @param rayNum index in ray parameters
     */
    @Override
    public Arrival createArrivalAtIndex(int rayNum) {
        int idx = 0;
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            if (rayNum-idx < sp.getNumRays()) {
                return sp.createArrivalAtIndex(rayNum-idx);
            }
            idx += sp.getNumRays();
        }
        throw new ArrayIndexOutOfBoundsException(rayNum+" >= "+getNumRays()+" for CompositeSeismicPhase");
    }

    @Override
    public Arrival shootRay(double rayParam) throws TauPException {
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            if (sp.getMinRayParam() <= rayParam && rayParam <= sp.getMaxRayParam()) {
                return sp.shootRay(rayParam);
            }
        }
        throw new TauPException("rayParam doesn't exist for this phase "+rayParam+", "+getName());
    }

    /**
     * True is all segments of this path are only P waves.
     */
    @Override
    public boolean isAllPWave() {
        return getSubPhaseList().get(0).isAllPWave();
    }

    /**
     * True is all segments of this path are only S waves.
     */
    @Override
    public boolean isAllSWave() {
        return getSubPhaseList().get(0).isAllSWave();
    }


    @Override
    public double calcRayParamForTakeoffAngle(double takeoffDegree) throws NoArrivalException {
        return getSubPhaseList().get(0).calcRayParamForTakeoffAngle(takeoffDegree);
    }

    @Override
    public double velocityAtSource() {
        return getSubPhaseList().get(0).velocityAtSource();
    }

    @Override
    public double velocityAtReceiver() {
        return getSubPhaseList().get(0).velocityAtReceiver();
    }

    @Override
    public double densityAtReceiver() {
        return getSubPhaseList().get(0).densityAtReceiver();
    }

    @Override
    public double densityAtSource() {
        return getSubPhaseList().get(0).densityAtSource();
    }

    @Override
    public double calcTakeoffAngleDegree(double arrivalRayParam) {
        return getSubPhaseList().get(0).calcTakeoffAngleDegree(arrivalRayParam);
    }

    @Override
    public double calcTakeoffAngle(double arrivalRayParam) {
        return getSubPhaseList().get(0).calcTakeoffAngle(arrivalRayParam);
    }

    @Override
    public double calcIncidentAngle(double arrivalRayParam) {
        return getSubPhaseList().get(0).calcIncidentAngle(arrivalRayParam);
    }

    @Override
    public double calcIncidentAngleDegree(double arrivalRayParam) {
        return getSubPhaseList().get(0).calcIncidentAngleDegree(arrivalRayParam);
    }

    /**
     * True if the initial leg, leaving the source, wavetype is a P wave, false if an S wave.
     */
    @Override
    public boolean sourceSegmentIsPWave() {
        return getSubPhaseList().get(0).sourceSegmentIsPWave();
    }

    /**
     * True if the final, incident, wavetype is a P wave, false if an S wave.
     */
    @Override
    public boolean finalSegmentIsPWave() {
        return getSubPhaseList().get(0).finalSegmentIsPWave();
    }

    /**
     * Calculates arrivals for this phase, but only for the exact distance in radians. This does not check multiple
     * laps nor going the long way around.
     *
     * @param searchDist
     */
    @Override
    public List<Arrival> calcTimeExactDistance(double searchDist) {
        List<Arrival> out = new ArrayList<>();
        for (SimpleContigSeismicPhase sp : getSubPhaseList()) {
            out.addAll(sp.calcTimeExactDistance(searchDist));
        }
        return out;
    }

    public SimpleContigSeismicPhase findSubphaseForArrival(Arrival currArrival) throws NoArrivalException {
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            if (sp.getMinRayParam() <= currArrival.getRayParam() && currArrival.getRayParam() <= sp.getMaxRayParam()) {
                try {
                    Arrival subphaseArrival = sp.shootRay(currArrival.getRayParam());
                    if (subphaseArrival.getTime() == currArrival.getTime() && subphaseArrival.getDist() == currArrival.getDist()) {
                        // found the phase that generated the arrival
                        return sp;
                    }
                } catch (SlownessModelException | NoSuchLayerException e) {
                    // not right subphase?
                }
            }
        }
        throw new NoArrivalException("can't find arrival within this phase "+currArrival.getName());
    }

    @Override
    public List<ArrivalPathSegment> calcSegmentPaths(Arrival currArrival, TimeDist prevEnd, int prevIdx) throws NoArrivalException {
        return findSubphaseForArrival(currArrival).calcSegmentPaths(currArrival, prevEnd, prevIdx);
    }

    @Override
    public void dump() {
        throw new RuntimeException("no impl for CompositeSeismicPhase");
    }

    @Override
    public SeismicPhase interpolatePhase(double maxDeltaDeg) {
        List<SimpleContigSeismicPhase> out = new ArrayList<>();
        for (SimpleContigSeismicPhase sp : getSubPhaseList()) {
            out.add(sp.interpolatePhase(maxDeltaDeg));
        }
        return new CompositeSeismicPhase(out);
    }

    @Override
    public double calcEnergyFluxFactorReflTranPSV(Arrival arrival) throws VelocityModelException, NoArrivalException {
        return findSubphaseForArrival(arrival).calcEnergyFluxFactorReflTranPSV(arrival);
    }

    @Override
    public double calcEnergyFluxFactorReflTranSH(Arrival arrival) throws VelocityModelException, NoArrivalException {
        return findSubphaseForArrival(arrival).calcEnergyFluxFactorReflTranSH(arrival);
    }

    @Override
    public List<TimeDist> calcPierceTimeDist(Arrival arrival) throws NoArrivalException {
        return findSubphaseForArrival(arrival).calcPierceTimeDist(arrival);
    }

    @Override
    public double calcTstar(Arrival arrival) throws NoArrivalException {
        return findSubphaseForArrival(arrival).calcTstar(arrival);
    }

    @Override
    public int getNumRays() {
        int out = 0;
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            out += sp.getNumRays();
        }
        out = out;
        return out;
    }


    @Override
    public String describe() {
        StringBuilder s = new StringBuilder();
        List<ShadowZone> shadowZones = getShadowZones();
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            s.append(sp.describe()).append("\n");
            int idx = simplePhaseList.indexOf(sp);
            if (idx != simplePhaseList.size()-1) {
                s.append(shadowZones.get(idx)).append("\n");
            }
        }
        return s.toString();
    }

    @Override
    public String describeShort() {
        StringBuilder s = new StringBuilder();
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            s.append(sp.describeShort()).append("\n");
        }
        return s.toString();
    }

    @Override
    public String describeJson() {
        throw new RuntimeException("no impl for CompositeSeismicPhase");
    }

}
