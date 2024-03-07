package edu.sc.seis.TauP;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;

public class TauP_XY extends TauP_AbstractTimeTool {
    public TauP_XY() {
        setDefaultOutputFormat();
        setOutFileBase("stdout");
    }

    @Override
    public String[] allowedOutputFormats() {
        return new String[]{TEXT, JSON, SVG, CSV};
    }

    @Override
    public void setDefaultOutputFormat() {
        setOutputFormat(SVG);
    }

    @Override
    protected String[] parseCmdLineArgs(String[] origArgs) throws IOException {
        String[] args = parseSourceModelCmdLineArgs(origArgs);
        args = parseOutputFormatCmdLineArgs(args);
        List<String> noComprendoArgs = new ArrayList();
        int j = 0;
        while (j < args.length) {
            String arg = args[j];
            if (j < args.length-1) {
                if (dashEquals("x", arg)) {
                    xAxisType = args[j+1];
                    j++;
                } else if (dashEquals("y", arg)) {
                    noComprendoArgs.remove(arg);
                    yAxisType = args[j+1];
                    j++;
                } else if (j < args.length-2) {
                    if (dashEquals("xminmax", arg)) {
                        noComprendoArgs.remove(arg);
                        xAxisMinMax = new double[] { Double.parseDouble(args[j+1]), Double.parseDouble(args[j+2])};
                        j+=2;
                    } else if (dashEquals("yminmax", arg)) {
                        yAxisMinMax = new double[] { Double.parseDouble(args[j+1]), Double.parseDouble(args[j+2])};
                        j+=2;
                    } else {
                        noComprendoArgs.add(arg);
                    }
                } else {
                    noComprendoArgs.add(arg);
                }
            } else {
                noComprendoArgs.add(arg);
            }
            j++;
        }
        return noComprendoArgs.toArray(new String[0]);
    }

    /**
     * preforms intialization of the tool. Properties are queried for the
     * default model to load, source depth to use, phases to use, etc.
     */
    public void init() throws TauPException {
        super.init();
    }

    @Override
    public void start() throws IOException, TauModelException, TauPException {
        System.out.println("start!");
        double tempDepth;
        if(depth != -1 * Double.MAX_VALUE) {
            /* enough info given on cmd line, so just do one calc. */
            setSourceDepth(Double.valueOf(toolProps.getProperty("taup.source.depth",
                            "0.0"))
                    .doubleValue());

            List<XYPlottingData>  xy = calculate(xAxisType, yAxisType);
            printResult(getWriter(), xy);
        } else {
            StreamTokenizer tokenIn = new StreamTokenizer(new InputStreamReader(System.in));
            tokenIn.parseNumbers();
            tokenIn.wordChars(',', ',');
            tokenIn.wordChars('_', '_');
            System.out.print("Enter Depth: ");
            tokenIn.nextToken();
            tempDepth = tokenIn.nval;
            if(tempDepth < 0.0 || depth > tMod.getRadiusOfEarth()) {
                System.out.println("Depth must be >= 0.0 and "
                        + "<= tMod.getRadiusOfEarth().\ndepth = " + tempDepth);
                return;
            }
            setSourceDepth(tempDepth);
            List<XYPlottingData>  xy = calculate(xAxisType, yAxisType);
            printResult(getWriter(), xy);
            getWriter().flush();
        }
    }

    public List<XYPlottingData> calculate(String xAxisType, String yAxisType) throws TauModelException, VelocityModelException, SlownessModelException {
        depthCorrect();
        List<SeismicPhase> phaseList = getSeismicPhases();
        List<XYPlottingData> out = new ArrayList<>();
        for (SeismicPhase phase: phaseList) {
            if(phase.hasArrivals()) {

                List<double[]> xData = calculatePlotForType(phase, xAxisType);
                List<double[]> yData = calculatePlotForType(phase, yAxisType);
                for (int i = 0; i < xData.size(); i++) {
                    out.add(new XYPlottingData(
                            xData.get(i), xAxisType,
                            yData.get(i), yAxisType,
                            phase.getName(), phase
                    ));
                }
            }
        }
        return out;
    }

