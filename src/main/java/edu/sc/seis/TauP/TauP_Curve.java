package edu.sc.seis.TauP;

import edu.sc.seis.TauP.CLI.GraphicOutputTypeArgs;
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

@CommandLine.Command(name = "curve")
public class TauP_Curve extends TauP_AbstractPhaseTool {
    public TauP_Curve() {
        setDefaultOutputFormat();
        setOutFileBase("stdout");
    }

    @Override
    public String[] allowedOutputFormats() {
        return new String[]{OutputTypes.TEXT, OutputTypes.JSON, OutputTypes.SVG, OutputTypes.CSV};
    }

    @Override
    public void setDefaultOutputFormat() {
        outputTypeArgs.setOutputType(OutputTypes.TEXT);
        setOutputFormat(OutputTypes.TEXT);
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
            if(tempDepth < 0.0 || tempDepth > getRadiusOfEarth()) {
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

    public Map<SeismicPhase, List<XYPlottingData>> calculate(AxisType xAxisType, AxisType yAxisType) throws TauPException {
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

    public Map<SeismicPhase, List<XYPlottingData>> calculateLinear(AxisType xAxisType, AxisType yAxisType) throws TauModelException, VelocityModelException, SlownessModelException {
        modelArgs.depthCorrected();
        List<SeismicPhase> phaseList = getSeismicPhases();
        Map<SeismicPhase, List<XYPlottingData>> outMap = new HashMap<>();
        for (SeismicPhase phase: phaseList) {
            List<XYPlottingData> out = new ArrayList<>();
            outMap.put(phase, out);
            boolean ensure180 = (xAxisType==AxisType.degree_180 || yAxisType==AxisType.degree_180
                    || xAxisType==AxisType.radian_pi || yAxisType==AxisType.radian_pi);
            if(phase.hasArrivals()) {
                if (yAxisType==AxisType.theta) {
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
                                segmentList, xAxisType.toString(), "Ray Param",
                                phase.getName(), phase
                        ));

                    }
                } else {
                    List<double[]> xData = calculatePlotForType(phase, xAxisType, ensure180);
                    List<double[]> yData = calculatePlotForType(phase, yAxisType, ensure180);
                    out.add(new XYPlottingData(xData, xAxisType.toString(), yData, yAxisType.toString(), phase.getName(), phase));

                    if (phase.isAllSWave()) {
                        // second calc needed for sh, as psv done in main calc
                        if (xAxisType==AxisType.amp
                                    || yAxisType==AxisType.amp) {
                            AxisType xOther = xAxisType==AxisType.amp ? AxisType.ampsh : xAxisType;
                            AxisType yOther = yAxisType==AxisType.amp ? AxisType.ampsh : yAxisType;

                            xData = calculatePlotForType(phase, xOther, ensure180);
                            yData = calculatePlotForType(phase, yOther, ensure180);
                            out.add(new XYPlottingData(xData, xAxisType.toString(), yData, yAxisType.toString(), phase.getName(), phase));
                        }
                        // what about case of amp vs refltran, need 4 outputs?
                        if (xAxisType==AxisType.refltran
                                || yAxisType==AxisType.refltran) {
                            AxisType xOther = xAxisType==AxisType.refltran ? AxisType.refltransh : xAxisType;
                            AxisType yOther = yAxisType==AxisType.refltran ? AxisType.refltransh : yAxisType;

                            xData = calculatePlotForType(phase, xOther, ensure180);
                            yData = calculatePlotForType(phase, yOther, ensure180);
                            out.add(new XYPlottingData(xData, xAxisType.toString(), yData, yAxisType.toString(), phase.getName(), phase));
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
        if (velFactor == 1.0 || velFactor == 0.0) {
            // either not set or nothing to do
            return xy;
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


    public List<double[]> calculatePlotForType(SeismicPhase phase, AxisType axisType, boolean ensure180) throws VelocityModelException, SlownessModelException, TauModelException {
        double[] out = new double[0];
        if (axisType==AxisType.radian || axisType==AxisType.radian_pi) {
            out = phase.getDist();
        } else if (axisType==AxisType.degree || axisType==AxisType.degree_180) {
            out = phase.getDist();
            for (int i = 0; i < out.length; i++) {
                out[i] *= 180/Math.PI;
            }
        } else if (axisType==AxisType.kilometer || axisType==AxisType.kilometer_180) {
            out = phase.getDist();
            double redToKm = getRadiusOfEarth();
            for (int i = 0; i < out.length; i++) {
                out[i] *= redToKm;
            }
        } else if (axisType==AxisType.rayparam) {
            out = phase.getRayParams();
        } else if (axisType==AxisType.time) {
            out = phase.getTime();
        } else if (axisType==AxisType.tau) {
            out = phase.getTau();
        } else if (axisType==AxisType.turndepth) {
            double[] dist = phase.getDist();
            out = new double[dist.length];
            for (int i = 0; i < dist.length; i++) {
                Arrival arrival = phase.createArrivalAtIndex(i);
                out[i] = arrival.getDeepestPierce().getDepth();
            }
        } else if (axisType==AxisType.amp ||
                axisType==AxisType.amppsv ||
                axisType==AxisType.ampsh) {
            boolean isAmpSH = axisType==AxisType.ampsh;
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

        } else if (axisType==AxisType.geospread) {
            double[] dist = phase.getDist();
            double[] amp = new double[dist.length];
            for (int i = 0; i < dist.length; i++) {
                Arrival arrival = phase.createArrivalAtIndex(i);
                amp[i] = arrival.getGeometricSpreadingFactor();
            }
            out = amp;
        } else if (axisType==AxisType.refltran ||
                axisType==AxisType.refltranpsv ||
                axisType==AxisType.refltransh) {
            boolean isSH = axisType==AxisType.refltransh;
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
        } else if (axisType == AxisType.index) {
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
            if (axisType== AxisType.degree_180) {
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
            JSONObject out = baseResultAsJSONObject( modelArgs.getModelName(), modelArgs.getSourceDepth(),  modelArgs.getReceiverDepth(), getPhaseNames());
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
                    pixelWidth, margin, getTauModelName()+" (h="+getSourceDepth()+" km)",
                    xAxisType.toString(), yAxisType.toString());


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

    /**
     * True if the axis type is distance-like.
     *
     * @param axisType
     * @return
     */
    public boolean axisIsDistanceLike(AxisType axisType) {
        return axisType == AxisType.degree
                || axisType == AxisType.degree_180
                || axisType == AxisType.radian
                || axisType == AxisType.radian_pi;
    }

    /**
     * True if the axis type is time.
     * @param axisType
     * @return
     */
    public boolean axisIsTimeLike(AxisType axisType) {
        return axisType == AxisType.time;
    }

    @Override
    public void validateArguments() throws TauModelException {

    }

    public AxisType getxAxisType() {
        return xAxisType;
    }

    @CommandLine.Option(names = "-x", description = "X axis data type")
    public void setxAxisType(AxisType xAxisType) {
        this.xAxisType = xAxisType;
    }

    public AxisType getyAxisType() {
        return yAxisType;
    }

    @CommandLine.Option(names = "-y", description = "Y axis data type")
    public void setyAxisType(AxisType yAxisType) {
        this.yAxisType = yAxisType;
    }

    public double[] getxAxisMinMax() {
        return xAxisMinMax;
    }

    @CommandLine.Option(names = "--xminmax",
            arity = "2",
            paramLabel = "x",
            description = "min and max x axis for plotting")
    public void setxAxisMinMax(double[] xAxisMinMax) {
        this.xAxisMinMax = xAxisMinMax;
    }

    public double[] getyAxisMinMax() {
        return yAxisMinMax;
    }

    @CommandLine.Option(names = "--yminmax",
            arity = "2",
            paramLabel = "y",
            description = "min and max y axis for plotting")
    public void setyAxisMinMax(double[] yAxisMinMax) {
        this.yAxisMinMax = yAxisMinMax;
    }

    public boolean isxAxisLog() {
        return xAxisLog;
    }

    @CommandLine.Option(names = "--xlog", description = "X axis is log")
    public void setxAxisLog(boolean xAxisLog) {
        this.xAxisLog = xAxisLog;
    }

    public boolean isyAxisLog() {
        return yAxisLog;
    }

    @CommandLine.Option(names = "--ylog", description = "Y axis is log")
    public void setyAxisLog(boolean yAxisLog) {
        this.yAxisLog = yAxisLog;
    }

    public boolean isReduceTime() {
        return reduceTime;
    }

    public void setReduceTime(boolean reduceTime) {
        this.reduceTime = reduceTime;
    }

    protected String relativePhaseName = "";

    public String getRelativePhaseName() {
        return relativePhaseName;
    }

    @CommandLine.Option(names = "--rel", description = "plot relative to the given phase, no effect unless distance/time")
    public void setRelativePhaseName(String relativePhaseName) {
        this.relativePhaseName = relativePhaseName;
    }

    /**
     * @return reducing velocity in degrees/second. The internal usage is
     *          radians/second.
     */
    public double getReduceVelRadian() {
        if (reduceVelKm != null) {
            try {
                return reduceVelKm / modelArgs.getTauModel().getRadiusOfEarth();
            } catch (TauModelException e) {
                throw new RuntimeException(e);
            }
        }
        if (reduceVelDeg != null) {
            return reduceVelDeg * Math.PI / 180.0;
        }
        return 0;
    }
    public double getReduceVelDeg() {
        return reduceVelDeg;
    }

    /**
     * set the reducing velocity, in degrees/second. The internal representation
     * is radians/second.
     */

    @CommandLine.Option(names = "reddeg",
            description = "outputs curves with a reducing velocity (deg/sec), no effect if axis is not distance-like/time")
    public void setReduceVelDeg(double reduceVel) {
        if(reduceVel != 0.0) {
            redVelString = reduceVel+" deg/s";
            this.reduceVelDeg = reduceVel;
        }
    }

    /**
     * @return reducing velocity in kilometers/second. The internal usage is
     *          radians/second.
     */
    public double getReduceVelKm() {
        return reduceVelKm;
    }

    public double reduceVelForAxis(AxisType axisType) {
        if (axisType==AxisType.degree) {
            return getReduceVelDeg();
        } else if (axisType==AxisType.kilometer) {
            return getReduceVelKm();
        } else if (axisType==AxisType.radian) {
            return getReduceVelDeg()*Math.PI/180;
        } else {
            throw new IllegalArgumentException("axis type not distance-like: "+axisType);
        }
    }

    /**
     * set the reducing velocity, in kilometers/second. The internal
     * representation is radians/second.
     */
    @CommandLine.Option(names = "redkm",
            description = "outputs curves with a reducing velocity (km/sec), no effect if axis is not distance-like/time")
    public void setReduceVelKm(double reduceVel) {
        redVelString = reduceVel+" km/s";
        if(reduceVel != 0.0) {
            this.reduceVelKm = reduceVel;
        } else {
            throw new IllegalArgumentException("Reducing velocity must be positive: "+reduceVel);
        }
    }

    protected AxisType xAxisType = AxisType.degree;
    protected AxisType yAxisType = AxisType.time;

    protected boolean xAxisLog = false;
    protected boolean yAxisLog = false;

    /** should the output times use a reducing velocity? */
    protected boolean reduceTime = false;

    /**
     * the reducing velocity to use if reduceTime == true, in units of
     * radians/second .
     */
    protected Double reduceVelDeg = .125;
    protected Double reduceVelKm = 8.0;

    protected String redVelString = ".125 deg/s";

    protected double[] xAxisMinMax = new double[0];
    protected double[] yAxisMinMax = new double[0];

    @CommandLine.Mixin
    GraphicOutputTypeArgs outputTypeArgs = new GraphicOutputTypeArgs();

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOuputFormat();
    }

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
