package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.ColorType;
import edu.sc.seis.TauP.cli.ColoringArgs;
import edu.sc.seis.TauP.cli.GraphicOutputTypeArgs;
import edu.sc.seis.TauP.cli.OutputTypes;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommandLine.Command(name = "curve", description = "plot traveltime and other curves for phases")
public class TauP_Curve extends TauP_AbstractPhaseTool {
    public TauP_Curve() {
        setDefaultOutputFormat();
        outputTypeArgs.setOutFileBase("taup_curve");
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
        PrintWriter writer = outputTypeArgs.createWriter();
        if(modelArgs.getSourceDepth() != -1 * Double.MAX_VALUE) {
            /* enough info given on cmd line, so just do one calc. */

            List<XYPlottingData> xy = calculate(xAxisType, yAxisType);
            printResult(writer, xy);
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

            List<XYPlottingData> xy = calculate(xAxisType, yAxisType);
            printResult(writer, xy);
        }
        writer.close();
    }

    public List<XYPlottingData> calculate(AxisType xAxisType, AxisType yAxisType) throws TauPException {
        List<XYPlottingData>  xy = calculateLinear(xAxisType, yAxisType);
        if (isReduceTime()) {
            xy = reduce(xy);
        }
        if (isxAxisLog() || isyAxisLog()) {
            xy = XYPlotOutput.recalcForLog(xy, isxAxisLog(), isyAxisLog());
        }
        return xy;
    }

    public List<XYPlottingData> calculateLinear(AxisType xAxisType, AxisType yAxisType) throws TauModelException, VelocityModelException, SlownessModelException {
        modelArgs.depthCorrected();
        List<SeismicPhase> phaseList = getSeismicPhases();
        List<XYPlottingData> out = new ArrayList<>();
        for (SeismicPhase phase: phaseList) {
            String phaseDesc = xAxisType+"/"+yAxisType+" "+phase.getName()+" for a source depth of "+modelArgs.getSourceDepth()+" kilometers in the "+modelArgs.getModelName()+" model";
            String p_or_s = "both_p_swave";
            if (phase.isAllSWave()) {
                p_or_s = "swave";
            } else if (phase.isAllPWave()) {
                p_or_s = "pwave";
            }
            boolean ensure180 = (xAxisType==AxisType.degree180 || yAxisType==AxisType.degree180
                    || xAxisType==AxisType.radian180 || yAxisType==AxisType.radian180);
            if(phase.hasArrivals()) {
                if (yAxisType==AxisType.theta) {
                    // temp for testing...
                    double dist = 15;
                    List<Arrival> arrivals = phase.calcTime(DistanceRay.ofDegrees(dist));
                    for (Arrival arrival : arrivals) {
                        Theta theta = new Theta(arrival);
                        List<double[]> xData = SeismicPhase.splitForRepeatRayParam(phase.getRayParams(), phase.getRayParams());
                        List<double[]> yData = SeismicPhase.splitForRepeatRayParam(theta.rayParams, theta.thetaAtX);
                        List<XYSegment> segmentList = new ArrayList<>();
                        for (int i = 0; i < xData.size(); i++) {
                            XYSegment seg = new XYSegment(xData.get(i), yData.get(i));
                            segmentList.add(seg);
                        }
                        List<String> cssClasses = new ArrayList<>();
                        cssClasses.add(p_or_s);
                        cssClasses.add(SvgUtil.classForPhase(arrival.getName()));
                        XYPlottingData xyp = new XYPlottingData(
                                segmentList, xAxisType.toString(), "Ray Param",
                                phase.getName(), phaseDesc, cssClasses
                        );
                        out.add(xyp);

                    }
                } else {

                    List<double[]> xData = calculatePlotForType(phase, xAxisType, ensure180);
                    List<double[]> yData = calculatePlotForType(phase, yAxisType, ensure180);
                    List<XYSegment> segments = XYSegment.createFromLists(xData, yData);
                    List<String> cssClasses = new ArrayList<>();
                    cssClasses.add(p_or_s);
                    cssClasses.add(SvgUtil.classForPhase(phase.getName()));
                    XYPlottingData xyp = new XYPlottingData(segments, xAxisType.toString(), yAxisType.toString(),
                            phase.getName(), phaseDesc, cssClasses);
                    xyp.cssClasses.add(p_or_s);
                    out.add(xyp);

                    if (phase.isAllSWave()) {
                        // second calc needed for sh, as psv done in main calc
                        if (xAxisType==AxisType.amp
                                    || yAxisType==AxisType.amp) {
                            AxisType xOther = xAxisType==AxisType.amp ? AxisType.ampsh : xAxisType;
                            AxisType yOther = yAxisType==AxisType.amp ? AxisType.ampsh : yAxisType;

                            xData = calculatePlotForType(phase, xOther, ensure180);
                            yData = calculatePlotForType(phase, yOther, ensure180);
                            List<XYSegment> sh_segments = XYSegment.createFromLists(xData, yData);
                            List<String> cssClassesCopy = new ArrayList<>(cssClasses);
                            cssClassesCopy.add("ampsh");
                            out.add(new XYPlottingData(sh_segments, xAxisType.toString(), yAxisType.toString(),
                                    phase.getName(), phaseDesc, cssClassesCopy));
                        }
                        // what about case of amp vs refltran, need 4 outputs?
                        if (xAxisType==AxisType.refltran
                                || yAxisType==AxisType.refltran) {
                            AxisType xOther = xAxisType==AxisType.refltran ? AxisType.refltransh : xAxisType;
                            AxisType yOther = yAxisType==AxisType.refltran ? AxisType.refltransh : yAxisType;

                            xData = calculatePlotForType(phase, xOther, ensure180);
                            yData = calculatePlotForType(phase, yOther, ensure180);
                            List<XYSegment> sh_segments = XYSegment.createFromLists(xData, yData);
                            List<String> cssClassesCopy = new ArrayList<>(cssClasses);
                            cssClassesCopy.add("refltransh");
                            out.add(new XYPlottingData(sh_segments, xAxisType.toString(), yAxisType.toString(),
                                    phase.getName(), phaseDesc, cssClassesCopy));
                        }
                    }
                }
            }
        }
        return out;
    }

