package edu.sc.seis.TauP;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OptionalDataException;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
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

/**
 * Plots of wavefronts, distance along the ray at points in time.
 */
public class TauP_Wavefront extends TauP_Path {

    int numRays = 30;

    float timeStep = 100;

    boolean separateFilesByTime = false;

    boolean negDistance = false;

    boolean doInteractive = true;
    
    Map<SeismicPhase, Map<Float, List<TimeDist>>> result;

    
    
    public TauP_Wavefront() {
        super();
        setOutFileBase("taup_wavefront");
    }

    public TauP_Wavefront(String modelName, String outFileBase) throws TauModelException {
        super(modelName, outFileBase);
    }

    public TauP_Wavefront(String modelName) throws TauModelException {
        super(modelName);
        setOutFileBase("taup_wavefront");
    }

    public TauP_Wavefront(TauModel tMod, String outFileBase) throws TauModelException {
        super(tMod, outFileBase);
    }

    public TauP_Wavefront(TauModel tMod) throws TauModelException {
        super(tMod);
        setOutFileBase("taup_wavefront");
    }

    @Override
    public void calculate(double degrees) throws TauModelException {
        // ignore degrees as we need a suite of distances
        result = calcIsochron();
    }

    @Override
    public void printUsage() {
        printStdUsage();
        System.out.println("--gmt             -- outputs path as a complete GMT script.");
        //System.out.println("--svg             -- outputs path as a complete SVG file.");
        System.out.println("--mapwidth        -- sets map width for GMT script.");
        System.out.println("--rays  num      -- number of raypaths/distances to sample.");
        System.out.println("--timestep  num  -- steps in time (seconds) for output.");
        System.out.println("--timefiles      -- outputs each time into a separate .ps file within the gmt script.");
        System.out.println("--negdist        -- outputs negative distance as well so wavefronts are in both halves.");
        printStdUsageTail();
    }

    @Override
    public void printScriptBeginning(PrintWriter out) throws IOException {
        if (!gmtScript) {
            return;
        }
        if (getOutFile().equals("stdout")) {
            psFile = "taup_wavefront.ps";
        }
        super.printScriptBeginning(out);
    }

