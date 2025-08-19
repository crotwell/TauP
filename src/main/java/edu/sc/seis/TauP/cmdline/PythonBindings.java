package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.BuildVersion;
import edu.sc.seis.TauP.cmdline.args.OutputTypes;
import picocli.CommandLine;

import java.io.*;
import java.util.*;

public class PythonBindings {

    public static String createPython(TauP_Tool tool) {
        StringWriter swConstructor = new StringWriter();
        PrintWriter writer = new PrintWriter(swConstructor);
        StringWriter swBody = new StringWriter();
        PrintWriter bodyWriter = new PrintWriter(swBody);
        StringWriter swParams = new StringWriter();
        PrintWriter paramsWriter = new PrintWriter(swParams);
        CommandLine cmd = new CommandLine(tool);
        cmd.setOut(new PrintWriter(System.out)); // ???
        CommandLine.Model.CommandSpec spec = cmd.getCommandSpec();
        String toolname = TauP_Tool.toolNameFromClass(tool.getClass());
        if (toolname.startsWith("taup ")) {
            toolname = toolname.substring(5);
        }

        autoCodeGenComment(writer);

        List<String> timeResultTools = List.of("time", "pierce", "path");
        if (timeResultTools.contains(toolname)) {
            writer.println("from .dataclass import TimeResult");
            writer.println();
        } else if (toolname.equals("distaz")) {
            writer.println("from .dataclass import DistazResult");
            writer.println();
        } else if (toolname.equals("curve")) {
            writer.println("from .dataclass import CurveResult");
            writer.println();
        } else if (toolname.equals("wavefront")) {
            writer.println("from .dataclass import WavefrontResult");
            writer.println();
        }

        String capToolname = capitalize(toolname);
        String IN = "    ";
        writer.println("class "+capToolname+"Query:");
        writer.println("  def __init__(self):");
        writer.println("    self.toolname= \""+toolname+"\"");
        writer.println();

        paramsWriter.println();
        paramsWriter.println("  def create_params(self):");
        paramsWriter.println("    \"\"\"");
        paramsWriter.println("    Create dict of params suitible for passing to requests query call.");
        paramsWriter.println("    \"\"\"");
        paramsWriter.println("    params = {");
        paramsWriter.println("      \"format\": \"json\",");
        paramsWriter.println("    }");

        Set<String> knownSimpleTypes = new HashSet<>();

        List<String> doneOptions = new ArrayList<>();
        List<CommandLine.Model.OptionSpec> sortedOptions = new ArrayList<>(spec.options());
        sortedOptions.sort(Comparator.comparing(CommandLine.Model.OptionSpec::longestName));
        for (CommandLine.Model.OptionSpec op : sortedOptions) {
            if (doneOptions.contains(op.longestName())) {
                System.err.println("Found duplicate op: "+op);
                continue;
            }
            doneOptions.add(op.longestName());
            String name =dashlessArgName( op.longestName());
            if (ignoreOptions.contains(name)) {
                continue;
            }

            // constructor, default values
            String simpleType = typeFromJavaType(op);
            knownSimpleTypes.add(simpleType);
            String defVal = "None";
            if (simpleType.equals("List")) {
                defVal = "[]";
            }
            writer.println(IN+"self._"+name+"="+defVal);
            // body, methods
            createGetSet(bodyWriter, op);

            if (simpleType.equals("List")) {
                paramsWriter.println("    if len(self._" + name + ") > 0:");
            } else {
                paramsWriter.println("    if self._" + name + " is not None:");
            }
            paramsWriter.println("      params[\""+name+"\"] = self._"+name);

        }

        if (timeResultTools.contains(toolname)
                || toolname.equals("distaz")
                ||  toolname.equals("curve")
                || toolname.equals("wavefront")) {
            writer.println();
            writer.println("  def calc(self, taupServer):");
            writer.println("    \"\"\"");
            writer.println("    Sends all params to the server, returns the result parsed from JSON into dataclasses.");
            writer.println("    \"\"\"");
            writer.println("    params = self.create_params()");
            if (timeResultTools.contains(toolname) ) {
                writer.println("    return TimeResult.from_json(self.calcJson(taupServer))");
            } else if (toolname.equals("distaz")) {
                writer.println("    return DistazResult.from_json(self.calcJson(taupServer))");
            } else if (toolname.equals("curve")) {
                writer.println("    return CurveResult.from_json(self.calcJson(taupServer))");
            } else if (toolname.equals("wavefront")) {
                writer.println("    return WavefrontResult.from_json(self.calcJson(taupServer))");
            }
        }

        writer.println();
        writer.println("  def calcJson(self, taupServer):");
        writer.println("    \"\"\"");
        writer.println("    Sends all params to the server, returns the result parsed from JSON.");
        writer.println("    \"\"\"");
        writer.println("    params = self.create_params()");
        writer.println("    return taupServer.queryJson(params, self.toolname)");
        writer.println();

        List<String> doneFormats = new ArrayList<>();
        for (String format : outputFormatOptions) {
            if (format.equals("json")) {
                // json special as parsed
                continue;
            }
            for (CommandLine.Model.OptionSpec op : sortedOptions) {
                for (String opname : op.names()) {
                    // check if tool supports this output format
                    if (opname.equals("--" + format) && ! doneFormats.contains(format)) {
                        doneFormats.add(format);
                        System.err.println("Found format: "+format+" for "+op.longestName()+" name: "+opname );
                        writer.println();
                        writer.println("  def calc" + capitalize(format) + "(self, taupServer):");
                        writer.println("    \"\"\"");
                        writer.println("    Sends all params to the server, returns the result as a text version of " + format + ".");
                        writer.println("    \"\"\"");
                        writer.println("    params = self.create_params()");
                        writer.println("    return taupServer.query" + capitalize(format) + "(params, self.toolname)");
                        writer.println();
                    }
                }
            }
        }

        paramsWriter.println("    return params");
        paramsWriter.println();

        paramsWriter.close();

        writer.println();
        writer.close();
        bodyWriter.close();
        bodyWriter.println();

        System.err.println("Known Simple Types:");
        for (String s : knownSimpleTypes) {
            System.err.println(s);
        }
        return swConstructor.toString()+ swBody + swParams;
    }

