package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cmdline.TauP_Tool;
import edu.sc.seis.TauP.cmdline.args.ColorType;
import edu.sc.seis.TauP.cmdline.args.ColoringArgs;
import edu.sc.seis.TauP.cmdline.args.GraphicOutputTypeArgs;
import edu.sc.seis.TauP.cmdline.args.ModelArgs;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static edu.sc.seis.TauP.AxisType.*;
import static edu.sc.seis.TauP.cmdline.TauP_AbstractPhaseTool.baseResultAsJSONObject;
import static edu.sc.seis.TauP.cmdline.args.ModelArgs.depthsToString;

public class XYPlotOutput {

    public XYPlotOutput(List<XYPlottingData> xyPlots, ModelArgs modelArgs) {
        this.xyPlots = xyPlots;
        this.modelArgs = modelArgs;
    }

    public static void checkEqualMinMax(double[] minmax, double xpercent, double ypercent) {
        if (minmax[0] == minmax[1]) {
            // x axis min=max
            if (minmax[0] == 0.0) {
                // min = max = zero, so go +-1
                minmax[0] = -1;
                minmax[1] = 1;
            } else {
                // 10%
                double shift = Math.abs(minmax[0]) * xpercent;
                minmax[0] = minmax[0] - shift;
                minmax[1] = minmax[1] + shift;
            }
        }
        if (minmax[2] == minmax[3]) {
            // y axis min=max
            if (minmax[2] == 0.0) {
                // min = max = zero, so go +-1
                minmax[2] = -1;
                minmax[3] = 1;
            } else {
                // 10%
                double shift = Math.abs(minmax[2]) * ypercent;
                minmax[2] = minmax[2] - shift;
                minmax[3] = minmax[3] + shift;
            }
        }
    }

    public static List<XYPlottingData> recalcForAbs(List<XYPlottingData> xy, boolean xAxisAbs, boolean yAxisAbs) {
        List<XYPlottingData> out = new ArrayList<>();
        for(XYPlottingData xyp : xy) {
            out.add( xyp.recalcForAbs(xAxisAbs, yAxisAbs));
        }
        return out;
    }


    public static List<XYPlottingData> recalcForLog(List<XYPlottingData> xy, boolean xAxisLog, boolean yAxisLog) {
        List<XYPlottingData> out = new ArrayList<>();
        for(XYPlottingData xyp : xy) {
            out.add( xyp.recalcForLog(xAxisLog, yAxisLog));
        }
        return out;
    }

    public void setPhaseNames(List<PhaseName> phaseNames) {
        this.phaseNames = phaseNames;
    }

    public void setxAxisMinMax(double[] minMax) {
        this.xAxisMinMax = minMax;
    }
    public void setyAxisMinMax(double[] minMax) {
        this.yAxisMinMax = minMax;
    }

    public JSONObject asJSON() {
        JSONObject out;
        if (modelArgs != null ) {
            out = baseResultAsJSONObject( modelArgs.getModelName(), modelArgs.getSourceDepth(),
                    modelArgs.getReceiverDepth(), phaseNames);
        } else {
            out = new JSONObject();
        }
        JSONArray phaseCurves = new JSONArray();
        for (XYPlottingData plotItem : xyPlots) {
            phaseCurves.put(plotItem.asJSON());
        }
        out.put("curves", phaseCurves);
        return out;
    }

    public void printAsJSON(PrintWriter writer, int indentFactor) {
        writer.println(asJSON().toString(indentFactor));
    }

    public void printAsGmtScript(PrintWriter writer,
                                 String toolname, List<String> cmdLineArgs,
                                 GraphicOutputTypeArgs outputTypeArgs,
                                 boolean isLegend) {
        printGmtScriptBeginning(writer, toolname, cmdLineArgs, outputTypeArgs);
        int idx = 0;
        for (XYPlottingData xyplotItem : xyPlots) {
            String lineColor = "";
            if (coloringArgs.getColoring() == ColorType.auto || coloringArgs.getColoring() == ColorType.phase) {
                lineColor = "-W,"+coloringArgs.colorForIndex(idx);
            } else if (coloringArgs.getColoring() == ColorType.wavetype) {
                HashMap<String, String> colorMap = coloringArgs.getWavetypeColors();
                for (String colorval : colorMap.keySet()) {
                    if (xyplotItem.cssClasses.contains(colorval)) {
                        lineColor= "-W,"+colorMap.get(colorval);
                        break;
                    }
                }
            } else if (coloringArgs.getColoring() == ColorType.none) {
                lineColor= "-W,black";
            }

            writer.println("# "+xyplotItem.label);
            // +" -lxxx"+xyplotItem.label
            writer.println("gmt plot "+" -l'"+xyplotItem.label+"' "+lineColor+" <<END");
            xyplotItem.asGMT(writer);
            writer.println("END");
            writer.println("# end of "+xyplotItem.label);
            idx++;
        }
        if (isLegend) {
            printGmtScriptLegend(writer);
        }
        TauP_Tool.endGmtAndCleanUp(writer);
    }

