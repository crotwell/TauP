package edu.sc.seis.TauP;

import edu.sc.seis.TauP.CLI.OutputTypes;
import edu.sc.seis.TauP.CLI.TextOutputTypeArgs;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;

public class TauP_Version extends TauP_Tool {

    @Override
    public String[] allowedOutputFormats() {
        return new String[] {OutputTypes.TEXT, OutputTypes.JSON};
    }

    @Override
    public void setDefaultOutputFormat() {
        super.setOutputFormat(OutputTypes.TEXT);
    }

    @CommandLine.Mixin
    TextOutputTypeArgs outputTypeArgs = new TextOutputTypeArgs();

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOuputFormat();
    }

    @Override
    public void init() throws TauPException {

    }

    @Override
    public void start() throws IOException, TauModelException, TauPException {
        printResult(getWriter());
    }

    public void printResult(PrintWriter out) throws IOException {
        if (outputTypeArgs.isJSON()) {
            printResultJSON(out);
        } else {
            printResultText(out);
        }
        out.flush();
    }

    public void printResultText(PrintWriter out) throws IOException {
        out.println(BuildVersion.getDetailedVersion());
    }

    public void printResultJSON(PrintWriter out) throws IOException {
        out.println(BuildVersion.getVersionAsJSON());
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void validateArguments() throws TauModelException {

    }
}