    public static void autoCodeGenComment(PrintWriter writer) {
        writer.println();
        writer.println("# autogenerated from picocli command line arguments in TauP");
        writer.println("# For The TauP Toolkit, version: "+BuildVersion.getVersion());
        writer.println();
    }

    public static void createGetSet(PrintWriter bodyWriter, CommandLine.Model.OptionSpec op ) {
        String varname =dashlessArgName( op.longestName());
        String simpleType = typeFromJavaType(op);
        for (String opname : op.names()) {
            opname = dashlessArgName(opname);
            bodyWriter.println("  def get_" + opname + "(self):");
            bodyWriter.println("    \"\"\"");
            bodyWriter.println("    returns current value of "+varname+" as a " + simpleType);
            bodyWriter.println("    \"\"\"");
            bodyWriter.println("    return self._" + varname);
            bodyWriter.println();
            if ( ! specialSetter(bodyWriter, op, opname)) {
                // normal setter
                bodyWriter.println("  def " + opname + "(self, val):");
                desc(bodyWriter, op, opname);

                if (simpleType.equals("List")) {
                    bodyWriter.println("    if not hasattr(val, \"__getitem__\"):");
                    if (op.arity().max() == 1) {
                        bodyWriter.println("      val = [ val ]");
                    } else {
                        bodyWriter.println("      raise Exception(f\"" + opname + "() requires a list, not {val}\")");
                    }
                }
                bodyWriter.println("    self._" + varname + " = val");
                bodyWriter.println("    return self");
                bodyWriter.println();
                if (simpleType.equals("List")) {

                    bodyWriter.println();
                    bodyWriter.println("  def and" + capitalize(opname) + "(self, val):");
                    desc(bodyWriter, op, opname, true);

                    bodyWriter.println("    self._" + varname + ".append(val)");
                    bodyWriter.println("    return self");
                    bodyWriter.println();
                }
            }
        }
    }

    public static void desc(PrintWriter bodyWriter, CommandLine.Model.OptionSpec op, String opname) {
        desc(bodyWriter, op, opname, false);
    }

