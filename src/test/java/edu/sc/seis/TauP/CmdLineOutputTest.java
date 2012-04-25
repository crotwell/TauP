package edu.sc.seis.TauP;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;

import org.junit.Test;

public class CmdLineOutputTest {

    @Test
    public void testTauPTime() throws Exception {
        runTests(new String[] {"taup_time -h 10 -ph P -deg 35 -mod prem",
                               "taup_time -h 10 -ph P -deg 35",
                               "taup_time -h 10 -ph P -deg 35 -mod ak135"});
    }

    @Test
    public void testTauPPierce() throws Exception {
        runTests(new String[] {"taup_pierce -h 10 -ph P -deg 35 -mod prem",
                               "taup_pierce -h 10 -ph P -deg 35",
                               "taup_pierce -h 10 -ph P -deg 35 -mod ak135"});
    }

    @Test
    public void testTauPPath() throws Exception {
        runTests(new String[] {"taup_path -o stdout -h 10 -ph P -deg 35 -mod prem",
                               "taup_path -o stdout -h 10 -ph P -deg 35",
                               "taup_path -o stdout -h 10 -ph P -deg 35 -mod ak135"});
    }

    public void runTests(String[] cmds) throws Exception {
        for (int i = 0; i < cmds.length; i++) {
            testCmd(cmds[i]);
        }
    }

    public void testCmd(String cmd) throws Exception {
        String[] s = cmd.split(" +");
        String tool = s[0];
        String[] cmdArgs = new String[s.length - 1];
        System.arraycopy(s, 1, cmdArgs, 0, cmdArgs.length);
        setUpStreams();
        assertEquals("sysout is not empty", 0, outContent.toByteArray().length);
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
    
    @Test
    public void loadTest() throws Exception {
        BufferedReader s = getPriorOutput("taup_path -o stdout -h 10 -ph P -deg 35 -mod prem");
        assertEquals("line one", "> P at   411.68 seconds at    35.00 degrees for a     10.0 km deep source in the prem model with rayParam    8.603 s/deg.", s.readLine());
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
