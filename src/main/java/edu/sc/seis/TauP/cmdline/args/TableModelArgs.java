package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.TauP_Tool;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TableModelArgs {


    @CommandLine.Option(names={"--mod", "--model"},
            defaultValue = "iasp91",
            description = {"use velocity model \"modelName\" for calculations. ",
                    "Default is ${DEFAULT-VALUE}. Other builtin models include prem, ak135, ak135fcont, and ak135favg."},

            completionCandidates = StdModelGenerator.StdModelCandidates.class
    )
    String modelName = toolProps.getProperty("taup.model.name",
            "iasp91");

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }

    public TauModel getTauModel() throws TauModelException {
        return  TauModelLoader.load(getModelName());
    }

    @CommandLine.Option(names = {"--stadepth", "--receiverdepth"},
            defaultValue = "0.0",
            paramLabel = "depth",
            split=",",
            description = "the receiver depth in km for stations not at the surface")
    List<Double> receiverDepths = new ArrayList<>();

    public List<Double> getReceiverDepths() {
        if (receiverDepths.isEmpty()) {
            receiverDepths.add(0.0);
        }
        return receiverDepths;
    }

    @CommandLine.Option(names = {"--scat", "--scatter"},
            arity = "2",
            paramLabel = "depth deg",
            hideParamSyntax = true,
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

    /**
     * Returns the scatterer if set, null otherwise.
     *
     * @return the scatterer
     */
    public Scatterer getScatterer() {
        if (scattererDepth != null &&  scattererDist != null) {
            return new Scatterer(scattererDepth, scattererDist);
        }
        return null;
    }

    public Double scattererDepth;
    public Double scattererDist;

    static Properties toolProps = TauP_Tool.configDefaults();
}
