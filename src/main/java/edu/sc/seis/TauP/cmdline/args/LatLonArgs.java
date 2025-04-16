package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.seisFile.Location;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

public class LatLonArgs {

    @CommandLine.Option(names = {"--sta", "--station"},
            arity = "2",
            paramLabel = "lat lon",
            hideParamSyntax = true,
            description = "station latitude and longitude. Creates a distance if event is also given."
    )
    protected List<Double> stationLatLonList = new ArrayList<>();

    public void setStationLatLon(List<Double> stationLatLon) {
        stationLatLonList = stationLatLon;
    }

    public List<Location> getStationLocations() {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < stationLatLonList.size(); i += 2) {
            out.add(new Location(stationLatLonList.get(i), stationLatLonList.get(i + 1)));
        }
        return out;
    }

    public boolean hasStationLatLon() {
        return !( this.stationLatLonList.isEmpty()) ;
    }

    @CommandLine.Option(names = {"--evt", "--event"},
            arity = "2",
            paramLabel = "lat lon",
            hideParamSyntax = true,
            description = "event latitude and longitude.  Creates a distance if station is also given.")
    protected List<Double> eventLatLonList = new ArrayList<>();

    public void setEventLatLon(List<Double> eventLatLon) {
        eventLatLonList = eventLatLon;
    }

    public List<Location> getEventLocations() {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < eventLatLonList.size(); i += 2) {
            out.add(new Location(eventLatLonList.get(i), eventLatLonList.get(i + 1)));
        }
        return out;
    }

    public boolean hasEventLatLon() {
        return !( this.eventLatLonList.isEmpty()) ;
    }

    public void validateArguments() {
        for (Location loc : getEventLocations()) {
            if (loc.getLatitude() < -90 || loc.getLatitude() > 90) {
                String desc = loc.getDescription();
                desc = (desc != null) ? desc : loc.getLatitude()+"/"+loc.getLongitude();
                throw new IllegalArgumentException("Latitude must be -90 <= lat <= 90, but was "
                        +loc.getLatitude()+" for "+desc);
            }
        }
        for (Location loc : getStationLocations()) {
            if (loc.getLatitude() < -90 || loc.getLatitude() > 90) {
                String desc = loc.getDescription();
                desc = (desc != null) ? desc : loc.getLatitude()+"/"+loc.getLongitude();
                throw new IllegalArgumentException("Latitude must be -90 <= lat <= 90, but was "
                        +loc.getLatitude()+" for "+desc);            }
        }
    }
}
