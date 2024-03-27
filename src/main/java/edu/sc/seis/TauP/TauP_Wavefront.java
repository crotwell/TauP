package edu.sc.seis.TauP;

import edu.sc.seis.TauP.CLI.GraphicOutputTypeArgs;
import edu.sc.seis.TauP.CLI.OutputTypes;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static edu.sc.seis.TauP.CLI.OutputTypes.GMT;
import static edu.sc.seis.TauP.CLI.OutputTypes.SVG;

/**
 * Plots of wavefronts, distance along the ray at points in time.
 */
@CommandLine.Command(name = "wavefront")
public class TauP_Wavefront extends TauP_AbstractPhaseTool {

    int numRays = 30;

    float timeStep = 100;

    boolean separateFilesByTime = false;

    boolean negDistance = false;

    boolean doInteractive = false;

    @CommandLine.Mixin
    GraphicOutputTypeArgs outputTypeArgs = new GraphicOutputTypeArgs();

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOuputFormat();
    }
    Map<SeismicPhase, Map<Float, List<TimeDist>>> result;

    
    
    public TauP_Wavefront() {
        super();
        setOutFileBase("taup_wavefront");
        setDefaultOutputFormat();
    }

    @Override
    public String[] allowedOutputFormats() {
        return new String[] {SVG, GMT};
    }

    @Override
    public void setDefaultOutputFormat() {
        outputTypeArgs.setOutputType(SVG);
    }

    public TauP_Wavefront(String modelName, String outFileBase) throws TauModelException {
        setModelName(modelName);
        setOutFileBase(outFileBase);
        setDefaultOutputFormat();
    }

    public TauP_Wavefront(String modelName) throws TauModelException {
        setModelName(modelName);
        setOutFileBase("taup_wavefront");
        setDefaultOutputFormat();
    }

    public TauP_Wavefront(TauModel tMod, String outFileBase) throws TauModelException {
        setTauModel(tMod);
        setOutFileBase(outFileBase);
        setDefaultOutputFormat();
    }

    public TauP_Wavefront(TauModel tMod) throws TauModelException {
        setTauModel(tMod);
        setOutFileBase("taup_wavefront");
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
            if (getOutFile().equals("stdout")) {
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
            float zoomScale = 1;
            float zoomTranslateX = 0;
            float zoomTranslateY = 0;
            double minDist = 0;
            double maxDist = Math.PI;


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

            TauP_Path.printModelAsSVG(out, tModDepth, minDist, maxDist, plotScale, plotSize, zoomScale, zoomTranslateX, zoomTranslateY);
            printResultSVG(out);
        } else {
            String psFile;
            if (getOutFileBase().equals("stdout")) {
                psFile = "taup_path.ps";
            } else if (getOutFile().endsWith(".gmt")) {
                psFile = getOutFile().substring(0, getOutFile().length() - 4) + ".ps";
            } else {
                psFile = getOutFile() + ".ps";
            }
            TauP_Path.printScriptBeginning(out, psFile, tModDepth, outputTypeArgs.mapwidth, outputTypeArgs.mapWidthUnit);
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
                String timeOutName = getOutFileBase()+timeExt;
                if (getOutFile().endsWith(".gmt")) {
                    timeOutName = getOutFile().substring(0, getOutFile().length() - 4)+timeExt + ".gmt";
                }
                if (timeOut != null && timeOut != out) {timeOut.close();}
                timeOut = new PrintWriter(new BufferedWriter(new FileWriter(timeOutName)));
                if (outputTypeArgs.gmtScript) {
                    TauP_Path.printScriptBeginning(timeOut, byTimePsFile, modelArgs.depthCorrected(), outputTypeArgs.mapwidth, outputTypeArgs.mapWidthUnit);
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
            timeOut.println("<polyline class=\"wavefront "+phase.getName()+" "+timeStr+"\" points=\"");
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
                TauP_Path.printDistRadiusAsXY(timeOut, td.getDistDeg(), radiusOfEarth - td.getDepth());
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
                timeOut.println("<polyline class=\"wavefront "+phase.getName()+" "+timeStr+"\" points=\"");
            }
            for (TimeDist td : wavefront) {
                if (outputTypeArgs.isGMT()) {
                    timeOut.println(Outputs.formatDistance(-1*td.getDistDeg()) + "  "
                            + Outputs.formatDepth(radiusOfEarth - td.getDepth()) + " " + Outputs.formatTime(time) + " "
                            + Outputs.formatRayParam(td.getP()));
                } else if (outputTypeArgs.isSVG()) {
                    TauP_Path.printDistRadiusAsXY(timeOut, -1*td.getDistDeg(), radiusOfEarth - td.getDepth());
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

    public Map<SeismicPhase, Map<Float, List<TimeDist>>> calcIsochron() throws TauModelException {
        modelArgs.depthCorrected();
        Map<SeismicPhase, Map<Float, List<TimeDist>>> resultOut = new HashMap<SeismicPhase, Map<Float, List<TimeDist>>>();
        SeismicPhase phase;
        List<SeismicPhase> phaseList = getSeismicPhases();
        for (int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
            phase = phaseList.get(phaseNum);
            if (verbose) {
                System.out.println("Work on " + phase.getName());
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
                    System.out.println("Time " + timeVal + " for " + phase.getName() + " " + allArrival.size());
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

    TimeDist interp(TimeDist x, TimeDist y, float t) {
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
        // TODO Auto-generated method stub
        super.init();
    }

    @Override
    public void start() throws IOException, TauModelException, TauPException {

        if (doInteractive) {
            throw new RuntimeException("interactive wavefront not yet impl");
        } else {
            /* enough info given on cmd line, so just do one calc. */
            calculate(new ArrayList<DistanceRay>());
            printResult(getWriter());
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
