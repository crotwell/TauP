package edu.sc.seis.TauP.CLI;

import edu.sc.seis.TauP.ToolRun;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class CommonArgs {


    @ArgGroup(validate = false, heading = "Verbosity Args %n")
    CommonArgsInner commonArgs = new CommonArgsInner();

    static class CommonArgsInner {
        /** Turns on debugging output. */
        @Option(names="--debug", description="enable debugging output")
        public boolean DEBUG = ToolRun.DEBUG;

        /** Turns on verbose output. */
        @Option(names="--verbose", description="enable verbose output")
        public boolean verbose = ToolRun.VERBOSE;

        @Option(names="--help", usageHelp = true, description="print this out, but you already know that!")
        public boolean helpRequest = false;

        @Option(names="--version", versionHelp = true, description="print the version")
        public boolean versionRequest = false;
    }
    
    public boolean isDebug() {
        return commonArgs.DEBUG;
    }
    
    public boolean isVerbose() {
        return commonArgs.verbose;
    }
    
    public boolean isHelpRequest() {
        return commonArgs.helpRequest;
    }
    
    public boolean isVersionRequest() {
        return commonArgs.versionRequest;
    }
    
}
