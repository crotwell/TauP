package edu.sc.seis.webtaup;

import edu.sc.seis.TauP.*;

import edu.sc.seis.seisFile.BuildVersion;
import io.undertow.Undertow;
import io.undertow.io.Sender;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.SetHeaderHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;


public class TauP_Web extends TauP_Tool {


    @Override
    protected String[] parseCmdLineArgs(String[] origArgs) throws IOException {
        return new String[0];
    }

    @Override
    public void init() throws TauPException {

    }

    @Override
    public void start() throws IOException, TauModelException, TauPException {
        System.out.println("   http://localhost:"+port);
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
                        System.err.println("handleRequest "+path);
                        TauP_Tool tool = createTool(path);
                        Map<String, Deque<String>> queryParams = exchange.getQueryParameters();
                        Set<String> unknownKeys;
                        if (tool != null) {
                            System.err.println("Handle via TauP Tool:" + exchange.getRequestPath());
                            unknownKeys = configTool(tool, queryParams);
                            unknownKeys.remove(QP_DISTDEG);
                            if (unknownKeys.size() > 0) {
                                String errorPage = "<html><head><title>Error</title></head><body>unknown query parameters: ";
                                for (String k : unknownKeys) {
                                    Deque<String> dq = queryParams.get(k);
                                    errorPage += " "+k+"="+(dq!=null?dq.getFirst():"");
                                }
                                errorPage += "</body></html>";
                                exchange.setStatusCode(400);
                                exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, "" + errorPage.length());
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
                                Sender sender = exchange.getResponseSender();
                                sender.send(errorPage);
                                return;
                            }

                            if (tool instanceof TauP_Time) {
                                List<Double> degreesList = new ArrayList<>();
                                if (queryParams.containsKey("distdeg")) {
                                    for (String distListStr : queryParams.get("distdeg")) {
                                        degreesList.addAll(TauP_Time.parseDegreeList(distListStr));
                                    }
                                } else if (tool instanceof TauP_Curve || tool instanceof TauP_Wavefront) {
                                    // doesn't matter for curve or wavefront
                                } else {
                                    final String errorPage = "<html><head><title>Error</title></head><body>distdeg parameter is required</body></html>";
                                    exchange.setStatusCode(400);
                                    exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, "" + errorPage.length());
                                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
                                    Sender sender = exchange.getResponseSender();
                                    sender.send(errorPage);
                                    return;
                                }
                                try {

                                    StringWriter out = new StringWriter();
                                    PrintWriter pw = new PrintWriter(out);
                                    tool.printScriptBeginning(pw);
                                    List<Arrival> arrivalList = ((TauP_Time) tool).calculate(degreesList);
                                    ((TauP_Time) tool).printResult(pw);
                                    configContentType(tool.outputFormat, exchange);
                                    exchange.getResponseSender().send(out.toString());
                                } catch (Exception e) {
                                    System.err.println("Error: " + e);
                                    throw e;
                                }
                            } else if (tool instanceof TauP_VelocityPlot){
                                System.err.println("Handle as VelocityPlot");
                                TauP_VelocityPlot vPlot = (TauP_VelocityPlot)tool;
                                StringWriter out = new StringWriter();
                                PrintWriter pw = new PrintWriter(out);
                                tool.setWriter(pw);
                                tool.printScriptBeginning(pw);
                                vPlot.printResult(pw);
                                configContentType(tool.outputFormat, exchange);
                                exchange.getResponseSender().send(out.toString());
                            } else {
                                System.err.println("Use other tool, likely doesn't work...");
                                StringWriter out = new StringWriter();
                                PrintWriter pw = new PrintWriter(out);
                                tool.setWriter(pw);
                                tool.printScriptBeginning(pw);
                                tool.start();
                                configContentType(tool.outputFormat, exchange);
                                exchange.getResponseSender().send(out.toString());
                            }
                        } else if (exchange.getRequestPath().equals("/favicon.ico")) {
                            new ResponseCodeHandler(404).handleRequest(exchange);
                            return;
                        } else if (exchange.getRequestPath().equals("/version")) {
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                            exchange.getResponseSender().send(BuildVersion.getDetailedVersion());
                        } else {
                            System.err.println("Try to load as classpath resource: "+exchange.getRequestPath());
                            new ResourceHandler(new ClassPathResourceManager(TauP_Web.class.getClassLoader(), "edu/sc/seis/webtaup/html")).handleRequest(exchange);
                        }
                    }
                })).build();
        server.start();
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void printUsage() {

    }

    @Override
    public void validateArguments() throws TauModelException {

    }

    public void configContentType(String format, HttpServerExchange exchange) throws TauPException {
        String contentType;
        if (format.equals(TauP_Tool.TEXT)) {
            contentType = "text/plain";
        } else if (format.equals(TauP_Tool.CSV)) {
            contentType = "text/csv";
        } else if (format.equals(TauP_Tool.SVG)) {
            contentType = "image/svg+xml";
        } else if (format.equals(TauP_Tool.JSON)) {
            contentType = "application/json";
        } else {
            throw new TauPException("Unknown format: "+format);
        }
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
    }

    public TauP_Tool createTool(String toolToRun) throws TauPException {
        TauP_Tool tool = null;
        if (toolToRun.contentEquals(ToolRun.CREATE)) {
            tool = new TauP_Create();
        } else if (toolToRun.contentEquals(ToolRun.CURVE)) {
            tool = new TauP_Curve();
        } else if (toolToRun.contentEquals(ToolRun.PATH)) {
            tool = new TauP_Path();
        } else if (toolToRun.contentEquals(ToolRun.PHASE)) {
            tool = new TauP_PhaseDescribe();
        } else if (toolToRun.contentEquals(ToolRun.PIERCE)) {
            tool = new TauP_Pierce();
        } else if (toolToRun.contentEquals(ToolRun.SPLOT)) {
            tool = new TauP_SlownessPlot();
        } else if (toolToRun.contentEquals(ToolRun.TABLE)) {
            //tool = new TauP_Table();
            tool = null;
        } else if (toolToRun.contentEquals(ToolRun.TIME)) {
            tool = new TauP_Time();
        } else if (toolToRun.contentEquals(ToolRun.VPLOT)) {
            tool = new TauP_VelocityPlot();
        } else if (toolToRun.contentEquals(ToolRun.VELMERGE)) {
            tool = new TauP_VelocityMerge();
        } else if (toolToRun.contentEquals(ToolRun.WAVEFRONT)) {
            tool = new TauP_Wavefront();
        } else {
            System.err.println("Tool '"+toolToRun+"' not recognized.");
            printUsage();
        }
        return tool;
    }

    public static String QP_MODEL = "model";
    public static String QP_DISTDEG = "distdeg";
    public static String QP_EVDEPTH = "evdepth";
    public static String QP_SCATTER = "scatter";
    public static String QP_PHASES = "phases";
    public static String QP_FORMAT = "format";

    // Wavefront
    public static String QP_TIMESTEP = "timestep";
    public static String QP_NEGDIST = "negdist";


    public Set<String> configTool(TauP_Tool tool, Map<String, Deque<String>> queryParameters) throws TauPException, IOException {
        Set<String> unknownKeys = new HashSet<String>();
        unknownKeys.addAll(queryParameters.keySet());

        if (queryParameters.containsKey(QP_FORMAT)) {
            unknownKeys.remove(QP_FORMAT);
            String format = queryParameters.get(QP_FORMAT).getFirst();
            tool.setOutputFormat(format);
        }

        if (tool instanceof TauP_VelocityPlot) {
            TauP_VelocityPlot vplot = (TauP_VelocityPlot) tool;
            if (queryParameters.containsKey(QP_MODEL)) {
                unknownKeys.remove(QP_MODEL);
                vplot.setModelName(queryParameters.get(QP_MODEL).getFirst());
            }
            // ignore evdepth, phases, etc
            unknownKeys.remove(QP_DISTDEG);
            unknownKeys.remove(QP_EVDEPTH);
            unknownKeys.remove(QP_PHASES);
            unknownKeys.remove(QP_SCATTER);
        }
        if (tool instanceof TauP_Time) {
            TauP_Time timeTool = (TauP_Time) tool;
            if (queryParameters.containsKey(QP_MODEL)) {
                unknownKeys.remove(QP_MODEL);
                TauModel tMod = TauModelLoader.load(queryParameters.get(QP_MODEL).getFirst());
                timeTool.setTauModel(tMod);
            } else {
                timeTool.loadTauModel("iasp91");
            }
            if (queryParameters.containsKey(QP_EVDEPTH)) {
                unknownKeys.remove(QP_EVDEPTH);
                timeTool.setSourceDepth(Double.parseDouble(queryParameters.get(QP_EVDEPTH).getFirst()));
            }
            if (queryParameters.containsKey(QP_SCATTER)) {
                unknownKeys.remove(QP_SCATTER);
                String[] splitQP = queryParameters.get(QP_SCATTER).getFirst().split(",");
                if (splitQP.length != 2) {
                    throw new TauPException("Expect depth,distdeg for scatter parameter:"+queryParameters.get(QP_SCATTER).getFirst());
                }
                double scatterDepth = Double.valueOf(splitQP[ 0]).doubleValue();
                double scatterDistDeg = Double.valueOf(splitQP[1]).doubleValue();
                timeTool.setScatterer(scatterDepth, scatterDistDeg);
            }
            String phases = TauP_Time.DEFAULT_PHASES;
            if (queryParameters.containsKey(QP_PHASES)) {
                unknownKeys.remove(QP_PHASES);
                phases = "";
                for (String phStr : queryParameters.get(QP_PHASES)) {
                    phases += " "+phStr;
                }
                phases = phases.trim();
                if (phases.length() == 0 ) {
                    phases = TauP_Time.DEFAULT_PHASES;
                }
            }
            timeTool.parsePhaseList(phases);
            // ignore
            unknownKeys.remove(QP_TIMESTEP);
            unknownKeys.remove(QP_NEGDIST);
        }

        if (tool instanceof TauP_Wavefront) {
            TauP_Wavefront wavefrontTool = ((TauP_Wavefront) tool);
            if (queryParameters.containsKey(QP_TIMESTEP)) {
                unknownKeys.remove(QP_TIMESTEP);
                float timeStep = Float.parseFloat(queryParameters.get(QP_TIMESTEP).getFirst());
                wavefrontTool.setTimeStep(timeStep);
            }
            if (queryParameters.containsKey(QP_NEGDIST)) {
                unknownKeys.remove(QP_NEGDIST);
                wavefrontTool.setNegDistance( true);
            }
        }
        return unknownKeys;
    }

    public static TauP_Web create() {
        return new TauP_Web();
    }


    @Override
    public String[] allowedOutputFormats() {
        return new String[0] ;
    }


    int port = 7049;

    String webRoot = "taupweb";



    /**
     * Allows TauP_Web to run as an application. Creates an instance of
     * TauP_Time.
     *
     * ToolRun.main should be used instead.
     */
    public static void main(String[] args) throws IOException {

        String[] argsPlusName = new String[args.length+1];
        argsPlusName[0] = "web";
        System.arraycopy(args, 0, argsPlusName, 1, args.length);
        ToolRun.main(argsPlusName);
    }
}
