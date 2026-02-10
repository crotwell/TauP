package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;

public class Daz {
    protected String description;
    protected DistanceRay ray;

    public Daz(DistanceRay ray) {
        this.ray = ray;
        this.description = ray.getDescription();
    }

    public double getDegrees() {
        return ray.getDegrees();
    }

    public LatLonLocatable getSource() {
        return ray.getSource();
    }

    public LatLonLocatable getReceiver() {
        return ray.getReceiver();
    }

    public Double getAzimuth() {
        return ray.getAzimuth();
    }

    public Double getBackAzimuth() {
        return ray.getBackAzimuth();
    }

    public boolean isGeodetic() {
        return ray.isGeodetic();
    }

    public Double getInvFlattening() {
        if (ray.getInvFlattening() != null) {
            return ray.getInvFlattening();
        } else if (ray.getGeodesic() != null) {
            return 1.0/ray.getGeodesic().Flattening();
        }
        return null;
    }
    public Double getEquitorialRadius() {
        if (ray.getGeodesic() != null) {
            return ray.getGeodesic().EquatorialRadius();
        }
        return null;
    }

    public String getDescription() {
        return description;
    }

    public Double getKilometers() {
        return ray.getKilometers();
    }
    public Double getKm() {
        return ray.getKilometers();
    }

    public boolean hasSource() {
        return ray.hasSource();
    }

    public boolean hasReceiver() {
        return ray.hasReceiver();
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
