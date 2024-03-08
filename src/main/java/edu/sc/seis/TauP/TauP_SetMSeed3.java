package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;
import edu.sc.seis.seisFile.SeisFileException;
import edu.sc.seis.seisFile.fdsnws.quakeml.Event;
import edu.sc.seis.seisFile.fdsnws.quakeml.EventIterator;
import edu.sc.seis.seisFile.fdsnws.quakeml.Origin;
import edu.sc.seis.seisFile.fdsnws.quakeml.Quakeml;
import edu.sc.seis.seisFile.fdsnws.stationxml.*;
import edu.sc.seis.seisFile.mseed3.*;
import edu.sc.seis.seisFile.TimeUtils;

import java.io.*;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.xml.stream.XMLStreamException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

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
            processRecord(dr3);
            dr3.write(dos);
        }
        dos.close();
        tmpFile.renameTo(msd3File);
    }

    public void processRecord(MSeed3Record dr3) throws TauPException {
        Location staLoc = null;
        Location evLoc = null;
        Instant evTime = null;


        MSeed3EH eh = new MSeed3EH(dr3.getExtraHeaders());
        Channel chan = findChannelBySID(dr3.getSourceId(), dr3.getStartInstant());
        if (chan != null) {
            staLoc = chan.asLocation();
        } else {
            staLoc = eh.channelLocation();
        }
        Event quake = findQuakeInTime(dr3.getStartInstant(), quakeOTimeTol);
        if (quake != null) {
            Origin o = quake.getPreferredOrigin();
            evLoc = o.asLocation();
            evTime = o.getTime().asInstant();
        } else {
            evLoc = eh.quakeLocation();
            evTime = eh.quakeTime();
        }
        Double gcarc = null;

        if (staLoc != null && evLoc != null) {
            DistAz distAz = new DistAz(staLoc, evLoc);
            gcarc = distAz.getDelta();
            depthCorrect(evLoc.getDepthKm(), staLoc.getDepthKm());
        } else  {
            if (evLoc != null) {
                depthCorrect(evLoc.getDepthKm(), 0.0);
            }
            gcarc = eh.gcarc() != null ? eh.gcarc().doubleValue() : null;
        }

        List<Arrival> arrivals = null;
        if (gcarc != null) {
            List<Double> degreesList = Arrays.asList(new Double[] {gcarc});
            arrivals = calculate(degreesList);
        }

        if (arrivals != null) {

            if (ehKey != null && ehKey.length() > 0) {
                JSONObject taup = resultAsJSONObject(modelName, depth, getReceiverDepth(), getPhaseNames(), arrivals);
                eh.getEH().put(ehKey, taup);
            } else {
                JSONObject bag = eh.getBagEH();
                if (evTime == null) {
                    System.err.println("Unable to extract event origin time, skipping record");
                }
                insertMarkers(bag, arrivals, evTime);
            }
        } else {
            System.out.println("Insufficient info in eh to calc travel times");
            System.out.println("  st: "+staLoc+"  gcarc="+gcarc+"  ev: "+evLoc+" at "+evTime);
        }
    }

    public static void insertMarkers(JSONObject bag, List<Arrival> arrivals, Instant evTime) {
        JSONArray markers = new JSONArray();
        if (bag.has("mark")) {
            markers = bag.getJSONArray("mark");
        }
        for (Arrival arrival : arrivals) {
            markers.put(createEHMarker(arrival, evTime));
        }
        bag.put("mark", markers);
    }

    public static JSONObject createEHMarker(Arrival arrival, Instant evTime) {
        JSONObject mark = new JSONObject();
        mark.put("n", arrival.getName());
        mark.put("tm", TimeUtils.toISOString(evTime.plusMillis(Math.round(arrival.getTime()*1000))));
        mark.put("mtype", "md");
        return mark;
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
        +"--staxml filename   -- load station location from stationxml file, default uses extra headers\n"
        +"--qml filename      -- load earthquake location from quakeml file, default uses extra headers\n"
        +"--qmltol tol        -- origin time tolerance when loading from quakeml file, default is 1 hour\n"
        +"--eh                -- eh key for full results, default is to only create markers\n"
        +getUsageTail()
        +"ms3filename [ms3filename ...]"
        +"\nEx: taup_setmseed3 "
                + "--mod S_prem -ph S,ScS wmq.ms3 wmq.ms3 wmq.ms3"
        +"puts the S and ScS as markers in the extra headers in each record in these files."
        +"Markers are within the \"bag/mark\" key. Full results are the same as the output of taup time --json";
    }

    @Override
    public String[] parseCmdLineArgs(String[] args) throws IOException, TauPException {
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
                } else if(dashEquals("qml", leftOverArgs[i])) {
                    quakemlFilename = leftOverArgs[i+1];
                    i++;
                } else if(dashEquals("qmltol", leftOverArgs[i])) {
                    try {
                        int seconds = Integer.parseInt(leftOverArgs[i + 1]);
                        quakeOTimeTol = Duration.ofSeconds(seconds);
                    } catch (NumberFormatException e) {
                        try {
                            quakeOTimeTol = Duration.parse(leftOverArgs[i + 1]);
                        } catch (DateTimeParseException ee) {
                            throw new TauPException("Unable to parse qmltol: "+leftOverArgs[i+1], ee);
                        }
                    }
                    i++;
                } else if(dashEquals("staxml", leftOverArgs[i])) {
                    stationxmlFilename = leftOverArgs[i+1];
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

        try {
            if (stationxmlFilename != null ) {
                FDSNStationXML staxml = FDSNStationXML.loadStationXML(stationxmlFilename);
                networks = staxml.extractAllNetworks();
            }
        } catch (XMLStreamException | SeisFileException e) {
            throw new TauPException("Unable to process stationxml from "+stationxmlFilename, e);
        }
        try {
            if (quakemlFilename != null) {
                FileReader reader = new FileReader(quakemlFilename);
                Quakeml quakeml = Quakeml.loadQuakeML(reader);
                quakes = quakeml.extractAllEvents();
                reader.close();
            }
        } catch (XMLStreamException | SeisFileException e) {
            throw new TauPException("Unable to process quakeml from "+quakemlFilename, e);
        }
        if(numNoComprendoArgs > 0) {
            String[] temp = new String[numNoComprendoArgs];
            System.arraycopy(noComprendoArgs, 0, temp, 0, numNoComprendoArgs);
            return temp;
        } else {
            return new String[0];
        }
    }

    Channel findChannelBySID(FDSNSourceId sid, Instant time) {
        for(Network n : networks.keySet()) {
            if (n.getNetworkCode().equals(sid.getNetworkCode())) {
                for (Station s : networks.get(n)) {
                    if (s.getStationCode().equals(sid.getStationCode())) {
                        for (Channel c : s.getChannelList()) {
                            if (c.getLocCode().equals(sid.getLocationCode()) && c.getChannelCode().equals(sid.getChannelCode())) {
                                if (c.getStartDateTime().isBefore(time) && (c.getEndDateTime() == null || c.getEndDateTime().isAfter(time))) {
                                    return c;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    Event findQuakeInTime(Instant time, Duration tol) {
        Instant early = time.minus(tol);
        Instant late = time.plus(tol);
        for (Event e : quakes) {
            Origin o = e.getPreferredOrigin();
            if (o != null) {
                Instant otime = o.getTime().asInstant();
                if (otime.isAfter(early) && otime.isBefore(late)) {
                    return e;
                }
            }
        }
        return null;
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

    protected Duration quakeOTimeTol = Duration.ofSeconds(3600);
    protected String quakemlFilename = null;
    protected String stationxmlFilename = null;

    protected List<String> mseed3FileNames = new ArrayList<String>();

    protected Map<Network, List<Station>> networks = new HashMap<>();

    protected List<Event> quakes = new ArrayList<>();
}