    @Override
    public void printResult(PrintWriter out) throws IOException {
        String byTimePsFile = psFile;
        double radiusOfEarth = getTauModelDepthCorrected().getRadiusOfEarth();
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
            if (separateFilesByTime) {
                String psFileBase = psFile;
                if (gmtScript && psFile.endsWith(".ps")) {
                    psFileBase = psFile.substring(0, psFile.length() - 3);
                }
                String timeExt = "_" + format.format(time);
                byTimePsFile = psFileBase + timeExt + ".ps";
                String timeOutName = getOutFileBase()+timeExt;
                if (getOutFile().endsWith(".gmt")) {
                    timeOutName = getOutFile().substring(0, getOutFile().length() - 4)+timeExt + ".gmt";
                }
                if (timeOut != null && timeOut != out) {timeOut.close();}
                timeOut = new PrintWriter(new BufferedWriter(new FileWriter(timeOutName)));
                if (gmtScript) {printScriptBeginning(timeOut, byTimePsFile);}
            }
            if (gmtScript) {
                timeOut.println("# timestep = " + time);
                timeOut.println("gmt psxy -P -R -K -O -Wblue -JP -m -A >> " + byTimePsFile + " <<END");
            }
            for (SeismicPhase phase : result.keySet()) {
                Map<Float, List<TimeDist>> phaseResult = result.get(phase);
                List<TimeDist> wavefront = phaseResult.get(time);
                if (wavefront == null || wavefront.size() == 0) {
                    continue;
                }
                timeOut.println("> " + phase.getName() + " at " + time + " seconds");
                Collections.sort(wavefront, new Comparator<TimeDist>() {

                    // @Override
                    public int compare(TimeDist arg0, TimeDist arg1) {
                        return Double.valueOf(arg0.getP()).compareTo(arg1.getP());
                    }
                });
                for (TimeDist td : wavefront) {
                    timeOut.println(Outputs.formatDistance(td.getDistDeg()) + "  "
                            + Outputs.formatDepth(radiusOfEarth - td.getDepth()) + " " + Outputs.formatTime(time) + " "
                            + Outputs.formatRayParam(td.getP()));
                }
                if (isNegDistance()) {
                    timeOut.write("> " + phase.getName() + " at " + time + " seconds (neg distance)\n");
                    for (TimeDist td : wavefront) {
                        timeOut.println(Outputs.formatDistance(-1*td.getDistDeg()) + "  "
                                + Outputs.formatDepth(radiusOfEarth - td.getDepth()) + " " + Outputs.formatTime(time) + " "
                                + Outputs.formatRayParam(td.getP()));
                    }
                }
            }
            if (gmtScript) {
                timeOut.println("END");
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
        if (gmtScript && ! separateFilesByTime) {
            out.println("# end postscript"); 
            out.println("gmt psxy -P -R -O -JP -m -A -T >> " + byTimePsFile);
            out.println("# convert ps to pdf, clean up .ps file"); 
            out.println("gmt psconvert -P -Tf  " + byTimePsFile+" && rm " + byTimePsFile);
            out.println("# clean up after gmt...");
            out.println("rm gmt.history");
        }
        timeOut.flush();
        out.flush();
    }

    public Map<SeismicPhase, Map<Float, List<TimeDist>>> calcIsochron() throws TauModelException {
        depthCorrect(getSourceDepth(), getReceiverDepth());
        Map<SeismicPhase, Map<Float, List<TimeDist>>> resultOut = new HashMap<SeismicPhase, Map<Float, List<TimeDist>>>();
        SeismicPhase phase;
        clearArrivals();
        List<SeismicPhase> phaseList = getSeismicPhases();
        for (int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
            phase = phaseList.get(phaseNum);
            if (verbose) {
                System.out.println("Work on " + phase.getName());
            }
            double minDist = phase.getMinDistanceDeg();
            double maxDist = phase.getMaxDistanceDeg();
            double deltaDist = (maxDist - minDist) / (numRays - 1);
            degrees = minDist;
            List<Arrival> allArrival = new ArrayList<Arrival>();
            for (int r = 0; r < getNumRays(); r++) {
                degrees = minDist + r * deltaDist;
                List<Arrival> phaseArrivals = phase.calcTime(degrees);
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
                for (Arrival arrival : allArrival) {
                    TimeDist[] path = arrival.getPath();
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
            }
        }
        return resultOut;
    }

    TimeDist interp(TimeDist x, TimeDist y, float t) {
        // this is probably wrong...
        return new TimeDist(x.getP(),
                            t,
                            Theta.linInterp(x.getTime(), y.getTime(), x.getDistRadian(), y.getDistRadian(), t),
                            Theta.linInterp(x.getTime(), y.getTime(), x.getDepth(), y.getDepth(), t));
    }

    public void setNumRays(int numRays) {
        this.numRays = numRays;
    }

    public int getNumRays() {
        return numRays;
    }

    public float getTimeStep() {
        return timeStep;
    }

    public void setTimeStep(float timeStep) {
        this.timeStep = timeStep;
    }

    public boolean isSeparateFilesByTime() {
        return separateFilesByTime;
    }

    public void setSeparateFilesByTime(boolean separateFilesByTime) {
        this.separateFilesByTime = separateFilesByTime;
    }

    public boolean isNegDistance() {
        return negDistance;
    }

    public void setNegDistance(boolean negDistance) {
        this.negDistance = negDistance;
    }

    public String[] parseCmdLineArgs(String[] args) throws IOException {
        int i = 0;
        String[] leftOverArgs;
        int numNoComprendoArgs = 0;

        for (int j = 0; j < args.length; j++) {
            // setting source depth is enough???
            if (dashEquals("h", args[j])) {
                doInteractive = false;
            }
        }
        
        leftOverArgs = super.parseCmdLineArgs(args);
        String[] noComprendoArgs = new String[leftOverArgs.length];
        while (i < leftOverArgs.length) {
            if (dashEquals("gmt", leftOverArgs[i])) {
                gmtScript = true;
            } else if (dashEquals("timefiles", leftOverArgs[i])) {
                separateFilesByTime = true;
            } else if (dashEquals("negdist", leftOverArgs[i])) {
                negDistance = true;
            } else if (dashEquals("rays", leftOverArgs[i]) && i < leftOverArgs.length - 1) {
                setNumRays(Integer.parseInt(leftOverArgs[i + 1]));
                i++;
            } else if (dashEquals("timestep", leftOverArgs[i]) && i < leftOverArgs.length - 1) {
                setTimeStep(Float.parseFloat(leftOverArgs[i + 1]));
                i++;
            } else if (dashEquals("mapwidth", leftOverArgs[i]) && i < leftOverArgs.length - 1) {
                setMapWidth(Float.parseFloat(leftOverArgs[i + 1]));
                i++;
            } else if (dashEquals("help", leftOverArgs[i])) {
                noComprendoArgs[numNoComprendoArgs++] = leftOverArgs[i];
            } else {
                noComprendoArgs[numNoComprendoArgs++] = leftOverArgs[i];
            }
            i++;
        }
        if (numNoComprendoArgs > 0) {
            String[] temp = new String[numNoComprendoArgs];
            System.arraycopy(noComprendoArgs, 0, temp, 0, numNoComprendoArgs);
            return temp;
        } else {
            return new String[0];
        }
    }

    @Override
    public void init() throws TauPException {
        // TODO Auto-generated method stub
        super.init();
    }

    @Override
    public void start() throws IOException, TauModelException, TauPException {

        if (doInteractive) {
            super.start();
        } else {
            /* enough info given on cmd line, so just do one calc. */
            calculate(degrees);
            printResult(getWriter());
        }
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
