package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.GeodeticArgs;
import edu.sc.seis.TauP.cmdline.args.OutputTypes;
import edu.sc.seis.TauP.cmdline.args.QmlStaxmlArgs;
import edu.sc.seis.seisFile.Location;
import edu.sc.seis.seisFile.SeisFileException;
import edu.sc.seis.seisFile.fdsnws.quakeml.Event;
import edu.sc.seis.seisFile.fdsnws.quakeml.Origin;
import edu.sc.seis.seisFile.fdsnws.quakeml.Quakeml;
import edu.sc.seis.seisFile.fdsnws.stationxml.*;
import edu.sc.seis.seisFile.mseed3.*;
import edu.sc.seis.seisFile.TimeUtils;

import java.io.*;

import edu.sc.seis.seisFile.mseed3.ehbag.Marker;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;

import javax.xml.stream.XMLStreamException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.ABREV_SYNOPSIS;
import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;

@CommandLine.Command(name = "setms3",
        description = {
                "Save travel times in the extra header of miniseed3 files.",
                "    https://crotwell.github.io/ms3eh/",
                "has details on the JSON structure.",
                ""
        },
        optionListHeading = OPTIONS_HEADING,
        abbreviateSynopsis = ABREV_SYNOPSIS,
        usageHelpAutoWidth = true)
public class TauP_SetMSeed3 extends TauP_AbstractPhaseTool {

    public TauP_SetMSeed3() {
        super(null);
    }

    @Override
    public String getOutputFormat() {
        return OutputTypes.MS3;
    }

