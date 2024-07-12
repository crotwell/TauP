package edu.sc.seis.TauP.cmdline.args;

public class VelModelOutputTypeArgs extends AbstractOutputTypeArgs {

    String outType = "nd";

    public VelModelOutputTypeArgs(String filebase) {
        super(filebase);
    }

    @Override
    public void setOutputFormat(String oType) {
      outType = oType;
    }

    @Override
    public String getOutputFormat() {
        return OutputTypes.ND;
    }

    @Override
    public String getOutFileExtension() {
        return OutputTypes.ND;
    }

}