    public static void desc(PrintWriter bodyWriter, CommandLine.Model.OptionSpec op, String opname, boolean isAppend) {
        String varname =dashlessArgName( op.longestName());
        String simpleType = typeFromJavaType(op);

        bodyWriter.println("    \"\"\"");
        if (isAppend) {
            bodyWriter.print("    Append a value to the " + varname + " parameter, ");
        } else {
            bodyWriter.print("    Sets the " + varname + " parameter, ");
        }

        if (op.typeInfo().isEnum()) {
            List<String> enums = op.typeInfo().getEnumConstantNames();
            bodyWriter.println("a choice of one of:");
            bodyWriter.print("     " + String.join(", ", enums));
        } else if (isAppend) {
            bodyWriter.print(" of type " + subtypeFromJavaType(op));
        } else {
            bodyWriter.print("of type " + simpleType);
        }
        if (!op.typeInfo().getActualGenericTypeArguments().isEmpty() && !isAppend) {
            bodyWriter.print(" of " + subtypeFromJavaType(op));
        }
        bodyWriter.println();
        if (simpleType.equals("List") && !isAppend) {
            if (op.arity().max() == 1) {
                bodyWriter.println("    If a single " + subtypeFromJavaType(op)
                        + " is passed in, it is automatically wrapped in a list. So");
                bodyWriter.println("    params." + opname + "( value )");
                bodyWriter.println("    and");
                bodyWriter.println("    params." + opname + "( [ value ] )");
                bodyWriter.println("    are equivalent.");
            } else if (varname.endsWith("range") && op.arity().max() == 3) {
                bodyWriter.println("    step or min,max or min,max,step");

            }
        }
        bodyWriter.println();
        for (String descStr : op.description()) {
            bodyWriter.println("    " + descStr.trim());
        }
        bodyWriter.println();
        for (String n : op.names()) {
            if ( opname.equals( dashlessArgName(n))) {
                bodyWriter.println("    Known as " + n + " in command line.");
            }
        }
        if (!opname.equals(varname)) {
            bodyWriter.println("    Also known as " + op.longestName() + " in command line.");
        }
        bodyWriter.println();
        bodyWriter.println("    :param val: value to set " + varname + " to");
        bodyWriter.println("    \"\"\"");
    }

    public static boolean specialSetter(PrintWriter bodyWriter, CommandLine.Model.OptionSpec op, String opname) {
        String varname =dashlessArgName( op.longestName());
        if (varname.equals("scatter")) {

            bodyWriter.println("  def " + opname + "(self, depth, degree):");
            desc(bodyWriter, op, opname);

            bodyWriter.println("    self._" + varname + " = [depth, degree]");
            bodyWriter.println("    return self");
            bodyWriter.println();
            return true;
        } else if (varname.equals("station") || varname.equals("event")) {
            bodyWriter.println("  def " + opname + "(self, lat, lon):");
            desc(bodyWriter, op, opname);

            bodyWriter.println("    self._" + varname + " = [lat, lon]");
            bodyWriter.println("    return self");
            bodyWriter.println();
            bodyWriter.println("  def and" + capitalize(opname) + "(self, lat, lon):");
            desc(bodyWriter, op, opname);

            bodyWriter.println("    self._" + varname + " += [lat, lon]");
            bodyWriter.println("    return self");
            bodyWriter.println();
            return true;
        }
        return false;
    }

    public static List<String> outputFormatOptions = List.of(
            OutputTypes.GMT,
            OutputTypes.HTML,
            OutputTypes.JSON,
            OutputTypes.CSV,
            OutputTypes.SVG,
            OutputTypes.TEXT,
            OutputTypes.LOCSAT,
            OutputTypes.MS3,
            OutputTypes.SAC,
            OutputTypes.TAUP,
            "nameddiscon" // arg for OutputTypes.ND
    );

    public static List<String> ignoreOptions = new ArrayList<>(List.of(
            "help", "version", "debug", "verbose",
            "prop", "output", "nd", "tvel",
            "quakeml", "staxml", "phasefile", "sid", "eid"
    ));
    static {
        ignoreOptions.addAll(outputFormatOptions);
    }

