package edu.sc.seis.TauP;

import edu.sc.seis.TauP.CLI.DistanceArgs;

import java.util.List;

public class DistanceRay extends RayCalculateable {

    public static DistanceRay ofDegrees(double deg) {
        DistanceRay val = new DistanceRay();
        val.degrees = deg;
        return val;
    }
    public static DistanceRay ofKilometers(double km) {
        DistanceRay val = new DistanceRay();
        val.kilometers = km;
        return val;
    }
    public static DistanceRay ofRadians(double rad) {
        DistanceRay val = new DistanceRay();
        val.radians = rad;
        return val;
    }

    public static DistanceRay ofStationEvent(double staLat, double staLon, double evtLat, double evtLon) {
        DistanceRay val = DistanceRay.ofDegrees(SphericalCoords.distance(evtLat, evtLon, staLat, staLon));
        val.lat = evtLat;
        val.lon = evtLon;
        val.azimuth = SphericalCoords.azimuth(evtLat, evtLon, staLat, staLon);
        val.backAzimuth = SphericalCoords.azimuth(staLat, staLon, evtLat, evtLon);
        return val;
    }

    public void withEventAzimuth(double evtLat, double evtLon, double azimuth) {
        this.lat = evtLat;
        this.lon = evtLon;
        this.azimuth = azimuth;
        this.backAzimuth = null;
    }

    public void withStationBackAzimuth(double staLat, double staLon, double backazimuth) {
        this.lat = staLat;
        this.lon = staLon;
        this.azimuth = null;
        this.backAzimuth = backazimuth;
    }

    public static DistanceRay ofGeodeticStationEvent(double staLat, double staLon, double evtLat, double evtLon, double flattening) {
        DistAz distAz = new DistAz(staLat, staLon, evtLat, evtLon, flattening);
        DistanceRay val = ofDegrees(distAz.getDelta());
        val.lat = evtLat;
        val.lon = evtLon;
        val.azimuth = distAz.getAz();
        val.backAzimuth = distAz.getBaz();
        val.flattening = flattening;
        val.geodetic = true;
        return val;
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) throws SlownessModelException, NoSuchLayerException {
        List<Arrival> phaseArrivals = phase.calcTime(getDegrees(phase.getTauModel().getRadiusOfEarth()));
        for (Arrival arrival : phaseArrivals) {
            arrival.setShootable(this);
        }
        return phaseArrivals;
    }

    public double getDegrees(double radius) {
        if (degrees != null) {
            return degrees;
        }
        if (radians != null) {
            return radians*SphericalCoords.rtod;
        }
        return kilometers/DistAz.kmPerDeg(radius);
    }

    public double getRadians(double radius) {
        if (radians != null) {
            return radians;
        }
        if (kilometers != null) {
            return kilometers/radius;
        }
        return degrees*SphericalCoords.dtor;
    }

    protected Double radians = null;
    protected Double degrees = null;
    protected Double kilometers = null;
    protected Double searchDist = null;

    public boolean isLatLong() {
        return lat != null;
    }

    protected Double lat = null;
    protected Double lon = null;
    protected Double azimuth = null;
    protected Double backAzimuth = null;
    protected boolean geodetic = false;
    protected Double flattening = null;

    protected DistanceArgs args = null;
}
