/*
 * The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
 * Copyright (C) 1998-2000 University of South Carolina This program is free
 * software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place -
 * Suite 330, Boston, MA 02111-1307, USA. The current version can be found at <A
 * HREF="www.seis.sc.edu">http://www.seis.sc.edu </A> Bug reports and comments
 * should be directed to H. Philip Crotwell, crotwell@seis.sc.edu or Tom Owens,
 * owens@seis.sc.edu
 */
package edu.sc.seis.TauP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.io.Writer;

/**
 * Calculate travel paths for different phases using a linear interpolated ray
 * parameter between known slowness samples.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * @author H. Philip Crotwell
 */
public class TauP_Path extends TauP_Pierce {

    protected float mapWidth = (float)6.0;

    protected boolean gmtScript = false;

    protected double maxPathInc = 1.0;

    protected static Format float8_4 = new Format("%8.4f");

    protected TauP_Path() {
        super();
        outFile = null;
    }

    public TauP_Path(TauModel tMod) throws TauModelException {
        super(tMod);
        outFile = null;
    }

    public TauP_Path(String modelName) throws TauModelException {
        super(modelName);
        outFile = null;
    }

    public TauP_Path(TauModel tMod, String outFileBase)
            throws TauModelException {
        super(tMod);
        setOutFileBase(outFileBase);
    }

    public TauP_Path(String modelName, String outFileBase)
            throws TauModelException {
        super(modelName);
        setOutFileBase(outFileBase);
    }

    /** Sets the output file base, appending ".gmt" for the filename. */
    public void setOutFileBase(String outFileBase) {
        if(outFileBase != null && outFileBase.length() != 0) {
            outFile = outFileBase + ".gmt";
        } else {
            outFile = "taup_path.gmt";
        }
    }

    /**
     * Sets the gmt map width to be used with the output script and for creating
     * the circles for each discontinuity. Default is 6 inches.
     */
    public void setMapWidth() {
    }

    /**
     * Gets the gmt map width to be used with the output script and for creating
     * the circles for each discontinuity.
     */
    public float getMapWidth() {
        return mapWidth;
    }

    public boolean getGmtScript() {
        return gmtScript;
    }

    public void setGmtScript(boolean gmtScript) {
        this.gmtScript = gmtScript;
    }

    public double getMaxPathInc() {
        return maxPathInc;
    }

    public void setMaxPathInc(double maxPathInc) {
        this.maxPathInc = maxPathInc;
    }

    public void calculate(double degrees) throws TauModelException {
        depthCorrect(getSourceDepth());
        recalcPhases();
        clearArrivals();
        calcPath(degrees);
    }

    public void calcPath(double degrees) {
        this.degrees = degrees;
        SeismicPhase phase;
        Arrival[] phaseArrivals;
        double calcTime, calcDist;
        for(int phaseNum = 0; phaseNum < phases.size(); phaseNum++) {
            phase = (SeismicPhase)phases.elementAt(phaseNum);
            phase.setDEBUG(DEBUG);
            phase.calcTime(degrees);
            if(phase.hasArrivals()) {
                phase.calcPath(tModDepth);
                phaseArrivals = phase.getArrivals();
                for(int i = 0; i < phaseArrivals.length; i++) {
                    arrivals.addElement(phaseArrivals[i]);
                }
            }
        }
    }

