package edu.sc.seis.TauP;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<String> noComprendoArgs = new ArrayList<>();
        int j = 0;
        while (j < args.length) {
            String arg = args[j];

            if (dashEquals("xlog", arg)) {
                noComprendoArgs.remove(arg);
                xAxisLog = true;
            } else if (dashEquals("ylog", arg)) {
                noComprendoArgs.remove(arg);
                yAxisLog = true;
            } else if (j < args.length-1) {
                if (dashEquals("x", arg)) {
                    xAxisType = args[j+1];
                    j++;
                } else if (dashEquals("xlog", arg)) {
                    noComprendoArgs.remove(arg);
                    yAxisType = args[j+1];
                    j++;
                } else if (dashEquals("y", arg)) {
                    noComprendoArgs.remove(arg);
                    yAxisType = args[j+1];
                    j++;
                } else if (dashEquals("ylog", arg)) {
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
        double tempDepth;
        if(depth != -1 * Double.MAX_VALUE) {
            /* enough info given on cmd line, so just do one calc. */
            setSourceDepth(Double.valueOf(toolProps.getProperty("taup.source.depth",
                    "0.0")));

            Map<SeismicPhase, List<XYPlottingData>> xy = calculate(xAxisType, yAxisType);
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

            Map<SeismicPhase, List<XYPlottingData>> xy = calculate(xAxisType, yAxisType);
            printResult(getWriter(), xy);
            getWriter().flush();
        }
    }

    public Map<SeismicPhase, List<XYPlottingData>> calculate(String xAxisType, String yAxisType) throws TauModelException, VelocityModelException, SlownessModelException {
        Map<SeismicPhase, List<XYPlottingData>>  xy = calculateLinear(xAxisType, yAxisType);
        if (xAxisLog || yAxisLog) {
            for (SeismicPhase phase : xy.keySet()) {
                xy.put(phase, recalcForLog(xy.get(phase), xAxisLog, yAxisLog));
            }
        }
        return xy;
    }

    public Map<SeismicPhase, List<XYPlottingData>> calculateLinear(String xAxisType, String yAxisType) throws TauModelException, VelocityModelException, SlownessModelException {
        depthCorrect();
        List<SeismicPhase> phaseList = getSeismicPhases();
        Map<SeismicPhase, List<XYPlottingData>> outMap = new HashMap<>();
        for (SeismicPhase phase: phaseList) {
            List<XYPlottingData> out = new ArrayList<>();
            outMap.put(phase, out);
            boolean ensure180 = (xAxisType.equalsIgnoreCase("degree_180") || yAxisType.equalsIgnoreCase("degree_180")
                    || xAxisType.equalsIgnoreCase("radian_pi") || yAxisType.equalsIgnoreCase("radian_pi"));
            if(phase.hasArrivals()) {
                if (yAxisType.equalsIgnoreCase("theta")) {
                    // temp for testing...
                    double dist = 15;
                    List<Arrival> arrivals = phase.calcTime(dist);
                    for (Arrival arrival : arrivals) {
                        Theta theta = new Theta(arrival);
                        List<double[]> xData = SeismicPhase.splitForRepeatRayParam(phase.getRayParams(), phase.getRayParams());
                        List<double[]> yData = SeismicPhase.splitForRepeatRayParam(theta.rayParams, theta.thetaAtX);
                        List<XYSegment> segmentList = new ArrayList<>();
                        for (int i = 0; i < xData.size(); i++) {
                            segmentList.add(new XYSegment(xData.get(i), yData.get(i)));

                        }
                        out.add(new XYPlottingData(
                                segmentList, xAxisType, "Ray Param",
                                phase.getName(), phase
                        ));

                    }
                } else {
                    List<double[]> xData = calculatePlotForType(phase, xAxisType, ensure180);
                    List<double[]> yData = calculatePlotForType(phase, yAxisType, ensure180);
                    out.add(new XYPlottingData(xData, xAxisType, yData, yAxisType, phase.getName(), phase));

                    if (phase.isAllSWave()) {
                        // second calc needed for sh, as psv done in main calc
                        if (xAxisType.equalsIgnoreCase("amp")
                                    || yAxisType.equalsIgnoreCase("amp")) {
                            String xOther = xAxisType.equalsIgnoreCase("amp") ? "ampsh" : xAxisType;
                            String yOther = yAxisType.equalsIgnoreCase("amp") ? "ampsh" : yAxisType;

                            xData = calculatePlotForType(phase, xOther, ensure180);
                            yData = calculatePlotForType(phase, yOther, ensure180);
                            out.add(new XYPlottingData(xData, xAxisType, yData, yAxisType, phase.getName(), phase));
                        }
                        // what about case of amp vs refltran, need 4 outputs?
                        if (xAxisType.equalsIgnoreCase("refltran")
                                || yAxisType.equalsIgnoreCase("refltran")) {
                            String xOther = xAxisType.equalsIgnoreCase("refltran") ? "refltransh" : xAxisType;
                            String yOther = yAxisType.equalsIgnoreCase("refltran") ? "refltransh" : yAxisType;

                            xData = calculatePlotForType(phase, xOther, ensure180);
                            yData = calculatePlotForType(phase, yOther, ensure180);
                            out.add(new XYPlottingData(xData, xAxisType, yData, yAxisType, phase.getName(), phase));
                        }
                    }
                }
            }
        }
        return outMap;
    }

    public List<double[]> calculatePlotForType(SeismicPhase phase, String axisType, boolean ensure180) throws VelocityModelException, SlownessModelException, TauModelException {
        double[] out = new double[0];
        if (axisType.equalsIgnoreCase("radian") || axisType.equalsIgnoreCase("radian_pi")) {
            out = phase.getDist();
        } else if (axisType.equalsIgnoreCase("degree") || axisType.equalsIgnoreCase("degree_180")) {
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
        } else if (axisType.equalsIgnoreCase("turndepth")) {
            double[] dist = phase.getDist();
            out = new double[dist.length];
            for (int i = 0; i < dist.length; i++) {
                Arrival arrival = phase.createArrivalAtIndex(i);
                out[i] = arrival.getDeepestPierce().getDepth();
            }
        } else if (axisType.equalsIgnoreCase("amp") ||
                axisType.equalsIgnoreCase("amppsv") ||
                axisType.equalsIgnoreCase("ampsh")) {
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
        } else if (axisType.equalsIgnoreCase("refltran") ||
                axisType.equalsIgnoreCase("refltranpsv") ||
                axisType.equalsIgnoreCase("refltransh")) {
            boolean isSH = axisType.equalsIgnoreCase("refltransh");
            double[] dist = phase.getDist();
            double[] amp = new double[dist.length];
            for (int i = 0; i < dist.length; i++) {
                Arrival arrival = phase.createArrivalAtIndex(i);
                if (isSH) {
                    amp[i] = arrival.getReflTransSH();
                } else {
                    amp[i] = arrival.getReflTransPSV();
                    if (amp[i] == 0.0) {
                        System.out.println(arrival);
                        System.out.println("index: "+arrival.getRayParamIndex());
                    }
                }
            }
            out = amp;

        } else {
            throw new IllegalArgumentException("Unknown axisType: "+axisType);
        }
        double[] rayParams = phase.getRayParams();
        if (ensure180) {
            double[] dist = phase.getDist();
            // insert extra values, linearly interpolated, if spans 180 deg
            int wrapMinIndex = (int)Math.round(Math.floor(phase.getMinDistance()/Math.PI));
            int wrapMaxIndex = (int)Math.round(Math.ceil(phase.getMaxDistance()/Math.PI));
            ArrayList<Integer> crossIdx = new ArrayList<>();
            ArrayList<Double> crossValue = new ArrayList<>();
            for (int j = wrapMinIndex; j <= wrapMaxIndex; j++) {
                double wrapRadian = j*Math.PI;
                for (int i=1; i < out.length; i++) {
                    if ((dist[i-1] - wrapRadian )*(dist[i] - wrapRadian) < 0 && rayParams[i-1] != rayParams[i]) {
                        // dist spans a multiple of PI, repeated ray params are already a break so don't interpolate
                        crossIdx.add(i);
                        crossValue.add(wrapRadian);
                    }
                }
            }

            double[] unwrappedOut = new double[out.length+crossIdx.size()];
            // also unwrap ray param as need to check for doubled ray params to separate discon
            double[] unwrappedRP = new double[out.length+crossIdx.size()];
            int prevIdx = 0;
            for (int i = 0; i < crossIdx.size(); i++) {
                int idx = crossIdx.get(i);
                double wrap = crossValue.get(i);
                System.arraycopy(out, prevIdx, unwrappedOut, prevIdx+i, idx-prevIdx);
                System.arraycopy(rayParams, prevIdx, unwrappedRP, prevIdx+i, idx-prevIdx);
                unwrappedOut[idx+i] = linearInterp(dist[idx-1], out[idx-1], dist[idx], out[idx], wrap);
                unwrappedRP[idx+i] = linearInterp(dist[idx-1], rayParams[idx-1], dist[idx], rayParams[idx], wrap);
                prevIdx = idx;
            }
            System.arraycopy(out, prevIdx, unwrappedOut, prevIdx+crossIdx.size(), out.length-prevIdx);
            System.arraycopy(rayParams, prevIdx, unwrappedRP, prevIdx+crossIdx.size(), rayParams.length-prevIdx);
            out = unwrappedOut;
            rayParams = unwrappedRP;
            if (axisType.equalsIgnoreCase("degree_180")) {
                System.err.println("recalc modulo");
                for (int j = 0; j < out.length; j++) {
                    out[j] = Math.abs(out[j] % 360.0);
                    if (out[j] > 180.0) {
                        out[j] = 360.0 - out[j];
                    }
                }
            }
        }

        // repeated ray parameters indicate break in curve, split into segments
        return SeismicPhase.splitForRepeatRayParam(rayParams, out);
    }
    protected List<XYPlottingData> recalcForLog(List<XYPlottingData> xy, boolean xAxisLog,  boolean yAxisLog) {
        List<XYPlottingData> out = new ArrayList<>();
        for(XYPlottingData xyp : xy) {
            out.add( xyp.recalcForLog(xAxisLog, yAxisLog));
        }
        return out;
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

    public void printResult(PrintWriter writer, Map<SeismicPhase, List<XYPlottingData>> xyPlots) {
        if (getOutputFormat().equalsIgnoreCase(JSON)) {
            JSONObject out = baseResultAsJSONObject( modelName, depth,  receiverDepth, getPhaseNames());
            JSONArray phaseCurves = new JSONArray();
            for (SeismicPhase phase: xyPlots.keySet() ) {
                for (XYPlottingData plotItem : xyPlots.get(phase)) {
                    phaseCurves.put(plotItem.asJSON());
                }
            }
            out.put("curves", phaseCurves);
            writer.println(out.toString(2));
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
            int margin = 80;
            int pixelWidth = 600+margin;//Math.round(72*mapWidth);
            int plotOffset = 60;

            double[] minmax = XYPlottingData.initMinMax();
            for (List<XYPlottingData> phaseXY : xyPlots.values()) {
                for (XYPlottingData xyplot : phaseXY) {
                    minmax = xyplot.minMax(minmax);
                }
            }
            checkEqualMinMax(minmax, 0.1, 0.1);
            SvgUtil.xyplotScriptBeginning(writer, toolNameFromClass(this.getClass()),
                    cmdLineArgs,  pixelWidth, plotOffset, extrtaCSS.toString(), minmax);
            // override minmax with user supplied if
            if (xAxisMinMax.length == 2) {
                minmax[0] = xAxisMinMax[0];
                minmax[1] = xAxisMinMax[1];
            }
            if (yAxisMinMax.length == 2) {
                minmax[2] = yAxisMinMax[0];
                minmax[3] = yAxisMinMax[1];
            }

            float plotWidth = pixelWidth - 2*margin;
            SvgUtil.createXYAxes(writer, minmax[0], minmax[1], 8, false,
                    minmax[2], minmax[3], 8, false,
                    pixelWidth, margin, "Titiel here", xAxisType, yAxisType);


            writer.println("<g transform=\"scale(1,-1) translate(0, -"+plotWidth+")\">");

            writer.println("<g transform=\"scale(" + (plotWidth / (minmax[1]-minmax[0])) + "," + ( plotWidth / (minmax[3]-minmax[2])) + ")\" >");
            writer.println("<g transform=\"translate("+(-1*minmax[0])+", "+(-1*minmax[2])+")\">");
            for (SeismicPhase phase : xyPlots.keySet()) {
                writer.println("    <g class=\"" + phase.getName() + "\">");
                for (XYPlottingData xyplotItem : xyPlots.get(phase)) {
                    xyplotItem.asSVG(writer);
                }
                writer.println("    </g> <!-- end "+phase.getName()+" -->");
            }

            writer.println("    <g class=\"phasename\">  <!-- begin labels -->");

            writer.println("    </g> <!-- end labels -->");

            writer.println("  </g> <!-- end translate -->");
            writer.println("  </g> <!-- end scale -->");
            writer.println("  </g> <!-- end translate -->");
            writer.println("</svg>");

        } else if (getOutputFormat().equalsIgnoreCase(TEXT)) {

            for (SeismicPhase phase : xyPlots.keySet()) {
                for (XYPlottingData xyplotItem : xyPlots.get(phase)) {
                    for (XYSegment segment : xyplotItem.segmentList) {
                        writer.println("> " + xyplotItem.label + " " + segment.x.length + " " + xyplotItem.phase);
                        for (int i = 0; i < segment.x.length; i++) {
                            writer.println(segment.x[i] + " " + segment.y[i]);
                        }
                    }
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

    public boolean isxAxisLog() {
        return xAxisLog;
    }

    public void setxAxisLog(boolean xAxisLog) {
        this.xAxisLog = xAxisLog;
    }

    public boolean isyAxisLog() {
        return yAxisLog;
    }

    public void setyAxisLog(boolean yAxisLog) {
        this.yAxisLog = yAxisLog;
    }

    protected String xAxisType = "degree";
    protected String yAxisType = "time";

    protected boolean xAxisLog = false;
    protected boolean yAxisLog = false;

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
