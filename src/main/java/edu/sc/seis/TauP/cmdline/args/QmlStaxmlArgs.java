package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.BuildVersion;
import edu.sc.seis.TauP.DistAz;
import edu.sc.seis.TauP.Outputs;
import edu.sc.seis.TauP.TauPException;
import edu.sc.seis.seisFile.LatLonLocatable;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QmlStaxmlArgs {


    public static final String staSourceIdRegExString =
            "FDSN:([A-Z0-9]{1,8})_"      // net
                    +"([A-Z0-9-]{1,8})"; // sta
    public static final Pattern staSourceIdRegEx = Pattern.compile(staSourceIdRegExString);

    public static List<Station> loadStationsForSid(List<String> sidList) throws FDSNSourceIdException, FDSNWSException {
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
                String[] splitSid = sid.split("[_.]");
                if (splitSid.length >= 2) {
                    staQP.appendToNetwork(splitSid[0]);
                    staQP.appendToStation(splitSid[1]);
                }
            }
        }

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
        return staList;
    }

    /**
     * Gets station locations via stationxml file or remote loading of station sourceid.
     * @return List of station/channel locations
     * @throws TauPException If error parsing xml file.
     */
    public List<LatLonLocatable> getStationLocations() throws TauPException {
        List<LatLonLocatable> staList = new ArrayList<>();
        staList.addAll(getSidLocations());
        staList.addAll(getStationXMLLocations());
        return staList;
    }

    public List<LatLonLocatable> getStationXMLLocations() throws TauPException {
        List<LatLonLocatable> staList = new ArrayList<>();
        Map<Network, List<Station>> networks = loadStationXML();
        for (Network net : networks.keySet()) {
            for (Station sta : networks.get(net)) {
                List<LatLonLocatable> allChans = new ArrayList<>();
                for (Channel chan : sta.getChannelList()) {
                    if (sta.asLocation().equals(chan.asLocation())) {
                        // same as station,
                        continue;
                    }
                    boolean found = false;
                    for (LatLonLocatable prev : allChans) {
                        if (prev.asLocation().equals(chan.asLocation())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        allChans.add(chan);
                    }
                }
                staList.addAll(allChans);
                if (allChans.isEmpty()) {
                    // no channels, maybe was retrieved with level=Station, add station location at surface
                    staList.add(sta);
                }
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
    public List<LatLonLocatable> getEventLocations() throws TauPException {
        List<LatLonLocatable> eventLocs = new ArrayList<>();
        eventLocs.addAll(loadQuakeML());
        eventLocs.addAll(loadEventsFromUSGS(getEventIdList()));
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

    public List<Station> getSidLocations() throws TauPException {
        try {
            return QmlStaxmlArgs.loadStationsForSid(getSidList());
        } catch (FDSNSourceIdException|FDSNWSException e) {
            throw new TauPException("Unable to load station locations from fedcat service", e);
        }
    }
    
    protected String quakemlFilename = null;
    protected String stationxmlFilename = null;

    public static String getTaupUserAgent() {
        return BuildVersion.getName()+"/"+ BuildVersion.getVersion();
    }
}
