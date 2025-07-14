package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;

public class Daz {
    protected double deg;
    protected double km;
    protected Location staLatLon = null;
    protected Location evtLatLon = null;
    protected double azimuth;
    protected double backAzimuth;
    protected boolean geodetic = false;
    protected Double invFlattening = null;
    protected String description;

    public Daz(DistanceAngleRay ray, double radiusOfEarth) {
        this(ray.getDegrees(radiusOfEarth),
                ray.getKilometers(radiusOfEarth),
                ray.getAzimuth(),
                ray.getBackAzimuth(),
                ray.getDescription());
        if (ray.hasReceiver()) {
            this.evtLatLon = ray.getReceiver();
        }
        if (ray.hasSource()) {
            this.staLatLon = ray.getSource();
        }
        if (ray.isGeodetic()) {
            this.geodetic = true;
            this.invFlattening = ray.getInvFlattening();
        }
    }
    public Daz(double deg, double km, double az, double baz, String description) {
        this.deg = deg;
        this.km = km;
        this.azimuth = az;
        this.backAzimuth = baz;
        this.description = description;
    }

    public double getDegrees() {
        return deg;
    }

    public Location getStaLatLon() {
        return staLatLon;
    }
    public Location getSource() {
        return getStaLatLon();
    }

    public Location getEvtLatLon() {
        return evtLatLon;
    }
    public Location getReceiver() {
        return getEvtLatLon();
    }

    public Double getAzimuth() {
        return azimuth;
    }

    public Double getBackAzimuth() {
        return backAzimuth;
    }

    public boolean isGeodetic() {
        return geodetic;
    }

    public Double getInvFlattening() {
        return invFlattening;
    }

    public String getDescription() {
        return description;
    }

    public Double getKilometers() {
        return km;
    }
    public Double getKm() {
        return km;
    }

    public boolean hasSource() {
        return staLatLon != null;
    }

    public boolean hasReceiver() {
        return evtLatLon != null;
    }

    public double getNormalizedAzimuth() {
        return RayCalculateable.normalizAzimuth(getAzimuth());
    }

    public double getNormalizedBackAzimuth() {
        return RayCalculateable.normalizAzimuth(getBackAzimuth());
    }

    public boolean hasDescription() {
        return this.description != null && !this.description.isEmpty();
    }
}
