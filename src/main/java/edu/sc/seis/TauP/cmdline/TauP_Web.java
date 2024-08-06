package edu.sc.seis.TauP.cmdline;

import picocli.CommandLine;

import java.util.concurrent.Callable;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.ABREV_SYNOPSIS;
import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;

@CommandLine.Command(name = "web",
        description = "Web based gui for the TauP Toolkit.",
        optionListHeading = OPTIONS_HEADING,
        abbreviateSynopsis = ABREV_SYNOPSIS,
        usageHelpAutoWidth = true)
public class TauP_Web implements Callable<Integer> {

    /**
     * Indirect startup for TauP webserver. This allows picocli to function for help even if the
     * dependencies (like undertow) are not on the classpath. See TauP_WebServe for the actual
     * implementation.
     *
     * @return 1 if successful, 0 otherwise
     * @throws Exception web startup fails
     */
    @Override
    public Integer call() throws Exception {
        try {
            TauP_WebServe tool = new TauP_WebServe();
            tool.port = port;

            tool.init();
            tool.start();
        } catch (NoClassDefFoundError e) {
            System.err.println("TauP Web does not seem to be installed, a required jar is not on the classpath.");
            System.err.println(e.getMessage());
            System.err.println(e.getCause()!=null?e.getCause().getMessage():"");
            return 1;
        }
        return 0;
    }

    @CommandLine.Option(names = {"-p", "--port"}, defaultValue = "7049", description = "port to use, defaults to ${DEFAULT-VALUE}")
    int port = 7049;

}
