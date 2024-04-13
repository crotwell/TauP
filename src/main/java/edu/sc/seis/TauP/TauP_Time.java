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

import edu.sc.seis.TauP.cli.OutputTypes;
import edu.sc.seis.TauP.cli.TextOutputTypeArgs;
import edu.sc.seis.seisFile.Location;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import picocli.CommandLine;

import java.io.*;
import java.util.*;

/**
 * Calculate travel times for different branches using linear interpolation
 * between known slowness samples.
 *
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * @author H. Philip Crotwell
 */
@CommandLine.Command(name = "time", description = "calculate travel times for seismic phases in an earth model")
public class TauP_Time extends TauP_AbstractRayTool {

    @CommandLine.Option(names = {"--rayp", "--onlyrayp"}, description = "only output the ray parameter")
    protected boolean onlyPrintRayP = false;

    @CommandLine.Option(names = {"--time", "--onlytime"}, description = "only output travel time")
    protected boolean onlyPrintTime = false;

    @CommandLine.Option(names = {"--first", "--onlyfirst"}, description = "only output the first arrival for each phase, no triplications")
    protected boolean onlyFirst = false;

    @CommandLine.Option(names = "--amp", description = "amplitude factor for each phase")
    protected boolean withAmplitude = false;

    @CommandLine.Option(names = "--rel", split = ",", description = "times relative to the first of the given phases")
    protected List<String> relativePhaseName = new ArrayList<>();

    protected List<SeismicPhase> relativePhaseList = new ArrayList<>();

