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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OptionalDataException;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
import java.util.List;

/**
 * Calculate pierce points for different branches using linear interpolation
 * between known slowness samples. A pierce point is where a ray pierces a tau
 * branch. This gives a (very) rough path through the model for a ray.
 * 
 * @version 1.1.3 Fri Apr 5 14:12:19 GMT 2002
 * @author H. Philip Crotwell
 */
public class TauP_Pierce extends TauP_Time {

    protected boolean onlyTurnPoints = false;

    protected boolean onlyRevPoints = false;

    protected boolean onlyUnderPoints = false;

    protected boolean onlyAddPoints = false;

    protected double[] addDepth = new double[0];

    public TauP_Pierce() {
        super();
        setDefaultOutputFormat();
    }

    public TauP_Pierce(TauModel tMod) throws TauModelException {
        super(tMod);
        setDefaultOutputFormat();
    }

    public TauP_Pierce(String modelName) throws TauModelException {
        super(modelName);
        setDefaultOutputFormat();
    }

    // get/set methods
    public void setOnlyTurnPoints(boolean onlyTurnPoints) {
        this.onlyTurnPoints = onlyTurnPoints;
    }

    public void setOnlyRevPoints(boolean onlyRevPoints) {
        this.onlyRevPoints = onlyRevPoints;
    }

    public void setOnlyUnderPoints(boolean onlyUnderPoints) {
        this.onlyUnderPoints = onlyUnderPoints;
    }

    public void setOnlyAddPoints(boolean onlyAddPoints) {
        this.onlyAddPoints = onlyAddPoints;
    }

    /**
     * sets depths for additional pierce points, ie depths that are not really
     * discontinuities in the model.
     */
    public void setAddDepths(String depthString) {
        addDepth = parseAddDepthsList(depthString);
    }

    public void appendAddDepths(String depthString) {
        double[] newDepths = parseAddDepthsList(depthString);
        double[] temp = new double[addDepth.length + newDepths.length];
        System.arraycopy(addDepth, 0, temp, 0, addDepth.length);
        System.arraycopy(newDepths, 0, temp, addDepth.length, newDepths.length);
        addDepth = temp;
    }

    protected double[] parseAddDepthsList(String depthList) {
        int offset = 0;
        int commaIndex;
        String degEntry;
        int numDepths = 0;
        depthList = depthList.replace(' ', ',');
        // remove any empty depths, ie two commas next to each other
        // should be replaced with one comma
        commaIndex = depthList.indexOf(",,", offset);
        while(commaIndex != -1) {
            depthList = depthList.substring(0, commaIndex)
                    + depthList.substring(commaIndex + 1);
            commaIndex = depthList.indexOf(",,", offset);
        }
        // remove comma at begining
        if(depthList.charAt(0) == ',') {
            if(depthList.length() > 1) {
                depthList = depthList.substring(1);
            } else {
                // depthList is just a single comma, no depths, so just return
                return new double[0];
            }
        }
        // and comma at end
        if(depthList.charAt(depthList.length() - 1) == ',') {
            // we know that the length is > 1 as if not then we would have
            // returned from the previous if
            depthList = depthList.substring(0, depthList.length() - 1);
        }
        double[] depthsFound = new double[depthList.length()];
        while(offset < depthList.length()) {
            commaIndex = depthList.indexOf(',', offset);
            if(commaIndex != -1) {
                degEntry = depthList.substring(offset, commaIndex);
                depthsFound[numDepths] = Double.valueOf(degEntry).doubleValue();
                offset = commaIndex + 1;
                numDepths++;
            } else {
                degEntry = depthList.substring(offset);
                depthsFound[numDepths] = Double.valueOf(degEntry).doubleValue();
                offset = depthList.length();
                numDepths++;
            }
        }
        double[] temp = new double[numDepths];
        System.arraycopy(depthsFound, 0, temp, 0, numDepths);
        depthsFound = temp;
        return depthsFound;
    }

    /** override depthCorrect so that we can put the pierce depths in. */
    public void depthCorrect(double depth, double receiverDepth, double scatterDepth) throws TauModelException {
        TauModel tModOrig = tMod; // save original
        tMod = splitPierceDepths(tMod); // add pierce depths
        super.depthCorrect(depth, receiverDepth, scatterDepth); // normal depth correction
        tMod = tModOrig; // restore orig to tMod
    }

