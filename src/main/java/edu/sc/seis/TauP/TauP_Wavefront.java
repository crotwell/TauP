package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.*;
import picocli.CommandLine;

import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

import static edu.sc.seis.TauP.SvgEarth.calcEarthScaleTrans;
import static edu.sc.seis.TauP.cli.OutputTypes.GMT;
import static edu.sc.seis.TauP.cli.OutputTypes.SVG;

/**
 * Plots of wavefronts, distance along the ray at points in time.
 */
@CommandLine.Command(name = "wavefront", description = "plot wavefronts of seismic phases at steps in time")
public class TauP_Wavefront extends TauP_AbstractPhaseTool {

    int numRays = 30;

    float timeStep = 100;

    boolean separateFilesByTime = false;

    boolean negDistance = false;

    boolean doInteractive = false;

    @CommandLine.Mixin
    ColoringArgs coloring = new ColoringArgs();

    @CommandLine.Option(names = "--colortime", description = "generate css colors by time, default is by phase name")
    boolean cssColorTime = false;
    @CommandLine.Mixin
    DistDepthRange distDepthRangeArgs = new DistDepthRange();

    @CommandLine.Mixin
    GraphicOutputTypeArgs outputTypeArgs = new GraphicOutputTypeArgs();


    @Override
    public String getOutFileExtension() {
        return outputTypeArgs.getOutFileExtension();
    }

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOuputFormat();
    }
    Map<SeismicPhase, Map<Float, List<TimeDist>>> result;

    
    
    public TauP_Wavefront() {
        super();
        outputTypeArgs.setOutFileBase("taup_wavefront");
        setDefaultOutputFormat();
    }

    @Override
    public String[] allowedOutputFormats() {
        return new String[] {SVG, GMT};
    }

    @Override
    public void setDefaultOutputFormat() {
        outputTypeArgs.setOutputType(SVG);
        setOutputFormat(OutputTypes.SVG);
    }

    public TauP_Wavefront(String modelName, String outFileBase) throws TauModelException {
        setModelName(modelName);
        outputTypeArgs.setOutFileBase(outFileBase);
        setDefaultOutputFormat();
    }

    public TauP_Wavefront(String modelName) throws TauModelException {
        setModelName(modelName);
        outputTypeArgs.setOutFileBase("taup_wavefront");
        setDefaultOutputFormat();
    }

    public TauP_Wavefront(TauModel tMod, String outFileBase) throws TauModelException {
        setTauModel(tMod);
        outputTypeArgs.setOutFileBase(outFileBase);
        setDefaultOutputFormat();
    }

    public TauP_Wavefront(TauModel tMod) throws TauModelException {
        setTauModel(tMod);
        outputTypeArgs.setOutFileBase("taup_wavefront");
        setDefaultOutputFormat();
    }

    public List<Arrival> calculate(List<DistanceRay> distanceRays) throws TauPException {
        // ignore degrees as we need a suite of distances for each phase
        result = calcIsochron();
        return new ArrayList<Arrival>();
    }


    @Override
    public void validateArguments() throws TauModelException {

    }

    @Override
    public void printScriptBeginning(PrintWriter out) throws IOException {
        if (getOutputFormat().equals(OutputTypes.JSON)) {
            throw new RuntimeException("JSON output for TauP_Path not yet supported.");
        } else if (outputTypeArgs.isSVG()) {
            return;
        } else if ( outputTypeArgs.gmtScript) {
            if (outputTypeArgs.getOutFile().equals("stdout")) {
                outputTypeArgs.psFile = "taup_wavefront.ps";
            }
            super.printScriptBeginning(out);
        }
    }

    public void printResult(PrintWriter out) throws IOException, TauModelException {
        TauModel tModDepth = modelArgs.depthCorrected();
        if (outputTypeArgs.isSVG()) {
            float pixelWidth = (72.0f * outputTypeArgs.mapwidth);
            int plotOffset = 0;
            float R;
            R = (float) tModDepth.getRadiusOfEarth();
            float plotOverScaleFactor = 1.1f;
            float plotSize = R * plotOverScaleFactor;
            float plotScale = pixelWidth / (2 * R * plotOverScaleFactor);
            List<Arrival> arrivalList = SvgEarth.createBoundingArrivals(getSeismicPhases());
            float[] scaleTrans = calcEarthScaleTrans(arrivalList, distDepthRangeArgs);
            float zoomScale = scaleTrans[0];

            int fontSize = (int) (plotSize / 20);
            fontSize = (int) (fontSize / zoomScale);


            StringBuffer extrtaCSS = new StringBuffer();
            extrtaCSS.append("        text.label {\n");
            extrtaCSS.append("            font: bold ;\n");
            extrtaCSS.append("            font-size: "+fontSize+"px;\n");
            extrtaCSS.append("            fill: black;\n");
            extrtaCSS.append("        }\n");
            extrtaCSS.append("        g.phasename text {\n");
            extrtaCSS.append("            font: bold ;\n");
            extrtaCSS.append("            font-size: "+fontSize+"px;\n");
            extrtaCSS.append("            fill: black;\n");
            extrtaCSS.append("        }\n");
            SvgUtil.xyplotScriptBeginning( out, toolNameFromClass(this.getClass()),
                    cmdLineArgs,  pixelWidth, plotOffset, extrtaCSS.toString());

            SvgEarth.printModelAsSVG(out, tModDepth, pixelWidth, scaleTrans);
            printResultSVG(out);
        } else {
            String psFile;
            if (outputTypeArgs.getOutFileBase().equals("stdout")) {
                psFile = "taup_path.ps";
            } else if (outputTypeArgs.getOutFile().endsWith(".gmt")) {
                psFile = outputTypeArgs.getOutFile().substring(0, outputTypeArgs.getOutFile().length() - 4) + ".ps";
            } else {
                psFile = outputTypeArgs.getOutFile() + ".ps";
            }
            SvgEarth.printGmtScriptBeginning(out, psFile, tModDepth, outputTypeArgs.mapwidth, outputTypeArgs.mapWidthUnit);
            printResultGMT(out);
        }
        out.flush();
    }

    public void printResultSVG(PrintWriter out) throws IOException {
        HashSet<Float> keySet = new HashSet<Float>();
        for (SeismicPhase phase : result.keySet()) {
            Map<Float, List<TimeDist>> phaseResult = result.get(phase);
            keySet.addAll(phaseResult.keySet());
        }
        if (keySet.size() == 0) {
            // no phases successful?
            System.err.println("taup wavefront, no phases successful...");
            return;
        }
        List<Float> keys = new ArrayList<Float>();
        keys.addAll(keySet);
        Collections.sort(keys);
        Float lastTime = keys.get(keys.size() - 1);
        int numDigits = 1;
        String formatStr = "0";
        while (Math.pow(10, numDigits) < lastTime) {
            numDigits++;
            formatStr += "0";
        }
        if (lastTime < 1) {
            formatStr += ".0";
            int fracDigits = 0;
            while (Math.pow(10, fracDigits) > lastTime) {
                fracDigits--;
                formatStr += "0";
            }
        }
        DecimalFormat format = new DecimalFormat(formatStr, new DecimalFormatSymbols(Locale.US));
        PrintWriter timeOut = out;

        List<SeismicPhase> phasekeys = new ArrayList<SeismicPhase>();
        phasekeys.addAll(result.keySet());
        Collections.sort(phasekeys, new Comparator<SeismicPhase>() {
            // @Override
            public int compare(SeismicPhase arg0, SeismicPhase arg1) {
                return arg0.getName().compareTo(arg1.getName());
            }
        });
        for (SeismicPhase phase : phasekeys) {
            timeOut.println("<g class=\"autocolor\" >");
            for (Float time : keys) {
                String timeStr = "time_"+time;
                timeStr = timeStr.replace('.','_');

                printResultPhaseAtTime(timeOut, phase, time,timeStr);
            }
            timeOut.println("</g> <!-- end "+phase.getName()+" -->");
        }
        out.println("</g> <!-- end translate -->");
        out.println("  </g> ");
        out.println("  </g> ");
        out.println("</svg>");

        timeOut.flush();
        out.flush();
    }

    public void printResultGMT(PrintWriter out) throws IOException, TauModelException {
        String byTimePsFile = outputTypeArgs.psFile;
        HashSet<Float> keySet = new HashSet<Float>();
        for (SeismicPhase phase : result.keySet()) {
            Map<Float, List<TimeDist>> phaseResult = result.get(phase);
            keySet.addAll(phaseResult.keySet());
        }
        if (keySet.size() == 0) {
            // no phases successful?
            System.err.println("taup wavefront, no phases successful...");
            return;
        }
        List<Float> keys = new ArrayList<Float>();
        keys.addAll(keySet);
        Collections.sort(keys);
        Float lastTime = keys.get(keys.size() - 1);
        int numDigits = 1;
        String formatStr = "0";
        while (Math.pow(10, numDigits) < lastTime) {
            numDigits++;
            formatStr += "0";
        }
        if (lastTime < 1) {
            formatStr += ".0";
            int fracDigits = 0;
            while (Math.pow(10, fracDigits) > lastTime) {
                fracDigits--;
                formatStr += "0";
            }
        }
        DecimalFormat format = new DecimalFormat(formatStr, new DecimalFormatSymbols(Locale.US));
        PrintWriter timeOut = out;
        for (Float time : keys) {
            String timeStr = "time_"+time;
            timeStr = timeStr.replace('.','_');
            if (separateFilesByTime) {
                String psFileBase = outputTypeArgs.psFile;
                if (outputTypeArgs.gmtScript && psFileBase.endsWith(".ps")) {
                    psFileBase = psFileBase.substring(0, psFileBase.length() - 3);
                }
                String timeExt = "_" + format.format(time);
                byTimePsFile = psFileBase + timeExt + ".ps";
                String timeOutName = outputTypeArgs.getOutFileBase()+timeExt;
                if (outputTypeArgs.getOutFile().endsWith(".gmt")) {
                    timeOutName = outputTypeArgs.getOutFile().substring(0, outputTypeArgs.getOutFile().length() - 4)+timeExt + ".gmt";
                }
                if (timeOut != null && timeOut != out) {timeOut.close();}
                timeOut = new PrintWriter(new BufferedWriter(new FileWriter(timeOutName)));
                if (outputTypeArgs.gmtScript) {
                    SvgEarth.printGmtScriptBeginning(timeOut, byTimePsFile, modelArgs.depthCorrected(), outputTypeArgs.mapwidth, outputTypeArgs.mapWidthUnit);
                }
            }
            if (outputTypeArgs.gmtScript) {
                timeOut.println("# timestep = " + time);
                timeOut.println("gmt psxy -P -R -K -O -Wblue -JP -m -A >> " + byTimePsFile + " <<END");
            }

            List<SeismicPhase> phasekeys = new ArrayList<SeismicPhase>();
            phasekeys.addAll(result.keySet());
            Collections.sort(phasekeys, new Comparator<SeismicPhase>() {
                // @Override
                public int compare(SeismicPhase arg0, SeismicPhase arg1) {
                    return arg0.getName().compareTo(arg1.getName());
                }
            });
            for (SeismicPhase phase : phasekeys) {
                printResultPhaseAtTime(timeOut, phase, time,timeStr);
            }
            if (outputTypeArgs.gmtScript) {
                if (separateFilesByTime) {
                    timeOut.println("# end postscript"); 
                    timeOut.println("gmt psxy -P -R -O -JP -m -A -T >> " + byTimePsFile );
                    timeOut.println("# convert ps to pdf, clean up .ps file"); 
                    timeOut.println("gmt psconvert -P -Tf  " + byTimePsFile+" && rm " + byTimePsFile);
                    timeOut.println("# clean up after gmt...");
                    timeOut.println("/bin/rm gmt.history");
                }
            }
        }
        if (outputTypeArgs.gmtScript && ! separateFilesByTime) {
            out.println("# end postscript"); 
            out.println("gmt psxy -P -R -O -JP -m -A -T >> " + byTimePsFile);
            out.println("# convert ps to pdf, clean up .ps file"); 
            out.println("gmt psconvert -P -Tf  " + byTimePsFile+" && rm " + byTimePsFile);
            out.println("# clean up after gmt...");
            out.println("rm gmt.history");
        } else if (outputTypeArgs.isSVG()) {
            out.println("</g> <!-- end translate -->");
            out.println("  </g> ");
            out.println("  </g> ");
            out.println("</svg>");
        }
        timeOut.flush();
        out.flush();
    }

    public void printResultPhaseAtTime(PrintWriter timeOut, SeismicPhase phase, Float time, String timeStr) throws IOException {

        Map<Float, List<TimeDist>> phaseResult = result.get(phase);
        List<TimeDist> wavefront = phaseResult.get(time);
        if (wavefront == null || wavefront.size() == 0) {
            return;
        }
        if (outputTypeArgs.isGMT()) {
            timeOut.println("> " + phase.getName() + " at " + time + " seconds");
        } else if (outputTypeArgs.isSVG()) {
            timeOut.println("<!-- " + phase.getName() + " at " + time + " seconds");
            timeOut.println(" -->");
            timeOut.println("<polyline class=\"wavefront "+SvgUtil.classForPhase(phase.getName())+" "+timeStr+"\" points=\"");
        }
        Collections.sort(wavefront, new Comparator<TimeDist>() {

            // @Override
            public int compare(TimeDist arg0, TimeDist arg1) {
                return Double.valueOf(arg0.getP()).compareTo(arg1.getP());
            }
        });
        double radiusOfEarth = phase.getTauModel().getRadiusOfEarth();
        for (TimeDist td : wavefront) {
            if (outputTypeArgs.isGMT()) {
                timeOut.println(Outputs.formatDistance(td.getDistDeg()) + "  "
                        + Outputs.formatDepth(radiusOfEarth - td.getDepth()) + " " + Outputs.formatTime(time) + " "
                        + Outputs.formatRayParam(td.getP()));
            } else if (outputTypeArgs.isSVG()) {
                SvgEarth.printDistRadiusAsXY(timeOut, td.getDistDeg(), radiusOfEarth - td.getDepth());
                timeOut.println();
            }
        }
        if (outputTypeArgs.isSVG()) {
            timeOut.println("\" />");
        }
        if (isNegDistance()) {
            if (outputTypeArgs.isGMT()) {
                timeOut.write("> " + phase.getName() + " at " + time + " seconds (neg distance)\n");
            } else if (outputTypeArgs.isSVG()) {
                timeOut.println("<!-- " + phase.getName() + " at " + time + " seconds (neg distance)");
                timeOut.println(" -->");
                timeOut.println("<polyline class=\"wavefront "+SvgUtil.classForPhase((phase.getName()))+" "+timeStr+"\" points=\"");
            }
            for (TimeDist td : wavefront) {
                if (outputTypeArgs.isGMT()) {
                    timeOut.println(Outputs.formatDistance(-1*td.getDistDeg()) + "  "
                            + Outputs.formatDepth(radiusOfEarth - td.getDepth()) + " " + Outputs.formatTime(time) + " "
                            + Outputs.formatRayParam(td.getP()));
                } else if (outputTypeArgs.isSVG()) {
                    SvgEarth.printDistRadiusAsXY(timeOut, -1*td.getDistDeg(), radiusOfEarth - td.getDepth());
                    timeOut.println();
                }
            }
            if (outputTypeArgs.isSVG()) {
                timeOut.println("\" />");
            }
        }
        if (outputTypeArgs.isSVG()) {
        } else if (outputTypeArgs.gmtScript) {
            timeOut.println("END");
        }
    }

    public void printIsochron(PrintWriter writer, List<XYPlottingData> xyPlots, String cssExtra, boolean negDistance) throws TauPException, IOException {
        if (negDistance) {
            List<XYPlottingData> negXYPlots = new ArrayList<>();
            for (XYPlottingData xyp : xyPlots) {
                List<XYSegment> negSegList = new ArrayList<>();
                for (XYSegment seg : xyp.segmentList) {
                    negSegList.add(seg);
                    double[] negx = new double[seg.x.length];
                    for (int i = 0; i < negx.length; i++) {
                        negx[i] = -1 * seg.x[i];
                    }
                    XYSegment nseg = new XYSegment(negx, seg.y);
                    nseg.cssClasses = seg.cssClasses;
                    nseg.description = seg.description;
                    negSegList.add(nseg);
                }
                XYPlottingData negxyp = new XYPlottingData(negSegList, xyp.xAxisType, xyp.yAxisType, xyp.label, xyp.cssClasses);
                negXYPlots.add(negxyp);
            }
            xyPlots = negXYPlots;
        }
        XYPlotOutput xyOut = new XYPlotOutput(xyPlots, modelArgs);
        xyOut.setxAxisMinMax(distDepthRangeArgs.getDistAxisMinMax());
        xyOut.setyAxisMinMax(distDepthRangeArgs.getDepthAxisMinMax());

        if (getOutputFormat().equalsIgnoreCase(OutputTypes.JSON)) {
            xyOut.printAsJSON(writer, 2);
        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.TEXT) || getOutputFormat().equalsIgnoreCase(OutputTypes.GMT)) {
            xyOut.printAsGmtText(writer);
        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.SVG)) {
            float pixelWidth = (72.0f * getGraphicOutputTypeArgs().mapwidth);
            TauModel tMod = modelArgs.depthCorrected();
            List<Arrival> arrivalList = SvgEarth.createBoundingArrivals(getSeismicPhases());
            float[] scaleTrans = calcEarthScaleTrans(arrivalList, distDepthRangeArgs);
            SvgEarth.printScriptBeginningSvg(writer, tMod, pixelWidth, scaleTrans, toolNameFromClass(this.getClass()), cmdLineArgs, cssExtra);
            SvgEarth.printModelAsSVG(writer, tMod, pixelWidth, scaleTrans);
            xyOut = xyOut.convertToCartesian();
            for (XYPlottingData xyp : xyOut.xyPlots) {
                xyp.asSVG(writer);
            }
            SvgEarth.printSvgEnding(writer);
        } else {
            throw new IllegalArgumentException("Unknown output format: " + getOutputFormat());
        }
        writer.flush();
    }

    public Map<Double, List<XYPlottingData>> calcIsochronNEW() throws TauModelException {
        Map<Double, List<XYPlottingData>> out = new HashMap<>();

        double maxTime = 0;
        for (SeismicPhase phase : getSeismicPhases()) {
            maxTime = Math.max(maxTime, phase.getMaxTime());
            Map<Double, XYPlottingData> isochrons = calcIsochronsForPhase(phase, timeStep);
            for (Double timeVal : isochrons.keySet()) {
                List<XYPlottingData> xypList;
                if (out.keySet().contains(timeVal)) {
                    xypList = out.get(timeVal);
                } else {
                    xypList = new ArrayList<>();
                    out.put(timeVal, xypList);
                }
                xypList.add(isochrons.get(timeVal));
            }
        }
        return out;
    }

    public Map<Double, XYPlottingData> calcIsochronsForPhase(SeismicPhase phase, double timeStep) {
        double minDist = phase.getMinDistanceDeg();
        double maxDist = phase.getMaxDistanceDeg();
        double deltaDist = (maxDist - minDist) / (numRays - 1);
        List<Arrival> allArrival = new ArrayList<Arrival>();
        Arrival zerothArrival = phase.createArrivalAtIndex(0);
        Arrival lastIdxArrival = phase.createArrivalAtIndex(phase.getRayParams().length-1);
        if (zerothArrival.getDist() > lastIdxArrival.getDist()) {
            // retrograde, so swap
            Arrival tmp = zerothArrival;
            zerothArrival = lastIdxArrival;
            lastIdxArrival = tmp;
        }
        allArrival.add(zerothArrival);
        for (int r = 0; r < getNumRays(); r++) {
            double degrees = minDist + r * deltaDist;
            List<Arrival> phaseArrivals = phase.calcTimeExactDistanceDeg(degrees);
            allArrival.addAll(phaseArrivals);
        }
        allArrival.add(lastIdxArrival);
        HashMap<Arrival, Integer> pathIdx = new HashMap<>();
        HashMap<Arrival, Integer> segIdx = new HashMap<>();
        for (Arrival arrival : allArrival) {
            pathIdx.put(arrival, 0);
            segIdx.put(arrival, 0);
        }
        double timeVal = 0;
        boolean done=false;
        Map<Double, XYPlottingData> out = new HashMap<>();
        while( ! done) {
            done = true;
            timeVal += timeStep;
            if (verbose) {
                System.err.println("wavefront calc for " + timeVal);
            }
            List<ArrivalPathSegment> wavefrontSegments = new ArrayList<>();
            ArrivalPathSegment curWaveSeg = null;
            Arrival prevArrival = null;
            ArrivalPathSegment prevPathSeg = null;
            for (Arrival arrival : allArrival) {
                // what about shadow zone?
                if (arrival.getTime() >= timeVal) {
                    done = false;
                    List<ArrivalPathSegment> segPath = arrival.getPathSegments();
                    ArrivalPathSegment curPathSeg = segPath.get(segIdx.get(arrival));

                    if (prevArrival != null && prevArrival.getTime() < timeVal) {
                        // need to connect wavefront to receiver depth when on arriving edge of the wavefront
                        double distConnect = linearInterp(prevArrival.getTime(), prevArrival.getDist(),
                                arrival.getTime(), arrival.getDist(), timeVal);
                        double raypConnect = linearInterp(prevArrival.getTime(), prevArrival.getRayParam(),
                                arrival.getTime(), arrival.getRayParam(), timeVal);
                        TimeDist interp = new TimeDist(raypConnect, timeVal, distConnect, arrival.getReceiverDepth());
                        ArrivalPathSegment lastSeg = prevArrival.getPathSegments().get(prevArrival.getPathSegments().size()-1);
                        if (curWaveSeg == null || curWaveSeg.isPWave != lastSeg.isPWave) {
                            TimeDist prevEnd = curWaveSeg == null ? null : curWaveSeg.getPathEnd();
                            List<TimeDist> tdList = new ArrayList<>();
                            if (prevEnd != null) {
                                // connect segments by adding prev end to start of new
                                // kind of wrong as phase change happened somewhere in between, but better than a gap???
                                // maybe should check endaction of prev path segment to see if reflection???
                                tdList.add(prevEnd);
                            }
                            curWaveSeg = new ArrivalPathSegment(tdList, lastSeg.isPWave, lastSeg.segmentName, prevEnd, arrival, lastSeg.phaseSegment);
                            wavefrontSegments.add(curWaveSeg);
                        }
                        curWaveSeg.path.add(interp);
                    }
                    int i = pathIdx.get(arrival);
                    TimeDist prevTD = null;
                    while (curPathSeg != null) {
                        if (i == curPathSeg.path.size()) {
                            prevPathSeg = curPathSeg;
                            if (segIdx.get(arrival) < segPath.size()) {
                                i = 0;
                                pathIdx.put(arrival, 0);
                                segIdx.put(arrival, segIdx.get(arrival) + 1);
                                curPathSeg = segPath.get(segIdx.get(arrival));

                            } else {
                                // no more points in this arrival
                                curPathSeg = null;
                                break;
                            }
                        }
                        TimeDist currTD = curPathSeg.path.get(i);
                        pathIdx.put(arrival, i);// remember where we were in path
                        if (prevTD != null && prevTD.getTime() <= timeVal && timeVal <= currTD.getTime()) {
                            TimeDist interp = interp(prevTD, currTD, timeVal);
                            if (curWaveSeg == null || curWaveSeg.isPWave != curPathSeg.isPWave) {
                                // change is wavetype, end current wavefront
                                TimeDist prevEnd = curWaveSeg == null ? null : curWaveSeg.getPathEnd();
                                List<TimeDist> tdList = new ArrayList<>();
                                if (prevEnd != null) {
                                    // connect segments by adding prev end to start of new
                                    // kind of wrong as phase change happened somewhere in between, but better than a gap???
                                    tdList.add(prevEnd);
                                }
                                curWaveSeg = new ArrivalPathSegment(tdList, curPathSeg.isPWave, curPathSeg.segmentName, prevEnd, arrival, curPathSeg.phaseSegment);
                                wavefrontSegments.add(curWaveSeg);
                            }
                            curWaveSeg.path.add(interp);
                            break;
                        }
                        prevTD = currTD;
                        i++;
                    }
                } else {
                    // break in arrival crossing distance
                    if (prevPathSeg != null) {
                        curWaveSeg = null;
                    }
                }
                prevArrival = arrival;
            }
            List<XYSegment> xySegmentList = new ArrayList<>();
            for (ArrivalPathSegment waveSeg : wavefrontSegments) {
                double[] xVals = new double[waveSeg.path.size()];
                double[] yVals = new double[waveSeg.path.size()];
                int idx = 0;
                for (TimeDist td : waveSeg.path) {
                    xVals[idx] = td.getDistDeg();
                    yVals[idx] = td.getDepth();
                    idx++;
                }
                XYSegment seg = new XYSegment(xVals, yVals);
                String p_or_s = waveSeg.isPWave ? "pwave" : "swave";
                seg.cssClasses.add(p_or_s);
                seg.cssClasses.add(String.format("time_%05d", (int) timeVal));
                seg.cssClasses.add(SvgUtil.classForPhase(waveSeg.segmentName));
                xySegmentList.add(seg);
            }
            List<String> cssClasses = new ArrayList<>();
            cssClasses.add(String.format("time_%05d", (int) timeVal));
            cssClasses.add(SvgUtil.classForPhase(phase.getName()));
            XYPlottingData xyp = new XYPlottingData(xySegmentList, AxisType.degree.name(), ModelAxisType.depth.name(), phase.getName(), cssClasses);
            out.put(timeVal, xyp);
        }
        return out;
    }

     public Map<SeismicPhase, Map<Float, List<TimeDist>>> calcIsochron() throws TauModelException {
        modelArgs.depthCorrected();
        Map<SeismicPhase, Map<Float, List<TimeDist>>> resultOut = new HashMap<SeismicPhase, Map<Float, List<TimeDist>>>();
        SeismicPhase phase;
        List<SeismicPhase> phaseList = getSeismicPhases();
        for (int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
            phase = phaseList.get(phaseNum);
            if (verbose) {
                System.err.println("Work on " + phase.getName());
            }
            double minDist = phase.getMinDistanceDeg();
            double maxDist = phase.getMaxDistanceDeg();
            double deltaDist = (maxDist - minDist) / (numRays - 1);
            double degrees = minDist;
            List<Arrival> allArrival = new ArrayList<Arrival>();
            for (int r = 0; r < getNumRays(); r++) {
                degrees = minDist + r * deltaDist;
                List<Arrival> phaseArrivals = phase.calcTimeExactDistanceDeg(degrees);
                allArrival.addAll(phaseArrivals);
            }
            Map<Float, List<TimeDist>> out = new HashMap<Float, List<TimeDist>>();
            resultOut.put(phase, out);
            boolean done = false;
            float timeVal = 0;
            while (!done) {
                done = true;
                timeVal += timeStep;
                if (verbose) {
                    System.err.println("Time " + timeVal + " for " + phase.getName() + " " + allArrival.size());
                }
                Arrival prevArrival = null;
                for (Arrival arrival : allArrival) {
                    if (arrival.getTime() >= timeVal) {
                        TimeDist[] path = arrival.getPath();
                        if (prevArrival != null && prevArrival.getTime() < timeVal) {
                            // need to connect wavefront to receiver depth when on arriving edge of the wavefront
                            double distConnect = linearInterp(prevArrival.getTime(), prevArrival.getDist(),
                                    arrival.getTime(), arrival.getDist(), timeVal);
                            double raypConnect = linearInterp(prevArrival.getTime(), prevArrival.getRayParam(),
                                    arrival.getTime(), arrival.getRayParam(), timeVal);
                            TimeDist interp = new TimeDist(raypConnect, timeVal, distConnect, arrival.getReceiverDepth());
                            List<TimeDist> tdList = out.get(timeVal);
                            if (tdList == null) {
                                tdList = new ArrayList<TimeDist>();
                                out.put(timeVal, tdList);
                            }
                            tdList.add(interp);
                        }
                        for (int i = 0; i < path.length; i++) {
                            if (path[i].getTime() <= timeVal && i < path.length - 1 && timeVal < path[i + 1].getTime()) {
                                TimeDist interp = interp(path[i], path[i + 1], timeVal);
                                List<TimeDist> tdList = out.get(timeVal);
                                if (tdList == null) {
                                    tdList = new ArrayList<TimeDist>();
                                    out.put(timeVal, tdList);
                                }
                                tdList.add(interp);
                                done = false;
                                break;
                            }
                        }
                    }
                    prevArrival = arrival;
                }
            }
        }
        return resultOut;
    }

    TimeDist interp(TimeDist x, TimeDist y, double t) {
        // this is probably wrong...
        double distInterp = linearInterp(x.getTime(), x.getDistRadian(),
                y.getTime(), y.getDistRadian(), t);
        double depthInterp = linearInterp(x.getTime(), x.getDepth(),
                y.getTime(), y.getDepth(), t);
        return new TimeDist(x.getP(),
                            t,
                            distInterp,
                            depthInterp);
    }

    @CommandLine.Option(names = "--rays", description = "number of raypaths/distances to sample.")
    public void setNumRays(int numRays) {
        this.numRays = numRays;
    }

    public int getNumRays() {
        return numRays;
    }

    public float getTimeStep() {
        return timeStep;
    }

    public GraphicOutputTypeArgs getGraphicOutputTypeArgs() {
        return outputTypeArgs;
    }

    @CommandLine.Option(names = "--timestep",
    defaultValue = "100",
    description = "steps in time (seconds) for output")
    public void setTimeStep(float timeStep) {
        this.timeStep = timeStep;
    }

    public boolean isSeparateFilesByTime() {
        return separateFilesByTime;
    }

    @CommandLine.Option(names="--timefiles", description = "outputs each time into a separate .ps file within the gmt script.")
    public void setSeparateFilesByTime(boolean separateFilesByTime) {
        this.separateFilesByTime = separateFilesByTime;
    }

    public boolean isNegDistance() {
        return negDistance;
    }

    @CommandLine.Option(names="--negdist", description = "outputs negative distance as well so wavefronts are in both halves.")
    public void setNegDistance(boolean negDistance) {
        this.negDistance = negDistance;
    }


    @Override
    public void init() throws TauPException {
        super.init();
        setOutputFormat(outputTypeArgs.getOuputFormat());
    }

    @Override
    public void start() throws IOException, TauPException {

        if (doInteractive) {
            throw new RuntimeException("interactive wavefront not yet impl");
        } else {
            /* enough info given on cmd line, so just do one calc. */
            Map<Double, List<XYPlottingData>> isochronMap = calcIsochronNEW();
            String cssExtra = "";
            if (coloring.getColor() == ColorType.phase) {
                StringBuffer cssPhaseColors = SvgUtil.createPhaseColorCSS(Arrays.asList(getPhaseNames()));
                cssExtra += cssPhaseColors;
            } else if (coloring.getColor() == ColorType.wavetype) {
                StringBuffer cssWaveTypeColors = SvgUtil.createWaveTypeColorCSS();
                cssExtra += cssWaveTypeColors;
            } else {
                // autocolor? by time?
                double maxTime = 0;
                for (Double timeVal : isochronMap.keySet()) {
                    maxTime = Math.max(maxTime, timeVal);
                }
                StringBuffer cssTime = SvgUtil.createTimeStepColorCSS((int) timeStep, (float) maxTime);
                cssExtra += cssTime;
            }
            List<Double> sortedKeys = new ArrayList<>();
            sortedKeys.addAll(isochronMap.keySet());
            Collections.sort(sortedKeys);
            if (isSeparateFilesByTime()) {
                for (Double timeVal : sortedKeys) {
                    String timeStr = String.format("_%05.2f", timeVal);

                    File timeOutFile = new File(outputTypeArgs.getOutFileBase()+timeStr+"."+outputTypeArgs.getOutFileExtension());
                    PrintWriter timeWriter = new PrintWriter(new BufferedWriter(new FileWriter(timeOutFile)));
                    printIsochron(timeWriter, isochronMap.get(timeVal), cssExtra, isNegDistance());
                    timeWriter.close();
                }
            } else {
                List<XYPlottingData> allIsochrons = new ArrayList<>();
                for (Double timeVal : sortedKeys) {
                    allIsochrons.addAll(isochronMap.get(timeVal));
                }
                PrintWriter writer = outputTypeArgs.createWriter();
                printIsochron(writer, allIsochrons, cssExtra, isNegDistance());
                writer.close();
            }
        }
    }


    @Override
    public void destroy() throws TauPException {

    }

    /**
     * Allows TauP_Isochron to run as an application. Creates an instance of
     * TauP_Wavefront.
     *  
     * ToolRun.main should be used instead.
     */
    public static void main(String[] args) throws IOException {
        ToolRun.legacyRunTool(ToolRun.WAVEFRONT, args);
    }
}
