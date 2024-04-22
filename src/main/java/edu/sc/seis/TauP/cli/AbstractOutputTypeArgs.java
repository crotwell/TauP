package edu.sc.seis.TauP.cli;

import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static edu.sc.seis.TauP.cli.OutputTypes.JSON;
import static edu.sc.seis.TauP.cli.OutputTypes.TEXT;

public abstract class AbstractOutputTypeArgs {

    public AbstractOutputTypeArgs(String filebase) {
        setOutFileBase(filebase);
    }

    String outFileBase = "taup";

    @CommandLine.Option(names = {"-o", "--output"}, description = "output to file, default is stdout.")
    public void setOutFile(String outfile) {
        setOutFileBase(outfile);
    }

    public abstract void setOutputType(String oType);

    public abstract String getOuputFormat();

    public String getOutFileBase() {
        return outFileBase;
    }

    public void setOutFileBase(String outFileBase) {
        this.outFileBase = outFileBase;
    }

    public abstract String getOutFileExtension();

    public void setOutFileExtension(String ext) {
        extension = ext;
    }

    protected String extension = null;

    public static final String STDOUT_FILENAME = "-";

    public boolean isStdout() {
        if(getOutFileBase() == null || getOutFileBase().length() == 0
                || getOutFileBase().equals(STDOUT_FILENAME)|| getOutFileBase().equals("stdout")) {
            return true;
        }
        return false;
    }

    public String getOutFile() {
        if (isStdout()) {
            return "stdout";
        } else {
            if (getOutFileExtension() == null || getOutFileExtension().length() == 0 || getOutFileBase().endsWith("." + getOutFileExtension())) {
                // don't do a dot if no extension or already there
                return getOutFileBase();
            }
            return getOutFileBase() + "." + getOutFileExtension();
        }
    }

    public PrintWriter createWriter(PrintWriter stdout) throws IOException {
        if (isStdout()) {
            return stdout;
        } else {
            return new PrintWriter(new BufferedWriter(new FileWriter(getOutFile())));
        }
    }
}
