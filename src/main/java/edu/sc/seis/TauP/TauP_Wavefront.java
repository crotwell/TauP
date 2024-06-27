package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.*;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;

import java.io.*;
import java.util.*;

import static edu.sc.seis.TauP.SvgUtil.createSurfaceWaveCSS;
import static edu.sc.seis.TauP.TauP_Tool.ABREV_SYNOPSIS;
import static edu.sc.seis.TauP.TauP_Tool.OPTIONS_HEADING;

/**
 * Plots of wavefronts, distance along the ray at points in time.
 */
@CommandLine.Command(name = "wavefront",
        description = "Plot wavefronts of seismic phases at steps in time.",
        optionListHeading = OPTIONS_HEADING,
        abbreviateSynopsis = ABREV_SYNOPSIS,
        usageHelpAutoWidth = true)
public class TauP_Wavefront extends TauP_AbstractPhaseTool {

    float timeStep = 100;

    boolean separateFilesByTime = false;

    boolean negDistance = false;

    boolean doInteractive = false;

    @CommandLine.Mixin
    ColoringArgs coloring = new ColoringArgs();

    @CommandLine.Option(names = "--legend", description = "create a legend")
    boolean isLegend = false;

    @CommandLine.Option(names = "--onlynameddiscon",
            description = "only draw circles on the plot for named discontinuities like moho, cmb, iocb but not 410")
    boolean onlyNamedDiscon = false;

    @CommandLine.Mixin
    DistDepthRange distDepthRangeArgs = new DistDepthRange();

    @CommandLine.Mixin
    GraphicOutputTypeArgs outputTypeArgs;


