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
import org.json.JSONArray;
import org.json.JSONObject;
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
    public List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> rayCalcList) throws TauPException {
        List<Arrival> arrivals = new ArrayList<>();
        for (SeismicPhase phase : phaseList) {
            List<Arrival> phaseArrivals = new ArrayList<>();
            for (RayCalculateable rayCalc : rayCalcList) {
                if (( ! rayCalc.hasSourceDepth() || rayCalc.getSourceDepth() == phase.getSourceDepth())
                        && ( ! rayCalc.hasReceiverDepth() || rayCalc.getReceiverDepth() == phase.getReceiverDepth())) {
                    phaseArrivals.addAll(rayCalc.calculate(phase));
                }
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
                // need to change this to be per-source somehow
                a.setSeismicMoment(sourceArgs.getMoment());
            }
        }

        if(!relativePhaseList.isEmpty()) {
            Set<Double> distList = new HashSet<>();
            for (Arrival arr : arrivals) {
                distList.add(arr.getModuloDist());
            }
            HashMap<Double, List<Arrival>> earliestAtDist = new HashMap<>();
            for (Double dist : distList) {
                DistanceRay distRay = DistanceRay.ofRadians(dist);
                List<Arrival> relativeArrivals = new ArrayList<>();
                for (SeismicPhase relPhase : relativePhaseList) {
                    relativeArrivals.addAll(distRay.calculate(relPhase));
                }
                Arrival.sortArrivals(relativeArrivals);
                earliestAtDist.put(dist, relativeArrivals);
            }
            for (Arrival arrival : arrivals) {
                List<Arrival> relativeArrivals = earliestAtDist.get(arrival.getModuloDist());
                for (Arrival relArrival : relativeArrivals) {
                    if (relArrival.getSourceDepth() == arrival.getSourceDepth() && relArrival.getReceiverDepth() == arrival.getReceiverDepth()) {
                        arrival.setRelativeToArrival(relArrival);
                        break;
                    }
                }
            }
        }
        return Arrival.sortArrivals(arrivals);
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
        printArrivalsAsText(out, arrivalList,
                modelArgs.getModelName(),
                modelArgs.getSourceDepths(),
                modelArgs.getReceiverDepths(),
                getScatterer(),
                onlyFirst, onlyPrintTime, onlyPrintRayP,
                isWithAmplitude(), sourceArgs.getMw(), sourceArgs.getAttenuationFrequency(),
                relativePhaseName);
    }

    public static void printArrivalsAsText(PrintWriter out,
                                           List<Arrival> arrivalList,
                                           String modelName,
                                           List<Double> sourceDepthList,
                                           List<Double>  receiverDepthList,
                                           Scatterer scatterer,
                                           boolean onlyFirst, boolean onlyPrintTime, boolean onlyPrintRayP,
                                           boolean withAmplitude, double Mw, double attenuationFrequency,
                                           List<String> relativePhaseName) {
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
            String modelLine =  "\nModel: " + modelName;

            if (scatterer != null && scatterer.depth != 0.0) {
                modelLine += "  Scatter Depth: "+ scatterer.depth+" km Dist: "+ scatterer.getDistanceDegree();
            }
            out.println(modelLine);
            String lineOne = "Distance   Depth   " + String.format(phaseFormat, "Phase")
                    + "   Travel    Ray Param  Takeoff  Incident Station   Purist   "
                    + String.format(phasePuristFormat, "Purist");
            String lineTwo = "  (deg)     (km)   " + String.format(phaseFormat, "Name ")
                    + "   Time (s)  p (s/deg)   (deg)    (deg)    (km)     Distance  "
                    + String.format(phasePuristFormat, "Name");
            if (withAmplitude) {
                lineOne += "    Amp  ~"+Outputs.formatDistanceNoPad(Mw)+" Mw";
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
                out.print(Outputs.formatDepth(currArrival.getReceiverDepth()) + " ");
                out.print(Outputs.formatDistance(currArrival.getDistDeg()));
                if (currArrival.getName().equals(currArrival.getPuristName())) {
                    out.print("   = ");
                } else {
                    out.print("   * ");
                }
                out.print(String.format(phasePuristFormat, currArrival.getPuristName()));
                if (withAmplitude) {
                    try {
                        double ampFactorPSV = currArrival.getAmplitudeFactorPSV(MomentMagnitude.mw_to_N_m(Mw), attenuationFrequency);
                        double ampFactorSH = currArrival.getAmplitudeFactorSH(MomentMagnitude.mw_to_N_m(Mw), attenuationFrequency);
                        out.print(" " + Outputs.formatAmpFactor(ampFactorPSV) + " " + Outputs.formatAmpFactor(ampFactorSH));
                    } catch (SlownessModelException | TauModelException | VelocityModelException e) {
                        throw new RuntimeException("Should not happen", e);
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
                if (arrival.getRayCalculateable().hasDescription()) {
                    out.print(" "+arrival.getRayCalculateable().getDescription());
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
        TauP_AbstractRayTool.writeJSON(out, "",
                getTauModelName(),
                modelArgs.getSourceDepths(),
                modelArgs.getReceiverDepths(),
                getSeismicPhases(),
                arrivalList,
                isWithAmplitude(),
                sourceArgs.getMw(), sourceArgs.getAttenuationFrequency());
    }

    public static JSONObject resultAsJSONObject(String modelName,
                                                List<Double> depth,
                                                List<Double> receiverDepth,
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
    @Override
    public void init() throws TauPException {
        super.init();
    }

    @Override
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
