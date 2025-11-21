package edu.sc.seis.TauP;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.sc.seis.TauP.cmdline.args.DistDepthRange;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.SphericalCoords.DtoR;

/**
 * Part of either the path of a seismic phase, or portion of a wavefront.
 */
public abstract class AbstractPathSegment {
    List<TimeDist> path;
    boolean isPWave;
    String segmentName;
    SeismicPhase phase;
    TimeDist prevEnd;
    int segmentIndex;
    int totalNumSegments;

    String pathCssClass = "path";

    public AbstractPathSegment(List<TimeDist> path, boolean isPWave, String segmentName, TimeDist prevEnd,
                               int segmentIndex, int totalNumSegments, SeismicPhase phase) {
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
                    calcDist = LinearInterpolation.linearInterp(prevEnd.getTime(), prevEnd.getDistDeg(),
                            td.getTime(), td.getDistDeg(),
                            maxPathTime);
                    calcDepth = LinearInterpolation.linearInterp(prevEnd.getTime(), prevEnd.getDepth(),
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
        if (!path.isEmpty()) {
            return path.get(path.size() - 1);
        }
        return prevEnd;
    }

    public TimeDist getPathStart() {
        if (!path.isEmpty()) {
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

    public boolean isPWave() {
        return isPWave;
    }

    public String getWavetypeStr() {
        return isPWave ? JSONLabels.PWAVE : JSONLabels.SWAVE;
    }

    public int getSegmentIndex() {
        return segmentIndex;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public TimeDist getPathPoint(int i) {
        return path.get(i);
    }

    public abstract String description();

    public JsonObject asJsonObject() {
        return asJsonObject(null);
    }

    public JsonObject asJsonObject(Arrival arrival) {
        JsonObject a = new JsonObject();
        a.addProperty(JSONLabels.SEGNAME, segmentName);
        a.addProperty(JSONLabels.WAVETYPE, isPWave ? JSONLabels.PWAVE : JSONLabels.SWAVE);
        JsonArray points = new JsonArray();
        a.add(JSONLabels.SEGMENT, points);
        for (TimeDist td : path) {
            JsonArray tdItems = new JsonArray();
            points.add(tdItems);
            tdItems.add((float)td.getDistDeg());
            tdItems.add((float)td.getDepth());
            tdItems.add((float)td.getTime());
            if (arrival != null && arrival.isLatLonable()) {
                double[] latlon = arrival.getLatLonable().calcLatLon(td.getDistDeg(), arrival.getDistDeg());
                tdItems.add((float)latlon[0]);
                tdItems.add((float)latlon[1]);
            }
        }
        return a;
    }

    public String getCssClasses() {
        return pathCssClass+" "+SvgUtil.classForPhase(getPhase().getName()) + (isPWave ? " pwave" : " swave");
    }


    public void writeSVGCartesian(PrintWriter pw) {
        writeSVGCartesian(pw, 0);
    }

    public void writeSVGCartesian(PrintWriter pw, double minPolylineSize) {
        double radiusOfEarth = getPhase().getTauModel().getRadiusOfEarth();
        pw.println("    <g>");
        pw.println("      <desc>" + description() + "</desc>");
        boolean isDegenerate = path.size() <= 2;
        double[] prevXY = SvgEarth.xyForDistRadius( path.get(0).getDistDeg(), radiusOfEarth - path.get(0).getDepth());
        if (minPolylineSize > 0) {
            // check if points are all within minPolylineSize, in which case we will draw a circle instead of a polyline
            for (TimeDist td : path) {
                double[] xy = SvgEarth.xyForDistRadius(td.getDistDeg(), radiusOfEarth - td.getDepth());
                if (Math.abs(xy[0] - prevXY[0]) > minPolylineSize || Math.abs(xy[1] - prevXY[1]) > minPolylineSize) {
                    // different enough to plot line
                    isDegenerate = false;
                    break;
                }
                isDegenerate = true;
            }
        }
        if (isDegenerate) {
            TimeDist td = path.get(0);
            double[] xy = SvgEarth.xyForDistRadius( td.getDistDeg(), radiusOfEarth - td.getDepth());
            pw.println("      <circle class=\"" + getCssClasses() + "\" cx=\""+xy[0] + "\" cy=\""+xy[1] + "\" r=\""+minPolylineSize+"\"/>");
        } else {
            pw.println("      <polyline class=\"" + getCssClasses() + "\" points=\"");
            String prevLine = "";
            for (TimeDist td : path) {
                String line = SvgEarth.formatDistRadiusAsXY(td.getDistDeg(), radiusOfEarth - td.getDepth());
                if ( ! line.equals(prevLine)) {
                    pw.println(line);
                }
                prevLine = line;
            }
            pw.println("\" />");
        }
        pw.println("    </g>");
    }

    public String gmtTextLine(TimeDist td, DistDepthRange distDepthRange, String xFormat, String yFormat,
                              boolean withTime,
                              DistanceAxisType distanceAxisType, DepthAxisType depthAxisType) {
        return gmtTextLine(td, distDepthRange, xFormat, yFormat, withTime, false, distanceAxisType, depthAxisType);
    }

    public String gmtTextLine(TimeDist td, DistDepthRange distDepthRange, String xFormat, String yFormat,
                              boolean withTime, boolean withLatLon,
                              DistanceAxisType distanceAxisType, DepthAxisType depthAxisType) {
        double xVal;
        double yVal;
        double R = getPhase().getTauModel().getRadiusOfEarth();
        switch (distanceAxisType) {
            case radian:
                xVal = td.getDistRadian();
                break;
            case kilometer:
                xVal = R * td.getDistRadian();
                break;
            case degree:
            default:
                // default to degree?
                xVal = td.getDistDeg();
        }
        switch (depthAxisType) {
            case depth:
                yVal = td.getDepth();
                break;
            case radius:
            default:
                yVal = R - td.getDepth();
        }
        String line = String.format(xFormat, xVal) + "  " + String.format(yFormat, yVal);
        if (withTime) {
            line += " " + Outputs.formatTime(td.getTime());
        }
        return line;
    }

    public void writeGMTText(PrintWriter pw, DistDepthRange distDepthRange, String xFormat, String yFormat, boolean withTime, boolean withLatLon) {
        //pw.println("> " + arrival.getCommentLine());
        DistanceAxisType distanceAxisType = distDepthRange.distAxisType != null ? distDepthRange.distAxisType : DistanceAxisType.degree;
        DepthAxisType depthAxisType = distDepthRange.depthAxisType != null ? distDepthRange.depthAxisType : DepthAxisType.radius;
        pw.println("> " + description());
        double R = getPhase().getTauModel().getRadiusOfEarth();
        String prevLine = "";
        for (TimeDist td : path) {
            String line = gmtTextLine(td, distDepthRange, xFormat, yFormat, withTime, withLatLon, distanceAxisType, depthAxisType);
            if (! line.equals(prevLine)) {
                // avoid duplicate lines in output, usually due to changes smaller than the output format
                pw.println(line);
            }
            prevLine = line;
        }
    }
}
