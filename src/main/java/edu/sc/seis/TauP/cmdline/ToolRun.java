package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.Alert;
import edu.sc.seis.TauP.TauPConfig;
import edu.sc.seis.TauP.cmdline.args.VersionProvider;
import picocli.AutoComplete;
import picocli.CommandLine;

import java.io.*;
import java.util.Arrays;


@CommandLine.Command(name = "taup",
		description="The TauP Toolkit",
		mixinStandardHelpOptions = true,
		separator = " ",
		versionProvider= VersionProvider.class,
		subcommands = {
				TauP_Create.class,
				TauP_Curve.class,
				TauP_DistAz.class,
				TauP_Find.class,
				TauP_Path.class,
				TauP_PhaseDescribe.class,
				TauP_Pierce.class,
				TauP_ReflTransPlot.class,
				TauP_SetMSeed3.class,
				TauP_SetSac.class,
				TauP_Spikes.class,
				TauP_Table.class,
				TauP_Time.class,
				TauP_VelocityMerge.class,
				TauP_VelocityPlot.class,
				TauP_Wavefront.class,
				TauP_Web.class,
				AutoComplete.GenerateCompletion.class,
				TauP_Version.class,
				CommandLine.HelpCommand.class
		},
		usageHelpAutoWidth = true)
public class ToolRun {

	public static String PHASE = "phase";
	public static String FIND = "find";
	public static String TIME = "time";
	public static String PIERCE = "pierce";
	public static String PATH = "path";
	public static String CURVE = "curve";
	public static String CREATE = "create";
	public static String SETSAC = "setsac";
	public static String SETMSEED3 = "setms3";
	public static String TABLE = "table";
    public static String VPLOT = "velplot";
    public static String VELMERGE = "velmerge";
	public static String WAVEFRONT = "wavefront";
	public static String SPIKES = "spikes";
	public static String REFLTRANSPLOT = "refltrans";
	public static String DISTAZ = "distaz";
	public static String VERSION = "version";

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
		} else if (toolToRun.contentEquals(FIND)) {
			tool = new TauP_Find();
		} else if (toolToRun.contentEquals(DISTAZ)) {
			tool = new TauP_DistAz();
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
		} else if (toolToRun.contentEquals(SPIKES)) {
			tool = new TauP_Spikes();
		} else if (toolToRun.contentEquals(VERSION)) {
			tool = new TauP_Version();
		}
		return tool;
	}

	@CommandLine.Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
	boolean versionInfoRequested;

	@CommandLine.Option(names = {"--help"}, usageHelp = true, description = "display this help message")
	boolean usageHelpRequested;


	public static int mainWithExitCode(String[] args) {
		// precheck args for debug and verbose to aid in debugging picocli issues
		for (String arg : args) {
			if(dashEquals("verbose", arg)) {
				TauPConfig.VERBOSE = true;
			} else if(dashEquals("debug", arg)) {
				TauPConfig.VERBOSE = true;
                TauPConfig.DEBUG = true;
			}
		}
		CommandLine commandLine = new CommandLine(new edu.sc.seis.TauP.cmdline.ToolRun());
		commandLine.setPosixClusteredShortOptionsAllowed(false);
		commandLine.setColorScheme(colorScheme);
		if (TauPConfig.DEBUG) {
			commandLine.setParameterExceptionHandler(new CommandLine.IParameterExceptionHandler() {

				@Override
				public int handleParseException(CommandLine.ParameterException ex, String[] strings) {
					commandLine.getErr().println(commandLine.getColorScheme().stackTraceText(ex));
					commandLine.getErr().println(commandLine.getColorScheme().errorText(ex.getMessage()));
					return 3;
				}
			});
		}
		int result = commandLine.execute(args);

		if (result != 0) {
			Alert.warning("Error code: " + result);
		}
		return result;
	}
	public static void main(String... args) {
		boolean doSysExit = true;
		for (String arg : args) {
			if (arg.equals("web")) {
				// need taup web to stay alive after calling start
				doSysExit = false;
			}
		}
		try {
			int result = mainWithExitCode(args);
			if (doSysExit) {
				System.exit(result);
			}
		} catch(Exception e) {
			Alert.warning("Error starting tool: "+args[0]+" "+e);
			e.printStackTrace();
			System.exit(1001);
		}
	}
	
	static void legacyRunTool(String toolname, String[] args) {
	    String[] argsPlusName = new String[args.length+1];
	    argsPlusName[0] = toolname;
	    System.arraycopy(args, 0, argsPlusName, 1, args.length);
	    ToolRun.main(argsPlusName);
	}

}
