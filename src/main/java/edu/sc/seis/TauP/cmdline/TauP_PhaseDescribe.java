package edu.sc.seis.TauP.cmdline;

import com.google.gson.GsonBuilder;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.AbstractOutputTypeArgs;
import edu.sc.seis.TauP.cmdline.args.OutputTypes;
import edu.sc.seis.TauP.cmdline.args.TextOutputTypeArgs;
import edu.sc.seis.TauP.gson.GsonUtil;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;

@CommandLine.Command(name = "phase",
        description = "Describe a seismic phase in the current model.",
        optionListHeading = OPTIONS_HEADING,
        usageHelpAutoWidth = true)
public class TauP_PhaseDescribe extends TauP_AbstractPhaseTool {

    public TauP_PhaseDescribe() {
        super(new TextOutputTypeArgs(OutputTypes.TEXT, AbstractOutputTypeArgs.STDOUT_FILENAME));
        outputTypeArgs = (TextOutputTypeArgs)abstractOutputTypeArgs;
    }

    /** Dumps raw interpolation points for phase. */
    public boolean dump = false;

    @CommandLine.Mixin
    TextOutputTypeArgs outputTypeArgs;

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOutputFormat();
    }

    @Override
    public void validateArguments() throws TauModelException {

    }

    @Override
    public String getOutFileExtension() {
        return outputTypeArgs.getOutFileExtension();
    }
    @Override
    public void start() throws IOException, TauPException {
        PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
        if (!getSeismicPhases().isEmpty()) {
            printResult(writer);
        } else {
            writer.println("No phases to describe.");
        }
        writer.close();
    }

    @Override
    public void destroy() throws TauPException {

    }

    public void printResult(PrintWriter writer) throws TauPException {
        if (getOutputFormat().equals(OutputTypes.TEXT)) {
            printResultText(writer);
        } else if (getOutputFormat().equals(OutputTypes.JSON)) {
            List<PhaseDescription> phaseDesc = new ArrayList<>();
            for (SeismicPhase sp : getSeismicPhases()) {
                phaseDesc.add(new PhaseDescription(sp));
            }
            PhaseDescribeResult result = new PhaseDescribeResult(modelArgs.getModelName(),
                    modelArgs.getSourceDepths(), modelArgs.getReceiverDepths(),
                    getPhaseArgs().parsePhaseNameList(),
                    getScatterer(), phaseDesc);
            GsonBuilder gsonBld = GsonUtil.createGsonBuilder();
            writer.println(gsonBld.create().toJson(result));
        } else {
            throw new IllegalArgumentException("Output format "+getOutputFormat()+" not recognized");
        }
        writer.flush();
    }

    public void printResultText(PrintWriter writer) throws TauPException {
        List<SeismicPhase> phaseList = getSeismicPhases();
        for (SeismicPhase phase: phaseList) {
            writer.println(phase.describe());
            if (dump) {
                double[] dist = phase.getDist();
                double[] time = phase.getTime();
                double[] rayParam = phase.getRayParams();
                writer.println("Dist (deg)  Time (s)  RayParam(rad/sec)");
                writer.println("----------------------------------------");
                for (int i = 0; i < dist.length; i++) {
                    writer.println((dist[i]* SphericalCoords.RtoD)+"  "+time[i]+"  "+rayParam[i]);
                }
                writer.println("----------------------------------------");
            }
            writer.println("--------");
        }
    }
}

