package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

public class TimeRay extends RayCalculateable {

    private final double seconds;

    public TimeRay(double seconds, DistanceCalc distCalc) {
        super(distCalc);
        this.seconds = seconds;
    }
    
    @Override
    public List<Arrival> calculate(SeismicPhase phase)  {
        List<Arrival> arrivals = new ArrayList<>();
        if (phase instanceof SimpleContigSeismicPhase &&
                phase.getMinTime() <= seconds && seconds <= phase.getMaxTime()) {
            SimpleContigSeismicPhase simp = (SimpleContigSeismicPhase)phase;
            double[] times = simp.getTime();
            double[] rayParams = simp.getRayParams();
            for (int rayNum = 0; rayNum < (times.length - 1); rayNum++) {
                if ((times[rayNum] <= seconds && seconds < times[rayNum+1] )
                        || (times[rayNum] >= seconds && seconds > times[rayNum+1] )) {
                    if ((rayParams[rayNum] == rayParams[rayNum + 1])
                            && simp.getMaxRayParam() > simp.getMinRayParam()) {
                        /*
                         * Here we have a shadow zone, so it is not really an
                         * arrival.
                         */
                        continue;
                    }
                    Arrival left = simp.createArrivalAtIndex(rayNum);
                    Arrival right = simp.createArrivalAtIndex(rayNum + 1);

                    double arrivalRayParam = LinearInterpolation.linearInterp(left.getTime(),left.getRayParam(),
                            right.getTime(), right.getRayParam(), seconds);
                    double arrivalDist;
                    Arrival neighborArrival = left.getTime()==seconds?right:left;
                    if (simp.getMaxRayParam() == simp.getMinRayParam()) {
                        // degenerate phase, all ray parameters are the same, just interpolate time
                        arrivalDist = LinearInterpolation.linearInterp(left.getTime(),left.getDist(),
                                right.getTime(), right.getDist(), seconds);
                    } else if (Math.abs(seconds - left.getTime()) < Math.abs(seconds - right.getTime())) {
                        arrivalDist = (seconds-left.getTime())/arrivalRayParam+left.getDist();
                        neighborArrival = left;
                    } else {
                        arrivalDist = (seconds-right.getTime())/arrivalRayParam+right.getDist();
                        neighborArrival = right;
                    }
                    Arrival a = new Arrival(phase, simp, seconds, arrivalDist, arrivalRayParam, rayNum, this);
                    a.setSearchValue(this);
                    a.setNeighborArrival(neighborArrival);
                    arrivals.add(a);
                }
            }
        }
        return arrivals;
    }

    @Override
    public LatLonable getLatLonable() {
        if (isLatLonable()) {
            if (evtLatLon != null) {
                return new EventAzimuth(evtLatLon, azimuth, distCalc);
            } else {
                return new StationBackAzimuth(staLatLon, backAzimuth, distCalc);
            }
        }
        return null;
    }

    @Override
    public boolean isLatLonable() {
        return (evtLatLon != null && azimuth != null) || (staLatLon != null && backAzimuth != null);
    }

    public double getSeconds() {
        return seconds;
    }
}
