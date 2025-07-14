package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.BuildVersion;
import edu.sc.seis.TauP.DistAz;
import edu.sc.seis.TauP.Outputs;
import edu.sc.seis.TauP.TauPException;
import edu.sc.seis.seisFile.Location;
import edu.sc.seis.seisFile.SeisFileException;
import edu.sc.seis.seisFile.fdsnws.*;
import edu.sc.seis.seisFile.fdsnws.quakeml.*;
import edu.sc.seis.seisFile.fdsnws.stationxml.Channel;
import edu.sc.seis.seisFile.fdsnws.stationxml.FDSNStationXML;
import edu.sc.seis.seisFile.fdsnws.stationxml.Network;
import edu.sc.seis.seisFile.fdsnws.stationxml.Station;
import edu.sc.seis.seisFile.mseed3.FDSNSourceId;
import edu.sc.seis.seisFile.mseed3.FDSNSourceIdException;
import picocli.CommandLine;

import javax.xml.stream.XMLStreamException;
import java.io.FileReader;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.sc.seis.seisFile.TimeUtils.TZ_UTC;

public class QmlStaxmlArgs {


    public static final String staSourceIdRegExString =
            "FDSN:([A-Z0-9]{1,8})_"      // net
                    +"([A-Z0-9-]{1,8})"; // sta
    public static final Pattern staSourceIdRegEx = Pattern.compile(staSourceIdRegExString);