    @Override
    public String getOutFileExtension() {
        return outputTypeArgs.getOutFileExtension();
    }

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOutputFormat();
    }
    
    public TauP_Wavefront() {
        super(new GraphicOutputTypeArgs(OutputTypes.SVG, "taup_wavefront"));
        outputTypeArgs = (GraphicOutputTypeArgs)abstractOutputTypeArgs;
    }

    @Override
    public void validateArguments() throws TauModelException {

    }

    public void printIsochron(PrintWriter out, Map<Double, List<WavefrontPathSegment>> timeSegmentMap, String psFile) throws TauPException {
        List<Double> sortedKeys = new ArrayList<>(timeSegmentMap.keySet());
        Collections.sort(sortedKeys);
        if (getOutputFormat().equals(OutputTypes.JSON)) {
            JSONArray jsonArray = new JSONArray();
            for (Double timeVal : sortedKeys) {
                JSONObject timeObject = new JSONObject();
                jsonArray.put(timeObject);
                timeObject.put("time", timeVal);
                JSONArray wavefrontArray = new JSONArray();
                timeObject.put("wavefronts", wavefrontArray);
                for (WavefrontPathSegment seg : timeSegmentMap.get(timeVal)) {
                    JSONObject jsonObject = new JSONObject();
                    wavefrontArray.put(jsonObject);
                    jsonObject.put("time", seg.timeVal);
                    jsonObject.put("phase", seg.phase.getName());
                    jsonObject.put("model", seg.phase.getTauModel().getModelName());
                    jsonObject.put("sourcedepth", seg.phase.getSourceDepth());
                    jsonObject.put("receiverdepth", seg.phase.getReceiverDepth());
                    if (seg.getPhase() instanceof ScatteredSeismicPhase) {
                        ScatteredSeismicPhase scatPhase = (ScatteredSeismicPhase) seg.getPhase();
                        jsonObject.put("scatterdepth", (float) scatPhase.getScattererDepth());
                        jsonObject.put("scatterdistdeg", scatPhase.getScattererDistanceDeg());
                    }
                    jsonObject.put("pwave", seg.isPWave);
                    jsonObject.put("segment_idx", seg.segmentIndex);
                    jsonObject.put("segments", seg.asJSONObject());
                }
            }
            out.println(jsonArray.toString(2));
        } else if (getOutputFormat().equals(OutputTypes.SVG)) {
            List<PhaseName> phaseNameList = parsePhaseNameList();
            String cssExtra = "";
            cssExtra += createSurfaceWaveCSS(phaseNameList)+"\n";
            double maxTime = 0;
            if (coloring.getColor() == ColorType.phase) {
                StringBuffer cssPhaseColors = SvgUtil.createPhaseColorCSS(phaseNameList);
                cssExtra += cssPhaseColors;
            } else if (coloring.getColor() == ColorType.wavetype) {
                String cssWaveTypeColors = SvgUtil.createWaveTypeColorCSS();
                cssExtra += cssWaveTypeColors;
            } else {
                for (SeismicPhase phase : getSeismicPhases()) {
                    if (phase.getMaxTime() > maxTime) {
                        maxTime = phase.getMaxTime();
                    }
                }
                StringBuffer cssTimeColors = SvgUtil.createTimeStepColorCSS(timeStep, (float) maxTime);
                cssExtra += cssTimeColors;
                // autocolor?
            }
            float pixelWidth = getGraphicOutputTypeArgs().getPixelWidth();
            SvgEarthScaling scaleTrans = SvgEarth.calcEarthScaleTransForPhaseList(getSeismicPhases(), distDepthRangeArgs, isNegDistance());

            SvgEarth.printScriptBeginningSvg(out, modelArgs.getTauModel(), pixelWidth,
                    scaleTrans, toolNameFromClass(this.getClass()), cmdLineArgs, cssExtra);

            SvgEarth.printModelAsSVG(out, modelArgs.getTauModel(), pixelWidth, scaleTrans, onlyNamedDiscon);

            if (coloring.getColor() == ColorType.auto){
                SvgUtil.startAutocolorG(out);
            }
            for (Double timeVal : sortedKeys) {
                out.println("    <g>");
                out.println("      <desc>Time" + Outputs.formatTimeNoPad(timeVal) + "</desc>");
                for (WavefrontPathSegment segment : timeSegmentMap.get(timeVal)) {
                    segment.writeSVGCartesian(out);
                    if (isNegDistance()) {
                        segment.asNegativeDistance().writeSVGCartesian(out);
                    }
                }
                out.println("</g>");
            }
            if (coloring.getColor() == ColorType.auto) {
                SvgUtil.endAutocolorG(out);
            }
            SvgEarth.printSvgEndZoom(out);
            if (isLegend) {
                float xtrans = (int)(-1*pixelWidth*.05);
                float ytrans = (int) (pixelWidth*.05);
                if (coloring.getColor() == ColorType.phase) {
                    SvgUtil.createPhaseLegend(out, getSeismicPhases(), "" , xtrans, ytrans);
                } else if (coloring.getColor() == ColorType.wavetype) {
                    SvgUtil.createWavetypeLegend(out, xtrans, ytrans, false);
                } else {
                    SvgUtil.createTimeStepLegend(out, timeStep, maxTime, "autocolor", xtrans, ytrans );
                }
            }
            SvgEarth.printSvgEnd(out);
        } else {
            // text/gmt

            if (getGraphicOutputTypeArgs().isGMT()) {
                SvgEarth.printGmtScriptBeginning(out, psFile, modelArgs.depthCorrected(), outputTypeArgs.mapwidth,
                        outputTypeArgs.mapWidthUnit, onlyNamedDiscon);
                if (coloring.getColor() != ColorType.wavetype) {
                    out.write("gmt psxy -P -R -K -O -JP -m -A >> " + psFile + " <<END\n");
                }
            }

            boolean withTime = false;
            for (Double timeVal : sortedKeys) {
                for (WavefrontPathSegment segment : timeSegmentMap.get(timeVal)) {
                    if (coloring.getColor() == ColorType.wavetype) {
                        String colorArg = "-W"+(segment.isPWave?"blue":"red")+" ";
                        out.write("gmt psxy -P -R -K -O -JP "+colorArg+" -m -A >> " + psFile + " <<END\n");
                    }
                    segment.writeGMTText(out, distDepthRangeArgs, Outputs.distanceFormat, Outputs.depthFormat, withTime);
                    if (coloring.getColor() == ColorType.wavetype) {
                        out.println("END");
                    }
                }
            }
            if (getGraphicOutputTypeArgs().isGMT()) {
                if (coloring.getColor() != ColorType.wavetype) {
                    out.write("END\n");
                }
                out.println("# end postscript");
                out.println("gmt psxy -P -R -O -JP -m -A -T >> " + psFile);
                out.println("# convert ps to pdf, clean up .ps file");
                out.println("gmt psconvert -P -Tf  " + psFile+" && rm " + psFile);
                out.println("# clean up after gmt...");
                out.println("rm gmt.history");
                out.flush();
            }
        }
    }



    public Map<Double, List<WavefrontPathSegment>> calcIsochron() throws TauModelException {
        Map<Double, List<WavefrontPathSegment>> out = new HashMap<>();

        double maxTime = 0;
        for (SeismicPhase phase : getSeismicPhases()) {
            if ( ! phase.hasArrivals()) { continue;}
            maxTime = Math.max(maxTime, phase.getMaxTime());
            Map<Double, List<WavefrontPathSegment>> wavefrontPathSegments = calcIsochronSegmentsForPhase(phase, timeStep);
            for (Double timeVal : wavefrontPathSegments.keySet()) {
                if ( ! out.containsKey(timeVal)) {
                    out.put(timeVal, new ArrayList<>());
                }
                out.get(timeVal).addAll(wavefrontPathSegments.get(timeVal));
            }
        }
        return out;
    }

    public Map<Double, List<WavefrontPathSegment>> calcIsochronSegmentsForPhase(SeismicPhase phase, double timeStep) {
        int totalNumSegments = (int) Math.floor(phase.getMaxTime()/timeStep);
        int waveSegIdx = 0;
        List<Arrival> allArrival = new ArrayList<>();
        for (int i=0; i<phase.getNumRays(); i++) {
            allArrival.add(phase.createArrivalAtIndex(i));
        }
        HashMap<Arrival, Integer> pathIdx = new HashMap<>();
        HashMap<Arrival, Integer> segIdx = new HashMap<>();
        HashMap<Arrival, TimeDist> prevTimeDistMap = new HashMap<>();
        for (Arrival arrival : allArrival) {
            pathIdx.put(arrival, 0);
            segIdx.put(arrival, 0);
            prevTimeDistMap.put(arrival, arrival.getSourceTimeDist());
        }
        Map<Double, List<WavefrontPathSegment>> out = new HashMap<>();
        double timeVal = 0;
        boolean done=false;
        while( ! done) {
            done = true;
            timeVal += timeStep;
            if (isVerbose()) {
                System.err.println("wavefront calc for " + timeVal);
            }
            List<WavefrontPathSegment> wavefrontSegments = new ArrayList<>();
            out.put(timeVal, wavefrontSegments);
            WavefrontPathSegment curWaveSeg = null;

            if (phase.getName().endsWith("kmps")) {
                // surface wave, so make wavefront from 0 to 100 km???
                List<TimeDist> surfaceWaveTD = new ArrayList<>();
                double dist = timeVal / phase.getRayParams(0);
                surfaceWaveTD.add(new TimeDist(phase.getRayParams(0), timeVal, dist, 0));
                surfaceWaveTD.add(new TimeDist(phase.getRayParams(0), timeVal, dist, 100));
                WavefrontPathSegment seg = new WavefrontPathSegment(surfaceWaveTD, false, phase.getName(),
                        null, waveSegIdx, totalNumSegments, phase, timeVal);
                wavefrontSegments.add(seg);
                done = dist + timeStep / phase.getRayParams(0) > phase.getMaxDistance();
                continue;
            } else if (phase instanceof ScatteredSeismicPhase &&
                    ((ScatteredSeismicPhase) phase).getInboundArrival().getTime() > timeVal
            ) {
                Arrival inboundArrival = ((ScatteredSeismicPhase) phase).getInboundArrival();
                TimeDist prevTD = null;
                for (ArrivalPathSegment curPathSeg : inboundArrival.getPathSegments()) {
                    for (TimeDist currTD : curPathSeg.getPath()) {
                        if (prevTD != null && prevTD.getTime() <= timeVal && timeVal <= currTD.getTime()) {
                            TimeDist interp = interp(prevTD, currTD, timeVal);
                            double deltaDepth = phase.getTauModel().getRadiusOfEarth() * 0.01;
                            TimeDist shallowerTd = new TimeDist(
                                    interp.getP(),
                                    timeVal,
                                    interp.getDistRadian(),
                                    interp.getDepth() - deltaDepth
                            );
                            List<TimeDist> tdList = new ArrayList<>();
                            tdList.add(interp);
                            tdList.add(shallowerTd);
                            TimeDist prevEnd = null;
                            curWaveSeg = new WavefrontPathSegment(tdList, curPathSeg.isPWave, curPathSeg.segmentName,
                                    prevEnd, waveSegIdx++, totalNumSegments, phase, timeVal);
                            wavefrontSegments.add(curWaveSeg);
                            done = false;
                            prevTD = currTD;
                            break;
                        }
                        prevTD = currTD;
                    }
                    if (prevTD.getTime() > timeVal) {
                        break;
                    }
                }
                //set indexs to end of inbound segments for later normal wavefronts
                for (Arrival arrival : allArrival) {
                    segIdx.put(arrival, inboundArrival.getPathSegments().size());
                    pathIdx.put(arrival, 0);
                    prevTimeDistMap.put(arrival, inboundArrival.getPiercePoint(inboundArrival.getNumPiercePoints()-1));
                }
                continue;
            }

            Arrival prevArrival = null;
            ArrivalPathSegment prevPathSeg = null;
            for (Arrival arrival : allArrival) {
                // what about shadow zone?
                if (arrival.getTime() >= timeVal) {
                    done = false;
                    List<ArrivalPathSegment> segPath = arrival.getPathSegments();
                    ArrivalPathSegment curPathSeg = segPath.get(segIdx.get(arrival));
                    TimeDist prevTD = prevTimeDistMap.get(arrival);

                    if (prevArrival != null && prevArrival.getTime() < timeVal) {
                        // need to connect wavefront to receiver depth when on arriving edge of the wavefront
                        double distConnect = linearInterp(prevArrival.getTime(), prevArrival.getDist(),
                                arrival.getTime(), arrival.getDist(), timeVal);
                        double raypConnect = linearInterp(prevArrival.getTime(), prevArrival.getRayParam(),
                                arrival.getTime(), arrival.getRayParam(), timeVal);
                        TimeDist interp = new TimeDist(raypConnect, timeVal, distConnect, arrival.getReceiverDepth());
                        ArrivalPathSegment lastSeg = prevArrival.getPathSegments().get(prevArrival.getPathSegments().size() - 1);
                        if (curWaveSeg == null || curWaveSeg.isPWave != lastSeg.isPWave) {
                            TimeDist prevEnd = curWaveSeg == null ? null : curWaveSeg.getPathEnd();
                            List<TimeDist> tdList = new ArrayList<>();
                            if (prevEnd != null) {
                                // connect segments by adding prev end to start of new
                                // kind of wrong as phase change happened somewhere in between, but better than a gap???
                                // maybe should check endaction of prev path segment to see if reflection???
                                tdList.add(prevEnd);
                            }
                            curWaveSeg = new WavefrontPathSegment(tdList, lastSeg.isPWave, lastSeg.segmentName,
                                    prevEnd, waveSegIdx++, totalNumSegments, arrival.getPhase(), timeVal);
                            wavefrontSegments.add(curWaveSeg);
                        }
                        curWaveSeg.path.add(interp);
                    }
                    int i = pathIdx.get(arrival);
                    while (curPathSeg != null) {
                        if (i == curPathSeg.path.size()) {
                            prevPathSeg = curPathSeg;
                            if (segIdx.get(arrival) < segPath.size() - 1) {
                                i = 0;
                                pathIdx.put(arrival, 0);
                                segIdx.put(arrival, segIdx.get(arrival) + 1);
                                curPathSeg = segPath.get(segIdx.get(arrival));

                            } else {
                                // no more points in this arrival
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
                                curWaveSeg = new WavefrontPathSegment(tdList, curPathSeg.isPWave, curPathSeg.segmentName,
                                        prevEnd, waveSegIdx++, totalNumSegments, arrival.getPhase(), timeVal);
                                wavefrontSegments.add(curWaveSeg);
                            }
                            curWaveSeg.path.add(interp);
                            break;
                        }
                        prevTD = currTD;
                        prevTimeDistMap.put(arrival, currTD);
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
        }
        return out;
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

    public float getTimeStep() {
        return timeStep;
    }

    public GraphicOutputTypeArgs getGraphicOutputTypeArgs() {
        return outputTypeArgs;
    }

    @CommandLine.Option(names = "--timestep",
    defaultValue = "100",
    description = "steps in time (seconds) for output, default is ${DEFAULT-VALUE}")
    public void setTimeStep(float timeStep) {
        if (timeStep < 0) {
            throw new IllegalArgumentException("TimeStep must be positive: "+timeStep);
        }
        this.timeStep = timeStep;
    }

    public boolean isSeparateFilesByTime() {
        return separateFilesByTime;
    }

    @CommandLine.Option(names="--timefiles",
            description = "outputs each time into a separate .ps file within the gmt script.")
    public void setSeparateFilesByTime(boolean separateFilesByTime) {
        this.separateFilesByTime = separateFilesByTime;
    }

    public boolean isNegDistance() {
        return negDistance;
    }

    @CommandLine.Option(names="--negdist",
            description = "outputs negative distance as well so wavefronts are in both halves.")
    public void setNegDistance(boolean negDistance) {
        this.negDistance = negDistance;
    }


    @Override
    public void init() throws TauPException {
        super.init();
        setOutputFormat(outputTypeArgs.getOutputFormat());
    }

    @Override
    public void start() throws IOException, TauPException {

        if (doInteractive) {
            throw new RuntimeException("interactive wavefront not yet impl");
        } else {
            /* enough info given on cmd line, so just do one calc. */
            Map<Double, List<WavefrontPathSegment>> isochronMap = calcIsochron();
            List<Double> sortedKeys = new ArrayList<>(isochronMap.keySet());
            Collections.sort(sortedKeys);
            if (isSeparateFilesByTime()) {
                Double lastTime = sortedKeys.get(sortedKeys.size() - 1);
                int numDigits = 1;
                while (Math.pow(10, numDigits) < lastTime) {
                    numDigits++;
                }
                String formatStr = "%0"+numDigits+".0f";
                for (Double timeVal : sortedKeys) {
                    String timeStr = String.format("_%05.2f", timeVal);
                    Map<Double, List<WavefrontPathSegment>> singleTimeIsochronMap = new HashMap<>();
                    singleTimeIsochronMap.put(timeVal, isochronMap.get(timeVal));
                    File timeOutFile = new File(outputTypeArgs.getOutFileBase()+timeStr+"."+outputTypeArgs.getOutFileExtension());
                    PrintWriter timeWriter = new PrintWriter(new BufferedWriter(new FileWriter(timeOutFile)));
                    String psFileBase = outputTypeArgs.getPsFile();
                    if (outputTypeArgs.gmtScript && psFileBase.endsWith(".ps")) {
                        psFileBase = psFileBase.substring(0, psFileBase.length() - 3);
                    }
                    String timeExt = "_" + String.format(formatStr, timeVal);
                    String byTimePsFile = psFileBase + timeExt + ".ps";
                    printIsochron(timeWriter, singleTimeIsochronMap, byTimePsFile);
                    timeWriter.close();
                }
            } else {
                PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
                printIsochron(writer, isochronMap, outputTypeArgs.getPsFile());
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
