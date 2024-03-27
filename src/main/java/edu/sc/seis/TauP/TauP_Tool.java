package edu.sc.seis.TauP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import edu.sc.seis.TauP.CLI.OutputTypes;
import picocli.CommandLine;

/**
 * Base class for tools within the TauP Toolkit.
 */
public abstract class TauP_Tool implements Callable<Integer> {

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
        start();
        return 0;
    }

    /** Turns on debugging output. */
    @CommandLine.Option(names = {"--debug"}, description = "display debugging message")
    public static boolean DEBUG = ToolRun.DEBUG;

    /** Turns on verbose output. */
    public boolean verbose = ToolRun.VERBOSE;

    public String outputFormat = OutputTypes.TEXT;

    protected String outFileBase = "";

    @CommandLine.Option(names = {"--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    public String[] cmdLineArgs = new String[0];

    protected PrintWriter writer;

    protected Properties toolProps;

    private String outFileExtension = null;
    

    /* Constructors */
    protected TauP_Tool() {
        toolProps = TauP_Tool.configDefaults();
    }

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


    public abstract String[] allowedOutputFormats();
    
    public abstract String getOutputFormat();

    public abstract void setDefaultOutputFormat();

    public OutputTypes outputType;

    /** usually one of TEXT or JSON. Subclasses may add
     * additional types, for example CSV, GMT or SVG.
     * @param val output format for results
     */
    public void setOutputFormat(String val) {
        boolean found = false;
        for (String t : allowedOutputFormats()) {
            if (t.equals(val)) { found = true;}
        }
        if ( ! found) {
            String allowed = "";
            for (String s : allowedOutputFormats()) { allowed+= s+",";}
            throw new IllegalArgumentException("output format for "+getClass().getName()+" must be one of "+allowed+" but was "+val);
        }
        this.outputFormat = val;
        if (val == OutputTypes.TEXT) {
            setOutFileExtension("txt");
        } else if (val == OutputTypes.GMT) {
            setOutFileExtension("gmt");
        } else if (val == OutputTypes.SVG) {
            setOutFileExtension("svg");
        } else if (val == OutputTypes.CSV) {
            setOutFileExtension("csv");
        } else if (val == OutputTypes.MS3) {
            setOutFileExtension("ms3");
        }
    }
    
    public String getOutFileBase() {
        return outFileBase;
    }
    
    public void setOutFileBase(String outFileBase) {
        this.outFileBase = outFileBase;
    }

    @CommandLine.Option(names = {"-o", "--output"}, description = "output to file, default is stdout.")
    public void setOutFile(String outfile) {
        this.outFileBase = outfile;
        this.outFileExtension = "";
    }
    
    public String getOutFileExtension() {
        return outFileExtension ;
    }
    
    public void setOutFileExtension(String outFileExtension) {
        this.outFileExtension = outFileExtension;
    }



    public String getOutFile() {
        if(getOutFileBase() == null || getOutFileBase().length() == 0 || getOutFileBase().equals("stdout")) {
            return "stdout";
        } else {
            if (getOutFileExtension() == null || getOutFileExtension().length() == 0 || getOutFileBase().endsWith("."+getOutFileExtension())) {
                // don't do a dot if no extension or already there
                return getOutFileBase();
            }
            return getOutFileBase()+"."+getOutFileExtension();
        }
    }
    
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            if(!(getOutFile().equals("stdout") || getOutFile().length()==0)) {
                writer = new PrintWriter(new BufferedWriter(new FileWriter(getOutFile())));
            } else {
                writer = new PrintWriter(new OutputStreamWriter(System.out));
            }
        }
        return writer;
    }

    public void closeWriter() {
        if (writer != null) {
            writer.close();
        }
        writer = null;
    }
    
    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }

        /** a noop that allows overriding classes to print things
         * before results are calculated. For example to set up GMT commands before drawing paths.
         * @param out
         * @throws IOException
         */
    public void printScriptBeginning(PrintWriter out)  throws IOException {}

    
    public void endGmtAndCleanUp(PrintWriter out, String psFile, String projectionType) {
        out.println("# end postscript"); 
        out.println("gmt psxy -J"+projectionType+" -R -m -O -T  >> " + psFile);
        out.println("# convert ps to pdf, clean up .ps file"); 
        out.println("gmt psconvert -P -Tf  " + psFile+" && rm " + psFile);
        
        out.println("# clean up after gmt...");
        out.println("rm gmt.history");
    }
    
    public static boolean dashEquals(String argName, String arg) {
        return ToolRun.dashEquals(argName, arg);
    }

    protected String[] parseOutputFormatCmdLineArgs(String[] origiArgs) {
        List<String> noComprendoArgs = new ArrayList<>(List.of(origiArgs));
        String[] allowed = allowedOutputFormats();
        for (int a = 0; a < allowed.length; a++) {
            for (String arg: noComprendoArgs) {
                if (dashEquals(allowed[a], arg)) {
                    setOutputFormat(allowed[a]);
                    noComprendoArgs.remove(arg);
                    break;
                }
            }
        }
        return noComprendoArgs.toArray(new String[0]);
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

    public static void printNoComprendoArgs(String[] noComprendoArgs) {
        if(noComprendoArgs.length > 0) {
            for(int i = 0; i < noComprendoArgs.length; i++) {
                if(dashEquals("help", noComprendoArgs[i]) 
                        || dashEquals("version", noComprendoArgs[i])) {
                    // short circuit for these args
                    return;
                }
            }
            String outStringA = "I don't understand the following arguments, continuing:";
            String outStringB = "";
            for(int i = 0; i < noComprendoArgs.length; i++) {
                outStringB += noComprendoArgs[i] + " ";
            }
            Alert.warning(outStringA, outStringB);
            noComprendoArgs = null;
        }
    }

    public abstract void validateArguments() throws TauModelException;
}