    public void printResult(Writer out) throws IOException {
        double calcTime, calcDist, prevDepth, calcDepth;
        double lat, lon;
        Arrival currArrival;
        int interpNum, maxInterpNum;
        double interpInc;
        double radiusOfEarth = tModDepth.getRadiusOfEarth();
        boolean longWayRound;
        Format float12_5 = new Format("%12.5f");
        for(int i = 0; i < arrivals.size(); i++) {
            currArrival = (Arrival)arrivals.elementAt(i);
            out.write("> " + currArrival.name + " at "
                    + outForms.formatDistance(currArrival.getDistDeg())
                    + " degrees for a "
                    + outForms.formatDepth(currArrival.sourceDepth)
                    + " km deep source in the " + modelName + " model arriving at "
                    + outForms.formatTime(currArrival.getTime())
                    + " s with rayParam "+outForms.formatRayParam(Math.PI/180*currArrival.getRayParam())+" s/deg.\n");
            longWayRound = false;
            if((currArrival.dist * 180 / Math.PI) % 360 > 180) {
                longWayRound = true;
            }
            calcTime = 0.0;
            calcDist = 0.0;
            calcDepth = currArrival.sourceDepth;
            for(int j = 0; j < currArrival.path.length; j++) {
                calcTime = currArrival.path[j].time;
                calcDepth = currArrival.path[j].depth;
                prevDepth = calcDepth; // only used if interpolate
                calcDist = currArrival.path[j].dist * 180.0 / Math.PI;
                if(longWayRound && calcDist != 0.0) {
                    calcDist = -1.0 * calcDist;
                }
                out.write(outForms.formatDistance(calcDist) + "  "
                        + outForms.formatDepth(radiusOfEarth - calcDepth));
                if(!gmtScript) {
                    printLatLon(out, calcDist);
                }
                out.write("\n");
                if(j < currArrival.path.length - 1
                        && (currArrival.rayParam != 0.0 && 180.0
                                / Math.PI
                                * (currArrival.path[j + 1].dist - currArrival.path[j].dist) > maxPathInc)) {
                    // interpolate to steps of at most maxPathInc degrees for
                    // path
                    maxInterpNum = (int)Math.ceil((currArrival.path[j + 1].dist - currArrival.path[j].dist)
                            * 180.0 / Math.PI / maxPathInc);
                    for(interpNum = 1; interpNum < maxInterpNum; interpNum++) {
                        calcTime += (currArrival.path[j + 1].time - currArrival.path[j].time)
                                / maxInterpNum;
                        if(longWayRound) {
                            calcDist -= (currArrival.path[j + 1].dist - currArrival.path[j].dist)
                                    / maxInterpNum * 180.0 / Math.PI;
                        } else {
                            calcDist += (currArrival.path[j + 1].dist - currArrival.path[j].dist)
                                    / maxInterpNum * 180.0 / Math.PI;
                        }
                        calcDepth = prevDepth + interpNum
                                * (currArrival.path[j + 1].depth - prevDepth)
                                / maxInterpNum;
                        out.write(outForms.formatDistance(calcDist)
                                + "  "
                                + outForms.formatDepth(radiusOfEarth
                                        - calcDepth));
                        if(!gmtScript) {
                            printLatLon(out, calcDist);
                        }
                        out.write("\n");
                    }
                }
                prevDepth = currArrival.path[j].depth;
            }
        }
    }

    protected void printLatLon(Writer out, double calcDist) throws IOException {
        double lat, lon;
        if(eventLat != Double.MAX_VALUE && eventLon != Double.MAX_VALUE
                && azimuth != Double.MAX_VALUE) {
            lat = SphericalCoords.latFor(eventLat, eventLon, calcDist, azimuth);
            lon = SphericalCoords.lonFor(eventLat, eventLon, calcDist, azimuth);
            out.write("  " + outForms.formatLatLon(lat) + "  "
                    + outForms.formatLatLon(lon));
        } else if(stationLat != Double.MAX_VALUE
                && stationLon != Double.MAX_VALUE
                && backAzimuth != Double.MAX_VALUE) {
            lat = SphericalCoords.latFor(stationLat, stationLon, degrees
                    - calcDist, backAzimuth);
            lon = SphericalCoords.lonFor(stationLat, stationLon, degrees
                    - calcDist, backAzimuth);
            out.write("  " + outForms.formatLatLon(lat) + "  "
                    + outForms.formatLatLon(lon));
        } else if(stationLat != Double.MAX_VALUE
                && stationLon != Double.MAX_VALUE
                && eventLat != Double.MAX_VALUE && eventLon != Double.MAX_VALUE) {
            azimuth = SphericalCoords.azimuth(eventLat,
                                              eventLon,
                                              stationLat,
                                              stationLon);
            backAzimuth = SphericalCoords.azimuth(stationLat,
                                                  stationLon,
                                                  eventLat,
                                                  eventLon);
            lat = SphericalCoords.latFor(eventLat, eventLon, calcDist, azimuth);
            lon = SphericalCoords.lonFor(eventLat, eventLon, calcDist, azimuth);
            out.write("  " + outForms.formatLatLon(lat) + "  "
                    + outForms.formatLatLon(lon));
        }
    }

