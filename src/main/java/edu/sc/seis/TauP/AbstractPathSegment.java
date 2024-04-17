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

public abstract class AbstractPathSegment {
    List<TimeDist> path;
    boolean isPWave;
    String segmentName;
    SeismicPhase phase;
    TimeDist prevEnd;
    int segmentIndex = -1;
    int totalNumSegments = -1;

    public AbstractPathSegment(List<TimeDist> path, boolean isPWave, String segmentName, TimeDist prevEnd, int segmentIndex, int totalNumSegments, SeismicPhase phase) {
        this.path = path;
        this.isPWave = isPWave;
        this.segmentName = segmentName;
        this.prevEnd = prevEnd;
        this.segmentIndex = segmentIndex;
        this.totalNumSegments = totalNumSegments;
        this.phase = phase;
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
                    out.add(new TimeDist(prevEnd.getP(), calcTime, calcDist * DtoR, calcDepth));
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
            out.add(new TimeDist(td.getP(), calcTime, calcDist * DtoR, calcDepth));
            prevEnd = td;
        }
        return new ArrivalPathSegment(out, segPath.isPWave, segPath.segmentName, segPath.prevEnd, segPath.arrival,
                segPath.phaseSegment, segPath.segmentIndex, segPath.totalNumSegments);
    }

    public static List<TimeDist> trimDuplicates(List<TimeDist> tdList) {
        if (tdList.size() < 3) {
            return tdList;
        }
        TimeDist prev = null;
        List<TimeDist> out = new ArrayList<>();
        for (TimeDist td : tdList) {
            if (!td.equals(prev)) {
                out.add(td);
            }
            prev = td;
        }
        return out;
    }

    public TimeDist getPathEnd() {
        if (path.size() > 0) {
            return path.get(path.size() - 1);
        }
        return prevEnd;
    }

    public TimeDist getPathStart() {
        if (path.size() > 0) {
            return path.get(0);
        }
        return prevEnd;
    }

    public List<TimeDist> negativeDistance() {
        List<TimeDist> out = new ArrayList<>();
        for(TimeDist td : path) {
            out.add(new TimeDist(td.getP(), td.getTime(), -1* td.getDistRadian(), td.getDepth()));
        }
        return out;
    }

    public List<TimeDist> getPath() {
        return path;
    }

    public SeismicPhase getPhase() {
        return phase;
    }

    public TimeDist getPathPoint(int i) {
        return path.get(i);
    }

    public abstract String description();

    public JSONObject asJSONObject() {
        JSONObject a = new JSONObject();
        a.put("name", segmentName);
        a.put("wavetype", isPWave ? "pwave" : "swave");
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
        pw.write(indent + "{" + NL);
        String innerIndent = indent + "  ";
        pw.write(innerIndent + JSONWriter.valueToString("index") + ": " + JSONWriter.valueToString(segmentIndex) + "," + NL);
        pw.write(innerIndent + JSONWriter.valueToString("name") + ": " + JSONWriter.valueToString(segmentName) + "," + NL);
        pw.write(innerIndent + JSONWriter.valueToString("wavetype") + ": " + JSONWriter.valueToString(isPWave ? "pwave" : "swave") + "," + NL);

        pw.write(innerIndent + JSONWriter.valueToString("path") + ": [" + NL);
        for (TimeDist td : path) {
            pw.write(innerIndent + "  [ " +
                    JSONWriter.valueToString((float) td.getDistDeg()) + ", " +
                    JSONWriter.valueToString((float) td.getDepth()) + ", " +
                    JSONWriter.valueToString((float) td.getTime()) + " ]," + NL);
        }
        pw.write(innerIndent + "]");
    }

    public String getCssClasses() {
        return "path "+SvgUtil.classForPhase(getPhase().getName()) + (isPWave ? " pwave" : " swave");
    }

    public void writeSVGCartesian(PrintWriter pw) {
        double radiusOfEarth = getPhase().getTauModel().getRadiusOfEarth();
        pw.println("    <g>");
        pw.println("      <desc>" + description() + "</desc>");

        pw.println("      <polyline class=\"" + getCssClasses() + "\" points=\"");
        for (TimeDist td : path) {
            SvgEarth.printDistRadiusAsXY(pw, td.getDistDeg(), radiusOfEarth - td.getDepth());
            pw.println();
        }
        pw.println("\" />");
        pw.println("    </g>");
    }

    public void writeGMTText(PrintWriter pw, DistDepthRange distDepthRange, String xFormat, String yFormat, boolean withTime) {
        //pw.println("> " + arrival.getCommentLine());
        DistanceAxisType distanceAxisType = distDepthRange.distAxisType != null ? distDepthRange.distAxisType : DistanceAxisType.degree;
        DepthAxisType depthAxisType = distDepthRange.depthAxisType != null ? distDepthRange.depthAxisType : DepthAxisType.radius;
        pw.println("> " + description());
        double R = getPhase().getTauModel().getRadiusOfEarth();
        for (TimeDist td : path) {
            double xVal;
            double yVal;
            switch (distanceAxisType) {
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
                    yVal = R - td.getDepth();
            }
            String timeStr = "";
            if (withTime) {
                pw.print(" " + Outputs.formatTime(td.getTime()));
            }
            pw.println(String.format(xFormat, xVal) + "  " + String.format(yFormat, yVal) + timeStr);
        }
    }
}