    public List<double[]> calculatePlotForType(SeismicPhase phase, String axisType) throws VelocityModelException, SlownessModelException, TauModelException {
        double[] out = new double[0];
        List<double[]> outList = new ArrayList<>();
        if (axisType.equalsIgnoreCase("radian")) {
            out = phase.getDist();
        } else if (axisType.equalsIgnoreCase("degree")) {
            out = phase.getDist();
            for (int i = 0; i < out.length; i++) {
                out[i] *= 180/Math.PI;
            }
        } else if (axisType.equalsIgnoreCase("rayparam")) {
            out = phase.getRayParams();
        } else if (axisType.equalsIgnoreCase("time")) {
            out = phase.getTime();
        } else if (axisType.equalsIgnoreCase("tau")) {
            out = phase.getTau();
        } else if (axisType.equalsIgnoreCase("amppsv") || axisType.equalsIgnoreCase("ampsh")) {
            boolean isAmpSH = axisType.equalsIgnoreCase("ampsh");
            double[] dist = phase.getDist();
            double[] amp = new double[dist.length];
            for (int i = 0; i < dist.length; i++) {
                Arrival arrival = phase.createArrivalAtIndex(i);
                if (isAmpSH) {
                    amp[i] = arrival.getAmplitudeFactorSH();
                } else {
                    amp[i] = arrival.getAmplitudeFactorPSV();
                }
            }
            out = amp;
        } else if (axisType.equalsIgnoreCase("geospread")) {
            double[] dist = phase.getDist();
            double[] amp = new double[dist.length];
            for (int i = 0; i < dist.length; i++) {
                Arrival arrival = phase.createArrivalAtIndex(i);
                amp[i] = arrival.getGeometricSpreadingFactor();
            }
            out = amp;

        } else {
            throw new IllegalArgumentException("Unknown axisType: "+axisType);
        }
        // repeated ray parameters indicate break in curve, split into segments
        return SeismicPhase.splitForRepeatRayParam(phase.getRayParams(), out);
    }

