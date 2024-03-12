package edu.sc.seis.TauP;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

public class ToolRun {
    
    public static boolean DEBUG = false;
    public static boolean VERBOSE = false;

	public static String PHASE = "phase";
	public static String TIME = "time";
	public static String PIERCE = "pierce";
	public static String PATH = "path";
	public static String CURVE = "curve";
	public static String XYPLOT = "xy";
	public static String CREATE = "create";
    public static String GUI = "gui";
	public static String SETSAC = "setsac";
	public static String SETMSEED3 = "setmseed3";
    public static String SPLOT = "slowplot";
	public static String TABLE = "table";
    public static String VPLOT = "velplot";
    public static String VELMERGE = "velmerge";
	public static String WAVEFRONT = "wavefront";
	public static String WEB = "web";
	public static String WKBJ = "wkbj";
	public static String REFLTRANSPLOT = "refltrans";
	public static String VERSION = "version";
	
	static String[] toolnames = { GUI, CREATE, CURVE, PATH, PHASE, PIERCE, SETSAC, SETMSEED3, SPLOT, TABLE, TIME,
			VPLOT, VELMERGE, WAVEFRONT, REFLTRANSPLOT, WKBJ, VERSION, XYPLOT };
	
	public static String getUsage() {
		return "Usage: taup <tool> <options>\n"
		+" where tool is one of "+Arrays.deepToString(toolnames)+"\n"
		+" taup <tool> --help\n"
        +" for help with a particular tool.";
	}

	public static void genUsageDocFiles() throws IOException {
		File toolDocDir = new File("src/doc/sphinx/source/cmdLineHelp");
		for ( String toolname : toolnames) {
			saveUsageToFile(toolname, toolDocDir, "taup_"+toolname+".usage");
		}
	}

	public static void saveUsageToFile(String toolname, File dir, String filename) throws IOException {
		TauP_Tool tool = getToolForName(toolname);
		if (tool == null) {
			return;
		}
		if ( ! dir.isDirectory()) {dir.mkdir(); }
		PrintStream fileOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(dir, filename))));
		fileOut.print(tool.getUsage());
		fileOut.flush();
		fileOut.close();
	}

	public static void printUsage() {
		System.out.println(getUsage());
	}

	/**
	 * Handles version, verbose and debug cmd line flags.
	 * 
	 * @param args command line args to parse
	 * @return all unrecognized args
	 */
    protected static String[] parseCommonCmdLineArgs(String[] args) {
        int i = 0;
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        while(i < args.length) {
            if(dashEquals("verbose", args[i])) {
                VERBOSE = true;
            } else if(dashEquals("debug", args[i])) {
                VERBOSE = true;
                DEBUG = true;
            } else {
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
    
    public static boolean dashEquals(String argName, String arg) {
        return arg.equalsIgnoreCase("-"+argName) || arg.equalsIgnoreCase("--"+argName);
    }

    public static TauP_Tool getToolForName(String toolToRun) {
		TauP_Tool tool = null;
		if (toolToRun.contentEquals(CREATE)) {
			tool = new TauP_Create();
		} else if (toolToRun.contentEquals(CURVE)) {
			tool = new TauP_Curve();
		} else if (toolToRun.contentEquals(XYPLOT)) {
			tool = new TauP_XY();
		} else if (toolToRun.contentEquals(PATH)) {
			tool = new TauP_Path();
		} else if (toolToRun.contentEquals(PHASE)) {
			tool = new TauP_PhaseDescribe();
		} else if (toolToRun.contentEquals(PIERCE)) {
			tool = new TauP_Pierce();
		} else if (toolToRun.contentEquals(SETSAC)) {
			tool = new TauP_SetSac();
		} else if (toolToRun.contentEquals(SETMSEED3)) {
			tool = new TauP_SetMSeed3();
		} else if (toolToRun.contentEquals(SPLOT)) {
			tool = new TauP_SlownessPlot();
		} else if (toolToRun.contentEquals(TABLE)) {
			tool = new TauP_Table();
		} else if (toolToRun.contentEquals(TIME)) {
			tool = new TauP_Time();
		} else if (toolToRun.contentEquals(VPLOT)) {
			tool = new TauP_VelocityPlot();
		} else if (toolToRun.contentEquals(VELMERGE)) {
			tool = new TauP_VelocityMerge();
		} else if (toolToRun.contentEquals(WAVEFRONT)) {
			tool = new TauP_Wavefront();
		} else if (toolToRun.contentEquals(REFLTRANSPLOT)) {
			tool = new TauP_ReflTransPlot();
		} else if (toolToRun.contentEquals(WKBJ)) {
			tool = new TauP_WKBJ();
		} else if (toolToRun.contentEquals(VERSION)) {
			tool = new TauP_Version();
		}
		return tool;
	}
    
	public static void main(String[] args) throws IOException {
	    String toolToRun;
	    String[] restOfArgs = new String[0];
		if (args.length == 0) {
		    // no args, so open the gui
		    toolToRun = GUI;
		} else {
	        toolToRun = args[0];
	        restOfArgs = Arrays.copyOfRange(args, 1, args.length);
		}
		TauP_Tool tool = getToolForName(toolToRun);

		try {
			// special cases:
            if (toolToRun.contentEquals(GUI)) {
                TauP_GUI t = new TauP_GUI();
                t.setQuitExits(true);
                t.setVisible(true);
                return;
			} else if (toolToRun.contentEquals(WEB)) {
            	try {
            		Class webClass = Class.forName("edu.sc.seis.webtaup.TauP_Web");
					Constructor con = webClass.getConstructor();
					if (con != null) {
						tool = (TauP_Tool)con.newInstance();
					}
				} catch (ClassNotFoundException e) {
            		System.err.println("TauP Web does not seem to be installed, the required jar is not on the classpath.");
            		return;
				}
			} else if (TauP_Tool.dashEquals("help", toolToRun) || toolToRun.equals("help")) {
                // short circuit for help or --help
			    printUsage();
                return;
			} else if (dashEquals("getcmdlinehelpfiles", toolToRun)) {
				// this handles --getcmdlinehelpfiles
				genUsageDocFiles();
				return;
			} else if (dashEquals("version", toolToRun)) {
				// this handles --version
				Alert.info(BuildVersion.getDetailedVersion());
				return;
			} else if (tool == null ){
				System.err.println("Tool "+toolToRun+" not recognized.");
				System.out.println("Tool "+toolToRun+" not recognized.");
				System.out.println(getUsage());
				return;
			}
            
            String[] noComprendoArgs = tool.parseCmdLineArgs(restOfArgs);
            TauP_Tool.printNoComprendoArgs(noComprendoArgs);
            if (noComprendoArgs.length != 0) {
                return;
            }
            if (DEBUG) {tool.DEBUG = DEBUG;}
            tool.init();
            tool.start();
            tool.destroy();
            if (tool.verbose) {
                System.out.println("Done with "+toolToRun);
            }
		} catch(Exception e) {
			System.err.println("Error starting tool: "+args[0]+" "+e);
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	static void legacyRunTool(String toolname, String[] args) throws IOException {
	    String[] argsPlusName = new String[args.length+1];
	    argsPlusName[0] = toolname;
	    System.arraycopy(args, 0, argsPlusName, 1, args.length);
	    ToolRun.main(argsPlusName);
	}

}
