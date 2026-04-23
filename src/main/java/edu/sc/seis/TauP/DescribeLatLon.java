package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.LatLonSimple;
import edu.sc.seis.seisFile.Location;

public class DescribeLatLon {

    public static String describeLatLon(LatLonLocatable latLonLocatable) {
        String desc = "";
        if (latLonLocatable != null) {
            Location loc = latLonLocatable.asLocation();
            if (latLonLocatable instanceof LatLonSimple) {
                desc = Location.formatLatLon(loc.getLatitude()).trim() + "/" + Location.formatLatLon(loc.getLongitude()).trim();
                if (loc.hasDepth() && loc.getDepthMeter() != 0.0) {
                    if (loc.getDepthMeter() > 4999) {
                        desc += " " + Location.formatLatLon(loc.getDepthKm()).trim() + " km";
                    } else {
                        desc += " " + Location.formatLatLon(loc.getDepthMeter()).trim() + " m";
                    }
                }
            } else {
                desc = latLonLocatable.getLocationDescription();
            }
        }
        return desc;
    }
}
