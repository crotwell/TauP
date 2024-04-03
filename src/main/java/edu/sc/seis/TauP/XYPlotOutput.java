package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.ModelArgs;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.TauP_AbstractPhaseTool.baseResultAsJSONObject;
import static edu.sc.seis.TauP.TauP_Tool.toolNameFromClass;

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
                double shift = Math.abs(minmax[0]) * ypercent;
                minmax[2] = minmax[2] - shift;
                minmax[3] = minmax[3] + shift;
            }
        }
    }

    protected static List<XYPlottingData> recalcForLog(List<XYPlottingData> xy, boolean xAxisLog, boolean yAxisLog) {
        List<XYPlottingData> out = new ArrayList<>();
        for(XYPlottingData xyp : xy) {
            out.add( xyp.recalcForLog(xAxisLog, yAxisLog));
        }
        return out;
    }

    public void setPhaseNames(String[] phaseNames) {
        this.phaseNames = phaseNames;
    }

    public void setxAxisMinMax(double[] minMax) {
        this.xAxisMinMax = minMax;
    }
    public void setyAxisMinMax(double[] minMax) {
        this.yAxisMinMax = minMax;
    }

    public JSONObject asJSON() {
        JSONObject out = baseResultAsJSONObject( modelArgs.getModelName(), modelArgs.getSourceDepth(),  modelArgs.getReceiverDepth(), phaseNames);
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

    public void printAsGmtText(PrintWriter writer) {
        for (XYPlottingData xyplotItem : xyPlots) {
            xyplotItem.asGMT(writer);
        }
    }

    public void printAsSvg(PrintWriter writer, String[] cmdLineArgs, String xAxisType, String yAxisType) {
        StringBuffer extrtaCSS = new StringBuffer();
        extrtaCSS.append("        text.label {\n");
        extrtaCSS.append("            font: bold ;\n");
        extrtaCSS.append("            fill: black;\n");
        extrtaCSS.append("        }\n");
        extrtaCSS.append("        g.phasename text {\n");
        extrtaCSS.append("            font: bold ;\n");
        extrtaCSS.append("            fill: black;\n");
        extrtaCSS.append("        }\n");

        int margin = 80;
        int pixelWidth = 600+margin;//Math.round(72*mapWidth);
        int plotOffset = 60;

        double[] minmax = XYPlottingData.initMinMax();
        for (XYPlottingData xyplot : xyPlots) {
            if (xAxisMinMax.length == 2 && yAxisMinMax.length == 0) {
                // given x range, find y range
                minmax = xyplot.minMaxInXRange(minmax, xAxisMinMax);
            } else {
                minmax = xyplot.minMax(minmax);
            }
        }
        checkEqualMinMax(minmax, 0.1, 0.1);
        // override minmax with user supplied if
        if (xAxisMinMax.length == 2) {
            minmax[0] = xAxisMinMax[0];
            minmax[1] = xAxisMinMax[1];
        }
        if (yAxisMinMax.length == 2) {
            minmax[2] = yAxisMinMax[0];
            minmax[3] = yAxisMinMax[1];
        }
        SvgUtil.xyplotScriptBeginning(writer, toolNameFromClass(this.getClass()),
                cmdLineArgs,  pixelWidth, margin, extrtaCSS.toString(), minmax);

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
        SvgUtil.createXYAxes(writer, axisMinMax[0], axisMinMax[1], 8, false,
                axisMinMax[2], axisMinMax[3], 8, false,
                pixelWidth, margin,
                modelArgs.getModelName()+" (h="+modelArgs.getSourceDepth()+" km)",
                xAxisType, yAxisType);

        writer.println("<g clip-path=\"url(#curve_clip)\">");

        writer.println("<g transform=\"scale(1,-1) translate(0, -"+plotWidth+")\">");

        writer.println("<g transform=\"scale(" + (plotWidth / (minmax[1]-minmax[0])) + "," + ( plotWidth / (minmax[3]-minmax[2])) + ")\" >");
        writer.println("<g transform=\"translate("+(-1*minmax[0])+", "+(-1*minmax[2])+")\">");
        writer.println("    <g class=\"autocolor\">");
        for (XYPlottingData xyplotItem : xyPlots) {
            if (xAxisInvert || yAxisInvert) {
                int xflip = xAxisInvert ? -1 : 1;
                int yflip = yAxisInvert ? -1 : 1;
                float xfliptrans = xAxisInvert ? -1*(float)minmax[1] : 0;
                float yfliptrans = yAxisInvert ? -1*(float)minmax[3] : 0;
                writer.println("<g transform=\"scale(" + xflip + "," + yflip + ") translate("+xfliptrans+", "+yfliptrans+")\" > <!-- invert axis -->");
            }
            xyplotItem.asSVG(writer);
            if (xAxisInvert || yAxisInvert) {
                writer.println("    </g> <!-- end invert axis -->");
            }
        }

        writer.println("    </g> <!-- end autocolor g -->");

        writer.println("    <g class=\"phasename\">  <!-- begin labels -->");

        writer.println("    </g> <!-- end labels -->");


        writer.println("  </g> <!-- end translate -->");


        writer.println("  </g> <!-- end scale -->");
        writer.println("  </g> <!-- end scaletranslate -->");
        writer.println("  </g> <!-- end clip-path -->");

        List<String> labels = new ArrayList<>();
        List<String> labelClasses = new ArrayList<>();
        for (XYPlottingData xyp : xyPlots) {
            labels.add(xyp.label);
            labelClasses.add(xyp.label);
        }

        SvgUtil.createLegend(writer, labels, labelClasses, "autocolor", (int)(plotWidth*.1), (int) (plotWidth*.1));
        writer.println("</svg>");
    }

    List<XYPlottingData> xyPlots;
    ModelArgs modelArgs;
    String[] phaseNames = null;


    double[] xAxisMinMax = new double[0];
    double[] yAxisMinMax = new double[0];

    boolean xAxisInvert = false;
    boolean yAxisInvert = false;
}
