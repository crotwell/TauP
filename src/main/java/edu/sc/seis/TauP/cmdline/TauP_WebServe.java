package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.TauP_Tool;
import edu.sc.seis.TauP.cmdline.TauP_Spikes;
import edu.sc.seis.TauP.cmdline.ToolRun;
import edu.sc.seis.TauP.cmdline.args.OutputTypes;
import edu.sc.seis.seisFile.mseed3.MSeed3Record;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;
import io.undertow.util.MimeMappings;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Pattern;


public class TauP_WebServe extends TauP_Tool {


    public TauP_WebServe() {
        super(null);
    }

    @Override
    public void init() {

    }

    @Override
    public void start() {
        System.out.println();
        System.out.println("   http://localhost:"+port);
        System.out.println();

        HttpHandler handler = new HttpHandler() {
            @Override
            public void handleRequest(final HttpServerExchange exchange) throws Exception {
                try {
                    handleTauPRequest(exchange);
                } catch (Exception e) {
                    System.err.println(e);
                    e.printStackTrace(System.err);
                    if(exchange.isResponseChannelAvailable()) {
                        final String errorPage = "<html><head><title>Error</title></head><body>"
                                +"<h3>Internal Error</h3>"
                                +"<p>"+exchange.getRequestURL()+"?"+exchange.getQueryString()+"</p>"
                                +"<p>"+e.getMessage()+"</p>"
                                +"</body></html>";
                        exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, "" + errorPage.length());
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
                        exchange.setStatusCode(500);
                        exchange.getResponseSender().send(errorPage);
                    }
                    exchange.getResponseSender().send(e.getMessage());
                    throw e;
                }
            }

            public void handleTauPRequest(final HttpServerExchange exchange) throws Exception {
                String path = exchange.getRequestPath();
                while (path.startsWith("/")) {
                    path = path.substring(1); // trim first slash
                }
                System.err.println("handleRequest "+path+" from "+exchange.getRequestPath());

                Map<String, Deque<String>> queryParams = exchange.getQueryParameters();
                if (path.equals("favicon.ico")) {
                    new ResponseCodeHandler(404).handleRequest(exchange);
                } else if (path.startsWith("cmdline")) {
                    TauP_Tool tool = createTool(path.substring(7));
                    handleCmdLine(tool, queryParams, exchange);
                } else if (path.equals("paramhelp")) {
                    handleParamHelp(queryParams, exchange);
                } else {
                    TauP_Tool tool = createTool(path);
                    if (tool != null) {
                        System.err.println("Handle via TauP Tool:" + path);
                        webRunTool(tool, queryParams, exchange);
                    } else {
                        System.err.println("Try to load as classpath resource: " + path);
                        ResourceHandler resHandler = new ResourceHandler(
                                new ClassPathResourceManager(TauP_Web.class.getClassLoader(),
                                        "edu/sc/seis/webtaup/html"));
                        MimeMappings nmm = MimeMappings.builder(true).addMapping("mjs", "application/javascript").build();
                        resHandler.setMimeMappings(nmm);
                        resHandler.handleRequest(exchange);
                        if (exchange.isComplete()) {
                            System.err.println(path+" ...as resource complete.");
                        } else {
                            System.err.println(path+" ...not loadable as resource.");
                        }
                    }
                }
            }
        };

        Undertow server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(new BlockingHandler(handler)).build();
        server.start();
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void validateArguments() throws TauModelException {

    }

    public void configContentType(String format, HttpServerExchange exchange) throws TauPException {
        String contentType;
        switch (format) {
            case OutputTypes.TEXT:
            case OutputTypes.GMT:
                contentType = "text/plain";
                break;
            case OutputTypes.CSV:
                contentType = "text/csv";
                break;
            case OutputTypes.SVG:
                contentType = "image/svg+xml";
                break;
            case OutputTypes.JSON:
                contentType = "application/json";
                break;
            case OutputTypes.MS3:
                contentType = "application/x-miniseed3"; // should update to const in seisFile

                break;
            default:
                throw new TauPException("Unknown format: " + format);
        }
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
    }

