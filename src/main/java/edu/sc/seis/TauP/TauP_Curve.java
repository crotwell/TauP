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
import java.io.PrintWriter;
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
    
    protected String redVelString = ".125 deg/s";

    protected float mapWidth = (float) 6.0;
    
    protected String mapWidthUnit = "i";

    protected TauP_Curve() {
        super();
        initFields();
    }

    public TauP_Curve(TauModel tMod) throws TauModelException {
        super(tMod);
        initFields();
    }

    public TauP_Curve(String modelName) throws TauModelException {
        super(modelName);
        initFields();
    }

    void initFields() {
        setOutFileBase("taup_curve");
        setOutputFormat(GMT);
    }

    @Override
    public String getOutFileExtension() {
        String extention = "gmt";
        if (outputFormat.equals(SVG)) {
            extention = "svg";
        }
        return extention;
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

    /**
     * Sets the gmt map width to be used with the output script and for creating
     * the circles for each discontinuity. Default is 6 inches.
     */
    public void setMapWidth(float mapWidth) {
        this.mapWidth = mapWidth;
    }

    /**
     * Gets the gmt map width to be used with the output script and for creating
     * the circles for each discontinuity.
     */
    public float getMapWidth() {
        return mapWidth;
    }
    
    public String getMapWidthUnit() {
        return mapWidthUnit;
    }
    
    public void setMapWidthUnit(String mapWidthUnit) {
        this.mapWidthUnit = mapWidthUnit;
    }

    public void calculate(double degrees) throws TauModelException {
        /*
         * no need to do any calculations. So, this just overrides
         * TauP_Time.calculate. printResult handles everything else.
         */
        depthCorrect(getSourceDepth(), getReceiverDepth());
    }
    

    public void printScriptBeginning(PrintWriter out)  throws IOException {
        if(gmtScript) {
            String psFile;
            if(getOutFile().endsWith(".gmt")) {
                psFile = getOutFile().substring(0, getOutFile().length() - 4) + ".ps";
            } else {
                psFile = getOutFile() + ".ps";
            }
            getWriter().println("#!/bin/sh");
            getWriter().println("#\n# This script will plot curves using GMT. If you want to\n"
                    + "#use this as a data file for psxy in another script, delete these"
                    + "\n# first lines, as well as the last line.\n#");
            getWriter().println("/bin/rm -f " + psFile + "\n");            
        } else if (outputFormat.equals(SVG)) {
            int plotOffset = 80;
            int plotSize = 600;
            out.println("<svg version=\"1.1\" baseProfile=\"full\" xmlns=\"http://www.w3.org/2000/svg\" width=\"500\" height=\"500\" viewBox=\"0 0 "+(plotSize)+" "+(plotSize)+"\">");
            
            out.println("<!--\n This script will travel time curves generated by TauP using SVG. -->");
            out.println("<defs>");
            out.println("    <style type=\"text/css\"><![CDATA[");
            out.println("        circle {");
            out.println("            vector-effect: non-scaling-stroke;");
            out.println("            stroke: grey;");
            out.println("            fill: transparent;");
            out.println("        }");
            out.println("        polyline {");
            out.println("            vector-effect: non-scaling-stroke;");
            out.println("            stroke: black;");
            out.println("            fill: transparent;");
            out.println("        }");
            out.println("        line {");
            out.println("            vector-effect: non-scaling-stroke;");
            out.println("            stroke: black;");
            out.println("            fill: transparent;");
            out.println("        }");
            out.println("        .xtick {");
            out.println("            text-anchor: middle;");
            out.println("            dominant-baseline: middle;");
            out.println("        }");
            out.println("        .ytick {");
            out.println("            text-anchor: end;");
            out.println("            dominant-baseline: middle;");
            out.println("        }");
            out.println("        .phaselabel {");
            out.println("            text-anchor: end;");
            out.println("            dominant-baseline: middle;");
            out.println("        }");
            out.println("    ]]></style>");
            out.println("</defs>");
            out.println("<g transform=\"translate("+plotOffset+","+plotOffset+")\" >");
            out.println("<!-- draw axis and label distances.-->");
            out.println();
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

    public void printUsage() {
        printStdUsage();
        System.out.println("--gmt              -- outputs curves as a complete GMT script.");
        System.out.println("--svg              -- outputs curves as a SVG image.");
        System.out.println("-reddeg velocity   -- outputs curves with a reducing velocity (deg/sec).");
        System.out.println("-redkm velocity    -- outputs curves with a reducing velocity (km/sec).");
        System.out.println("-rel phasename     -- outputs relative travel time");
        System.out.println("--mapwidth width   -- sets map width for GMT script.");
        printStdUsageTail();
    }

    public void start() throws IOException, TauModelException {
        double tempDepth;
        if(depth != -1 * Double.MAX_VALUE) {
            /* enough info given on cmd line, so just do one calc. */
            setSourceDepth(Double.valueOf(toolProps.getProperty("taup.source.depth",
                                                              "0.0"))
                    .doubleValue());
            calculate(degrees);
            printResult(getWriter());
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
            setSourceDepth(tempDepth);
            calculate(degrees);
            printResult(getWriter());
        }
    }

    public void destroy() throws TauPException {
        if(gmtScript && writer != null) {
            writer.close();
        }
        super.destroy();
    }

    @Override
    public void printResult(PrintWriter out) throws IOException {
        SeismicPhase phase;
        double[] dist, time, rayParams;
        double arcDistance;
        double maxTime = -1 * Double.MAX_VALUE, minTime = Double.MAX_VALUE;
        List<SeismicPhase> relPhases = new ArrayList<SeismicPhase>();
        if (relativePhaseName != "") {
            try {
                List<String> splitNames = getPhaseNames(relativePhaseName);
                for (String sName : splitNames) {
                    relPhases.add(new SeismicPhase(sName, getTauModelDepthCorrected()));
                }
            } catch(TauModelException e) {
                Alert.warning("Error with phase=" + relativePhaseName,
                              e.getMessage() + "\nSkipping relative phase");
            }
        }
        List<SeismicPhase> phaseList = getSeismicPhases();
        String psFile = null;

        if(gmtScript || outputFormat.equals(SVG)) {
            String scriptStuff = "";
            if(getOutFile().endsWith(".gmt")) {
                psFile = getOutFile().substring(0, getOutFile().length() - 4) + ".ps";
            } else {
                psFile = getOutFile() + ".ps";
            }
            String title = modelName+" (h="+getSourceDepth()+" km)";
            if(reduceTime) {
                title += " reduce vel "+redVelString;
            } else if (relativePhaseName != "") {
                title += " relative phase "+relativePhaseName;
            }
            for(int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                phase = (SeismicPhase)phaseList.get(phaseNum);
                if(phase.hasArrivals()) {
                    dist = phase.getDist();
                    time = phase.getTime();
                    int phaseMinIndex = 0;
                    int phaseMaxIndex = 0;
                    double phaseMaxTime = -1 * Double.MAX_VALUE;
                    double phaseMinTime = Double.MAX_VALUE;
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
                    int midSample = dist.length / 2 ;
                    arcDistance = Math.acos(Math.cos(dist[midSample]));
                    if(reduceTime || relativePhaseName != "") {
                        double[] timeValue = calcTimeValue(dist[midSample], time[midSample], relPhases);
                        double labelTime;
                        if (timeValue.length == 0) {
                            // can't use mid point, due to rel phase, use max from above
                            arcDistance = Math.acos(Math.cos(dist[phaseMaxIndex]));
                            labelTime = phaseMaxTime;
                        } else {
                            labelTime = timeValue[0];
                        }
                        scriptStuff += (float)(180.0 / Math.PI * arcDistance)
                                + "  "
                                + (float)(phaseMaxTime) + " 10 0 0 9 "
                                + phase.getName() + "\n";
                    } else {
                        int lix = (dist[1] > Math.PI) ? 1 : dist.length - 1;
                        double ldel = 180.0 / Math.PI
                                * Math.acos(Math.cos(dist[midSample]));
                        scriptStuff += (float)ldel + "  " + (float)time[midSample]
                                + " 10 0 0 1 " + phase.getName() + "\n";
                    }
                }
            }
            // round max and min time to nearest 100 seconds
            maxTime = Math.ceil(maxTime / 100) * 100;
            minTime = Math.floor(minTime / 100) * 100;
            if (outputFormat.equals(GMT)) {
                out.println("gmt psbasemap -JX" + getMapWidth() + getMapWidthUnit() + " -P -R0/180/" + minTime + "/" + maxTime
                        + " -Bxa20+l'Distance (deg)' -Bya100+l'Time (sec)' -BWSne+t'" + title + "' -K > " + psFile);
                out.println("gmt pstext -JX -P -R  -O -K >> " + psFile + " <<END");
                out.print(scriptStuff);
                out.println("END\n");
                out.println("gmt psxy -JX -R -m -O -K >> " + psFile + " <<END");
            } else if (outputFormat.equals(SVG)) {
                float margin = 10;
                float tick_length = 10;
                float text_height = 12; // guess text font height to shift x-axis tick labels
                float pixelWidth =  (72.0f*getMapWidth());
                out.println("<text x=\""+(pixelWidth/2)+"\" y=\""+(10)+"\">"+title+"</text>");
                out.println("<g> <!-- scale bar -->");
                out.println("<line x1=\"0\" y1=\"0\" x2=\"0\" y2=\""+pixelWidth+"\" />");
                ArrayList<Double> yticks = PlotTicks.getTicks(0, maxTime,10);
                for (double tick: yticks) {
                    // Y axis
                    double tick_pixel = tick/maxTime*pixelWidth;
                    String tick_text = ""+tick;
                    out.println("<text class=\"ytick\" x=\""+(-1*tick_length-2)+"\" y=\""+(pixelWidth-tick_pixel)+"\">"+tick_text+"</text>");
                    out.println("<line x1=\"0\" y1=\""+(pixelWidth-tick_pixel)+"\" x2=\"-"+tick_length+"\" y2=\""+(pixelWidth-tick_pixel)+"\" />");
                }
                out.println("<line x1=\"0\" y1=\""+pixelWidth+"\" x2=\""+(pixelWidth)+"\" y2=\""+pixelWidth+"\" />");
                ArrayList<Double> xticks = PlotTicks.getTicks(0, 180,9);
                for (double tick: xticks) {
                    // X axis
                    double tick_pixel = tick/180*pixelWidth;
                    String tick_text = ""+tick;
                    out.println("<text class=\"xtick\" x=\""+tick_pixel+"\" y=\""+(text_height+margin+pixelWidth)+"\">"+tick_text+"</text>");
                    out.println("<line x1=\""+tick_pixel+"\" y1=\""+pixelWidth+"\" x2=\""+tick_pixel+"\" y2=\""+(pixelWidth+tick_length)+"\" />");
                }
                out.println("</g> <!-- scale bar -->");
                // phase labels

                out.println("<!-- phase name labels -->");
                for(int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                    phase = phaseList.get(phaseNum);
                    if (phase.hasArrivals()) {
                        dist = phase.getDist();
                        time = phase.getTime();
                        double[] cval = new double[0];
                        for (int i = 0; i < 10; i++) {
                            // try up to 10 times to create a label position,
                            // can fail due to relPhase not existing
                            int randIdx = (int)(Math.random()*dist.length);
                            cval = calcPlotValue(dist[randIdx], time[randIdx], relPhases);
                            if (cval.length != 0) {
                                break;
                            }
                        }
                        double xPos = cval[0]*pixelWidth/180;
                        double yPos = pixelWidth-cval[1]*pixelWidth/maxTime;
                        out.println("<text class=\"phaselabel\" x=\""+xPos+"\" y=\""+yPos+"\">"+phase.getName()+"</text>");

                    }
                }
                out.println("<g transform=\"scale(1,-1) translate(0, -"+pixelWidth+")\">");
                out.println("<g transform=\"scale("+(pixelWidth/180)+","+(pixelWidth/maxTime)+")\" >");
            }
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
        for(int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
            phase = phaseList.get(phaseNum);
            if(phase.hasArrivals()) {
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
                    String commentLine = phase.getName() + " for a source depth of "
                            + depth + " kilometers in the " + modelName
                            + " model";
                    if (relativePhaseName != "") {
                        commentLine += " relative to " + relativePhaseName;
                    }
                    if (outputFormat.equals(GMT)) {
                        out.println("> " + commentLine);
                    } else if (outputFormat.equals(SVG)) {
                        out.println("<!-- "+commentLine);
                        out.println(" -->");
                        out.print("<polyline points=\"");
                    }
                }
                for(int i = 0; i < dist.length; i++) {
                    writeValue(dist[i], time[i], relPhases, out);
                    if(i < dist.length - 1 && (rayParams[i] == rayParams[i + 1])
                            && rayParams.length > 2) {
                        /* Here we have a shadow zone, so put a break in the curve. */
                        if (outputFormat.equals(GMT)) {
                            out.println("> Shadow Zone");
                        } else if (outputFormat.equals(SVG)) {
                            out.println("\" />");
                            out.println("<!-- Shadow Zone -->");
                            out.print("<polyline points=\"");
                        }
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
                if (outputFormat.equals(SVG)) {
                    // end polyline
                    out.println("\" />");
                }
            } else {
                if (verbose) {
                    System.out.println("Phase "+phase.getName()+" does not exist in "+phase.getTauModel().getModelName()+" for depth "+phase.getTauModel().getSourceDepth());
                }
            }
        }
        if (isGmtScript()) {
            out.println("END");
            endGmtAndCleanUp(out, psFile, "X");
        } else if (outputFormat.equals(SVG)) {
            out.println("</g>");
            out.println("</g>");
            out.println("</g>");
            out.println("</svg>");
        }
        out.flush();
    }

    protected void checkBoundary(double boundaryDistRadian,
                                 int distIndex,
                                 SeismicPhase phase,
                                 List<SeismicPhase> relPhase,
                                 PrintWriter out) throws IOException {
        double arcDistance = Math.acos(Math.cos(boundaryDistRadian));
        double distCheck = boundaryDistRadian;
        if (distIndex < phase.getDist().length-1) {
            while (distCheck < phase.getMaxDistance()) {
                if ((phase.getDist()[distIndex] < distCheck && distCheck < phase.getDist()[distIndex + 1])
                        || (phase.getDist()[distIndex + 1] < distCheck && distCheck < phase.getDist()[distIndex])) {
                    List<Arrival> phaseArrivals = phase.calcTime(arcDistance * 180 / Math.PI);
                    // find arrival with ray param between original two rays, write it out
                    for (Arrival arrival : phaseArrivals) {
                        // can be equal in case of Pdiff where rayparam is const
                        if ((phase.rayParams[distIndex] - arrival.getRayParam())
                                * (arrival.getRayParam() - phase.rayParams[distIndex + 1]) >= 0) {
                            writeValue(arcDistance, arrival.getTime(), relPhase, out);
                            break;
                        }
                    }
                }
                distCheck += 2 * Math.PI;
            }
        }
    }
    
    protected double[] calcTimeValue(double distRadian, double time, List<SeismicPhase> relPhase) throws IOException {
        double timeReduced = time;
        /* Here we use a trig trick to make sure the dist is 0 to PI. */
        double arcDistance = Math.acos(Math.cos(distRadian));
        double distDeg = arcDistance*180/Math.PI;
        if(reduceTime) {
            timeReduced = time - arcDistance / reduceVel;
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
    
    public double[] calcPlotValue(double distRadian, double time, List<SeismicPhase> relPhase) throws IOException {
        double[] timeReduced = calcTimeValue(distRadian, time, relPhase);
        if (timeReduced.length == 0) {
            return timeReduced;
        }
        double arcDistance = Math.acos(Math.cos(distRadian));
        double distDeg = arcDistance * 180 / Math.PI;
        return new double[]{distDeg, timeReduced[0]};
    }
    public void writeValue(double distRadian, double time, List<SeismicPhase> relPhase, PrintWriter out) throws IOException {
        double[] cval = calcPlotValue(distRadian, time, relPhase);
        if (cval.length == 2) {
            double distDeg = cval[0];
            double timeReduced = cval[1];
            if (outputFormat.equals(SVG)) {
                out.print(Outputs.formatDistanceNoPad(distDeg) + "  "
                        + Outputs.formatTimeNoPad(timeReduced) + " ");
            } else {
                out.println(Outputs.formatDistance(distDeg) + "  "
                        + Outputs.formatTime(timeReduced));
            }
        }
    }

    public static final boolean isBetween(double a, double b, double value) {
        return (a < value && value < b) || (a > value && value > b);
    }
    
    public String[] parseCmdLineArgs(String[] args) throws IOException {
        int i = 0;
        String[] leftOverArgs;
        int numNoComprendoArgs = 0;
        leftOverArgs = super.parseCmdLineArgs(args);
        String[] noComprendoArgs = new String[leftOverArgs.length];
        while(i < leftOverArgs.length) {
            if(dashEquals("gmt", leftOverArgs[i])) {
                gmtScript = true;
                outputFormat = GMT;
            } else if (dashEquals("svg", leftOverArgs[i])) {
                outputFormat = SVG;
            } else if(dashEquals("reddeg", leftOverArgs[i]) && i < leftOverArgs.length - 1) {
                setReduceTime(true);
                setReduceVelDeg(Double.valueOf(leftOverArgs[i + 1])
                        .doubleValue());
                i++;
            } else if(dashEquals("redkm", leftOverArgs[i]) && i < leftOverArgs.length - 1) {
                setReduceTime(true);
                setReduceVelKm(Double.valueOf(leftOverArgs[i + 1])
                        .doubleValue());
                i++;
            } else if(dashEquals("mapwidth", leftOverArgs[i]) && i < leftOverArgs.length - 1) {
                setMapWidth(Float.parseFloat(leftOverArgs[i + 1]));
                i++;
            } else if(dashEquals("help", leftOverArgs[i])) {
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
     * TauP_Curve. 
     * ToolRun.main should be used instead.
     */
    public static void main(String[] args) throws IOException {
        ToolRun.legacyRunTool(ToolRun.CURVE, args);
    }
}