    public void printGmtScriptBeginning(PrintWriter writer,
                                        String toolname, List<String> cmdLineArgs,
                                        GraphicOutputTypeArgs outputTypeArgs) {
        String psFile = outputTypeArgs.getOutFileBase();
        writer.println("#!/usr/bin/env bash");
        SvgUtil.taupMetadataGMT(writer, toolname, cmdLineArgs, null);
        writer.println("gmt begin " + psFile + "\n");
        double[] minmax = calcMinMax();
        ArrayList<Double> xTicks = PlotTicks.getTicks(minmax[0], minmax[1], numXTicks, false);
        double xTickStep = xTicks.size()>1 ? xTicks.get(1) - xTicks.get(0) : 1;
        ArrayList<Double> yTicks = PlotTicks.getTicks(minmax[2], minmax[3], numYTicks, false);
        double yTickStep = yTicks.size()>1 ? yTicks.get(1) - yTicks.get(0) : 1;
        String xLabelParam = " -Bxa"+xTickStep;
        if (!getXLabel().isEmpty()) {
            xLabelParam+="+l'"+getXLabel()+"'";
        }
        String yLabelParam = " -Bya"+yTickStep;
        if (!getYLabel().isEmpty()) {
            yLabelParam += "+l'"+getYLabel()+"'";
        }
        String xScale = outputTypeArgs.mapwidth + outputTypeArgs.mapWidthUnit;
        if (isxAxisInvert()) {
            xScale = "-"+xScale;
        }
        String yScale = outputTypeArgs.mapwidth + outputTypeArgs.mapWidthUnit;
        if (isyAxisInvert()) {
            yScale = "-"+yScale;
        }
        String dataRange = " -R"+minmax[0]+"/"+minmax[1]+"/" + minmax[2] + "/" + minmax[3];
        writer.println("gmt basemap -JX" + xScale+"/"+yScale + dataRange
                + xLabelParam+yLabelParam+" -BWSne+t'" + getTitle() + "' " );

    }

    public void printGmtScriptLegend(PrintWriter writer) {
        writer.println("gmt legend -DjTL+o0.25i -Mh -F ");
    }

    public void printAsGmtText(PrintWriter writer) {
        for (XYPlottingData xyplotItem : xyPlots) {
            xyplotItem.asGMT(writer);
        }
    }

    public double[] calcMinMax() {
        double[] minmax = XYPlottingData.initMinMax();
        for (XYPlottingData xyplot : xyPlots) {
            if (xAxisMinMax.length == 2 && yAxisMinMax.length == 0) {
                // given x range, find y range
                minmax = xyplot.minMaxInXRange(minmax, xAxisMinMax);
            } else if (xAxisMinMax.length == 0 && yAxisMinMax.length == 2) {
                // given x range, find y range
                minmax = xyplot.minMaxInYRange(minmax, yAxisMinMax);
            } else {
                minmax = xyplot.minMax(minmax);
            }
        }
        // override minmax with user supplied if
        if (xAxisMinMax.length == 2) {
            minmax[0] = xAxisMinMax[0];
            minmax[1] = xAxisMinMax[1];
        }
        if (yAxisMinMax.length == 2) {
            minmax[2] = yAxisMinMax[0];
            minmax[3] = yAxisMinMax[1];
        }
        // but make sure not equal, widen by 10% if values are same?
        checkEqualMinMax(minmax, 0.05, 0.05);
        return minmax;
    }



