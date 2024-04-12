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

public class ArrivalPathSegment {

    public ArrivalPathSegment(List<TimeDist> path, boolean isPWave, String segmentName, TimeDist prevEnd, Arrival arrival,
                              SeismicPhaseSegment phaseSegment) {
        this(path, isPWave, segmentName, prevEnd, arrival, phaseSegment, 0);
    }

    public ArrivalPathSegment(List<TimeDist> path, boolean isPWave, String segmentName, TimeDist prevEnd, Arrival arrival,
                              SeismicPhaseSegment phaseSegment, int segmentIndex) {
        this.path = path;
        this.isPWave = isPWave;
        this.segmentName = segmentName;
        this.prevEnd = prevEnd;
        this.arrival = arrival;
        this.phaseSegment = phaseSegment;
        this.segmentIndex = segmentIndex;
    }

    public static ArrivalPathSegment linearInterpPath(ArrivalPathSegment segPath, double maxPathInc, double maxPathTime) {
        TimeDist prevEnd = null;
        List<TimeDist> out = new ArrayList<>();

        double calcTime = 0.0;
        double calcDist = 0.0;
        double calcDepth;
        for (TimeDist td : segPath.path) {
            if (prevEnd != null && (prevEnd.getP() != 0.0 &&
                    Math.abs(prevEnd.getDistDeg() - td.getDistDeg()) > maxPathInc)) {
                // interpolate to steps of at most maxPathInc degrees for
                // path
                int maxInterpNum = (int) Math
                        .ceil(Math.abs(td.getDistDeg() - prevEnd.getDistDeg())
                                / maxPathInc);

                for (int interpNum = 1; interpNum < maxInterpNum && calcTime < maxPathTime; interpNum++) {
                    calcTime += (td.getTime() - prevEnd.getTime())
                            / maxInterpNum;
                    if (calcTime > maxPathTime) {
                        break;
                    }
                    calcDist += (td.getDistDeg() - prevEnd.getDistDeg())
                            / maxInterpNum;
                    calcDepth = prevEnd.getDepth() + interpNum
                            * (td.getDepth() - prevEnd.getDepth())
                            / maxInterpNum;
                    out.add(new TimeDist(prevEnd.getP(), calcTime, calcDist*DtoR, calcDepth));
                }
            }

            calcTime = td.getTime();
            calcDepth = td.getDepth();
            calcDist = td.getDistDeg();
            if (calcTime > maxPathTime) {
                // now check to see if past maxPathTime, to create partial path up to a time
                if (prevEnd != null && prevEnd.getTime() < maxPathTime) {
                    // overlap max time, so interpolate to maxPathTime
                    calcDist = TauP_AbstractPhaseTool.linearInterp(prevEnd.getTime(), prevEnd.getDistDeg(),
                            td.getTime(), td.getDistDeg(),
                            maxPathTime);
                    calcDepth = TauP_AbstractPhaseTool.linearInterp(prevEnd.getTime(), prevEnd.getDepth(),
                            td.getTime(), td.getDepth(),
                            maxPathTime);
                    calcTime = maxPathTime;
                } else {
                    // past max time, so done
                    break;
                }
            }
            out.add(new TimeDist(td.getP(), calcTime, calcDist*DtoR, calcDepth));
            prevEnd = td;
        }
        return new ArrivalPathSegment(out, segPath.isPWave, segPath.segmentName, segPath.prevEnd, segPath.arrival, segPath.phaseSegment, segPath.segmentIndex);
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
            if (arrival.isLongWayAround()) {
                shifty *= -1;
            }
            if (Math.abs(1.0-shifty) > .02 ) {
                System.err.println("Path error is greater than 2%, correction may cause errors. "+shifty+" "+arrival);
                System.err.println("  "+distRadian+" "+finalPathDist+"  "+arrival.isLongWayAround());
            }
            List<ArrivalPathSegment> out = new ArrayList<>();
            TimeDist prevEnd = inPath.get(0).prevEnd;
            for (ArrivalPathSegment seg : inPath) {
                ArrivalPathSegment shiftySeg = new ArrivalPathSegment(seg.adjustPathForShifty(shifty),
                        seg.isPWave, seg.segmentName, prevEnd, seg.arrival, seg.phaseSegment, seg.segmentIndex);
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

    public static List<TimeDist> trimDuplicates(List<TimeDist> tdList) {
        if (tdList.size() < 3) {
            return tdList;
        }
        TimeDist prev = null;
        List<TimeDist> out = new ArrayList<>();
        for (TimeDist td : tdList) {
            if (! td.equals(prev)) {
                out.add(td);
            }
            prev = td;
        }
        return out;
    }


    public TimeDist getPathEnd() {
        if (path.size()>0) {
            return path.get(path.size()-1);
        }
        return prevEnd;
    }

    public TimeDist getPathStart() {
        if (path.size() > 0) {
            return path.get(0);
        }
        return prevEnd;
    }

    public List<TimeDist> getPath() {
        return path;
    }

    public TimeDist getPathPoint(int i) {
        return path.get(i);
    }

    public String description() {
        return "seg "+segmentIndex+" "+segmentName+" of "+arrival.getCommentLine()+" in "+phaseSegment.describeBranchRange();

    }

    public JSONObject asJSONObject() {
        JSONObject a = new JSONObject();
        a.put("name", segmentName);
        a.put("wavetype", isPWave?"pwave":"swave");
        JSONArray points = new JSONArray();
        a.put("path", points);
        for (TimeDist td : path) {
            JSONArray tdItems = new JSONArray();
            points.put(tdItems);
            tdItems.put(td.getDistDeg());
            tdItems.put(td.getDepth());
            tdItems.put(td.getTime());
        }
        return a;
    }

    public void writeJSON(PrintWriter pw, String indent) throws IOException {
        String NL = "\n";
        pw.write(indent+"{"+NL);
        String innerIndent = indent+"  ";
        pw.write(innerIndent+JSONWriter.valueToString("index")+": "+JSONWriter.valueToString(segmentIndex)+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("name")+": "+JSONWriter.valueToString(segmentName)+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("wavetype")+": "+JSONWriter.valueToString(isPWave?"pwave":"swave")+","+NL);

        pw.write(innerIndent+ JSONWriter.valueToString("path")+": ["+NL);
        for (TimeDist td : path) {
            pw.write(innerIndent+"  [ "+
                    JSONWriter.valueToString((float)td.getDistDeg())+", "+
                    JSONWriter.valueToString((float)td.getDepth())+", "+
                    JSONWriter.valueToString((float)td.getTime())+" ],"+NL);
        }
        pw.write(innerIndent+"]");
    }

    public void writeSVGCartesian(PrintWriter pw) {
        double radiusOfEarth = arrival.getPhase().getTauModel().getRadiusOfEarth();
        pw.println("<g>");
        pw.println("    <desc>" + description() + "</desc>");
        pw.println("    <polyline class=\"path "+SvgUtil.classForPhase(arrival.getName())+" "+(isPWave?"pwave":"swave")+"\" points=\"");
        for (TimeDist td : path) {
            SvgEarth.printDistRadiusAsXY(pw, td.getDistDeg(), radiusOfEarth - td.getDepth());
            pw.println();
        }
        pw.println("\" />");
        pw.println("</g>");
    }

    public void writeGMTText(PrintWriter pw, DistDepthRange distDepthRange, String xFormat, String yFormat, boolean withTime) {
        //pw.println("> " + arrival.getCommentLine());
        DistanceAxisType distanceAxisType = distDepthRange.distAxisType != null ? distDepthRange.distAxisType : DistanceAxisType.degree ;
        DepthAxisType depthAxisType = distDepthRange.depthAxisType != null ? distDepthRange.depthAxisType : DepthAxisType.radius;
        pw.println("> "+description());
        double R = arrival.getPhase().getTauModel().getRadiusOfEarth();
        for (TimeDist td : path) {
            double xVal;
            double yVal;
            switch (distanceAxisType)  {
                case radian:
                    xVal = td.getDistRadian();
                case kilometer:
                    xVal = R * td.getDistRadian();
                case degree:
                default:
                    // default to degree?
                    xVal = td.getDistDeg();
            }
            switch (depthAxisType) {
                case depth:
                    yVal = td.getDepth();
                case radius:
                default:
                    yVal = R-td.getDepth();
            }
            String timeStr = "";
            if (withTime) {
                pw.print(" " + Outputs.formatTime(td.getTime()));
            }
            pw.println(String.format(xFormat, xVal)+"  "+String.format(yFormat, yVal)+timeStr);
        }
    }

    List<TimeDist> path;

    boolean isPWave;

    String segmentName;

    TimeDist prevEnd;

    int segmentIndex = -1;

    SeismicPhaseSegment phaseSegment;

    Arrival arrival;
}
