package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;
import edu.sc.seis.seisFile.Location;

import java.util.List;

/**
 * Base class for information used to calculate a particular path for a seismic phase. For example distance from
 * source to receiver, or takeoff angle from source.
 */
public abstract class RayCalculateable {


    public abstract List<Arrival> calculate(SeismicPhase phase) throws TauPException;

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

    public abstract boolean isLatLonable();
    public abstract LatLonable getLatLonable();

    public boolean hasSourceDepth() {
        return evtLatLon != null && evtLatLon.hasDepth();
    }
    public Double getSourceDepth() {
        return evtLatLon != null ? evtLatLon.getDepthKm() : null;
    }
    public boolean hasSource() {
        return evtLatLon != null;
    }
    public Location getSource() {
        return evtLatLon;
    }

    public boolean hasReceiverDepth() {
        return staLatLon != null && staLatLon.hasDepth();
    }
    public Double getReceiverDepth() {
        return staLatLon != null ? staLatLon.getDepthKm() : null;
    }

    public boolean hasReceiver() {
        return staLatLon != null;
    }
    public Location getReceiver() {
        return staLatLon;
    }
    public boolean hasAzimuth() {
        return azimuth != null || (this.staLatLon!=null && this.evtLatLon!=null);
    }

    public boolean isGeodetic() { return geodetic;}

    /**
     * Returns azimuth, if available, in the range -180&lt;baz&lt;=180.
     * @return azimuth
     */
    public Double getNormalizedAzimuth() {
        Double az = getAzimuth();
        if (az != null ) {
            az = az % 360;
            if (az > 180) {
                az = az - 360;
            }
        }
        return az;
    }

    /**
     * Gets azimuth if available, null otherwise.
     * @return azimuth
     */
    public Double getAzimuth() {
        if (azimuth != null ) {
            return azimuth;
        } else if (this.staLatLon!=null && this.backAzimuth!=null) {
            if (getLatLonable().isGeodetic()) {
                return null;
            } else {
                return null;
            }
        } else if (this.evtLatLon!=null && this.staLatLon!=null) {
            if (getLatLonable().isGeodetic()) {
                DistAz distAz = new DistAz(this.staLatLon, this.evtLatLon, invFlattening);
                return distAz.getAz();
            } else {
                return SphericalCoords.azimuth(evtLatLon, staLatLon);
            }
        } else {
            throw new RuntimeException("should not happen");
        }
    }
    public void setAzimuth(Double azimuth) {
        this.azimuth = azimuth;
    }


    public boolean hasBackAzimuth() {
        return backAzimuth != null || (this.evtLatLon!=null && this.staLatLon!=null);
    }
    /**
     * Returns back azimuth, if available, in the range -180&lt;baz&lt;=180.
     * @return back azimuth
     */
    public Double getNormalizedBackAzimuth() {
        Double az = getBackAzimuth();
        if (az != null ) {
            az = az % 360;
            if (az > 180) {
                az = az - 360;
            }
        }
        return az;
    }
    /**
     * Gets azimuth if available, null otherwise.
     * @return azimuth
     */
    public Double getBackAzimuth() {
        if (backAzimuth != null ) {
            return backAzimuth;
        } else if (this.staLatLon!=null && this.azimuth!=null) {
            // shoudl be able to calc
            if (getLatLonable().isGeodetic()) {
                return null;
            } else {
                return null;
            }
        } else if (this.evtLatLon!=null && this.staLatLon!=null) {
            if (getLatLonable().isGeodetic()) {
                DistAz distAz = new DistAz(this.staLatLon, this.evtLatLon, invFlattening);
                return distAz.getBaz();
            } else {
                return SphericalCoords.azimuth(staLatLon, evtLatLon);
            }
        } else {
            throw new RuntimeException("should not happen");
        }
    }

    public void setBackAzimuth(Double backAzimuth) {
        this.backAzimuth = backAzimuth;
    }

    public boolean hasSourceArgs() {
        return sourceArgs != null;
    }
    public void setSourceArgs(SeismicSourceArgs sourceArgs) {
        this.sourceArgs = sourceArgs;
    }
    public SeismicSourceArgs getSourceArgs() {
        return sourceArgs;
    }

    public boolean hasDescription() {
        return description != null && ! description.isEmpty();
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String desc) {
        this.description = desc;
    }

    public String toString() {
        if (hasDescription()) {
            return getDescription();
        }
        return "";
    }


    protected Location staLatLon = null;
    protected Location evtLatLon = null;
    protected Double azimuth = null;
    protected Double backAzimuth = null;
    protected boolean geodetic = false;
    protected Double invFlattening = null;
    protected String description = null;

    /**
     * Optional source args for amp calculations.
     */
    protected SeismicSourceArgs sourceArgs = null;

}
