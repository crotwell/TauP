package edu.sc.seis.TauP;

import net.sf.geographiclib.Geodesic;

/**
 * Abstract calculatable ray where initial departure ray parameter from source is known, but receiver is not.
 */
public abstract class ShootableRay extends RayCalculateable {

    public ShootableRay(GeoDistType geoDistType, Geodesic geodesic) {
        super(geoDistType, geodesic);
    }

    @Override
    public LatLonable getLatLonable() {
        if (isLatLonable()) {
            if (evtLatLon != null) {
                return new EventAzimuth(evtLatLon, azimuth, geoDistType, geodesic);
            } else {
                return new StationBackAzimuth(staLatLon, backAzimuth, geoDistType, geodesic);
            }
        }
        return null;
    }

    @Override
    public boolean isLatLonable() {
        return (evtLatLon != null && azimuth != null) || (staLatLon != null && backAzimuth != null);
    }
}