    public void printResult(PrintWriter writer, List<XYPlottingData> xyPlots) {
        if (getOutputFormat().equalsIgnoreCase(JSON)) {
            JSONObject out = baseResultAsJSONObject( modelName, depth,  receiverDepth, getPhaseNames());
            JSONObject curves = new JSONObject();
            out.put("curve", curves);

        } else if (getOutputFormat().equalsIgnoreCase(SVG)) {
            StringBuffer extrtaCSS = new StringBuffer();
            extrtaCSS.append("        text.label {\n");
            extrtaCSS.append("            font: bold ;\n");
            extrtaCSS.append("            fill: black;\n");
            extrtaCSS.append("        }\n");
            extrtaCSS.append("        g.phasename text {\n");
            extrtaCSS.append("            font: bold ;\n");
            extrtaCSS.append("            fill: black;\n");
            extrtaCSS.append("        }\n");
            float mapWidth = 6;
            int plotOffset = 30;
            SvgUtil.xyplotScriptBeginning(writer, toolNameFromClass(this.getClass()),
                    cmdLineArgs,  mapWidth, plotOffset, extrtaCSS.toString());

            double[] minmax = XYPlottingData.initMinMax();
            for (XYPlottingData xyplot: xyPlots) {
                minmax = xyplot.minMax(minmax);
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

            int margin = 30;
            int pixelWidth = 300+margin;//Math.round(72*mapWidth);
            float plotWidth = pixelWidth - margin;
            SvgUtil.createXYAxes(writer, minmax[0], minmax[1], 8, false,
                    minmax[2], minmax[3], 8, false,
                    pixelWidth, margin, "Titiel here", xAxisType, yAxisType);


            writer.println("<g transform=\"scale(1,-1) translate(0, -"+plotWidth+")\">");

            writer.println("<g transform=\"scale(" + (plotWidth / (minmax[1]-minmax[0])) + "," + ( plotWidth / (minmax[3]-minmax[2])) + ")\" >");
            writer.println("<g transform=\"translate("+(-1*minmax[0])+", "+(-1*minmax[2])+")\">");
            for (XYPlottingData xyplotItem: xyPlots) {
                String p_or_s = "both_p_swave";
                if (xyplotItem.phase.isAllSWave()) {
                    p_or_s = "swave";
                } else if (xyplotItem.phase.isAllPWave()) {
                    p_or_s = "pwave";
                }
                writer.println("    <g class=\""+xyplotItem.label+"\">");
                writer.println("    <polyline class=\""+p_or_s+"\" points=\"");
                for (int i = 0; i < xyplotItem.xValues.length; i++) {
                    if (Double.isFinite(xyplotItem.xValues[i]) && Double.isFinite(xyplotItem.yValues[i])) {
                        writer.println(xyplotItem.xValues[i] + " " + xyplotItem.yValues[i]);
                    } else if (i != 0 && i != xyplotItem.xValues.length) {
                        writer.println("  \"  /> <!-- "+xyplotItem.label+"-->");
                        writer.println("    <polyline class=\""+p_or_s+"\" points=\"");
                    }
                }
                writer.println("  \"  /> <!-- "+xyplotItem.label+"-->");
                writer.println("    </g> <!-- end \"+xyplotItem.label+\" -->");
            }

            writer.println("    <g class=\"phasename\">  <!-- begin labels -->");

            writer.println("    </g> <!-- end labels -->");

            writer.println("  </g> <!-- end translate -->");
            writer.println("  </g> <!-- end scale -->");
            writer.println("  </g> <!-- end translate -->");
            writer.println("  </g> <!-- end translate -->");
            writer.println("</svg>");

        } else if (getOutputFormat().equalsIgnoreCase(TEXT)) {

            for (XYPlottingData xyplotItem: xyPlots) {
                writer.println("> "+xyplotItem.label+" "+xyplotItem.xValues.length+" "+xyplotItem.phase);
                for (int i = 0; i < xyplotItem.xValues.length; i++) {
                    writer.println(xyplotItem.xValues[i]+" "+xyplotItem.yValues[i]);
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown output format: "+getOutputFormat());
        }
        writer.flush();

    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public String getUsage() {
        return null;
    }

    @Override
    public void validateArguments() throws TauModelException {

    }

    public String getxAxisType() {
        return xAxisType;
    }

    public void setxAxisType(String xAxisType) {
        this.xAxisType = xAxisType;
    }

    public String getyAxisType() {
        return yAxisType;
    }

    public void setyAxisType(String yAxisType) {
        this.yAxisType = yAxisType;
    }

    public double[] getxAxisMinMax() {
        return xAxisMinMax;
    }

    public void setxAxisMinMax(double[] xAxisMinMax) {
        this.xAxisMinMax = xAxisMinMax;
    }

    public double[] getyAxisMinMax() {
        return yAxisMinMax;
    }

    public void setyAxisMinMax(double[] yAxisMinMax) {
        this.yAxisMinMax = yAxisMinMax;
    }

    protected String xAxisType = "degree";
    protected String yAxisType = "time";

    protected double[] xAxisMinMax = new double[0];
    protected double[] yAxisMinMax = new double[0];

    public void setxMinMax(double min, double max) {
        if (min < max) {
            xAxisMinMax = new double[]{min, max};
        } else {
            throw new IllegalArgumentException("min must be < max: "+min+" < "+max);
        }
    }
    public void setyMinMax(double min, double max) {
        if (min < max) {
            yAxisMinMax = new double[]{min, max};
        } else {
            throw new IllegalArgumentException("min must be < max: "+min+" < "+max);
        }
    }
}
