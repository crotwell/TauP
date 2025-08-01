package edu.sc.seis.TauP.cmdline.args;

import picocli.CommandLine;

import static edu.sc.seis.TauP.cmdline.args.OutputTypes.*;


public class SeismogramOutputTypeArgs extends AbstractOutputTypeArgs {

    public SeismogramOutputTypeArgs(String defaultFormat, String filebase) {
        super(filebase);
        setOutputFormat(defaultFormat);
    }

    @CommandLine.ArgGroup(heading = "Output Type %n")
    SeismogramOutputType outputType = new SeismogramOutputType();

    public boolean isMS3() {
        return outputType._isMS3;
    }
    public boolean isSAC() {
        return outputType._isSAC;
    }

    @Override
    public String getOutputFormat() {
        if (isSAC()) return SAC;
        if (isMS3()) return MS3;
        throw new RuntimeException("Unknown output format");
    }

    public void setOutputFormat(String oType) {
        outputType._isMS3 = false;
        outputType._isSAC = false;
        if (oType.equalsIgnoreCase(MS3)) {
            outputType._isMS3 = true;
        } else if (oType.equalsIgnoreCase(SAC)) {
            outputType._isSAC = true;
        } else {
            throw new IllegalArgumentException("output type " + oType + " not recognized.");
        }
    }

    @Override
    public String getOutFileExtension() {
        String extention = "ms3";
        if (isSAC()) {
            extention = "sac";
        }
        return extention;
    }

    static class SeismogramOutputType {
        @CommandLine.Option(names = {"--ms3"}, required = true, description = "outputs as mseed3")
        boolean _isMS3 = false;
        @CommandLine.Option(names = {"--sac"}, required = true, description = "outputs as SAC")
        boolean _isSAC = false;
    }

}
