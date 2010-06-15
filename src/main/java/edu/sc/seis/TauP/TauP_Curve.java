/*
 * The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
 * Copyright (C) 1998-2000 University of South Carolina
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * The current version can be found at <A
 * HREF="www.seis.sc.edu">http://www.seis.sc.edu</A>
 * 
 * Bug reports and comments should be directed to H. Philip Crotwell,
 * crotwell@seis.sc.edu or Tom Owens, owens@seis.sc.edu
 * 
 */
package edu.sc.seis.TauP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.io.StreamTokenizer;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculates travel time curves at known slowness samples.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * 
 * 
 * 
 * @author H. Philip Crotwell
 * 
 */
public class TauP_Curve extends TauP_Time {

    /** should the output file be a compete script? */
    protected boolean gmtScript = false;

    /** should the output times use a reducing velocity? */
    protected boolean reduceTime = false;

    /**
     * the reducing velocity to use if reduceTime == true, in units of
     * radians/second .
     */
    protected double reduceVel = .125 * Math.PI / 180;

    protected TauP_Curve() {
        super();
    }

    public TauP_Curve(TauModel tMod) throws TauModelException {
        super(tMod);
    }

    public TauP_Curve(String modelName) throws TauModelException {
        super(modelName);
    }

    public boolean isGmtScript() {
        return gmtScript;
    }

    public void setGmtScript(boolean gmtScript) {
        this.gmtScript = gmtScript;
    }

    public boolean isReduceTime() {
        return reduceTime;
    }

    public void setReduceTime(boolean reduceTime) {
        this.reduceTime = reduceTime;
    }

    /**
     * @returns reducing velocity in degrees/second. The internal usage is
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
            this.reduceVel = Math.PI / 180.0 * reduceVel;
        }
    }

    /**
     * @returns reducing velocity in kilometers/second. The internal usage is
     *          radians/second.
     */
    public double getReduceVelKm() {
        return reduceVel * tMod.getRadiusOfEarth();
    }

    /**
     * set the reducing velocity, in kilometers/second. The internal
     * representation is radians/second.
     */
    public void setReduceVelKm(double reduceVel) {
        if(reduceVel > 0.0) {
            if(tMod != null) {
                this.reduceVel = reduceVel / tMod.getRadiusOfEarth();
            } else {
                this.reduceVel = reduceVel / 6371.0;
            }
        }
    }

    public void calculate(double degrees) {
        /*
         * no need to do any calculations, just check the phases since they have
         * already been done within the seismic phase. So, this just overrides
         * TauP_Time.calculate. printResult handles everything else.
         */
        recalcPhases();
    }

    public void init() throws IOException {
        super.init();
        if(gmtScript) {
            String psFile;
            if(outFile.endsWith(".gmt")) {
                psFile = outFile.substring(0, outFile.length() - 4) + ".ps";
            } else {
                psFile = outFile + ".ps";
            }
            dos.writeBytes("#!/bin/sh\n");
            dos.writeBytes("#\n# This script will plot curves using GMT. If you want to\n"
                    + "#use this as a data file for psxy in another script, delete these"
                    + "\n# first lines, as well as the last line.\n#\n");
            dos.writeBytes("/bin/rm -f " + psFile + "\n\n");
        }
    }

