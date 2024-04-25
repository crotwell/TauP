package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.DistDepthRange;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.Arrival.DtoR;

public class ArrivalPathSegment extends AbstractPathSegment {

    public ArrivalPathSegment(List<TimeDist> path, boolean isPWave, String segmentName, TimeDist prevEnd, Arrival arrival,
                              SeismicPhaseSegment phaseSegment) {
        this(path, isPWave, segmentName, prevEnd, arrival, phaseSegment, 0, -1);
    }

    public ArrivalPathSegment(List<TimeDist> path, boolean isPWave, String segmentName, TimeDist prevEnd, Arrival arrival,
                              SeismicPhaseSegment phaseSegment, int segmentIndex, int totalNumSegments) {
        super(path, isPWave, segmentName, prevEnd, segmentIndex, totalNumSegments, arrival.getPhase());
        this.arrival = arrival;
        this.phaseSegment = phaseSegment;
    }


    /**
     * Adjust path so the end point lines up.
     * Because we are shooting a ray parameter through the model, and that ray parameter came from an
     * interpolation, it can happen for long paths that the output path doesn't quite end at the requested
     * distance. We do a simple scaling of all path distances so it hits the output.
     * @param inPath
     * @param arrival
     * @return
     */
    public static List<ArrivalPathSegment> adjustPath(List<ArrivalPathSegment> inPath, Arrival arrival) {
        double distRadian = arrival.getDist();
        // start might not be zero is part of scattered phase
        TimeDist firstPoint = inPath.get(0).getPathStart();
        ArrivalPathSegment lastSeg = inPath.get(inPath.size()-1);
        double finalPathDist = lastSeg.getPathEnd().getDistRadian()- firstPoint.getDistRadian();
        if (inPath.size() != 0 && distRadian != 0 && finalPathDist != 0) {
            double shifty = distRadian/finalPathDist;
            if (Math.abs(1.0-shifty) > .02 && Math.abs(1+shifty) > 1e-5 ) {
                // don't flag shifty that just reflects, ie -1
                System.err.println("Path error is greater than 2%, correction may cause errors. "+shifty);
                System.err.println("  "+arrival);
                System.err.println("  "+distRadian+" "+finalPathDist+"  "+arrival.isLongWayAround());
            }
            if (arrival.isLongWayAround()) {
                shifty *= -1;
            }
            List<ArrivalPathSegment> out = new ArrayList<>();
            TimeDist prevEnd = inPath.get(0).prevEnd;
            for (ArrivalPathSegment seg : inPath) {
                ArrivalPathSegment shiftySeg = new ArrivalPathSegment(seg.adjustPathForShifty(shifty),
                        seg.isPWave, seg.segmentName, prevEnd, seg.arrival,
                        seg.phaseSegment, seg.segmentIndex, seg.totalNumSegments);
                prevEnd = shiftySeg.getPathEnd();
                out.add(shiftySeg);
            }
            return out;
        } else {
            return inPath;
        }
    }

    public List<TimeDist> adjustPathForShifty(double shifty) {
        ArrayList<TimeDist> out = new ArrayList<TimeDist>();
        for (TimeDist td : path) {
            out.add(new TimeDist(td.getP(),
                    td.getTime(),
                    td.getDistRadian() * shifty,
                    td.getDepth()));
        }
        return out;
    }


    @Override
    public String description() {
        return "seg "+segmentIndex+"/"+totalNumSegments+" "+segmentName+" of "+arrival.getCommentLine()+" in "+phaseSegment.describeBranchRange();

    }

    SeismicPhaseSegment phaseSegment;

    Arrival arrival;
}
