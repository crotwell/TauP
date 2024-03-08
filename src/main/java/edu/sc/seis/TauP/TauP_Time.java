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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.io.*;
import java.util.*;

/**
 * Calculate travel times for different branches using linear interpolation
 * between known slowness samples.
 *
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * @author H. Philip Crotwell
 */
public class TauP_Time extends TauP_AbstractTimeTool {


    protected List<Double> degreesList = new ArrayList<Double>();

    /**
     * For when command line args uses --km for distance. Have to wait until
     * after the model is read in to get radius of earth.
     */
    protected List<Double> distKilometersList = new ArrayList<Double>();

    protected double azimuth = Double.MAX_VALUE;

    protected double backAzimuth = Double.MAX_VALUE;

    protected double takeoffAngle = Double.MAX_VALUE;

    protected double shootRayp = Double.MAX_VALUE;

    protected double stationLat = Double.MAX_VALUE;

    protected double stationLon = Double.MAX_VALUE;

    protected double eventLat = Double.MAX_VALUE;

    protected double eventLon = Double.MAX_VALUE;

    protected List<Arrival> arrivals = new ArrayList<Arrival>();

    protected boolean GUI = false;

    protected boolean onlyPrintRayP = false;

    protected boolean onlyPrintTime = false;

    protected boolean onlyFirst = false;

    protected String relativePhaseName = "";

    public TauP_Time() {
        setDefaultOutputFormat();
    }

    public TauP_Time(TauModel tMod)  {
        setDefaultOutputFormat();
        setTauModel(tMod);
    }

    /**
     * creates a TauP_Time object with the tau model specified by modelName
     * already loaded.
     *
     * @throws TauModelException
     *             if the file can't be found or is corrupted in some way.
     */
    public TauP_Time(String modelName) throws TauModelException {
        try {
            loadTauModel(modelName);
        } catch(FileNotFoundException e) {
            throw new TauModelException("FileNotFoundException:"
                    + e.getMessage(), e);
        } catch(InvalidClassException e) {
            throw new TauModelException("InvalidClassException:"
                    + e.getMessage(), e);
        } catch(StreamCorruptedException e) {
            throw new TauModelException("StreamCorruptedException:"
                    + e.getMessage(), e);
        } catch(OptionalDataException e) {
            throw new TauModelException("OptionalDataException:"
                    + e.getMessage(), e);
        } catch(IOException e) {
            throw new TauModelException("IOException:" + e.getMessage(), e);
        }
        setDefaultOutputFormat();
    }

    @Override
    public String[] allowedOutputFormats() {
        String[] formats = {TEXT, JSON};
        return formats;
    }
    @Override
    public void setDefaultOutputFormat() {
        setOutputFormat(TEXT);
    }


    public void setEventLatLon(double lat, double lon) {
        this.eventLat = lat;
        this.eventLon = lon;
    }

    public void setStationLatLon(double lat, double lon) {
        this.stationLat = lat;
        this.stationLon = lon;
    }

    @Override
    public void clearPhases() {
        super.clearPhases();
        clearArrivals();
    }

    public void clearArrivals() {
        arrivals = new ArrayList<Arrival>();
    }

    public int getNumArrivals() {
        return arrivals.size();
    }

    public Arrival getArrival(int i) {
        return (Arrival)arrivals.get(i);
    }

    public List<Arrival> getArrivals() {
        return Collections.unmodifiableList(arrivals);
    }

    /* Normal methods */

    /**
     * Parses a comma separated list of distances and returns them in an array.
     */
    public static List<Double> parseDegreeList(String degList) {
        degList = degList.trim();
        while (degList.startsWith(",")) {
            degList = degList.substring(1);
        }
        while(degList.endsWith(",")) {
            degList = degList.substring(0, degList.length()-1);
        }
        String[] split = degList.trim().split(",");
        List<Double> degreesFound = new ArrayList<Double>(split.length);
        for (int i = 0; i < split.length; i++) {
            try {
                degreesFound.add(Double.parseDouble(split[i].trim()));
            } catch (NumberFormatException e) {
                // oh well
                System.err.println("can't parse '"+split[i]+"' as number, skipping.");
            }
        }
        return degreesFound;
    }

