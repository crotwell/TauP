package edu.sc.seis.TauP.cli;

import picocli.CommandLine;

import java.io.*;

import static edu.sc.seis.TauP.cli.OutputTypes.JSON;
import static edu.sc.seis.TauP.cli.OutputTypes.TEXT;
import static edu.sc.seis.TauP.cli.OutputTypes.CSV;
import static edu.sc.seis.TauP.cli.OutputTypes.LOCSAT;


import picocli.CommandLine;

import java.io.*;

import static edu.sc.seis.TauP.cli.OutputTypes.*;
import static edu.sc.seis.TauP.cli.OutputTypes.SVG;

public class TableOutputTypeArgs {

    @CommandLine.ArgGroup(exclusive=true, multiplicity="0..1")
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

    public void setOutputType(String oType) {
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

    public String getOuputFormat() {
        if (isJSON()) return JSON;
        if (isCSV()) return CSV;
        if (isLocsat()) return LOCSAT;
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
        if (isCSV()) {
            extention = "csv";
        }
        if (isLocsat()) {
            extention = "locsat";
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

    public PrintWriter createWriter() throws IOException {
        if(!(getOutFile().equals("stdout") || getOutFile().length()==0)) {
            return new PrintWriter(new BufferedWriter(new FileWriter(getOutFile())));
        } else {
            return new PrintWriter(new OutputStreamWriter(System.out));
        }
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

