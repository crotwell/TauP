package edu.sc.seis.TauP;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Line, possbily in multiple segments, representing a single data type in an xy plot.
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

    public XYPlottingData trimToMinMax(double[] xAxisMinMax, double[] yAxisMinMax) {
        List<XYSegment> outSeg = new ArrayList<>();
        for (XYSegment seg : segmentList) {
            List<XYSegment> trimmed = seg.trimToMinMax(xAxisMinMax, yAxisMinMax);
            if (trimmed != null) {
                outSeg.addAll(trimmed);
            }
        }
        XYPlottingData out = new XYPlottingData(outSeg, xAxisType, yAxisType, label, description, cssClasses);
        return out;
    }

    public static List<XYPlottingData> trimAllToMinMax(List<XYPlottingData> xyList, double[] xAxisMinMax, double[] yAxisMinMax) {
        if (xAxisMinMax.length == 2 || yAxisMinMax.length == 2) {
            List<XYPlottingData> trimmed = new ArrayList<>();
            for (XYPlottingData xyp : xyList) {
                XYPlottingData t = xyp.trimToMinMax(xAxisMinMax, yAxisMinMax);
                if (t != null) {
                    trimmed.add(t);
                }
            }
            xyList = trimmed;
        }
        return xyList;
    }

    public XYPlottingData recalcForAbs(boolean xAxisAbs, boolean yAxisAbs) {
        List<XYSegment> out = new ArrayList<>();
        for (XYSegment segment : segmentList) {
            out.add(segment.recalcForAbs(xAxisAbs, yAxisAbs));
        }
        String xAxis = xAxisAbs ? "abs "+xAxisType : xAxisType;
        String yAxis = yAxisAbs ? "abs "+yAxisType : yAxisType;
        return new XYPlottingData(out, xAxis, yAxis, label, description, cssClasses);
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
        return new XYPlottingData(logList, xAxis, yAxis, label, description, cssClasses);
    }

    public String cssClassesAsString() {
        String cssClassParam = "";
        if (cssClasses != null && !cssClasses.isEmpty()) {
            for (String s : cssClasses) {
                cssClassParam += " " + s;
            }
        }
        return cssClassParam.trim();
    }

    public String createCSSClassParam() {
        String cssClassParam = cssClassesAsString();
        if (cssClasses != null && !cssClasses.isEmpty()){
            cssClassParam = "class=\""+cssClassParam+"\"";
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
            segment.asSVG(writer, "");
        }
        writer.println("    </g> <!-- end "+ description+" -->");
    }

    public void asGMT(PrintWriter writer) {
        int idx = 1;
        for (XYSegment segment : segmentList) {
            segment.asGMT(writer, idx+"/"+segmentList.size()+" "+label+" "+description);
            idx++;
        }
    }

    public final List<XYSegment> segmentList;
    public final String xAxisType;

    public final String yAxisType;

    public final String label;

    public final String description;

    public List<String> cssClasses;
}
