package edu.sc.seis.TauP;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Segment of a XYPlottineData line in an xy plot.
 */
public class XYSegment {

    public XYSegment(double[] x, double[] y) {
        this.x = x;
        this.y = y;
        if (x.length != y.length) {
            throw new IllegalArgumentException("x and y data must be of equal length: " + x.length + " " + y.length);
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
        return new XYSegment(rp, vals);
    }

    public static List<XYSegment> createFromLists(List<double[]> xData, List<double[]> yData) {
        if (xData.size() != yData.size()) {
            throw new IllegalArgumentException("xData and yData must have same size: " + xData.size() + " " + yData.size());
        }
        List<XYSegment> segmentList = new ArrayList<>();
        for (int i = 0; i < xData.size(); i++) {
            segmentList.add(new XYSegment(xData.get(i), yData.get(i)));

        }
        return segmentList;
    }

    public List<XYSegment> trimToMinMax(double[] xAxisMinMax, double[] yAxisMinMax) {
        List<XYSegment> outList = new ArrayList<>();
        List<Double> outX = new ArrayList<>();
        List<Double> outY = new ArrayList<>();
        if (xAxisMinMax.length == 2 && yAxisMinMax.length == 0) {
            for (int i = 0; i < x.length; i++) {
                if (xAxisMinMax[0] <= x[i] && x[i] < xAxisMinMax[1] ) {
                    if (i>0) {
                        if ( x[i-1] < xAxisMinMax[0] && xAxisMinMax[0] < x[i]) {
                            outX.add(xAxisMinMax[0]);
                            double yInterp = LinearInterpolation.linearInterp(x[i -1], y[i -1], x[i], y[i], xAxisMinMax[0]);
                            outY.add(yInterp);
                        } else if (x[i-1] > xAxisMinMax[1] && xAxisMinMax[1] > x[i]) {
                            double yInterp = LinearInterpolation.linearInterp(x[i-1], y[i-1], x[i], y[i], xAxisMinMax[1]);
                            outX.add(xAxisMinMax[1]);
                            outY.add(yInterp);
                        }
                    }
                    outX.add( x[i]);
                    outY.add( y[i]);
                } else if (outX.size()>0) {
                    if (i>0) {
                        if ( x[i-1] > xAxisMinMax[0] && xAxisMinMax[0] > x[i]) {
                            outX.add(xAxisMinMax[0]);
                            double yInterp = LinearInterpolation.linearInterp(x[i - 1], y[i - 1], x[i], y[i], xAxisMinMax[0]);
                            outY.add(yInterp);
                        } else if (x[i-i] < xAxisMinMax[1] && xAxisMinMax[1] < x[i]) {
                            double yInterp = LinearInterpolation.linearInterp(x[i - 1], y[i - 1], x[i], y[i], xAxisMinMax[1]);
                            outX.add(xAxisMinMax[1]);
                            outY.add(yInterp);
                        }
                    }
                    double[] xarr = Stream.of(outX.toArray(new Double[0])).mapToDouble(Double::doubleValue).toArray();
                    double[] yarr = Stream.of(outY.toArray(new Double[0])).mapToDouble(Double::doubleValue).toArray();
                    XYSegment trimSeg = new XYSegment(xarr, yarr);
                    trimSeg.cssClasses = cssClasses;
                    trimSeg.description = description;
                    outList.add(trimSeg);
                    outX = new ArrayList<>();
                    outY = new ArrayList<>();

                }
            }
        } else if (xAxisMinMax.length == 0 && yAxisMinMax.length == 2) {
            for (int i = 0; i < x.length; i++) {
                if (yAxisMinMax[0] <= y[i] && y[i] <= yAxisMinMax[1]) {
                    if (i>0) {
                        if ( y[i-1] < yAxisMinMax[0] && yAxisMinMax[0] < y[i]) {
                            outY.add(yAxisMinMax[0]);
                            double xInterp = LinearInterpolation.linearInterp(y[i-1 ], x[i -1], y[i], x[i], yAxisMinMax[0]);
                            outX.add(xInterp);
                        } else if (y[i-1] > yAxisMinMax[1] && yAxisMinMax[1] > y[i]) {
                            double xInterp = LinearInterpolation.linearInterp(y[i -1], x[i -1], y[i], x[i], yAxisMinMax[1]);
                            outY.add(yAxisMinMax[1]);
                            outX.add(xInterp);
                        }
                    }
                    outX.add( x[i]);
                    outY.add( y[i]);
                } else if (outX.size()>0) {
                    if (i>0) {
                        if ( y[i-1] > yAxisMinMax[0] && yAxisMinMax[0] > y[i]) {
                            outY.add(yAxisMinMax[0]);
                            double xInterp = LinearInterpolation.linearInterp(y[i - 1], x[i - 1], y[i], x[i], yAxisMinMax[0]);
                            outX.add(xInterp);
                        } else if (y[i-i] < yAxisMinMax[1] && yAxisMinMax[1] < y[i]) {
                            double xInterp = LinearInterpolation.linearInterp(y[i - 1], x[i - 1], y[i], x[i], yAxisMinMax[1]);
                            outY.add(yAxisMinMax[1]);
                            outX.add(xInterp);
                        }
                    }
                    double[] xarr = Stream.of(outX.toArray(new Double[0])).mapToDouble(Double::doubleValue).toArray();
                    double[] yarr = Stream.of(outY.toArray(new Double[0])).mapToDouble(Double::doubleValue).toArray();
                    XYSegment trimSeg = new XYSegment(xarr, yarr);
                    trimSeg.cssClasses = cssClasses;
                    trimSeg.description = description;
                    outList.add(trimSeg);
                    outX = new ArrayList<>();
                    outY = new ArrayList<>();

                }
            }
        } else if (xAxisMinMax.length == 2 && yAxisMinMax.length == 2) {
            for (int i = 0; i < x.length; i++) {
                if (xAxisMinMax[0] <= x[i] && x[i] < xAxisMinMax[1] && yAxisMinMax[0] <= y[i] && y[i] <= yAxisMinMax[1]) {
                    if (i>0) {
                        if ( x[i-1] < xAxisMinMax[0] && xAxisMinMax[0] < x[i]) {
                            outX.add(xAxisMinMax[0]);
                            double yInterp = LinearInterpolation.linearInterp(x[i-1], y[i-1 ], x[i], y[i], xAxisMinMax[0]);
                            outY.add(yInterp);
                        } else if (x[i-1] > xAxisMinMax[1] && xAxisMinMax[1] > x[i]) {
                            double yInterp = LinearInterpolation.linearInterp(x[i-1], y[i-1], x[i], y[i], xAxisMinMax[1]);
                            outX.add(xAxisMinMax[1]);
                            outY.add(yInterp);
                        } else if ( y[i-1] < yAxisMinMax[0] && yAxisMinMax[0] < y[i]) {
                            outY.add(yAxisMinMax[0]);
                            double xInterp = LinearInterpolation.linearInterp(y[i-1 ], x[i-1 ], y[i], x[i], yAxisMinMax[0]);
                            outX.add(xInterp);
                        } else if (y[i-1] > yAxisMinMax[1] && yAxisMinMax[1] > y[i]) {
                            double xInterp = LinearInterpolation.linearInterp(y[i-1 ], x[i -1], y[i], x[i], yAxisMinMax[1]);
                            outY.add(yAxisMinMax[1]);
                            outX.add(xInterp);
                        }
                    }
                    outX.add( x[i]);
                    outY.add( y[i]);
                } else if (outX.size()>0) {
                    if (i>0) {
                        if ( x[i-1] > xAxisMinMax[0] && xAxisMinMax[0] > x[i]) {
                            outX.add(xAxisMinMax[0]);
                            double yInterp = LinearInterpolation.linearInterp(x[i - 1], y[i - 1], x[i], y[i], xAxisMinMax[0]);
                            outY.add(yInterp);
                        } else if (x[i-i] < xAxisMinMax[1] && xAxisMinMax[1] < x[i]) {
                            double yInterp = LinearInterpolation.linearInterp(x[i - 1], y[i - 1], x[i], y[i], xAxisMinMax[1]);
                            outX.add(xAxisMinMax[1]);
                            outY.add(yInterp);
                        } else if ( y[i-1] > yAxisMinMax[0] && yAxisMinMax[0] > y[i]) {
                            outY.add(yAxisMinMax[0]);
                            double xInterp = LinearInterpolation.linearInterp(y[i - 1], x[i - 1], y[i], x[i], yAxisMinMax[0]);
                            outX.add(xInterp);
                        } else if (y[i-i] < yAxisMinMax[1] && yAxisMinMax[1] < y[i]) {
                            double xInterp = LinearInterpolation.linearInterp(y[i - 1], x[i - 1], y[i], x[i], yAxisMinMax[1]);
                            outY.add(yAxisMinMax[1]);
                            outX.add(xInterp);
                        }
                    }
                    double[] xarr = Stream.of(outX.toArray(new Double[0])).mapToDouble(Double::doubleValue).toArray();
                    double[] yarr = Stream.of(outY.toArray(new Double[0])).mapToDouble(Double::doubleValue).toArray();
                    XYSegment trimSeg = new XYSegment(xarr, yarr);
                    trimSeg.cssClasses = cssClasses;
                    trimSeg.description = description;
                    outList.add(trimSeg);
                    outX = new ArrayList<>();
                    outY = new ArrayList<>();

                }
            }
        } else {
            // no trim, both zero length
            return List.of(this);
        }
        if (outX.size()>0) {
            double[] xarr = Stream.of(outX.toArray(new Double[0])).mapToDouble(Double::doubleValue).toArray();
            double[] yarr = Stream.of(outY.toArray(new Double[0])).mapToDouble(Double::doubleValue).toArray();
            XYSegment trimSeg = new XYSegment(xarr, yarr);
            trimSeg.cssClasses = cssClasses;
            trimSeg.description = description;
            outList.add(trimSeg);
        }
        return outList;
    }

    public double[] minMax(double[] priorMinMax) {
        double minX = priorMinMax[0];
        double maxX = priorMinMax[1];
        double minY = priorMinMax[2];
        double maxY = priorMinMax[3];
        for (int i = 0; i < x.length; i++) {
            if (Double.isFinite(x[i])) {
                if (x[i] < minX) {
                    minX = x[i];
                }
                if (x[i] > maxX) {
                    maxX = x[i];
                }
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
        return new double[]{minX, maxX, minY, maxY};
    }

    public double[] minMaxInXRange(double[] priorMinMax, double[] xRange) {
        double minY = priorMinMax[2];
        double maxY = priorMinMax[3];
        for (int i = 0; i < y.length; i++) {
            if (i > 0) {
                for (int j = 0; j < 2; j++) {
                    if ((x[i - 1] < xRange[j] && xRange[j] < x[i]) || (xRange[j] > x[i] && x[i - 1] > xRange[j])) {
                        // crosses x boundary
                        double interp = LinearInterpolation.linearInterp(x[i - 1], y[i - 1], x[i], y[i], xRange[j]);
                        if (interp < minY) {
                            minY = interp;
                        }
                        if (interp > maxY) {
                            maxY = interp;
                        }
                    }
                }
            }
            if ((x[i] - xRange[0]) * (x[i] - xRange[1]) < 0) {
                // point inside xRange
                if (y[i] < minY) {
                    minY = y[i];
                }
                if (y[i] > maxY) {
                    maxY = y[i];
                }
            }
        }
        return new double[]{xRange[0], xRange[1], minY, maxY};
    }


    public double[] minMaxInYRange(double[] priorMinMax, double[] yRange) {
        double minX = priorMinMax[0];
        double maxX = priorMinMax[1];
        for (int i = 0; i < x.length; i++) {
            if (i > 0) {
                for (int j = 0; j < 2; j++) {
                    if ((y[i - 1] < yRange[j] && yRange[j] < y[i]) || (yRange[j] > y[i] && y[i - 1] > yRange[j])) {
                        // crosses y boundary
                        double interp = LinearInterpolation.linearInterp(y[i - 1], x[i - 1], y[i], x[i], yRange[j]);
                        if (interp < minX) {
                            minX = interp;
                        }
                        if (interp > maxX) {
                            maxX = interp;
                        }
                    }
                }
            }
            if ((y[i] - yRange[0]) * (y[i] - yRange[1]) < 0) {
                // point inside xRange
                if (x[i] < minX) {
                    minX = x[i];
                }
                if (x[i] > maxX) {
                    maxX = x[i];
                }
            }
        }
        return new double[]{minX, maxX, yRange[0], yRange[1]};
    }


    public XYSegment recalcForAbs(boolean xAxisAbs, boolean yAxisAbs) {
        double[] outX = new double[x.length];
        double[] outY = new double[y.length];
        for (int i = 0; i < x.length; i++) {
            outX[i] = xAxisAbs ? Math.abs(x[i]) : x[i];
            outY[i] = yAxisAbs ? Math.abs(y[i]) : y[i];
        }
        return new XYSegment(outX, outY);
    }
    public XYSegment recalcForLog(boolean xAxisLog, boolean yAxisLog) {
        double[] outX = new double[x.length];
        double[] outY = new double[y.length];
        for (int i = 0; i < x.length; i++) {
            outX[i] = xAxisLog ? Math.log10(Math.abs(x[i])) : x[i];
            outY[i] = yAxisLog ? Math.log10(Math.abs(y[i])) : y[i];
        }
        return new XYSegment(outX, outY);
    }

    /**
     * Splits the segment around any NaN values, or optionally for any zero values, ie for log.
     */
    public List<XYSegment> recalcForInfinite(boolean xAxisSplitZero, boolean yAxisSplitZero) {
        List<XYSegment> out = new ArrayList<>();
        double[] outX = new double[x.length];
        double[] outY = new double[y.length];
        int tmpOffset = 0;
        for (int i = 0; i < x.length; i++) {
            if ( ((xAxisSplitZero && x[i] == 0.0) || (yAxisSplitZero && y[i] == 0.0))
                || !Double.isFinite(x[i]) || ! Double.isFinite(y[i])) {
                // break due to NaN or zero for log which would become NaN
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
                outX[tmpOffset] = x[i];
                outY[tmpOffset] = y[i];
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
        asSVG(writer, css_class, "%f", "%f");
    }

    public void asSVG(PrintWriter writer, String css_class, String xFormat, String yFormat) {

        String cssClassParam = ""+css_class;
        if (cssClasses != null && !cssClasses.isEmpty()){
            cssClassParam = "";
            for (String s : cssClasses) {
                cssClassParam += " " + s;
            }
        }
        cssClassParam = cssClassParam.trim();
        if (!cssClassParam.isEmpty()) {
            cssClassParam = "class=\""+cssClassParam+"\"";
        }
        writer.println("  <g>");
        writer.println("    <desc>" + description + "</desc>");
        writer.println("    <polyline " + cssClassParam + " points=\"");
        boolean priorIsFinite = true;
        for (int i = 0; i < x.length; i++) {
            float xf = (float)x[i];
            float yf = (float)y[i];
            if (Float.isFinite(xf) && Float.isFinite(yf)) {
                //writer.println(String.format(xFormat + " " + yFormat, xf, yf));
                writer.println(xf+"  "+ yf);
                priorIsFinite = true;
            } else if (i != 0 && i != x.length && priorIsFinite) {
                writer.println("  \"  /> <!-- " + css_class + "-->");
                writer.println("    <polyline " + cssClassParam + " points=\"");
            }
        }
        writer.println("  \"  /> <!-- " + cssClassParam + "-->");
        writer.println("  </g>");

    }


    public void asGMT(PrintWriter writer, String label) {
        asGMT(writer, label, "%3g", "%3g");
    }

    public void asGMT(PrintWriter writer, String label, String xFormat, String yFormat) {
        writer.println("> "+label);
        for (int i = 0; i < x.length; i++) {
            float xf = (float)x[i];
            float yf = (float)y[i];
            boolean priorIsFinite = true;
            if (Float.isFinite(xf) && Float.isFinite(yf)) {
                writer.println(String.format(xFormat + "  " + yFormat, xf, yf));
                priorIsFinite = true;
            } else if (i != 0 && i != x.length && priorIsFinite) {
                writer.println("> "+label+" NaN break "+xf+" "+yf);
            }
        }
    }

    public final double[] x;
    public final double[] y;

    public List<String> cssClasses = new ArrayList<>();
    public String description = "";
}