    public TauP_Tool createTool(String toolToRun) {
        while (toolToRun.startsWith("/")) {
            toolToRun = toolToRun.substring(1);
        }
        TauP_Tool tool = ToolRun.getToolForName(toolToRun);
        return tool;
    }

    static Pattern allowedModelNamePat = Pattern.compile("[^[\\w._-]]");

    public static List<String> disableOptions = List.of("o", "output", "help", "version");

    public static List<String> queryParamsToCmdLineArgs(CommandLine.Model.CommandSpec spec,
                                                        Map<String, Deque<String>> queryParams) throws TauPException {
        List<String> out = new ArrayList<>();

        for (String qp : queryParams.keySet()) {
            String dashedQP = (qp.length() == 1 ? "-" : "--")+qp;
            if (disableOptions.contains(qp)) {
                // ignore these options
                continue;
            }

            CommandLine.Model.OptionSpec op = spec.findOption(dashedQP);

            Deque<String> qpList = queryParams.get(qp);
            if (qp.equalsIgnoreCase("mod") || qp.equalsIgnoreCase("model")) {
                // since this might become a file lookup, make sure no directory chars, so model name
                // can be only [a-zA-Z0-9._-]
                for (String p : qpList) {
                    if (allowedModelNamePat.matcher(p).find()) {
                        throw new TauPException("Modelname contains unallowed characters: "+p);
                    }
                }
            }
            if (op != null) {
                out.add(dashedQP);
                if (qpList.size() == 1 && qpList.getFirst().equalsIgnoreCase("true")) {
                    // skip as just a flag
                } else {
                    if (op.splitRegex().trim().isEmpty()) {
                        // default split is whitespace, so split on comma
                        for (String p : qpList) {
                            out.addAll(Arrays.asList(p.split(",")));
                        }
                    } else if (op.splitRegex().trim().equals(",")) {
                        String commaList = "";
                        for (String p : qpList) {
                            commaList += p+",";
                        }
                        commaList = commaList.substring(0, commaList.length()-1);
                        out.add(commaList);
                    } else {
                        // add as is and let picocli handle comma splitting
                        out.addAll(queryParams.get(qp));
                    }
                }
                continue;
            } else if (qp.equalsIgnoreCase("format")) {
                if (qpList.size() > 1) {
                    throw new TauPException("Only one format at a time: " + qpList.getFirst() + " " + qpList.peek());
                }

                String format = "--" + qpList.getFirst();
                op = spec.findOption(format);
                if (op != null) {
                    out.add(format);
                    continue;
                }
            }
            for (String unarg : spec.commandLine().getUnmatchedArguments()) {
                System.err.println(unarg);
            }
            System.err.println(spec.commandLine().getUsageMessage());
            throw new TauPException("Unknown parameter: "+qp+" value:"+qpList.getFirst());
        }
        return out;
    }

    public void handleParamHelp(Map<String, Deque<String>> queryParams, HttpServerExchange exchange) throws TauPException {
        TauP_Tool tool;
        if (queryParams.containsKey("tool")) {
            String toolname = queryParams.get("tool").getFirst();
            tool = createTool(toolname);
            JSONObject out = new JSONObject();
            out.put("tool", toolname);
            CommandLine cmd = new CommandLine(tool);
            CommandLine.Model.CommandSpec spec = cmd.getCommandSpec();
            if (queryParams.containsKey("param")) {
                String arg = queryParams.get("param").getFirst();
                String dashedQP = (arg.length() == 1 ? "-" : "--")+arg;
                CommandLine.Model.OptionSpec op = spec.findOption(dashedQP);
                if (op != null) {
                    String desc = "";
                    for (String s : op.description()) {
                        desc += s;
                    }
                    out.put("param", arg);
                    out.put("desc", desc);
                }
            } else {
                out.put("name", cmd.getCommandName());
                JSONArray allOps = new JSONArray();
                out.put("params", allOps);
                for (CommandLine.Model.OptionSpec op : spec.options()) {
                    JSONObject opObj = new JSONObject();
                    opObj.put("name", op.names());
                    opObj.put("desc", op.description());
                    allOps.put(opObj);
                }
            }
            //configContentType(OutputTypes.JSON, exchange);
            exchange.getResponseSender().send(out.toString(2));
            return;
        }
        throw new TauPException("Unable to create param help for "+exchange.getQueryString()+" "+(queryParams.containsKey("tool")));
    }

