package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.AbstractOutputTypeArgs;
import edu.sc.seis.TauP.cli.ModelArgs;
import edu.sc.seis.TauP.cli.OutputTypes;
import edu.sc.seis.TauP.cli.TextOutputTypeArgs;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@CommandLine.Command(name = "find",
        description = "find seismic phases in an earth model near a search time",
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

    @Override
    public void start() throws IOException, TauPException {
        TauModel tMod = modelArgs.depthCorrected();
        SeismicPhaseWalk walker = new SeismicPhaseWalk();
        List<List<SeismicPhaseSegment>> walk = walker.walkPhases(tMod, maxActions);
        if (minRayParamRange != null) {
            double minRP = minRayParamRange[0];
            double maxRP = minRP;
            if (minRayParamRange.length > 1) {
                maxRP = minRayParamRange[1];
            }
            walk = walker.overlapsRayParam(walk, minRP, maxRP);
        }
        if (outputTypeArgs.isText()) {
            printResultText(walk);
        } else if (outputTypeArgs.isJSON()) {
            printResultJson(walk);
        }
    }

    public void printResultText(List<List<SeismicPhaseSegment>> walk) throws IOException {
        PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
        int maxNameLength = 1;
        for (List<SeismicPhaseSegment> segList : walk) {
            maxNameLength = Math.max(maxNameLength,
                    SeismicPhaseWalk.phaseNameForSegments(segList).length());
        }
        String phaseFormat = "%-" + maxNameLength + "s";
        for (List<SeismicPhaseSegment> segList : walk) {
            SeismicPhaseSegment endSeg = segList.get(segList.size()-1);

            if (showrayparam) {
                writer.print(String.format(phaseFormat, SeismicPhaseWalk.phaseNameForSegments(segList))
                        +" "+Outputs.formatRayParam(endSeg.minRayParam) + " " + Outputs.formatRayParam(endSeg.maxRayParam));
            } else {
                writer.print(SeismicPhaseWalk.phaseNameForSegments(segList));
            }
            writer.println();
            //SimpleSeismicPhase phase = new SimpleSeismicPhase();
        }
        writer.flush();
    }
    public void printResultJson(List<List<SeismicPhaseSegment>> walk) throws IOException {
        JSONObject out = new JSONObject();
        out.put("model", modelArgs.getModelName());
        out.put("sourcedepth", modelArgs.getSourceDepth());
        out.put("receiverdepth", modelArgs.getReceiverDepth());
        out.put("max", maxActions);
        JSONArray phases = new JSONArray();
        out.put("phases", phases);
        for (List<SeismicPhaseSegment> segList : walk) {
            phases.put(SeismicPhaseWalk.phaseNameForSegments(segList));
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

    }


    @CommandLine.Mixin
    ModelArgs modelArgs = new ModelArgs();

    @CommandLine.Mixin
    TextOutputTypeArgs outputTypeArgs;

    @CommandLine.Option(names = "--showrayparam", description = "show min and max ray parameter for each phase name")
    boolean showrayparam = false;

    @CommandLine.Option(names = "--max", defaultValue = "2", description = "Maximum number of reflections and phase conversion")
    int maxActions;

    @CommandLine.Option(names = "--rayparam", arity = "1..2", description = "only keep phases that overlap the given ray parameter range")
    Double[] minRayParamRange;


}
