package edu.sc.seis.TauP;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Line, possbily in multiple segments, representing a single data type.
 */
public class XYPlottingData {

    public XYPlottingData(List<XYSegment> segments, String xAxisType, String yAxisType, String label, List<String> cssClasses) {
        this(segments, xAxisType, yAxisType, label, "", cssClasses);
    }
    public XYPlottingData(List<XYSegment> segments, String xAxisType, String yAxisType, String label, String description, List<String> cssClasses) {
        segmentList = segments;
        this.xAxisType = xAxisType;
        this.yAxisType = yAxisType;
        this.label = label;
        this.description = description;
        this.cssClasses = cssClasses;
    }

    public static double[] initMinMax() {
        double minX = Double.MAX_VALUE;
        double maxX = -1*Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -1*Double.MAX_VALUE;
        return new double[] { minX, maxX, minY, maxY};
    }
    public double[] minMax() {
        return minMax(initMinMax());
    }

    public double[] minMax(double[] priorMinMax) {
        for (XYSegment segment : segmentList) {
            priorMinMax = segment.minMax(priorMinMax);
        }
        return priorMinMax;
    }

    public double[] minMaxInXRange(double[] priorMinMax, double[] xRange) {
        for (XYSegment segment : segmentList) {
            priorMinMax = segment.minMaxInXRange(priorMinMax, xRange);
        }
        return priorMinMax;
    }

    public double[] minMaxInYRange(double[] priorMinMax, double[] yRange) {
        for (XYSegment segment : segmentList) {
            priorMinMax = segment.minMaxInYRange(priorMinMax, yRange);
        }
        return priorMinMax;
    }


    public XYPlottingData recalcForAbs(boolean xAxisAbs, boolean yAxisAbs) {
        List<XYSegment> out = new ArrayList<>();
        for (XYSegment segment : segmentList) {
            out.add(segment.recalcForAbs(xAxisAbs, yAxisAbs));
        }
        String xAxis = xAxisAbs ? "abs "+xAxisType : xAxisType;
        String yAxis = yAxisAbs ? "abs "+yAxisType : yAxisType;
        return new XYPlottingData(out, xAxis, yAxis, label, cssClasses);
    }

    public XYPlottingData recalcForLog(boolean xAxisLog, boolean yAxisLog) {
        List<XYSegment> nanSplit = new ArrayList<>();
        for (XYSegment segment : segmentList) {
            nanSplit.addAll(segment.recalcForInfinite(xAxisLog, yAxisLog));
        }
        List<XYSegment> logList = new ArrayList<>();
        for (XYSegment segment : nanSplit) {
            logList.add(segment.recalcForLog(xAxisLog, yAxisLog));
        }
        String xAxis = xAxisLog ? "log "+xAxisType : xAxisType;
        String yAxis = yAxisLog ? "log "+yAxisType : yAxisType;
        return new XYPlottingData(logList, xAxis, yAxis, label, cssClasses);
    }

    public String createCSSClassParam() {
        String cssClassParam = "";
        if (cssClasses != null && !cssClasses.isEmpty()){
            cssClassParam = "";
            for (String s : cssClasses) {
                cssClassParam += " " + s;
            }
            cssClassParam = "class=\""+cssClassParam.trim()+"\"";
        }
        return cssClassParam;
    }

    /**
     * Output as an SVG g containing polyline. Limit to float precision per SVG spec. Label, phase name and wave type
     * are added as CSS class names.
     *
     * @param writer to write to
     */
    public void asSVG(PrintWriter writer) {
        String cssClassParam = createCSSClassParam();
        writer.println("    <g "+cssClassParam+" tauplabel=\"" + description + "\" " +" >");
        for (XYSegment segment : segmentList) {
            segment.asSVG(writer, "", Outputs.formatStringForAxisType(xAxisType), Outputs.formatStringForAxisType(yAxisType));
        }
        writer.println("    </g> <!-- end "+ description+" -->");
    }

    public void asGMT(PrintWriter writer) {
        String xFormat = Outputs.formatStringForAxisType(xAxisType);
        String yFormat = Outputs.formatStringForAxisType(yAxisType);
        int idx = 1;
        for (XYSegment segment : segmentList) {
            segment.asGMT(writer, idx+"/"+segmentList.size()+" "+label+" "+description, xFormat, yFormat);
            idx++;
        }
    }

    public JSONObject asJSON() {
        JSONObject out = new JSONObject();
        out.put("label", label);
        out.put("description", description);
        out.put("x", xAxisType);
        out.put("y", yAxisType);
        JSONArray segarr = new JSONArray();
        out.put("segments", segarr);
        for (XYSegment seg : segmentList) {
            segarr.put(seg.asJSON());
        }
        return out;
    }

    public final List<XYSegment> segmentList;
    public final String xAxisType;

    public final String yAxisType;

    public final String label;

    public final String description;

    public List<String> cssClasses;
}
