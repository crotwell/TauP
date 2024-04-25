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


    protected Location staLatLon = null;
    protected Location evtLatLon = null;
    protected Double azimuth = null;
    protected Double backAzimuth = null;
    protected boolean geodetic = false;
    protected Double flattening = null;

}