    public static List<Location> loadStationsForSid(List<String> sidList) throws FDSNSourceIdException, FDSNWSException {
        if (sidList.isEmpty()) {
            // don't query for empty list
            return List.of();
        }
        FDSNStationQueryParams staQP = new FDSNStationQueryParams();
        staQP.setLevel("station");

        for (String sid : sidList) {
            if (sid.startsWith(FDSNSourceId.FDSN_PREFIX)) {
                Matcher m = staSourceIdRegEx.matcher(sid);
                if (m.matches()) {
                    // looks like FDSH:XX_STATION so station id
                    staQP.appendToNetwork(m.group(1));
                    staQP.appendToStation(m.group(2));
                    continue;
                }
                FDSNSourceId fdsn = FDSNSourceId.parse(sid);
                staQP.appendToNetwork(fdsn.getNetworkCode());
                staQP.appendToStation(fdsn.getStationCode());
            } else {
                String[] splitSid = sid.split("[_\\.]");
                if (splitSid.length >= 2) {
                    staQP.appendToNetwork(splitSid[0]);
                    staQP.appendToStation(splitSid[1]);
                }
            }
        }

        List<Location> out = new ArrayList<>();
        IRISFedCatQueryParams qp = new IRISFedCatQueryParams(staQP);
        qp.setFormat(IRISFedCatQueryParams.FORMAT_TEXT);
        IRISFedCatQuerier querier = new IRISFedCatQuerier(qp);
        querier.setUserAgent(getTaupUserAgent());

        List<Station> staList = querier.getStationsFromText();
        List<Station> uniq = new ArrayList<>();
        for (int i = 0; i < staList.size(); i++) {
            Station a = staList.get(i);
            boolean found = false;
            for (int j = i+1; j < staList.size(); j++) {
                Station b = staList.get(j);
                if (Objects.equals(a.getNetworkCode(), b.getNetworkCode())
                        && Objects.equals(a.getStationCode(), b.getStationCode())) {
                    DistAz distAz = new DistAz(a.asLocation(), b.asLocation());
                    if (distAz.getDelta() < 0.01) {
                        // hundredth of degree approx 1 km, so looks like same station
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                uniq.add(a);
            }
        }
        staList= uniq;
        for (Station sta : staList) {
            Location staLoc = sta.asLocation();
            String desc = sta.getNetworkCode()+"."+sta.getStationCode()
                    +" " + Outputs.formatLatLon(staLoc.getLatitude()).trim()
                    + "/" + Outputs.formatLatLon(staLoc.getLongitude()).trim();
            staLoc.setDescription(desc);
            out.add(staLoc);
        }
        return out;
    }

    /**
     * Gets station locations via stationxml file or remote loading of station sourceid.
     * @return List of station locations
     * @throws TauPException If error parsing xml file.
     */
    public List<Location> getStationLocations() throws TauPException {
        List<Location> staList = new ArrayList<>();
        staList.addAll(getSidLocations());
        staList.addAll(getStationXMLLocations());
        return staList;
    }

    public List<Location> getStationXMLLocations() throws TauPException {
        List<Location> staList = new ArrayList<>();
        Map<Network, List<Station>> networks = loadStationXML();
        for (Network net : networks.keySet()) {
            for (Station sta : networks.get(net)) {
                List<Location> allChans = new ArrayList<>();
                for (Channel chan : sta.getChannelList()) {
                    Location cLoc = new Location(chan);
                    boolean found = false;
                    for (Location prev : allChans) {
                        if (prev.equals(cLoc)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        String desc = sta.getNetworkCode()+"."+sta.getStationCode()
                                +" " + Outputs.formatLatLon(cLoc.getLatitude()).trim()
                                + "/" + Outputs.formatLatLon(cLoc.getLongitude()).trim();
                        cLoc.setDescription(desc);
                        allChans.add(cLoc);
                    }
                }
                if (sta.getChannelList().isEmpty()) {
                    // no channels, maybe was retrieved with level=Station, add station location at surface
                    Location staLoc = new Location(sta);
                    String desc = sta.getNetworkCode()+"."+sta.getStationCode()
                            +" " + Outputs.formatLatLon(staLoc.getLatitude()).trim()
                            + "/" + Outputs.formatLatLon(staLoc.getLongitude()).trim();
                    staLoc.setDescription(desc);
                    allChans.add(staLoc);
                }
                staList.addAll(allChans);
            }
        }
        return staList;
    }

    /**
     * Returns a list of event locations as parsed from the command line arguments.
     *
     * @throws TauPException If unable to parse files or load remote resources.
     * @return a {@code List<EventLocation>} representing all parsed event locations,
     *         or an empty list if none have been specified.
     */
    public List<Location> getEventLocations() throws TauPException {
        List<Location> eventLocs = new ArrayList<>();
        List<Event> quakes = new ArrayList<>();
        quakes.addAll(loadQuakeML());
        quakes.addAll(loadEventsFromUSGS(getEventIdList()));
        DateTimeFormatter dformat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(TZ_UTC);
        for (Event evt : quakes) {
            Location evtLoc = new Location(evt);
            Origin origin = evt.getPreferredOrigin();
            Magnitude mag = evt.getPreferredMagnitude();
            StringBuilder desc = new StringBuilder();
            if (origin != null) {
                desc.append(dformat.format(origin.getTime().asInstant()));
            }
            if (mag != null) {
                if (desc.length()!= 0) {
                    desc.append(" ");
                }
                desc.append(mag.getMag().getValue()).append(" ").append(mag.getType());
            }
            if (desc.length() != 0) {
                desc.append(" ");
            }
            desc.append(Outputs.formatLatLon(evtLoc.getLatitude()).trim())
                    .append("/")
                    .append(Outputs.formatLatLon(evtLoc.getLongitude()).trim());
            if ( desc.length()!= 0) {
                evtLoc.setDescription(desc.toString());
            }
            eventLocs.add(evtLoc);
        }
        return eventLocs;
    }

    public boolean hasQml() {
        return getQuakemlFilename() != null || !getEventIdList().isEmpty();
    }

    public boolean hasStationXML() {
        return getStationxmlFilename() != null ||  ! this.sidList.isEmpty();
    }

    public Map<Network, List<Station>> loadStationXML() throws TauPException {
        Map<Network, List<Station>> networks = new HashMap<>();
        try {
            if (stationxmlFilename != null ) {
                FDSNStationXML staxml = FDSNStationXML.loadStationXML(stationxmlFilename);
                networks = staxml.extractAllNetworks();
            }
        } catch (IOException | XMLStreamException | SeisFileException e) {
            throw new TauPException("Unable to process stationxml from "+stationxmlFilename, e);
        }
        return networks;
    }

    public List<Event> loadQuakeML() throws TauPException {
        List<Event> quakes = new ArrayList<>();
        try {
            if (quakemlFilename != null) {
                FileReader reader = new FileReader(quakemlFilename);
                Quakeml quakeml = Quakeml.loadQuakeML(reader);
                quakes = quakeml.extractAllEvents();
                reader.close();
            }
        } catch (IOException | XMLStreamException | SeisFileException e) {
            throw new TauPException("Unable to process quakeml from "+quakemlFilename, e);
        }
        return quakes;
    }

    public String getQuakemlFilename() {
        return quakemlFilename;
    }

    @CommandLine.Option(names = {"--qml", "--quakeml"},
            description = "QuakeML file to load for earthquake origins to use")
    public void setQuakemlFilename(String quakemlFilename) {
        this.quakemlFilename = quakemlFilename;
    }


    @CommandLine.Option(names = {"--eid"},
            paramLabel = "evt",
            split = ",",
            description = "event id, like us7000pn9s, for lookup via USGS fdsn event web service. Creates a distance if station is also given."
    )
    public List<String> eidList = new ArrayList<>();

    public List<String> getEventIdList() {
        return eidList;
    }

    public List<Event> loadEventsFromUSGS(List<String> eidList) throws TauPException {
        List<Event> out = new ArrayList<>();
        try {
            for (String eid : eidList) {
                FDSNEventQueryParams qp = new FDSNEventQueryParams();
                qp.setEventid(eid);
                FDSNEventQuerier querier = new FDSNEventQuerier(qp);
                querier.setUserAgent(getTaupUserAgent());
                out.addAll(querier.getQuakeML().extractAllEvents());
            }
        } catch (SeisFileException|XMLStreamException e) {
            throw new TauPException("Unable to load station locations from fedcat service", e);
        }
        return out;
    }

    public String getStationxmlFilename() {
        return stationxmlFilename;
    }

    @CommandLine.Option(names = "--staxml",
            description = "StationXML file to extract station latitudes and longitudes from")
    public void setStationxmlFilename(String stationxmlFilename) {
        this.stationxmlFilename = stationxmlFilename;
    }

    @CommandLine.Option(names = {"--sid"},
            paramLabel = "sta",
            split = ",",
            description = "station id, like CO.HAW or FDSN:CO_HAW, for lookup via fedcat web service. Creates a distance if event is also given."
    )
    public List<String> sidList = new ArrayList<>();

    public List<String> getSidList() {
        return sidList;
    }

    public List<Location> getSidLocations() throws TauPException {
        try {
            return QmlStaxmlArgs.loadStationsForSid(getSidList());
        } catch (FDSNSourceIdException|FDSNWSException e) {
            throw new TauPException("Unable to load station locations from fedcat service", e);
        }
    }

    public static String createDescription(Location evtLoc) {
        String evtDesc;
        if (evtLoc.hasDescription()) {
            evtDesc = evtLoc.getDescription();
        } else {
            evtDesc = Outputs.formatLatLon(evtLoc.getLatitude()).trim()
                    +"/"+Outputs.formatLatLon(evtLoc.getLongitude()).trim();
        }
        return evtDesc;
    }
    
    protected String quakemlFilename = null;
    protected String stationxmlFilename = null;

    public static String getTaupUserAgent() {
        return BuildVersion.getName()+"/"+ BuildVersion.getVersion();
    }
}
