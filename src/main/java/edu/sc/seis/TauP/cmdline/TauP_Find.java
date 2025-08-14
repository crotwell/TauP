package edu.sc.seis.TauP.cmdline;

import com.google.gson.Gson;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.*;
import edu.sc.seis.TauP.gson.GsonUtil;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.SphericalCoords.RtoD;
import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;

@CommandLine.Command(name = "find",
        description = "Find seismic phases in an earth model.",
        optionListHeading = OPTIONS_HEADING,
        usageHelpAutoWidth = true)
public class TauP_Find extends TauP_AbstractPhaseTool {


    public TauP_Find() {
        super(new TextCsvOutputTypeArgs(OutputTypes.TEXT, AbstractOutputTypeArgs.STDOUT_FILENAME));
        outputTypeArgs = (TextCsvOutputTypeArgs) this.abstractOutputTypeArgs;
    }

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOutputFormat();
    }

    @Override
    public void init() throws TauPException {
        super.init();
    }

    public SeismicPhaseWalk createWalker(TauModel tMod, double receiverDepth, List<Double> excludeDepths) throws TauModelException {

        Double minRP = null;
        Double maxRP = null;

        if (getRayParamRange() != null && getRayParamRange().length > 0) {
            minRP = getRayParamRange()[0];
            maxRP = minRP;
            if (getRayParamRange().length > 1) {
                maxRP = getRayParamRange()[1];
            }
        }
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod,
                minRP, maxRP,
                tMod.findBranch(receiverDepth));
        if (onlyPWave) {
            walker.setAllowSWave(false);
        } else if (onlySWave) {
            walker.setAllowPWave(false);
        }
        walker.excludeBoundaries(excludeDepths);
        if (isVerbose() && !getExcludedDepths(tMod).isEmpty()) {
            System.out.println("Exclude: "+getExcludedDepths(tMod).size()+" depths:");
            for (int i : walker.getExcludeBranch()) {
                System.out.println(i+" "+ walker.gettMod().getTauBranch(i, true).getTopDepth()+" km");
            }
        }
        return walker;
    }

    @Override
    public void start() throws IOException, TauPException {
        List<RayCalculateable> distanceValues = distanceArgs.getRayCalculatables(sourceArgs);
        if (azimuth != null) {
            for (RayCalculateable rc : distanceValues) {
                rc.setAzimuth(azimuth);
            }
        }
        List<Arrival> arrivalList = new ArrayList<>();
        List<ProtoSeismicPhase> allwalk = new ArrayList<>();
        for (Double sourceDepth : modelArgs.getSourceDepths()) {
            TauModel tMod = modelArgs.depthCorrected(sourceDepth);
            for (Double recDepth : modelArgs.getReceiverDepths()) {
                TauModel tModRecDepth = tMod.splitBranch(recDepth);
                List<Double> excludeDepths = getExcludedDepths(tModRecDepth);
                List<Double> actualExcludeDepths = matchDepthToDiscon(excludeDepths, tMod.getVelocityModel(), excludeDepthTol);

                SeismicPhaseWalk walker = createWalker(tModRecDepth, recDepth, actualExcludeDepths);
                List<ProtoSeismicPhase> walk = walker.findEndingPaths(maxActions);

                if (!distanceValues.isEmpty()) {
                    double[] rayParamRange = new double[2];
                    if (getRayParamRange().length == 0) {
                        rayParamRange = new double[0];
                    } else if (getRayParamRange().length == 1) {
                        rayParamRange[0] = getRayParamRange()[0]-deltaRayParam;
                        rayParamRange[1] = getRayParamRange()[0]+deltaRayParam;
                    } else {
                        rayParamRange[0] = getRayParamRange()[0];
                        rayParamRange[1] = getRayParamRange()[1];
                    }
                    arrivalList.addAll(findForDist(walk, tModRecDepth, distanceValues, rayParamRange));
                } else {
                    allwalk.addAll(findForAllDepth(walk));
                }
            }
        }
        PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
        if((!distanceValues.isEmpty())) {
            printResult(writer, arrivalList);
            writer.flush();
        } else {
            if (outputTypeArgs.isText()) {
                printResultText(writer, allwalk);
            } else if (outputTypeArgs.isJSON()) {
                printResultJson(writer, allwalk);
            } else if (outputTypeArgs.isHTML()) {
                printResultHtml(writer, allwalk);
            } else if (outputTypeArgs.isCSV()) {
                printResultCsv(writer, allwalk);
            } else {
                throw new TauPException("Unknown output: "+outputTypeArgs.getOutputFormat());
            }
        }
        writer.close();
    }

    public List<Arrival> findForDist(List<ProtoSeismicPhase> walk,
                                     TauModel tMod,
                                     List<RayCalculateable> distanceValues,
                                     double[] rayParamRange) throws TauPException {
        List<SeismicPhase> phaseList = new ArrayList<>();
        List<String> phaseNameList = new ArrayList<>();
        for (ProtoSeismicPhase proto : walk) {
            phaseNameList.add(proto.getName());
            phaseList.add(proto.asSeismicPhase());
        }
        if ( ! phaseArgs.isEmpty()) {
            List<PhaseName> givenPhases = parsePhaseNameList();
            for (PhaseName pn : givenPhases) {
                phaseNameList.add(pn.getName());
                for (Double rd : modelArgs.getReceiverDepths()) {
                    phaseList.addAll(SeismicPhaseFactory.createSeismicPhases(pn.getName(),
                            tMod, tMod.getSourceDepth(), rd,
                            modelArgs.getScatterer(), isDEBUG()));
                }
            }
        }
        TauP_Time timeTool = new TauP_Time();
        timeTool.setPhaseNames(phaseNameList);
        timeTool.modelArgs = modelArgs;
        timeTool.outputTypeArgs = outputTypeArgs;
        timeTool.sourceArgs = sourceArgs;
        List<Arrival> arrivalList = timeTool.calcAll(phaseList,distanceValues);
        if (!times.isEmpty()) {
            double minTime = times.get(0);
            double maxTime;
            if (times.size()>1) {
                maxTime = times.get(1);
            } else {
                maxTime = minTime + deltaTime;
                minTime = minTime - deltaTime;
            }
            List<Arrival> timedArrivalList = new ArrayList<>();
            for (Arrival a : arrivalList) {
                if (minTime <= a.getTime() && a.getTime() <= maxTime) {
                    timedArrivalList.add(a);
                }
            }
            arrivalList = timedArrivalList;
        }
        if ( rayParamRange != null && rayParamRange.length == 2) {
            List<Arrival> rpArrivalList = new ArrayList<>();
            for (Arrival a : arrivalList) {
                if (rayParamRange[0] <= a.getRayParam() && a.getRayParam() <= rayParamRange[1]) {
                    rpArrivalList.add(a);
                }
            }
            arrivalList = rpArrivalList;
        }
        return arrivalList;
    }


    public void printResult(PrintWriter out, List<Arrival> arrivalList) throws TauPException {
        if (getOutputFormat().equals(OutputTypes.JSON)) {
            FindTimeResult result = createTimeResult(arrivalList, modelArgs.getTauModel());
            out.println(GsonUtil.toJson(result));
        } else {
            boolean onlyPrintTime = false;
            boolean onlyPrintRayP = false;
            List<String> relativePhaseName = new ArrayList<>();
            if (getOutputFormat().equals(OutputTypes.HTML)) {
                TauP_Time.printArrivalsAsHtml(out, arrivalList,
                        modelArgs.getModelName(),
                        getScatterer(),
                        isWithAmplitude(), sourceArgs,
                        relativePhaseName, "Find");
            } else {
                TauP_Time.printArrivalsAsText(out, arrivalList,
                        modelArgs.getModelName(),
                        getScatterer(),
                        onlyPrintTime, onlyPrintRayP,
                        isWithAmplitude(), sourceArgs,
                        relativePhaseName);
            }
        }
        out.flush();
    }

    public List<ProtoSeismicPhase> findForAllDepth(List<ProtoSeismicPhase> walk) throws TauPException {
        List<SeismicPhase> givenPhases = new ArrayList<>();
        if ( ! phaseArgs.isEmpty()) {
            givenPhases = getSeismicPhases();
        }
        for (SeismicPhase sp : givenPhases) {
            if (sp instanceof SimpleContigSeismicPhase) {
                SimpleContigSeismicPhase ssp = (SimpleContigSeismicPhase) sp;
                walk.add(ssp.getProto());
            } else if (sp instanceof CompositeSeismicPhase) {
                CompositeSeismicPhase csp = (CompositeSeismicPhase) sp;
                for (SimpleContigSeismicPhase subsp : csp.getSubPhaseList()) {
                    walk.add(subsp.getProto());
                }
            } else {
                ScatteredSeismicPhase scat = (ScatteredSeismicPhase) sp;
                if (scat.getScatteredPhase() instanceof SimpleContigSeismicPhase) {
                    SimpleContigSeismicPhase ssp = (SimpleContigSeismicPhase) scat.getScatteredPhase();
                    walk.add(ssp.getProto());
                } else if (scat.getScatteredPhase() instanceof CompositeSeismicPhase) {
                    CompositeSeismicPhase csp = (CompositeSeismicPhase) scat.getScatteredPhase();
                    for (SimpleContigSeismicPhase subsp : csp.getSubPhaseList()) {
                        walk.add(subsp.getProto());
                    }
                }
            }
        }
        return walk;
    }


    public List<Double> matchDepthToDiscon(List<Double> excludeDepth, VelocityModel vMod, double tol) throws NoSuchLayerException {
        List<Double> out = new ArrayList<>();
        double[] disconDepths = vMod.getDisconDepths();
        for (Double d : excludeDepth) {
            double best = Double.MAX_VALUE;
            for (double discon : disconDepths) {
                if (Math.abs(d-discon) < Math.abs(d-best) && Math.abs(d-discon) < tol) {
                    best = discon;
                }
            }
            if (best != Double.MAX_VALUE) {
                out.add(best);
            } else {
                String disconList = "";
                for (double v : disconDepths) {
                    disconList += " "+v;
                }
                throw new NoSuchLayerException((vMod.getRadiusOfEarth() -d),
                        "Unable to find discontinuity within "+tol+" km of "+d+" in "+vMod.getModelName()+"\n"
                +"Discons in model: "+disconList);
            }
        }
        return out;
    }

    public void printResultHtml(PrintWriter writer, List<ProtoSeismicPhase> walk) throws TauPException {
        List<String> head = new ArrayList<>();
        head.add("Phase");
        if (showrayparam) {
            head.addAll(List.of("Min", "Max (s/deg)"));
        }
        List<List<String>> values = new ArrayList<>();
        for (ProtoSeismicPhase segList : walk) {
            SeismicPhaseSegment endSeg = segList.get(segList.size() - 1);
            List<String> row = new ArrayList<>();
            row.add(segList.phaseNameForSegments());
            if (showrayparam) {
                row.add(Outputs.formatRayParam(endSeg.getMinRayParam() / RtoD));
                row.add(Outputs.formatRayParam(endSeg.getMaxRayParam() / RtoD));
            }
            values.add(row);
        }

        HTMLUtil.createHtmlStart(writer, "TauP Find", HTMLUtil.createTableCSS(), true);
        writer.println(HTMLUtil.createBasicTable(head, values));
        HTMLUtil.addSortTableJS(writer);
        writer.println(HTMLUtil.createHtmlEnding());
        writer.flush();
    }

    public void printResultText(PrintWriter writer, List<ProtoSeismicPhase> walk) {
        int maxNameLength = 1;
        for (ProtoSeismicPhase segList : walk) {
            maxNameLength = Math.max(maxNameLength,
                    segList.phaseNameForSegments().length());
        }
        String phaseFormat = "%-" + maxNameLength + "s";

        if (showrayparam) {
            writer.println(String.format(phaseFormat, "Phase")+"    Min     Max (s/deg)");
            for (int i = 0; i < maxNameLength; i++) {
                writer.print("-");
            }
            writer.println("-----------------------");
        }
        for (ProtoSeismicPhase segList : walk) {
            SeismicPhaseSegment endSeg = segList.get(segList.size()-1);

            if (showrayparam) {
                writer.print(String.format(phaseFormat, segList.phaseNameForSegments())
                        +" "+Outputs.formatRayParam(endSeg.getMinRayParam() / RtoD) + " " + Outputs.formatRayParam(endSeg.getMaxRayParam() / RtoD));
            } else {
                writer.print(segList.phaseNameForSegments());
            }
            writer.println();
            //SimpleSeismicPhase phase = new SimpleSeismicPhase();
        }
        writer.flush();
    }


    public void printResultCsv(PrintWriter writer, List<ProtoSeismicPhase> walk) {
        String comma = ",";

        writer.print("Phase");
        if (showrayparam) {
            writer.print(",Min RP (s/deg),Max RP (s/deg)");
        }
        writer.println();
        for (ProtoSeismicPhase segList : walk) {
            SeismicPhaseSegment endSeg = segList.get(segList.size()-1);

            writer.print(segList.phaseNameForSegments());
            if (showrayparam) {
                writer.print(comma+Outputs.formatRayParam(endSeg.getMinRayParam() / RtoD).trim()
                                + comma + Outputs.formatRayParam(endSeg.getMaxRayParam() / RtoD).trim());
            }
            writer.println();
        }
        writer.flush();
    }

    FindTimeResult createTimeResult(List<Arrival> arrivals, TauModel optTauModel) throws TauPException {
        TauModel tMod = optTauModel;
        if (!arrivals.isEmpty()) {
            tMod = arrivals.get(0).getTauModel();
        }
        List<Float> excludeList = new ArrayList<>();
        for (double d : getExcludedDepths(tMod)) {
            excludeList.add((float) d);
        }
        List<String> phases = new ArrayList<>();
        List<PhaseName> inputPhases = new ArrayList<>();
        if ( ! getPhaseArgs().isEmpty()) {
            inputPhases.addAll(getPhaseArgs().parsePhaseNameList());
        }
        FindTimeResult result = new FindTimeResult(getTauModelName(), getSourceDepths(), getReceiverDepths(),
                inputPhases, getScatterer(),
                false, sourceArgs,
                arrivals,
                maxActions, excludeList, phases);
        return result;
    }
    FindResult createResult(List<ProtoSeismicPhase> walk, TauModel optTauModel) throws TauPException {
        TauModel tMod = optTauModel;
        if (!walk.isEmpty()) {
            tMod = walk.get(0).gettMod();
        }
        List<Float> excludeList = new ArrayList<>();
        for (double d : getExcludedDepths(tMod)) {
            excludeList.add((float) d);
        }
        List<PhaseName> inputPhases = new ArrayList<>();
        if ( ! getPhaseArgs().isEmpty()) {
            inputPhases.addAll(getPhaseArgs().parsePhaseNameList());
        }
        List<String> phases = new ArrayList<>();
        for (ProtoSeismicPhase segList : walk) {
            phases.add(segList.phaseNameForSegments());
        }
        FindResult result = new FindResult(getTauModelName(), getSourceDepths(), getReceiverDepths(),
                inputPhases, getScatterer(),
                maxActions, excludeList, phases
        );
        return result;
    }
    public void printResultJson(PrintWriter writer, List<ProtoSeismicPhase> walk) throws TauPException {
        FindResult result = createResult(walk, modelArgs.getTauModel());
        Gson gson = GsonUtil.createGsonBuilder().create();

        writer.println(gson.toJson(result));
        writer.flush();
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void validateArguments() throws TauPException {
        if (onlyPWave && onlySWave) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                    "Only one of --pwaveonly and --swaveonly may be used");
        }
        TauModel tMod = modelArgs.getTauModel();
        List<Double> excludeDepths = getExcludedDepths(tMod);
        // throws if cant find depth near discon
        List<Double> actualExcludeDepths = matchDepthToDiscon(excludeDepths, tMod.getVelocityModel(), excludeDepthTol);

        if (rayParamRangeDeg != null &&  rayParamRangeKm != null) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                    "Only one of --rayparamdeg and --rayparamkm may be used");
        }
        if (isWithAmplitude() && modelArgs.getTauModel().getVelocityModel().densityIsDefault()) {
            throw new TauModelException("model "+modelArgs.getModelName()+" does not include density, but amplitude requires density.");
        }
        if (isWithAmplitude() && modelArgs.getTauModel().getVelocityModel().QIsDefault()) {
            throw new TauModelException("model "+modelArgs.getModelName()
                    +" does not include Q, but amplitude requires Q. Please choose a differet model.");
        }
        sourceArgs.validateArguments();
        if (sourceArgs.hasStrikeDipRake() && azimuth == null) {
            throw new TauModelException("strike,dip,rake requires azimuth");
        }
        if (!distanceArgs.getRayCalculatables(sourceArgs).isEmpty()
                && (rayParamRangeDeg != null ||  rayParamRangeKm != null)
                && getRayParamRange().length == 1) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                    "Single value for --rayparamdeg or --rayparamkm not allowed when also giving --degree distance.");
        }
    }

    @CommandLine.Mixin
    TextCsvOutputTypeArgs outputTypeArgs;

    @CommandLine.Option(names = "--showrayparam", description = "show min and max ray parameter for each phase name")
    boolean showrayparam = false;

    @CommandLine.Option(names = "--max",
            required = true,
            description = "Maximum number of reflections and phase conversion")
    int maxActions;

    @CommandLine.Option(names = "--rayparamdeg",
            arity = "1..2",
            paramLabel = "s/deg",
            description = "only keep phases that overlap the given ray parameter range in s/deg")
    Double[] rayParamRangeDeg;

    @CommandLine.Option(names = "--rayparamkm",
            arity = "1..2",
            paramLabel = "s/km",
            description = "only keep phases that overlap the given ray parameter range in s/km")
    Double[] rayParamRangeKm;

    protected Double[] getRayParamRange() throws TauModelException {

        if (rayParamRangeDeg == null &&  rayParamRangeKm == null) {
            return new Double[0];
        } else if (rayParamRangeDeg != null &&  rayParamRangeKm != null) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                    "Only one of --rayparamdeg and --rayparamkm may be used");
        }
        Double[] rpRange = new Double[0];
        if (rayParamRangeKm != null) {
            rpRange = new Double[rayParamRangeKm.length];
            rpRange[0] = rayParamRangeKm[0] / modelArgs.getTauModel().getRadiusOfEarth();
            if (rayParamRangeKm.length > 1) {
                rpRange[1] = rayParamRangeKm[1] / modelArgs.getTauModel().getRadiusOfEarth();
            }
        } else if (rayParamRangeDeg != null) {
            rpRange = new Double[rayParamRangeDeg.length];
            rpRange[0] = rayParamRangeDeg[0] / SphericalCoords.DtoR;
            if (rayParamRangeDeg.length > 1) {
                rpRange[1] = rayParamRangeDeg[1] / SphericalCoords.DtoR;
            }
        }
        return rpRange;
    }

    public List<Double> getExcludedDepths(TauModel tMod) {
        List<Double> exList = new ArrayList<>(getExcludeDepth(tMod.getVelocityModel()));
        double[] branchDepths = tMod.getBranchDepths();
        for (double branchDepth : branchDepths) {
            if ((onlyNamedDiscon && branchDepth != 0
                    && !tMod.isNoDisconDepth(branchDepth)
                    &&  !tMod.getVelocityModel().isNamedDisconDepth(branchDepth))) {
                exList.add(branchDepth);
            }
        }
        return exList;
    }

    double excludeDepthTol = 10.0;

    @CommandLine.Option(names = "--exclude",
            arity = "1..",
            paramLabel = "depth",
            split = ",",
            description = {
                    "Exclude boundaries from phase conversion or reflection interactions",
                    "May be depth (within tol) or named boundary like moho, cmb, iocb"
            })
    List<String> excludeDepthNames = new ArrayList<>();

    List<Double> getExcludeDepth(VelocityModel vMod) {
        List<Double> excludeDepth = new ArrayList<>();
        for (String discon : excludeDepthNames) {
            if(NamedVelocityDiscon.isMoho(discon)) {
                excludeDepth.add(vMod.getMohoDepth());
                continue;
            } else if(NamedVelocityDiscon.isCmb(discon)) {
                excludeDepth.add(vMod.getCmbDepth());
                continue;
            } else if(NamedVelocityDiscon.isIcocb(discon)) {
                excludeDepth.add(vMod.getIocbDepth());
                continue;
            } else {
                // others like ICE, SEABED, etc
                boolean found = false;
                for (NamedVelocityDiscon nd : vMod.namedDiscon) {
                    if (discon.equals(nd.getName())) {
                        excludeDepth.add(nd.getDepth());
                        found = true;
                        break;
                    }
                }
                if (found) {
                    continue;
                }
            }
            try {
                double d = Double.parseDouble(discon);
                excludeDepth.add(d);
            } catch (NumberFormatException e) {
                throw new CommandLine.ParameterException(spec.commandLine(), "Unable to find discontinuity depth for "+discon);
            }
        }
        return excludeDepth;
    }


    @CommandLine.Option(names = "--onlynameddiscon", description = "only interact with named discontinuities like moho, cmb, iocb")
    boolean onlyNamedDiscon = false;

    @CommandLine.Option(names = "--pwaveonly", description = "only P wave legs, no S")
    boolean onlyPWave = false;

    @CommandLine.Option(names = "--swaveonly", description = "only S wave legs, no P")
    boolean onlySWave = false;

    @CommandLine.Option(names = "--time",
            arity = "1..2",
            paramLabel = "t",
            description = "find arrivals within the given range")
    List<Double> times = new ArrayList<>();

    @CommandLine.Option(names = "--deltatime",
            paramLabel = "dt",
            description = "find arrivals within the +- deltatime, --times must have single time")
    Double deltaTime = 5.0;

    /**
     * Used to limit times when only one ray param is given instead of range. Matches default precision from
     * text output of arrival ray param.
     */
    Double deltaRayParam = 0.001 / SphericalCoords.DtoR;

    public boolean isWithAmplitude() {
        return getSourceArgs().isWithAmplitude();
    }

    @CommandLine.Mixin
    AmplitudeArgs sourceArgs = new AmplitudeArgs();

    public AmplitudeArgs getSourceArgs() {
        return sourceArgs;
    }

    @CommandLine.Option(names="--az", description="azimuth in degrees, for amp calculations")
    protected Double azimuth = null;

    @CommandLine.Option(names={"--deg", "--degree"},
            paramLabel="d",
            description="distance in degrees", split=",")
    protected void setDegree(List<Double> degreesList) {
        distanceArgs.setDegreeList(degreesList);
    }

    DistanceArgs distanceArgs = new DistanceArgs();
}


class FindResult extends AbstractPhaseResult {

    public FindResult(String modelName, List<Double> depth, List<Double> receiverDepth,
                      List<PhaseName> phaseNameList, Scatterer scatterer,
                      int maxActions, List<Float> exclude,
                      List<String> foundphases) {
        super(modelName, depth, receiverDepth, phaseNameList, scatterer);
        this.maxactions = maxActions;
        this.exclude = exclude;
        this.foundphases = foundphases;
    }

    int maxactions;
    List<Float> exclude;
    List<String> foundphases;
}


class FindTimeResult extends TimeResult {

    public FindTimeResult(String modelName, List<Double> depth, List<Double> receiverDepth,
                          List<PhaseName> phaseNameList, Scatterer scatterer,
                          boolean withAmp, SeismicSourceArgs sourceArgs, List<Arrival> arrivals,
                          int maxActions, List<Float> exclude,
                          List<String> foundphases) {
        super(modelName, depth, receiverDepth, phaseNameList, scatterer, withAmp, sourceArgs, arrivals);
        this.maxactions = maxActions;
        this.exclude = exclude;
        this.foundphases = foundphases;
    }
    int maxactions;
    List<Float> exclude;
    List<String> foundphases;
}