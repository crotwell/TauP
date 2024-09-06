package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.AbstractOutputTypeArgs;
import edu.sc.seis.TauP.cmdline.args.DistanceArgs;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class TauP_AbstractRayTool extends TauP_AbstractPhaseTool {

    @CommandLine.Mixin
    protected DistanceArgs distanceArgs = new DistanceArgs();

    public TauP_AbstractRayTool(AbstractOutputTypeArgs outputTypeArgs) {
        super(outputTypeArgs);
    }

    public static void writeJSON(PrintWriter pw, String indent,
                                 String modelName,
                                 List<Double> depthList,
                                 double receiverDepth,
                                 List<SeismicPhase> phases,
                                 List<Arrival> arrivals) {
        TauP_AbstractRayTool.writeJSON(pw, indent, modelName, depthList, receiverDepth, phases, arrivals,  false, 4.0f);
    }

    public static void writeJSON(PrintWriter pw, String indent,
                                 String modelName,
                                 List<Double> depthList,
                                 double receiverDepth,
                                 List<SeismicPhase> phases,
                                 List<Arrival> arrivals,
                                 boolean withAmplitude,
                                 float Mw) {
        String innerIndent = indent+"  ";
        String NL = "\n";
        pw.write("{"+NL);
        pw.write(innerIndent+ JSONWriter.valueToString("model")+": "+JSONWriter.valueToString(modelName)+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("sourcedepthlist")+": [");

        boolean firstDp = true;
        for (Double depth : depthList) {
            pw.write((firstDp ? "" : ", ") + JSONWriter.valueToString(depth.floatValue()));
            firstDp = false;
        }

        pw.write("]," +NL);
        pw.write(innerIndent+JSONWriter.valueToString("receiverdepth")+": "+JSONWriter.valueToString((float)receiverDepth)+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("phases")+": [ ");
        boolean first = true;
        for (SeismicPhase phase : phases) {
            if (first) {
                first = false;
            } else {
                pw.write(", ");
            }
            pw.write(JSONWriter.valueToString(phase.getName()));
        }
        pw.write(" ],"+NL);
        if (withAmplitude) {
            pw.write(innerIndent+JSONWriter.valueToString("Mw")+": "+JSONWriter.valueToString(Mw)+","+NL);
        }
        pw.write(innerIndent+JSONWriter.valueToString("arrivals")+": ["+NL);
        first = true;
        for (Arrival arrival : arrivals) {
            if (first) {
                first = false;
            } else {
                pw.write(","+NL);
            }
            try {
                arrival.writeJSON(pw, innerIndent + "  ", withAmplitude);
            } catch (JSONException e) {
                System.err.println("Error in json: "+ arrival);
                throw e;
            }
        }
        pw.write(NL);
        pw.write(innerIndent+"]"+NL);
        pw.write("}"+NL);
    }

    public DistanceArgs getDistanceArgs() {
        return this.distanceArgs;
    }


    @Override
    public List<SeismicPhase> getSeismicPhases() throws TauPException {
        List<SeismicPhase> phases = super.getSeismicPhases();
        // add any depths that are part of eventLatLon locations so we calculate those phases as well
        Set<Double> knownDepths = new HashSet<>();
        for (DistanceRay dr : getDistanceArgs().getDistances()) {
            if (dr.hasSourceDepth()) {
                knownDepths.add(dr.getSourceDepth());
            }
        }
        for (SeismicPhase phase : phases) {
            knownDepths.remove(phase.getSourceDepth());
        }
        for (Double depth : knownDepths) {
            phases.addAll(calcSeismicPhases(depth));
        }
        return phases;
    }

    @Override
    public void validateArguments() throws TauPException {
        if (modelArgs.getTauModel() == null) {
            throw new TauModelException("Model for '"+this.getTauModelName()+"' is null, unable to calculate.");
        }
        for (double sourceDepth : modelArgs.getSourceDepth()) {
            if (modelArgs.getTauModel().getRadiusOfEarth() < sourceDepth) {
                throw new TauModelException("Source depth of " + sourceDepth + " in '" + this.getTauModelName()
                        + "' is greater than radius of earth, " + modelArgs.getTauModel().getRadiusOfEarth() + ", unable to calculate.");
            }
        }
        if (modelArgs.getTauModel().getRadiusOfEarth() < modelArgs.getReceiverDepth()) {
            throw new TauModelException("Receiver depth of "+modelArgs.getReceiverDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+modelArgs.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }
        if (modelArgs.getScatterer() != null && modelArgs.getTauModel().getRadiusOfEarth() < getScattererDepth()) {
            throw new TauModelException("Scatterer depth of "+modelArgs.getScatterer().depth+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+modelArgs.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }
        distanceArgs.validateArguments();

    }

    public abstract List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException;


    public abstract void printResult(PrintWriter out, List<Arrival> arrivalList) throws IOException, TauPException;

}
