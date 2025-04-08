package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.AbstractOutputTypeArgs;
import edu.sc.seis.TauP.cmdline.args.DistanceArgs;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class TauP_AbstractRayTool extends TauP_AbstractPhaseTool {

    @CommandLine.Mixin
    protected DistanceArgs distanceArgs = new DistanceArgs();

    public TauP_AbstractRayTool(AbstractOutputTypeArgs outputTypeArgs) {
        super(outputTypeArgs);
    }


    public DistanceArgs getDistanceArgs() {
        return this.distanceArgs;
    }

    @Override
    public List<Double> getSourceDepths() throws TauPException {
        List<Double> simpleSourceDepths = modelArgs.getSourceDepths();
        List<Double> out = new ArrayList<>();
        Set<Double> knownDepths = new HashSet<>();
        knownDepths.addAll(simpleSourceDepths);
        for (DistanceRay dr : getDistanceArgs().getDistances()) {
            if (dr.hasSourceDepth()) {
                knownDepths.add(dr.getSourceDepth());
            }
        }
        out.addAll(knownDepths);
        if (out.isEmpty()) {
            out.add(Double.parseDouble(toolProps.getProperty("taup.source.depth", "0.0")));
        }
        return out;
    }

    @Override
    public List<Double> getReceiverDepths() throws TauPException {
        List<Double> simpleReceiverDepths = modelArgs.getReceiverDepths();
        List<Double> out = new ArrayList<>();
        Set<Double> knownDepths = new HashSet<>();
        knownDepths.addAll(simpleReceiverDepths);
        for (DistanceRay dr : getDistanceArgs().getDistances()) {
            if (dr.hasReceiverDepth()) {
                knownDepths.add(dr.getReceiverDepth());
            }
        }
        out.addAll(knownDepths);
        if (out.isEmpty()) {
            out.add(0.0);
        }
        return out;
    }

    @Override
    public void validateArguments() throws TauPException {
        if (modelArgs.getTauModel() == null) {
            throw new TauModelException("Model for '"+this.getTauModelName()+"' is null, unable to calculate.");
        }
        for (double sourceDepth : modelArgs.getSourceDepths()) {
            if (modelArgs.getTauModel().getRadiusOfEarth() < sourceDepth) {
                throw new TauModelException("Source depth of " + sourceDepth + " in '" + this.getTauModelName()
                        + "' is greater than radius of earth, " + modelArgs.getTauModel().getRadiusOfEarth() + ", unable to calculate.");
            }
        }
        for (Double recDepth : modelArgs.getReceiverDepths() ) {
            if (modelArgs.getTauModel().getRadiusOfEarth() < recDepth) {
                throw new TauModelException("Receiver depth of " + recDepth + " in '" + this.getTauModelName()
                        + "' is greater than radius of earth, " + modelArgs.getTauModel().getRadiusOfEarth() + ", unable to calculate.");
            }
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