    public void handleCmdLine(TauP_Tool tool, Map<String, Deque<String>> queryParams, HttpServerExchange exchange) throws TauPException {
        CommandLine cmd = new CommandLine(tool);
        CommandLine.Model.CommandSpec spec = cmd.getCommandSpec();
        List<String> argList = queryParamsToCmdLineArgs(spec, queryParams);
        StringBuffer buffer = new StringBuffer();
        buffer.append(TauP_Tool.toolNameFromClass(tool.getClass()));
        for (String s : argList) {
            buffer.append(" "+s);
        }
        configContentType(OutputTypes.TEXT, exchange);
        exchange.getResponseSender().send(buffer.toString());
    }

    public void webRunTool(TauP_Tool tool, Map<String, Deque<String>> queryParams, HttpServerExchange exchange) throws Exception {
        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(tool);
        cmd.setOut(new PrintWriter(sw));
        CommandLine.Model.CommandSpec spec = cmd.getCommandSpec();
        List<String> argList = queryParamsToCmdLineArgs(spec, queryParams);
        argList.add("-o");
        argList.add("stdout");
        
        StringBuffer buffer = new StringBuffer();
        buffer.append(TauP_Tool.toolNameFromClass(tool.getClass()));
        for (String s : argList) {
            buffer.append(" "+s);
        }
        System.err.println(buffer);
        try {
            CommandLine.ParseResult parseResult = cmd.parseArgs(argList.toArray(argList.toArray(new String[0])));
            tool.setOutFileBase("stdout");

            // Did user request usage help (--help)?
            if (cmd.isUsageHelpRequested()) {
                cmd.usage(cmd.getOut());

                // Did user request version help (--version)?
            } else if (cmd.isVersionHelpRequested()) {
                cmd.printVersionHelp(cmd.getOut());
            } else if (tool instanceof TauP_Spikes) {
                // special because output is not text
                TauP_Spikes wkbj = (TauP_Spikes) tool;
                try {
                    wkbj.validateArguments();
                } catch (Exception e) {
                    if(exchange.isResponseChannelAvailable()) {
                        exchange.setStatusCode(500);
                        exchange.setReasonPhrase(e.getMessage());
                        exchange.endExchange();
                        return;
                    }
                }
                List<MSeed3Record> allRecords = new ArrayList<>();
                //List<MSeed3Record> wkbjRecords = wkbj.calcWKBJ(wkbj.getDistances());
                //allRecords.addAll(wkbjRecords);
                List<MSeed3Record> spikeRecords = wkbj.calcSpikes(wkbj.getDistances());
                allRecords.addAll(spikeRecords);

                List<ByteBuffer> bufList = new ArrayList<>();
                for (MSeed3Record ms3 : allRecords) {
                    bufList.add(ms3.asByteBuffer());
                }
                exchange.getResponseSender().send(bufList.toArray(new ByteBuffer[0])); //.send(ByteBuffer.wrap(buf.array()));
            } else {
                // invoke the business logic
                Integer statusCode = tool.call();
                cmd.setExecutionResult(statusCode);
                if (statusCode != 0) {
                    System.err.println("\nWARN: status code non-zero: "+statusCode+"\n");
                }

                configContentType(tool.getOutputFormat(), exchange);
                exchange.getResponseSender().send(sw.toString());

            }

        } catch (Exception e) {
            System.err.println("\nException in tool exec: "+e.getMessage()+"\n");
            System.err.println(buffer);
            System.err.println(e);
            throw e;
        }
    }

    @Override
    public String getOutputFormat() {
        return null;
    }

    // see edu.sc.seis.TauP.TauP_Web for picocli cmd line interface
    public int port = 7049;

    /**
     * Allows TauP_Web to run as an application. Creates an instance of
     * TauP_Web.
     *
     * ToolRun.main should be used instead.
     */
    public static void main(String[] args) {

        String[] argsPlusName = new String[args.length+1];
        argsPlusName[0] = "web";
        System.arraycopy(args, 0, argsPlusName, 1, args.length);
        ToolRun.main(argsPlusName);
    }
}
