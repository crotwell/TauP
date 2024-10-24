package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.*;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

public class ModelArgs {

    public String getModelName() {
        return modelArgsInner.getModelName();
    }
    public void setModelName(String modelName) {
        unsetDepthCorrected();
        tMod = null;
        modelArgsInner.setModelName( modelName);
    }

    public List<Double> getSourceDepths() {
        return modelArgsInner.sourceDepth;
    }
    public void setSourceDepths(List<Double> depths) {
        modelArgsInner.sourceDepth = depths;
    }
    public List<Double> getReceiverDepths() {
        return modelArgsInner.getReceiverDepths();
    }
    public void setReceiverDepths(List<Double> depths) {
        modelArgsInner.receiverDepths = depths;
    }

    public static String depthsToString(List<Double> depths) {
        StringBuffer buf = new StringBuffer();
        boolean first = true;
        for (Double d : depths) {
            if ( ! first) {
                buf.append(", ");
            }
            buf.append(d);
            first = false;
        }
        return buf.toString();
    }

    /**
     * Returns the scatterer if set, null otherwise.
     *
     * @return the scatterer
     */
    public Scatterer getScatterer() {
        return modelArgsInner.getScatterer();
    }
    public void setScatterer(double depth, double dist) {
        modelArgsInner.setScatterer( depth,  dist);
    }

    public TauModel getTauModel() throws TauModelException {
        if (tMod == null) {
            tMod = modelArgsInner.getTauModel();
        }
        return tMod;
    }

    public void setTMod(TauModel tMod) {
        unsetDepthCorrected();
        this.tMod = tMod;
    }

    public void unsetDepthCorrected() {
        tModDepth = null;
    }

    /**
     * Additional depths, beyond source, receiver and scatterer depths to split the model branches. For example
     * to generate pierce points at other depths.
     */
    public void setModelSplitDepths(List<Double> modelSplitDepths) {
        this.modelSplitDepths = modelSplitDepths;
    }
    public List<Double> getModelSplitDepths() {
        return modelSplitDepths;
    }

    public TauModel depthCorrected(double sourceDepth) throws TauModelException {
        if (tModDepth == null) {
            tModDepth = getTauModel();
        }
        if (tModDepth.getSourceDepth() != sourceDepth) {
            tModDepth = getTauModel().depthCorrect(sourceDepth);
            if (getScatterer() != null && !tModDepth.isBranchDepth(getScatterer().depth)) {
                tModDepth = tModDepth.splitBranch(getScatterer().depth);
            }
            for (Double d : modelSplitDepths) {
                if (! tModDepth.isBranchDepth(d)) {
                    tModDepth = tModDepth.splitBranch(d);
                }
            }
        }
        return tModDepth;
    }

    /**
     * Tau model calculated previously.
     *
     * @see TauModel
     */
    protected TauModel tMod;

    /**
     * TauModel derived from tMod by correcting it for a non-surface source.
     */
    protected transient TauModel tModDepth;

    protected List<Double> modelSplitDepths = new ArrayList<>();

    @CommandLine.ArgGroup(heading = "Model Args %n", exclusive = false)
    ModelArgsInner modelArgsInner = new ModelArgsInner();

    static class ModelArgsInner extends TableModelArgs {

        @CommandLine.Option(names={"-h", "--sourcedepth", "--evdepth"},
                paramLabel = "depth",
                defaultValue = "0.0",
                split=",",
                description = "source depth in km")
        List<Double> sourceDepth = new ArrayList<>();

    }

}
