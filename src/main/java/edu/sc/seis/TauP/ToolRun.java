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
	public static String WAVEFRONT = "wavefront";
	
	static String[] toolnames = { GUI, CREATE, CURVE, PATH, PHASE, PIERCE, SETSAC, SPLOT, TABLE, TIME, VPLOT, WAVEFRONT };
	
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
            if(dashEquals("version", args[i])) {
                Alert.info(BuildVersion.getDetailedVersion());
                noComprendoArgs[numNoComprendoArgs++] = args[i];
            } else if(dashEquals("verbose", args[i])) {
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

                System.out.println("Run GUI "+toolToRun+" "+args.length);
                TauP t = new TauP();
                t.setQuitExits(true);
                t.show();
                System.out.println("Done with "+toolToRun);
                return;
            } else if (toolToRun.contentEquals(CREATE)) {
				TauP_Create tauPCreate = new TauP_Create();
				tool = tauPCreate;
			} else if (toolToRun.contentEquals(CURVE)) {
				TauP_Curve tauPCurve = new TauP_Curve();
				tool = tauPCurve;
			} else if (toolToRun.contentEquals(PATH)) {
				TauP_Path tauPPath = new TauP_Path();
				tool = tauPPath;
			} else if (toolToRun.contentEquals(PHASE)) {
			    DEBUG = true;
				TauP_Time timetool = new TauP_Time();
				tool = timetool;
				String[] noComprendoArgs = timetool.parseCmdLineArgs(restOfArgs);
				TauP_Time.printNoComprendoArgs(noComprendoArgs);
				if (noComprendoArgs.length != 0) {
					return;
				}
				timetool.init();
				List<SeismicPhase> phaseList = timetool.getSeismicPhases();
				for (SeismicPhase phase: phaseList) {
					System.out.println(phase.describe());
                    System.out.println("--------");
				}
                return;
			} else if (toolToRun.contentEquals(PIERCE)) {
				TauP_Pierce tauPPierce = new TauP_Pierce();
				tool = tauPPierce;
			} else if (toolToRun.contentEquals(SETSAC)) {
				TauP_SetSac tauPSetSac = new TauP_SetSac();
				tool = tauPSetSac;
				String[] noComprendoArgs = tauPSetSac.parseCmdLineArgs(restOfArgs);
				TauP_SetSac.printNoComprendoArgs(noComprendoArgs);
                if (noComprendoArgs.length != 0) {
                    return;
                }
				if(TauP_Time.DEBUG) {
					System.out.println("Done reading " + tauPSetSac.modelName);
				}
				tauPSetSac.init();
				tauPSetSac.start();
				if (tauPSetSac.sacFileNames.size() == 0) {
					tauPSetSac.printUsage();
				}
            } else if (toolToRun.contentEquals(SPLOT)) {
                TauP_SlownessPlot tauPSPlot = new TauP_SlownessPlot();
                tool = tauPSPlot;
			} else if (toolToRun.contentEquals(TABLE)) {
				TauP_Table tableTool = new TauP_Table();
				tool = tableTool;
			} else if (toolToRun.contentEquals(TIME)) {
				TauP_Time tauPTime = new TauP_Time();
				tool = tauPTime;
			} else if (toolToRun.contentEquals(VPLOT)) {
                TauP_VelocityPlot tauPVPlot = new TauP_VelocityPlot();
                tool = tauPVPlot;
            } else if (toolToRun.contentEquals(WAVEFRONT)) {
				TauP_Wavefront tauP_wavefront = new TauP_Wavefront();
				tool = tauP_wavefront;
			} else {
				System.err.println("Tool "+toolToRun+" not recognized.");
				printUsage();
				return;
			}
            
            if (tool == null) {
                String argstring = "";
                for (String ss : args) {
                    argstring += ss;
                }
                throw new RuntimeException("no tool!?!? " + argstring);
            }
            
            String[] noComprendoArgs = tool.parseCmdLineArgs(restOfArgs);
            TauP_Time.printNoComprendoArgs(noComprendoArgs);
            if (noComprendoArgs.length != 0) {
                return;
            }
            tool.init();
            tool.start();
            tool.destroy();
            System.out.println("Done with "+toolToRun);
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