    public void printAsHtml(PrintWriter writer, String toolname, List<String> cmdLineArgs, String extraCSS, boolean isLegend) {
        writer.println("<!DOCTYPE html>");
        writer.println("<html><body>");
        printAsSvg(writer, toolname, cmdLineArgs, extraCSS, isLegend);
        writer.println("</body></html>");
    }

    public void printAsSvg(PrintWriter writer, String toolname, List<String> cmdLineArgs,
                           String extraCSS, boolean isLegend) {

        int margin = 80;
        int pixelWidth = 600+margin;//Math.round(72*mapWidth);
        double[] minmax = calcMinMax();
        SvgUtil.xyplotScriptBeginning(writer, toolname,
            cmdLineArgs,  pixelWidth, margin, coloringArgs.getColorList(), extraCSS, minmax);

        float plotWidth = pixelWidth - 2*margin;
        double[] axisMinMax = new double[4];
        System.arraycopy(minmax, 0, axisMinMax, 0, minmax.length);
        // flipping minmax will invert axis as drawn
        if (xAxisInvert) {
            double tmp = axisMinMax[1];
            axisMinMax[1] = axisMinMax[0];
            axisMinMax[0] = tmp;
        }
        if (yAxisInvert) {
            double tmp = axisMinMax[3];
            axisMinMax[3] = axisMinMax[2];
            axisMinMax[2] = tmp;
        }

        int xflip = xAxisInvert ? -1 : 1;
        int yflip = yAxisInvert ? -1 : 1;
        float xfliptrans = xAxisInvert ? -1*(float)(minmax[1]+minmax[0]) : 0;
        float yfliptrans = yAxisInvert ? -1*(float)(minmax[3]+minmax[2]) : 0;
        float xtrans = (float)  minmax[0];
        float ytrans = (float)  minmax[2];

        SvgUtil.createXYAxes(writer, axisMinMax[0], axisMinMax[1], numXTicks, false,
                axisMinMax[2], axisMinMax[3], numYTicks, false,
                pixelWidth, margin,
                getTitle(),
                getXLabel(), getYLabel());

        writer.println("<g clip-path=\"url(#curve_clip)\">");

        writer.println("<g transform=\"scale(1,-1) translate(0, -"+plotWidth+")\">");

        writer.println("<g transform=\"scale(" + (plotWidth / (minmax[1]-minmax[0])) + "," + ( plotWidth / (minmax[3]-minmax[2])) + ")\" >");
        writer.println("<g transform=\"translate("+(-1*xtrans)+", "+(-1*ytrans)+")\">");
        if (coloringArgs.getColoring() == ColorType.auto) {
            writer.println("    <g class=\"autocolor\">");
        }
        for (XYPlottingData xyplotItem : xyPlots) {
            if (xAxisInvert || yAxisInvert) {
                writer.println("<g transform=\"scale(" + xflip + "," + yflip + ") translate("+xfliptrans+", "+yfliptrans+")\" > <!-- invert axis -->");
            }
            xyplotItem.asSVG(writer);
            if (xAxisInvert || yAxisInvert) {
                writer.println("    </g> <!-- end invert axis -->");
            }
        }

        if (coloringArgs.getColoring() == ColorType.auto) {
            writer.println("    </g> <!-- end autocolor g -->");
        }

        writer.println("    <g class=\"phasename\">  <!-- begin labels -->");

        writer.println("    </g> <!-- end labels -->");


        writer.println("  </g> <!-- end translate -->");


        writer.println("  </g> <!-- end scale -->");
        writer.println("  </g> <!-- end scaletranslate -->");
        writer.println("  </g> <!-- end clip-path -->");

        if (isLegend) {
            List<String> labels = new ArrayList<>();
            List<String> labelClasses = new ArrayList<>();
            for (XYPlottingData xyp : xyPlots) {
                labels.add(xyp.label);
                labelClasses.add(xyp.cssClassesAsString());
            }

            String autocolorStr = coloringArgs.getColoring() == ColorType.auto ? "autocolor" : "";
            SvgUtil.createLegend(writer, labels, labelClasses, autocolorStr , (int) (plotWidth * .1), (int) (plotWidth * .1));
        }
        writer.println("</svg>");
    }

