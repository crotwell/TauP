package edu.sc.seis.TauP.cmdline.args;

import picocli.CommandLine;

public class TableOutputTypeArgs extends AbstractOutputTypeArgs {

    public TableOutputTypeArgs(String defaultFormat, String filebase) {
        super(filebase);
        setOutputFormat(defaultFormat);
    }

    @CommandLine.ArgGroup(heading = OUTPUTTYPE_HEADING)
    TableOutputType outputType = new TableOutputType();


    public boolean isText() {
        return outputType._isText;
    }
    public boolean isJSON() {
        return outputType._isJSON;
    }
    public boolean isCSV() {
        return outputType._isCSV;
    }
    public boolean isLocsat() {
        return outputType._isLocsat;
    }

    public void setOutputFormat(String oType) {
        outputType._isText = false;
        outputType._isJSON = false;
        outputType._isCSV = false;
        outputType._isLocsat = false;
        if (oType.equalsIgnoreCase(OutputTypes.TEXT) || oType.equalsIgnoreCase("generic")) {
            outputType._isText = true;
        } else if (oType.equalsIgnoreCase(OutputTypes.JSON)) {
            outputType._isJSON = true;
        } else if (oType.equalsIgnoreCase(OutputTypes.CSV)) {
            outputType._isCSV = true;
        } else if (oType.equalsIgnoreCase(OutputTypes.LOCSAT)) {
            outputType._isLocsat = true;
        } else {
            throw new IllegalArgumentException("output type "+oType+" not recognized.");
        }
    }

    public String getOutputFormat() {
        if (isJSON()) return OutputTypes.JSON;
        if (isCSV()) return OutputTypes.CSV;
        if (isLocsat()) return OutputTypes.LOCSAT;
        return OutputTypes.TEXT;
    }

    public String getOutFileExtension() {
        if (extension != null && !extension.isEmpty()) {
            return extension;
        }
        String calcExt = "text";
        if (isJSON()) {
            calcExt = "json";
        }
        if (isCSV()) {
            calcExt = "csv";
        }
        if (isLocsat()) {
            calcExt = "locsat";
        }
        return calcExt;
    }

    static class TableOutputType {
        @CommandLine.Option(names = {"--text", "--generic"}, required = true, description = "outputs as Text")
        boolean _isText = true;
        @CommandLine.Option(names = {"--json"}, required = true, description = "outputs as JSON")
        boolean _isJSON = false;
        @CommandLine.Option(names = {"--csv"}, required = true, description = "outputs as CSV")
        boolean _isCSV = false;
        @CommandLine.Option(names = {"--locsat"}, required = true, description = "outputs as Locsat")
        boolean _isLocsat = false;
    }

}

