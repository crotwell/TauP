package edu.sc.seis.TauP;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class TauP_PhaseDescribe extends TauP_Time {

    /** Prints the command line arguments common to all TauP tools. */
    @Override
    public void printStdUsage() {
        TauP_Tool.printStdUsageHead(this.getClass());
        Alert.info("-ph phase list        -- comma separated phase list\n"
                + "-pf phasefile         -- file containing phases\n\n"
                + "-mod[el] modelname    -- use velocity model \"modelname\" for calculations\n"
                + "                         Default is iasp91.\n\n"
                + "-h depth              -- source depth in km\n\n"
                + "--stadepth depth      -- receiver depth in km\n\n"
                + "--scat[ter] depth deg -- scattering depth and distance\n"
                + "--dump                -- dump raw sample points\n\n");
    }

    /** Dumps raw interpolation points for phase. */
    public boolean dump = false;

    public void printUsage() {
        printStdUsage();
        printStdUsageTail();
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
    public void printResult(PrintWriter writer) {
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
}
