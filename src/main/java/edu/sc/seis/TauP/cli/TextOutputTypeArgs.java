package edu.sc.seis.TauP.cli;

import picocli.CommandLine;

import java.io.*;

import static edu.sc.seis.TauP.cli.OutputTypes.*;
import static edu.sc.seis.TauP.cli.OutputTypes.SVG;

public class TextOutputTypeArgs extends AbstractOutputTypeArgs {

    public TextOutputTypeArgs(String defaultFormat, String filebase) {
        super(filebase);
        setOutputType(defaultFormat);
    }

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1", heading = "Output Type %n")
    TextOutputTypeArgs.TextOutputType outputType = new TextOutputTypeArgs.TextOutputType();

    public boolean isText() {
        return outputType._isText;
    }
    public boolean isJSON() {
        return outputType._isJSON;
    }

    @Override
    public String getOuputFormat() {
        if (isJSON()) return JSON;
        return TEXT;
    }

    public void setOutputType(String oType) {
        outputType._isText = false;
        outputType._isJSON = false;
        if (oType.equalsIgnoreCase(TEXT)) {
            outputType._isText = true;
        } else if (oType.equalsIgnoreCase(JSON)) {
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
        @CommandLine.Option(names = {"--text"}, required = true, description = "outputs as text")
        boolean _isText = true;
        @CommandLine.Option(names = {"--json"}, required = true, description = "outputs as JSON")
        boolean _isJSON = false;
    }

}
