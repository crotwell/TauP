package edu.sc.seis.TauP;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class XYPlottingData {
    public XYPlottingData(double[] xValues, String xAxisType, double[] yValues, String yAxisType, String label, SeismicPhase phase) {
        segmentList.add(new XYSegment(xValues, yValues));
        this.xAxisType = xAxisType;
        this.yAxisType = yAxisType;
        this.label = label;
        this.phase = phase;

    }
    public XYPlottingData(List<double[]> xData, String xAxisType, List<double[]> yData, String yAxisType, String label, SeismicPhase phase) {
        this(XYSegment.createFromLists(xData, yData), xAxisType, yAxisType, phase.getName(), phase);
    }

    public XYPlottingData(List<XYSegment> segments, String xAxisType, String yAxisType, String label, SeismicPhase phase) {
        segmentList.addAll(segments);
        this.xAxisType = xAxisType;
        this.yAxisType = yAxisType;
        this.label = label;
        this.phase = phase;

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


    public XYPlottingData recalcForLog(boolean xAxisLog, boolean yAxisLog) {
        List<XYSegment> out = new ArrayList<>();
        for (XYSegment segment : segmentList) {
            out.addAll(segment.recalcForLog(xAxisLog, yAxisLog));
        }
        return new XYPlottingData(out, xAxisType, yAxisType, label, phase);
    }

    public void asSVG(PrintWriter writer) {
        String p_or_s = "both_p_swave";
        if (phase.isAllSWave()) {
            p_or_s = "swave";
        } else if (phase.isAllPWave()) {
            p_or_s = "pwave";
        }
        writer.println("    <g class=\"" + phase.getName()+" "+ label + " " +p_or_s +"\">");
        for (XYSegment segment : segmentList) {
            segment.asSVG(writer, p_or_s);
        }
        writer.println("    </g> <!-- end "+phase.getName()+" "+ label+ " " +p_or_s +" -->");
    }

    public JSONObject asJSON() {
        JSONObject out = new JSONObject();
        out.put("phase", phase.getName());
        JSONArray segarr = new JSONArray();
        out.put("segments", segarr);
        for (XYSegment seg : segmentList) {
            segarr.put(seg.asJSON());
        }
        return out;
    }

    public final SeismicPhase phase;

    public final List<XYSegment> segmentList = new ArrayList<>();
    public final String xAxisType;

    public final String yAxisType;

    public final String label;
}
