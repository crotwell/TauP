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
package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.AbstractOutputTypeArgs;
import edu.sc.seis.TauP.cmdline.args.OutputTypes;
import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;
import edu.sc.seis.TauP.cmdline.args.TextOutputTypeArgs;
import edu.sc.seis.seisFile.Location;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import picocli.CommandLine;

import java.io.*;
import java.util.*;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.ABREV_SYNOPSIS;
import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;

/**
 * Calculate travel times for different branches using linear interpolation
 * between known slowness samples.
 *
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * @author H. Philip Crotwell
 */
@CommandLine.Command(name = "time",
        description = "Calculate travel times for seismic phases in an earth model.",
        optionListHeading = OPTIONS_HEADING,
        abbreviateSynopsis = ABREV_SYNOPSIS,
        usageHelpAutoWidth = true)
public class TauP_Time extends TauP_AbstractRayTool {

    @CommandLine.Option(names = {"--rayp", "--onlyrayp"}, description = "only output the ray parameter")
    protected boolean onlyPrintRayP = false;

    @CommandLine.Option(names = {"--time", "--onlytime"}, description = "only output travel time")
    protected boolean onlyPrintTime = false;

    @CommandLine.Option(names = {"--first", "--onlyfirst"}, description = "only output the first arrival for each phase, no triplications")
    protected boolean onlyFirst = false;

    @CommandLine.Option(names = "--amp", description = "show amplitude factor for each phase")
    public boolean withAmplitude = false;

    public boolean isWithAmplitude() {
        return withAmplitude;
    }

    @CommandLine.Mixin
    SeismicSourceArgs sourceArgs = new SeismicSourceArgs();

    @CommandLine.Option(names = "--rel", split = ",", paramLabel = "phase", description = "times relative to the first of the given phases")
    protected List<String> relativePhaseName = new ArrayList<>();

    protected List<SeismicPhase> relativePhaseList = new ArrayList<>();

