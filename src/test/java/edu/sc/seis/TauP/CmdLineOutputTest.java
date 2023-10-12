package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.*;


public class CmdLineOutputTest {

    String[] timeTestCmds = new String[] {"taup time -h 10 -ph P -deg 35 -mod prem",
                                          "taup time -h 10 -ph P -deg 35",
                                          "taup time -h 10 -ph P -deg 35 -mod ak135",
                                          "taup time -h 10 -ph ttall -deg 35 -mod prem",
                                          "taup time -h 10 -ph ttall -deg 35",
                                          "taup time -h 10 -ph ttall -deg 35 -mod ak135",
                                          "taup time -h 10 -ph ttall -deg 35 -mod ak135 --json",
                                          "taup time -h 10 -ph ttall -deg 145 -mod ak135 --rel P",
                                          "taup time -h 10 -ph ttall -deg 145 -mod ak135 --rel P,PKP"
                                            };

    String[] pierceTestCmds = new String[] {"taup pierce -h 10 -ph P -deg 35 -mod prem",
                                            "taup pierce -h 10 -ph P -deg 35",
                                            "taup pierce -h 10 -ph P -deg 35 -mod ak135",
                                            "taup pierce -mod prem -h 600 -deg 45 -ph PKiKP -pierce 5049.5",
                                            "taup pierce -h 0 -ph Pn -deg 6",
                                            "taup pierce -h 0 -ph Pdiff -deg 120"
                                          };

    String[] pathTestCmds = new String[] {"taup path -o stdout -h 10 -ph P -deg 35 -mod prem",
                                          "taup path -o stdout -h 10 -ph P -deg 35",
            "taup path -o stdout -h 10 -ph Pdiff -deg 135",
            "taup path -o stdout -h 10 -ph 2kmps -deg 35",
            "taup path -o stdout -h 10 -ph Pn -deg 10",
            "taup path -o stdout -h 10 -ph PnPn -deg 10",
            "taup path -o stdout -h 10 -ph PdiffPdiff -deg 135",
                                          "taup path -o stdout -h 10 -ph P -deg 35 --svg",
                                          "taup path -o stdout -h 10 -ph P -deg 35 -mod ak135"};

    String[] curveTestCmds = new String[] {
            "taup curve -o stdout -h 10 -ph P -mod prem",
            "taup curve -o stdout -h 10 -ph P",
            "taup curve -o stdout -h 10 -ph P -mod ak135"
            // curve labels are random position in --svg, so diff breaks
            // "taup curve -o stdout -h 10 -ph P --svg",
    };

    String[] wavefrontTestCmds = new String[] {
            "taup wavefront -o stdout --mod ak135 --svg -h 100 -ph P,S,PKIKP",
            "taup wavefront -o stdout --mod ak135 --svg -h 10 -ph P,S,PedOP --scatter 200 -5"
    };

    String[] velplotTestCmds = new String[] {
            "taup velplot -o stdout --mod ak135 --svg",
            "taup slowplot -o stdout --mod ak135 --svg"
    };
    String[] reflTransPlotTestCmds = new String[] {
            "taup relftrans -o stdout --mod fgms --depth 35 --svg"
    };


    String[] helpTestCmds = new String[] {"taup --help",
                                          "taup time --help",
                                          "taup pierce --help",
                                          "taup path --help",
                                          "taup phase --help",
                                          "taup curve --help",
                                          "taup wavefront --help",
                                          "taup table --help",
                                          "taup velmerge --help",
                                          "taup velplot --help",
                                          "taup slowplot --help",
                                          "taup create --help"};

    String versionCmd = "taup --version";

    /** 
     * regenerating the cmd line output test resources.
     * new text files will be in cmdLineTest in cwd
     *
     * @throws Exception
     */
    public void regenSavedOutput() throws Exception {
        List<String> allList = new ArrayList<String>();
        allList.addAll(Arrays.asList(helpTestCmds));
        allList.addAll(Arrays.asList(timeTestCmds));
        allList.addAll(Arrays.asList(pierceTestCmds));
        allList.addAll(Arrays.asList(pathTestCmds));
        allList.addAll(Arrays.asList(curveTestCmds));
        allList.addAll(Arrays.asList(wavefrontTestCmds));
        allList.addAll(Arrays.asList(velplotTestCmds));
        allList.addAll(Arrays.asList(reflTransPlotTestCmds));
        for (String cmd : allList) {
            System.err.println(cmd);
            saveOutputToFile(cmd);
        }
    }

