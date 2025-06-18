package edu.sc.seis.TauP.cmdline;

import picocli.CommandLine;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        String capToolname = toolname.substring(0,1).toUpperCase()+toolname.substring(1);
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
        for (CommandLine.Model.OptionSpec op : spec.options()) {
            if (doneOptions.contains(op.longestName())) {
                System.err.println("Found duplicate op: "+op);
                continue;
            }
            doneOptions.add(op.longestName());
            String name = op.longestName();
            if (name.startsWith("--")) {
                name = name.substring(2);
            } else if (name.startsWith("-")) {
                name = name.substring(1);
            }
            if (ignoreOptions.contains(name)) {
                continue;
            }

            String shortestName = op.shortestName();
            if (shortestName.startsWith("--")) {
                shortestName = shortestName.substring(2);
            } else if (shortestName.startsWith("-")) {
                shortestName = shortestName.substring(1);
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
            bodyWriter.println("  @property");
            bodyWriter.println("  def "+name+"(self):");
            bodyWriter.println("    \"\"\"");
            bodyWriter.println("    "+simpleType);
            bodyWriter.println("    "+op.descriptionKey());
            for (String descStr : op.description()) {
                bodyWriter.println("    " + descStr);
            }
            if ( ! shortestName.equals(name)) {
                bodyWriter.println("    Also known as " + op.shortestName()+ " and "+op.longestName()+" in command line.");
            }
            bodyWriter.println("    \"\"\"");
            bodyWriter.println("    return self._"+name);
            bodyWriter.println();
            bodyWriter.println("  @"+name+".setter");
            bodyWriter.println("  def "+name+"(self, val):");
            if (simpleType.equals("List")) {
                bodyWriter.println("    if not hasattr(val, \"__getitem__\"):");
                bodyWriter.println("      raise Exception(f\"{"+name+"} must be a list, not {val}\")");
            }
            bodyWriter.println("    self._"+name+" = val");
            bodyWriter.println();

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
        return swConstructor.toString()+swBody.toString()+swParams.toString();
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

    public static List<String> ignoreCommands = List.of("help", "web", "generate-completion",
            "setsac", "setms3", "create", "spikes");

    public static List<String> knownTools() {
        CommandLine cmd = new CommandLine(new ToolRun());
        cmd.setOut(new PrintWriter(System.out)); // ???
        CommandLine.Model.CommandSpec spec = cmd.getCommandSpec();
        List<String> out = new ArrayList<>();
        System.err.println("Subcommands:");
        for (String subcmd : spec.subcommands().keySet()) {
            if (! ignoreCommands.contains(subcmd)) {
                System.err.println("  " + subcmd);
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
            for (String toolname : knownTools()) {
                String capToolname = toolname.substring(0, 1).toUpperCase() + toolname.substring(1);
                out.println("from ." + toolname + " import " + capToolname + "Query");
            }
            out.println();
            out.println("__all__ = [");
            for (String toolname : knownTools()) {
                String capToolname = toolname.substring(0, 1).toUpperCase() + toolname.substring(1);
                out.println("    \"" + capToolname + "Query\"");
            }
            out.println("]");

            out.close();
            knownTools();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
