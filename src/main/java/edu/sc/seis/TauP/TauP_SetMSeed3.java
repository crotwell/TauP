package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;
import edu.sc.seis.seisFile.SeisFileException;
import edu.sc.seis.seisFile.fdsnws.quakeml.Event;
import edu.sc.seis.seisFile.fdsnws.quakeml.Origin;
import edu.sc.seis.seisFile.fdsnws.quakeml.Quakeml;
import edu.sc.seis.seisFile.fdsnws.stationxml.*;
import edu.sc.seis.seisFile.mseed3.*;
import edu.sc.seis.seisFile.TimeUtils;

import java.io.*;

import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;

import javax.xml.stream.XMLStreamException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

@CommandLine.Command(name = "setmseed3")
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
            CommandLine.usage(this, System.out);
            return;
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
        Channel chan = FDSNStationXML.findChannelBySID(networks, dr3.getSourceId(), dr3.getStartInstant());
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
            // geodetic vs spherical???
            DistAz distAz = new DistAz(staLoc, evLoc);
            gcarc = distAz.getDelta();
            modelArgs.setSourceDepth(evLoc.getDepthKm());
            modelArgs.setReceiverDepth(staLoc.getDepthKm());
        } else  {
            if (evLoc != null) {
                modelArgs.setReceiverDepth(staLoc.getDepthKm());
            }
            gcarc = eh.gcarc() != null ? eh.gcarc().doubleValue() : null;
        }

        List<Arrival> arrivals = null;
        if (gcarc != null) {
            List<RayCalculateable> degreesList = Arrays.asList(DistanceRay.ofDegrees(gcarc));
            arrivals = calcAll(getSeismicPhases(), degreesList);
        }

        if (arrivals != null) {

            if (ehKey != null && ehKey.length() > 0) {
                JSONObject taup = resultAsJSONObject(modelArgs.getModelName(), modelArgs.getSourceDepth(), getReceiverDepth(), getPhaseNames(), arrivals);
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

    public String getEhKey() {
        return ehKey;
    }

    @CommandLine.Option(names = "--eh",
            description = "key to store full output within extra headers within, otherwise use abbreviateg 'bag' markers")
    public void setEhKey(String ehKey) {
        this.ehKey = ehKey;
    }

    public Duration getQuakeOTimeTol() {
        return quakeOTimeTol;
    }

    @CommandLine.Option(names = "--qmltol",
            defaultValue = "PT1H",
            description = "time window to search for origins in a QuakeML file")
    public void setQuakeOTimeTol(Duration quakeOTimeTol) {
        this.quakeOTimeTol = quakeOTimeTol;
    }

    public String getQuakemlFilename() {
        return quakemlFilename;
    }

    @CommandLine.Option(names = {"--qml", "--quakeml"},
            description = "QuakeML file to load to search for origins that match this waveform")
    public void setQuakemlFilename(String quakemlFilename) {
        this.quakemlFilename = quakemlFilename;
    }

    public String getStationxmlFilename() {
        return stationxmlFilename;
    }

    @CommandLine.Option(names = "--staxml", description = "StationXML file to extract station lat/lon from")
    public void setStationxmlFilename(String stationxmlFilename) {
        this.stationxmlFilename = stationxmlFilename;
    }

    public List<String> getMseed3FileNames() {
        return mseed3FileNames;
    }

    @CommandLine.Parameters
    public void setMseed3FileNames(List<String> mseed3FileNames) {
        this.mseed3FileNames = mseed3FileNames;
    }

    public Map<Network, List<Station>> getNetworks() {
        return networks;
    }

    public void setNetworks(Map<Network, List<Station>> networks) {
        this.networks = networks;
    }

    public List<Event> getQuakes() {
        return quakes;
    }

    public void setQuakes(List<Event> quakes) {
        this.quakes = quakes;
    }

    protected String ehKey = null;

    protected Duration quakeOTimeTol = Duration.ofSeconds(3600);
    protected String quakemlFilename = null;
    protected String stationxmlFilename = null;

    protected List<String> mseed3FileNames = new ArrayList<String>();

    protected Map<Network, List<Station>> networks = new HashMap<>();

    protected List<Event> quakes = new ArrayList<>();
}
