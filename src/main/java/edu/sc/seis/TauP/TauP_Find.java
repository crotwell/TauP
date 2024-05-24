package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.*;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.Arrival.RtoD;

@CommandLine.Command(name = "find",
        description = "Find seismic phases in an earth model.",
        optionListHeading = "%nOptions:%n%n",
        abbreviateSynopsis = true,
        usageHelpAutoWidth = true)
public class TauP_Find extends TauP_Tool {


    public TauP_Find() {
        super(new TextOutputTypeArgs(OutputTypes.TEXT, AbstractOutputTypeArgs.STDOUT_FILENAME));
        outputTypeArgs = (TextOutputTypeArgs) this.abstractOutputTypeArgs;
    }

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOutputFormat();
    }

    @Override
    public void init() throws TauPException {

    }

    public SeismicPhaseWalk createWalker(TauModel tMod, List<Double> excludeDepths) throws TauModelException {

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
                tMod.findBranch(modelArgs.getReceiverDepth()));
        if (onlyPWave) {
            walker.allowSWave = false;
        }
        walker.excludeBoundaries(excludeDepths);
        if (isVerbose() && !getExcludedDepths(tMod).isEmpty()) {
            System.out.println("Exclude: "+getExcludedDepths(tMod).size()+" depths:");
            for (int i : walker.excludeBranch) {
                System.out.println(i+" "+walker.tMod.getTauBranch(i, true).getTopDepth()+" km");
            }
        }
        return walker;
    }

    @Override
    public void start() throws IOException, TauPException {
        TauModel tMod = modelArgs.depthCorrected();
        List<Double> excludeDepths = getExcludedDepths(tMod);
        List<Double> actualExcludeDepths = matchDepthToDiscon(excludeDepths, tMod.getVelocityModel(), excludeDepthTol);

        SeismicPhaseWalk walker = createWalker(tMod, actualExcludeDepths);
        List<ProtoSeismicPhase> walk = walker.findEndingPaths(maxActions);

        List<RayCalculateable> distanceValues = distanceArgs.getRayCalculatables();
        if((!distanceValues.isEmpty())) {
            List<SeismicPhase> phaseList = new ArrayList<>();
            List<String> phaseNameList = new ArrayList<>();
            for (ProtoSeismicPhase proto : walk) {
                phaseNameList.add(proto.getName());
                phaseList.add(proto.asSeismicPhase());
            }
            TauP_Time timeTool = new TauP_Time();
            timeTool.setPhaseNames(phaseNameList);
            timeTool.modelArgs = modelArgs;
            timeTool.outputTypeArgs = outputTypeArgs;
            timeTool.sourceArgs = sourceArgs;
            timeTool.withAmplitude= withAmplitude;
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
            PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
            timeTool.printResult(writer, arrivalList);
            writer.flush();
        } else {
            System.err.println("No distances given, just find phases...");
            if (outputTypeArgs.isText()) {
                printResultText(walk);
            } else if (outputTypeArgs.isJSON()) {
                printResultJson(walk);
            }
        }
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
                throw new NoSuchLayerException((vMod.radiusOfEarth-d),
                        "Unable to find discontinuity within "+tol+" km of "+d+" in "+vMod.getModelName()+"\n"
                +"Discons in model: "+disconList);
            }
        }
        return out;
    }

    public void printResultText(List<ProtoSeismicPhase> walk) throws IOException {
        PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
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
                        +" "+Outputs.formatRayParam(endSeg.minRayParam / RtoD) + " " + Outputs.formatRayParam(endSeg.maxRayParam / RtoD));
            } else {
                writer.print(segList.phaseNameForSegments());
            }
            writer.println();
            //SimpleSeismicPhase phase = new SimpleSeismicPhase();
        }
        writer.flush();
    }
    public void printResultJson(List<ProtoSeismicPhase> walk) throws IOException {
        JSONObject out = new JSONObject();
        out.put("model", modelArgs.getModelName());
        out.put("sourcedepth", modelArgs.getSourceDepth());
        out.put("receiverdepth", modelArgs.getReceiverDepth());
        out.put("max", maxActions);
        JSONArray exclude = new JSONArray();
        out.put("exclude", exclude);
        TauModel tMod;
        if (walk.isEmpty()) {
            try {
                tMod = modelArgs.getTauModel();
            } catch (TauModelException e) {
                throw new RuntimeException(e);
            }
        } else {
            tMod = walk.get(0).tMod;
        }
        for (double d : getExcludedDepths(tMod)) {
            exclude.put(d);
        }
        JSONArray phases = new JSONArray();
        out.put("phases", phases);
        for (ProtoSeismicPhase segList : walk) {
            phases.put(segList.phaseNameForSegments());
        }
        PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
        out.write(writer, 2, 0);
        writer.println();
        writer.flush();
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void validateArguments() throws TauPException {
        TauModel tMod = modelArgs.depthCorrected();
        List<Double> excludeDepths = getExcludedDepths(tMod);
        // throws if cant find depth near discon
        List<Double> actualExcludeDepths = matchDepthToDiscon(excludeDepths, tMod.getVelocityModel(), excludeDepthTol);
    }


    @CommandLine.Mixin
    ModelArgs modelArgs = new ModelArgs();

    @CommandLine.Mixin
    TextOutputTypeArgs outputTypeArgs;

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
    Double[] minRayParamRange;

    @CommandLine.Option(names = "--rayparamkm",
            arity = "1..2",
            paramLabel = "s/km",
            description = "only keep phases that overlap the given ray parameter range in s/km")
    Double[] minRayParamRangeKm;

    protected Double[] getRayParamRange() throws TauModelException {

        if (minRayParamRange == null &&  minRayParamRangeKm == null) {
            return null;
        } else if (minRayParamRange != null &&  minRayParamRangeKm != null) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                    "Only one of --rayparamdeg and --rayparamkm may be used");
        }
        Double[] rpRange = null;
        if (minRayParamRangeKm != null) {
            rpRange = new Double[minRayParamRangeKm.length];
            rpRange[0] = minRayParamRangeKm[0] / modelArgs.getTauModel().getRadiusOfEarth();
            if (minRayParamRangeKm.length > 1) {
                rpRange[1] = minRayParamRangeKm[1] / modelArgs.getTauModel().getRadiusOfEarth();
            }
        } else if (minRayParamRange != null) {
            rpRange = minRayParamRange;
        }
        return rpRange;
    }

    public List<Double> getExcludedDepths(TauModel tMod) {
        List<Double> exList = new ArrayList<>(excludeDepth);
        double[] branchDepths = tMod.getBranchDepths();
        for (double branchDepth : branchDepths) {
            if ((onlyNamedDiscon && branchDepth != 0 &&  !tMod.getVelocityModel().isNamedDisconDepth(branchDepth))) {
                exList.add(branchDepth);
            }
        }
        return exList;
    }

    double excludeDepthTol = 10.0;

    @CommandLine.Option(names = "--exclude",
            arity = "1..",
            paramLabel = "depth",
            description = "Exclude boundaries from phase conversion or reflection interactions")
    List<Double> excludeDepth = new ArrayList<>();

    @CommandLine.Option(names = "--onlynameddiscon", description = "only draw circles on the plot for named discontinuities like moho, cmb, iocb")
    boolean onlyNamedDiscon = false;

    @CommandLine.Option(names = "--pwaveonly", description = "only P wave legs, no S")
    boolean onlyPWave = false;

    @CommandLine.Option(names = "--time",
            arity = "1..2",
            paramLabel = "t",
            description = "find arrivals within the given range")
    List<Double> times = new ArrayList<>();

    @CommandLine.Option(names = "--deltatime",
            paramLabel = "dt",
            description = "find arrivals within the +- deltatime, --times must have single time")
    Double deltaTime = 5.0;


    @CommandLine.Option(names = "--amp",
            description = "show amplitude factor for each phase, only if --deg")
    public boolean withAmplitude = false;

    public boolean isWithAmplitude() {
        return withAmplitude;
    }

    @CommandLine.Mixin
    SeismicSourceArgs sourceArgs = new SeismicSourceArgs();

    @CommandLine.Option(names={"--deg", "--degree"},
            paramLabel="d",
            description="distance in degrees", split=",")
    protected void setDegree(List<Double> degreesList) {
        distanceArgs.setDegreeList(degreesList);
    }

    DistanceArgs distanceArgs = new DistanceArgs();
}
