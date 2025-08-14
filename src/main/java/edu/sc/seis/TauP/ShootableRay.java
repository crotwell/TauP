package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;

/**
 * Abstract calculatable ray where initial departure ray parameter from source is known, but receiver is not.
 */
public abstract class ShootableRay extends RayCalculateable {

    public void withEventAzimuth(LatLonLocatable evt, double azimuth) {
        super.withEventAzimuth(evt, azimuth);
        this.staLatLon = null;
        this.backAzimuth = null;
    }

    public void withStationBackAzimuth(LatLonLocatable sta, double backAzimuth) {
        super.withStationBackAzimuth(sta, backAzimuth);
        this.evtLatLon = null;
        this.azimuth = null;
    }

    @Override
    public LatLonable getLatLonable() {
        if (isLatLonable()) {
            if (evtLatLon != null) {
                return new EventAzimuth(evtLatLon, azimuth);
            } else {
                return new StationBackAzimuth(staLatLon, backAzimuth);
            }
        }
        return null;
    }

    @Override
    public boolean isLatLonable() {
        return (evtLatLon != null && azimuth != null) || (staLatLon != null && backAzimuth != null);
    }
}
