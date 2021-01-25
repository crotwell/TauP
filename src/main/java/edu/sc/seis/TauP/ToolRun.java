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
	public static String SETSAC = "setsac";
    public static String SPLOT = "slowplot";
	public static String TABLE = "table";
    public static String VPLOT = "velplot";
	public static String WAVEFRONT = "wavefront";
	
	static String[] toolnames = { CREATE, CURVE, PATH, PHASE, PIERCE, SETSAC, SPLOT, TABLE, TIME, VPLOT, WAVEFRONT };
	
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
		if (args.length == 0) {
			printUsage();
			return;
		}
		String toolToRun = args[0];
		String[] restOfArgs = Arrays.copyOfRange(args, 1, args.length);

		try {
			if (toolToRun.contentEquals(CREATE)) {
				TauP_Create tauPCreate = new TauP_Create();
				String[] noComprendoArgs = tauPCreate.parseCmdLineArgs(restOfArgs);
				TauP_Time.printNoComprendoArgs(noComprendoArgs);
                if (noComprendoArgs.length != 0) {
                    return;
                }
				if (tauPCreate.verbose) {
					System.out.println("TauP_Create starting...");
				}

				tauPCreate.loadVMod();
				tauPCreate.start();
				if (tauPCreate.verbose) {
					System.out.println("Done!");
				}
			} else if (toolToRun.contentEquals(CURVE)) {
				TauP_Curve tauPCurve = new TauP_Curve();
				tauPCurve.setOutFileBase("taup_curve");
				String[] noComprendoArgs = tauPCurve.parseCmdLineArgs(restOfArgs);
				TauP_Curve.printNoComprendoArgs(noComprendoArgs);
                if (noComprendoArgs.length != 0) {
                    return;
                }
				if(tauPCurve.DEBUG) {
					System.out.println("Done reading " + tauPCurve.modelName);
				}
				tauPCurve.init();
				tauPCurve.start();
				tauPCurve.destroy();
			} else if (toolToRun.contentEquals(PATH)) {
				TauP_Path tauPPath = new TauP_Path();
				tauPPath.setOutFileBase("taup_path");
				String[] noComprendoArgs = tauPPath.parseCmdLineArgs(restOfArgs);
				TauP_Path.printNoComprendoArgs(noComprendoArgs);
                if (noComprendoArgs.length != 0) {
                    return;
                }
				tauPPath.init();
				if (TauP_Time.DEBUG) {
					System.out.println("Done reading " + tauPPath.modelName);
				}
				tauPPath.start();
				tauPPath.destroy();
			} else if (toolToRun.contentEquals(PHASE)) {
			    DEBUG = true;
				TauP_Time timetool = new TauP_Time();
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
			} else if (toolToRun.contentEquals(PIERCE)) {
				TauP_Pierce tauPPierce = new TauP_Pierce();
				String[] noComprendoArgs = tauPPierce.parseCmdLineArgs(restOfArgs);
				TauP_Pierce.printNoComprendoArgs(noComprendoArgs);
                if (noComprendoArgs.length != 0) {
                    return;
                }
				if(TauP_Time.DEBUG) {
					System.out.println("Done reading " + tauPPierce.modelName);
				}
				tauPPierce.init();
				tauPPierce.start();
				tauPPierce.destroy();
			} else if (toolToRun.contentEquals(SETSAC)) {
				TauP_SetSac tauPSetSac = new TauP_SetSac();
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
                String[] noComprendoArgs = tauPSPlot.parseCmdLineArgs(restOfArgs);
                TauP_Time.printNoComprendoArgs(noComprendoArgs);
                if (noComprendoArgs.length != 0) {
                    return;
                }
                tauPSPlot.start();
			} else if (toolToRun.contentEquals(TABLE)) {
				TauP_Table me;
				me = new TauP_Table();
				String[] noComprendoArgs = me.parseCmdLineArgs(restOfArgs);
				TauP_Table.printNoComprendoArgs(noComprendoArgs);
                if (noComprendoArgs.length != 0) {
                    return;
                }
				me.init();
				me.start();
			} else if (toolToRun.contentEquals(TIME)) {
				TauP_Time tauPTime = new TauP_Time();
				String[] noComprendoArgs = tauPTime.parseCmdLineArgs(restOfArgs);
				TauP_Time.printNoComprendoArgs(noComprendoArgs);
                if (noComprendoArgs.length != 0) {
                    return;
                }
				tauPTime.init();
				tauPTime.start();
				tauPTime.destroy();
				
			} else if (toolToRun.contentEquals(VPLOT)) {
                TauP_VelocityPlot tauPVPlot = new TauP_VelocityPlot();
                String[] noComprendoArgs = tauPVPlot.parseCmdLineArgs(restOfArgs);
                TauP_Time.printNoComprendoArgs(noComprendoArgs);
                if (noComprendoArgs.length != 0) {
                    return;
                }
                tauPVPlot.start();
            } else if (toolToRun.contentEquals(WAVEFRONT)) {
				TauP_Wavefront tauP_wavefront = new TauP_Wavefront();
	            tauP_wavefront.setOutFileBase("taup_wavefront");
	            String[] noComprendoArgs = tauP_wavefront.parseCmdLineArgs(restOfArgs);
	            TauP_Wavefront.printNoComprendoArgs(noComprendoArgs);
                if (noComprendoArgs.length != 0) {
                    return;
                }
                boolean doInteractive = true;
                for (int i = 0; i < args.length; i++) {
                    if (dashEquals("h", args[i])) {
                        doInteractive = false;
                    }
                }
                tauP_wavefront.init();
                if (doInteractive) {
                    tauP_wavefront.start();
                } else {
                    /* enough info given on cmd line, so just do one calc. */
                    tauP_wavefront.calculate(tauP_wavefront.degrees);
                    tauP_wavefront.printResult(tauP_wavefront.getWriter());
                }
	            if (tauP_wavefront.DEBUG) {
	                System.out.println("Done reading " + tauP_wavefront.modelName);
	            }
	            tauP_wavefront.destroy();
			} else {
				System.err.println("Tool "+toolToRun+" not recognized.");
				printUsage();
				return;
			}
		} catch(Exception e) {
			System.err.println("Error starting tool: "+e);
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
