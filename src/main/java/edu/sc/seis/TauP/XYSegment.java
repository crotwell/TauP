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

    public static List<XYSegment> createFromLists(List<double[]> xData, List<double[]> yData) {
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

    /**
     * Output as an SVG polyline. Limit to float precision per SVG spec.
     *
     * @param writer to write to
     * @param css_class optional class to add to css class attribute
     */
    public void asSVG(PrintWriter writer, String css_class) {
        writer.println("    <polyline class=\"" + css_class + "\" points=\"");
        for (int i = 0; i < x.length; i++) {
            float xf = (float)x[i];
            float yf = (float)y[i];
            if (Float.isFinite(xf) && Float.isFinite(yf)) {
                writer.println(xf + " " + yf);
            } else if (i != 0 && i != x.length) {
                writer.println("  \"  /> <!-- " + css_class + "-->");
                writer.println("    <polyline class=\"" + css_class + "\" points=\"");
            }
        }
        writer.println("  \"  /> <!-- " + css_class + "-->");
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

}
