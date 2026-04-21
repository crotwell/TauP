package edu.sc.seis.TauP;

import net.sf.geographiclib.Geodesic;

/**
 * Abstract calculatable ray where initial departure ray parameter from source is known, but receiver is not.
 */
public abstract class ShootableRay extends RayCalculateable {

    public ShootableRay(DistanceCalc distCalc) {
        super(distCalc);
    }

    @Override
    public LatLonable getLatLonable() {
        if (isLatLonable()) {
            if (evtLatLon != null) {
                return new EventAzimuth(evtLatLon, azimuth, distCalc);
            } else {
                return new StationBackAzimuth(staLatLon, backAzimuth, distCalc);
            }
        }
        return null;
    }

    @Override
    public boolean isLatLonable() {
        return (evtLatLon != null && azimuth != null) || (staLatLon != null && backAzimuth != null);
    }
}
