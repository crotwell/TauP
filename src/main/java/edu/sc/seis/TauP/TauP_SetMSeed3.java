package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.SeisFileException;
import edu.sc.seis.seisFile.mseed3.*;
import edu.sc.seis.seisFile.TimeUtils;

import java.io.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.ZonedDateTime;
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
        File tmpFile = File.createTempFile("taup", "ms3", msd3File.getParentFile());
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(msd3File)));
        while (bytesRead < fileBytes && (dr3 = MSeed3Record.read(dis)) != null) {
            bytesRead += dr3.getSize();
            JSONObject eh = dr3.getExtraHeaders();
            JSONObject bag = new JSONObject();
            if (eh.has("bag")) {
                bag = eh.getJSONObject("bag");
            } else {
                System.out.println("No bag");
            }
            Double staLat = null;
            Double staLon = null;
            Double staDepth = 0.0;
            Double evLat = null;
            Double evLon = null;
            Double evDepth = 0.0; // default to zero depth
            Instant evTime = null;

            if (bag.has("st")) {
                JSONObject st = bag.getJSONObject("st");
                if (st.has("la")) {
                    staLat = st.getDouble("la");
                }
                if (st.has("lo")) {
                    staLon = st.getDouble("lo");
                }
                if (st.has("dp")) {
                    // station depth is in meters
                    staDepth = st.getDouble("dp")/1000;
                }
            }
            if (bag.has("ev")) {
                JSONObject ev = bag.getJSONObject("ev");
                if (ev.has("or")) {
                    JSONObject origin = ev.getJSONObject("or");
                    if (origin.has("tm")) {
                        evTime = TimeUtils.parseISOString(origin.getString("tm"));
                    }
                    if (origin.has("la")) {
                        evLat = origin.getDouble("la");
                    }
                    if (origin.has("lo")) {
                        evLon = origin.getDouble("lo");
                    }
                    if (origin.has("dp")) {
                        evDepth = origin.getDouble("dp");
                    }
                }
            }
            depthCorrect(evDepth, staDepth);
            List<Arrival> arrivals = null;
            if (bag.has("path")) {
                JSONObject path = bag.getJSONObject("path");
                if (path.has("gcarc")) {
                    double gcarc = path.getDouble("gcarc");
                    List<Double> degreesList = Arrays.asList(new Double[] {gcarc});
                    arrivals = calculate(degreesList);
                    System.out.println("calc via gcarc");
                }
            }
            if (arrivals == null && staLat != null && staLon != null && evLat != null && evLon != null) {
                List<Double[]> staList = new ArrayList<>();
                staList.add(new Double[] {staLat, staLon});
                arrivals = calcEventStation( new Double[] {evLat,evLon},
                        staList);
                System.out.println("calc via st/ev");
            }
            if (arrivals != null) {
                if (ehKey != null && ehKey.length() > 0) {
                    JSONObject taup = resultAsJSONObject(modelName, depth, getReceiverDepth(), getPhaseNames(), arrivals);
                    eh.put(ehKey, taup);
                } else {
                    JSONArray markers = new JSONArray();
                    if (bag.has("mark")) {
                        markers = bag.getJSONArray("mark");
                    }
                    for (Arrival arrival : arrivals) {
                        JSONObject mark = new JSONObject();
                        mark.put("n", arrival.getName());
                        mark.put("tm", TimeUtils.toISOString(evTime.plusMillis(Math.round(arrival.getTime()*1000))));
                        mark.put("mtype", "md");
                        markers.put(mark);
                    }
                    bag.put("mark", markers);
                    if (! eh.has("bag")) {
                        // in case empty not already in eh
                        eh.put("bag", bag);
                    }
                }
            } else {
                System.out.println("Insufficient info in eh to calc travel times: path: "+bag.has("path")+" st: "+bag.has("st")+" ev:"+bag.has("ev"));
                System.out.println("  st: "+staLat+" "+staLon);
                System.out.println("  ev: "+evLat+" "+evLon+"  dp: "+evDepth+" at "+evTime);
            }
            dr3.write(dos);
        }
        dos.close();
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
        +"--eh                -- eh key to store in, default is to only create markers\n"
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
            } else if(i < leftOverArgs.length-1) {
                if(dashEquals("eh", leftOverArgs[i])) {
                    ehKey = leftOverArgs[i+1];
                    i++;
                }
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

    protected String ehKey = null;

    protected List<String> mseed3FileNames = new ArrayList<String>();

}
