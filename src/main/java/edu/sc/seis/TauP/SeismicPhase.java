package edu.sc.seis.TauP;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public interface SeismicPhase extends Serializable, Cloneable {

    boolean PWAVE = true;
    boolean SWAVE = false;


    boolean phasesExistsInModel();

    Arrival getEarliestArrival(double degrees);

    double getMinDistanceDeg();

    double getMinDistance();

    double getMaxDistanceDeg();

    double getMaxDistance();

    double getMaxRayParam();

    double getMinRayParam();

    double getMinTime();

    double getMaxTime();

    String getName();

    String getPuristName();

    double getSourceDepth();

    double getReceiverDepth();

    boolean hasArrivals();

    String describe();

    String describeShort();

    TauModel getTauModel();

    static Arrival getEarliestArrival(List<SeismicPhase> phases, double degrees) {
        Arrival minArrival = null;
        for (SeismicPhase seismicPhase : phases) {
            Arrival currArrival = seismicPhase.getEarliestArrival(degrees);
            if (currArrival != null && (minArrival == null || minArrival.getTime() > currArrival.getTime())) {
                minArrival = currArrival;
            }
        }
        return minArrival;
    }

    boolean isFail();

    String failReason();

    int getMaxRayParamIndex();

    int getMinRayParamIndex();

    List<List<SeismicPhaseSegment>> getListPhaseSegments();

    SeismicPhaseSegment getInitialPhaseSegment();

    SeismicPhaseSegment getFinalPhaseSegment();

    int countFlatLegs();

    double getRayParams(int i);

    double[] getRayParams();

    double getDist(int i);

    double[] getDist();

    double getTime(int i);

    double[] getTime();

    double getTau(int i);

    double[] getTau();

    static double distanceTrim180(double deg) {
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
        return tempDeg;
    }

    /**
     * Creates an Arrival for a sampled ray parameter from the model. No interpolation between rays as this is a sample.
     * @param rayNum index in ray parameters
     */
    Arrival createArrivalAtIndex(int rayNum);

    Arrival shootRay(double rayParam) throws TauPException;

    /** True is all segments of this path are only P waves.
     *
     */
    boolean isAllPWave();

    /** True is all segments of this path are only S waves.
     *
     */
    boolean isAllSWave();

    double calcRayParamForTakeoffAngle(double takeoffDegree) throws NoArrivalException;

    double calcRayParamForIncidentAngle(double incidentDegree) throws NoArrivalException;

    double velocityAtSource();

    double velocityAtReceiver();

    double densityAtReceiver();

    double densityAtSource();

    double calcTakeoffAngleDegree(double arrivalRayParam);

    double calcTakeoffAngle(double arrivalRayParam);

    double calcIncidentAngle(double arrivalRayParam);

    double calcIncidentAngleDegree(double arrivalRayParam);

    /**
     * True if the initial leg, leaving the source, wavetype is a P wave, false if an S wave.
     */
    boolean sourceSegmentIsPWave();

    /**
     * True if the final, incident, wavetype is a P wave, false if an S wave.
     */
    boolean finalSegmentIsPWave();

    List<ArrivalPathSegment> calcSegmentPaths(Arrival currArrival) throws NoArrivalException, SlownessModelException, TauModelException;

    String toString();

    void dump();

    SeismicPhase interpolatePhase(double maxDeltaDeg);

    double calcEnergyFluxFactorReflTranPSV(Arrival arrival) throws VelocityModelException, SlownessModelException, NoArrivalException;

    double calcEnergyFluxFactorReflTranSH(Arrival arrival) throws VelocityModelException, SlownessModelException, NoArrivalException;

    List<TimeDist> interpPierceTimeDist(Arrival arrival) throws NoArrivalException, TauModelException;

    double calcTstar(Arrival currArrival) throws NoArrivalException;

    /**
     * Split calculated array into segments for repeated ray parameter values, which indicate a
     * discontinuity in the calculations usually due to low velocity zone.
     *
     * @param rayParams ray parameter array for phase
     * @param values derived array, such as distance, time, tau, etc.
     * @return list of arrays for each contiguous segment
     */
    static List<double[]> splitForRepeatRayParam(double[] rayParams, double[] values) {
        List<double[]> out = new ArrayList<>();
        int partialStart = 0;
        if (rayParams.length != values.length) {
            throw new IllegalArgumentException("rayParams and values must be same length: "+rayParams.length+" "+values.length);
        }
        for (int i = 0; i < values.length-1; i++) {
            if((rayParams[i] == rayParams[i + 1]) && rayParams.length > 2) {
                double[] partialValues = new double[1+i-partialStart];
                System.arraycopy(values, partialStart, partialValues, 0, partialValues.length);
                out.add(partialValues);
                partialStart = i+1;
            }
        }
        // and last partial section
        double[] partialValues = new double[values.length-partialStart];
        System.arraycopy(values, partialStart, partialValues, 0, partialValues.length);
        out.add(partialValues);
        return out;
    }

    static String baseDescribe(SeismicPhase phase) {
        String desc = "";
        if (phase.phasesExistsInModel()) {

            String mod180Min = "";
            if (phase.getMinDistanceDeg() > 180 || phase.getMinDistanceDeg() < -180) {
                mod180Min = " ("+Outputs.formatDistanceNoPad(SeismicPhase.distanceTrim180(phase.getMinDistanceDeg()))+") ";
            }
            String mod180Max = "";
            if (phase.getMaxDistanceDeg() > 180 || phase.getMaxDistanceDeg() < -180) {
                mod180Max = " ("+Outputs.formatDistanceNoPad(SeismicPhase.distanceTrim180(phase.getMaxDistanceDeg()))+") ";
            }
            desc += "  exists from "+Outputs.formatDistanceNoPad(phase.getMinDistanceDeg())+mod180Min+" to "
                    +Outputs.formatDistanceNoPad(phase.getMaxDistanceDeg())+mod180Max+" degrees in the "
                    +phase.getTauModel().getModelName()+" model.\n";
            if (phase.getMaxRayParam() > phase.getMinRayParam()) {
                desc += "  with ray parameter from " + Outputs.formatRayParam(phase.getMaxRayParam() / SphericalCoords.RtoD)
                        + " down to " + Outputs.formatRayParam(phase.getMinRayParam() / SphericalCoords.RtoD) + " sec/deg.\n";
            } else {
                desc += "  with degenerate ray parameter of " + Outputs.formatRayParam(phase.getMaxRayParam() / SphericalCoords.RtoD) + " sec/deg.\n";
            }
            desc += "  travel times from " + Outputs.formatTimeNoPad(phase.getMinTime()) + " to " + Outputs.formatTimeNoPad(phase.getMaxTime()) + " sec";
            StringBuilder builder = new StringBuilder();
            for (ShadowZone shad : phase.getShadowZones()) {
                Arrival pre = shad.getPreArrival();
                Arrival post = shad.getPostArrival();
                builder.append( "\n  with shadow zone between " + Outputs.formatDistance(pre.getDistDeg())
                        + " and " + Outputs.formatDistance(post.getDistDeg()) + " deg");
            }
            builder.append(".\n");
            desc += builder.toString();
        } else {
            desc += "  FAILS to exist, because no ray parameters satisfy the path.\n";
        }
        return desc;
    }

    static String segmentDescribe(SeismicPhase phase) {
        return SeismicPhaseSegment.segmentListDescribe(phase.getListPhaseSegments());
    }

    int getNumRays();

    List<ShadowZone> getShadowZones();
}