    public TauModel splitPierceDepths(TauModel tModOrig) throws TauModelException {
        boolean mustRecalc = false;
        TauModel tModOut = tModOrig;
        if(addDepth != null) {
            double[] branchDepths = tModOrig.getBranchDepths();
            for(int i = 0; i < addDepth.length; i++) {
                for(int j = 0; j < branchDepths.length; j++) {
                    if(addDepth[i] == branchDepths[j]) {
                        // found it, so break and go to the next addDepth
                        break;
                    }
                    // we only get here if we didn't find the depth as a
                    // branch due to the break statement,
                    // so this means we must recalculate
                    mustRecalc = true;
                }
                if(mustRecalc) {
                    // must recalculate, so break out of addDepth loop
                    break;
                }
            }
        }
        if (mustRecalc) {
            if (addDepth != null) {
                for (int i = 0; i < addDepth.length; i++) {
                    tModOut = tModOut.splitBranch(addDepth[i]);
                }
            }
        }
        return tModOut;
    }

    @Override
    public List<Arrival> calculate(List<Double> degreesList) throws TauModelException {
        List<Arrival> arrivalList = super.calculate(degreesList);
        for (Arrival arrival : getArrivals()) {
            arrival.getPierce(); // side effect of calculating pierce points
        }
        return arrivalList;
    }
    
    String getCommentLine(Arrival currArrival) {
        String outName = currArrival.getName();
        if ( ! currArrival.getName().equals(currArrival.getPuristName())) {
            outName+="("+currArrival.getPuristName()+")";
        }
        String out = "> " + outName + " at "
                + Outputs.formatTime(currArrival.getTime())
                + " seconds at "
                + Outputs.formatDistance(currArrival.getDistDeg())
                + " degrees for a "
                + Outputs.formatDepth(currArrival.getSourceDepth())
                + " km deep source in the " + modelName + " model with rayParam "
                + Outputs.formatRayParam(Math.PI / 180 * currArrival.getRayParam()) 
                + " s/deg.";
        if (getReceiverDepth() != 0.0) {
            out += " Receiver at depth: "+getReceiverDepth()+" km.";
        }
        return out;
    }

    @Override
    public void printResultText(PrintWriter out) throws IOException {
        double prevDepth, nextDepth;
        double lat, lon;
        for(int i = 0; i < arrivals.size(); i++) {
            Arrival currArrival = (Arrival) arrivals.get(i);
            out.println(getCommentLine(currArrival));

            TimeDist[] pierce = currArrival.getPierce();
            prevDepth = pierce[0].getDepth();
            for(int j = 0; j < pierce.length; j++) {
                double calcDist = pierce[j].getDistDeg();
                if(j < pierce.length - 1) {
                    nextDepth = pierce[j + 1].getDepth();
                } else {
                    nextDepth = pierce[j].getDepth();
                }
                if(!(onlyTurnPoints || onlyRevPoints || onlyUnderPoints || onlyAddPoints)
                        || ( onlyRevPoints
                                && pierce[j].getDepth() == getScattererDepth()  // scat are always rev points
                                && pierce[j].getDistDeg() == getScattererDistDeg()
                            )
                        || ((onlyAddPoints && isAddDepth(pierce[j].getDepth()))
                                || (onlyRevPoints && ((prevDepth - pierce[j].getDepth())
                                        * (pierce[j].getDepth() - nextDepth) < 0))
                                || (onlyTurnPoints && j != 0
                                        && ((prevDepth - pierce[j].getDepth()) <= 0
                                        && (pierce[j].getDepth() - nextDepth) >= 0))
                                || (onlyUnderPoints
                                        && ((prevDepth - pierce[j].getDepth()) >= 0
                                        && (pierce[j].getDepth() - nextDepth) <= 0)))) {
                    out.write(Outputs.formatDistance(calcDist));
                    out.write(Outputs.formatDepth(pierce[j].getDepth()));
                    out.write(Outputs.formatTime(pierce[j].getTime()));
                    if(eventLat != Double.MAX_VALUE
                            && eventLon != Double.MAX_VALUE
                            && azimuth != Double.MAX_VALUE) {
                        lat = SphericalCoords.latFor(eventLat,
                                                     eventLon,
                                                     calcDist,
                                                     azimuth);
                        lon = SphericalCoords.lonFor(eventLat,
                                                     eventLon,
                                                     calcDist,
                                                     azimuth);
                        out.write("  " + Outputs.formatLatLon(lat) + "  "
                                + Outputs.formatLatLon(lon));
                    } else if(stationLat != Double.MAX_VALUE
                            && stationLon != Double.MAX_VALUE
                            && backAzimuth != Double.MAX_VALUE) {
                        lat = SphericalCoords.latFor(stationLat,
                                                     stationLon,
                                currArrival.getDistDeg() - calcDist,
                                                     backAzimuth);
                        lon = SphericalCoords.lonFor(stationLat,
                                                     stationLon,
                                currArrival.getDistDeg() - calcDist,
                                                     backAzimuth);
                        out.write("  " + Outputs.formatLatLon(lat) + "  "
                                + Outputs.formatLatLon(lon));
                    } else if(stationLat != Double.MAX_VALUE
                            && stationLon != Double.MAX_VALUE
                            && eventLat != Double.MAX_VALUE
                            && eventLon != Double.MAX_VALUE) {
                        azimuth = SphericalCoords.azimuth(eventLat,
                                                          eventLon,
                                                          stationLat,
                                                          stationLon);
                        backAzimuth = SphericalCoords.azimuth(stationLat,
                                                              stationLon,
                                                              eventLat,
                                                              eventLon);
                        lat = SphericalCoords.latFor(eventLat,
                                                     eventLon,
                                                     calcDist,
                                                     azimuth);
                        lon = SphericalCoords.lonFor(eventLat,
                                                     eventLon,
                                                     calcDist,
                                                     azimuth);
                        out.write("  " + Outputs.formatLatLon(lat) + "  "
                                + Outputs.formatLatLon(lon));
                    }
                    out.write("\n");
                }
                prevDepth = pierce[j].getDepth();
            }
        }
    }

