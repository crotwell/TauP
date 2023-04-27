package edu.sc.seis.TauP;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface SeismicPhase extends Serializable, Cloneable {
    /**
     * @return max refractions distance for head waves
     * @deprecated see SeismicPhaseFactory
     */
    static double getMaxRefraction() {
        return SeismicPhaseFactory.getMaxRefraction();
    }

    /**
     * set max refractions distance for head waves
     *
     * @deprecated see SeismicPhaseFactory
     */
    static void setMaxRefraction(double max) {
        SeismicPhaseFactory.setMaxRefraction(max);
    }

    /**
     * @return max diffraction distance for diff waves
     * @deprecated see SeismicPhaseFactory
     */
    static double getMaxDiffraction() {
        return SeismicPhaseFactory.getMaxDiffraction();
    }

    /**
     * set max diffraction distance for diff waves
     *
     * @deprecated see SeismicPhaseFactory
     */
    static void setMaxDiffraction(double max) {
        SeismicPhaseFactory.setMaxDiffraction(max);
    }

    static Arrival getEarliestArrival(List<SeismicPhase> phases, double degrees) {
        Arrival minArrival = null;
        for (SeismicPhase seismicPhase : phases) {
            seismicPhase.calcTime(degrees);
            Arrival currArrival = seismicPhase.getEarliestArrival(degrees);
            if (currArrival != null && (minArrival == null || minArrival.getTime() > currArrival.getTime())) {
                minArrival = currArrival;
            }
        }
        return minArrival;
    }

    boolean phasesExistsInModel();

    Arrival getEarliestArrival(double degrees);

    TauModel getTauModel();

    double getMinDistanceDeg();

    double getMinDistance();

    double getMaxDistanceDeg();

    double getMaxDistance();

    double getMaxRayParam();

    double getMinRayParam();

    int getMaxRayParamIndex();

    int getMinRayParamIndex();

    String getName();

    String getPuristName();

    double getSourceDepth();

    double getReceiverDepth();

    List<String> getLegs();

    List<SeismicPhaseSegment> getPhaseSegments();

    double getRayParams(int i);

    double[] getRayParams();

    double getDist(int i);

    double[] getDist();

    double getTime(int i);

    double[] getTime();

    double getTau(int i);

    double[] getTau();

    boolean[] getDownGoing();

    boolean[] getWaveType();

    int[] getLegAction();

    boolean hasArrivals();

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

    List<Arrival> calcTime(double deg);

    Arrival shootRay(double rayParam) throws SlownessModelException, NoSuchLayerException;

    double calcRayParamForTakeoffAngle(double takeoffDegree);

    double calcTakeoffAngle(double arrivalRayParam);

    double calcIncidentAngle(double arrivalRayParam);

    String describe();

    String toString();

    void dump();

    List<TimeDist> calcPierceTimeDist(Arrival arrival);

    List<TimeDist> calcPathTimeDist(Arrival arrival);
}
