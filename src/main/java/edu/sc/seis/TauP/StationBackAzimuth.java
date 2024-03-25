package edu.sc.seis.TauP;

public class StationBackAzimuth extends LatLonable {
    public StationBackAzimuth(double staLat, double staLon, double backAzimuth) {
        this.staLat= staLat;
        this.staLon = staLon;
        this.backAzimuth = backAzimuth;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist) {
        double[] out =  new double[2];
        if (isGeodetic()) {
            throw new RuntimeException("geodtic not yet");
        }
        double evtLat = SphericalCoords.latFor(staLat, staLon, backAzimuth, totalDist);
        double evtLon = SphericalCoords.lonFor(staLat, staLon, backAzimuth, totalDist);
        double azimuth = SphericalCoords.azimuth(evtLat, evtLon, staLat, staLon);
        out[0] = SphericalCoords.latFor(evtLat, evtLon, calcDist, azimuth);
        out[1] = SphericalCoords.lonFor(evtLat, evtLon, calcDist, azimuth);
        return out;
    }

    double staLat;
    double staLon;
    double backAzimuth;
}
