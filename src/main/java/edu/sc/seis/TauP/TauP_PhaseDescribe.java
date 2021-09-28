package edu.sc.seis.TauP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.List;

public class TauP_PhaseDescribe extends TauP_Time {

    /** Prints the command line arguments common to all TauP tools. */
    @Override
    public void printStdUsage() {
        TauP_Tool.printStdUsageHead(this.arrivals.getClass());
        Alert.info("-ph phase list     -- comma separated phase list\n"
                + "-pf phasefile      -- file containing phases\n\n"
                + "-mod[el] modelname -- use velocity model \"modelname\" for calculations\n"
                + "                      Default is iasp91.\n\n"
                + "-h depth           -- source depth in km\n\n"
                + "--stadepth depth   -- receiver depth in km\n\n"
                + "--dump             -- dump raw sample points\n\n");
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
        String[] args = ToolRun.parseCommonCmdLineArgs(origArgs);
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        boolean cmdLineArgPhase = false;
        boolean cmdLineArgPhaseFile = false;
        while(i < args.length) {
            if(dashEquals("help", args[i])) {
                printUsage();
                noComprendoArgs[numNoComprendoArgs++] = args[i];
            } else if(dashEquals("dump", args[i])) {
                dump = true;
            } else if(dashEquals("expert", args[i])) {
                expert = true;
            } else if(i < args.length - 1) {
                if(dashEquals("mod", args[i]) || dashEquals("model", args[i])) {
                    toolProps.put("taup.model.name", args[i + 1]);
                    i++;
                } else if(args[i].equalsIgnoreCase("-h")) {
                    toolProps.put("taup.source.depth", args[i + 1]);
                    i++;
                } else if(args[i].equalsIgnoreCase("--stadepth")) {
                    setReceiverDepth(Double.parseDouble(args[i + 1]));
                    i++;
                } else if(args[i].equalsIgnoreCase("-o")) {
                    outFileBase = args[i + 1];
                    i++;
                } else if(dashEquals("ph", args[i])) {
                    if(cmdLineArgPhase) {
                        // previous cmd line -ph so append
                        toolProps.put("taup.phase.list",
                                      toolProps.getProperty("taup.phase.list",
                                                            "")
                                              + "," + args[i + 1]);
                    } else {
                        // no previous cmd line -ph so replace defaults
                        toolProps.put("taup.phase.list", args[i + 1]);
                    }
                    cmdLineArgPhase = true;
                    i++;
                } else if(dashEquals("pf", args[i])) {
                    cmdLineArgPhaseFile = true;
                    toolProps.put("taup.phase.file", args[i + 1]);
                    i++;
                } else if(dashEquals("prop", args[i])) {
                    File f = new File(args[i+1]);
                    if (! f.exists()) {
                        throw new FileNotFoundException(args[i+1]); // ToDo better error msg
                    }
                    Reader r = new BufferedReader(new FileReader(args[i + 1]));
                    toolProps.load(r);
                    Outputs.configure(toolProps);
                    i++;
                } else if(i < args.length - 2) {
                    if (args[i].contains("set") && args[i+1].startsWith("taup.")) {
                        toolProps.setProperty(args[i+1], args[i+2]);
                        Outputs.configure(toolProps);
                        i += 2;
                    } else {
                        /*
                         * I don't know how to interpret this argument, so pass
                         * it back
                         */
                        noComprendoArgs[numNoComprendoArgs++] = args[i];
                    }
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
        // check to see if there were phases or a phase file as an argument.
        // if so then dump the defaults
        if(cmdLineArgPhaseFile || cmdLineArgPhase) {
            if(cmdLineArgPhaseFile && !cmdLineArgPhase) {
                toolProps.remove("taup.phase.list");
            }
            if(!cmdLineArgPhaseFile && cmdLineArgPhase) {
                toolProps.remove("taup.phase.file");
            }
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
        depthCorrect(getSourceDepth(), getReceiverDepth());
        printResult(getWriter());
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
