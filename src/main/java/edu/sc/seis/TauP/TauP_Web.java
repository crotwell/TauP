package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.OutputTypes;
import picocli.CommandLine;

import java.lang.reflect.Constructor;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "web", description = "web based gui for the TauP Toolkit")
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
                tool.init();
                tool.start();
            }
        } catch (ClassNotFoundException e) {
            System.err.println("TauP Web does not seem to be installed, the required jar is not on the classpath.");
            return 1;
        }
        return 0;
    }

}
