package edu.sc.seis.TauP;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import org.junit.Test;

public class CmdLineOutputTest {

    String[] timeTestCmds = new String[] {"taup_time -h 10 -ph P -deg 35 -mod prem",
                                          "taup_time -h 10 -ph P -deg 35",
                                          "taup_time -h 10 -ph P -deg 35 -mod ak135",
                                          "taup_time -h 10 -ph ttall -deg 35 -mod prem",
                                          "taup_time -h 10 -ph ttall -deg 35",
                                          "taup_time -h 10 -ph ttall -deg 35 -mod ak135"};
    
    String[] pierceTestCmds = new String[] {"taup_pierce -h 10 -ph P -deg 35 -mod prem",
                                            "taup_pierce -h 10 -ph P -deg 35",
                                            "taup_pierce -h 10 -ph P -deg 35 -mod ak135"};
    
    String[] pathTestCmds = new String[] {"taup_path -o stdout -h 10 -ph P -deg 35 -mod prem",
                                          "taup_path -o stdout -h 10 -ph P -deg 35",
                                          "taup_path -o stdout -h 10 -ph P -deg 35 --svg",
                                          "taup_path -o stdout -h 10 -ph P -deg 35 -mod ak135"};
    
    String[] curveTestCmds = new String[] {"taup_curve -o stdout -h 10 -ph P -mod prem",
                                           "taup_curve -o stdout -h 10 -ph P",
                                           "taup_curve -o stdout -h 10 -ph P -mod ak135"};
    

    /** disable unless regenerating the cmd line output test resources. 
     * new text files will be in cmdLineTest in cwd
     *     
     * @throws Exception
     */
    // @Test
    public void testSaveOutput() throws Exception {
        List<String> allList = new ArrayList<String>();
        allList.addAll(Arrays.asList(timeTestCmds));
        allList.addAll(Arrays.asList(pierceTestCmds));
        allList.addAll(Arrays.asList(pathTestCmds));
        allList.addAll(Arrays.asList(curveTestCmds));
        for (String cmd : allList) {
            System.err.println(cmd);
            saveOutputToFile(cmd);
        }
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
       // runTests(new String[] {"taup_table -ph ttall -generic"});
    }

    public void runTests(String[] cmds) throws Exception {
        for (int i = 0; i < cmds.length; i++) {
            testCmd(cmds[i]);
        }
    }
    
    public void runCmd(String cmd) throws Exception {
        String[] s = cmd.split(" +");
        String tool = s[0];
        String[] cmdArgs = new String[s.length - 1];
        System.arraycopy(s, 1, cmdArgs, 0, cmdArgs.length);
        if (tool.equals("taup_time")) {
            TauP_Time.main(cmdArgs);
        } else if (tool.equals("taup_pierce")) {
            TauP_Pierce.main(cmdArgs);
        } else if (tool.equals("taup_path")) {
            TauP_Path.main(cmdArgs);
        } else if (tool.equals("taup_curve")) {
            TauP_Curve.main(cmdArgs);
        } else if (tool.equals("taup_table")) {
            TauP_Table.main(cmdArgs);
        } else { 
            throw new Exception("Unknown tool: "+tool);
        }
    }

    public void testCmd(String cmd) throws Exception {
        setUpStreams();
        assertEquals("sysout is not empty", 0, outContent.toByteArray().length);
        runCmd(cmd);
        BufferedReader prior = getPriorOutput(cmd);
        BufferedReader current = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outContent.toByteArray())));
        int lineNum = 0;
        String priorLine;
        String currentLine;
        assertTrue("Current output is empty.", current.ready());
        while (prior.ready() && current.ready()) {
            priorLine = prior.readLine();
            currentLine = current.readLine();
            origOut.println(currentLine);
            assertEquals(cmd + " line " + lineNum, priorLine, currentLine);
            lineNum++;
        }
        while (prior.ready()) {
            priorLine = prior.readLine();
            if (priorLine.trim().length() != 0) {
                fail("Prior has extra lines: " + priorLine);
            }
        }
        while (current.ready()) {
            currentLine = current.readLine();
            if (currentLine.trim().length() != 0) {
                fail("Current has extra lines: " + currentLine);
            }
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
        assertEquals("line one", "> P at   411.69 seconds at    35.00 degrees for a     10.0 km deep source in the prem model with rayParam    8.604 s/deg.", s.readLine());
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
        assertNotNull("Resource " + resource + " for " + cmd + " not found.", res);
        InputStream inStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("edu/sc/seis/TauP/cmdLineTest/" + fileizeCmd(cmd));
        if (inStream == null) {
            fail("Resource " + fileizeCmd(cmd) + " for " + cmd + " not found.");
        }
        System.err.println("Load "+resource);
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
        return in;
    }

    public String fileizeCmd(String cmd) {
        return cmd.replaceAll(" ", "_");
    }
}
