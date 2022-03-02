package edu.sc.seis.TauP;

import java.io.IOException;
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
    public static String CREATE = "create";
    public static String GUI = "gui";
	public static String SETSAC = "setsac";
    public static String SPLOT = "slowplot";
	public static String TABLE = "table";
    public static String VPLOT = "velplot";
    public static String VELMERGE = "velmerge";
	public static String WAVEFRONT = "wavefront";
	
	static String[] toolnames = { GUI, CREATE, CURVE, PATH, PHASE, PIERCE, SETSAC, SPLOT, TABLE, TIME, VPLOT, VELMERGE, WAVEFRONT };
	
	public static void printUsage() {
		System.out.println("Usage: taup <tool> <options>");
		System.out.println(" where tool is one of "+Arrays.deepToString(toolnames));
		System.out.println(" taup <tool> --help");
        System.out.println(" for help with a particular tool.");
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
		TauP_Tool tool = null;

		try {
            if (toolToRun.contentEquals(GUI)) {
                TauP_GUI t = new TauP_GUI();
                t.setQuitExits(true);
                t.setVisible(true);
                return;
            } else if (toolToRun.contentEquals(CREATE)) {
				tool = new TauP_Create();
			} else if (toolToRun.contentEquals(CURVE)) {
			    tool = new TauP_Curve();
			} else if (toolToRun.contentEquals(PATH)) {
			    tool = new TauP_Path();
			} else if (toolToRun.contentEquals(PHASE)) {
			    tool = new TauP_PhaseDescribe();
			} else if (toolToRun.contentEquals(PIERCE)) {
			    tool = new TauP_Pierce();
			} else if (toolToRun.contentEquals(SETSAC)) {
			    tool = new TauP_SetSac();
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
			} else if (TauP_Tool.dashEquals("help", toolToRun) || toolToRun.equals("help")) {
                // short circuit for these args
			    printUsage();
                return;
			} else if (dashEquals("version", toolToRun) || toolToRun.equals("version")) {
			    // this handles help and version
                Alert.info(BuildVersion.getDetailedVersion());
                // short circuit for these args
			    return;
			} else {
				System.err.println("Tool "+toolToRun+" not recognized.");
				printUsage();
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
