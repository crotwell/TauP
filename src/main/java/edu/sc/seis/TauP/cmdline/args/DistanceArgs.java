package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.*;
import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.LatLonSimple;
import net.sf.geographiclib.Geodesic;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DistanceArgs {

    private ModelArgs modelArgs;

    public DistanceArgs(ModelArgs modelArgs) {
        this.modelArgs = modelArgs;
    }

    public List<DistanceRay> getDistances(List<DistanceCalc> distCalcList) throws TauPException {
        assert !distCalcList.isEmpty() : "No geodesics given";
        List<DistanceRay> simpleDistanceList = new ArrayList<>();
        for (Double d : distArgs.degreesList) {
            for (DistanceCalc distCalc : distCalcList) {
                simpleDistanceList.add(DistanceRay.ofDegrees(d, distCalc));
            }
        }
        for (Double d : distArgs.exactDegreesList) {
            for (DistanceCalc distCalc : distCalcList) {
                simpleDistanceList.add(DistanceRay.ofExactDegrees(d, distCalc));
            }
        }
        if (!distArgs.degreeRange.isEmpty()) {
            for (Double d : createListFromRangeDeg(distArgs.degreeRange)) {
                for (DistanceCalc distCalc : distCalcList) {
                    simpleDistanceList.add(DistanceRay.ofDegrees(d, distCalc));
                }
            }
        }
        for (Double d : distArgs.distKilometersList) {
            for (DistanceCalc distCalc : distCalcList) {
                simpleDistanceList.add(DistanceRay.ofKilometers(d, distCalc));
            }
        }
        for (Double d : distArgs.exactDistKilometersList) {
            for (DistanceCalc distCalc : distCalcList) {
                simpleDistanceList.add(DistanceRay.ofExactKilometers(d, distCalc));
            }
        }

        if (!distArgs.kilometerRange.isEmpty()) {
            for (Double d : createListFromRangeKm(distArgs.kilometerRange)) {
                for (DistanceCalc distCalc : distCalcList) {
                    simpleDistanceList.add(DistanceRay.ofKilometers(d, distCalc));
                }
            }
        }
        if (!distArgs.exactDegreeRange.isEmpty()) {
            for (Double d : createListFromRangeDeg(distArgs.exactDegreeRange)) {
                for (DistanceCalc distCalc : distCalcList) {
                    simpleDistanceList.add(DistanceRay.ofExactDegrees(d, distCalc));
                }
            }
        }
        if (!distArgs.exactKilometerRange.isEmpty()) {
            for (Double d : createListFromRangeKm(distArgs.exactKilometerRange)) {
                for (DistanceCalc distCalc : distCalcList) {
                    simpleDistanceList.add(DistanceRay.ofExactKilometers(d, distCalc));
                }
            }
        }
        if (getAzimuth() != null) {
            for (DistanceRay dr : simpleDistanceList) {
                if (!dr.hasAzimuth()) {
                    dr.setAzimuth(getAzimuth());
                }
            }
        } else if (getBackAzimuth() != null) {
            for (DistanceRay dr : simpleDistanceList) {
                if (!dr.hasBackAzimuth()) {
                    dr.setBackAzimuth(getBackAzimuth());
                }
            }
        }

        boolean hasEvent = hasEventLatLon();
        List<LatLonLocatable> quakes = new ArrayList<>();
        if (hasEvent) {
            quakes = getEventLatLon();
            hasEvent = ! quakes.isEmpty();
        }

        boolean hasStation = hasStationLatLon();
        List<LatLonLocatable> stationList = new ArrayList<>();
        if (hasStation) {
            stationList = getStationLatLon();
        }
        hasStation = ! stationList.isEmpty();


        List<DistanceRay> out = new ArrayList<>();
        if (hasEvent && getAzimuth() != null && !hasStation) {
            List<DistanceRay> evtOut = new ArrayList<>();
            for (DistanceRay dr : simpleDistanceList) {
                if (dr.isLatLonable()) {
                    // already enough info, so just add
                    evtOut.add(dr);
                } else {
                    for (LatLonLocatable evtLoc : quakes) {
                        DistanceRay evtDr = DistanceRay.duplicate(dr);
                        evtDr.withEventAzimuth(evtLoc, getAzimuth());
                        String sourceDesc = evtLoc.getLocationDescription();
                        if (sourceDesc.endsWith(" 0.00 m")) {
                            sourceDesc = sourceDesc.substring(0, sourceDesc.length()-7);
                        }
                        evtDr.setDescription(sourceDesc + " to az " + Outputs.formatDistance(getAzimuth()).trim());
                        evtDr.insertSeismicSource(evtLoc);
                        evtOut.add(evtDr);

                    }
                }
            }
            out.addAll( evtOut);
        } else if ( ! hasEvent && hasStation && getBackAzimuth() != null) {
            List<DistanceRay> staOut = new ArrayList<>();
            for (DistanceRay dr : simpleDistanceList) {
                if (dr.isLatLonable()) {
                    // already enough info, so just add
                    staOut.add(dr);
                } else {
                    for (LatLonLocatable staLoc : stationList) {
                        DistanceRay staDr = DistanceRay.duplicate(dr);
                        staDr.withStationBackAzimuth(staLoc, getBackAzimuth());
                        String receiverDesc = staLoc.getLocationDescription();
                        if (receiverDesc.endsWith(" 0.00 m")) {
                            receiverDesc = receiverDesc.substring(0, receiverDesc.length()-7);
                        }
                        staDr.setDescription("baz " + Outputs.formatDistance(getBackAzimuth()) + " from " + receiverDesc);
                        staOut.add(staDr);
                    }
                }
            }
            out.addAll(staOut);
        } else if (hasEvent && hasStation) {
            // add simple distances
            out.addAll(simpleDistanceList);
            // now add evt-station pairs, already have latlonable
            for (LatLonLocatable evtLoc : quakes) {
                for (LatLonLocatable staLoc : stationList) {
                    for (DistanceCalc distCalc : distCalcList) {
                        DistanceRay dr = DistanceRay.ofEventStation(evtLoc, staLoc, distCalc);
                        String sourceDesc = dr.getSource().getLocationDescription();
                        if (sourceDesc.endsWith(" 0.00 m")) {
                            sourceDesc = sourceDesc.substring(0, sourceDesc.length()-7);
                        }
                        String receiverDesc = dr.getReceiver().getLocationDescription();
                        if (receiverDesc.endsWith(" 0.00 m")) {
                            receiverDesc = receiverDesc.substring(0, receiverDesc.length()-7);
                        }
                        dr.setDescription(sourceDesc+" to "+receiverDesc);
                        out.add(dr);
                    }
                }
            }
        } else {
            // no event or station, so just add simple distances
            out.addAll(simpleDistanceList);
        }
        return out;
    }

    public List<RayParamKmRay> getRayParamKmRays(List<DistanceCalc> distCalcList) throws TauPException {
        List<RayParamKmRay> rpList = new ArrayList<>();
        for (Double d : distArgs.shootKmRaypList) {
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                for (LatLonLocatable evt : getEventLatLon()) {
                    for (DistanceCalc distCalc : distCalcList) {
                        RayParamKmRay evtDr = new RayParamKmRay(d, distCalc);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                for (LatLonLocatable sta : getStationLatLon()) {
                    for (DistanceCalc distCalc : distCalcList) {
                        RayParamKmRay staDr = new RayParamKmRay(d, distCalc);
                        staDr.withStationBackAzimuth(sta, getBackAzimuth());
                        rpList.add(staDr);
                    }
                }
            } else {
                for (DistanceCalc distCalc : distCalcList) {
                    rpList.add(new RayParamKmRay(d, distCalc));
                }
            }
        }
        return rpList;
    }

    public List<TimeRay> getTimeRays(List<DistanceCalc> distCalcList) throws TauPException {
        List<TimeRay> rpList = new ArrayList<>();

        for (Double d : distArgs.timeList) {
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                for (LatLonLocatable evt : getEventLatLon()) {
                    for (DistanceCalc distCalc : distCalcList) {
                        TimeRay evtDr = new TimeRay(d, distCalc);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                for (LatLonLocatable sta : getStationLatLon()) {
                    for (DistanceCalc distCalc : distCalcList) {
                        TimeRay staDr = new TimeRay(d, distCalc);
                        staDr.withStationBackAzimuth(sta, getBackAzimuth());
                        rpList.add(staDr);
                    }
                }
            } else {
                for (DistanceCalc distCalc : distCalcList) {
                    rpList.add(new TimeRay(d, distCalc));
                }
            }
        }
        return rpList;
    }

    public List<RayParamRay> getRayParamDegRays(List<DistanceCalc> distCalcList) throws TauPException {
        List<RayParamRay> rpList = new ArrayList<>();
        for (Double d : distArgs.shootRaypList) {
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                for (LatLonLocatable evt : getEventLatLon()) {
                    for (DistanceCalc distCalc : distCalcList) {
                        RayParamRay evtDr = RayParamRay.ofRayParamSDegree(d, distCalc);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                for (LatLonLocatable sta : getStationLatLon()) {
                    for (DistanceCalc distCalc : distCalcList) {
                        RayParamRay staDr = RayParamRay.ofRayParamSDegree(d, distCalc);
                        staDr.withStationBackAzimuth(sta, getBackAzimuth());
                        rpList.add(staDr);
                    }
                }
            } else {
                for (DistanceCalc distCalc : distCalcList) {
                    rpList.add(RayParamRay.ofRayParamSDegree(d, distCalc));
                }
            }
        }
        return rpList;
    }

    public List<RayParamRay> getRayParamRadianRays(List<DistanceCalc> distCalcList) throws TauPException {
        List<RayParamRay> rpList = new ArrayList<>();

        for (Double d : distArgs.shootRadianRaypList) {
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                for (LatLonLocatable evt : getEventLatLon()) {
                    for (DistanceCalc distCalc : distCalcList) {
                        RayParamRay evtDr = RayParamRay.ofRayParamSRadian(d, distCalc);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                for (LatLonLocatable sta : getStationLatLon()) {
                    for (DistanceCalc distCalc : distCalcList) {
                        RayParamRay staDr = RayParamRay.ofRayParamSRadian(d, distCalc);
                        staDr.withStationBackAzimuth(sta, getBackAzimuth());
                        rpList.add(staDr);
                    }
                }
            } else {
                for (DistanceCalc distCalc : distCalcList) {
                    rpList.add(RayParamRay.ofRayParamSRadian(d, distCalc));
                }
            }
        }
        return rpList;
    }


    public List<RayParamIndexRay> getRayParamIndexRays(List<DistanceCalc> distCalcList) throws TauPException {
        List<RayParamIndexRay> rpList = new ArrayList<>();

        for (Integer d : distArgs.shootIndexRaypList) {
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                for (LatLonLocatable evt : getEventLatLon()) {
                    for (DistanceCalc distCalc : distCalcList) {
                        RayParamIndexRay evtDr = new RayParamIndexRay(d, distCalc);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                for (LatLonLocatable sta : getStationLatLon()) {
                    for (DistanceCalc distCalc : distCalcList) {
                        RayParamIndexRay staDr = new RayParamIndexRay(d, distCalc);
                        staDr.withStationBackAzimuth(sta, getBackAzimuth());
                        rpList.add(staDr);
                    }
                }
            } else {
                for (DistanceCalc distCalc : distCalcList) {
                    rpList.add(new RayParamIndexRay(d, distCalc));
                }
            }
        }
        return rpList;
    }

    public List<TakeoffAngleRay> getTakeoffAngleRays(List<DistanceCalc> distCalcList) throws TauPException {
        List<TakeoffAngleRay> rpList = new ArrayList<>();
        List<Double> takeoffInputList = new ArrayList<>();
        takeoffInputList.addAll(distArgs.takeoffAngle);
        if (!distArgs.takeoffRange.isEmpty()) {
            takeoffInputList.addAll(createListFromRangeDeg(distArgs.takeoffRange));
        }

        for (Double d : takeoffInputList) {
            if (d < 0 || d > 180) {
                throw new ArgumentValidationException("Takeoff angle should be between 0 and 180 degrees: "+d);
            }
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                for (LatLonLocatable evt : getEventLatLon()) {
                    for (DistanceCalc distCalc : distCalcList) {
                        TakeoffAngleRay evtDr = new TakeoffAngleRay(d, distCalc);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                for (LatLonLocatable sta : getStationLatLon()) {
                    for (DistanceCalc distCalc : distCalcList) {
                        TakeoffAngleRay staDr = new TakeoffAngleRay(d, distCalc);
                        staDr.withStationBackAzimuth(sta, getBackAzimuth());
                        rpList.add(staDr);
                    }
                }
            } else {
                for (DistanceCalc distCalc : distCalcList) {
                    rpList.add(TakeoffAngleRay.ofTakeoffAngle(d, distCalc));
                }
            }
        }
        return rpList;
    }

    public List<IncidentAngleRay> getIncidentAngleRays(List<DistanceCalc> distCalcList) throws TauPException {
        List<IncidentAngleRay> rpList = new ArrayList<>();
        List<Double> incidentAngleInputList = new ArrayList<>();
        incidentAngleInputList.addAll(distArgs.incidentAngle);
        if (!distArgs.incidentRange.isEmpty()) {
            incidentAngleInputList.addAll(createListFromRangeDeg(distArgs.incidentRange));
        }

        for (Double d : incidentAngleInputList) {
            if (d < 0 || d > 180) {
                throw new ArgumentValidationException("Incident angle should be between 0 and 180 degrees: "+d);
            }
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                for (LatLonLocatable evt : getEventLatLon()) {
                    for (DistanceCalc distCalc : distCalcList) {
                        IncidentAngleRay evtDr = new IncidentAngleRay(d, distCalc);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                for (LatLonLocatable sta : getStationLatLon()) {
                    for (DistanceCalc distCalc : distCalcList) {
                        IncidentAngleRay staDr = new IncidentAngleRay(d, distCalc);
                        staDr.withStationBackAzimuth(sta, getBackAzimuth());
                        rpList.add(staDr);
                    }
                }
            } else {
                for (DistanceCalc distCalc : distCalcList) {
                    rpList.add(IncidentAngleRay.ofIncidentAngle(d, distCalc));
                }
            }
        }
        return rpList;
    }

    public static List<Double> createListFromRangeDeg(List<Double> minMaxStep) {
        double step = 10;
        double start = 0;
        double stop = 180;
        return createListFromRange(minMaxStep, start, stop, step);
    }

    public static List<Double> createListFromRangeKm(List<Double> minMaxStep) {
        double step = 100;
        double start = 0;
        double stop = 1000;
        return createListFromRange(minMaxStep, start, stop, step);
    }

    public static List<Double> createListFromRange(List<Double> minMaxStep, double defaultStart, double defaultStop, double defaultStep) {
        double step = defaultStep;
        double start = defaultStart;
        double stop = defaultStop;
        switch (minMaxStep.size()) {
            case 3:
                step = minMaxStep.get(2);
            case 2:
                start = minMaxStep.get(0);
                stop = minMaxStep.get(1);
                break;
            case 1:
                step = minMaxStep.get(0);
                break;
            case 0:
                break;
            default:
                throw new ArgumentValidationException("range length should be 1-3 but was "+minMaxStep.size());
        }
        if (step == 0.0) {
            throw new ArgumentValidationException("Step cannot be zero");
        }
        if ((step<0 && (start<stop)) || (step>0 && (start>stop))){
            double tmp = start;
            start = stop;
            stop = tmp;
        }
        double d = start;
        List<Double> out = new ArrayList<>();
        while (d<= stop) {
            out.add(d);
            d+=step;
        }
        return out;
    }

    /**
     * Creates ray calculatables for all distances, times, ray parameters, etc.
     * @param sourceArgs initialize seismic source information
     * @return
     * @throws TauPException
     */
    public List<RayCalculateable> getRayCalculatables(SeismicSourceArgs sourceArgs) throws TauPException {
        return getRayCalculatables(createDistanceCalcs(), sourceArgs);
    }
    public List<DistanceCalc> createDistanceCalcs() throws TauModelException {
        return geodeticArgs.createDistanceCalcs(modelArgs.getTauModel().getVelocityModel());
    }
    public Map<GeoDistType, Geodesic> createGeodesicMap() throws TauModelException {
        return geodeticArgs.createGeodesics(modelArgs.getTauModel().getVelocityModel());
    }

    public List<RayCalculateable> getRayCalculatables(List<DistanceCalc> distCalcList, SeismicSourceArgs sourceArgs) throws TauPException {
        List<RayCalculateable> simpleRays = new ArrayList<>();
        simpleRays.addAll(getDistances(distCalcList));
        simpleRays.addAll(getTimeRays(distCalcList));
        simpleRays.addAll(getRayParamDegRays(distCalcList));
        simpleRays.addAll(getRayParamKmRays(distCalcList));
        simpleRays.addAll(getRayParamRadianRays(distCalcList));
        simpleRays.addAll(getTakeoffAngleRays(distCalcList));
        simpleRays.addAll(getIncidentAngleRays(distCalcList));
        simpleRays.addAll(getRayParamIndexRays(distCalcList));

        if (hasAzimuth()) {
            for (RayCalculateable rc : simpleRays) {
                if (!rc.hasAzimuth()) {
                    rc.setAzimuth(getAzimuth());
                }
            }
        }
        if (hasBackAzimuth()) {
            for (RayCalculateable rc : simpleRays) {
                if (!rc.hasBackAzimuth()) {
                    rc.setBackAzimuth(getBackAzimuth());
                }
            }
        }

        if (sourceArgs != null) {
            if (sourceArgs.hasStrikeDipRake()) {
                for (RayCalculateable rc : simpleRays) {
                    if (!rc.hasFaultPlane()) {
                        float Mw = rc.hasMw() ? rc.getMw() : sourceArgs.getMw();
                        rc.setSeismicSource(new SeismicSource(Mw, sourceArgs.getFaultPlane()));
                    }
                }
            } else {
                for (RayCalculateable rc : simpleRays) {
                    if (!rc.hasMw()) {
                        rc.setSeismicSource(new SeismicSource(sourceArgs.getMw()));
                    }
                }
            }
        }
        return simpleRays;
    }

    public Double getAzimuth() {
        return geodeticArgs.azimuth;
    }
    public void setAzimuth(double val) { geodeticArgs.azimuth = val;}
    public boolean hasAzimuth() {return geodeticArgs.hasAzimuth();}

    public Double getBackAzimuth() {
        return geodeticArgs.backAzimuth;
    }
    public boolean hasBackAzimuth() {return geodeticArgs.hasBackAzimuth();}
    public void setBackAzimuth(double val) { geodeticArgs.backAzimuth = val;}

    public boolean hasEventLatLon() {
        return  geodeticArgs.hasEventLatLon() || qmlStaxmlArgs.hasQml();
    }

    public boolean hasStationLatLon() {
        return geodeticArgs.hasStationLatLon() || qmlStaxmlArgs.hasStationXML();
    }

    public void validateArguments() {
        if (distArgs.allEmpty()
                && ( (! hasEventLatLon()) || (!hasStationLatLon() ) )
        ) {
            throw new ArgumentValidationException("Must specify at least one distance or station, event.");
        }
        distArgs.validateArguments();
        if (hasEventLatLon() && hasBackAzimuth()) {
            throw new ArgumentValidationException("Cannot specify back azimuth and event");
        }
        if (hasStationLatLon() && hasAzimuth() ) {
            throw new ArgumentValidationException("Cannot specify azimuth and station");
        }
        if ((hasAzimuth() && hasBackAzimuth())) {
            throw new ArgumentValidationException("Cannot specify both azimuth and back azimuth");
        }
        geodeticArgs.validateArguments();
    }

    public boolean isAllIndexRays() {
        return distArgs.allIndexRays;
    }

    @ArgGroup(exclusive = false, multiplicity = "0..*", heading = "Distance is given by:%n")
    DistanceRayArgs distArgs = new DistanceRayArgs();

    @ArgGroup(validate = false, heading = "Lat,Lon influenced by:%n")
    GeodeticArgs geodeticArgs = new GeodeticArgs();

    @CommandLine.Mixin
    QmlStaxmlArgs qmlStaxmlArgs = new QmlStaxmlArgs();

    public void setQuakemlText(String quakemlText) {
        qmlStaxmlArgs.setQuakemlText(quakemlText);
    }
    public void setStationxmlText(String staxmlText) {
        qmlStaxmlArgs.setStationxmlText(staxmlText);
    }

    public List<LatLonLocatable> getStationLatLon() throws TauPException {
        List<LatLonLocatable> staList = new ArrayList<>();
        for (LatLonSimple sta : geodeticArgs.getStationLocations()) {
            for (Double depth : modelArgs.getReceiverDepths()) {
                LatLonSimple staDepth = new LatLonSimple(sta.asLocation().getLatitude(), sta.asLocation().getLongitude(), depth);
                staList.add(staDepth);
            }
        }
        // stationxml events already have station depth
        staList.addAll(qmlStaxmlArgs.getStationLocations());
        return staList;
    }

    public List<LatLonLocatable> getEventLatLon() throws TauPException {
        List<LatLonLocatable> eventLocs = new ArrayList<>();
        for (LatLonSimple evt : geodeticArgs.getEventLocations()) {
            for (Double depth : modelArgs.getSourceDepths()) {
                LatLonSimple evtDepth = new LatLonSimple(evt.asLocation().getLatitude(), evt.asLocation().getLongitude(), depth);
                eventLocs.add(evtDepth);
            }
        }
        // quakeml events already have event depth
        eventLocs.addAll(qmlStaxmlArgs.getEventLocations());
        return eventLocs;
    }

    public void setDegreeList(List<Double> degreesList) {
        distArgs.degreesList = degreesList;
    }

    public void setTakeoffAngles(List<Double> degreesList) {
        distArgs.takeoffAngle = degreesList;
    }

    public void setShootRayParams(List<Double> rayParamList) {
        distArgs.shootRaypList = rayParamList;
    }

    public void setShootRayParamKM(List<Double> rayParamKMList) {
        distArgs.shootKmRaypList = rayParamKMList;
    }

    public void clear() {
        geodeticArgs.stationLatLonList.clear();
        geodeticArgs.eventLatLonList.clear();
        distArgs.takeoffAngle.clear();
        distArgs.incidentAngle.clear();
        distArgs.degreesList.clear();
        distArgs.distKilometersList.clear();
        distArgs.shootRaypList.clear();
        distArgs.shootKmRaypList.clear();
        distArgs.timeList.clear();
        distArgs.shootRadianRaypList.clear();
        distArgs.shootIndexRaypList.clear();
    }


}
