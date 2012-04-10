package edu.sc.seis.TauP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TauP_Wavefront extends TauP_Path {

    int numRays = 10;

    float timeStep = 100;

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
        System.out.println("--phasecolor     -- steps in time (seconds) for output.");
        printStdUsageTail();
    }
    
    @Override
    public void printResult(Writer out) throws IOException {
        double radiusOfEarth = tModDepth.getRadiusOfEarth();
        for (SeismicPhase phase : result.keySet()) {
            if (gmtScript) {
                out.write("psxy -P -R -K -O -JP -m -A >> " + psFile + " <<END\n");
            }
            List<Float> keys = new ArrayList<Float>();
            Map<Float, List<TimeDist>> phaseResult = result.get(phase);
            keys.addAll(phaseResult.keySet());
            Collections.sort(keys);
            for (Float time : keys) {
                out.write("> " + phase.getName() + " at " + time + " seconds\n");
                List<TimeDist> isochron = phaseResult.get(time);
                Collections.sort(isochron, new Comparator<TimeDist>() {
                    @Override
                    public int compare(TimeDist arg0, TimeDist arg1) {
                        return new Double(arg0.getP()).compareTo(arg1.getP());
                    }
                });
                for (TimeDist td : isochron) {
                    out.write(Outputs.formatDistance(td.getDistDeg()) + "  "
                            + Outputs.formatDepth(radiusOfEarth - td.getDepth()) + " " 
                            + Outputs.formatTime(time) + " " 
                            + Outputs.formatRayParam(td.getP())
                            + "\n");
                }
            }
            if (gmtScript) {
                out.write("END\n");
            }
        }
        if (gmtScript) {
            out.write("psxy -P -R -O -JP -m -A >> " + psFile + " <<END\n");
            out.write("END\n");
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
                System.out.println("Work on "+phase.getName());
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
                    System.out.println("Time "+timeVal+" for "+phase.getName()+" "+firstArrival.size());
                }
                for (Arrival arrival : firstArrival) {
                    TimeDist[] path = arrival.getPath();
                    for (int i = 0; i < path.length; i++) {
                        if (path[i].getTime() <= timeVal && i < path.length-1 && timeVal < path[i + 1].getTime()) {
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

    public String[] parseCmdLineArgs(String[] args) throws IOException {
        int i = 0;
        String[] leftOverArgs;
        int numNoComprendoArgs = 0;
        leftOverArgs = super.parseCmdLineArgs(args);
        String[] noComprendoArgs = new String[leftOverArgs.length];
        while (i < leftOverArgs.length) {
            if (dashEquals("gmt", leftOverArgs[i])) {
                gmtScript = true;
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
            TauP_Wavefront tauP_isochron = new TauP_Wavefront();
            tauP_isochron.outFile = "taup_isochron.gmt";
            String[] noComprendoArgs = tauP_isochron.parseCmdLineArgs(args);
            printNoComprendoArgs(noComprendoArgs);
            for (int i = 0; i < args.length; i++) {
                if ("-h".equals(args[i])) {
                    doInteractive = false;
                }
            }
            if (tauP_isochron.DEBUG) {
                System.out.println("Done reading " + tauP_isochron.modelName);
            }
            tauP_isochron.init();
            if (doInteractive) {
                tauP_isochron.start();
            } else {
                /* enough info given on cmd line, so just do one calc. */
                tauP_isochron.depthCorrect(Double.valueOf(tauP_isochron.toolProps.getProperty("taup.source.depth",
                                                                                              "0.0")).doubleValue());
                tauP_isochron.calculate(tauP_isochron.degrees);
                tauP_isochron.printResult(tauP_isochron.getWriter());
            }
            tauP_isochron.destroy();
        } catch(TauModelException e) {
            System.out.println("Caught TauModelException: " + e.getMessage());
            e.printStackTrace();
        } catch(TauPException e) {
            System.out.println("Caught TauPException: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