    @CommandLine.Mixin
    TextOutputTypeArgs outputTypeArgs = new TextOutputTypeArgs();

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOuputFormat();
    }

    @Override
    public String getOutFileExtension() {
        return outputTypeArgs.getOutFileExtension();
    }

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
        modelArgs.setModelName(modelName);
        setDefaultOutputFormat();
    }

    @Override
    public String[] allowedOutputFormats() {
        String[] formats = {OutputTypes.TEXT, OutputTypes.JSON};
        return formats;
    }
    @Override
    public void setDefaultOutputFormat() {
        setOutputFormat(OutputTypes.TEXT);
        outputTypeArgs.setOutFileBase("stdout");
    }



    /* Normal methods */


    @Deprecated
    public List<Arrival> calculate(double degrees) throws TauPException {
        List<DistanceRay> dList = Arrays.asList( DistanceRay.ofDegrees(degrees));
        return calculate(dList);
    }

    @Deprecated
    public List<Arrival> calculate(List<DistanceRay> distanceRays) throws TauPException {
        return calcTime(distanceRays);
    }

    public Arrival calculateRelativeArrival(double degrees) throws TauModelException {
        Arrival relativeArrival = null;
        if (relativePhaseName.size() != 0) {
            List<SeismicPhase> relPhases = new ArrayList<SeismicPhase>();
            for (String sName : relativePhaseName) {
                try {
                    List<SeismicPhase> calcRelPhaseList = SeismicPhaseFactory.createSeismicPhases(
                            sName,
                            modelArgs.depthCorrected(),
                            this.getSourceDepth(),
                            this.getReceiverDepth(),
                            this.getScatterer(),
                            DEBUG);
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
    public List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException {
        List<Arrival> arrivals = new ArrayList<>();
        modelArgs.depthCorrected();
        for (SeismicPhase phase : phaseList) {
            for (RayCalculateable shoot : shootables) {
                arrivals.addAll(shoot.calculate(phase));
            }
        }

        if(relativePhaseList.size() != 0) {
            Set<Double> distList = new HashSet<>();
            for (Arrival arr : arrivals) {
                distList.add(arr.getModuloDist());
            }
            HashMap<Double, Arrival> earliestAtDist = new HashMap<>();
            for (Double dist : distList) {
                DistanceRay distRay = DistanceRay.ofRadians(dist);
                List<Arrival> relativeArrivals = new ArrayList<>();
                for (SeismicPhase relPhase : relativePhaseList) {
                    relativeArrivals.addAll(relPhase.calcTime(distRay));
                }
                relativeArrivals = Arrival.sortArrivals(relativeArrivals);
                if (relativeArrivals.size() != 0) {
                    Arrival earliest = relativeArrivals.get(0);
                    earliestAtDist.put(dist, earliest);
                } else {
                    earliestAtDist.put(dist, null);
                }
            }
            for (Arrival arrival : arrivals) {
                for (Double dist : earliestAtDist.keySet()) {
                    if (dist == arrival.getModuloDist()) {
                        Arrival earliest =earliestAtDist.get(dist);
                        arrival.setRelativeToArrival(earliest);
                    }
                }
            }
        }
        return Arrival.sortArrivals(arrivals);
    }

    /**
     * recalculates the given phases using a possibly new or changed tau model.
     * This should not need to be called by outside classes as it is called by
     * depthCorrect, and calculate.
     */
    @Override
    protected synchronized void recalcPhases() throws TauModelException {
        super.recalcPhases();
        relativePhaseList = new ArrayList<>();
        if (relativePhaseName.size() != 0) {
            for (String sName : relativePhaseName) {
                try {
                    List<SeismicPhase> calcRelPhaseList = SeismicPhaseFactory.createSeismicPhases(
                            sName,
                            modelArgs.depthCorrected(),
                            this.getSourceDepth(),
                            this.getReceiverDepth(),
                            this.getScatterer(),
                            DEBUG);
                    relativePhaseList.addAll(calcRelPhaseList);
                } catch (ScatterArrivalFailException e) {
                    Alert.warning(e.getMessage(),
                            "    Skipping this relative phase");
                    if (verbose || DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Deprecated
    public List<Arrival> calcTime(List<DistanceRay> degreesList) throws TauPException {
        validateArguments();
        modelArgs.depthCorrected();
        SeismicPhase phase;
        List<Arrival> arrivalList = new ArrayList<>();
        List<SeismicPhase> phaseList = getSeismicPhases();
        for (DistanceRay distVal : degreesList) {
            double degrees = distVal.getDegrees(getRadiusOfEarth());
            for (int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                phase = phaseList.get(phaseNum);
                List<Arrival> phaseArrivals = phase.calcTime(distVal);
                Arrival relativeArrival = calculateRelativeArrival(degrees);
                for (Arrival arrival : phaseArrivals) {
                    arrival.setRelativeToArrival(relativeArrival);
                }
                if (!onlyFirst) {
                    arrivalList.addAll(phaseArrivals);
                } else {
                    if (phaseArrivals.size() > 0) {
                        arrivalList.add(phaseArrivals.get(0));
                    }
                }
            }
        }
        return Arrival.sortArrivals(arrivalList);
    }


    @Override
    public void printResult(PrintWriter out, List<Arrival> arrivalList) throws IOException, TauModelException {
        if (getOutputFormat().equals(OutputTypes.JSON)) {
            printResultJSON(out, arrivalList);
        } else {
            printResultText(out, arrivalList);
        }
        out.flush();
    }

    public void printResultText(PrintWriter out, List<Arrival> arrivalList) throws IOException {
        Arrival currArrival;
        int maxNameLength = 5;
        int maxPuristNameLength = 5;
        for(int j = 0; j < arrivalList.size(); j++) {
            if(arrivalList.get(j).getName().length() > maxNameLength) {
                maxNameLength = arrivalList.get(j).getName()
                        .length();
            }
            if(arrivalList.get(j).getPuristName().length() > maxPuristNameLength) {
                maxPuristNameLength = arrivalList.get(j).getPuristName()
                        .length();
            }
        }
        //Format phaseFormat = new Format("%-" + maxNameLength + "s");
        String phaseFormat = "%-" + maxNameLength + "s";
        //Format phasePuristFormat = new Format("%-" + maxPuristNameLength + "s");
        String phasePuristFormat = "%-" + maxPuristNameLength + "s";
        if(!(onlyPrintRayP || onlyPrintTime)) {
            String modelLine =  "\nModel: " + modelArgs.getModelName();
            if (getReceiverDepth() != 0.0) {
                modelLine += "  Receiver Depth: "+getReceiverDepth()+" km";
            }
            if (getScatterer() != null && getScatterer().dist.getDegrees(getRadiusOfEarth()) != 0.0) {
                modelLine += "  Scatter Depth: "+ getScattererDepth()+" km Dist: "+ getScatterer().dist.getDegrees(getRadiusOfEarth());
            }
            out.println(modelLine);
            String lineOne = "Distance   Depth   " + String.format(phaseFormat, "Phase")
                    + "   Travel    Ray Param  Takeoff  Incident  Purist   "+String.format(phasePuristFormat, "Purist");
            String lineTwo = "  (deg)     (km)   " + String.format(phaseFormat, "Name ")
                    + "   Time (s)  p (s/deg)   (deg)    (deg)   Distance   "+String.format(phasePuristFormat, "Name");
            if (withAmplitude) {
                lineOne += "    Amp   ";
                lineTwo += "  Factor";
            }
            if (relativePhaseName.size() != 0) {
                String allRelPhase = "";
                for (String s : relativePhaseName) {
                    allRelPhase += s+",";
                }
                allRelPhase = allRelPhase.substring(0, allRelPhase.length()-1);
                lineOne += " Relative to";
                for (int s=0; s<(11-allRelPhase.length())/2;s++) {
                    lineTwo += " ";
                }
                lineTwo += "  "+String.format(phaseFormat, allRelPhase);
            }
            out.println(lineOne);
            out.println(lineTwo);
            for(int i = 0; i < Math.max(lineOne.length(), lineTwo.length()); i++) {
                out.write("-");
            }
            out.write("\n");
            for(int j = 0; j < arrivalList.size(); j++) {
                currArrival = arrivalList.get(j);
                out.print(Outputs.formatDistance(currArrival.getSearchDistDeg()));
                out.print(Outputs.formatDepth(currArrival.getPhase().getSourceDepth()) + "   ");
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
                if (withAmplitude) {
                    try {
                        double ampFactorPSV = currArrival.getAmplitudeFactorPSV();
                        double ampFactorSH = currArrival.getAmplitudeFactorSH();
                        out.print(" " + Outputs.formatAmpFactor(ampFactorPSV) + "," + Outputs.formatAmpFactor(ampFactorSH));
                    } catch (SlownessModelException | TauModelException | VelocityModelException e) {
                        throw new RuntimeException("SHould not happen", e);
                    }
                }

                if (relativePhaseName.size() != 0) {
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
            for(int j = 0; j < arrivalList.size(); j++) {
                currArrival = arrivalList.get(j);
                out.print((float) (currArrival.getTime()) + " ");
            }
            out.println();
        } else if(onlyPrintRayP) {
            for(int j = 0; j < arrivalList.size(); j++) {
                currArrival = arrivalList.get(j);
                out.write((float) (Math.PI / 180.0 * currArrival.getRayParam())
                        + " ");
            }
            out.println();
        }
        out.println();
        out.flush();
    }

    public void printResultJSON(PrintWriter out, List<Arrival> arrivalList) throws IOException, TauModelException {
        writeJSON(out, "", arrivalList);
    }

    public void writeJSON(PrintWriter pw, String indent, List<Arrival> arrivalList) throws IOException, TauModelException {
        writeJSON(pw, indent,
                getTauModelName(),
                getSourceDepth(),
                getReceiverDepth(),
                getSeismicPhases(),
                arrivalList);
    }

    public static void writeJSON(PrintWriter pw, String indent,
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
                System.err.println("Erro in json: "+ arrival);
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
            Arrival currArrival = arrivals.get(j);
            outArrivals.put(currArrival.asJSONObject());
        }
        return out;
    }

    /**
     * preforms intialization of the tool. Properties are queried for the
     * default model to load, source depth to use, phases to use, etc.
     */
    public void init() throws TauPException {
        super.init();
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

    public void calcAndPrint(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException, IOException {
        List<Arrival> arrivalList = calcAll(phaseList, shootables);
        PrintWriter writer = outputTypeArgs.createWriter();
        printResult(writer, arrivalList);
        writer.close();
    }

    public void start() throws IOException, TauPException {
        List<RayCalculateable> distanceValues = getDistanceArgs().getRayCalculatables();
        if((distanceValues.size() != 0)) {
            /* enough info given on cmd line, so just do one calc. */
            List<Arrival> arrivalList = calcAll(getSeismicPhases(), distanceValues);
            PrintWriter writer = outputTypeArgs.createWriter();
            printResult(writer, arrivalList);
            writer.close();
        } else {
            /* interactive mode... */
            long prevTime = 0;
            long currTime;
            char readMode = 'd';
            double tempDepth = modelArgs.getSourceDepth();
            setSourceDepth(modelArgs.getSourceDepth());
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
                                || tempDepth > getRadiusOfEarth()) {
                            Alert.warning("Depth must be >= 0.0 and <= tMod.getRadiusOfEarth().",
                                          "depth = " + tempDepth
                                                  + " getRadiusOfEarth= "
                                                  + getRadiusOfEarth());
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
                            getDistanceArgs().clear();
                            getDistanceArgs().setDegreeList(Arrays.asList(degrees));
                            calcAndPrint(getSeismicPhases(), distanceValues);
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
                        calcAndPrint(getSeismicPhases(), distanceValues);
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
                            getDistanceArgs().setAzimuth(tokenIn.nval);
                            getDistanceArgs().clearStationLatLon();
                        } else {
                            Alert.warning("Expected a number.", "got "
                                    + tokenIn + " instead.");
                            printHelp();
                            break;
                        }
                        if(! getDistanceArgs().hasEventLatLon()) {
                            readMode = 'e';
                        } else if(getDistanceArgs().getRayCalculatables().size() == 0) {
                            readMode = 'd';
                        } else {
                            calcAndPrint(getSeismicPhases(), distanceValues);
                        }
                        readMode = 'd';
                        break;
                    case 'b':
                        // event to station back azimuth (ie station to event
                        // azimuth)
                        System.out.print("Enter back azimuth: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            getDistanceArgs().setBackAzimuth(tokenIn.nval);
                            getDistanceArgs().clearEventLatLon();
                            if(DEBUG) {
                                Alert.info("backAzimuth=" + tokenIn.nval);
                            }
                        } else {
                            Alert.warning("Expected a number.", "got "
                                    + tokenIn + " instead");
                            printHelp();
                            break;
                        }
                        if( ! getDistanceArgs().hasStationLatLon()) {
                            readMode = 's';
                        } else if(getDistanceArgs().getRayCalculatables().size() == 0) {
                            readMode = 'd';
                        } else {
                            calcAndPrint(getSeismicPhases(), distanceValues);
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
                                Alert.info("eventLat=" + evLat);
                            }
                            tokenIn.nextToken();
                            if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                                double evLon = tokenIn.nval;
                                getDistanceArgs().getEventList().add( new Location(evLat, evLon));
                                if(DEBUG) {
                                    Alert.info("eventLon=" + evLon);
                                }
                            } else {
                                printHelp();
                            }
                        } else {
                            printHelp();
                        }
                        if(getDistanceArgs().hasStationLatLon()) {
                            calcAndPrint(getSeismicPhases(), distanceValues);
                        }
                        readMode = 'd';
                        break;
                    case 's':
                        // station lat and lon
                        System.out.print("Enter station lat and lon: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            double stationLat = tokenIn.nval;
                            if(DEBUG) {
                                Alert.info("stationLat=" + tokenIn.nval);
                            }
                            tokenIn.nextToken();
                            if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                                double stationLon = tokenIn.nval;
                                if(DEBUG) {
                                    Alert.info("stationLon=" + tokenIn.nval);
                                }
                                getDistanceArgs().getStationList().clear();
                                getDistanceArgs().getStationList().add(new Location(stationLat, stationLon));
                            } else {
                                printHelp();
                                break;
                            }
                        } else {
                            printHelp();
                            break;
                        }
                        if(getDistanceArgs().hasEventLatLon() || getDistanceArgs().getBackAzimuth() != null) {
                            calcAndPrint(getSeismicPhases(), distanceValues);
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
                        String oldModelName = modelArgs.getModelName();
                        TauModel oldTMod = modelArgs.getTauModel();
                        System.out.print("Enter model name: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_WORD) {
                            modelArgs.setModelName( tokenIn.sval);
                        }
                        tokenIn.ordinaryChars('0', '9');
                        tokenIn.ordinaryChars('.', '.');
                        tokenIn.ordinaryChars('-', '-');
                        tokenIn.parseNumbers();
                        readMode = 'd';
                        break;
                    case 't':
                        System.out.print("Enter takeoff angle (deg): ");
                        // takeoff angle
                        double takeoffAngle;
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            takeoffAngle = tokenIn.nval;
                            getDistanceArgs().setTakeoffAngles(Arrays.asList(takeoffAngle));
                            calcAndPrint(getSeismicPhases(), distanceValues);
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
                        // shoot a ray
                        double shootRayp;
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            shootRayp = tokenIn.nval;
                            getDistanceArgs().setShootRayParams(Arrays.asList(shootRayp));
                            calcAndPrint(getSeismicPhases(), distanceValues);
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
