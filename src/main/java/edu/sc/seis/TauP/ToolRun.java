package edu.sc.seis.TauP;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ToolRun {

	public static String PHASE = "phase";
	public static String TIME = "time";
	public static String PIERCE = "pierce";
	public static String PATH = "path";
	public static String CURVE = "curve";
	public static String CREATE = "create";
	public static String SETSAC = "setsac";
	public static String TABLE = "table";
	public static String WAVEFRONT = "wavefront";
	
	static String[] toolnames = { CREATE, CURVE, PATH, PHASE, PIERCE, SETSAC, TABLE, TIME, WAVEFRONT };
	
	public static void printUsage() {
		System.out.println("Usage: taup <tool> <options>");
		System.out.println(" where tool is one of "+Arrays.deepToString(toolnames));
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
			} else if (toolToRun.contentEquals(WAVEFRONT)) {
				TauP_Wavefront tauP_wavefront = new TauP_Wavefront();
	            tauP_wavefront.setOutFileBase("taup_wavefront");
	            String[] noComprendoArgs = tauP_wavefront.parseCmdLineArgs(restOfArgs);
	            TauP_Wavefront.printNoComprendoArgs(noComprendoArgs);
                if (noComprendoArgs.length != 0) {
                    return;
                }
	            if (tauP_wavefront.DEBUG) {
	                System.out.println("Done reading " + tauP_wavefront.modelName);
	            }
	            tauP_wavefront.init();
	            tauP_wavefront.start();
	            tauP_wavefront.destroy();
			} else {
				System.err.println("Tool "+toolToRun+" not recognized.");
				printUsage();
				return;
			}
		} catch(Exception e) {
			System.err.println("Error starting tool: "+e);
			e.printStackTrace();
		}
	}

}
