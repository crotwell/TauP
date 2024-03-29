package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;

import java.util.ArrayList;
import java.util.List;

public class RayParamRay extends RayCalculateable {

    public RayParamRay(RayParamRay rpr) {
        this.rayParam = rpr.getRayParam();
    }

    public RayParamRay(Double rayParam) {
        this.rayParam = rayParam;
    }

    public static RayParamRay ofRayParam(Double d) {
        RayParamRay ray = new RayParamRay(d);
        return ray;
    }
    public static RayParamRay ofRayParamSDegree(Double d) {
        RayParamRay ray =  RayParamRay.ofRayParam(d/SphericalCoords.dtor);
        return ray;
    }

    public void withEventAzimuth(Location evt, double azimuth) {
        this.evtLatLon = evt;
        this.azimuth = azimuth;
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) throws SlownessModelException, NoSuchLayerException {
        List<Arrival> arrivals = new ArrayList<>();
        if (phase.getMinRayParam() <= rayParam && rayParam <= phase.getMaxRayParam()) {
            Arrival phaseArrival = phase.shootRay(rayParam);
            phaseArrival.setShootable(this);
            arrivals.add(phaseArrival);
        }
        return arrivals;
    }

    /**
     * ray param in s/radian
     *
     * @return
     */
    public Double getRayParam() {
        return rayParam;
    }


    @Override
    public LatLonable getLatLonable() {
        if (isLatLonable()) {
            return new EventAzimuth(evtLatLon, azimuth);
        }
        return null;
    }

    @Override
    public boolean isLatLonable() {
        return evtLatLon != null && azimuth != null;
    }

    /**
     * Ray parameter in s/radian.
     */
    Double rayParam;
    Location evtLatLon;
    Double azimuth;

}