    /**
     * reproduce figures from books for comparison
     * new text files will be in cmdLineTest/refltranCompare in cwd
     *
     * @throws Exception
     */
    public void regenReflTranCompareFigures() throws Exception {

        // not actual tests, but allow visual compare with figures in
        // Foundations of Modern Global Seismology
        HashMap<String, String> fmgsFigureTestCmds = new HashMap<>();
        HashMap<String, String> figureTitles = new HashMap<>();
        HashMap<String, String> figureCompare = new HashMap<>();
        fmgsFigureTestCmds.put("FMGS_fig_13_12a.svg",
                "taup refltrans -o stdout --abs --pwave --inlayer 8.0 4.6 3.3 --trlayer 6.4 3.7 2.8 --linrayparam --svg");
        figureTitles.put("FMGS_fig_13_12a.svg", "FMGS, fig 13.12a, solid-solid P");
        figureCompare.put("FMGS_fig_13_12a.svg", "FMGS_fig_13_12a.png");
        fmgsFigureTestCmds.put("FMGS_fig_13_12b.svg",
                "taup refltrans -o stdout --abs --swave --inlayer 8.0 4.6 3.3 --trlayer 6.4 3.7 2.8 --linrayparam --svg");
        figureTitles.put("FMGS_fig_13_12b.svg", "FMGS, fig 13.12b, solid-solid S");
        figureCompare.put("FMGS_fig_13_12b.svg", "FMGS_fig_13_12b.png");
        fmgsFigureTestCmds.put("FMGS_fig_13_13a.svg",
                "taup refltrans -o stdout --abs --pwave --inlayer 6.4 3.7 2.8 --trlayer 8.0 4.6 3.3 --linrayparam --svg");
        figureTitles.put("FMGS_fig_13_13a.svg", "FMGS, fig 13.13a, solid-solid P");
        figureCompare.put("FMGS_fig_13_13a.svg", "FMGS_fig_13_13a.png");
        fmgsFigureTestCmds.put("FMGS_fig_13_13b.svg",
                "taup refltrans -o stdout --abs --swave --inlayer 6.4 3.7 2.8 --trlayer 8.0 4.6 3.3 --linrayparam --svg");
        figureTitles.put("FMGS_fig_13_13b.svg", "FMGS, fig 13.13b, solid-solid S");
        figureCompare.put("FMGS_fig_13_13b.svg", "FMGS_fig_13_13b.png");
        fmgsFigureTestCmds.put("FMGS_fig_13_14a.svg",
                "taup refltrans -o stdout --abs --pwave --inlayer 5.8 3.35 2.5 --trlayer 0.3 0 1.2 --linrayparam --svg");
        figureTitles.put("FMGS_fig_13_14a.svg", "FMGS, fig 13.14a, solid-air P");
        figureCompare.put("FMGS_fig_13_14a.svg", "FMGS_fig_13_14a.png");
        fmgsFigureTestCmds.put("FMGS_fig_13_14b.svg",
                "taup refltrans -o stdout --abs --swave --inlayer 5.8 3.35 2.5 --trlayer 0.3 0 1.2 --linrayparam --svg");
        figureTitles.put("FMGS_fig_13_14b.svg", "FMGS, fig 13.14b, solid-air P");
        fmgsFigureTestCmds.put("FMGS_fig_13_15.svg",
                "taup refltrans -o stdout --abs --pwave  --swave --inlayer 5.8 3.35 2.5 --linrayparam --svg");
        figureTitles.put("FMGS_fig_13_15.svg", "FMGS, fig 13.15, free surface");
        figureCompare.put("FMGS_fig_13_15.svg", "FMGS_fig_13_15.png");
        fmgsFigureTestCmds.put("AR_fig_5_06.svg",
                "taup refltrans -o stdout --pwave --swave --inlayer 5 3 2.5 --linrayparam --svg");
        figureTitles.put("AR_fig_5_06.svg", "Aki and Richards, fig 5.6");
        figureCompare.put("AR_fig_5_06.svg", "AR_fig_5_6.pdf");
        fmgsFigureTestCmds.put("AR_fig_5_10.svg",
                "taup refltrans -o stdout --abs --swave --inlayer 5 3 2.5 --linrayparam --svg");
        figureTitles.put("AR_fig_5_10.svg", "Aki and Richards, fig 5.10");
        figureCompare.put("AR_fig_5_10.svg", "AR_fig_5_10.pdf");
        fmgsFigureTestCmds.put("Shearer_fig_6_5.svg",
                "taup refltrans -o stdout --abs --shwave --mod prem --depth 24.4 --down --svg");
        figureTitles.put("Shearer_fig_6_5.svg", "Shearer, fig 6.5");
        figureCompare.put("Shearer_fig_6_5.svg", "Shearer_fig_6_5.pdf");

        File topdir = new File("cmdLineTest");
        if ( ! topdir.isDirectory()) {topdir.mkdir(); }
        File dir = new File(topdir, "refltranCompare");
        if ( ! dir.isDirectory()) {dir.mkdir(); }

        PrintStream indexOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(dir, "index.html"))));
        indexOut.println("<html>");
        indexOut.println("<body>");
        indexOut.println("<h3>Comparison of Reflection and Transmission Coefficients</h3>");
        indexOut.println("<table>");
        indexOut.println("<tr><th>TauP SVG</th><th>Command Line</th><th>Reference Image</th></tr>");
        List<String> sortedKeys = new ArrayList<String>();
        sortedKeys.addAll(fmgsFigureTestCmds.keySet());
        Collections.sort(sortedKeys);
        for (String key : sortedKeys) {
            String cmd = fmgsFigureTestCmds.get(key);
            System.err.println(cmd);
            saveOutputToFile( cmd, dir, key);
            String compare = figureCompare.containsKey(key) ? figureCompare.get(key) : "";
            indexOut.println("<tr><td><a href=\""+key+"\">"+figureTitles.get(key)+"</a></td><td><code>"
                    +cmd.replace("-o stdout", "")+"</code></td><td><a target=\"_blank\" href=\""+compare+"\">compare</a></td></tr>");
        }
        indexOut.println("</table>");
        indexOut.println("</body>");
        indexOut.println("</html>");
        indexOut.close();
    }
    /** 
     * regenerating the cmd line output test resources.
     * new text files will be in cmdLineTest in cwd.
     * This one just does a single test for when adding new output test.
     *
     * @throws Exception
     */
    public void regenSavedOutputSingle() throws Exception {
        saveOutputToFile(helpTestCmds[0]);
    }
    
    /**
     * version test needs to be separate because act of committing an update to the version cmd line output
     * changes the git version making the test fail.
     * 
     * @throws Exception
     */
    @Test
    public void testVersion() throws Exception {
        setUpStreams();
        assertEquals( 0, outContent.toByteArray().length, "sysout is not empty");
        runCmd(versionCmd);
        BufferedReader current = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outContent.toByteArray())));
        int lineNum = 0;
        String currentLine;
        assertTrue(current.ready());
        currentLine = current.readLine();
        BuildVersion ver;
        // should only be one line
        assertFalse(current.ready());
        assertTrue(currentLine.contains(BuildVersion.getGroup()));
        assertTrue(currentLine.contains(BuildVersion.getName()));
        assertTrue(currentLine.contains(BuildVersion.getVersion()));
        cleanUpStreams();
    }

    @Test
    public void testTauPHelp() throws Exception {
        runTests(helpTestCmds);
    }

    @Test
    public void testTauPTime() throws Exception {
        runTests(timeTestCmds);
    }

    @Test
    public void testTauPPierce() throws Exception {
        runTests(pierceTestCmds);
    }

    @Test
    public void testTauPPath() throws Exception {
        runTests(pathTestCmds);
    }

    @Test
    public void testTauPCurve() throws Exception {
        runTests(curveTestCmds);
    }

    @Test
    public void testTauPWavefront() throws Exception {
        runTests(wavefrontTestCmds);
    }

    @Test
    public void testTauPVelplot() throws Exception {
        runTests(velplotTestCmds);
    }

    @Test
    public void testTauPReflTransplot() throws Exception {
        runTests(reflTransPlotTestCmds);
    }

    @Test
    public void testTauPTable() throws Exception {
        // this one takes a lot of memory
       // runTests(new String[] {"taup table -ph ttall -generic"});
    }

    public void runTests(String[] cmds) throws Exception {
        for (int i = 0; i < cmds.length; i++) {
            testCmd(cmds[i]);
        }
    }

    public void runCmd(String cmd) throws Exception {
        String[] s = cmd.split(" +");
        String tool = s[0];
        if ( ! tool.equalsIgnoreCase("taup")) {
            throw new Exception("Unknown first word of command: "+tool+", should be taup");
        }
        String[] cmdArgs = new String[s.length - 1];
        System.arraycopy(s, 1, cmdArgs, 0, cmdArgs.length);
        System.err.println(cmd);
        ToolRun.main(cmdArgs);
    }

    public void testCmd(String cmd) throws Exception {
        setUpStreams();
        assertEquals( 0, outContent.toByteArray().length, "sysout is not empty");
        runCmd(cmd);
        BufferedReader prior = getPriorOutput(cmd);
        BufferedReader current = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outContent.toByteArray())));
        int lineNum = 1;
        String priorLine;
        String currentLine;
        assertTrue( current.ready() , "Current output is empty for "+cmd);
        while (prior.ready() && current.ready()) {
            priorLine = prior.readLine();
            currentLine = current.readLine();
            origOut.println(currentLine);
            assertEquals(priorLine, currentLine, cmd + " line " + lineNum);
            lineNum++;
        }
        while (prior.ready()) {
            priorLine = prior.readLine();
            assertEquals(0, priorLine.trim().length(), "Prior has extra lines: " + priorLine);
        }
        while (current.ready()) {
            currentLine = current.readLine();
            assertEquals(0, currentLine.trim().length(), "Current has extra lines: " + currentLine);
        }
        cleanUpStreams();
        origErr.println("Done with " + cmd);
    }

    /**
     * test loading prior results text file from test resources. Kind of a meta-test... :)
     * @throws Exception
     */
    @Test
    public void loadTest() throws Exception {
        BufferedReader s = getPriorOutput("taup_path -o stdout -h 10 -ph P -deg 35 -mod prem");
        String priorS = s.readLine();
        String shouldBeS = "> P at   411.69 seconds at    35.00 degrees for a     10.0 km deep source in the prem model with rayParam    8.604 s/deg.";
        assertEquals(shouldBeS.length(), priorS.length(), "line one length" );
        assertEquals("> P at   411.69 seconds at    35.00 degrees for a     10.0 km deep source in the prem model with rayParam    8.604 s/deg.", priorS, "line one");
    }


    public void saveOutputToFile(String cmd) throws Exception {
        File dir = new File("cmdLineTest");
        String filename = fileizeCmd(cmd);
        saveOutputToFile( cmd, dir, filename);
    }

    public void saveOutputToFile(String cmd, File dir, String filename) throws Exception {
        if ( ! dir.isDirectory()) {dir.mkdir(); }
        PrintStream fileOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(dir, filename))));
        System.setOut(fileOut);
        runCmd(cmd);
        fileOut.flush();
        System.setOut(origOut);
        fileOut.close();
    }

    private ByteArrayOutputStream outContent;

    private ByteArrayOutputStream errContent;

    PrintStream origOut = System.out;

    PrintStream origErr = System.err;

    public void setUpStreams() {
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        // System.setErr(new PrintStream(errContent));
    }

    public void cleanUpStreams() {
        System.setOut(origOut);
        // System.setErr(origErr);
        outContent.reset();
        errContent.reset();
    }

    public BufferedReader getPriorOutput(String cmd) throws IOException {
        String resource = "edu/sc/seis/TauP/cmdLineTest/" + fileizeCmd(cmd);
        URL res = this.getClass().getClassLoader().getResource(resource);
        assertNotNull( res, "Resource " + resource + " for " + cmd + " not found.");
        InputStream inStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("edu/sc/seis/TauP/cmdLineTest/" + fileizeCmd(cmd));
        assertNotNull(inStream, "Resource " + fileizeCmd(cmd) + " for " + cmd + " not found.");
        System.err.println("Load "+resource);
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
        return in;
    }

    public String fileizeCmd(String cmd) {
        cmd = cmd.replaceAll(",", "_");
        return cmd.replaceAll(" ", "_");
    }
    
    public static void main(String[] args) throws Exception {
        CmdLineOutputTest me = new CmdLineOutputTest();
        me.regenSavedOutput();
        me.regenReflTranCompareFigures();
    }
}
