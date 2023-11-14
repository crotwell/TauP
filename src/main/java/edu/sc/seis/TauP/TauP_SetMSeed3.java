package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.SeisFileException;
import edu.sc.seis.seisFile.mseed3.*;

import java.io.*;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TauP_SetMSeed3 extends TauP_Time {

    public TauP_SetMSeed3() {
    }

    public TauP_SetMSeed3(TauModel tMod) {
        super(tMod);
    }

    public TauP_SetMSeed3(String modelName) throws TauModelException {
        super(modelName);
    }

    @Override
    public void start() throws IOException, TauPException {
        if (mseed3FileNames.size() == 0) {
            printUsage();
            return;
        }
        for (String filename : mseed3FileNames) {
            try {
                if (verbose) {
                    System.out.println(filename);
                }
                processMSeed3File(new File(filename));
            } catch(SeisFileException e) {
                throw new TauPException(e);
            }
        }
    }

    public void processMSeed3File(File msd3File) throws IOException, SeisFileException, TauPException {
        int fileBytes = (int) msd3File.length();
        int bytesRead = 0;
        MSeed3Record dr3;
        int drNum = 0;
        File tmpFile = File.createTempFile("taup", "msd3", msd3File.getParentFile());
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(msd3File)));
        while (bytesRead < fileBytes && (dr3 = MSeed3Record.read(dis)) != null) {
            JSONObject eh = dr3.getExtraHeaders();
            Double staLat = null;
            Double staLon = null;
            Double staDepth = 0.0;
            Double evLat = null;
            Double evLon = null;
            Double evDepth = 0.0; // default to zero depth
            if (eh.has("st")) {
                JSONObject st = eh.getJSONObject("st");
                if (st.has("la")) {
                    staLat = st.getDouble("la");
                }
                if (st.has("lo")) {
                    staLat = st.getDouble("lo");
                }
                if (st.has("dp")) {
                    // station depth is in meters
                    staDepth = st.getDouble("dp")/1000;
                }
            }
            if (eh.has("ev")) {
                JSONObject ev = eh.getJSONObject("ev");
                if (ev.has("la")) {
                    evLat = ev.getDouble("la");
                }
                if (ev.has("lo")) {
                    evLat = ev.getDouble("lo");
                }
                if (ev.has("dp")) {
                    evDepth = ev.getDouble("dp");
                }
            }
            if (staLat != null && staLon != null && evLat != null && evLon != null) {
                depthCorrect(evDepth, staDepth);
                List<Double[]> staList = new ArrayList<>();
                staList.add(new Double[] {staLat, staLon});
                List<Arrival> arrivals = calcEventStation( new Double[] {evLat,evLon},
                        staList);
                JSONObject taup = resultAsJSONObject(modelName, depth, getReceiverDepth(), getPhaseNames(), arrivals);
                eh.put("taup", taup);
            }
        }
        tmpFile.renameTo(msd3File);
    }

    @Override
    public List<Arrival> calculate(List<Double> degreesList) throws TauPException {
        return super.calculate(degreesList);
    }


    @Override
    public String getStdUsage() {
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1,
                className.length());
        return "Usage: " + className.toLowerCase() + " [arguments]"
        +"  or, for purists, java "
                + this.getClass().getName() + " [arguments]"
        +"\nArguments are:"
        +"-ph phase list     -- comma separated phase list,\n"
                + "                      use phase-# to specify the sac header,\n"
                + "                      for example, ScS-8 puts ScS in t8\n"
                + "-pf phasefile      -- file containing phases\n\n"
                + "-mod[el] modelname -- use velocity model \"modelname\" for calculations\n"
                + "                      Default is iasp91.\n\n";
    }

    public String getUsageTail() {
        return "\n"
                + "--prop [propfile]   -- set configuration properties\n"
                + "--debug             -- enable debugging output\n"
                + "--verbose           -- enable verbose output\n"
                + "--version           -- print the version\n"
                + "--help              -- print this out, but you already know that!\n";
    }

    @Override
    public String getUsage() {
        return getStdUsage()
        +"--evdpkm            -- sac depth header is in km, default is meters\n"
        +getUsageTail()
        +"ms3filename [ms3filename ...]"
        +"\nEx: taup_setmseed3 "
                + "--mod S_prem -ph S,ScS wmq.ms3 wmq.ms3 wmq.ms3"
        +"puts the S and ScS in the extra headers in each record in these files."
        +"Values are within the \"taup\" key and are the same as the output of taup time --json";
    }

    @Override
    public String[] parseCmdLineArgs(String[] args) throws IOException {
        int i = 0;
        String[] leftOverArgs;
        int numNoComprendoArgs = 0;
        File tempFile;
        leftOverArgs = super.parseCmdLineArgs(args);
        String[] noComprendoArgs = new String[leftOverArgs.length];
        while(i < leftOverArgs.length) {
            if(dashEquals("help", leftOverArgs[i])) {
                noComprendoArgs[numNoComprendoArgs++] = leftOverArgs[i];
            } else {
                tempFile = new File(leftOverArgs[i]);
                if(tempFile.exists() && (tempFile.isFile() || tempFile.isDirectory() ) && tempFile.canRead()) {
                    mseed3FileNames.add(leftOverArgs[i]);
                } else {
                    if(! tempFile.exists()) {
                        System.err.println(leftOverArgs[i]+" does not exist. "+tempFile.getAbsolutePath() );
                    } else if( ! (tempFile.isFile() || tempFile.isDirectory())) {
                        System.err.println(leftOverArgs[i]+" is not a file or directory.");
                    } else if( ! tempFile.canRead()) {
                        System.err.println(leftOverArgs[i]+" is not readable.");
                    }
                    noComprendoArgs[numNoComprendoArgs++] = leftOverArgs[i];
                }
            }
            i++;
        }
        if(numNoComprendoArgs > 0) {
            String[] temp = new String[numNoComprendoArgs];
            System.arraycopy(noComprendoArgs, 0, temp, 0, numNoComprendoArgs);
            return temp;
        } else {
            return new String[0];
        }
    }

    /**
     * Allows TauP_SetMSeed3 to run as an application. Creates an instance of
     * TauP_SetMSeed3.
     *
     * ToolRun.main should be used instead.
     */
    public static void main(String[] args) throws IOException {
        ToolRun.legacyRunTool(ToolRun.SETMSEED3, args);
    }


    protected List<String> mseed3FileNames = new ArrayList<String>();

}
