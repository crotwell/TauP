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
			
		} else if (toolToRun.contentEquals(CURVE)) {
			
		} else if (toolToRun.contentEquals(PATH)) {
			
		} else if (toolToRun.contentEquals(PHASE)) {
			TauP_Time timetool = new TauP_Time();
			timetool.parseCmdLineArgs(restOfArgs);
			timetool.init();
			List<SeismicPhase> phaseList = timetool.getSeismicPhases();
			for (SeismicPhase phase: phaseList) {
				System.out.println(phase.describe());
				System.out.println("--------");
			}
		} else if (toolToRun.contentEquals(PIERCE)) {
			
		} else if (toolToRun.contentEquals(SETSAC)) {
			
		} else if (toolToRun.contentEquals(TABLE)) {
			
		} else if (toolToRun.contentEquals(TIME)) {
			TauP_Time tauPTime = new TauP_Time();
            String[] noComprendoArgs = tauPTime.parseCmdLineArgs(args);
            TauP_Time.printNoComprendoArgs(noComprendoArgs);
            tauPTime.init();
            tauPTime.start();
            tauPTime.destroy();
		} else if (toolToRun.contentEquals(WAVEFRONT)) {
			
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