    public List<XYPlottingData> reduce(List<XYPlottingData> xy) throws TauModelException {
        List<XYPlottingData> out = new ArrayList<>();
        Double velFactor;
        if (axisIsDistanceLike(xAxisType) && axisIsTimeLike(yAxisType)) {
            // x is distance, y is time
            velFactor = reduceVelForAxis(xAxisType);
        } else if (axisIsDistanceLike(yAxisType) && axisIsTimeLike(xAxisType)) {
            velFactor = reduceVelForAxis(yAxisType);
        } else {
            throw new IllegalArgumentException("axis are not time and distance: "+xAxisType+" "+yAxisType);
        }
        if (velFactor == null || velFactor == 1.0 || velFactor == 0.0) {
            // either not set or nothing to do
            throw new IllegalArgumentException("Unable to reduce, red vel is invalid: "+velFactor);
            //return xy;
        }

        for (XYPlottingData xyp : xy) {
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
            XYPlottingData redxyp = new XYPlottingData(redSeg, xyp.xAxisType, xyp.yAxisType,
                    xyp.label, xyp.description+", reduce: "+getRedVelLabel(), xyp.cssClasses);
            out.add(redxyp);
        }
        return out;
    }


    public List<double[]> calculatePlotForType(SeismicPhase phase, AxisType axisType, boolean ensure180) throws VelocityModelException, SlownessModelException, TauModelException {
        double[] out = new double[0];
        boolean flatPhase = phase.getRayParams().length == 2;
        if (axisType==AxisType.radian || axisType==AxisType.radian180) {
            out = phase.getDist();
        } else if (axisType==AxisType.degree || axisType==AxisType.degree180) {
            out = phase.getDist();
            for (int i = 0; i < out.length; i++) {
                out[i] *= 180/Math.PI;
            }
        } else if (axisType==AxisType.kilometer || axisType==AxisType.kilometer180) {
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
            double[] dist = new double[phase.getDist().length];
            System.arraycopy(phase.getDist(), 0, dist, 0, dist.length);
            // insert extra values, linearly interpolated, if spans 180 deg
            int wrapMinIndex = (int)Math.round(Math.floor(phase.getMinDistance()/Math.PI));
            int wrapMaxIndex = (int)Math.round(Math.ceil(phase.getMaxDistance()/Math.PI));
            for (int j = wrapMinIndex; j <= wrapMaxIndex; j++) {
                double wrapRadian = j*Math.PI;
                for (int i=1; i < out.length; i++) {
                    if ((dist[i-1] < wrapRadian && wrapRadian < dist[i] ) || (dist[i-1] > wrapRadian &&  wrapRadian > dist[i]) ) {
                        // dist spans a multiple of PI, repeated ray params are already a break so don't interpolate
                        double[] outSeg = new double[out.length+1];
                        double[] distSeg = new double[out.length+1];
                        double[] rpSeg = new double[rayParams.length+1];
                        System.arraycopy(out, 0, outSeg, 0, i);
                        System.arraycopy(dist, 0, distSeg, 0, i);
                        System.arraycopy(rayParams, 0, rpSeg, 0, i);
                        outSeg[i] = linearInterp(dist[i-1], out[i-1], dist[i], out[i], wrapRadian);
                        distSeg[i] = wrapRadian;
                        rpSeg[i] = linearInterp(dist[i-1], rayParams[i-1], dist[i], rayParams[i], wrapRadian);
                        System.arraycopy(out, i, outSeg, i+1, out.length-i);
                        System.arraycopy(dist, i, distSeg, i+1, out.length-i);
                        System.arraycopy(rayParams, i, rpSeg, i+1, out.length-i);
                        out = outSeg;
                        dist = distSeg;
                        rayParams = rpSeg;
                        break;
                    }
                }
            }
            if (axisType== AxisType.degree180) {
                for (int j = 0; j < out.length; j++) {
                    out[j] = Math.abs(out[j] % 360.0);
                    if (out[j] > 180.0) {
                        out[j] = 360.0 - out[j];
                    }
                }
            } else if (axisType == AxisType.radian180) {
                for (int j = 0; j < out.length; j++) {
                    out[j] = Math.abs(out[j] % (2*Math.PI));
                    if (out[j] > Math.PI) {
                        out[j] = 2*Math.PI - out[j];
                    }
                }
            } else if (axisType == AxisType.kilometer180) {
                double km180 = phase.getTauModel().getRadiusOfEarth()*Math.PI;
                for (int j = 0; j < out.length; j++) {
                    out[j] = Math.abs(out[j] % (2*km180));
                    if (out[j] > km180) {
                        out[j] = 2*km180 - out[j];
                    }
                }
            }
        }

        List<double[]> splitCurve;
        if (! flatPhase) {
            // repeated ray parameters indicate break in curve, split into segments
            splitCurve = SeismicPhase.splitForRepeatRayParam(rayParams, out);
        } else {
            splitCurve = List.of(out);
        }
        return splitCurve;
    }

