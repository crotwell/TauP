package edu.sc.seis.TauP;

import edu.sc.seis.TauP.CLI.DistanceArgs;
import edu.sc.seis.TauP.CLI.Scatterer;
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

    public DistanceArgs getDistanceArgs() {
        return this.distanceArgs;
    }

    @Override
    public void validateArguments() throws TauModelException {
        if (this.getTauModel() == null) {
            throw new TauModelException("Model for '"+this.getTauModelName()+"' is null, unable to calculate.");
        }
        if (this.getTauModel().getRadiusOfEarth() < getSourceDepth()) {
            throw new TauModelException("Source depth of "+getSourceDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+this.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }
        if (this.getTauModel().getRadiusOfEarth() < getReceiverDepth()) {
            throw new TauModelException("Receiver depth of "+getReceiverDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+this.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }
        if (this.getScatterer() != null && this.getTauModel().getRadiusOfEarth() < getScattererDepth()) {
            throw new TauModelException("Scatterer depth of "+getScattererDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+this.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }

    }

    public abstract List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException;

    public void calcAndPrint(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException, IOException {
        List<Arrival> arrivalList = calcAll(phaseList, shootables);
        printResult(getWriter(), arrivalList);
    }

    public abstract void printResult(PrintWriter out, List<Arrival> arrivalList) throws IOException;
}
