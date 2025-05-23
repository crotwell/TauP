package edu.sc.seis.TauP.cmdline.args;

import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class AbstractOutputTypeArgs {

    public AbstractOutputTypeArgs(String filebase) {
        setOutFileBase(filebase);
    }

    String outFileBase = "taup";

    @CommandLine.Option(names = {"-o", "--output"}, description = "output to file, default is stdout.")
    public void setOutFile(String outfile) {
        setOutFileBase(outfile);
    }

    public abstract void setOutputFormat(String oType);

    public abstract String getOutputFormat();

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

    public static final String OUTPUTTYPE_HEADING = "Output Type:%n";

    public boolean isStdout() {
        return getOutFileBase() == null || getOutFileBase().isEmpty()
                || getOutFileBase().equals(STDOUT_FILENAME) || getOutFileBase().equals("stdout");
    }

    public String getOutFile() {
        if (isStdout()) {
            return "stdout";
        } else {
            if (getOutFileExtension() == null || getOutFileExtension().isEmpty() || getOutFileBase().endsWith("." + getOutFileExtension())) {
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
