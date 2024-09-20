package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;

import java.util.List;

public abstract class RayCalculateable {


    public abstract List<Arrival> calculate(SeismicPhase phase) throws SlownessModelException, NoSuchLayerException;

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

    public boolean hasReceiverDepth() {
        return staLatLon != null && staLatLon.hasDepth();
    }
    public Double getReceiverDepth() {
        return staLatLon != null ? staLatLon.getDepthKm() : null;
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


    protected Location staLatLon = null;
    protected Location evtLatLon = null;
    protected Double azimuth = null;
    protected Double backAzimuth = null;
    protected boolean geodetic = false;
    protected Double flattening = null;
    protected String description = null;

}
