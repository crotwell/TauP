package edu.sc.seis.TauP;

import net.sf.geographiclib.Geodesic;

import java.util.List;

/**
 * Calculatable ray based on ray parameter departing the source in seconds per kilometer.
 */
public class RayParamKmRay extends ShootableRay {

    public RayParamKmRay(Double rpSecKm, GeoDistType geoDistType, Geodesic geodesic) {
        super(geoDistType, geodesic);
        this.rpSecKm = rpSecKm;
        setDescription(rpSecKm+" s/km");
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) throws TauPException {
        RayParamRay rpRay = RayParamRay.ofRayParamSRadian(getRayParamSKm()*phase.getTauModel().getRadiusOfEarth(),
                getGeoDistType(), getGeodesic());
        List<Arrival> arrivals = rpRay.calculate(phase);
        for (Arrival a : arrivals) {
            a.setSearchValue(this);
        }
        return arrivals;
    }

    /**
     * ray param in s/km
     *
     * @return ray param in seconds per kilometer
     */
    public Double getRayParamSKm() {
        return rpSecKm;
    }

    private final double rpSecKm;
}