    public static String subtypeFromJavaType(CommandLine.Model.OptionSpec op) {
        String subtype = "";
        if (!op.typeInfo().getActualGenericTypeArguments().isEmpty()) {
            subtype = typeFromJavaType(op.typeInfo().getActualGenericTypeArguments().get(0));
        }
        return subtype;
    }
    public static String typeFromJavaType(CommandLine.Model.OptionSpec op) {
        String type = op.typeInfo().getClassName();
        return typeFromJavaType(type);
    }
    public static String typeFromJavaType(String type) {
        switch (type) {
            case "java.lang.String":
                return "String";
            case "java.util.List":
                return "List";
            case "java.lang.Float":
            case "float":
                return "Float";
            case "java.lang.Double":
            case "double":
                return "Double";
            case "java.lang.Integer":
            case "int":
                return "Integer";
            case "boolean":
                return "Boolean";
        }
        return type;
    }

    public static String dashlessArgName(String argName) {
        if (argName.startsWith("--")) {
            argName = argName.substring(2);
        } else if (argName.startsWith("-")) {
            argName = argName.substring(1);
        }
        return argName;
    }

    public static String capitalize(String s) {
        return s.substring(0,1).toUpperCase()+s.substring(1);
    }

    public static List<String> ignoreCommands = List.of("help", "web", "generate-completion",
            "setsac", "setms3", "create", "spikes");


    public static List<String> knownTools() {
        CommandLine cmd = new CommandLine(new ToolRun());
        cmd.setOut(new PrintWriter(System.out)); // ???
        CommandLine.Model.CommandSpec spec = cmd.getCommandSpec();
        List<String> out = new ArrayList<>();
        for (String subcmd : spec.subcommands().keySet()) {
            if (! ignoreCommands.contains(subcmd)) {
                out.add(subcmd);
            }
        }
        return out;
    }

    public static void main(String[] args) {
        try {
            File dir = new File(".");
            for (String toolname : knownTools()) {
                BufferedWriter out = new BufferedWriter(new FileWriter(new File(dir, toolname + ".py")));
                out.write(createPython(ToolRun.getToolForName(toolname)));
                out.close();
            }

            File taupversionFile = new File(dir, "taupversion.py");
            PrintWriter taupversion = new PrintWriter(new BufferedWriter(new FileWriter(taupversionFile)));
            taupversion.println();
            taupversion.println("# Version of TauP the python code corresponds to. ");
            taupversion.println("# Use with other versions may not work.");
            taupversion.println("TAUP_VERSION = \""+BuildVersion.getVersion()+"\"");
            taupversion.println();
            taupversion.println("TAUP_DOWNLOAD = \"https://doi.org/10.5281/zenodo.15426279\"");
            taupversion.println();
            taupversion.close();

            File initFile = new File(dir, "__init__.py");
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(initFile)));
            out.println("__version__ = \"0.0.1-dev\"");
            out.println();

            out.println("from .taupversion import TAUP_VERSION");
            out.println("from .http_server import TauPServer");
            for (String toolname : knownTools()) {
                String capToolname = toolname.substring(0, 1).toUpperCase() + toolname.substring(1);
                out.println("from ." + toolname + " import " + capToolname + "Query");
            }
            out.println("from .dataclass import (");
            out.println("    Amplitude, Arrival, Fault, PathSegment,");
            out.println("    RelativeArrival, Scatter, Source, TimeDist, TimeResult,");
            out.println("    Isochron, Wavefront, WavefrontResult");
            out.println(")");

            out.println();
            out.println("__all__ = [");
            out.println("    \"TAUP_VERSION\",");
            out.println("    \"TauPServer\",");
            for (String toolname : knownTools()) {
                String capToolname = toolname.substring(0, 1).toUpperCase() + toolname.substring(1);
                out.println("    \"" + capToolname + "Query\",");
            }
            out.println("    \"Amplitude\",");
            out.println("    \"Arrival\",");
            out.println("    \"Fault\",");
            out.println("    \"PathSegment\",");
            out.println("    \"RelativeArrival\",");
            out.println("    \"Scatter\",");
            out.println("    \"Source\",");
            out.println("    \"TimeDist\",");
            out.println("    \"TimeResult\",");
            out.println("    \"Isochron\",");
            out.println("    \"Wavefront\",");
            out.println("    \"WavefrontResult\"");
            out.println("]");

            out.close();
            knownTools();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