    @Override
    public void start() throws IOException, TauPException {
        if (mseed3FileNames.isEmpty()) {
            CommandLine.usage(this, System.out);
            return;
        }

        try {
            if (qmlStaxmlArgs.hasStationXML() ) {
                FDSNStationXML staxml = FDSNStationXML.loadStationXML(qmlStaxmlArgs.getStationxmlFilename());
                networks = staxml.extractAllNetworks();
            }
        } catch (XMLStreamException | SeisFileException e) {
            throw new TauPException("Unable to process stationxml from "+qmlStaxmlArgs.getStationxmlFilename(), e);
        }
        try {
            if (qmlStaxmlArgs.hasQml()) {
                FileReader reader = new FileReader(qmlStaxmlArgs.getQuakemlFilename());
                Quakeml quakeml = Quakeml.loadQuakeML(reader);
                quakes = quakeml.extractAllEvents();
                reader.close();
            }
        } catch (XMLStreamException | SeisFileException e) {
            throw new TauPException("Unable to process quakeml from "+qmlStaxmlArgs.getQuakemlFilename(), e);
        }
        for (String filename : mseed3FileNames) {
            try {
                if (isVerbose()) {
                    Alert.debug(filename);
                }
                processMSeed3File(new File(filename));
            } catch(SeisFileException e) {
                throw new TauPException(e);
            }
        }
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void validateArguments() throws TauPException {

    }

    public void processMSeed3File(File msd3File) throws IOException, SeisFileException, TauPException {
        int fileBytes = (int) msd3File.length();
        int bytesRead = 0;
        MSeed3Record dr3;
        File tmpFile = File.createTempFile("taup", "ms3", msd3File.getParentFile());
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(msd3File)));
        while (bytesRead < fileBytes && (dr3 = MSeed3Record.read(dis)) != null) {
            bytesRead += dr3.getSize();
            processRecord(dr3);
            dr3.write(dos);
        }
        dos.close();
        if ( !tmpFile.renameTo(msd3File)) {
            throw new SetSacException("unable to rename temp file: "+tmpFile+" to "+msd3File);
        }
    }

    public void processRecord(MSeed3Record dr3) throws TauPException {
        Location staLoc;
        Location evLoc;
        Instant evTime;


        RayCalculateable rayCalculateable;
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

        if (staLoc != null && evLoc != null) {
            // geodetic vs spherical???
            if (geodeticArgs.isGeodetic()) {
                rayCalculateable = DistanceRay.ofGeodeticStationEvent(
                        staLoc,
                        evLoc,
                        geodeticArgs.getInverseEllipFlattening()
                );
            } else {
                rayCalculateable = DistanceRay.ofStationEvent(staLoc, evLoc);
            }
        } else if (eh.gcarc() != null) {
            rayCalculateable = DistanceRay.ofDegrees(eh.gcarc());
        } else {
            throw new SetSacException("Unable to get distance from MS3 record, skipping. :"+dr3);
        }
        
        // save values used to make calc, if not already from eh
        if (quake != null) {
            eh.addToBag(quake);
        }
        if (chan != null) {
            eh.addToBag(chan);
        }

        List<Double> receiverDepthList = new ArrayList<>();
        if (staLoc != null) {
            setSingleReceiverDepth(staLoc.getDepthKm());
            receiverDepthList.add(staLoc.getDepthKm());
        } else {
            setSingleReceiverDepth(0);
            receiverDepthList.add(0.0);
        }
        double depth = 0;
        if (evLoc != null) {
            depth = evLoc.getDepthKm();
        }
        List<SeismicPhase> seismicPhaseList = calcSeismicPhases( depth, receiverDepthList, modelArgs.getScatterer());

        List<Arrival> arrivals = new ArrayList<>();
        for (SeismicPhase phase : seismicPhaseList) {
            arrivals.addAll(rayCalculateable.calculate(phase));
        }

        if (!arrivals.isEmpty()) {

            if (ehKey != null && !ehKey.isEmpty()) {
                JSONObject taup = TauP_Time.resultAsJSONObject(modelArgs.getModelName(),
                        modelArgs.getSourceDepths(), modelArgs.getReceiverDepths(), parsePhaseNameList(), arrivals);
                eh.getEH().put(ehKey, taup);
            } else {
                JSONObject bag = eh.getBagEH();
                if (evTime == null) {
                    Alert.warning("Unable to extract event origin time, skipping record");
                }

                List<Marker> mList = new ArrayList<>();
                for (Arrival arrival : arrivals) {
                    mList.add(createEHMarker(arrival, evTime));
                }
                insertMarkers(bag, arrivals, evTime);
            }
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

    public static Marker createEHMarker(Arrival arrival, Instant evTime) {
        Marker m = new Marker(arrival.getName(), evTime.plusMillis(Math.round(arrival.getTime()*1000)).atZone(TimeUtils.TZ_UTC));
        m.setType(MSeed3EHKeys.MARKER_MODELED);
        return m;
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

    public String getEhKey() {
        return ehKey;
    }

    @CommandLine.Option(names = "--taupeh",
            arity = "0..1",
            fallbackValue = "taup",
            description = "key to store full TauP JSON output within extra headers within, otherwise use abbreviated 'bag' style markers."+
                    "If specified without parameter, extra header key of ${FALLBACK-VALUE} will be used.")
    public void setEhKey(String ehKey) {
        this.ehKey = ehKey;
    }

    public Duration getQuakeOTimeTol() {
        return quakeOTimeTol;
    }

    @CommandLine.Option(names = "--qmltol",
            defaultValue = "PT1H",
            description = "time window to search for origins in a QuakeML file as an ISO8601 string, default value is ${DEFAULT-VALUE}.")
    public void setQuakeOTimeTol(Duration quakeOTimeTol) {
        this.quakeOTimeTol = quakeOTimeTol;
    }

    public List<String> getMseed3FileNames() {
        return mseed3FileNames;
    }

    @CommandLine.Parameters(description = "Miniseed3 files to process.",
            paramLabel = "mseed3file")
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

    @CommandLine.Mixin
    protected QmlStaxmlArgs qmlStaxmlArgs = new QmlStaxmlArgs();

    protected List<String> mseed3FileNames = new ArrayList<>();

    protected Map<Network, List<Station>> networks = new HashMap<>();

    protected List<Event> quakes = new ArrayList<>();

    @CommandLine.Mixin
    GeodeticArgs geodeticArgs = new GeodeticArgs();
}
