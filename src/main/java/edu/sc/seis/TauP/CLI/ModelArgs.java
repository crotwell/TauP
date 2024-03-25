package edu.sc.seis.TauP.CLI;

import picocli.CommandLine;

public class ModelArgs {

    public String getModelName() {
        return modelArgsInner.modelname;
    }
    public void setModelName(String modelName) {
        modelArgsInner.modelname = modelName;
    }
    public double getSourceDepth() {
        return modelArgsInner.sourceDepth;
    }
    public void setSourceDepth(double depth) {
        modelArgsInner.sourceDepth = depth;
    }
    public double getReceiveDepth() {
        return modelArgsInner.receiverDepth;
    }
    public void setReceiverDepth(double depth) {
        modelArgsInner.receiverDepth = depth;
    }

    @CommandLine.ArgGroup(heading = "Model Args %n")
    ModelArgsInner modelArgsInner = new ModelArgsInner();
    static class ModelArgsInner {

        @CommandLine.Option(names={"--mod", "--model"},
                defaultValue = "iasp91",
                description = "use velocity model \"modelname\" for calculations\n" +
                "                      Default is iasp91.")
        String modelname = "iasp91";

        @CommandLine.Option(names={"-h"}, defaultValue = "0.0", description = "source depth in km")
        double sourceDepth = 0.0;

        @CommandLine.Option(names = {"--stadepth"},
                defaultValue = "0.0",
                paramLabel = "station depth",
                description = "the receiver depth in km for stations not at the surface")
        double receiverDepth = 0.0;
    }
}
