package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

public abstract class SimpleSeismicPhase implements SeismicPhase {


    /**
     * Calculates arrivals for this phase, but only for the exact distance in radians. This does not check multiple
     * laps nor going the long way around.
     *  */
    public abstract List<Arrival> calcTimeExactDistance(double searchDist);

    public List<ArrivalPathSegment> calcSegmentPaths(Arrival currArrival) throws NoArrivalException, SlownessModelException, TauModelException {
        return calcSegmentPaths(currArrival, new TimeDist(currArrival.getRayParam(), 0, 0, currArrival.getSourceDepth()), 0);
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
    public List<ArrivalPathSegment> calcSegmentPaths(Arrival currArrival, TimeDist prevEnd, int prevIdx) throws NoArrivalException, SlownessModelException, TauModelException {
        int idx = prevIdx+1;
        List<ArrivalPathSegment> segmentPaths = new ArrayList<>();
        int numSegments = currArrival.listPhaseSegments().size();
        for (SeismicPhaseSegment seg : currArrival.listPhaseSegments()) {
            ArrivalPathSegment segPath = seg.calcPathTimeDist(currArrival, prevEnd, idx++, prevIdx+ numSegments);

            if (segPath.path.isEmpty()) {
                continue;
            }
            segmentPaths.add(segPath);
            prevEnd = segPath.getPathEnd();
        }
        return ArrivalPathSegment.adjustPath(segmentPaths, currArrival);
    }

    public abstract SimpleSeismicPhase interpolateSimplePhase(double maxDeltaDeg);

}
