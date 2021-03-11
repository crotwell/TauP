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
import java.util.Properties;

public abstract class TauP_Tool {


    /** Turns on debugging output. */
    public static boolean DEBUG = ToolRun.DEBUG;

    /** Turns on verbose output. */
    public boolean verbose = ToolRun.VERBOSE;

    public String outputFormat = TEXT;

    protected String outFileBase = "";

    public static final String JSON = "json";

    public static final String TEXT = "text";

    /** Turns on expert mode. */
    public static boolean expert = false;
    

    protected PrintWriter writer;

    protected Properties toolProps;

    protected Outputs outForms;
    

    /* Constructors */
    protected TauP_Tool() {
        try {
            toolProps = PropertyLoader.load();
        } catch(Exception e) {
            Alert.warning("Unable to load properties, using defaults.",
                          e.getMessage());
            toolProps = new Properties();
        }
        Outputs.configure(toolProps);
    }

    
    
    public String getOutputFormat() {
        return outputFormat;
    }
    
    /** usually one of TauP_Time.TEXT or TauP_Time.JSON. Subclasses may add
     * additional types, for example TauP_Path.SVG.
     * @param val output format for results
     */
    public void setOutputFormat(String val) {
        this.outputFormat = val;
    }
    
    public String getOutFileBase() {
        return outFileBase;
    }
    
    public void setOutFileBase(String outFileBase) {
        this.outFileBase = outFileBase;
    }
    
    public String getOutFileExtension() {
        return "gmt";
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
            if(!getOutFile().equals("stdout")) {
                writer = new PrintWriter(new BufferedWriter(new FileWriter(getOutFile())));
            } else {
                writer = new PrintWriter(new OutputStreamWriter(System.out));
            }
            printScriptBeginning(writer);
        }
        return writer;
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

    protected abstract String[] parseCmdLineArgs(String[] origArgs) throws IOException;
    public abstract void init() throws TauPException;
    public abstract void start() throws IOException, TauModelException, TauPException;
    public abstract void destroy() throws TauPException;
    public abstract void printUsage();

    /*
     * parses the standard command line args for the taup package. Other tools
     * that subclass this class will likely override this.
     */
    protected String[] parseCommonCmdLineArgs(String[] origArgs) throws IOException {
        int i = 0;
        String[] args = ToolRun.parseCommonCmdLineArgs(origArgs);
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        while(i < args.length) {
            if(dashEquals("help", args[i])) {
                printUsage();
                noComprendoArgs[numNoComprendoArgs++] = args[i];
            } else if(dashEquals("expert", args[i])) {
                expert = true;
            } else if(i < args.length - 1) {
                if(args[i].equalsIgnoreCase("-o")) {
                    outFileBase = args[i + 1];
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
        if(numNoComprendoArgs > 0) {
            String[] temp = new String[numNoComprendoArgs];
            System.arraycopy(noComprendoArgs, 0, temp, 0, numNoComprendoArgs);
            return temp;
        } else {
            return new String[0];
        }
    }

    public static void printStdUsageHead(Class toolClass) {
        String className = toolClass.getName();
        className = className.substring(className.lastIndexOf('.') + 1,
                                        className.length());
        Alert.info("Usage: " + className.toLowerCase() + " [arguments]");
        Alert.info("  or, for purists, java " + toolClass.getName()
                + " [arguments]");
        Alert.info("\nArguments are:");
    }



    public static void printStdUsageTail() {
        Alert.info("\n-o [stdout|outfile]         -- output is redirected to stdout or to the \"outfile\" file\n"
                + "--prop [propfile]   -- set configuration properties\n"
                + "--debug             -- enable debugging output\n"
                + "--verbose           -- enable verbose output\n"
                + "--version           -- print the version\n"
                + "--help              -- print this out, but you already know that!\n");
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
}