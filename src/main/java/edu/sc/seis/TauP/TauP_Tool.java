package edu.sc.seis.TauP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Properties;
import java.util.concurrent.Callable;

import edu.sc.seis.TauP.cli.AbstractOutputTypeArgs;
import picocli.CommandLine;

/**
 * Base class for tools within the TauP Toolkit.
 */
public abstract class TauP_Tool implements Callable<Integer> {

    /* Constructors */

    public TauP_Tool(AbstractOutputTypeArgs outputTypeArgs) {
        this.abstractOutputTypeArgs = outputTypeArgs;
        toolProps = TauP_Tool.configDefaults();
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public Integer call() throws Exception {
        Outputs.configure(toolProps);
        SeismicPhaseFactory.configure(toolProps);
        init();
        try {
            validateArguments();
        } catch (TauPException e) {
            if (spec != null ) {
                throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage(), e);
            } else {
                throw e;
            }
        }
        start();
        return 0;
    }

    /** Turns on debugging output. */
    @CommandLine.Option(names="--debug", description="enable debugging output")
    public void setDEBUG(boolean debug) {
        ToolRun.DEBUG = debug;
    }

    public boolean isDEBUG() {
        return ToolRun.DEBUG;
    }

    /** Turns on verbose output. */
    @CommandLine.Option(names="--verbose", description="enable verbose output")
    public void setVerbose(boolean verbose) {
        ToolRun.VERBOSE = verbose;
    }

    public boolean isVerbose() {
        return ToolRun.VERBOSE || ToolRun.DEBUG;
    }

    @CommandLine.Option(names = {"--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    public String[] cmdLineArgs = new String[0];

    protected Properties toolProps;
    

    public static Properties configDefaults() {
        Properties toolProps;
        try {
            toolProps = PropertyLoader.load();
        } catch(Exception e) {
            Alert.warning("Unable to load properties, using defaults.",
                          e.getMessage());
            toolProps = new Properties();
        }
        Outputs.configure(toolProps);
        SeismicPhaseFactory.configure(toolProps);
        return toolProps;
    }
    
    public abstract String getOutputFormat();

    public AbstractOutputTypeArgs abstractOutputTypeArgs;

    /** usually one of TEXT or JSON. Subclasses may add
     * additional types, for example CSV, GMT or SVG.
     * @param val output format for results
     */
    public void setOutputFormat(String val) {
        abstractOutputTypeArgs.setOutputFormat(val);
    }
    
    public String getOutFileBase() {
        return abstractOutputTypeArgs.getOutFileBase();
    }
    
    public void setOutFileBase(String outFileBase) {
        abstractOutputTypeArgs.setOutFileBase(outFileBase);
    }
    
    public String getOutFileExtension() {
        return abstractOutputTypeArgs.getOutFileExtension() ;
    }
    
    public void setOutFileExtension(String outFileExtension) {
        abstractOutputTypeArgs.setOutFileExtension(outFileExtension);
    }

    public static void endGmtAndCleanUp(PrintWriter out, String psFile, String projectionType) {
        out.println("# end postscript"); 
        out.println("gmt psxy -J"+projectionType+" -R -m -O -T  >> " + psFile);
        out.println("# convert ps to pdf, clean up .ps file"); 
        out.println("gmt psconvert -P -Tf  " + psFile+" && rm " + psFile);
        
        out.println("# clean up after gmt...");
        out.println("rm gmt.history");
    }

    @CommandLine.Option(names = "--prop", description = "load defaults from properties file")
    public void setToolPropsFilename(String filename) throws IOException {
        File f = new File(filename);
        if (! f.exists()) {
            throw new FileNotFoundException(filename); // ToDo better error msg
        }
        Reader r = new BufferedReader(new FileReader(filename));
        toolProps.load(r);
    }

    public abstract void init() throws TauPException;
    public abstract void start() throws IOException, TauModelException, TauPException;
    public abstract void destroy() throws TauPException;
    
    public static String toolNameFromClass(Class toolClass) {
        String className = toolClass.getName();
        className = className.substring(className.lastIndexOf('.') + 1,
                                        className.length());
        String toolName = className;
        if (toolName.startsWith("TauP_")) {
            toolName = toolName.replace('_', ' ');
        }
        if (toolClass.equals(TauP_VelocityPlot.class) ) {
            toolName = "taup velplot";
        } else if (toolClass.equals(TauP_VelocityMerge.class) ) {
            toolName = "taup velmerge";
        } else if (toolClass.equals(TauP_PhaseDescribe.class) ) {
            toolName = "taup phase";
        } 
        
        return toolName.toLowerCase();
    }

    public static String getStdUsageHead(Class toolClass) {
        String toolName = toolNameFromClass(toolClass);
        return "Usage: " + toolName + " [arguments]\n"
        + "  or, for purists, java " + toolClass.getName()
                + " [arguments]\n"
        + "\nArguments are:\n";
    }

    public static String getModDepthUsage() {
        return "-mod[el] modelname -- use velocity model \"modelname\" for calculations\n"
                + "                      Default is iasp91.\n\n"
                + "-h depth           -- source depth in km\n\n"
                + "--stadepth depth   -- receiver depth in km\n"
                + "--scat[ter] depth deg   -- scattering depth and distance\n\n"
        ;
    }

    public static String getStdUsageTail() {
        return "\n-o [stdout|outfile]         -- output is redirected to stdout or to the \"outfile\" file\n"
                + "--prop [propfile]   -- set configuration properties\n"
                + "--debug             -- enable debugging output\n"
                + "--verbose           -- enable verbose output\n"
                + "--version           -- print the version\n"
                + "--help              -- print this out, but you already know that!\n";
    }

    public abstract void validateArguments() throws TauPException;

    /**
     * injected by picocli
     *
     */
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;
}
