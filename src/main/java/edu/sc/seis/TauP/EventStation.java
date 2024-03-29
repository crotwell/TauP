package edu.sc.seis.TauP;


import edu.sc.seis.seisFile.Location;

public class EventStation extends LatLonable {
    public EventStation(double evtLat, double evtLon, double staLat, double staLon) {
        this.evt = new Location(evtLat, evtLon);
        this.sta = new Location(staLat, staLon);
    }
    public EventStation(Location evt, Location sta) {
        this.evt = evt;
        this.sta = sta;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist) {
        double[] out =  new double[2];
        if (isGeodetic()) {
            throw new RuntimeException("geodtic not yet");
        }
        double azimuth = SphericalCoords.azimuth(evt, sta);
        out[0] = SphericalCoords.latFor(evt, calcDist, azimuth);
        out[1] = SphericalCoords.lonFor(evt, calcDist, azimuth);
        return out;
    }

    Location evt;
    Location sta;
}
