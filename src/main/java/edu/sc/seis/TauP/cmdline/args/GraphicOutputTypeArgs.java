package edu.sc.seis.TauP.cmdline.args;

import picocli.CommandLine;

import java.io.File;

public class GraphicOutputTypeArgs extends AbstractOutputTypeArgs {

    public GraphicOutputTypeArgs(String defaultFormat, String filebase) {
        super(filebase);
        setOutputFormat(defaultFormat);
    }

    @CommandLine.ArgGroup(heading = OUTPUTTYPE_HEADING)
    GraphicsOutputType outputType = new GraphicsOutputType();

    /** ps filename for use within gmt script. Usually named after the tool that created the output. */
    public String psFile = null;

    @CommandLine.Option(names="--mapwidth", description = "plot width in units from --mapwidthunit.")
    public Float mapwidth = 6f;


    @CommandLine.Option(names="--mapwidthunit",
            defaultValue = "i",
            description = "plot width unit, i for inch, c for cm or p for px.")
    public String mapWidthUnit = "i";

    public String getGmtOutFileBase(String toolName) {
        if (psFile != null) {
            return psFile;
        }
        String base = getOutFileBase();
        if (base.equals(STDOUT_FILENAME) || base.equals("stdout")) {
            base = toolName;
        } else if (base.contains(java.io.File.separator)) {
            File tosplit = new File(base);
            base = tosplit.getName();
        }
        if (base.endsWith(".gmt")) {
            base = base.substring(0, base.length()-4);
        }
        return base;
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

    public String mapWidthGMT() {
        return mapwidth+mapWidthUnit;
    }

    public boolean isText() {
        return outputType._isText;
    }
    public boolean isSVG() {
        return outputType._isSVG;
    }
    public boolean isHTML() {
        return outputType._isHTML;
    }
    public boolean isGMT() {
        return outputType._isGMT;
    }
    public boolean isJSON() {
        return outputType._isJSON;
    }

    public void setOutputFormat(String oType) {
        outputType._isText = false;
        outputType._isJSON = false;
        outputType._isGMT = false;
        outputType._isSVG = false;
        outputType._isHTML = false;
        if (oType.equalsIgnoreCase(OutputTypes.TEXT)) {
            outputType._isText = true;
        } else if (oType.equalsIgnoreCase(OutputTypes.JSON)) {
            outputType._isJSON = true;
        } else if (oType.equalsIgnoreCase(OutputTypes.GMT)) {
            outputType._isGMT = true;
            if (mapwidth == null ) {
                mapwidth = 6.0f;
            }
        } else if (oType.equalsIgnoreCase(OutputTypes.SVG) || oType.equalsIgnoreCase(OutputTypes.HTML)) {
            if (oType.equalsIgnoreCase(OutputTypes.SVG)) {
                outputType._isSVG = true;
            } else {
                outputType._isHTML = true;
            }
            if (mapwidth == null ) {
                mapwidth = 1000.0f;
            }
        } else {
            throw new IllegalArgumentException("output type "+oType+" not recognized.");
        }
    }

    public String getOutputFormat() {
        if (isGMT()) return OutputTypes.GMT;
        if (isSVG()) return OutputTypes.SVG;
        if (isHTML()) return OutputTypes.HTML;
        if (isJSON()) return OutputTypes.JSON;
        return OutputTypes.TEXT;
    }

    public String getOutFileExtension() {
        String ext;
        if (extension != null) {
            return extension;
        } else {
            ext = OutputTypes.TEXT;
        }
        if (isSVG()) {
            ext = OutputTypes.SVG;
        } else if (isHTML()) {
            ext = OutputTypes.HTML;
        } else if (isJSON()) {
            ext = OutputTypes.JSON;
        } else if (isGMT()) {
            ext = OutputTypes.GMT;
        }
        return ext;
    }

    public float getPixelWidth() {
        return getPixelWidth(mapwidth, mapWidthUnit);
    }

    public static float getPixelWidth(float mapwidth, String mapWidthUnit) {
        if (mapWidthUnit.equals("i")) {
            // inch
            return (96.0f * mapwidth);
        } else if (mapWidthUnit.equals("c")) {
            // cm = 1/2.54 * in
            return (96.0f/2.54f * mapwidth);
        } else if (mapWidthUnit.equals("p") || mapwidth > 100) {
            // no unit, assume in pixels if large
            return mapwidth;
        }
        // default 1000?
        return 1000;
    }

    static class GraphicsOutputType {

        @CommandLine.Option(names = {"--text"}, required = true, description = "outputs as Text")
        boolean _isText = false;
        @CommandLine.Option(names = {"--json"}, required = true, description = "outputs as JSON")
        boolean _isJSON = false;
        @CommandLine.Option(names = {"--gmt"}, required = true, description = "outputs as GMT")
        boolean _isGMT = false;
        @CommandLine.Option(names = {"--svg"}, required = true, description = "outputs as SVG")
        boolean _isSVG = false;
        @CommandLine.Option(names = {"--html"}, required = true, description = "outputs as SVG in HTML")
        boolean _isHTML = false;
    }
}
