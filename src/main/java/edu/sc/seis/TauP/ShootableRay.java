package edu.sc.seis.TauP;

/**
 * Abstract calculatable ray where initial departure ray parameter from source is known, but receiver is not.
 */
public abstract class ShootableRay extends RayCalculateable {

    @Override
    public LatLonable getLatLonable() {
        if (isLatLonable()) {
            if (evtLatLon != null) {
                return new EventAzimuth(evtLatLon, azimuth, geodesic);
            } else {
                return new StationBackAzimuth(staLatLon, backAzimuth, geodesic);
            }
        }
        return null;
    }

    @Override
    public boolean isLatLonable() {
        return (evtLatLon != null && azimuth != null) || (staLatLon != null && backAzimuth != null);
    }
}
