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

import com.google.gson.GsonBuilder;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.*;
import edu.sc.seis.TauP.gson.ArrivalSerializer;
import edu.sc.seis.TauP.gson.GsonUtil;
import picocli.CommandLine;

import java.io.*;
import java.util.*;

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
        usageHelpAutoWidth = true)
public class TauP_Time extends TauP_AbstractRayTool {

    @CommandLine.Option(names = {"--rayp", "--onlyrayp"}, description = "only output the ray parameter")
    protected boolean onlyPrintRayP = false;

    @CommandLine.Option(names = {"--time", "--onlytime"}, description = "only output travel time")
    protected boolean onlyPrintTime = false;

    @CommandLine.Option(names = {"--first", "--onlyfirst"}, description = "only output the first arrival for each phase, no triplications")
    protected boolean onlyFirst = false;

    @CommandLine.Mixin
    AmplitudeArgs sourceArgs = new AmplitudeArgs();

    public AmplitudeArgs getSourceArgs() {
        return sourceArgs;
    }

    public boolean isWithAmplitude() {
        return getSourceArgs().isWithAmplitude();
    }

    @CommandLine.Option(names = "--rel", split = ",",
            paramLabel = "phase",
            description = "times relative to the first of the given phases")
    protected List<String> relativePhaseName = new ArrayList<>();

    protected List<SeismicPhase> relativePhaseList = new ArrayList<>();

