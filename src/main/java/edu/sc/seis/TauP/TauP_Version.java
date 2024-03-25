package edu.sc.seis.TauP;

import edu.sc.seis.TauP.CLI.OutputTypes;

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

    @Override
    protected String[] parseCmdLineArgs(String[] origArgs) throws IOException {
        String[] args = super.parseCommonCmdLineArgs(origArgs);
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        int i = 0;
        while(i < args.length) {
            if(dashEquals("json", args[i])) {
                outputFormat = OutputTypes.JSON;
            } else {
                /* I don't know how to interpret this argument, so pass it back */
                noComprendoArgs[numNoComprendoArgs++] = args[i];
            }
            i++;
        }
        if(numNoComprendoArgs > 0) {
            String[] temp = new String[numNoComprendoArgs];
            System.arraycopy(noComprendoArgs, 0, temp, 0, numNoComprendoArgs);
            return temp;
        } else {
            return new String[0];
        }
    }

    @Override
    public void init() throws TauPException {

    }

    @Override
    public void start() throws IOException, TauModelException, TauPException {
        printResult(getWriter());
    }

    public void printResult(PrintWriter out) throws IOException {
        if (outputFormat.equals(OutputTypes.JSON)) {
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
    public String getUsage() {
        return TauP_Tool.getStdUsageHead(this.getClass())
                + "--json             -- output version as json\n\n"
                + TauP_Tool.getStdUsageTail();
    }

    @Override
    public void validateArguments() throws TauModelException {

    }
}
