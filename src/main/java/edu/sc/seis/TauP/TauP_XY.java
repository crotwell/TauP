package edu.sc.seis.TauP;

import edu.sc.seis.TauP.CLI.OutputTypes;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "xy")
public class TauP_XY extends TauP_AbstractPhaseTool {
    public TauP_XY() {
        setDefaultOutputFormat();
        setOutFileBase("stdout");
    }

    @Override
    public String[] allowedOutputFormats() {
        return new String[]{OutputTypes.TEXT, OutputTypes.JSON, OutputTypes.SVG, OutputTypes.CSV};
    }

    @Override
    public void setDefaultOutputFormat() {
        setOutputFormat(OutputTypes.SVG);
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
                } else if (dashEquals("y", arg)) {
                    noComprendoArgs.remove(arg);
                    yAxisType = args[j+1];
                    j++;
                } else if(dashEquals("reddeg", arg)) {
                    setReduceTime(true);
                    setReduceVelDeg(Double.parseDouble(args[j + 1]));
                    j++;
                } else if(dashEquals("redkm", arg)) {
                    setReduceTime(true);
                    setReduceVelKm(Double.parseDouble(args[j + 1]));
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
        if(modelArgs.getSourceDepth() != -1 * Double.MAX_VALUE) {
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
            if(tempDepth < 0.0 || tempDepth > tMod.getRadiusOfEarth()) {
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

    public Map<SeismicPhase, List<XYPlottingData>> calculate(String xAxisType, String yAxisType) throws TauPException {
        Map<SeismicPhase, List<XYPlottingData>>  xy = calculateLinear(xAxisType, yAxisType);
        if (isReduceTime()) {
            xy = reduce(xy);
        }
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

    public Map<SeismicPhase, List<XYPlottingData>> reduce(Map<SeismicPhase, List<XYPlottingData>> xy) {
        Map<SeismicPhase, List<XYPlottingData>> out = new HashMap<>();
        double velFactor;
        if (axisIsDistanceLike(xAxisType) && axisIsTimeLike(yAxisType)) {
            // x is distance, y is time
            velFactor = reduceVelForAxis(xAxisType);
        } else if (axisIsDistanceLike(yAxisType) && axisIsTimeLike(xAxisType)) {
            velFactor = reduceVelForAxis(yAxisType);
        } else {
            throw new IllegalArgumentException("axis are not time and distance: "+xAxisType+" "+yAxisType);
        }

        for (SeismicPhase phase : xy.keySet()) {
            List<XYPlottingData> plotList = xy.get(phase);
            List<XYPlottingData> redplotList = new ArrayList<>();
            for (XYPlottingData xyp : plotList) {
                List<XYSegment> redSeg = new ArrayList<>();
                for (XYSegment xyseg : xyp.segmentList) {
                    double[] dist;
                    double[] time;
                    if (axisIsDistanceLike(xAxisType)) {
                        dist = xyseg.x;
                        time = xyseg.y;
                    } else {
                        dist = xyseg.y;
                        time = xyseg.x;
                    }
                    for (int i = 0; i < dist.length; i++) {
                        time[i] = time[i] - dist[i] / velFactor;
                    }
                    if (axisIsDistanceLike(xAxisType)) {
                        redSeg.add(new XYSegment(dist, time));
                    } else {
                        redSeg.add(new XYSegment(time, dist));
                    }
                }
                XYPlottingData redxyp = new XYPlottingData(redSeg, xyp.xAxisType, xyp.yAxisType, xyp.label, xyp.phase);
                redplotList.add(redxyp);
            }
            out.put(phase, redplotList);
        }
        return out;
    }

    public static final String[] axisTypes = new String[] {
            "radian",
            "radian_pi",
            "degree",
            "degree_180",
            "kilometer",
            "rayparam",
            "time",
            "tau",
            "turndepth",
            "amp",
            "amppsv",
            "ampsh",
            "geospread",
            "refltran",
            "refltranpsv",
            "refltransh",
            "index"
    };
    public List<double[]> calculatePlotForType(SeismicPhase phase, String axisType, boolean ensure180) throws VelocityModelException, SlownessModelException, TauModelException {
        double[] out = new double[0];
        if (axisType.equalsIgnoreCase("radian") || axisType.equalsIgnoreCase("radian_pi")) {
            out = phase.getDist();
        } else if (axisType.equalsIgnoreCase("degree") || axisType.equalsIgnoreCase("degree_180")) {
            out = phase.getDist();
            for (int i = 0; i < out.length; i++) {
                out[i] *= 180/Math.PI;
            }
        } else if (axisType.equalsIgnoreCase("kilometer") || axisType.equalsIgnoreCase("kilometer_180")) {
            out = phase.getDist();
            double redToKm = tMod.getRadiusOfEarth();
            for (int i = 0; i < out.length; i++) {
                out[i] *= redToKm;
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
        } else if (axisType.equalsIgnoreCase("index")) {
            out = new double[phase.getRayParams().length+1];
            for (int i = phase.getMinRayParamIndex(); i <= phase.getMaxRayParamIndex(); i++) {
                out[i-phase.getMinRayParamIndex()] = i;
            }
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
        if (getOutputFormat().equalsIgnoreCase(OutputTypes.JSON)) {
            JSONObject out = baseResultAsJSONObject( modelArgs.getModelName(), tModDepth.getSourceDepth(),  modelArgs.getReceiveDepth(), getPhaseNames());
            JSONArray phaseCurves = new JSONArray();
            for (SeismicPhase phase: xyPlots.keySet() ) {
                for (XYPlottingData plotItem : xyPlots.get(phase)) {
                    phaseCurves.put(plotItem.asJSON());
                }
            }
            out.put("curves", phaseCurves);
            writer.println(out.toString(2));
        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.SVG)) {
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
                    pixelWidth, margin, getTauModelName()+" (h="+getSourceDepth()+" km)", xAxisType, yAxisType);


            writer.println("<g transform=\"scale(1,-1) translate(0, -"+plotWidth+")\">");

            writer.println("<g transform=\"scale(" + (plotWidth / (minmax[1]-minmax[0])) + "," + ( plotWidth / (minmax[3]-minmax[2])) + ")\" >");
            writer.println("<g transform=\"translate("+(-1*minmax[0])+", "+(-1*minmax[2])+")\">");
            writer.println("    <g class=\"autocolor\">");
            for (SeismicPhase phase : xyPlots.keySet()) {
                // group all
                writer.println("    <g class=\"" + phase.getName() + "\">");
                for (XYPlottingData xyplotItem : xyPlots.get(phase)) {
                    xyplotItem.asSVG(writer);
                }
                writer.println("    </g> <!-- end "+phase.getName()+" -->");
            }
            writer.println("    </g> <!-- end autocolor g -->");

            writer.println("    <g class=\"phasename\">  <!-- begin labels -->");

            writer.println("    </g> <!-- end labels -->");


            writer.println("  </g> <!-- end translate -->");


            writer.println("  </g> <!-- end scale -->");
            writer.println("  </g> <!-- end translate -->");

            List<String> labels = new ArrayList<>();
            List<String> labelClasses = new ArrayList<>();
            for (SeismicPhase phase : xyPlots.keySet()) {
                labels.add(phase.getName());
                labelClasses.add(phase.getName());
            }

            SvgUtil.createLegend(writer, labels, labelClasses, "autocolor", (int)(plotWidth*.1), (int) (plotWidth*.1));
            writer.println("</svg>");

        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.TEXT)) {

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

    public String getStdUsage() {
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1,
                className.length());
        return "Usage: " + className.toLowerCase() + " [arguments]\n"
                +"  or, for purists, java "
                + this.getClass().getName() + " [arguments]\n"
                +"\nArguments are:\n"
                +"-ph phase list     -- comma separated phase list\n"
                + "-pf phasefile      -- file containing phases\n\n"
                + "-mod[el] modelname -- use velocity model \"modelname\" for calculations\n"
                + "                      Default is iasp91.\n\n"
                + "-h depth           -- source depth in km\n\n\n";
    }

    public String getUsage() {
        return getStdUsage()
                +"-x type            -- x axis value, default is degree, one of "+String.join(", ", getAxisTypes())+".\n"
                +"-xlog              -- x axis is log.\n"
                +"-y type            -- y axis value, default is time, same items as -x.\n"
                +"-ylog              -- y axis is log.\n"
                +"--gmt              -- outputs curves as a complete GMT script.\n"
                +"--svg              -- outputs curves as a SVG image.\n"
                +"-reddeg velocity   -- outputs curves with a reducing velocity (deg/sec).\n"
                +"-redkm velocity    -- outputs curves with a reducing velocity (km/sec).\n"
                +"-rel phasename     -- outputs relative travel time\n"
                +"--distancevertical -- distance on vertical axis, time horizontal\n"
                +"--mapwidth width   -- sets map width for GMT script.\n"
                +getStdUsageTail();
    }

    public String[] getAxisTypes() {
        return axisTypes;
    }

    /**
     * True if the axis type is distance-like.
     *
     * @param axisType
     * @return
     */
    public boolean axisIsDistanceLike(String axisType) {
        return axisType.equalsIgnoreCase("degree")
                || axisType.equalsIgnoreCase("degree_180")
                || axisType.equalsIgnoreCase("radian")
                || axisType.equalsIgnoreCase("radian_pi");
    }

    /**
     * True if the axis type is time.
     * @param axisType
     * @return
     */
    public boolean axisIsTimeLike(String axisType) {
        return axisType.equalsIgnoreCase("time");
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

    public boolean isReduceTime() {
        return reduceTime;
    }

    public void setReduceTime(boolean reduceTime) {
        this.reduceTime = reduceTime;
    }

    /**
     * @return reducing velocity in degrees/second. The internal usage is
     *          radians/second.
     */
    public double getReduceVelDeg() {
        return 180.0 / Math.PI * reduceVel;
    }

    /**
     * set the reducing velocity, in degrees/second. The internal representation
     * is radians/second.
     */
    public void setReduceVelDeg(double reduceVel) {
        if(reduceVel > 0.0) {
            redVelString = reduceVel+" deg/s";
            this.reduceVel = Math.PI / 180.0 * reduceVel;
        }
    }

    /**
     * @return reducing velocity in kilometers/second. The internal usage is
     *          radians/second.
     */
    public double getReduceVelKm() {
        return reduceVel * tMod.getRadiusOfEarth();
    }

    public double reduceVelForAxis(String axisType) {
        if (axisType.equalsIgnoreCase("degree")) {
            return getReduceVelDeg();
        } else if (axisType.equalsIgnoreCase("kilometer")) {
            return getReduceVelKm();
        } else if (axisType.equalsIgnoreCase("radian")) {
            return getReduceVelDeg()*Math.PI/180;
        } else {
            throw new IllegalArgumentException("axis type not distance-like: "+axisType);
        }
    }

    /**
     * set the reducing velocity, in kilometers/second. The internal
     * representation is radians/second.
     */
    public void setReduceVelKm(double reduceVel) {
        redVelString = reduceVel+" km/s";
        if(reduceVel > 0.0) {
            if(tMod != null) {
                this.reduceVel = reduceVel / tMod.getRadiusOfEarth();
            } else {
                this.reduceVel = reduceVel / 6371.0;
            }
        } else {
            throw new IllegalArgumentException("Reducing velocity must be positive: "+reduceVel);
        }
    }

    protected String xAxisType = "degree";
    protected String yAxisType = "time";

    protected boolean xAxisLog = false;
    protected boolean yAxisLog = false;

    /** should the output times use a reducing velocity? */
    protected boolean reduceTime = false;

    /**
     * the reducing velocity to use if reduceTime == true, in units of
     * radians/second .
     */
    protected double reduceVel = .125 * Math.PI / 180;

    protected String redVelString = ".125 deg/s";

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
