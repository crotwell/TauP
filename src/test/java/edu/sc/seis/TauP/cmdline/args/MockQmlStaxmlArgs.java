package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.TauPException;
import edu.sc.seis.seisFile.SeisFileException;
import edu.sc.seis.seisFile.fdsnws.quakeml.Event;
import edu.sc.seis.seisFile.fdsnws.quakeml.Quakeml;
import edu.sc.seis.seisFile.fdsnws.stationxml.FDSNStationXML;
import edu.sc.seis.seisFile.fdsnws.stationxml.Network;
import edu.sc.seis.seisFile.fdsnws.stationxml.Station;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MockQmlStaxmlArgs extends QmlStaxmlArgs {



    public MockQmlStaxmlArgs() {
        String testqmlFilename = "my_midatlantic.qml";
        String teststaxmlFilename = "my_stations.staml";
        setQuakemlFilename(testqmlFilename);
        setStationxmlFilename(teststaxmlFilename);
    }

    @Override
    public boolean hasQml() {
        return true;
    }

    @Override
    public boolean hasStationXML() {
        return true;
    }

    @Override
    public Map<Network, List<Station>> loadStationXML() throws TauPException {
        Map<Network, List<Station>> networks = new HashMap<>();
        try {
            if (stationxmlFilename != null ) {
                String resource = "edu/sc/seis/TauP/" + getStationxmlFilename();
                InputStream inStream = this.getClass()
                        .getClassLoader()
                        .getResourceAsStream(resource);
                assertNotNull(inStream, "Resource " + resource + " not found.");
                BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
                FDSNStationXML staxml = FDSNStationXML.loadStationXML(in);
                networks = staxml.extractAllNetworks();
            }
        } catch (IOException | XMLStreamException | SeisFileException e) {
            throw new TauPException("Unable to process stationxml from "+stationxmlFilename, e);
        }
        return networks;
    }

    @Override
    public List<Event> loadQuakeML() throws TauPException {
        List<Event> quakes = new ArrayList<>();
        try {
            if (quakemlFilename != null) {
                String resource = "edu/sc/seis/TauP/" + getQuakemlFilename();
                InputStream inStream = this.getClass()
                        .getClassLoader()
                        .getResourceAsStream(resource);
                assertNotNull(inStream, "Resource " + resource + " not found.");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
                Quakeml quakeml = Quakeml.loadQuakeML(reader);
                quakes = quakeml.extractAllEvents();
                reader.close();
            }
        } catch (IOException | XMLStreamException | SeisFileException e) {
            throw new TauPException("Unable to process quakeml from "+quakemlFilename, e);
        }
        return quakes;
    }
    @Override
    public String getQuakemlFilename() {
        return super.getQuakemlFilename();
    }

    @Override
    public void setQuakemlFilename(String quakemlFilename) {
        super.setQuakemlFilename(quakemlFilename);
    }

    @Override
    public String getStationxmlFilename() {
        return super.getStationxmlFilename();
    }

    @Override
    public void setStationxmlFilename(String stationxmlFilename) {
        super.setStationxmlFilename(stationxmlFilename);
    }
}
