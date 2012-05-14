package edu.sc.seis.TauP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OptionalDataException;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class TauP_Wavefront extends TauP_Path {

    int numRays = 10;

    float timeStep = 100;

    boolean separateFilesByTime;

    Map<SeismicPhase, Map<Float, List<TimeDist>>> result;

    @Override
    public void calculate(double degrees) throws TauModelException {
        depthCorrect(getSourceDepth());
        recalcPhases();
        result = calcIsochron(degrees);
    }

    @Override
    public void printUsage() {
        printStdUsage();
        System.out.println("--rays  num      -- number of raypaths/distances to sample.");
        System.out.println("--timestep  num  -- steps in time (seconds) for output.");
        printStdUsageTail();
    }

    @Override
    public void printScriptBeginning(PrintWriter out) throws IOException {
        if (!gmtScript) {
            return;
        }
        if (outFile == null) {
            outFile = "taup_wavefront.gmt";
            psFile = "taup_wavefront.ps";
        }
        super.printScriptBeginning(out);
    }

    @Override
    public void printResult(PrintWriter out) throws IOException {
        String byTimePsFile = psFile;
        double radiusOfEarth = tModDepth.getRadiusOfEarth();
        HashSet<Float> keySet = new HashSet<Float>();
        for (SeismicPhase phase : result.keySet()) {
            Map<Float, List<TimeDist>> phaseResult = result.get(phase);
            keySet.addAll(phaseResult.keySet());
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
        DecimalFormat format = new DecimalFormat(formatStr);
        for (Float time : keys) {
            if (separateFilesByTime) {
                byTimePsFile = format.format(time) + "_" + psFile;
                printScriptBeginning(out, byTimePsFile);
            }
            if (gmtScript) {
                out.println("# timestep = " + time);
                out.println("psxy -P -R -K -O -JP -m -A >> " + byTimePsFile + " <<END");
            }
            for (SeismicPhase phase : result.keySet()) {
                Map<Float, List<TimeDist>> phaseResult = result.get(phase);
                List<TimeDist> wavefront = phaseResult.get(time);
                if (wavefront == null || wavefront.size() == 0) {continue;}
                out.write("> " + phase.getName() + " at " + time + " seconds\n");
                Collections.sort(wavefront, new Comparator<TimeDist>() {

                    // @Override
                    public int compare(TimeDist arg0, TimeDist arg1) {
                        return new Double(arg0.getP()).compareTo(arg1.getP());
                    }
                });
                for (TimeDist td : wavefront) {
                    out.println(Outputs.formatDistance(td.getDistDeg()) + "  "
                            + Outputs.formatDepth(radiusOfEarth - td.getDepth()) + " " + Outputs.formatTime(time) + " "
                            + Outputs.formatRayParam(td.getP()));
                }
            }
            if (gmtScript) {
                out.println("END");
                if (separateFilesByTime) {
                    out.println("psxy -P -R -O -JP -m -A >> " + byTimePsFile + " <<END");
                    out.println("END");
                }
            }
        }
    }

    public Map<SeismicPhase, Map<Float, List<TimeDist>>> calcIsochron(double degrees) {
        // ignore degrees as we need a suite of distances
        Map<SeismicPhase, Map<Float, List<TimeDist>>> resultOut = new HashMap<SeismicPhase, Map<Float, List<TimeDist>>>();
        SeismicPhase phase;
        clearArrivals();
        for (int phaseNum = 0; phaseNum < phases.size(); phaseNum++) {
            phase = phases.get(phaseNum);
            if (verbose) {
                System.out.println("Work on " + phase.getName());
            }
            double minDist = phase.getMinDistanceDeg();
            double maxDist = phase.getMaxDistanceDeg();
            double deltaDist = (maxDist - minDist) / (numRays - 1);
            degrees = minDist;
            List<Arrival> firstArrival = new ArrayList<Arrival>();
            for (int r = 0; r < getNumRays(); r++) {
                degrees = minDist + r * deltaDist;
                List<Arrival> phaseArrivals = phase.calcTime(degrees);
                firstArrival.addAll(phaseArrivals);
            }
            Map<Float, List<TimeDist>> out = new HashMap<Float, List<TimeDist>>();
            resultOut.put(phase, out);
            boolean done = false;
            float timeVal = 0;
            while (!done) {
                done = true;
                timeVal += timeStep;
                if (verbose) {
                    System.out.println("Time " + timeVal + " for " + phase.getName() + " " + firstArrival.size());
                }
                for (Arrival arrival : firstArrival) {
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
        return new TimeDist(x.getP(), t, Theta.linInterp(x.getTime(),
                                                         y.getTime(),
                                                         x.getDistRadian(),
                                                         y.getDistRadian(),
                                                         t), Theta.linInterp(x.getTime(),
                                                                             y.getTime(),
                                                                             x.getDepth(),
                                                                             y.getDepth(),
                                                                             t));
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

    public String[] parseCmdLineArgs(String[] args) throws IOException {
        int i = 0;
        String[] leftOverArgs;
        int numNoComprendoArgs = 0;
        leftOverArgs = super.parseCmdLineArgs(args);
        String[] noComprendoArgs = new String[leftOverArgs.length];
        while (i < leftOverArgs.length) {
            if (dashEquals("gmt", leftOverArgs[i])) {
                gmtScript = true;
            } else if (dashEquals("timefiles", leftOverArgs[i])) {
                    separateFilesByTime = true;
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

    /**
     * Allows TauP_Isochron to run as an application. Creates an instance of
     * TauP_Isochron. .
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, StreamCorruptedException,
            ClassNotFoundException, OptionalDataException {
        boolean doInteractive = true;
        try {
            TauP_Wavefront tauP_wavefront = new TauP_Wavefront();
            tauP_wavefront.outFile = "taup_wavefront.gmt";
            String[] noComprendoArgs = tauP_wavefront.parseCmdLineArgs(args);
            printNoComprendoArgs(noComprendoArgs);
            for (int i = 0; i < args.length; i++) {
                if (dashEquals("h", args[i])) {
                    doInteractive = false;
                }
            }
            if (tauP_wavefront.DEBUG) {
                System.out.println("Done reading " + tauP_wavefront.modelName);
            }
            tauP_wavefront.init();
            if (doInteractive) {
                tauP_wavefront.start();
            } else {
                /* enough info given on cmd line, so just do one calc. */
                tauP_wavefront.depthCorrect(Double.valueOf(tauP_wavefront.toolProps.getProperty("taup.source.depth",
                                                                                                "0.0")).doubleValue());
                tauP_wavefront.calculate(tauP_wavefront.degrees);
                tauP_wavefront.printResult(tauP_wavefront.getWriter());
            }
            tauP_wavefront.destroy();
        } catch(TauModelException e) {
            System.out.println("Caught TauModelException: " + e.getMessage());
            e.printStackTrace();
        } catch(TauPException e) {
            System.out.println("Caught TauPException: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
