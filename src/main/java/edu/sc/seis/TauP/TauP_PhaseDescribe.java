package edu.sc.seis.TauP;

import edu.sc.seis.TauP.CLI.OutputTypes;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@CommandLine.Command(name = "phase")
public class TauP_PhaseDescribe extends TauP_AbstractPhaseTool {

    public TauP_PhaseDescribe() {
        super();
        setDefaultOutputFormat();
    }

    /** Prints the command line arguments common to all TauP tools. */
    public String getStdUsage() {
        return TauP_Tool.getStdUsageHead(this.getClass())
                + "-ph phase list        -- comma separated phase list\n"
                + "-pf phasefile         -- file containing phases\n\n"
                + "-mod[el] modelname    -- use velocity model \"modelname\" for calculations\n"
                + "                         Default is iasp91.\n\n"
                + "-h depth              -- source depth in km\n\n"
                + "--stadepth depth      -- receiver depth in km\n\n"
                + "--scat[ter] depth deg -- scattering depth and distance\n"
                + "--dump                -- dump raw sample points\n\n\n";
    }

    /** Dumps raw interpolation points for phase. */
    public boolean dump = false;

    public String getUsage() {
        return getStdUsage()
        + getStdUsageTail();
    }

    @Override
    public void validateArguments() throws TauModelException {

    }

    @Override
    public String[] allowedOutputFormats() {
        String[] formats = {OutputTypes.TEXT, OutputTypes.JSON};
        return formats;
    }
    @Override
    public void setDefaultOutputFormat() {
        setOutputFormat(OutputTypes.TEXT);
    }

    @Override
    protected String[] parseCmdLineArgs(String[] origArgs) throws IOException {
        int i = 0;
        String[] args = super.parseSourceModelCmdLineArgs(origArgs);
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        boolean cmdLineArgPhase = false;
        boolean cmdLineArgPhaseFile = false;
        while(i < args.length) {
            if(dashEquals("dump", args[i])) {
                dump = true;
            } else if(i < args.length - 1) {
                if(args[i].equalsIgnoreCase("-o")) {
                    outFileBase = args[i + 1];
                    i++;
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
        if(numNoComprendoArgs > 0) {
            String[] temp = new String[numNoComprendoArgs];
            System.arraycopy(noComprendoArgs, 0, temp, 0, numNoComprendoArgs);
            return temp;
        } else {
            return new String[0];
        }
    }

    @Override
    public void start() throws IOException, TauModelException, TauPException {
        if (getSeismicPhases().size() > 0) {
            depthCorrect();
            printResult(getWriter());
        } else {
            getWriter().println("No phases to describe.");
        }
    }

    @Override
    public void destroy() throws TauPException {

    }

    public void printResult(PrintWriter writer) {
        if (outputFormat.equals(OutputTypes.TEXT)) {
            printResultText(writer);
        } else if (outputFormat.equals(OutputTypes.JSON)) {
            printResultJSON(writer);
        } else {
            throw new IllegalArgumentException("Output format "+outputFormat+" not recognized");
        }
        writer.flush();
    }

    public void printResultText(PrintWriter writer) {
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
                    writer.println((dist[i]*Arrival.RtoD)+"  "+time[i]+"  "+rayParam[i]);
                }
                writer.println("----------------------------------------");
            }
            writer.println("--------");
        }
    }

    public void printResultJSON(PrintWriter writer) {
        List<SeismicPhase> phaseList = getSeismicPhases();
        writer.println("[");
        boolean first = true;
        for (SeismicPhase phase : phaseList) {
            if (first) {
                writer.println("");
                first = false;
            } else {
                writer.println(",");
            }
            writer.print(phase.describeJson());
        }
        writer.println("]\n");
    }
}
