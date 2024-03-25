package edu.sc.seis.TauP;

public class EventAzimuth extends LatLonable {

    public EventAzimuth(double evtLat, double evtLon, double azimuth) {
        this.evtLat = evtLat;
        this.evtLon = evtLon;
        this.azimuth = azimuth;
    }

    double evtLat;
    double evtLon;
    double azimuth;

    @Override
    public double[] calcLatLon(double calcDist, double totalDist) {
        double[] out =  new double[2];
        if (isGeodetic()) {
            throw new RuntimeException("geodtic not yet");
        }
        out[0] = SphericalCoords.latFor(evtLat, evtLon, calcDist, azimuth);
        out[1] = SphericalCoords.lonFor(evtLat, evtLon, calcDist, azimuth);
        return out;
    }
}
