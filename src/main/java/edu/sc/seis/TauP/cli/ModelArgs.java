package edu.sc.seis.TauP.cli;

import edu.sc.seis.TauP.*;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ModelArgs {

    public String getModelName() {
        return modelArgsInner.modelname;
    }
    public void setModelName(String modelName) {
        unsetDepthCorrected();
        tMod = null;
        modelArgsInner.modelname = modelName;
    }

    public double getSourceDepth() {
        return modelArgsInner.sourceDepth;
    }
    public void setSourceDepth(double depth) {
        modelArgsInner.sourceDepth = depth;
    }
    public double getReceiverDepth() {
        return modelArgsInner.receiverDepth;
    }
    public void setReceiverDepth(double depth) {
        modelArgsInner.receiverDepth = depth;
    }

    /**
     * Returns the scatterer if set, null otherwise.
     *
     * @return the scatterer
     */
    public Scatterer getScatterer() {
        if (modelArgsInner.scattererDepth != null &&  modelArgsInner.scattererDist != null) {
            return new Scatterer(modelArgsInner.scattererDepth, modelArgsInner.scattererDist);
        }
        return null;
    }
    public void setScatterer(double depth, double dist) {
        modelArgsInner.setScatterer( depth,  dist);
    }

    public TauModel getTauModel() throws TauModelException {
        if (tMod == null) {
            tMod = TauModelLoader.load(getModelName());
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

    public TauModel depthCorrected() throws TauModelException {
        if (tModDepth == null) {
            tModDepth = getTauModel();
        }
        if (tModDepth.getSourceDepth() != getSourceDepth()) {
            tModDepth = getTauModel().depthCorrect(getSourceDepth());
            if (!tModDepth.isBranchDepth(getReceiverDepth())) {
                tModDepth = tModDepth.splitBranch(getReceiverDepth());
            }
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

    static Properties toolProps = TauP_Tool.configDefaults();

    static class ModelArgsInner {

        @CommandLine.Option(names={"--mod", "--model"},
                defaultValue = "iasp91",
                description = {"use velocity model \"modelname\" for calculations. ",
                        "Default is ${DEFAULT-VALUE}. Other builtin models include prem, ak135, ak135fcont, and ak135favg."},

                completionCandidates = StdModelGenerator.StdModelCandidates.class
        )
        String modelname = toolProps.getProperty("taup.model.name",
                "iasp91");

        @CommandLine.Option(names={"-h", "--sourcedepth", "--evdepth"},
                paramLabel = "depth",
                defaultValue = "0.0",
                description = "source depth in km")
        double sourceDepth = Double.parseDouble(toolProps.getProperty("taup.source.depth", "0.0"));

        @CommandLine.Option(names = {"--stadepth", "--receiverdepth"},
                defaultValue = "0.0",
                paramLabel = "depth",
                description = "the receiver depth in km for stations not at the surface")
        double receiverDepth = 0.0;

        @CommandLine.Option(names = {"--scat", "--scatter"},
                arity = "2",
                paramLabel = "s",
                description = "scattering depth and distance in degrees, which may be negative. "+
                        "Only effects phases with 'o' or 'O' in the phase name."
        )
        public void setScatterer(List<Double> depth_dist) {
            if (depth_dist.size() == 2) {
                setScatterer(depth_dist.get(0), depth_dist.get(1));
            } else {
                unsetScatterer();
            }
        }
        public void setScatterer(double depth, double dist) {
            scattererDepth = depth;
            scattererDist = dist;
        }
        public void unsetScatterer() {
            scattererDepth = null;
            scattererDist = null;
        }
        public Double scattererDepth;
        public Double scattererDist;
    }

}
