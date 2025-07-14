package edu.sc.seis.TauP.cmdline.args;

import picocli.CommandLine;

public class TextCsvOutputTypeArgs extends AbstractOutputTypeArgs {

    public TextCsvOutputTypeArgs(String defaultFormat, String filebase) {
        super(filebase);
        setOutputFormat(defaultFormat);
    }

    @CommandLine.ArgGroup(heading = OUTPUTTYPE_HEADING)
    TextCsvOutputType outputType = new TextCsvOutputType();


    public boolean isText() {
        return outputType._isText;
    }
    public boolean isJSON() {
        return outputType._isJSON;
    }
    public boolean isHTML() {
        return outputType._isHTML;
    }
    public boolean isCSV() {
        return outputType._isCSV;
    }

    public void setOutputFormat(String oType) {
        outputType._isText = false;
        outputType._isJSON = false;
        outputType._isHTML = false;
        outputType._isCSV = false;
        if (oType.equalsIgnoreCase(OutputTypes.TEXT)) {
            outputType._isText = true;
        } else if (oType.equalsIgnoreCase(OutputTypes.JSON)) {
            outputType._isJSON = true;
        } else if (oType.equalsIgnoreCase(OutputTypes.HTML)) {
            outputType._isHTML = true;
        } else if (oType.equalsIgnoreCase(OutputTypes.CSV)) {
            outputType._isCSV = true;
        } else {
            throw new IllegalArgumentException("output type "+oType+" not recognized.");
        }
    }

    public String getOutputFormat() {
        if (isJSON()) return OutputTypes.JSON;
        if (isCSV()) return OutputTypes.CSV;
        if (isHTML()) return OutputTypes.HTML;
        return OutputTypes.TEXT;
    }

    public String getOutFileExtension() {
        if (extension != null && !extension.isEmpty()) {
            return extension;
        }
        String calcExt = OutputTypes.TEXT;
        if (isJSON()) {
            calcExt = OutputTypes.JSON;
        } else if (isHTML()) {
            calcExt = OutputTypes.HTML;
        } else if (isCSV()) {
            calcExt = OutputTypes.CSV;
        }
        return calcExt;
    }

    static class TextCsvOutputType {
        @CommandLine.Option(names = {"--text"}, required = true, description = "outputs as Text")
        boolean _isText = false;
        @CommandLine.Option(names = {"--json"}, required = true, description = "outputs as JSON")
        boolean _isJSON = false;
        @CommandLine.Option(names = {"--html"}, required = true, description = "outputs as HTML")
        boolean _isHTML = false;
        @CommandLine.Option(names = {"--csv"}, required = true, description = "outputs as CSV")
        boolean _isCSV = false;
    }

}
