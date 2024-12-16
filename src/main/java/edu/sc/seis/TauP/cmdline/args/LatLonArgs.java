package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.seisFile.Location;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

public class LatLonArgs {
    protected List<Location> stationList = new ArrayList<>();
    protected List<Location> eventList = new ArrayList<>();

    @CommandLine.Option(names = {"--sta", "--station"},
            arity = "2",
            paramLabel = "l",
            description = "station latitude and longitude. Creates a distance if event is also given."
    )
    public void setStationLatLon(List<Double> stationLatLon) {
        if (stationLatLon.isEmpty()) {
            stationList.clear();
        }
        if (stationLatLon.size() % 2 != 0) {
            throw new IllegalArgumentException("Station lat lon must have even number of items: " + stationLatLon.size());
        }
        for (int i = 0; i < stationLatLon.size() / 2; i += 2) {
            stationList.add(new Location(stationLatLon.get(i), stationLatLon.get(i + 1)));
        }
    }

    public List<Location> getStationLocations() {
        return stationList;
    }

    @CommandLine.Option(names = {"--evt", "--event"},
            paramLabel = "l",
            arity = "2",
            description = "event latitude and longitude.  Creates a distance if station is also given.")
    public void setEventLatLon(List<Double> eventLatLon) {
        if (eventLatLon.isEmpty()) {
            eventList.clear();
        }
        if (eventLatLon.size() % 2 != 0) {
            throw new IllegalArgumentException("Event lat lon must have even number of items: " + eventLatLon.size());
        }
        for (int i = 0; i < eventLatLon.size() / 2; i += 2) {
            eventList.add(new Location(eventLatLon.get(i), eventLatLon.get(i + 1)));
        }
    }

    public List<Location> getEventLocations() {
        return eventList;
    }
}
