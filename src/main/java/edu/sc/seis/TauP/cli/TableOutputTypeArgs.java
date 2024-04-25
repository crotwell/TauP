package edu.sc.seis.TauP.cli;

import picocli.CommandLine;

import static edu.sc.seis.TauP.cli.OutputTypes.JSON;
import static edu.sc.seis.TauP.cli.OutputTypes.TEXT;
import static edu.sc.seis.TauP.cli.OutputTypes.CSV;
import static edu.sc.seis.TauP.cli.OutputTypes.LOCSAT;

public class TableOutputTypeArgs extends AbstractOutputTypeArgs {

    public TableOutputTypeArgs(String defaultFormat, String filebase) {
        super(filebase);
        setOutputFormat(defaultFormat);
    }

    @CommandLine.ArgGroup(heading = "Output Type %n")
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
        if (oType.equalsIgnoreCase(TEXT) || oType.equalsIgnoreCase("generic")) {
            outputType._isText = true;
        } else if (oType.equalsIgnoreCase(JSON)) {
            outputType._isJSON = true;
        } else if (oType.equalsIgnoreCase(CSV)) {
            outputType._isCSV = true;
        } else if (oType.equalsIgnoreCase(LOCSAT)) {
            outputType._isLocsat = true;
        } else {
            throw new IllegalArgumentException("output type "+oType+" not recognized.");
        }
    }

    public String getOutputFormat() {
        if (isJSON()) return JSON;
        if (isCSV()) return CSV;
        if (isLocsat()) return LOCSAT;
        return TEXT;
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

    String outFileBase = "taup_table";

    static class TableOutputType {
        @CommandLine.Option(names = {"--text", "--generic"}, required = true, description = "outputs as text")
        boolean _isText = true;
        @CommandLine.Option(names = {"--json"}, required = true, description = "outputs as JSON")
        boolean _isJSON = false;
        @CommandLine.Option(names = {"--csv"}, required = true, description = "outputs as CSV")
        boolean _isCSV = false;
        @CommandLine.Option(names = {"--locsat"}, required = true, description = "outputs as locsat")
        boolean _isLocsat = false;
    }

}

