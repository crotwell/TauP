package edu.sc.seis.TauP;

import picocli.CommandLine;

import java.io.*;
import java.util.Arrays;


@CommandLine.Command(name = "taup",
		description="The TauP Toolkit",
		mixinStandardHelpOptions = true,
		separator = " ",
		versionProvider=edu.sc.seis.TauP.CLI.VersionProvider.class,
		subcommands = {
				TauP_Time.class,
				TauP_Pierce.class,
				TauP_Path.class,
				TauP_Create.class,
				TauP_Curve.class,
				TauP_ReflTransPlot.class,
				TauP_SetMSeed3.class,
				TauP_SetSac.class,
				edu.sc.seis.TauP.TauP_VelocityPlot.class,
				TauP_Table.class,
				TauP_Wavefront.class,
				edu.sc.seis.TauP.TauP_PhaseDescribe.class,
				TauP_Web.class,
				CommandLine.HelpCommand.class
		})
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
			VPLOT, VELMERGE, WAVEFRONT, REFLTRANSPLOT, WKBJ, VERSION };
	
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
		if ( ! dir.isDirectory()) {dir.mkdirs(); }
		PrintStream fileOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(dir, filename))));
		CommandLine.usage(tool, fileOut);
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


	public static CommandLine.Help.ColorScheme colorScheme = new CommandLine.Help.ColorScheme.Builder()
			.commands    (CommandLine.Help.Ansi.Style.bold, CommandLine.Help.Ansi.Style.underline)    // combine multiple styles
			.options     (CommandLine.Help.Ansi.Style.fg_black, CommandLine.Help.Ansi.Style.bold)
			.parameters  (CommandLine.Help.Ansi.Style.fg_black)
			.optionParams(CommandLine.Help.Ansi.Style.italic)
			.errors      (CommandLine.Help.Ansi.Style.fg_red, CommandLine.Help.Ansi.Style.bold)
			.stackTraces (CommandLine.Help.Ansi.Style.italic).build();


	public static TauP_Tool getToolForName(String toolToRun) {
		TauP_Tool tool = null;
		if (toolToRun.contentEquals(CREATE)) {
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
		} else if (toolToRun.contentEquals(SETMSEED3)) {
			tool = new TauP_SetMSeed3();
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

	@CommandLine.Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
	boolean versionInfoRequested;

	@CommandLine.Option(names = {"--help"}, usageHelp = true, description = "display this help message")
	boolean usageHelpRequested;


	public static int mainWithExitCode(String[] args) throws IOException {
		// precheck args for debug and verbose to aid in debugging picocli issues
		for (String arg : args) {
			if(dashEquals("verbose", arg)) {
				VERBOSE = true;
			} else if(dashEquals("debug", arg)) {
				VERBOSE = true;
				DEBUG = true;
				TauP_Tool.DEBUG = true;
			}
		}
		CommandLine commandLine = new CommandLine(new edu.sc.seis.TauP.ToolRun());
		commandLine.setPosixClusteredShortOptionsAllowed(false);
		commandLine.setColorScheme(colorScheme);
		if (DEBUG) {
			commandLine.setParameterExceptionHandler(new CommandLine.IParameterExceptionHandler() {

				@Override
				public int handleParseException(CommandLine.ParameterException ex, String[] strings) throws Exception {
					commandLine.getErr().println(commandLine.getColorScheme().stackTraceText(ex));
					commandLine.getErr().println(commandLine.getColorScheme().errorText(ex.getMessage()));
					return 3;
				}
			});
		}
		int result = commandLine.execute(args);
		if (result != 0) {
			System.err.println("Error code: " + result);
		}
		return result;
	}
	public static void main(String... args) {
		try {
			int result = mainWithExitCode(args);
			System.exit(result);
		} catch(Exception e) {
			System.err.println("Error starting tool: "+args[0]+" "+e);
			e.printStackTrace();
			System.exit(1001);
		}
	}
	
	static void legacyRunTool(String toolname, String[] args) throws IOException {
	    String[] argsPlusName = new String[args.length+1];
	    argsPlusName[0] = toolname;
	    System.arraycopy(args, 0, argsPlusName, 1, args.length);
	    ToolRun.main(argsPlusName);
	}

}
