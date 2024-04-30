package edu.sc.seis.TauP.cli;

import picocli.CommandLine;

import java.util.Objects;

import static edu.sc.seis.TauP.cli.OutputTypes.*;

public class VelPlotOutputTypeArgs extends AbstractOutputTypeArgs {


    public VelPlotOutputTypeArgs(String defaultFormat, String filebase) {
        super(filebase);
        setOutputFormat(defaultFormat);
    }

    @Override
    public String getOutputFormat() {

        if (isGMT()) return GMT;
        if (isSVG()) return SVG;
        if (isJSON()) return JSON;
        if (isCSV()) return CSV;
        if (isND()) return ND;
        return TEXT;
    }

    @Override
    public String getOutFileExtension() {
        String ext;
        if (extension != null) {
            return extension;
        } else {
            ext = TEXT;
        }
        if (isSVG()) {
            ext = SVG;
        } else if (isJSON()) {
            ext = JSON;
        } else if (isGMT()) {
            ext = GMT;
        } else if (isND()) {
            ext = ND;
        } else if (isCSV()) {
            ext = CSV;
        }
        return ext;
    }

    public void setOutputFormat(String oType) {
        outputType._isText = false;
        outputType._isJSON = false;
        outputType._isGMT = false;
        outputType._isSVG = false;
        outputType._isCsv = false;
        outputType._isND = false;
        if (oType.equalsIgnoreCase(TEXT)) {
            outputType._isText = true;
        } else if (oType.equalsIgnoreCase(JSON)) {
            outputType._isJSON = true;
        } else if (oType.equalsIgnoreCase(GMT)) {
            outputType._isGMT = true;
            if (mapwidth == null ) {
                mapwidth = 6.0f;
            }
        } else if (oType.equalsIgnoreCase(SVG)) {
            outputType._isSVG = true;
            if (mapwidth == null) {
                mapwidth = 1000.0f;
            }
        } else if (oType.equalsIgnoreCase(CSV)) {
            outputType._isCsv = true;
        } else if (oType.equalsIgnoreCase(ND)) {
            outputType._isND = true;
        } else {
            throw new IllegalArgumentException("output type "+oType+" not recognized.");
        }
    }

    @CommandLine.ArgGroup(heading = "Output Type %n")
    VelPlotOutputType outputType = new VelPlotOutputType();

    /** ps filename for use within gmt script. Usually named after the tool that created the output. */
    public String psFile = null;

    public boolean gmtScript = false;

    @CommandLine.Option(names="--mapwidth", description = "plot width in inches for GMT, pixels for SVG.")
    public Float mapwidth = 6f;


    @CommandLine.Option(names="--mapwidthunit", defaultValue = "i", description = "plot width unit for GMT. Default is i for inchs")
    public String mapWidthUnit = "i";

    public String getPsFile() {
        if (psFile != null) {
            return psFile;
        }
        return getOutFileBase()+".ps";
    }

    public void setPsFile(String psFile) {
        this.psFile = psFile;
    }

    public Float getMapwidth() {
        return mapwidth;
    }

    public void setMapwidth(Float mapwidth) {
        this.mapwidth = mapwidth;
    }

    public String getMapWidthUnit() {
        return mapWidthUnit;
    }

    public void setMapWidthUnit(String mapWidthUnit) {
        this.mapWidthUnit = mapWidthUnit;
    }

    public boolean isText() {
        return outputType._isText;
    }
    public boolean isSVG() {
        return outputType._isSVG;
    }
    public boolean isGMT() {
        return outputType._isGMT;
    }
    public boolean isJSON() {
        return outputType._isJSON;
    }

    public boolean isND() {
        return ((VelPlotOutputType)outputType)._isND;
    }
    public boolean isCSV() {
        return ((VelPlotOutputType)outputType)._isCsv;
    }

    public float getPixelWidth() {
        return (72.0f * mapwidth);
    }

    static class VelPlotOutputType {

        @CommandLine.Option(names = {"--text"}, required = true, description = "outputs as text")
        boolean _isText = false;
        @CommandLine.Option(names = {"--json"}, required = true, description = "outputs as JSON")
        boolean _isJSON = false;
        @CommandLine.Option(names = {"--gmt"}, required = true, description = "outputs as GMT")
        boolean _isGMT = false;
        @CommandLine.Option(names = {"--svg"}, required = true, description = "outputs as SVG")
        boolean _isSVG = false;

        @CommandLine.Option(names = {"--csv"}, required = true, description = "outputs as csv")
        boolean _isCsv = false;
        @CommandLine.Option(names = {"--nameddiscon"}, required = true, description = "outputs as .nd named discontinuity model file")
        boolean _isND = false;
    }

}
