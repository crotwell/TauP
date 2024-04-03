package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.DistanceArgs;
import edu.sc.seis.TauP.cli.Scatterer;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class TauP_AbstractRayTool extends TauP_AbstractPhaseTool {


    @CommandLine.Mixin
    protected DistanceArgs distanceArgs = new DistanceArgs();

    /**
     * Parses a comma separated list of distances and returns them in an array.
     */
    public static List<DistanceRay> parseDegreeList(String degList) {
        List<Double> degreesFound = TauP_AbstractRayTool.parseDoubleList(degList);
        List<DistanceRay> distanceRays = new ArrayList<DistanceRay>();
        for (Double d : degreesFound) {
            distanceRays.add(DistanceRay.ofDegrees(d));
        }
        return distanceRays;
    }

    public static List<Double> parseDoubleList(String degList) {
        degList = degList.trim();
        while (degList.startsWith(",")) {
            degList = degList.substring(1);
        }
        while(degList.endsWith(",")) {
            degList = degList.substring(0, degList.length()-1);
        }
        String[] split = degList.trim().split(",");
        List<Double> degreesFound = new ArrayList<Double>(split.length);
        for (int i = 0; i < split.length; i++) {
            try {
                degreesFound.add(Double.parseDouble(split[i].trim()));
            } catch (NumberFormatException e) {
                // oh well
                System.err.println("can't parse '"+split[i]+"' as number, skipping.");
            }
        }
        return degreesFound;
    }

    public static String resultAsJSON(String modelName,
                                      double depth,
                                      double receiverDepth,
                                      String[] phases,
                                      List<Arrival> arrivals,
                                      boolean withPierce,
                                      boolean withPath) {
        String Q = ""+'"';
        String COMMA = ",";
        String QCOMMA = Q+COMMA;
        String COLON = ": "; // plus space
        String S = "  ";
        String QC = Q+COLON;
        String QCQ = QC+Q;
        String SS = S+S;
        String SQ = S+Q;
        String SSQ = S+SQ;
        String SSSQ = S+SSQ;
        // use cast to float to limit digits printed
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println("{");
        out.println(SQ+"model"+QCQ+modelName+QCOMMA);
        out.println(SQ+"sourcedepth"+QC+(float)depth+COMMA);
        out.println(SQ+"receiverdepth"+QC+(float)receiverDepth+COMMA);
        out.print(SQ+"phases"+Q+": [");
        for(int p=0; p<phases.length; p++) {
            out.print(" "+Q+phases[p]+Q);
            if ( p != phases.length-1) {
                out.print(COMMA);
            }
        }
        out.println(" ]"+COMMA);
        out.println(SQ+"arrivals"+Q+": [");
        for(int j = 0; j < arrivals.size(); j++) {
            Arrival currArrival = arrivals.get(j);
            out.print(currArrival.asJSONObject().toString(2));
            //out.print(currArrival.asJSON(true, SS, withPierce, withPath));
            if (j != arrivals.size()-1) {
                out.print(COMMA);
            }
            out.println();
        }
        out.println(S+"]");
        out.print("}");
        return sw.toString();
    }

    public DistanceArgs getDistanceArgs() {
        return this.distanceArgs;
    }

    @Override
    public void validateArguments() throws TauPException {
        if (modelArgs.getTauModel() == null) {
            throw new TauModelException("Model for '"+this.getTauModelName()+"' is null, unable to calculate.");
        }
        if (modelArgs.getTauModel().getRadiusOfEarth() < getSourceDepth()) {
            throw new TauModelException("Source depth of "+getSourceDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+modelArgs.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }
        if (modelArgs.getTauModel().getRadiusOfEarth() < getReceiverDepth()) {
            throw new TauModelException("Receiver depth of "+getReceiverDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+modelArgs.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }
        if (modelArgs.getScatterer() != null && modelArgs.getTauModel().getRadiusOfEarth() < getScattererDepth()) {
            throw new TauModelException("Scatterer depth of "+getScattererDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+modelArgs.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }

    }

    public abstract List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException;

    public void calcAndPrint(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException, IOException {
        List<Arrival> arrivalList = calcAll(phaseList, shootables);
        printResult(getWriter(), arrivalList);
    }

    public abstract void printResult(PrintWriter out, List<Arrival> arrivalList) throws IOException, TauModelException;
}
