package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

/**
 * Seismic Phase that is simple, but may contain shadow zones due to high slowness layers (low velocity zones) in the model.
 */
public class CompositeSeismicPhase extends SimpleSeismicPhase {

    private final List<SimpleContigSeismicPhase> simplePhaseList;

    public CompositeSeismicPhase(List<SimpleContigSeismicPhase> subphaseList) {
        this.simplePhaseList = subphaseList;
        if (simplePhaseList.size() == 0) {
            throw new IllegalArgumentException("Subphase list must not be empty");
        }
    }

    public List<SimpleContigSeismicPhase> getSubPhaseList() {
        return simplePhaseList;
    }

    @Override
    public List<ShadowZone> getShadowZones() throws SlownessModelException, NoSuchLayerException {
        List<ShadowZone> out = new ArrayList<>();
        SimpleContigSeismicPhase prev = null;
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            if (prev != null) {
                out.add(new ShadowZone(this, prev.getMinRayParam(), prev.getMinRayParamIndex(),
                        prev.createArrivalAtIndex(prev.getNumRays()-1),
                        sp.createArrivalAtIndex(0)
                ));
            }
            prev = sp;
        }
        return out;
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
        return List.of();
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
        return getSubPhaseList().get(0).countFlatLegs();
    }

    @Override
    public double getRayParams(int i) {
        return 0;
    }

    @Override
    public double[] getRayParams() {
        return new double[0];
    }

    @Override
    public double getDist(int i) {
        return 0;
    }

    @Override
    public double[] getDist() {
        return new double[0];
    }

    @Override
    public double getTime(int i) {
        return 0;
    }

    @Override
    public double[] getTime() {
        return new double[0];
    }

    @Override
    public double getTau(int i) {
        return 0;
    }

    @Override
    public double[] getTau() {
        return new double[0];
    }

    /**
     * Creates an Arrival for a sampled ray parameter from the model. No interpolation between rays as this is a sample.
     *
     * @param rayNum index in ray parameters
     */
    @Override
    public Arrival createArrivalAtIndex(int rayNum) {
        return null;
    }

    @Override
    public Arrival shootRay(double rayParam) throws SlownessModelException, NoSuchLayerException {
        return null;
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

    @Override
    public List<ArrivalPathSegment> calcSegmentPaths(Arrival currArrival, TimeDist prevEnd, int prevIdx) {
        throw new RuntimeException("no impl for CompositeSeismicPhase");
    }

    @Override
    public void dump() {
        throw new RuntimeException("no impl for CompositeSeismicPhase");
    }

    @Override
    public SeismicPhase interpolatePhase(double maxDeltaDeg) {
        return null;
    }

    @Override
    public double calcEnergyFluxFactorReflTranPSV(Arrival arrival) throws VelocityModelException, SlownessModelException {
        throw new RuntimeException("no impl for CompositeSeismicPhase");
    }

    @Override
    public double calcEnergyFluxFactorReflTranSH(Arrival arrival) throws VelocityModelException, SlownessModelException {
        throw new RuntimeException("no impl for CompositeSeismicPhase");
    }

    @Override
    public List<TimeDist> calcPierceTimeDist(Arrival arrival) {
        throw new RuntimeException("no impl for CompositeSeismicPhase");
    }

    @Override
    public double calcTstar(Arrival currArrival) {
        throw new RuntimeException("no impl for CompositeSeismicPhase");
    }

    @Override
    public int getNumRays() {
        return 0;
    }


    @Override
    public String describe() {
        StringBuilder s = new StringBuilder();
        for (SimpleContigSeismicPhase sp : simplePhaseList) {
            s.append(sp.describe()).append("\n");
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