    public void printResult(PrintWriter writer, List<XYPlottingData> xyPlots) throws TauModelException {
        XYPlotOutput xyOut = new XYPlotOutput(xyPlots, modelArgs);
        xyOut.setPhaseNames(getPhaseNames());
        xyOut.setxAxisMinMax(xAxisMinMax);
        xyOut.setyAxisMinMax(yAxisMinMax);
        if (yAxisType == AxisType.turndepth) {
            xyOut.yAxisInvert = true;
        }
        if (getOutputFormat().equalsIgnoreCase(OutputTypes.JSON)) {
            xyOut.printAsJSON(writer, 2);
        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.TEXT) || getOutputFormat().equalsIgnoreCase(OutputTypes.GMT)) {
            xyOut.printAsGmtText(writer);
        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.SVG)) {
            String cssExtra = "";
            if (coloring.getColor() == ColorType.phase) {
                cssExtra += SvgUtil.createPhaseColorCSS(Arrays.asList(getPhaseNames()));
            } else if (coloring.getColor() == ColorType.wavetype) {
                cssExtra += SvgUtil.createWaveTypeColorCSS();
            } else {
            }
            xyOut.printAsSvg(writer, cmdLineArgs, xAxisType.toString(), yAxisType.toString(), cssExtra, isLegend);
        } else {
            throw new IllegalArgumentException("Unknown output format: " + getOutputFormat());
        }
        writer.flush();
    }

    @Override
    public void destroy() throws TauPException {

    }

    /**
     * True if the axis type is distance-like.
     *
     */
    public static boolean axisIsDistanceLike(AxisType axisType) {
        return axisType == AxisType.degree
                || axisType == AxisType.degree180
                || axisType == AxisType.radian
                || axisType == AxisType.radian180;
    }

    /**
     * True if the axis type is time.
     */
    public static boolean axisIsTimeLike(AxisType axisType) {
        return axisType == AxisType.time;
    }

    @Override
    public void validateArguments() throws TauModelException {

    }

    public AxisType getxAxisType() {
        return xAxisType;
    }

    @CommandLine.Option(names = "-x", description = "X axis data type, one of ${COMPLETION-CANDIDATES}", defaultValue = "degree180")
    public void setxAxisType(AxisType xAxisType) {
        this.xAxisType = xAxisType;
    }

    public AxisType getyAxisType() {
        return yAxisType;
    }

    @CommandLine.Option(names = "-y", description = "Y axis data type, one of ${COMPLETION-CANDIDATES}", defaultValue = "time")
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
        return reduceVelKm != null || reduceVelDeg != null;
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
    public Double getReduceVelDeg() {
        return reduceVelDeg;
    }

    /**
     * set the reducing velocity, in degrees/second. The internal representation
     * is radians/second.
     */

    @CommandLine.Option(names = "--reddeg",
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
    public Double getReduceVelKm() {
        return reduceVelKm;
    }

    public Double reduceVelForAxis(AxisType axisType) throws TauModelException {
        if (axisType==AxisType.degree || axisType==AxisType.degree180) {
            if (getReduceVelDeg() != null) {
                return getReduceVelDeg();
            } else if (getReduceVelKm() != null) {
                return getReduceVelRadian() * 180/Math.PI;
            }
        } else if (axisType==AxisType.kilometer || axisType==AxisType.kilometer180) {
            if (getReduceVelKm() != null) {
                return getReduceVelKm();
            } else if (getReduceVelDeg() != 0) {
                return getReduceVelRadian() * modelArgs.getTauModel().getRadiusOfEarth();
            }
        } else if (axisType==AxisType.radian) {
            return getReduceVelRadian();
        }
        throw new IllegalArgumentException("axis type not distance-like: "+axisType);

    }

    /**
     * set the reducing velocity, in kilometers/second. The internal
     * representation is radians/second.
     */
    @CommandLine.Option(names = "--redkm",
            description = "outputs curves with a reducing velocity (km/sec), no effect if axis is not distance-like/time")
    public void setReduceVelKm(double reduceVel) {
        redVelString = reduceVel+" km/s";
        if(reduceVel != 0.0) {
            this.reduceVelKm = reduceVel;
        } else {
            throw new IllegalArgumentException("Reducing velocity must be positive: "+reduceVel);
        }
    }

    public String getRedVelLabel() {
        return redVelString;
    }

    protected AxisType xAxisType = AxisType.degree;
    protected AxisType yAxisType = AxisType.time;

    protected boolean xAxisLog = false;
    protected boolean yAxisLog = false;

    /**
     * the reducing velocity to use if reduceTime == true, in units of
     * radians/second .
     */
    protected Double reduceVelDeg = null;
    protected Double reduceVelKm = null;

    protected String redVelString = "";

    protected double[] xAxisMinMax = new double[0];
    protected double[] yAxisMinMax = new double[0];

    @CommandLine.Mixin
    GraphicOutputTypeArgs outputTypeArgs = new GraphicOutputTypeArgs();

    @CommandLine.Mixin
    ColoringArgs coloring = new ColoringArgs();

    @CommandLine.Option(names = "--legend", description = "create a legend")
    boolean isLegend = false;

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOuputFormat();
    }

    @Override
    public String getOutFileExtension() {
        return outputTypeArgs.getOutFileExtension();
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