    /*
     * parses the standard command line args for the taup package. Other tools
     * that subclass this class will likely override this.
     */
    protected String[] parseCmdLineArgs(String[] origArgs) throws IOException, TauPException {
        int i = 0;
        String[] args = parseSourceModelCmdLineArgs(origArgs);
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        boolean cmdLineArgPhase = false;
        boolean cmdLineArgPhaseFile = false;
        while(i < args.length) {
            if(dashEquals("json", args[i])) {
                outputFormat = TauP_Tool.JSON;
            } else if(dashEquals("gui", args[i])) {
                GUI = true;
            } else if(dashEquals("rayp", args[i])) {
                onlyPrintRayP = true;
                onlyPrintTime = false;
            } else if(dashEquals("time", args[i])) {
                onlyPrintTime = true;
                onlyPrintRayP = false;
            } else if(dashEquals("first", args[i])) {
                onlyFirst = true;
            } else if(i < args.length - 1) {
                if(dashEquals("deg", args[i])) {
                    degreesList = parseDegreeList(args[i+1]);
                    i++;
                } else if(dashEquals("km", args[i])) {
                    distKilometersList = parseDegreeList(args[i+1]);
                    //distKilometersList.add(Double.valueOf(args[i + 1]).doubleValue());
                    i++;
                } else if(dashEquals("az", args[i])) {
                    azimuth = Double.valueOf(args[i + 1]).doubleValue();
                    i++;
                } else if(dashEquals("baz", args[i])) {
                    backAzimuth = Double.valueOf(args[i + 1]).doubleValue();
                    i++;
                } else if(dashEquals("takeoff", args[i])) {
                    takeoffAngle = Double.valueOf(args[i + 1]).doubleValue();
                    i++;
                } else if(dashEquals("shootray", args[i])) {
                    shootRayp = Double.valueOf(args[i + 1]).doubleValue();
                    i++;
                } else if(dashEquals("rel", args[i])) {
                    relativePhaseName = args[i + 1];
                    i++;
                } else if(i < args.length - 2) {
                    if(dashEquals("sta", args[i])
                            || dashEquals("station", args[i])) {
                        setStationLatLon(Double.valueOf(args[i + 1]).doubleValue(),
                                         Double.valueOf(args[i + 2]).doubleValue());
                        i += 2;
                    } else if(dashEquals("evt", args[i])
                            || dashEquals("event", args[i])) {
                        setEventLatLon( Double.valueOf(args[i + 1]).doubleValue(),
                                        Double.valueOf(args[i + 2]).doubleValue());
                        i += 2;
                    } else {
                        /*
                         * I don't know how to interpret this argument, so pass
                         * it back
                         */
                        noComprendoArgs[numNoComprendoArgs++] = args[i];
                    }
                } else {
                    /*
                     * I don't know how to interpret this argument, so pass it
                     * back
                     */
                    noComprendoArgs[numNoComprendoArgs++] = args[i];
                }
            } else {
                /* I don't know how to interpret this argument, so pass it back */
                noComprendoArgs[numNoComprendoArgs++] = args[i];
            }
            i++;
        }
        // check to see if there were phases or a phase file as an argument.
        // if so then dump the defaults
        if(cmdLineArgPhaseFile || cmdLineArgPhase) {
            if(cmdLineArgPhaseFile && !cmdLineArgPhase) {
                toolProps.remove("taup.phase.list");
            }
            if(!cmdLineArgPhaseFile && cmdLineArgPhase) {
                toolProps.remove("taup.phase.file");
            }
        }
        if(numNoComprendoArgs > 0) {
            String[] temp = new String[numNoComprendoArgs];
            System.arraycopy(noComprendoArgs, 0, temp, 0, numNoComprendoArgs);
            return temp;
        } else {
            return new String[0];
        }
    }

    public synchronized List<Arrival> sortArrivals(List<Arrival> arrivals) {
        Collections.sort(arrivals, new Comparator<Arrival>() {
            public int compare(Arrival o1, Arrival o2) {
                return Double.compare(o1.getTime(), o2.getTime());
            }
        });
        return arrivals;
    }

    public List<Arrival> calculate(double degrees) throws TauPException {
        List<Double> dList = Arrays.asList(new Double[] {degrees});
        return calculate(dList);
    }

    public List<Arrival> calculate(List<Double> degreesList) throws TauPException {
        List<Arrival> arrivals = calcTime(degreesList);
        return sortArrivals(arrivals);
    }

    public List<Arrival> calcEventStation(Double[] evloc, List<Double[]> staloc) throws TauPException {
        setEventLatLon(evloc[0], evloc[1]);
        List<Arrival> out = new ArrayList<>();
        for (Double[] sta : staloc) {
            clearArrivals();
            setEventLatLon(evloc[0], evloc[1]);
            setStationLatLon(sta[0], sta[1]);
            degreesList.add(SphericalCoords.distance(sta[0], sta[1], evloc[0], evloc[1]));
            azimuth = SphericalCoords.azimuth(evloc[0], evloc[1], sta[0], sta[1]);
            backAzimuth = SphericalCoords.azimuth(sta[0], sta[1], evloc[0], evloc[1]);
            out.addAll(calculate(degreesList));
        }
        this.arrivals = sortArrivals(out);
        return this.arrivals;
    }

    public Arrival calculateRelativeArrival(double degrees) throws TauModelException {
        Arrival relativeArrival = null;
        if (relativePhaseName != "") {
            List<SeismicPhase> relPhases = new ArrayList<SeismicPhase>();
            List<String> splitNames = extractPhaseNames(relativePhaseName);
            for (String sName : splitNames) {
                try {
                    List<SeismicPhase> calcRelPhaseList = SeismicPhaseFactory.createSeismicPhases(
                            sName,
                            getTauModelDepthCorrected(),
                            this.getSourceDepth(),
                            this.getReceiverDepth(),
                            this.getScattererDepth(),
                            this.getScattererDistDeg(),
                            this.DEBUG);
                    relPhases.addAll(calcRelPhaseList);
                } catch (ScatterArrivalFailException e) {
                    Alert.warning(e.getMessage(),
                            "    Skipping this relative phase");
                    if (verbose || DEBUG) {
                        e.printStackTrace();
                    }
                }

            }
            relativeArrival = SeismicPhase.getEarliestArrival(relPhases, degrees);
        }
        return relativeArrival;
    }