    @CommandLine.Mixin
    TextOutputTypeArgs outputTypeArgs;

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOutputFormat();
    }

    @Override
    public String getOutFileExtension() {
        return outputTypeArgs.getOutFileExtension();
    }

    public TauP_Time() {
        super(new TextOutputTypeArgs(OutputTypes.TEXT, AbstractOutputTypeArgs.STDOUT_FILENAME));
        outputTypeArgs = (TextOutputTypeArgs)abstractOutputTypeArgs;
    }

    public TauP_Time(TauModel tMod)  {
        this();
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
        this();
        modelArgs.setModelName(modelName);
    }


    /* Normal methods */


    @Override
    public List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException {
        List<Arrival> arrivals = new ArrayList<>();
        modelArgs.depthCorrected();
        for (SeismicPhase phase : phaseList) {
            List<Arrival> phaseArrivals = new ArrayList<>();
            for (RayCalculateable shoot : shootables) {
                phaseArrivals.addAll(shoot.calculate(phase));
            }
            if (!onlyFirst) {
                arrivals.addAll(phaseArrivals);
            } else {
                if (!phaseArrivals.isEmpty()) {
                    arrivals.add(phaseArrivals.get(0));
                }
            }
        }
        if (isWithAmplitude()) {
            for (Arrival a : arrivals) {
                a.setSeismicMoment(sourceArgs.getMoment());
            }
        }

        if(!relativePhaseList.isEmpty()) {
            Set<Double> distList = new HashSet<>();
            for (Arrival arr : arrivals) {
                distList.add(arr.getModuloDist());
            }
            HashMap<Double, Arrival> earliestAtDist = new HashMap<>();
            for (Double dist : distList) {
                DistanceRay distRay = DistanceRay.ofRadians(dist);
                List<Arrival> relativeArrivals = new ArrayList<>();
                for (SeismicPhase relPhase : relativePhaseList) {
                    relativeArrivals.addAll(distRay.calculate(relPhase));
                }
                relativeArrivals = Arrival.sortArrivals(relativeArrivals);
                if (!relativeArrivals.isEmpty()) {
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
        if (!relativePhaseName.isEmpty()) {
            for (String sName : relativePhaseName) {
                try {
                    List<SeismicPhase> calcRelPhaseList = SeismicPhaseFactory.createSeismicPhases(
                            sName,
                            modelArgs.depthCorrected(),
                            modelArgs.getSourceDepth(),
                            modelArgs.getReceiverDepth(),
                            modelArgs.getScatterer(),
                            isDEBUG());
                    relativePhaseList.addAll(calcRelPhaseList);
                } catch (ScatterArrivalFailException e) {
                    Alert.warning(e.getMessage(),
                            "    Skipping this relative phase");
                    if (isVerbose() || isDEBUG()) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void printResult(PrintWriter out, List<Arrival> arrivalList) throws IOException, TauPException {
        if (getOutputFormat().equals(OutputTypes.JSON)) {
            printResultJSON(out, arrivalList);
        } else {
            printResultText(out, arrivalList);
        }
        out.flush();
    }

    public void printResultText(PrintWriter out, List<Arrival> arrivalList) {
        Arrival currArrival;
        int maxNameLength = 5;
        int maxPuristNameLength = 5;
        for (Arrival arrival : arrivalList) {
            if (arrival.getName().length() > maxNameLength) {
                maxNameLength = arrival.getName()
                        .length();
            }
            if (arrival.getPuristName().length() > maxPuristNameLength) {
                maxPuristNameLength = arrival.getPuristName()
                        .length();
            }
        }
        //Format phaseFormat = new Format("%-" + maxNameLength + "s");
        String phaseFormat = "%-" + maxNameLength + "s";
        //Format phasePuristFormat = new Format("%-" + maxPuristNameLength + "s");
        String phasePuristFormat = "%-" + maxPuristNameLength + "s";
        if(!(onlyPrintRayP || onlyPrintTime)) {
            String modelLine =  "\nModel: " + modelArgs.getModelName();
            if (modelArgs.getReceiverDepth() != 0.0) {
                modelLine += "  Receiver Depth: "+modelArgs.getReceiverDepth()+" km";
            }
            if (getScatterer() != null && getScatterer().dist.getDegrees(getRadiusOfEarth()) != 0.0) {
                modelLine += "  Scatter Depth: "+ getScattererDepth()+" km Dist: "+ getScatterer().dist.getDegrees(getRadiusOfEarth());
            }
            out.println(modelLine);
            String lineOne = "Distance   Depth   " + String.format(phaseFormat, "Phase")
                    + "   Travel    Ray Param  Takeoff  Incident  Purist   "+String.format(phasePuristFormat, "Purist");
            String lineTwo = "  (deg)     (km)   " + String.format(phaseFormat, "Name ")
                    + "   Time (s)  p (s/deg)   (deg)    (deg)   Distance   "+String.format(phasePuristFormat, "Name");
            if (isWithAmplitude()) {
                lineOne += "    Amp  ~"+Outputs.formatDistanceNoPad(sourceArgs.getMw())+" Mw";
                lineTwo += "  Factor PSv   Sh";
            }
            if (!relativePhaseName.isEmpty()) {
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
            for (Arrival arrival : arrivalList) {
                currArrival = arrival;
                out.print(Outputs.formatDistance(currArrival.getSearchDistDeg()));
                out.print(Outputs.formatDepth(currArrival.getPhase().getSourceDepth()) + "   ");
                out.print(String.format(phaseFormat, currArrival.getName()));
                out.print("  "
                        + Outputs.formatTime(currArrival.getTime())
                        + "  "
                        + Outputs.formatRayParam(currArrival.getRayParam() / SphericalCoords.RtoD) + "  ");
                out.print(Outputs.formatDistance(currArrival.getTakeoffAngleDegree()) + " ");
                out.print(Outputs.formatDistance(currArrival.getIncidentAngleDegree()) + " ");
                out.print(Outputs.formatDistance(currArrival.getDistDeg()));
                if (currArrival.getName().equals(currArrival.getPuristName())) {
                    out.print("   = ");
                } else {
                    out.print("   * ");
                }
                out.print(String.format(phasePuristFormat, currArrival.getPuristName()));
                if (isWithAmplitude()) {
                    try {
                        double ampFactorPSV = currArrival.getAmplitudeFactorPSV(sourceArgs.getMoment());
                        double ampFactorSH = currArrival.getAmplitudeFactorSH(sourceArgs.getMoment());
                        out.print(" " + Outputs.formatAmpFactor(ampFactorPSV) + " " + Outputs.formatAmpFactor(ampFactorSH));
                    } catch (SlownessModelException | TauModelException | VelocityModelException e) {
                        throw new RuntimeException("SHould not happen", e);
                    }
                }

                if (!relativePhaseName.isEmpty()) {
                    if (currArrival.isRelativeToArrival()) {
                        out.print(" " + Outputs.formatTime(currArrival.getTime() - currArrival.getRelativeToArrival().getTime()));
                        out.print(" +" + String.format(phaseFormat, currArrival.getRelativeToArrival().getName()));
                    } else {
                        out.print(String.format(phaseFormat, " no arrival"));
                    }
                }

                out.println();
            }
        } else if(onlyPrintTime) {
            for (Arrival arrival : arrivalList) {
                currArrival = arrival;
                out.print((float) (currArrival.getTime()) + " ");
            }
            out.println();
        } else if(onlyPrintRayP) {
            for (Arrival arrival : arrivalList) {
                currArrival = arrival;
                out.write((float) (Math.PI / 180.0 * currArrival.getRayParam())
                        + " ");
            }
            out.println();
        }
        out.println();
        out.flush();
    }

    public void printResultJSON(PrintWriter out, List<Arrival> arrivalList) throws TauModelException, TauPException {
        writeJSON(out, "", arrivalList);
    }

    public void writeJSON(PrintWriter pw, String indent, List<Arrival> arrivalList) throws TauModelException {
        writeJSON(pw, indent,
                getTauModelName(),
                modelArgs.getSourceDepth(),
                modelArgs.getReceiverDepth(),
                getSeismicPhases(),
                arrivalList,
                isWithAmplitude(),
                sourceArgs.getMw());
    }

    public static void writeJSON(PrintWriter pw, String indent,
                                 String modelName,
                                 double depth,
                                 double receiverDepth,
                                 List<SeismicPhase> phases,
                                 List<Arrival> arrivals) {
        writeJSON(pw, indent, modelName, depth, receiverDepth, phases, arrivals,  false, 4.0f);
    }
    public static void writeJSON(PrintWriter pw, String indent,
                                 String modelName,
                                 double depth,
                                 double receiverDepth,
                                 List<SeismicPhase> phases,
                                 List<Arrival> arrivals,
                                 boolean withAmplitude,
                                 float Mw) {
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
        if (withAmplitude) {
            pw.write(innerIndent+JSONWriter.valueToString("Mw")+": "+JSONWriter.valueToString(Mw)+","+NL);
        }
        pw.write(innerIndent+JSONWriter.valueToString("arrivals")+": ["+NL);
        first = true;
        for (Arrival arrival : arrivals) {
            if (first) {
                first = false;
            } else {
                pw.write(","+NL);
            }
            try {
                arrival.writeJSON(pw, innerIndent + "  ", withAmplitude);
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
                                      List<PhaseName> phases,
                                      List<Arrival> arrivals) {
        JSONObject out = baseResultAsJSONObject( modelName, depth,  receiverDepth, phases);
        JSONArray outArrivals = new JSONArray();
        out.put("arrivals", outArrivals);
        for (Arrival currArrival : arrivals) {
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
        PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
        printResult(writer, arrivalList);
        writer.close();
    }

    public void start() throws IOException, TauPException {
        List<RayCalculateable> distanceValues = getDistanceArgs().getRayCalculatables();
        List<Arrival> arrivalList = calcAll(getSeismicPhases(), distanceValues);
        PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
        printResult(writer, arrivalList);
        writer.close();
    }

    @Override
    public void destroy() throws TauPException {

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