    public void init() throws IOException {
        super.init();
        if(gmtScript) {
            String psFile;
            if(outFile == null) {
                outFile = "taup_path.gmt";
                psFile = "taup_path.ps";
            } else if(outFile.endsWith(".gmt")) {
                psFile = outFile.substring(0, outFile.length() - 4) + ".ps";
            } else {
                psFile = outFile + ".ps";
            }
            dos.writeBytes("#!/bin/sh\n");
            dos.writeBytes("#\n# This script will plot ray paths using GMT. If you want to\n"
                    + "#use this as a data file for psxy in another script, delete these"
                    + "\n# first lines, to the last psxy, as well as the last line.\n#\n");
            dos.writeBytes("/bin/rm -f " + psFile + "\n\n");
            dos.writeBytes("# draw surface and label distances.\n"
                    + "psbasemap -K -P -R0/360/0/6371 -JP" + mapWidth
                    + " -B30p/500N > " + psFile + "\n\n");
            dos.writeBytes("# draw circles for branches, note these are scaled for a \n"
                    + "# map using -JP"
                    + mapWidth
                    + "\n"
                    + "psxy -K -O -P -R -JP -Sc -A >> "
                    + psFile
                    + " <<ENDLAYERS\n");
            for(int j = 0; j < 2; j++) {
                dos.writeBytes("0.0 0.0 "
                        + (float)(tMod.getRadiusOfEarth() * mapWidth / tMod.getRadiusOfEarth())
                        + "\n");
                for(int i = 0; i < tMod.tauBranches[j].length; i++) {
                    dos.writeBytes("0.0 0.0 "
                            + (float)((tMod.getRadiusOfEarth() - tMod.tauBranches[j][i].getBotDepth())
                                    * mapWidth / tMod.getRadiusOfEarth())
                            + "\n");
                }
            }
            dos.writeBytes("ENDLAYERS\n\n");
            dos.writeBytes("# draw paths\n");
            dos.writeBytes("psxy -P -R -O -JP -M -A >> " + psFile + " <<END\n");
        }
    }

    public void printUsage() {
        printStdUsage();
        System.out.println("-gmt             -- outputs path as a complete GMT script.");
        printStdUsageTail();
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

    public void start() throws IOException, TauModelException, TauPException {
        super.start();
    }

    public void destroy() throws IOException {
        if(gmtScript) {
            dos.writeBytes("END\n");
        }
        super.destroy();
    }

    /**
     * Allows TauP_Path to run as an application. Creates an instance of
     * TauP_Path and calls TauP_Path.init() and TauP_Path.start().
     */
    public static void main(String[] args) throws FileNotFoundException,
            IOException, StreamCorruptedException, ClassNotFoundException,
            OptionalDataException {
        try {
            TauP_Path tauPPath = new TauP_Path();
            tauPPath.setOutFileBase("taup_path");
            String[] noComprendoArgs = tauPPath.parseCmdLineArgs(args);
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
                    if(noComprendoArgs[i].equals("-help")
                            || noComprendoArgs[i].equals("-version")) {
                        System.out.println();
                        System.exit(0);
                    }
                }
                System.out.println();
                noComprendoArgs = null;
            }
            tauPPath.init();
            if(tauPPath.DEBUG) {
                System.out.println("Done reading " + tauPPath.modelName);
            }
            tauPPath.start();
            tauPPath.destroy();
        } catch(TauModelException e) {
            System.out.println("Caught TauModelException: " + e.getMessage());
            e.printStackTrace();
        } catch(TauPException e) {
            System.out.println("Caught TauPException: " + e.getMessage());
            e.printStackTrace();
        }
    }
}