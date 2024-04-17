package edu.sc.seis.TauP.cli;

import picocli.CommandLine;

import java.io.*;

import static edu.sc.seis.TauP.cli.OutputTypes.*;
import static edu.sc.seis.TauP.cli.OutputTypes.SVG;

public class TextOutputTypeArgs {

    @CommandLine.ArgGroup(exclusive=true, multiplicity="0..1", heading = "Output Type %n")
    TextOutputType outputType = new TextOutputType();


    public boolean isText() {
        return outputType._isText;
    }
    public boolean isJSON() {
        return outputType._isJSON;
    }
    public void setOutputType(String oType) {
        outputType._isText = false;
        outputType._isJSON = false;
        if (oType.equalsIgnoreCase(TEXT)) {
            outputType._isText = true;
        } else if (oType.equalsIgnoreCase(JSON)) {
                outputType._isText = true;
        } else {
            throw new IllegalArgumentException("output type "+oType+" not recognized.");
        }
    }

    public String getOuputFormat() {
        if (isJSON()) return JSON;
        return TEXT;
    }

    public String getOutFileBase() {
        return outFileBase;
    }

    public void setOutFileBase(String outFileBase) {
        this.outFileBase = outFileBase;
    }

    public String getOutFileExtension() {
        String extention = "text";
        if (isJSON()) {
            extention = "json";
        }
        return extention;
    }

    public String getOutFile() {
        if(getOutFileBase() == null || getOutFileBase().length() == 0 || getOutFileBase().equals("stdout")) {
            return "stdout";
        } else {
            if (getOutFileExtension() == null || getOutFileExtension().length() == 0 || getOutFileBase().endsWith("."+getOutFileExtension())) {
                // don't do a dot if no extension or already there
                return getOutFileBase();
            }
            return getOutFileBase()+"."+getOutFileExtension();
        }
    }

    public PrintWriter createWriter(PrintWriter stdout) throws IOException {
        if(!(getOutFile().equals("stdout") || getOutFile().length()==0)) {
            return new PrintWriter(new BufferedWriter(new FileWriter(getOutFile())));
        } else {
            return stdout;
        }
    }

    String outFileBase = "taup";

    static class TextOutputType {
        @CommandLine.Option(names = {"--text"}, required = true, description = "outputs as text")
        boolean _isText = true;
        @CommandLine.Option(names = {"--json"}, required = true, description = "outputs as JSON")
        boolean _isJSON = false;
    }

}
