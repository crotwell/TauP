package edu.sc.seis.TauP;

import java.util.List;

public class BeachBall {

    public BeachBall(FaultPlane faultPlane,
                     BeachballType bbType,
                     HemisphereType hemisphereType,
                     List<Arrival> arrivalList,
                     List<RadiationAmplitude> radiationAmplitudeList) {
        this.faultPlane = faultPlane;
        this.bbType = bbType;
        this.hemisphereType = hemisphereType;
        this.arrivalList = arrivalList;
        this.radiationAmplitudeList = radiationAmplitudeList;
    }

    public FaultPlane getFaultPlane() {
        return faultPlane;
    }

    public BeachballType getBbType() {
        return bbType;
    }

    public HemisphereType getHemisphereType() {
        return hemisphereType;
    }

    public List<Arrival> getArrivals() {
        return arrivalList;
    }

    public List<RadiationAmplitude> getRadiationAmplitudeList() {
        return radiationAmplitudeList;
    }

    FaultPlane faultPlane;
    BeachballType bbType;
    HemisphereType hemisphereType;
    List<Arrival> arrivalList;
    List<RadiationAmplitude> radiationAmplitudeList;
}
