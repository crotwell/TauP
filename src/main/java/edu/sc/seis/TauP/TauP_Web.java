package edu.sc.seis.TauP;

import picocli.CommandLine;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;

import static edu.sc.seis.TauP.TauP_Tool.ABREV_SYNOPSIS;
import static edu.sc.seis.TauP.TauP_Tool.OPTIONS_HEADING;

@CommandLine.Command(name = "web",
        description = "Web based gui for the TauP Toolkit.",
        optionListHeading = OPTIONS_HEADING,
        abbreviateSynopsis = ABREV_SYNOPSIS,
        usageHelpAutoWidth = true)
public class TauP_Web implements Callable<Integer> {

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public Integer call() throws Exception {
        try {
            Class webClass = Class.forName("edu.sc.seis.webtaup.TauP_Web");
            Constructor con = webClass.getConstructor();
            if (con != null) {
                TauP_Tool tool = (TauP_Tool)con.newInstance();

                Field portField = webClass.getDeclaredField("port");
                portField.setInt(tool, port);
                tool.init();
                tool.start();
            }
        } catch (ClassNotFoundException e) {
            System.err.println("TauP Web does not seem to be installed, the required jar is not on the classpath.");
            return 1;
        }
        return 0;
    }

    @CommandLine.Option(names = {"-p", "--port"}, defaultValue = "7049", description = "port to use, defaults to ${DEFAULT-VALUE}")
    int port = 7049;

}
