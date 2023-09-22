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

    String describeJson();

    String toString();

    void dump();

    List<TimeDist> calcPierceTimeDist(Arrival arrival);

    List<TimeDist> calcPathTimeDist(Arrival arrival);

    public static String baseDescribe(SeismicPhase phase) {
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
                    +Outputs.formatDistanceNoPad(phase.getMaxDistanceDeg())+mod180Max+" degrees.\n";
            if (phase.getMaxRayParam() > phase.getMinRayParam()) {
                desc += "  with ray parameter from " + Outputs.formatRayParam(phase.getMaxRayParam() / Arrival.RtoD)
                        + " down to " + Outputs.formatRayParam(phase.getMinRayParam() / Arrival.RtoD) + " sec/deg.\n";
            } else {
                desc += "  with degenerate ray parameter of " + Outputs.formatRayParam(phase.getMaxRayParam() / Arrival.RtoD) + " sec/deg.\n";
            }
            double[] time = phase.getTime();
            double[] dist = phase.getDist();
            double[] rayParams = phase.getRayParams();
            desc += "  travel times from " + Outputs.formatTimeNoPad(time[0]) + " to " + Outputs.formatTimeNoPad(time[time.length - 1]) + " sec";
            for (int i = 0; i < dist.length; i++) {
                if (i < dist.length - 1 && (rayParams[i] == rayParams[i + 1])
                        && rayParams.length > 2) {
                    /* Here we have a shadow zone, so output a warning of break in curve. */
                    desc += "\n  with shadow zone between " + Outputs.formatDistance(Arrival.RtoD * dist[i])
                            + " and " + Outputs.formatDistance(Arrival.RtoD * dist[i + 1]) + " deg";
                }
            }
            desc += ".\n";
        } else {
            desc += "  FAILS to exist, because no ray parameters satisfy the path.\n";
        }
        return desc;
    }

    public static String baseDescribeJSON(SeismicPhase phase) {
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
            double[] time = phase.getTime();
            double[] dist = phase.getDist();
            double[] rayParams = phase.getRayParams();

            desc += "  \"minexists\": { \n"+
                    "    \"dist\": "+Outputs.formatDistanceNoPad(phase.getMinDistanceDeg())+mod180Min+",\n"+
                    "    \"rayparameter\": "+Outputs.formatRayParam(phase.getMaxRayParam() / Arrival.RtoD)+",\n"+
                    "    \"time\": "+Outputs.formatTimeNoPad(time[0])+"\n"+
                    "  },"+
                    "  \"maxexists\": { \n"+
                    "    \"dist\": "+Outputs.formatDistanceNoPad(phase.getMaxDistanceDeg())+mod180Max+",\n"+
                    "    \"rayparameter\": "+Outputs.formatRayParam(phase.getMinRayParam() / Arrival.RtoD)+",\n"+
                    "    \"time\": "+Outputs.formatTimeNoPad(time[time.length - 1])+"\n"+
                    "  },\n"+
                    "  \"shadow\": [";

            boolean hasPrevShadow = false;
            for (int i = 0; i < dist.length; i++) {
                if (i < dist.length - 1 && (rayParams[i] == rayParams[i + 1])
                        && rayParams.length > 2) {
                    if (hasPrevShadow) {
                        desc +=",\n";
                    }
                    /* Here we have a shadow zone, so output a warning of break in curve. */
                    desc += "  {"+
                            "    \"min_dist\": " + Outputs.formatDistance(Arrival.RtoD * dist[i])+",\n"+
                            "    \"max_dist\": " + Outputs.formatDistance(Arrival.RtoD * dist[i + 1])+",\n"+
                            "  }";
                    hasPrevShadow = true;
                }
            }
            desc += "  ]\n";
        } else {
            desc += "  \n";
        }
        return desc;
    }

    public static String segmentDescribe(SeismicPhase phase) {
        String desc = "";
        String indent = "  ";
        for(SeismicPhaseSegment segment : phase.getPhaseSegments()) {
            desc += indent+ segment.toString()+"\n";
        }
        return desc;
    }
    public static String segmentDescribeJSON(SeismicPhase phase) {
        String desc = "";
        String indent = "  ";
        desc += indent+"\"segment\": [\n";
        boolean first=true;
        for(SeismicPhaseSegment segment : phase.getPhaseSegments()) {
            if (first) {
                first = false;
                desc+= "\n";
            } else {
                desc+= ",\n";
            }
            desc += indent+segment.toJSONString();
        }
        desc += indent+"]\n";
        return desc;
    }
}