    @Override
    public void printResultJSON(PrintWriter out) {
        String s = resultAsJSON(modelName, depth, getReceiverDepth(), getPhaseNames(), arrivals, true, false);
        out.println(s);
    }

    /**
     * checks to see if the given depth has been "added" as a pierce point.
     */
    public synchronized boolean isAddDepth(double depth) {
        for(int i = 0; i < addDepth.length; i++) {
            if(depth == addDepth[i]) {
                return true;
            }
        }
        return false;
    }

    public void printLimitUsage() {
        Alert.info("--first            -- only output the first arrival for each phase, no triplications\n"

                +"-rev               -- only prints underside and bottom turn points, e.g. ^ and v\n"
        +"-turn              -- only prints bottom turning points, e.g. v\n"
        +"-under             -- only prints underside reflection points, e.g. ^\n\n"
        +"-pierce depth      -- adds depth for calculating pierce points\n"
        +"-nodiscon          -- only prints pierce points for the depths added with -pierce\n"
        );
    }

    /** prints the known command line flags. */
    public void printUsage() {
        printStdUsage();
        System.out.println("-az azimuth        -- sets the azimuth (event to station)\n"
                + "                      used to output lat and lon of pierce points\n"
                + "                      if the event lat lon and distance are also\n"
                + "                      given. Calculated if station and event\n"
                + "                      lat and lon are given.");
        System.out.println("-baz backazimuth   -- sets the back azimuth (station to event)\n"
                + "                      used to output lat and lon of pierce points\n"
                + "                      if the station lat lon and distance are also\n"
                + "                      given. Calculated if station and event\n"
                + "                      lat and lon are given.\n");
        printLimitUsage();
        printStdUsageTail();
    }

    public String[] parseCmdLineArgs(String[] args) throws IOException {
        int i = 0;
        String[] leftOverArgs;
        int numNoComprendoArgs = 0;
        leftOverArgs = super.parseCmdLineArgs(args);
        String[] noComprendoArgs = new String[leftOverArgs.length];
        while(i < leftOverArgs.length) {
            if(dashEquals("turn", leftOverArgs[i])) {
                onlyTurnPoints = true;
            } else if(dashEquals("rev", leftOverArgs[i])) {
                onlyRevPoints = true;
            } else if(dashEquals("under", leftOverArgs[i])) {
                onlyUnderPoints = true;
            } else if(dashEquals("pierce", leftOverArgs[i])
                    && i < leftOverArgs.length - 1) {
                appendAddDepths(leftOverArgs[i + 1]);
                i++;
            } else if(dashEquals("nodiscon", leftOverArgs[i])) {
                onlyAddPoints = true;
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
     * Allows TauP_Pierce to run as an application. Creates an instance of
     * TauP_Pierce. 
     * ToolRun.main should be used instead.
     */
    public static void main(String[] args) throws IOException {
        ToolRun.legacyRunTool(ToolRun.PIERCE, args);
    }
    
}
