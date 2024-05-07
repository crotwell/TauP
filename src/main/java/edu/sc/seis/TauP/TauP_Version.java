package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.AbstractOutputTypeArgs;
import edu.sc.seis.TauP.cli.OutputTypes;
import edu.sc.seis.TauP.cli.TextOutputTypeArgs;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;

@CommandLine.Command(name = "version",
        description = "print version",
        usageHelpAutoWidth = true)
public class TauP_Version extends TauP_Tool {

    public TauP_Version() {
        super(new TextOutputTypeArgs(OutputTypes.TEXT, AbstractOutputTypeArgs.STDOUT_FILENAME));
        outputTypeArgs = (TextOutputTypeArgs)abstractOutputTypeArgs;
    }

    @CommandLine.Mixin
    TextOutputTypeArgs outputTypeArgs;

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOutputFormat();
    }

    @Override
    public String getOutFileExtension() {
        return outputTypeArgs.getOutFileExtension();
    }

    @Override
    public void init() throws TauPException {

    }

    @Override
    public void start() throws IOException, TauPException {
        PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
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

    public void printResultText(PrintWriter out) {
        out.println(BuildVersion.getDetailedVersion());
    }

    public void printResultJSON(PrintWriter out) {
        out.println(BuildVersion.getVersionAsJSON());
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void validateArguments() throws TauModelException {

    }
}
