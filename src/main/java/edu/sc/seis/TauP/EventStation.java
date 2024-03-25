package edu.sc.seis.TauP;

public class EventStation extends LatLonable {
    public EventStation(double evtLat, double evtLon, double staLat, double staLon) {
        this.evtLat = evtLat;
        this.evtLon = evtLon;
        this.staLat= staLat;
        this.staLon = staLon;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist) {
        double[] out =  new double[2];
        if (isGeodetic()) {
            throw new RuntimeException("geodtic not yet");
        }
        double azimuth = SphericalCoords.azimuth(evtLat, evtLon, staLat, staLon);
        out[0] = SphericalCoords.latFor(evtLat, evtLon, calcDist, azimuth);
        out[1] = SphericalCoords.lonFor(evtLat, evtLon, calcDist, azimuth);
        return out;
    }

    double evtLat;
    double evtLon;
    double staLat;
    double staLon;
}
