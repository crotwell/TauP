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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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
                                          "taup path -o stdout -h 10 -ph P -deg 35 --svg",
                                          "taup path -o stdout -h 10 -ph P -deg 35 -mod ak135"};

    String[] curveTestCmds = new String[] {"taup curve -o stdout -h 10 -ph P -mod prem",
                                           "taup curve -o stdout -h 10 -ph P",
                                           "taup curve -o stdout -h 10 -ph P -mod ak135"};

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
        for (String cmd : allList) {
            System.err.println(cmd);
            saveOutputToFile(cmd);
        }
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
        int lineNum = 0;
        String priorLine;
        String currentLine;
        assertTrue( current.ready() , "Current output is empty.");
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
        if ( ! dir.isDirectory()) {dir.mkdir(); }
        String filename = fileizeCmd(cmd);
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
        return cmd.replaceAll(" ", "_");
    }
    
    public static void main(String[] args) throws Exception {
        CmdLineOutputTest me = new CmdLineOutputTest();
        me.regenSavedOutput();
    }
}
