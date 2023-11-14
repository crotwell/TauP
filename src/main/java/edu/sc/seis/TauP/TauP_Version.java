package edu.sc.seis.TauP;

import java.io.IOException;
import java.io.PrintWriter;

public class TauP_Version extends TauP_Tool {

    @Override
    public String[] allowedOutputFormats() {
        return new String[] {TEXT, JSON};
    }

    @Override
    public void setDefaultOutputFormat() {
        super.setOutputFormat(TEXT);
    }

    @Override
    protected String[] parseCmdLineArgs(String[] origArgs) throws IOException {
        return super.parseCommonCmdLineArgs(origArgs);
    }

    @Override
    public void init() throws TauPException {

    }

    @Override
    public void start() throws IOException, TauModelException, TauPException {
        printResult(getWriter());
    }

    public void printResult(PrintWriter out) throws IOException {
        if (outputFormat.equals(TauP_Tool.JSON)) {
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
                + TauP_Tool.getStdUsageTail();
    }

    @Override
    public void validateArguments() throws TauModelException {

    }
}
