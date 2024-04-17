package edu.sc.seis.TauP;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class XYSegment {

    public XYSegment(double[] x, double[] y) {
        this.x = x;
        this.y = y;
        if (x.length != y.length) {
            throw new IllegalArgumentException("x and y data must be of equal length: "+x.length+" "+y.length);
        }
    }

    public static XYSegment fromSingleList(List<Double> xData, List<Double> yData) {
        double[] rp = new double[xData.size()];
        for (int j = 0; j < rp.length; j++) {
            rp[j] = xData.get(j);
        }
        double[] vals = new double[rp.length];
        for (int j = 0; j < yData.size(); j++) {
            vals[j] = yData.get(j);
        }
        XYSegment seg = new XYSegment(rp, vals);
        return seg;
    }

    public static List<XYSegment> createFromLists(List<double[]> xData, List<double[]> yData) {
        if (xData.size() != yData.size()) {
            throw new IllegalArgumentException("xData and yData must have same size: "+xData.size()+" "+yData.size());
        }
        List<XYSegment> segmentList = new ArrayList<>();
        for (int i = 0; i < xData.size(); i++) {
            segmentList.add(new XYSegment(xData.get(i), yData.get(i)));

        }
        return segmentList;
    }

    public double[] minMax(double[] priorMinMax) {
        double minX = priorMinMax[0];
        double maxX = priorMinMax[1];
        double minY = priorMinMax[2];
        double maxY = priorMinMax[3];
        for (int i = 0; i < x.length; i++) {
            if (Double.isFinite(x[i])) {
                if (x[i] < minX) { minX = x[i];}
                if (x[i] > maxX) { maxX = x[i];}
            }
            if (Double.isFinite(y[i])) {
                if (y[i] < minY) {
                    minY = y[i];
                }
                if (y[i] > maxY) {
                    maxY = y[i];
                }
            }
        }
        double[] out = new double[] { minX, maxX, minY, maxY};
        return out;
    }

    public double[] minMaxInXRange(double[] priorMinMax, double[] xRange) {
        double minX = priorMinMax[0];
        double maxX = priorMinMax[1];
        double minY = priorMinMax[2];
        double maxY = priorMinMax[3];
        for (int i = 0; i < y.length; i++) {
            if (i > 0) {
                for (int j = 0; j < 2; j++) {
                    if ((x[i-1] < xRange[j] && xRange[j] < x[i]) || (xRange[j] > x[i] && x[i-1] > xRange[j] )) {
                        // crosses x boundary
                        double interp = TauP_AbstractPhaseTool.linearInterp(x[i-1], y[i-1], x[i], y[i], xRange[j]);
                        if (interp < minY) { minY = interp;}
                        if (interp > maxY) {maxY = interp;}
                    }
                }
            }
            if ((x[i] - xRange[0])*(x[i]-xRange[1]) < 0) {
                // point inside xRange
                if (y[i] < minY) { minY = y[i];}
                if (y[i] > maxY) {maxY = y[i];}
            }
        }
        double[] out = new double[] { xRange[0], xRange[1], minY, maxY};
        return out;
    }


    public List<XYSegment> recalcForLog(boolean xAxisLog, boolean yAxisLog) {
        List<XYSegment> out = new ArrayList<>();
        double[] outX = new double[x.length];
        double[] outY = new double[y.length];
        int tmpOffset = 0;
        for (int i = 0; i < x.length; i++) {
            if ((xAxisLog && x[i] == 0.0) || (yAxisLog && y[i] == 0.0) || !Double.isFinite(x[i]) || ! Double.isFinite(y[i])) {
                // break due to log zero
                if (tmpOffset > 0) {
                    double[] prex = new double[tmpOffset];
                    System.arraycopy(outX, 0, prex, 0, prex.length);
                    double[] prey = new double[tmpOffset];
                    System.arraycopy(outY, 0, prey, 0, prey.length);
                    out.add(new XYSegment(prex, prey));
                }
                double[] postx = new double[outX.length-tmpOffset-1];
                System.arraycopy(outX, tmpOffset+1, postx, 0, postx.length);
                double[] posty = new double[outX.length-tmpOffset-1];
                System.arraycopy(outY, tmpOffset+1, posty, 0, posty.length);
                outX = postx;
                outY = posty;
                tmpOffset = 0;
            } else {
                outX[tmpOffset] = xAxisLog ? Math.log10(Math.abs(x[i])) : x[i];
                outY[tmpOffset] = yAxisLog ? Math.log10(Math.abs(y[i])) : y[i];
                tmpOffset++;
            }
        }
        if (outX.length > 0) {
            out.add(new XYSegment(outX, outY));
        }
        return out;
    }

    public static XYSegment radianDepthToXY(XYSegment segment, double R) {
        double[] xVal = new double[segment.x.length];
        double[] yVal = new double[xVal.length];
        for (int i = 0; i < xVal.length; i++) {
            double radius = R - segment.y[i];
            double radian = segment.x[i]-Math.PI/2;
            xVal[i] = radius*Math.cos(radian);
            yVal[i] = radius*Math.sin(radian);
        }
        XYSegment out = new XYSegment(xVal, yVal);
        out.cssClasses = List.copyOf(segment.cssClasses);
        out.description = segment.description;
        return out;
    }

    /**
     * Output as an SVG polyline. Limit to float precision per SVG spec.
     *
     * @param writer to write to
     * @param css_class optional class to add to css class attribute
     */
    public void asSVG(PrintWriter writer, String css_class) {
        asSVG(writer, css_class, "%3g", "%3g");
    }
    public void asSVG(PrintWriter writer, String css_class, String xFormat, String yFormat) {

        String cssClassParam = ""+css_class;
        if (cssClasses != null && cssClasses.size()>0){
            cssClassParam = "";
            for (String s : cssClasses) {
                cssClassParam += " " + s;
            }
        }
        cssClassParam = cssClassParam.trim();
        if (cssClassParam.length() > 0) {
            cssClassParam = "class=\""+cssClassParam+"\"";
        }
        writer.println("  <g>");
        writer.println("    <desc>" + description + "</desc>");
        writer.println("    <polyline " + cssClassParam + " points=\"");
        for (int i = 0; i < x.length; i++) {
            float xf = (float)x[i];
            float yf = (float)y[i];
            if (Float.isFinite(xf) && Float.isFinite(yf)) {
                writer.println(String.format(xFormat + " " + yFormat, xf, yf));
            } else if (i != 0 && i != x.length) {
                writer.println("  \"  /> <!-- " + css_class + "-->");
                writer.println("    <polyline " + cssClassParam + " points=\"");
            }
        }
        writer.println("  \"  /> <!-- " + cssClassParam + "-->");
        writer.println("  </g>");

    }


    public void asGMT(PrintWriter writer, String label, String xFormat, String yFormat) {
        writer.println("> "+label);
        for (int i = 0; i < x.length; i++) {
            float xf = (float)x[i];
            float yf = (float)y[i];
            if (Float.isFinite(xf) && Float.isFinite(yf)) {
                writer.println(String.format(xFormat + "  " + yFormat, xf, yf));
            } else if (i != 0 && i != x.length) {
                writer.println("> "+label+" NaN break "+xf+" "+yf);
            }
        }
    }

    /**
     * Output as JSON Object. NaN and Infinity values are skipped per JSON spec.
     * @return
     */
    public JSONObject asJSON() {
        JSONObject out = new JSONObject();
        JSONArray xarr = new JSONArray();
        JSONArray yarr = new JSONArray();
        for (int i = 0; i < x.length; i++) {
            if ( Double.isFinite(x[i]) && Double.isFinite(y[i])) {
                // skip NaN/Infinity values due to JSON limitation
                xarr.put(x[i]);
                yarr.put(y[i]);
            }
        }
        out.put("x", xarr);
        out.put("y", yarr);
        return out;
    }

    public final double[] x;
    public final double[] y;

    public List<String> cssClasses = new ArrayList<>();
    public String description = "";
}
