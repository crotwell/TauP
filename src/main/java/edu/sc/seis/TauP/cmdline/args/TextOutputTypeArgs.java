package edu.sc.seis.TauP.cmdline.args;

import picocli.CommandLine;

public class TextOutputTypeArgs extends AbstractOutputTypeArgs {

    public TextOutputTypeArgs(String defaultFormat, String filebase) {
        super(filebase);
        setOutputFormat(defaultFormat);
    }

    @CommandLine.ArgGroup(heading = OUTPUTTYPE_HEADING)
    TextOutputTypeArgs.TextOutputType outputType = new TextOutputTypeArgs.TextOutputType();

    public boolean isText() {
        return outputType._isText;
    }
    public boolean isJSON() {
        return outputType._isJSON;
    }

    @Override
    public String getOutputFormat() {
        if (isJSON()) return OutputTypes.JSON;
        return OutputTypes.TEXT;
    }

    public void setOutputFormat(String oType) {
        outputType._isText = false;
        outputType._isJSON = false;
        if (oType.equalsIgnoreCase(OutputTypes.TEXT)) {
            outputType._isText = true;
        } else if (oType.equalsIgnoreCase(OutputTypes.JSON)) {
            outputType._isText = true;
        } else {
            throw new IllegalArgumentException("output type " + oType + " not recognized.");
        }
    }

    @Override
    public String getOutFileExtension() {
        String extention = "text";
        if (isJSON()) {
            extention = "json";
        }
        return extention;
    }

    static class TextOutputType {
        @CommandLine.Option(names = {"--text"}, required = true, description = "outputs as Text")
        boolean _isText = false;
        @CommandLine.Option(names = {"--json"}, required = true, description = "outputs as JSON")
        boolean _isJSON = false;
    }

}
