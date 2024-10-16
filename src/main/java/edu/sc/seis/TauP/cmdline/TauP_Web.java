package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.TauModelLoader;
import edu.sc.seis.TauP.VelocityModel;
import edu.sc.seis.TauP.VelocityModelException;
import picocli.CommandLine;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
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
            for (String modName : extraModelNames) {
                VelocityModel vMod = TauModelLoader.loadVelocityModel(modName);
                if (vMod == null) {
                    // were not able to find it
                    throw new VelocityModelException("Unable to load model: "+modName);
                }
            }
            tool.additionalModels.addAll(extraModelNames);

            tool.init();
            tool.start();

            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("http://localhost:"+port));
                }
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
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

    @CommandLine.Option(names = {"--models"},
            arity = "1..*",
            description = "List of additional models to use"
    )
    List<String> extraModelNames = new ArrayList<>();
}
