package edu.sc.seis.webtaup;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cli.OutputTypes;
import edu.sc.seis.seisFile.BuildVersion;
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
import picocli.CommandLine;

import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.*;


public class TauP_Web extends TauP_Tool {


    public TauP_Web() {
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
        Undertow server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(new BlockingHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        try {
                            handleTauPRequest(exchange);
                        } catch (Exception e) {
                            System.err.println(e);
                            e.printStackTrace(System.err);
                            throw e;
                        }
                    }

                    public void handleTauPRequest(final HttpServerExchange exchange) throws Exception {
                        String path = exchange.getRequestPath().substring(1); // trim first slash
                        System.err.println("handleRequest "+path+" from "+exchange.getRequestPath());
                        TauP_Tool tool = createTool(path);
                        Map<String, Deque<String>> queryParams = exchange.getQueryParameters();
                        if (tool != null) {
                            System.err.println("Handle via TauP Tool:" + path);
                            webRunTool(tool, queryParams, exchange);
                        } else if (exchange.getRequestPath().equals("/favicon.ico")) {
                            new ResponseCodeHandler(404).handleRequest(exchange);
                        } else if (exchange.getRequestPath().equals("/version")) {
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                            exchange.getResponseSender().send(BuildVersion.getDetailedVersion());
                        } else {
                            System.err.println("Try to load as classpath resource: "+exchange.getRequestPath());
                            ResourceHandler resHandler = new ResourceHandler(
                                    new ClassPathResourceManager(TauP_Web.class.getClassLoader(),
                                     "edu/sc/seis/webtaup/html"));
                            MimeMappings nmm = MimeMappings.builder(true).addMapping("mjs", "application/javascript").build();
                            resHandler.setMimeMappings(nmm);
                            resHandler.handleRequest(exchange);
                        }
                    }
                })).build();
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
        if (toolToRun.startsWith("/")) {
            toolToRun = toolToRun.substring(1);
        }
        TauP_Tool tool = ToolRun.getToolForName(toolToRun);
        // special cases:
        if (tool == null) {
            System.err.println("Tool '"+toolToRun+"' not recognized.");
        }
        return tool;
    }

    public static List<String> disableOptions = List.of("o", "help", "version");

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
            } else if (tool instanceof TauP_WKBJ) {
                // special because output is not text
                TauP_WKBJ wkbj = (TauP_WKBJ) tool;
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
        }
    }

    @Override
    public String getOutputFormat() {
        return null;
    }

    int port = 7049;

    String webRoot = "taupweb";

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
