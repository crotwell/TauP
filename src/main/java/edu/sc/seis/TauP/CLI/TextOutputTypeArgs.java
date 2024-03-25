package edu.sc.seis.TauP.CLI;

import picocli.CommandLine;

import static edu.sc.seis.TauP.CLI.OutputTypes.JSON;
import static edu.sc.seis.TauP.CLI.OutputTypes.TEXT;

public class TextOutputTypeArgs {

    @CommandLine.ArgGroup(exclusive=true, multiplicity="1")
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

    static class TextOutputType {
        @CommandLine.Option(names = {"-text"}, required = true, description = "outputs as text")
        boolean _isText;
        @CommandLine.Option(names = {"-json"}, required = true, description = "outputs as JSON")
        boolean _isJSON;
    }

}
