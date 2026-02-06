package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

public class TimeRay extends RayCalculateable {

    private final double seconds;

    public TimeRay(double seconds) {
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
                    double dRPdDist;
                    if (simp.getMaxRayParam() == simp.getMinRayParam()) {
                        // degenerate phase, all ray parameters are the same, just interpolate time
                        arrivalDist = LinearInterpolation.linearInterp(left.getTime(),left.getDist(),
                                right.getTime(), right.getDist(), seconds);
                        dRPdDist = 0;
                    } else if (Math.abs(seconds - left.getTime()) < Math.abs(seconds - right.getTime())) {
                        arrivalDist = (seconds-left.getTime())/arrivalRayParam+left.getDist();
                        dRPdDist = (left.getRayParam() - arrivalRayParam) / (left.getDist() - arrivalDist);
                    } else {
                        arrivalDist = (seconds-right.getTime())/arrivalRayParam+right.getDist();
                        dRPdDist = (right.getRayParam() - arrivalRayParam) / (right.getDist() - arrivalDist);
                    }
                    Arrival a = new Arrival(phase, simp, seconds, arrivalDist, arrivalRayParam, rayNum, dRPdDist);
                    a.setSearchValue(this);
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
                return new EventAzimuth(evtLatLon, azimuth, geodesic);
            } else {
                return new StationBackAzimuth(staLatLon, backAzimuth, geodesic);
            }
        }
        return null;
    }

    @Override
    public boolean isLatLonable() {
        return (evtLatLon != null && azimuth != null) || (staLatLon != null && backAzimuth != null);
    }
}
