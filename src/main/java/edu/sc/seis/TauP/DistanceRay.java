package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.DistanceArgs;
import edu.sc.seis.seisFile.Location;

import java.util.List;

public class DistanceRay extends RayCalculateable {

    DistanceRay() {}
    public DistanceRay(DistanceRay dr) {
          radians = dr.radians;
          degrees = dr.degrees;
          kilometers = dr.kilometers;
          staLatLon = dr.staLatLon;
          evtLatLon = dr.evtLatLon;
          lat = dr.lat;
          lon = dr.lon;
          azimuth = dr.azimuth;
          backAzimuth = dr.backAzimuth;
          geodetic = dr.geodetic;
          flattening = dr.flattening;

          args = dr.args;
    }
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

    public static DistanceRay ofStationEvent(Location sta, Location evt) {
        DistanceRay val = DistanceRay.ofDegrees(SphericalCoords.distance(evt, sta));
        val.evtLatLon = evt;
        val.staLatLon = sta;
        val.azimuth = SphericalCoords.azimuth(evt, sta);
        val.backAzimuth = SphericalCoords.azimuth(sta, evt);
        return val;
    }

    public void withEventAzimuth(Location evt, double azimuth) {
        this.evtLatLon = evt;
        this.azimuth = azimuth;
        this.backAzimuth = null;
    }

    public void withStationBackAzimuth(Location sta, double backazimuth) {
        this.staLatLon = sta;
        this.azimuth = null;
        this.backAzimuth = backazimuth;
    }

    public static DistanceRay ofGeodeticStationEvent(Location sta, Location evt, double flattening) {
        DistAz distAz = new DistAz(sta, evt, flattening);
        DistanceRay val = ofDegrees(distAz.getDelta());
        val.staLatLon = sta;
        val.evtLatLon = evt;
        val.azimuth = distAz.getAz();
        val.backAzimuth = distAz.getBaz();
        val.flattening = flattening;
        val.geodetic = true;
        return val;
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) throws SlownessModelException, NoSuchLayerException {
        List<Arrival> phaseArrivals = phase.calcTime(this);
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

    @Override
    public boolean isLatLonable() {
        return (staLatLon != null && evtLatLon != null) || (staLatLon != null && backAzimuth != null) || (evtLatLon != null && azimuth != null);
    }

    @Override
    public LatLonable getLatLonable() {
        if (staLatLon != null && evtLatLon != null) {
            return new EventStation(evtLatLon, staLatLon);
        } else if (staLatLon != null && backAzimuth != null) {
            return new StationBackAzimuth(staLatLon, backAzimuth);
        } else if (evtLatLon != null && azimuth != null) {
            return new EventAzimuth(evtLatLon, azimuth);
        }
        return null;
    }

    protected Double radians = null;
    protected Double degrees = null;
    protected Double kilometers = null;
    protected Location staLatLon = null;
    protected Location evtLatLon = null;
    protected Double lat = null;
    protected Double lon = null;
    protected Double azimuth = null;
    protected Double backAzimuth = null;
    protected boolean geodetic = false;
    protected Double flattening = null;

    protected DistanceArgs args = null;
}
