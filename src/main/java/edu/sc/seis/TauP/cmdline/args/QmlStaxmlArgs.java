package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.Outputs;
import edu.sc.seis.TauP.TauPException;
import edu.sc.seis.seisFile.Location;
import edu.sc.seis.seisFile.SeisFileException;
import edu.sc.seis.seisFile.fdsnws.quakeml.Event;
import edu.sc.seis.seisFile.fdsnws.quakeml.Magnitude;
import edu.sc.seis.seisFile.fdsnws.quakeml.Origin;
import edu.sc.seis.seisFile.fdsnws.quakeml.Quakeml;
import edu.sc.seis.seisFile.fdsnws.stationxml.Channel;
import edu.sc.seis.seisFile.fdsnws.stationxml.FDSNStationXML;
import edu.sc.seis.seisFile.fdsnws.stationxml.Network;
import edu.sc.seis.seisFile.fdsnws.stationxml.Station;
import picocli.CommandLine;

import javax.xml.stream.XMLStreamException;
import java.io.FileReader;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.sc.seis.seisFile.TimeUtils.TZ_UTC;

public class QmlStaxmlArgs {


    public List<Location> getStationLocations() throws TauPException {
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
                staList.addAll(allChans);
            }
        }
        return staList;
    }

    public List<Location> getEventLocations() throws TauPException {
        List<Location> eventLocs = new ArrayList<>();
        List<Event> quakes = loadQuakeML();
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
        return getQuakemlFilename() != null;
    }

    public boolean hasStationXML() {
        return getStationxmlFilename() != null;
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

    public String getStationxmlFilename() {
        return stationxmlFilename;
    }

    @CommandLine.Option(names = "--staxml",
            description = "StationXML file to extract station latitudes and longitudes from")
    public void setStationxmlFilename(String stationxmlFilename) {
        this.stationxmlFilename = stationxmlFilename;
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
}