    public void printStdUsage() {
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1,
                                        className.length());
        System.out.println("Usage: " + className.toLowerCase() + " [arguments]");
        System.out.println("  or, for purists, java "
                + this.getClass().getName() + " [arguments]");
        System.out.println("\nArguments are:");
        System.out.println("-ph phase list     -- comma separated phase list\n"
                + "-pf phasefile      -- file containing phases\n\n"
                + "-mod[el] modelname -- use velocity model \"modelname\" for calculations\n"
                + "                      Default is iasp91.\n\n"
                + "-h depth           -- source depth in km\n\n");
    }

    public void printStdUsageTail() {
        System.out.println("\n-o outfile         -- output is redirected to \"outfile\" instead of taup_curve.gmt\n"
                + "-debug             -- enable debugging output\n"
                + "-verbose           -- enable verbose output\n"
                + "-version           -- print the version\n"
                + "-help              -- print this out, but you already know that!\n\n");
    }

    public void printUsage() {
        printStdUsage();
        System.out.println("-gmt               -- outputs curves as a complete GMT script.");
        System.out.println("-reddeg velocity   -- outputs curves with a reducing velocity (deg/sec).");
        System.out.println("-redkm velocity    -- outputs curves with a reducing velocity (km/sec).");
        System.out.println("-rel phasename     -- outputs relative travel time");
        printStdUsageTail();
    }

    public void start() throws IOException, TauModelException {
        double tempDepth;
        if(depth != -1 * Double.MAX_VALUE) {
            /* enough info given on cmd line, so just do one calc. */
            depthCorrect(Double.valueOf(toolProps.getProperty("taup.source.depth",
                                                              "0.0"))
                    .doubleValue());
            calculate(degrees);
            printResult(dos);
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
            depthCorrect(tempDepth);
            calculate(degrees);
            printResult(dos);
        }
    }

    public void destroy() throws IOException {
        if(gmtScript) {
            dos.writeBytes("END\n");
        }
        super.destroy();
    }

    public void printResult(Writer out) throws IOException {
        SeismicPhase phase;
        double[] dist, time, rayParams;
        double arcDistance, timeReduced;
        double maxTime = -1 * Double.MAX_VALUE, minTime = Double.MAX_VALUE;
        List<SeismicPhase> relPhases = new ArrayList<SeismicPhase>();
        if (relativePhaseName != "") {
            try {
                List<String> splitNames = getPhaseNames(relativePhaseName);
                for (String sName : splitNames) {
                    relPhases.add(new SeismicPhase(sName, tModDepth));
                }
            } catch(TauModelException e) {
                Alert.warning("Error with phase=" + relativePhaseName,
                              e.getMessage() + "\nSkipping relative phase");
            }
        }
        if(gmtScript) {
            String scriptStuff = "";
            String psFile;
            if(outFile.endsWith(".gmt")) {
                psFile = outFile.substring(0, outFile.length() - 4) + ".ps";
            } else {
                psFile = outFile + ".ps";
            }
            String title = modelName;
            if(reduceTime) {
                title += " reduce vel "+reduceVel;
            } else if (relativePhaseName != "") {
                title += " relative phase "+relativePhaseName;
            }
            for(int phaseNum = 0; phaseNum < phases.size(); phaseNum++) {
                phase = (SeismicPhase)phases.get(phaseNum);
                dist = phase.getDist();
                time = phase.getTime();
                int phaseMinIndex = 0;
                int phaseMaxIndex = 0;
                double phaseMaxTime = -1 * Double.MAX_VALUE;
                double phaseMinTime = Double.MAX_VALUE;
                if(dist.length > 0) {
                    // find max and min time
                    for(int i = 0; i < time.length; i++) {
                        double[] timeValue = calcTimeValue(dist[i], time[i], relPhases);
                        if (timeValue.length == 0) {continue;}
                        if(timeValue[0] > maxTime) {
                            maxTime = timeValue[0];
                        }
                        if(timeValue[0] < minTime) {
                            minTime = timeValue[0];
                        }
                        if(timeValue[0] > phaseMaxTime) {
                            phaseMaxTime = timeValue[0];
                            phaseMaxIndex = i;
                        }
                        if(timeValue[0] < phaseMinTime) {
                            phaseMinTime = timeValue[0];
                            phaseMinIndex = i;
                        }
                    }
                    arcDistance = Math.acos(Math.cos(dist[phaseMaxIndex]));
                    if(reduceTime || relativePhaseName != "") {
                        scriptStuff += (float)(180.0 / Math.PI * arcDistance)
                                + "  "
                                + (float)(phaseMaxTime) + " 10 0 0 9 "
                                + phase.getName() + "\n";
                    } else {
                        int lix = (dist[1] > Math.PI) ? 1 : dist.length - 1;
                        double ldel = 180.0 / Math.PI
                                * Math.acos(Math.cos(dist[lix]));
                        scriptStuff += (float)ldel + "  " + (float)time[lix]
                                + " 10 0 0 1 " + phase.getName() + "\n";
                    }
                }
            }
            // round max and min time to nearest 100 seconds
            maxTime = Math.ceil(maxTime / 100) * 100;
            minTime = Math.floor(minTime / 100) * 100;
            out.write("pstext -JX6 -P -R0/180/" + minTime + "/" + maxTime
                    + " -B20/100/:.'" + title + "': -K > " + psFile
                    + " <<END\n");
            out.write(scriptStuff);
            out.write("END\n\n");
            out.write("psxy -JX -R -M -O >> " + psFile + " <<END\n");
        }
        double minDist = 0;
        double maxDist = Math.PI;
        if(relativePhaseName != "") {
            for (SeismicPhase seismicPhase : relPhases) {
                double[] relDist = seismicPhase.getDist();
                if (relDist.length == 0) {
                    continue;
                }
                minDist = relDist[0];
                maxDist = relDist[0];
                for (int i = 0; i < relDist.length; i++) {
                    if (relDist[i] < minDist) {minDist = relDist[i];}
                    if (relDist[i] > maxDist) {maxDist = relDist[i];}
                }
            }
        }
        for(int phaseNum = 0; phaseNum < phases.size(); phaseNum++) {
            phase = phases.get(phaseNum);
            dist = phase.getDist();
            time = phase.getTime();
            rayParams = phase.getRayParams();
            double minPhaseDist = dist[0];
            double maxPhaseDist = dist[0];
            if(relativePhaseName != "") {
                for (int i = 0; i < dist.length; i++) {
                    if (dist[i] < minPhaseDist) {minDist = dist[i];}
                    if (dist[i] > maxPhaseDist) {maxDist = dist[i];}
                }
            }
            if(dist.length > 0) {
                out.write("> " + phase.getName() + " for a source depth of "
                        + depth + " kilometers in the " + modelName
                        + " model");
                if(relativePhaseName != "") {
                    out.write(" relative to "+relativePhaseName);
                }
                out.write("\n");
            }
            for(int i = 0; i < dist.length; i++) {
                /* Here we use a trig trick to make sure the dist is 0 to PI. */
                arcDistance = Math.acos(Math.cos(dist[i]));
                writeValue(arcDistance, time[i], relPhases, out);
                if(i < dist.length - 1 && (rayParams[i] == rayParams[i + 1])
                        && rayParams.length > 2) {
                    /* Here we have a shadow zone, so put a break in the curve. */
                    out.write("> Shadow Zone\n");
                    continue;
                }
                checkBoundary(0, i, phase, relPhases, out);
                checkBoundary(Math.PI, i, phase, relPhases, out);
                if (minDist != 0 && minDist != Math.PI) {
                    checkBoundary(minDist, i, phase, relPhases, out);
                }
                if (maxDist != 0 && maxDist != Math.PI) {
                    checkBoundary(maxDist, i, phase, relPhases, out);
                }
            }
        }
    }
    
    protected void checkBoundary(double boundaryDistRadian,
                                 int distIndex,
                                 SeismicPhase phase,
                                 List<SeismicPhase> relPhase,
                                 Writer out) throws IOException {
        double arcDistance = Math.acos(Math.cos(boundaryDistRadian));
        if (distIndex < phase.getDist().length-1 && 
                (isBetween(Math.acos(Math.cos(phase.getDist()[distIndex])),
                           Math.acos(Math.cos(phase.getDist()[distIndex+1])),
                           arcDistance))) {
            List<Arrival> phaseArrivals = phase.calcTime(arcDistance*180/Math.PI);
            for (Arrival arrival : phaseArrivals) {
                if((phase.rayParams[distIndex] - arrival.getRayParam())
                        * (arrival.getRayParam() - phase.rayParams[distIndex + 1]) > 0) {
                    if(reduceTime) {
                        writeValue(arcDistance, arrival.getTime() - arcDistance
                                / reduceVel, relPhase, out);
                    } else {
                        writeValue(arcDistance, arrival.getTime(), relPhase, out);
                    }
                    break;
                }
            }
        }
    }
    
    protected double[] calcTimeValue(double distRadian, double time, List<SeismicPhase> relPhase) throws IOException {
        double timeReduced = time;
        double distDeg = distRadian*180/Math.PI;
        if(reduceTime) {
            timeReduced = time - distRadian / reduceVel;
        } else if(relativePhaseName != "") {
            relativeArrival = SeismicPhase.getEarliestArrival(relPhase, distDeg);
            if (relativeArrival == null) {
                // no relative arrival at this dist, skip
                return new double[0];
            }
            timeReduced = time - relativeArrival.getTime();
        } else {
            timeReduced = time;
        }
        return new double[] { timeReduced };
    }
    
    public void writeValue(double distRadian, double time, List<SeismicPhase> relPhase, Writer out) throws IOException {
        double[] timeReduced = calcTimeValue(distRadian, time, relPhase);
        if (timeReduced.length == 0) {return; }
        double distDeg = distRadian*180/Math.PI;
        out.write(outForms.formatDistance(distDeg) + "  "
                + outForms.formatTime(timeReduced[0]) + "\n");
    }

    public static final boolean isBetween(double a, double b, double value) {
        return (a < value && value < b) || (a > value && value > b);
    }
    
    public String[] parseCmdLineArgs(String[] args) throws IOException {
        int i = 0;
        String[] leftOverArgs;
        int numNoComprendoArgs = 0;
        File tempFile;
        leftOverArgs = super.parseCmdLineArgs(args);
        String[] noComprendoArgs = new String[leftOverArgs.length];
        while(i < leftOverArgs.length) {
            if(leftOverArgs[i].equalsIgnoreCase("-gmt")) {
                gmtScript = true;
            } else if(leftOverArgs[i].equals("-reddeg")) {
                setReduceTime(true);
                setReduceVelDeg(Double.valueOf(leftOverArgs[i + 1])
                        .doubleValue());
                i++;
            } else if(leftOverArgs[i].equals("-redkm")) {
                setReduceTime(true);
                setReduceVelKm(Double.valueOf(leftOverArgs[i + 1])
                        .doubleValue());
                i++;
            } else if(leftOverArgs[i].equals("-help")) {
                noComprendoArgs[numNoComprendoArgs++] = leftOverArgs[i];
            } else {
                noComprendoArgs[numNoComprendoArgs++] = leftOverArgs[i];
            }
            i++;
        }
        if(numNoComprendoArgs > 0) {
            String[] temp = new String[numNoComprendoArgs];
            System.arraycopy(noComprendoArgs, 0, temp, 0, numNoComprendoArgs);
            return temp;
        } else {
            return new String[0];
        }
    }

    /**
     * Allows TauP_Curve to run as an application. Creates an instance of
     * TauP_Curve. .
     */
    public static void main(String[] args) throws FileNotFoundException,
            IOException, StreamCorruptedException, ClassNotFoundException,
            OptionalDataException {
        boolean doInteractive = true;
        try {
            TauP_Curve tauPCurve = new TauP_Curve();
            tauPCurve.outFile = "taup_curve.gmt";
            String[] noComprendoArgs = tauPCurve.parseCmdLineArgs(args);
            if(noComprendoArgs.length > 0) {
                for(int i = 0; i < noComprendoArgs.length; i++) {
                    if(noComprendoArgs[i].equals("-help")
                            || noComprendoArgs[i].equals("-version")) {
                        System.exit(0);
                    }
                }
                System.out.println("I don't understand the following arguments, continuing:");
                for(int i = 0; i < noComprendoArgs.length; i++) {
                    System.out.print(noComprendoArgs[i] + " ");
                    if(noComprendoArgs[i].equals("-help")) {
                        System.out.println();
                        System.exit(0);
                    }
                }
                System.out.println();
                noComprendoArgs = null;
            }
            for(int i = 0; i < args.length; i++) {
                if(args[i] == "-h") {
                    doInteractive = false;
                }
            }
            if(tauPCurve.DEBUG) {
                System.out.println("Done reading " + tauPCurve.modelName);
            }
            tauPCurve.init();
            if(doInteractive) {
                tauPCurve.start();
            } else {
                /* enough info given on cmd line, so just do one calc. */
                tauPCurve.depthCorrect(Double.valueOf(tauPCurve.toolProps.getProperty("taup.source.depth",
                                                                                      "0.0"))
                        .doubleValue());
                tauPCurve.calculate(tauPCurve.degrees);
                tauPCurve.printResult(tauPCurve.dos);
            }
            tauPCurve.destroy();
        } catch(TauModelException e) {
            System.out.println("Caught TauModelException: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
