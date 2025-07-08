package edu.sc.seis.TauP.cmdline;

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
            String simpleType = typeFromJavaType(op.typeInfo().getClassName());
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

        writer.println();
        writer.println("  def calc(self, taupServer):");
        writer.println("    params = self.create_params()");
        writer.println("    return taupServer.queryJson(params, self.toolname)");
        writer.println();

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

    public static void createGetSet(PrintWriter bodyWriter, CommandLine.Model.OptionSpec op ) {
        String varname =dashlessArgName( op.longestName());
        String simpleType = typeFromJavaType(op.typeInfo().getClassName());
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
                    desc(bodyWriter, op, opname);

                    bodyWriter.println("    self._" + varname + ".append(val)");
                    bodyWriter.println("    return self");
                    bodyWriter.println();
                }
            }
        }
    }

    public static void desc(PrintWriter bodyWriter, CommandLine.Model.OptionSpec op, String opname) {
        String varname =dashlessArgName( op.longestName());
        String simpleType = typeFromJavaType(op.typeInfo().getClassName());

        bodyWriter.println("    \"\"\"");
        bodyWriter.print("    Sets the " + varname + " parameter, of type " + simpleType);
        if (!op.typeInfo().getActualGenericTypeArguments().isEmpty()) {
            bodyWriter.println(" of " + typeFromJavaType(op.typeInfo().getActualGenericTypeArguments().get(0)));
        }
        if (simpleType.equals("List")) {
            if (op.arity().max() == 1) {
                bodyWriter.println("    If a single " + typeFromJavaType(op.typeInfo().getActualGenericTypeArguments().get(0))
                        + " is passed in, it is automatically wrapped in a list. So ");
                bodyWriter.println("    x." + opname + "( value )");
                bodyWriter.println("    and ");
                bodyWriter.println("    .x" + opname + "( [ value ] )");
                bodyWriter.println("    are equivalent. ");
            } else if (varname.endsWith("range")) {

            }
        }
        bodyWriter.println("    " + op.descriptionKey());
        for (String descStr : op.description()) {
            bodyWriter.println("    " + descStr);
        }
        if (!opname.equals(varname)) {
            bodyWriter.println("    Also known as " + op.longestName() + " in command line.");
        }
        bodyWriter.println();
        bodyWriter.println("    :param val: value to set " + varname + " to");
        bodyWriter.println("    \"\"\"");
    }

    public static boolean specialSetter(PrintWriter bodyWriter, CommandLine.Model.OptionSpec op, String opname) {
        String simpleType = typeFromJavaType(op.typeInfo().getClassName());
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

    public static List<String> ignoreOptions = List.of(
      "help", "version", "debug", "verbose", "prop",
            "json", "html", "text", "svg", "gmt", "csv", "output"
    );

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
            File initFile = new File(dir, "__init__.py");
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(initFile)));
            out.println("__version__ = \"0.0.1-dev\"");
            out.println();
            out.println("from .http_server import TauPServer");
            for (String toolname : knownTools()) {
                String capToolname = toolname.substring(0, 1).toUpperCase() + toolname.substring(1);
                out.println("from ." + toolname + " import " + capToolname + "Query");
            }
            out.println();
            out.println("__all__ = [");
            out.println("    \"TauPServer\",");
            for (String toolname : knownTools()) {
                String capToolname = toolname.substring(0, 1).toUpperCase() + toolname.substring(1);
                out.println("    \"" + capToolname + "Query\",");
            }
            out.println("]");

            out.close();
            knownTools();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