    @Override
    public void validateArguments() throws TauModelException {
        if (this.getTauModel() == null) {
            throw new TauModelException("Model for '"+this.getTauModelName()+"' is null, unable to calculate.");
        }
        if (this.getTauModel().getRadiusOfEarth() < getSourceDepth()) {
            throw new TauModelException("Source depth of "+getSourceDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+this.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }
        if (this.getTauModel().getRadiusOfEarth() < getReceiverDepth()) {
            throw new TauModelException("Receiver depth of "+getReceiverDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+this.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }
        if (this.getTauModel().getRadiusOfEarth() < getScattererDepth()) {
            throw new TauModelException("Scatterer depth of "+getScattererDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+this.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }
    }

    public List<Arrival> calcTime(double degrees) throws TauModelException {
        List<Double> dList = Arrays.asList(new Double[] {degrees});
        return calcTime(dList);
    }

    public List<Arrival> calcTime(List<Double> degreesList) throws TauModelException {
        validateArguments();
        depthCorrect();
        clearArrivals();
        SeismicPhase phase;
        clearArrivals();
        List<SeismicPhase> phaseList = getSeismicPhases();
        for (double degrees : degreesList) {
            for (int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                phase = phaseList.get(phaseNum);
                List<Arrival> phaseArrivals = phase.calcTime(degrees);
                Arrival relativeArrival = calculateRelativeArrival(degrees);
                for (Arrival arrival : phaseArrivals) {
                    arrival.setRelativeToArrival(relativeArrival);
                }
                if (!onlyFirst) {
                    for (Arrival arrival : phaseArrivals) {
                        arrivals.add(arrival);
                    }
                } else {
                    if (phaseArrivals.size() > 0) {
                        arrivals.add(phaseArrivals.get(0));
                    }
                }
            }
        }
        return sortArrivals(arrivals);
    }


    public List<Arrival> calcTakeoff(double takeoffAngle) throws TauModelException {
        List<Double> dList = Arrays.asList(new Double[] {takeoffAngle});
        return calcTakeoff(dList);
    }

    public List<Arrival> calcTakeoff(List<Double> takeoffAngleList) throws TauModelException {
        stationLat = Double.MAX_VALUE;
        stationLon = Double.MAX_VALUE;
        depthCorrect();
        clearArrivals();
        SeismicPhase phase;
        List<SeismicPhase> phaseList = getSeismicPhases();
        List<Arrival> arrivals = new ArrayList<>();
        for (double takeoffAngle : takeoffAngleList) {
            for (int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                phase = phaseList.get(phaseNum);
                if (phase.getDownGoing()[0] == (takeoffAngle <= 90)) {
                    // check both downgoing or both upgoing
                    double rayParam = phase.calcRayParamForTakeoffAngle(takeoffAngle);
                    Arrival phaseArrival;
                    try {
                        if (phase.getMinRayParam() <= rayParam && rayParam <= phase.getMaxRayParam()) {
                            phaseArrival = phase.shootRay(rayParam);
                            arrivals.add(phaseArrival);
                        }
                    } catch (NoSuchLayerException e) {
                        Alert.warning("NoSuchLayerException", e.getMessage());
                    } catch (SlownessModelException e) {
                        Alert.warning("SlownessModelException", e.getMessage());
                    }
                }
            }
        }
        this.arrivals = sortArrivals(arrivals);
        return this.arrivals;    }

    public List<Arrival> calcRayParameter(double rayparameter) throws TauModelException {
        List<Double> dList = Arrays.asList(new Double[] {rayparameter});
        return calcRayParameter(dList);
    }

    /**
     * Shoots ray parameters for each phases from the source.
     * @param rayParameterList ray parameter list in s/deg
     * @throws TauModelException
     */
    public List<Arrival> calcRayParameterSDeg(List<Double> rayParameterList) throws TauModelException {
        List<Double> rayParameterListRad = new ArrayList<>();
        for (Double d : rayParameterList) {
            rayParameterListRad.add(d/SphericalCoords.dtor);
        }
        return calcRayParameter(rayParameterListRad);
    }

    /**
     * Shoots ray parameters for each phases from the source.
     * @param rayParameterList ray parameter list in s/radian
     * @throws TauModelException
     */
    public List<Arrival> calcRayParameter(List<Double> rayParameterList) throws TauModelException {
        stationLat = Double.MAX_VALUE;
        stationLon = Double.MAX_VALUE;
        depthCorrect();
        clearArrivals();
        SeismicPhase phase;
        List<SeismicPhase> phaseList = getSeismicPhases();
        List<Arrival> arrivals = new ArrayList<>();
        for (Double rayParam : rayParameterList) {
            for (int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                phase = phaseList.get(phaseNum);
                Arrival phaseArrival;
                try {
                    if (phase.getMinRayParam() <= rayParam && rayParam <= phase.getMaxRayParam()) {
                        phaseArrival = phase.shootRay(rayParam);
                        arrivals.add(phaseArrival);
                    }
                } catch (NoSuchLayerException e) {
                    Alert.warning("NoSuchLayerException", e.getMessage());
                } catch (SlownessModelException e) {
                    Alert.warning("SlownessModelException", e.getMessage());
                }
            }
        }
        this.arrivals = sortArrivals(arrivals);
        return this.arrivals;
    }

    public void printResult(PrintWriter out) throws IOException {
        if (outputFormat.equals(TauP_Tool.JSON)) {
            printResultJSON(out);
        } else {
            printResultText(out);
        }
        out.flush();
    }

    public void printResultText(PrintWriter out) throws IOException {
        Arrival currArrival;
        int maxNameLength = 5;
        int maxPuristNameLength = 5;
        for(int j = 0; j < arrivals.size(); j++) {
            if(((Arrival)arrivals.get(j)).getName().length() > maxNameLength) {
                maxNameLength = ((Arrival)arrivals.get(j)).getName()
                        .length();
            }
            if(((Arrival)arrivals.get(j)).getPuristName().length() > maxPuristNameLength) {
                maxPuristNameLength = ((Arrival)arrivals.get(j)).getPuristName()
                        .length();
            }
        }
        //Format phaseFormat = new Format("%-" + maxNameLength + "s");
        String phaseFormat = "%-" + maxNameLength + "s";
        //Format phasePuristFormat = new Format("%-" + maxPuristNameLength + "s");
        String phasePuristFormat = "%-" + maxPuristNameLength + "s";
        if(!(onlyPrintRayP || onlyPrintTime)) {
            String modelLine =  "\nModel: " + modelName;
            if (getReceiverDepth() != 0.0) {
                modelLine += "  Receiver Depth: "+getReceiverDepth()+" km";
            }
            if (getScattererDistDeg() != 0.0) {
                modelLine += "  Scatter Depth: "+ getScattererDepth()+" km Dist: "+ getScattererDistDeg() ;
            }
            out.println(modelLine);
            String lineOne = "Distance   Depth   " + String.format(phaseFormat, "Phase")
                    + "   Travel    Ray Param  Takeoff  Incident  Purist   "+String.format(phasePuristFormat, "Purist")+"    Amp   ";
            String lineTwo = "  (deg)     (km)   " + String.format(phaseFormat, "Name ")
                    + "   Time (s)  p (s/deg)   (deg)    (deg)   Distance   "+String.format(phasePuristFormat, "Name") +"  Factor";
            if (relativePhaseName != "") {
                lineOne += " Relative to";
                for (int s=0; s<(11-relativePhaseName.length())/2;s++) {
                    lineTwo += " ";
                }
                lineTwo += "  "+String.format(phaseFormat, relativePhaseName);
            }
            out.println(lineOne);
            out.println(lineTwo);
            for(int i = 0; i < Math.max(lineOne.length(), lineTwo.length()); i++) {
                out.write("-");
            }
            out.write("\n");
            for(int j = 0; j < arrivals.size(); j++) {
                currArrival = (Arrival)arrivals.get(j);
                out.print(Outputs.formatDistance(currArrival.getSearchDistDeg()));
                out.print(Outputs.formatDepth(depth) + "   ");
                out.print(String.format(phaseFormat, currArrival.getName()));
                out.print("  "
                        + Outputs.formatTime(currArrival.getTime())
                        + "  "
                        + Outputs.formatRayParam(currArrival.getRayParam() / Arrival.RtoD) + "  ");
                out.print(Outputs.formatDistance(currArrival.getTakeoffAngle())+" ");
                out.print(Outputs.formatDistance(currArrival.getIncidentAngle())+" ");
                out.print(Outputs.formatDistance(currArrival.getDistDeg()));
                if(currArrival.getName().equals(currArrival.getPuristName())) {
                    out.print("   = ");
                } else {
                    out.print("   * ");
                }
                out.print(String.format(phasePuristFormat, currArrival.getPuristName()));
                try {
                    double ampFactorPSV = currArrival.getAmplitudeFactorPSV();
                    double ampFactorSH = currArrival.getAmplitudeFactorSH();
                    out.print(" " + Outputs.formatAmpFactor(ampFactorPSV)+","+Outputs.formatAmpFactor(ampFactorSH));
                } catch (SlownessModelException | TauModelException | VelocityModelException e) {
                    throw new RuntimeException("SHould not happen", e);
                }

                if (relativePhaseName != "") {
                    if (currArrival.isRelativeToArrival()) {
                        out.print(" "+Outputs.formatTime(currArrival.getTime() - currArrival.getRelativeToArrival().getTime()));
                        out.print(" +"+String.format(phaseFormat, currArrival.getRelativeToArrival().getName()));
                    } else {
                        out.print(String.format(phaseFormat, " no arrival"));
                    }
                }

                out.println();
            }
        } else if(onlyPrintTime) {
            for(int j = 0; j < arrivals.size(); j++) {
                currArrival = (Arrival)arrivals.get(j);
                out.print(String.valueOf((float)(currArrival.getTime())) + " ");
            }
            out.println();
        } else if(onlyPrintRayP) {
            for(int j = 0; j < arrivals.size(); j++) {
                currArrival = (Arrival)arrivals.get(j);
                out.write(String.valueOf((float)(Math.PI / 180.0 * currArrival.getRayParam()))
                        + " ");
            }
            out.println();
        }
        out.println();
        out.flush();
    }

    public void printResultJSON(PrintWriter out) throws IOException {
        writeJSON(out, "");
    }

    public void writeJSON(PrintWriter pw, String indent) throws IOException {
        writeJSON(pw, indent,
                getTauModelName(),
                getSourceDepth(),
                getReceiverDepth(),
                getSeismicPhases(),
                getArrivals());
    }

    public void writeJSON(PrintWriter pw, String indent,
                          String modelName,
                          double depth,
                          double receiverDepth,
                          List<SeismicPhase> phases,
                          List<Arrival> arrivals) throws IOException {
        String innerIndent = indent+"  ";
        String NL = "\n";
        pw.write("{"+NL);
        pw.write(innerIndent+JSONWriter.valueToString("model")+": "+JSONWriter.valueToString(modelName)+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("sourcedepth")+": "+JSONWriter.valueToString((float)depth)+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("receiverdepth")+": "+JSONWriter.valueToString((float)receiverDepth)+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("phases")+": [ ");
        boolean first = true;
        for (SeismicPhase phase : phases) {
            if (first) {
                first = false;
            } else {
                pw.write(", ");
            }
            pw.write(JSONWriter.valueToString(phase.getName()));
        }
        pw.write(" ],"+NL);

        pw.write(innerIndent+JSONWriter.valueToString("arrivals")+": ["+NL);
        first = true;
        for (Arrival arrival : arrivals) {
            if (first) {
                first = false;
            } else {
                pw.write(","+NL);
            }
            try {
                arrival.writeJSON(pw, innerIndent + "  ");
            } catch (JSONException e) {
                System.err.println("Erro in json: "+arrival.toString());
                throw e;
            }
        }
        pw.write(NL);
        pw.write(innerIndent+"]"+NL);
        pw.write("}"+NL);
    }

    public static JSONObject resultAsJSONObject(String modelName,
                                      double depth,
                                      double receiverDepth,
                                      String[] phases,
                                      List<Arrival> arrivals) {
        JSONObject out = baseResultAsJSONObject( modelName, depth,  receiverDepth, phases);
        JSONArray outArrivals = new JSONArray();
        out.put("arrivals", outArrivals);
        for(int j = 0; j < arrivals.size(); j++) {
            Arrival currArrival = (Arrival)arrivals.get(j);
            outArrivals.put(currArrival.asJSONObject());
        }
        return out;
    }

    public static String resultAsJSON(String modelName,
                                      double depth,
                                      double receiverDepth,
                                      String[] phases,
                                      List<Arrival> arrivals,
                                      boolean withPierce,
                                      boolean withPath) {
        String Q = ""+'"';
        String COMMA = ",";
        String QCOMMA = Q+COMMA;
        String COLON = ": "; // plus space
        String S = "  ";
        String QC = Q+COLON;
        String QCQ = QC+Q;
        String SS = S+S;
        String SQ = S+Q;
        String SSQ = S+SQ;
        String SSSQ = S+SSQ;
        // use cast to float to limit digits printed
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println("{");
        out.println(SQ+"model"+QCQ+modelName+QCOMMA);
        out.println(SQ+"sourcedepth"+QC+(float)depth+COMMA);
        out.println(SQ+"receiverdepth"+QC+(float)receiverDepth+COMMA);
        out.print(SQ+"phases"+Q+": [");
        for(int p=0; p<phases.length; p++) {
            out.print(" "+Q+phases[p]+Q);
            if ( p != phases.length-1) {
                out.print(COMMA);
            }
        }
        out.println(" ]"+COMMA);
        out.println(SQ+"arrivals"+Q+": [");
        for(int j = 0; j < arrivals.size(); j++) {
            Arrival currArrival = (Arrival)arrivals.get(j);
            out.print(currArrival.asJSONObject().toString(2));
            //out.print(currArrival.asJSON(true, SS, withPierce, withPath));
            if (j != arrivals.size()-1) {
                out.print(COMMA);
            }
            out.println();
        }
        out.println(S+"]");
        out.print("}");
        return sw.toString();
    }

    /**
     * preforms intialization of the tool. Properties are queried for the
     * default model to load, source depth to use, phases to use, etc.
     */
    public void init() throws TauPException {
        super.init();
        // check for command line arg distance in km
        for (double distKilometers : distKilometersList) {
                degreesList.add(distKilometers / getTauModel().getRadiusOfEarth()
                        * 180.0 / Math.PI);
        }
    }

    public void printHelp() {
        Alert.info("Enter:\nh for new depth\nr to recalculate\n"
                + "p to append phases, \nc to clear phases\n"
                + "l to list phases\n"
                + "s for new station lat lon\ne for new event lat lon\n"
                + "a for new azimuth\nb for new back azimuth\n"
                + "t for takeoff angle\n"
                + "m for new model or \nq to quit.\n");
    }

    public void start() throws IOException, TauModelException, TauPException {
        if((degreesList.size() != 0 || takeoffAngle != Double.MAX_VALUE
                || shootRayp != Double.MAX_VALUE
                || (stationLat != Double.MAX_VALUE
                && stationLon != Double.MAX_VALUE
                && eventLat != Double.MAX_VALUE && eventLon != Double.MAX_VALUE))) {
            /* enough info given on cmd line, so just do one calc. */
            if (takeoffAngle != Double.MAX_VALUE) {
                calcTakeoff(takeoffAngle);
            } else if (shootRayp != Double.MAX_VALUE) {
                calcRayParameter(shootRayp/SphericalCoords.dtor);
            } else {
                if(degreesList.size() == 0) {
                    Double[] evlatlon = new Double[] {eventLat, eventLon};
                    List<Double[]> stalatlonList = new ArrayList<>();
                    stalatlonList.add(new Double[] {stationLat, stationLon});
                    calcEventStation(evlatlon, stalatlonList);
                } else {
                    calculate(degreesList);
                }
            }
            printResult(getWriter());
        } else {
            /* interactive mode... */
            long prevTime = 0;
            long currTime;
            char readMode = 'd';
            double tempDepth = depth;
            setSourceDepth(depth);
            StreamTokenizer tokenIn = new StreamTokenizer(new InputStreamReader(System.in));
            tokenIn.parseNumbers();
            tokenIn.wordChars(',', ',');
            tokenIn.wordChars('_', '_');
            tokenIn.wordChars('^', '^');
            tokenIn.ordinaryChar('/');
            tokenIn.wordChars('/', '/');
            tokenIn.commentChar('#');
            printHelp();
            do {
                switch(readMode){
                    case 'h':
                        // new source depth
                        System.out.print("Enter Depth: ");
                        tokenIn.nextToken();
                        tempDepth = tokenIn.nval;
                        if(tempDepth < 0.0
                                || tempDepth > tMod.getRadiusOfEarth()) {
                            Alert.warning("Depth must be >= 0.0 and <= tMod.getRadiusOfEarth().",
                                          "depth = " + tempDepth
                                                  + " getRadiusOfEarth= "
                                                  + tMod.getRadiusOfEarth());
                            continue;
                        }
                        prevTime = System.currentTimeMillis();
                        setSourceDepth(tempDepth);
                        currTime = System.currentTimeMillis();
                        if(verbose) {
                            Alert.info("depthCorrect time="
                                    + (currTime - prevTime));
                        }
                        readMode = 'd';
                        break;
                    case 'd':
                        // new distance or option
                        System.out.print("Enter Distance or Option [hrpclseabmqt]: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            double degrees = tokenIn.nval;
                            if(DEBUG) {
                                Alert.info("degrees=" + degrees);
                            }
                            degreesList.clear();
                            degreesList.add(degrees);
                            calculate(degreesList);
                            printResult(getWriter());
                        } else {
                            if(tokenIn.ttype == StreamTokenizer.TT_EOF
                                    || (tokenIn.ttype == StreamTokenizer.TT_WORD && (tokenIn.sval.equalsIgnoreCase("q")
                                            || tokenIn.sval.equalsIgnoreCase("quit")
                                            || tokenIn.sval.equalsIgnoreCase("exit") || tokenIn.sval.equalsIgnoreCase("bye")))) {
                                readMode = 'q';
                            } else if(tokenIn.ttype == StreamTokenizer.TT_WORD) {
                                if(tokenIn.sval.equalsIgnoreCase("l")) {
                                    readMode = 'l';
                                } else if(tokenIn.sval.equalsIgnoreCase("c")) {
                                    readMode = 'c';
                                } else if(tokenIn.sval.equalsIgnoreCase("s")) {
                                    readMode = 's';
                                } else if(tokenIn.sval.equalsIgnoreCase("e")) {
                                    readMode = 'e';
                                } else if(tokenIn.sval.equalsIgnoreCase("a")) {
                                    readMode = 'a';
                                } else if(tokenIn.sval.equalsIgnoreCase("b")) {
                                    readMode = 'b';
                                } else if(tokenIn.sval.equalsIgnoreCase("r")) {
                                    readMode = 'r';
                                } else if(tokenIn.sval.equalsIgnoreCase("p")) {
                                    readMode = 'p';
                                } else if(tokenIn.sval.equalsIgnoreCase("m")) {
                                    readMode = 'm';
                                } else if(tokenIn.sval.equalsIgnoreCase("h")) {
                                    readMode = 'h';
                                } else if(tokenIn.sval.equalsIgnoreCase("x")) {
                                    readMode = 'x';
                                } else if(tokenIn.sval.equalsIgnoreCase("t")) {
                                    readMode = 't';
                                } else if(tokenIn.sval.equalsIgnoreCase("?")) {
                                    printHelp();
                                } else {
                                    Alert.warning("I don't understand this option",
                                            tokenIn.sval);
                                    printHelp();
                                }
                            } else {
                                printHelp();
                            }
                        }
                        break;
                    case 'r':
                        // recalulate
                        if(degreesList.size() != 0) {
                            calculate(degreesList);
                            printResult(getWriter());
                        }
                        readMode = 'd';
                        break;
                    case 'p':
                        // append phases
                        System.out.print("Enter phases (ie P,p,PcP,S): ");
                        tokenIn.ordinaryChars('0', '9');
                        tokenIn.ordinaryChar('.');
                        tokenIn.ordinaryChar('-');
                        tokenIn.wordChars('0', '9');
                        tokenIn.wordChars('.', '.');
                        tokenIn.wordChars('-', '-');
                        tokenIn.ordinaryChar(' ');
                        tokenIn.wordChars(' ', ' ');
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_WORD) {
                            parsePhaseList(tokenIn.sval);
                            recalcPhases();
                        } else {
                            Alert.warning("Input phases not recognized.",
                                          "Please retry.");
                        }
                        tokenIn.parseNumbers();
                        tokenIn.ordinaryChar(' ');
                        tokenIn.whitespaceChars(' ', ' ');
                        readMode = 'd';
                        break;
                    case 'l':
                        // list phases
                        int numPhases = phaseNames.size();
                        String output = numPhases + " phases.";
                        Alert.info(output);
                        output = "";
                        for(int i = 0; i < numPhases; i++) {
                            output += phaseNames.get(i).getName();
                            if(i < numPhases - 1) {
                                output += ",";
                            }
                        }
                        Alert.info(output);
                        readMode = 'd';
                        break;
                    case 'c':
                        // clear phases and then enter new phases
                        clearPhaseNames();
                        readMode = 'p';
                        break;
                    case 'a':
                        // event to station azimuth
                        System.out.print("Enter azimuth: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            azimuth = tokenIn.nval;
                            stationLat = Double.MAX_VALUE;
                            stationLon = Double.MAX_VALUE;
                            if(DEBUG) {
                                Alert.info("azimuth=" + azimuth);
                            }
                        } else {
                            Alert.warning("Expected a number.", "got "
                                    + tokenIn + " instead.");
                            printHelp();
                            break;
                        }
                        if(eventLat == Double.MAX_VALUE
                                || eventLon == Double.MAX_VALUE) {
                            readMode = 'e';
                        } else if(degreesList.size() == 0) {
                            readMode = 'd';
                        } else {
                            calculate(degreesList);
                            printResult(getWriter());
                        }
                        readMode = 'd';
                        break;
                    case 'b':
                        // event to station back azimuth (ie station to event
                        // azimuth)
                        System.out.print("Enter back azimuth: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            backAzimuth = tokenIn.nval;
                            setEventLatLon( Double.MAX_VALUE, Double.MAX_VALUE);
                            if(DEBUG) {
                                Alert.info("backAzimuth=" + backAzimuth);
                            }
                        } else {
                            Alert.warning("Expected a number.", "got "
                                    + tokenIn + " instead");
                            printHelp();
                            break;
                        }
                        if(stationLat == Double.MAX_VALUE
                                || stationLon == Double.MAX_VALUE) {
                            readMode = 's';
                        } else if(degreesList.size() == 0) {
                            readMode = 'd';
                        } else {
                            calculate(degreesList);
                            printResult(getWriter());
                        }
                        readMode = 'd';
                        break;
                    case 'e':
                        // event lat and lon
                        System.out.print("Enter event lat and lon: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            double evLat = tokenIn.nval;
                            if(DEBUG) {
                                Alert.info("eventLat=" + eventLat);
                            }
                            tokenIn.nextToken();
                            if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                                double evLon = tokenIn.nval;
                                setEventLatLon(evLat, evLon);
                                if(DEBUG) {
                                    Alert.info("eventLon=" + eventLon);
                                }
                            } else {
                                printHelp();
                            }
                        } else {
                            printHelp();
                        }
                        if(stationLat != Double.MAX_VALUE
                                && stationLon != Double.MAX_VALUE) {

                            Double[] evlatlon = new Double[] {eventLat, eventLon};
                            List<Double[]> stalatlonList = new ArrayList<>();
                            stalatlonList.add(new Double[] {stationLat, stationLon});
                            calcEventStation(evlatlon, stalatlonList);
                            printResult(getWriter());
                        }
                        readMode = 'd';
                        break;
                    case 's':
                        // station lat and lon
                        System.out.print("Enter station lat and lon: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            stationLat = tokenIn.nval;
                            if(DEBUG) {
                                Alert.info("stationLat=" + stationLat);
                            }
                            tokenIn.nextToken();
                            if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                                stationLon = tokenIn.nval;
                                if(DEBUG) {
                                    Alert.info("stationLon=" + stationLon);
                                }
                            } else {
                                printHelp();
                                break;
                            }
                        } else {
                            printHelp();
                            break;
                        }
                        if(eventLat != Double.MAX_VALUE
                                && eventLon != Double.MAX_VALUE) {
                            Double[] evlatlon = new Double[] {eventLat, eventLon};
                            List<Double[]> stalatlonList = new ArrayList<>();
                            stalatlonList.add(new Double[] {stationLat, stationLon});
                            calcEventStation(evlatlon, stalatlonList);
                            printResult(getWriter());
                        }
                        readMode = 'd';
                        break;
                    case 'm':
                        // change model
                        tokenIn.ordinaryChars('0', '9');
                        tokenIn.wordChars('0', '9');
                        tokenIn.ordinaryChars('.', '.');
                        tokenIn.wordChars('.', '.');
                        tokenIn.ordinaryChars('-', '-');
                        tokenIn.wordChars('-', '-');
                        String oldModelName = modelName;
                        TauModel oldTMod = tMod;
                        TauModel oldTModDepth = tModDepth;
                        System.out.print("Enter model name: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_WORD) {
                            modelName = tokenIn.sval;
                        }
                        tokenIn.ordinaryChars('0', '9');
                        tokenIn.ordinaryChars('.', '.');
                        tokenIn.ordinaryChars('-', '-');
                        tokenIn.parseNumbers();
                        if(!modelName.equals(oldModelName)) {
                            try {
                                readTauModel();
                                setSourceDepth(depth);
                            } catch(TauModelException e) {
                                if (e.getCause() instanceof InvalidClassException) {
                                    Alert.warning("Model file "
                                                  + modelName
                                                  + " is not compatible with the current version.",
                                          "Recreate using taup_create. Still using model "
                                                  + oldModelName + ".");
                                } else {
                                    Alert.warning("I can't load model file "
                                                  + modelName, "Still using model "
                                                  + oldModelName + ".");
                                }
                                modelName = oldModelName;
                                tMod = oldTMod;
                                tModDepth = oldTModDepth;
                            }
                        }
                        readMode = 'd';
                        break;
                    case 't':
                        System.out.print("Enter takeoff angle (deg): ");
                        // takeoff angle
                        double takeoffAngle;
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            takeoffAngle = tokenIn.nval;
                            calcTakeoff(takeoffAngle);
                            printResult(getWriter());
                        } else {
                            Alert.warning("Expected a number.", "got "
                                    + tokenIn + " instead.");
                            printHelp();
                            break;
                        }
                        readMode = 'd';
                        break;
                    case 'z':
                        System.out.print("Enter ray parameter (s/deg): ");
                        // takeoff angle
                        double shootRayp;
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            shootRayp = tokenIn.nval;
                            calcRayParameter(shootRayp/SphericalCoords.dtor);
                            printResult(getWriter());
                        } else {
                            Alert.warning("Expected a number.", "got "
                                    + tokenIn + " instead.");
                            printHelp();
                            break;
                        }
                        readMode = 'd';
                        break;
                    case 'q':
                        return;
                }
            } while(tokenIn.ttype == StreamTokenizer.TT_NUMBER
                    || tokenIn.ttype != StreamTokenizer.TT_WORD
                    || (tokenIn.ttype == StreamTokenizer.TT_WORD && !tokenIn.sval.equalsIgnoreCase("q")));
        }
    }

    public void destroy() throws TauPException {
        if(writer != null) {
            writer.close();
            writer = null;
        }
    }

    public String getStdUsageHead() {
        return TauP_Tool.getStdUsageHead(this.getClass());
    }

    /** Prints the command line arguments common to all TauP tools. */
    public String getStdUsage() {
        return getStdUsageHead()
        + getPhaseUsage()
        + getDistanceUsage();
    }

    public String getDistanceUsage() {
        return "Distance is given by:\n\n"
                + "-deg degrees       -- distance in degrees,\n"
                + "-km kilometers     -- distance in kilometers,\n"
                + "                      assumes radius of earth is 6371km,\n\n"
                + "or by giving the station and event latitude and lonitude,\n"
                + "                      assumes a spherical earth,\n\n"
                + "-sta[tion] lat lon -- sets the station latitude and longitude\n"
                + "-evt       lat lon -- sets the event latitude and longitude\n\n"
                + "or by giving the takeoff angle,\n"
                + "--takeoff angle    -- takeoff angle from the source\n"
                + "                      zero is down, 90 horizontal, 180 is up\n\n"
                + "or by giving the ray parameter,\n"
                + "--shootray param   -- ray parameter from the source in s/deg\n"
                + "                      up or down is determined by the phase\n\n\n"

        ;
    }

    public String getLimitUsage() {
        return "--first            -- only output the first arrival for each phase, no triplications\n"
                    + "--rayp             -- only output the ray parameter\n"
                    + "--time             -- only output travel time\n"
                    + "--rel phasename    -- also output relative travel time\n\n"
                    + "--json             -- output travel times as json\n\n"
        ;
    }

    public String getUsage() {
        StringBuffer buf = new StringBuffer();
        buf.append(getStdUsage());
        buf.append(getLimitUsage());
        buf.append(getStdUsageTail());
        return buf.toString();
    }

    /**
     * Allows TauP_Time to run as an application. Creates an instance of
     * TauP_Time.
     *
     * ToolRun.main should be used instead.
     */
    public static void main(String[] args) throws IOException {
        ToolRun.legacyRunTool(ToolRun.TIME, args);
    }

}