    public String getTitle() {
        if (title == null && modelArgs != null) {
            return modelArgs.getModelName() + " (h=" + depthsToString(modelArgs.getSourceDepth()) + " km)";
        } else {
            return title;
        }
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getXLabel() {
        return xLabel;
    }
    public void setXLabel(String x) {
        this.xLabel = x;
    }

    String xLabel = "";

    public String getYLabel() {
        return yLabel;
    }
    public void setYLabel(String y) {
        this.yLabel = y;
    }
    String yLabel = "";

    public XYPlotOutput convertToCartesian() throws TauPException {
        List<XYPlottingData> convXYPlotList = new ArrayList<>();
        for (XYPlottingData xyp : xyPlots) {
            if ( ! (Objects.equals(xyp.xAxisType, radian.name())
                        || Objects.equals(xyp.xAxisType, degree180.name())
                        || Objects.equals(xyp.xAxisType, degree.name())
                        || Objects.equals(xyp.yAxisType, ModelAxisType.depth.name())
                        || Objects.equals(xyp.yAxisType, ModelAxisType.radius.name())
                )) {
                throw new TauPException("Unable to convert to cartesian for axis: "+xyp.xAxisType+" "+xyp.yAxisType);
            }
            List<XYSegment> convSegList = new ArrayList<>();
            for (XYSegment seg : xyp.segmentList) {
                double[] xVal = new double[seg.x.length];
                double[] yVal = new double[xVal.length];
                for (int i = 0; i < xVal.length; i++) {
                    double radian = 0;
                    if (Objects.equals(xyp.xAxisType, AxisType.radian.name())) {
                        radian = seg.x[i]-Math.PI/2;
                    } else if (Objects.equals(xyp.xAxisType, degree.name())
                            || Objects.equals(xyp.xAxisType, degree180.name())) {
                        radian = (seg.x[i]-90)*Math.PI/180;
                    }
                    double radius = 0;
                    if (Objects.equals(xyp.yAxisType, ModelAxisType.depth.name())) {
                        radius = modelArgs.getTauModel().getRadiusOfEarth()-seg.y[i];
                    } else if (Objects.equals(xyp.yAxisType, ModelAxisType.radius.name())) {
                        radius = seg.y[i];
                    }
                    xVal[i] = radius*Math.cos(radian);
                    yVal[i] = radius*Math.sin(radian);
                }
                XYSegment convSeg = new XYSegment(xVal, yVal);
                convSeg.cssClasses = List.copyOf(seg.cssClasses);
                convSeg.description = seg.description;
                convSegList.add(convSeg);
            }
            convXYPlotList.add(new XYPlottingData(convSegList, kilometer.name(), kilometer.name(),
                    xyp.label, xyp.description, xyp.cssClasses));
        }
        XYPlotOutput out = new XYPlotOutput(convXYPlotList, modelArgs);
        out.setPhaseNames(phaseNames);
        out.title = title;
        out.coloringArgs = coloringArgs;
        return out;
    }

    public boolean isAutoColor() {
        return coloringArgs.getColoring() == ColorType.auto;
    }

    public void setAutoColor(boolean autoColor) {
        coloringArgs.setColoring(ColorType.auto);
    }

    public ColoringArgs getColoringArgs() {
        return coloringArgs;
    }

    public void setColoringArgs(ColoringArgs ca) {
        coloringArgs = ca;
    }

    public int getNumXTicks() {
        return numXTicks;
    }

    public void setNumXTicks(int numXTicks) {
        this.numXTicks = numXTicks;
    }

    public int getNumYTicks() {
        return numYTicks;
    }

    public void setNumYTicks(int numYTicks) {
        this.numYTicks = numYTicks;
    }

    public boolean isxAxisInvert() {
        return xAxisInvert;
    }

    public void setxAxisInvert(boolean xAxisInvert) {
        this.xAxisInvert = xAxisInvert;
    }

    public boolean isyAxisInvert() {
        return yAxisInvert;
    }

    public void setyAxisInvert(boolean yAxisInvert) {
        this.yAxisInvert = yAxisInvert;
    }

    List<XYPlottingData> xyPlots;
    ModelArgs modelArgs;
    List<PhaseName> phaseNames = null;

    String title = null;

    ColoringArgs coloringArgs = new ColoringArgs();

    int numXTicks = 8;
    int numYTicks = 8;

    double[] xAxisMinMax = new double[0];
    double[] yAxisMinMax = new double[0];

    boolean xAxisInvert = false;
    boolean yAxisInvert = false;

}
