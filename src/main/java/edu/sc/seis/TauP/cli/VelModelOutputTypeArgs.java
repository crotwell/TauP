package edu.sc.seis.TauP.cli;

import picocli.CommandLine;

public class VelModelOutputTypeArgs extends AbstractOutputTypeArgs {

    String outType = "nd";

    public VelModelOutputTypeArgs(String filebase) {
        super(filebase);
    }

    @Override
    public void setOutputType(String oType) {
      outType = oType;
    }

    @Override
    public String getOuputFormat() {
        return OutputTypes.ND;
    }

    @Override
    public String getOutFileExtension() {
        return OutputTypes.ND;
    }

}
