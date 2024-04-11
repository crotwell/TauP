package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.OutputTypes;
import edu.sc.seis.TauP.cli.TextOutputTypeArgs;
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
    public String getOutFileExtension() {
        return outputTypeArgs.getOutFileExtension();
    }

    @Override
    public void init() throws TauPException {

    }

    @Override
    public void start() throws IOException, TauModelException, TauPException {
        PrintWriter writer = outputTypeArgs.createWriter();
        printResult(writer);
        writer.close();
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
