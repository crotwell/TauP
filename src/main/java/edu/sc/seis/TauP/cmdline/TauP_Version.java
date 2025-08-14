package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.BuildVersion;
import edu.sc.seis.TauP.HTMLUtil;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauPException;
import edu.sc.seis.TauP.cmdline.args.AbstractOutputTypeArgs;
import edu.sc.seis.TauP.cmdline.args.OutputTypes;
import edu.sc.seis.TauP.cmdline.args.TextOutputTypeArgs;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;

@CommandLine.Command(name = "version",
        description = "Print the version.",
        optionListHeading = OPTIONS_HEADING,
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

    public void printResult(PrintWriter out) throws TauPException {
        if (outputTypeArgs.isJSON()) {
            printResultJSON(out);
        } else if (outputTypeArgs.isHTML()) {
            HTMLUtil.createHtmlStart(out, "TauP Version", "", false);
            printResultText(out);
            out.println(HTMLUtil.createHtmlEnding());
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
