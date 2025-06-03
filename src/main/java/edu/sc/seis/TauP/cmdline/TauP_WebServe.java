package edu.sc.seis.TauP.cmdline;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.OutputTypes;
import edu.sc.seis.TauP.gson.GsonUtil;
import edu.sc.seis.seisFile.mseed3.MSeed3Record;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;
import io.undertow.util.MimeMappings;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Pattern;


public class TauP_WebServe extends TauP_Tool {

    /**
     * Default for local usage, no prefix to urls
     *
     */
    public TauP_WebServe() {
        super(null);
    }

    /**
     * Default for local usage, no prefix to urls
     *
     */
    public TauP_WebServe(String wsNamespace) {
        super(null);
        this.wsNamespace = wsNamespace;
    }

    @Override
    public void init() {

    }

    @Override
    public void start() {
        System.out.println();
        System.out.println("   http://localhost:"+port);
        System.out.println();

        HttpHandler taupToolHandler = new HttpHandler() {
            @Override
            public void handleRequest(final HttpServerExchange exchange) throws Exception {
                try {
                    handleTauPRequest(exchange);
                } catch (Exception e) {
                    Alert.warning(e);
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
                Alert.debug("handle TauP: "+path);
                if (path.equals("favicon.ico")) {
                    new ResponseCodeHandler(404).handleRequest(exchange);
                    return;
                }
                while (path.startsWith("/")) {
                    path = path.substring(1); // trim first slash
                }
                String namespace = "";
                String service = DEFAULT_SERVICE;
                String version = DEFAULT_SERVICE_VERSION;
                String toolname = "";
                String cmdLineToolname = "";
                String[] splitPath = path.split("/");

                if (splitPath.length >= 4) {
                    // namespace, service, verson, tool like FDSNWS
                    // uscws/taup/1/time?deg=35&p=PKP
                    namespace = splitPath[0];
                    service = splitPath[1];
                    version = splitPath[2];
                    toolname = splitPath[3];
                    if (splitPath.length>=5) {
                        cmdLineToolname = splitPath[4];
                    }
                    Alert.debug("load via split: ns: "+namespace+" ser: "+service+" ver: "+version+" tool: "+toolname);
                } else if (splitPath.length >= 1) {
                    // simple url where tool is whole path
                    namespace = wsNamespace;
                    service = wsServiceName;
                    version = wsServiceVersion;
                    if (isKnownTool(splitPath[0])) {
                        toolname = splitPath[0];
                        if (splitPath.length>=2) {
                            cmdLineToolname = splitPath[1];
                        }
                        Alert.debug("load via single: "+toolname);
                    }
                }
                if (wsNamespace.equals(namespace) && wsServiceName.equals(service)
                        && wsServiceVersion.equals(version)) {
                    if (toolname.equals(MODEL_NAMES)) {
                        handleKnownModels(exchange);
                    } else if (toolname.equals(CMD_LINE) && ToolRun.isKnownToolName(cmdLineToolname)) {
                        TauP_Tool tool = createTool(cmdLineToolname);
                        handleCmdLine(tool, exchange);
                    } else if (toolname.equals(PARAM_HELP)) {
                        handleParamHelp(exchange);
                    } else if (ToolRun.isKnownToolName(toolname)) {
                        handleTauPTool(exchange, namespace, service, version, toolname);
                    }
                }
            }

            public boolean isKnownTool(String toolname) {
                if (toolname == null || toolname.isEmpty()) {
                    return false;
                }
                if (toolname.startsWith(CMD_LINE)
                        || toolname.equals(PARAM_HELP)
                        || toolname.equals(MODEL_NAMES)) {
                    return true;
                }
                return ToolRun.isKnownToolName(toolname);
            }

            public void handleTauPTool(final HttpServerExchange exchange,
                                       String namespace,
                                       String service,
                                       String version,
                                       String toolname) throws Exception {
                Alert.debug("Try to run as tool: " + toolname);
                Map<String, Deque<String>> queryParams = exchange.getQueryParameters();

                if ( ! ToolRun.isKnownToolName(toolname)) {
                    return;
                }
                TauP_Tool tool = createTool(toolname);
                if (tool != null) {
                    Alert.debug("Handle via TauP Tool:" + toolname);
                    webRunTool(tool, queryParams, exchange);
                } else {
                    Alert.debug("Can't find tool for :"+toolname+" in "+exchange.getRequestPath());
                }

            }
        };

        ResourceHandler resHandler = new ResourceHandler(
                new ClassPathResourceManager(TauP_Web.class.getClassLoader(),
                        "edu/sc/seis/TauP/html"));
        MimeMappings nmm = MimeMappings.builder(true)
                .addMapping("mjs", "application/javascript")
                .addMapping("tvel", "text/plain")
                .addMapping("nd", "text/plain")
                .build();
        resHandler.setMimeMappings(nmm);
        resHandler.addWelcomeFiles("index.html");
        PathHandler pathHandler = new PathHandler(resHandler);
        String prefix = "/"+wsNamespace+"/"+wsServiceName+"/"+wsServiceVersion;
        HttpHandler toolAndResHandler = new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                taupToolHandler.handleRequest(exchange);
                if (exchange.isComplete()) {
                    return;
                }
                if (exchange.getRequestPath().startsWith(prefix)) {
                    exchange.setRequestPath(exchange.getRequestPath().substring(prefix.length()));
                }
                resHandler.handleRequest(exchange);
            }
        };
        HttpHandler serviceVersionList = new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                String path = exchange.getRequestPath();
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length()-1);
                }
                if (path.equals("/"+wsNamespace+"/"+wsServiceName)) {
                    String versionsPage = "<!DOCTYPE html>\n<html><head><title>Service Versions</title></head><body>"
                            + "<h3><a href=\"/"+wsNamespace+"\">"+wsNamespace+"</a>/"+wsServiceName+" Service Versions:</h3>\n";

                    versionsPage += "<a href=\"" + "/" + wsNamespace + "/" + wsServiceName + "/" + wsServiceVersion + "\">"
                            + "/" + wsNamespace + "/" + wsServiceName + "/" + wsServiceVersion + "</a>\n"
                            + "</body></html>";
                    exchange.getResponseSender().send(versionsPage);
                    return;
                }
            }
        };
        HttpHandler servicesList = new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                String path = exchange.getRequestPath();
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length()-1);
                }
                if (path.equals("/"+wsNamespace)) {
                    String servicesPage = "<!DOCTYPE html>\n<html><head><title>Services</title></head><body>"
                            + "<h3>Known "+wsNamespace+" Services:</h3>\n";

                    servicesPage += "<a href=\"" + "/" + wsNamespace + "/" + wsServiceName  + "\">"
                            + "/" + wsNamespace + "/" + wsServiceName  + "</a>\n"
                            + "</body></html>";
                    exchange.getResponseSender().send(servicesPage);
                    return;
                }
            }
        };
        // add taup tools at ws path like /uscws/taup/3/time?deg=35&p=P
        pathHandler.addPrefixPath(prefix, toolAndResHandler);
        // services
        pathHandler.addPrefixPath("/"+wsNamespace, servicesList);
        // versions
        pathHandler.addPrefixPath("/"+wsNamespace+"/"+wsServiceName, serviceVersionList);
        // also add taup tools at root, like /time?deg=35&p=P
        pathHandler.addPrefixPath("/", toolAndResHandler);
        Undertow server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(new BlockingHandler(pathHandler)).build();
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
            case OutputTypes.ND:
            case OutputTypes.LOCSAT:
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
        return ToolRun.getToolForName(toolToRun);
    }

    static Pattern allowedModelNamePat = Pattern.compile("[^[\\w._-]]");

    public static List<String> disableOptions = List.of("o", "output", "help", "version", "phasefile");

    public static List<String> queryParamsToCmdLineArgs(CommandLine.Model.CommandSpec spec,
                                                        Map<String, Deque<String>> queryParams) throws TauPException {
        List<String> out = new ArrayList<>();

        for (String qp : queryParams.keySet()) {
            String dashedQP = (qp.length() == 1 ? "-" : "--")+qp;
            if (disableOptions.contains(qp)) {
                // ignore these options
                throw new TauPException("Query param not allowed: "+qp);
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
                if (op.typeInfo().isBoolean()) {
                    if (qpList.size() == 1 && qpList.getFirst().equalsIgnoreCase("false")) {
                        // don't add boolean flags if value is false as flag means true
                    } else if (qpList.size() == 1 && qpList.getFirst().equalsIgnoreCase("true")) {
                        // skip value as just a flag
                        out.add(dashedQP);
                    }
                } else {
                    out.add(dashedQP);
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
                Alert.warning(unarg);
            }
            Alert.warning(spec.commandLine().getUsageMessage());
            throw new TauPException("Unknown parameter: "+qp+" value:"+qpList.getFirst());
        }
        return out;
    }

    public void handleParamHelp(HttpServerExchange exchange) throws TauPException {
        TauP_Tool tool;
        Map<String, Deque<String>> queryParams = exchange.getQueryParameters();
        if (queryParams.containsKey("tool")) {
            String toolname = queryParams.get("tool").getFirst();
            tool = createTool(toolname);
            JsonObject out = new JsonObject();
            out.addProperty("tool", toolname);
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
                    out.addProperty("param", arg);
                    out.addProperty(JSONLabels.DESC, desc);
                }
            } else {
                out.addProperty("name", cmd.getCommandName());
                JsonArray allOps = new JsonArray();
                out.add("params", allOps);
                for (CommandLine.Model.OptionSpec op : spec.options()) {
                    JsonObject opObj = new JsonObject();
                    JsonArray nameArr = new JsonArray(op.names().length);
                    for (String n : op.names()) {
                        nameArr.add(n);
                    }
                    opObj.add("name", nameArr);
                    String descStr = "";
                    for (String n : op.description()) {
                        descStr += n+"\n";
                    }
                    opObj.addProperty("desc", descStr.trim());
                    allOps.add(opObj);
                }
            }
            //configContentType(OutputTypes.JSON, exchange);
            Gson gson = GsonUtil.createGsonBuilder().create();
            exchange.getResponseSender().send(gson.toJson(out));
            return;
        }
        throw new TauPException("Unable to create param help for "+exchange.getQueryString()+" "+(queryParams.containsKey("tool")));
    }

    public void handleKnownModels(HttpServerExchange exchange) {
        Gson gson = GsonUtil.createGsonBuilder().create();
        exchange.getResponseSender().send(gson.toJson(getKnownModels()));
    }

    public List<String> getKnownModels() {
        List<String> out = new ArrayList<>();
            out.addAll(TauModelLoader.defaultModelList);
        out.addAll(TauModelLoader.otherVelocityModels.keySet());
        return out;
    }

    public void handleCmdLine(TauP_Tool tool, HttpServerExchange exchange) throws TauPException {
        CommandLine cmd = new CommandLine(tool);
        CommandLine.Model.CommandSpec spec = cmd.getCommandSpec();
        List<String> argList = queryParamsToCmdLineArgs(spec, exchange.getQueryParameters());
        StringBuilder buffer = new StringBuilder();
        buffer.append(TauP_Tool.toolNameFromClass(tool.getClass()));
        for (String s : argList) {
            buffer.append(" " + s);
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
        ArrayList<String> modList = new ArrayList<>();
        if (queryParams.containsKey("mod")) {
            modList.addAll(queryParams.get("mod"));
        }
        if (queryParams.containsKey("model")) {
            modList.addAll(queryParams.get("model"));
        }
        List<String> knownModels = getKnownModels();
        for (String mod : modList) {
            if ( ! knownModels.contains(mod)) {
                throw new VelocityModelException("Unknown model "+mod+" in "+tool.getClass().getName());
            }
        }
        argList.add("-o");
        argList.add("stdout");
        
        StringBuilder buffer = new StringBuilder();
        buffer.append(TauP_Tool.toolNameFromClass(tool.getClass()));
        for (String s : argList) {
            buffer.append(" "+s);
        }
        Alert.debug("  "+buffer);
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
                TauP_Spikes taup_spikes = (TauP_Spikes) tool;
                try {
                    taup_spikes.validateArguments();
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
                List<MSeed3Record> spikeRecords = taup_spikes.calcSpikes(taup_spikes.getRayCalculatables());
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
                    Alert.warning("\nWARN: status code non-zero: "+statusCode+"\n");
                }

                configContentType(tool.getOutputFormat(), exchange);
                exchange.getResponseSender().send(sw.toString());

            }

        } catch (Exception e) {
            Alert.warning("\nException in tool exec: "+e.getMessage()+"\n");
            Alert.warning("  "+buffer);
            Alert.warning("  "+e);
            throw e;
        }
    }

    @Override
    public String getOutputFormat() {
        return null;
    }

    // see edu.sc.seis.TauP.TauP_Web for picocli cmd line interface
    public int port = 7409;

    public String host = "localhost";

    public String wsNamespace = DEFAULT_SERVICE_NAMESPACE;
    public String wsServiceName = DEFAULT_SERVICE;
    public String wsServiceVersion = DEFAULT_SERVICE_VERSION;

    public static final String PARAM_HELP = "paramhelp";
    public static final String CMD_LINE = "cmdline";
    public static final String MODEL_NAMES = "modelnames";

    public static final String DEFAULT_SERVICE_NAMESPACE = "uscws";
    public static final String DEFAULT_SERVICE = "taup";
    public static final String DEFAULT_SERVICE_VERSION = "3";

}