    @CommandLine.Mixin
    TextCsvOutputTypeArgs outputTypeArgs;

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOutputFormat();
    }

    @Override
    public String getOutFileExtension() {
        return outputTypeArgs.getOutFileExtension();
    }

    public TauP_Time() {
        super(new TextCsvOutputTypeArgs(OutputTypes.TEXT, AbstractOutputTypeArgs.STDOUT_FILENAME));
        outputTypeArgs = (TextCsvOutputTypeArgs)abstractOutputTypeArgs;
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
    public List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> rayCalcList) throws TauPException {
        List<Arrival> arrivals = internalCalcAll(phaseList, rayCalcList, onlyFirst);

        if(!relativePhaseList.isEmpty() && ! arrivals.isEmpty()) {
            Arrival.sortArrivalsBySourceReceiverDepth(arrivals);
            double calcDepth = -1;
            double calcRecDepth = -1;
            List<SeismicPhase> relPhaseList = new ArrayList<>();
            for (Arrival arrival : arrivals) {
                if (calcDepth != arrival.getSourceDepth() || calcRecDepth != arrival.getReceiverDepth()) {
                    // not same source depth or receiver depth, so need to recalc relative phases
                    relPhaseList = calcRelativeSeismicPhases(arrival.getTauModel(),
                            arrival.getReceiverDepth(),
                            getScatterer());
                    calcDepth = arrivals.get(0).getSourceDepth();
                    calcRecDepth = arrivals.get(0).getReceiverDepth();
                }
                DistanceRay distRay = DistanceRay.ofRadians(arrival.getModuloDist());
                List<Arrival> relativeArrivals = new ArrayList<>();
                for (SeismicPhase relPhase : relPhaseList) {
                    relativeArrivals.addAll(distRay.calculate(relPhase));
                }
                if ( ! relativeArrivals.isEmpty()) {
                    Arrival.sortArrivals(relativeArrivals);
                    arrival.setRelativeToArrival(relativeArrivals.get(0));
                }
            }
        }
        Arrival.sortArrivals(arrivals);
        return arrivals;
    }

    /**
     * Check if ray source and receiver depths are compatible with the phase, to avoid duplicate results
     * when using earthquakes or station/channels that have depths.
     * @param rayCalc ray to check if has source/receiver depths
     * @param phase phase to see if compatible
     * @return true if source and receiver depths are compatible
     */
    public static boolean isRayOkForPhase(RayCalculateable rayCalc, SeismicPhase phase) {
        return ( ! rayCalc.hasSourceDepth() || rayCalc.getSourceDepth() == phase.getSourceDepth())
                && ( ! rayCalc.hasReceiverDepth() || rayCalc.getReceiverDepth() == phase.getReceiverDepth());
    }

    static List<Arrival> internalCalcAll(List<SeismicPhase> phaseList,
                                                List<RayCalculateable> rayCalcList,
                                                boolean onlyFirst) throws TauPException {
        List<Arrival> arrivals = new ArrayList<>();
        for (SeismicPhase phase : phaseList) {
            for (RayCalculateable rayCalc : rayCalcList) {
                if (isRayOkForPhase(rayCalc, phase)) {
                    List<Arrival> rayArrivals = rayCalc.calculate(phase);
                    Arrival.sortArrivals(rayArrivals);
                    if (onlyFirst && ! rayArrivals.isEmpty()) {
                        rayArrivals = List.of(rayArrivals.get(0));
                    }
                    arrivals.addAll(rayArrivals);
                }
            }
        }

        Arrival.sortArrivals(arrivals);
        return arrivals;
    }

    /**
     * recalculates the given phases using a possibly new or changed tau model.
     * Also calculates the relativePhase list.
     */
    @Override
    public synchronized List<SeismicPhase> calcSeismicPhases(double sourceDepth, List<Double> receiverDepths, Scatterer scatterer) throws TauModelException {
        List<SeismicPhase> phaseList = super.calcSeismicPhases(sourceDepth, receiverDepths, scatterer);
        relativePhaseList = new ArrayList<>();
        if (!relativePhaseName.isEmpty()) {
            for (Double recDepth : receiverDepths) {
                TauModel sourceReceiverTMod = modelArgs.depthCorrected(sourceDepth).splitBranch(recDepth);
                for (String sName : relativePhaseName) {
                    try {
                        List<SeismicPhase> calcRelPhaseList = SeismicPhaseFactory.createSeismicPhases(
                                sName,
                                sourceReceiverTMod,
                                sourceDepth,
                                recDepth,
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
        return phaseList;
    }

    public List<SeismicPhase> calcRelativeSeismicPhases(TauModel tauModel, double receiverDepth, Scatterer scatterer) throws TauModelException {
        List<SeismicPhase> relativePhaseList = new ArrayList<>();
        for (String sName : relativePhaseName) {
            try {
                List<SeismicPhase> calcRelPhaseList = SeismicPhaseFactory.createSeismicPhases(
                        sName,
                        tauModel,
                        tauModel.getSourceDepth(),
                        receiverDepth,
                        scatterer,
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
        return relativePhaseList;
    }

    public GsonBuilder createGsonBuilder() {
        GsonBuilder gsonBld = GsonUtil.createGsonBuilder();
        gsonBld.registerTypeAdapter(Arrival.class,
                new ArrivalSerializer(false, false, isWithAmplitude()));
        return gsonBld;
    }

    @Override
    public void printResult(PrintWriter out, List<Arrival> arrivalList) throws TauPException {
        if (getOutputFormat().equals(OutputTypes.JSON)) {
            TimeResult result = createTimeResult(isWithAmplitude(), sourceArgs.getSeismicSource(), arrivalList);
            GsonBuilder gsonBld = createGsonBuilder();
            out.println(gsonBld.create().toJson(result));
        } else if (getOutputFormat().equals(OutputTypes.HTML)) {
            printResultHtml(out, arrivalList);
        } else if (getOutputFormat().equals(OutputTypes.CSV)) {
            printResultCsv(out, arrivalList);
        } else {
            printResultText(out, arrivalList);
        }
        out.flush();
    }

    public void printResultText(PrintWriter out, List<Arrival> arrivalList) {
        printArrivalsAsText(out, arrivalList,
                modelArgs.getModelName(),
                getScatterer(),
                onlyPrintTime, onlyPrintRayP,
                isWithAmplitude(), sourceArgs,
                relativePhaseName);
    }

    public void printResultHtml(PrintWriter out, List<Arrival> arrivalList) throws TauPException {
        printArrivalsAsHtml(out, arrivalList,
                modelArgs.getModelName(),
                getScatterer(),
                isWithAmplitude(), sourceArgs,
                relativePhaseName, "Time");
    }

    public static List<String> createModelHeaderLine(String modelName,
                                                     Scatterer scatterer) {
        List<String> modelLine = new ArrayList<>(List.of("Model: ", modelName));

        if (scatterer != null && scatterer.depth != 0.0) {
            modelLine.addAll(List.of("  Scatter Depth: ", scatterer.depth+" km", " Dist: ", scatterer.getDistanceDegree()+" deg"));
        }
        return modelLine;
    }

    public static List<List<String>> createHeaderLines(List<Arrival> arrivalList,
                                                       String modelName,
                                                       Scatterer scatterer,
                                                       boolean withAmplitude, SeismicSourceArgs sourceArgs,
                                                       List<String> relativePhaseName,
                                                       String phaseFormat,
                                                       String phasePuristFormat) {
        List<String> modelLine = createModelHeaderLine(modelName, scatterer);

        List<String> lineOne = new ArrayList<>(List.of("Distance   ", "Depth   ",
                String.format(phaseFormat, "Phase")+ "   ",
                "Travel    ", "Ray Param  ", "Takeoff  ", "Incident ", "Station   ", "Purist   ",
                "", String.format(phasePuristFormat, "Purist")));
        List<String> lineTwo = new ArrayList<>(List.of("  (deg)    ", " (km)   ",
                String.format(phaseFormat, "Name ")+ "   ",
                "Time (s)  ", "p (s/deg)  ", " (deg)   ", " (deg)   ", " (km)     ", "Distance ",
                "", String.format(phasePuristFormat, "Name  ")));
        if (withAmplitude) {
            lineOne.addAll(List.of("    Amp  ", "~"+Outputs.formatDistanceNoPad(sourceArgs.getMw())+" Mw  "));
            lineTwo.addAll(List.of("  Factor PSv", "   Sh"));
        }
        if (!relativePhaseName.isEmpty()) {
            String allRelPhase = "";
            for (String s : relativePhaseName) {
                allRelPhase += s+",";
            }
            allRelPhase = allRelPhase.substring(0, allRelPhase.length()-1);
            lineOne.add(" Relative");
            String space = " ";
            lineTwo.add(space.repeat((11-allRelPhase.length())/2)+"  "+String.format(phaseFormat, allRelPhase));
            lineOne.add(" to");
            lineTwo.add("");
        }
        for (Arrival arrival : arrivalList) {
            if (arrival.getRayCalculateable().hasDescription()) {
                lineOne.add(" Description");
                lineTwo.add("            ");
                break;
            }
        }
        for (int i = 0; i < lineOne.size(); i++) {
            if (lineOne.get(i).length() != lineTwo.get(i).length()) {
                System.err.println("now same length: "+i+" '"+lineOne.get(i)+"' '"+lineTwo.get(i)+"'"+" "+lineOne.get(i).length()+" != "+lineTwo.get(i).length());
            }
        }
        return List.of(modelLine, lineOne, lineTwo);
    }

    /**
     * Combines two header line lists into single single list by concate each pair.
     * @param headLines header lines, size 2
     * @return combined items
     */
    public static List<String> combineHeadLines(List<List<String>> headLines) {
        List<String> out = new ArrayList<>();
        // modelLine = headLines.get(0);
        List<String> head1 = headLines.get(1);
        List<String> head2 = headLines.get(2);
        for (int i = 0; i < head1.size(); i++) {
            String val = head1.get(i).trim() + " " + head2.get(i).trim();
            out.add(val);
        }
        return out;
    }

    public static void printArrivalsAsText(PrintWriter out,
                                           List<Arrival> arrivalList,
                                           String modelName,
                                           Scatterer scatterer,
                                           boolean onlyPrintTime, boolean onlyPrintRayP,
                                           boolean withAmplitude, SeismicSourceArgs sourceArgs,
                                           List<String> relativePhaseName) {
        int maxNameLength = 5;
        int maxPuristNameLength = 6;
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
            List<List<String>> headLines = createHeaderLines(arrivalList, modelName, scatterer,
                    withAmplitude, sourceArgs, relativePhaseName,
                    phaseFormat, phasePuristFormat);
            if (withAmplitude) {
                out.println(AmplitudeArgs.AMPLITUDE_WARNING);
            }
            String modelLine = String.join("", headLines.get(0));
            out.println("\n"+modelLine);
            String lineOne = String.join("", headLines.get(1));
            out.println(lineOne);
            String lineTwo = String.join("", headLines.get(2));
            out.println(lineTwo);
            StringBuilder dashes = new StringBuilder();
            dashes.append("-".repeat(Math.max(lineOne.length(), lineTwo.length())));
            out.write(dashes.append("\n").toString());
            for (Arrival arrival : arrivalList) {
                List<String> lineItems = arrival.asStringList(false,
                        phaseFormat, phasePuristFormat, withAmplitude, !relativePhaseName.isEmpty());
                // fix indentation
                out.println(String.join("", lineItems));
            }
        } else if(onlyPrintTime) {
            for (Arrival arrival : arrivalList) {
                out.print((float) (arrival.getTime()) + " ");
            }
            out.println();
        } else {
            // onlyPrintRayP must be true
            for (Arrival arrival : arrivalList) {
                out.write((float) (Math.PI / 180.0 * arrival.getRayParam())
                        + " ");
            }
            out.println();
        }
        out.println();
        out.flush();
    }

    public static void printArrivalsAsHtml(PrintWriter out,
                                           List<Arrival> arrivalList,
                                           String modelName,
                                           Scatterer scatterer,
                                           boolean withAmplitude, SeismicSourceArgs sourceArgs,
                                           List<String> relativePhaseName, String toolname) throws TauPException {
        String phaseFormat = "%s";
        List<List<String>> headLines = createHeaderLines(arrivalList, modelName, scatterer,
                withAmplitude, sourceArgs, relativePhaseName,
                phaseFormat, phaseFormat);

        List<String> mergedHeaders = combineHeadLines(headLines);

        HTMLUtil.createHtmlStart(out, "TauP "+toolname, HTMLUtil.createTableCSS(), true);
        if (withAmplitude) {
            out.println("<p>"+AmplitudeArgs.AMPLITUDE_WARNING+"</p>");
        }
        String modelLine = String.join("", headLines.get(0));
        out.println("<h5>"+modelLine+"</h5>");
        List<List<String>> values = new ArrayList<>();
        for (Arrival arrival : arrivalList) {
            List<String> lineItems = arrival.asStringList(false,
                    phaseFormat, phaseFormat, withAmplitude, !relativePhaseName.isEmpty());
            values.add(lineItems);
        }

        out.println(HTMLUtil.createBasicTable(mergedHeaders, values));
        HTMLUtil.addSortTableJS(out);
        out.println(HTMLUtil.createHtmlEnding());
        out.flush();
    }


    public void printResultCsv(PrintWriter out, List<Arrival> arrivalList) throws TauPException {
        printArrivalsAsCsv(out, arrivalList,
                modelArgs.getModelName(),
                getScatterer(),
                isWithAmplitude(), sourceArgs,
                relativePhaseName, "Time");
    }

    public static void printArrivalsAsCsv(PrintWriter out,
                                           List<Arrival> arrivalList,
                                           String modelName,
                                           Scatterer scatterer,
                                           boolean withAmplitude, SeismicSourceArgs sourceArgs,
                                           List<String> relativePhaseName, String toolname) throws TauPException {
        String phaseFormat = "%s";
        List<List<String>> headLines = createHeaderLines(arrivalList, modelName, scatterer,
                withAmplitude, sourceArgs, relativePhaseName,
                phaseFormat, phaseFormat);
        List<String> mergedHeaders = combineHeadLines(headLines);

        String comma = ",";
        boolean first = true;
        for (String val : mergedHeaders) {
            if (val.contains("\"") || val.contains(",")) {
                val = "\""+val.replaceAll("\"", "\"\"");
            }
            if (! first) {
                out.print(comma);
            }
            out.print(val);
            first = false;
        }
        // print model lines as more columns in header???
        for (String v : headLines.get(0)) {
            out.print(comma);
            out.print(v.trim());
        }
        out.println();

        for (Arrival arrival : arrivalList) {
            comma = ",";
            List<String> lineItems = arrival.asStringList(false,
                    phaseFormat, phaseFormat, withAmplitude, !relativePhaseName.isEmpty());
            for (int i = 0; i < lineItems.size(); i++) {
                String val = lineItems.get(i).trim();
                if (val.contains("\"") || val.contains(",")) {
                    val = "\""+val.replaceAll("\"", "\"\"");
                }
                if (i != 0) {
                    out.print(comma);
                }
                out.print(val);
            }
            out.println();
        }
        out.flush();
    }

    /**
     * preforms intialization of the tool. Properties are queried for the
     * default model to load, source depth to use, phases to use, etc.
     */
    @Override
    public void init() throws TauPException {
        super.init();
    }

    public static List<Arrival> calcAllIndexRays(List<SeismicPhase> seismicPhases)
            throws SlownessModelException, NoSuchLayerException {
        List<Arrival> indexArrivalList = new ArrayList<>();
        for (SeismicPhase phase : seismicPhases) {
            if (phase instanceof SimpleSeismicPhase) {
                SimpleSeismicPhase simpPhase = (SimpleSeismicPhase) phase;
                for (int i = 0; i < simpPhase.getNumRays(); i++) {
                    indexArrivalList.addAll(new RayParamIndexRay(i).calculate(simpPhase));
                }
            }
        }
        return indexArrivalList;
    }

    @Override
    public void start() throws IOException, TauPException {
        List<RayCalculateable> distanceValues = getDistanceArgs().getRayCalculatables(this.sourceArgs);
        List<Arrival> arrivalList = calcAll(getSeismicPhases(), distanceValues);
        if (getDistanceArgs().isAllIndexRays()) {
            List<Arrival> indexArrivalList = calcAllIndexRays(getSeismicPhases());
            arrivalList.addAll(indexArrivalList);
        }
        PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
        printResult(writer, arrivalList);
        writer.close();
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void validateArguments() throws TauPException {
        super.validateArguments();
        sourceArgs.validateArguments();
        if (isWithAmplitude()) {
            sourceArgs.validateArgumentsForAmplitude(modelArgs, getDistanceArgs().getRayCalculatables(sourceArgs));
        }
    }

}
