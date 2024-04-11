package edu.sc.seis.TauP;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class XYPlottingData {

    public XYPlottingData(List<XYSegment> segments, String xAxisType, String yAxisType, String label, List<String> cssClasses) {
        segmentList = segments;
        this.xAxisType = xAxisType;
        this.yAxisType = yAxisType;
        this.label = label;
        this.cssClasses = cssClasses;
    }

    public static final int MIN_IDX = 0;
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

    public XYPlottingData recalcForLog(boolean xAxisLog, boolean yAxisLog) {
        List<XYSegment> out = new ArrayList<>();
        for (XYSegment segment : segmentList) {
            out.addAll(segment.recalcForLog(xAxisLog, yAxisLog));
        }
        String xAxis = xAxisLog ? "log "+xAxisType : xAxisType;
        String yAxis = yAxisLog ? "log "+yAxisType : yAxisType;
        return new XYPlottingData(out, xAxis, yAxis, label, cssClasses);
    }

    public String createCSSClassParam() {
        String cssClassParam = "";
        if (cssClasses != null && cssClasses.size()>0){
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
        writer.println("    <g "+cssClassParam+" tauplabel=\"" + label + "\" " +" >");
        for (XYSegment segment : segmentList) {
            segment.asSVG(writer, "", Outputs.formatStringForAxisType(xAxisType), Outputs.formatStringForAxisType(yAxisType));
        }
        writer.println("    </g> <!-- end "+ label+" -->");
    }

    public void asGMT(PrintWriter writer) {
        String xFormat = Outputs.formatStringForAxisType(xAxisType);
        String yFormat = Outputs.formatStringForAxisType(yAxisType);
        for (XYSegment segment : segmentList) {
            segment.asGMT(writer, label, xFormat, yFormat);
        }
    }

    public JSONObject asJSON() {
        JSONObject out = new JSONObject();
        out.put("label", label);
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

    public List<String> cssClasses = new ArrayList<>();
}
