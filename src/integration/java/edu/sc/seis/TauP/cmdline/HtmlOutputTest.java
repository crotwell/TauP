package edu.sc.seis.TauP.cmdline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.sc.seis.TauP.TauPException;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class HtmlOutputTest {


    public HtmlOutputTest() throws TauPException {
        String[] testVelModels = new String[] { "highSlownessDiscon.nd" };
        CmdLineOutputTest.loadTestVelocityModels(testVelModels);
    }
    
    public static String[] htmlTestCmds = new String[] {
            "taup curve -o stdout -h 10 -p P,2kmps --mod prem --html",
            "taup time -h 10 -p P --deg 35 --html",
            "taup time --mod ak135 -h 10 -p P,S,PedOP --scatter 200 -5 --deg 40 --html",
            "taup time --mod ak135 -h 10 -p P,S,PedoP --scatter 200 5 --deg 40, --html",
            "taup time -h 10 -p ttall --deg 35 --mod ak135 --html",
            "taup pierce -o stdout -h 10 -p P,pP,S,ScS --deg 15 --html",
            "taup pierce --mod ak135 -h 10 -p P,S,PedOP --scatter 200 -5 --deg 40 --html",
            "taup path -o stdout -h 10 -p P,pP,S,ScS --deg 15 --html",
            "taup phase -p Pv410p,PV410p --html",
            "taup phase -p S --html --mod highSlownessDiscon.nd",
            "taup distaz -o stdout --sta 35 -82 --sta 33 -81 --evt 22 -101 --html",
            "taup velplot -o stdout --mod ak135 --html",
            "taup wavefront -o stdout --mod ak135 -h 100 -p P,S,PKIKP --timestep 500 --html",
            "taup discon -o stdout --mod ak135 --html",
            "taup find -o stdout --mod ak135fcont --sourcedepth 100 --max 3 --pwaveonly --exclude 20,moho,iocb --html",
            "taup refltrans -o stdout --mod ak135 --depth 35 --html",

    };

    @Test
    public void testTauPHtml() throws Exception {
        runHtmlTests(Arrays.asList(htmlTestCmds));
    }

    public void runHtmlTests(List<String> cmdList) throws Exception {
        for (String cmd : cmdList) {
            String outContent = CmdLineOutputTest.runCmd(cmd);
            assertNotNull(outContent);
            assertNotEquals(0, outContent.length());
        }
    }
}
