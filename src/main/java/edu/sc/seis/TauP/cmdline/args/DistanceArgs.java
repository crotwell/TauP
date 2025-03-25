package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.*;
import edu.sc.seis.seisFile.Location;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;

import java.util.ArrayList;
import java.util.List;

public class DistanceArgs {

    public List<DistanceRay> getDistances() throws TauPException {
        List<DistanceRay> out = new ArrayList<>();
        List<DistanceRay> simpleDistanceList = new ArrayList<>();
        for (Double d : distArgs.degreesList) {
            simpleDistanceList.add(DistanceRay.ofDegrees(d));
        }
        for (Double d : distArgs.exactDegreesList) {
            simpleDistanceList.add(ExactDistanceRay.ofDegrees(d));
        }

        if (!distArgs.degreeRange.isEmpty()) {
            for (Double d : createListFromRange(distArgs.degreeRange, 0, 180, 10)) {
                simpleDistanceList.add(DistanceRay.ofDegrees(d));
            }
        }
        for (Double d : distArgs.distKilometersList) {
            simpleDistanceList.add(DistanceRay.ofKilometers(d));
        }
        for (Double d : distArgs.exactDistKilometersList) {
            simpleDistanceList.add(ExactDistanceRay.ofKilometers(d));
        }

        if (!distArgs.kilometerRange.isEmpty()) {
            for (Double d : createListFromRange(distArgs.kilometerRange, 0, 1000, 100)) {
                simpleDistanceList.add(DistanceRay.ofKilometers(d));
            }
        }

        boolean hasEvent = hasEventLatLon() || qmlStaxmlArgs.hasQml();
        List<Location> quakes = new ArrayList<>();
        if (hasEvent) {
            quakes = getEventList();
            hasEvent = ! quakes.isEmpty();
        }

        boolean hasStation = hasStationLatLon() || qmlStaxmlArgs.hasStationXML();
        List<Location> stationList = new ArrayList<>();
        if (hasStation) {
            stationList = getStationList();
        }
        hasStation = ! stationList.isEmpty();


        if (hasEvent && getAzimuth() != null && !hasStation) {
            List<DistanceRay> evtOut = new ArrayList<>();
            for (DistanceRay dr : simpleDistanceList) {
                if (dr.isLatLonable()) {
                    // already enough info, so just add
                    evtOut.add(dr);
                } else {
                    for (Location evtLoc : quakes) {
                        DistanceRay evtDr = new DistanceRay(dr);
                        evtDr.withEventAzimuth(evtLoc, getAzimuth());
                        String desc;
                        if (evtLoc.hasDescription()) {
                            desc = evtLoc.getDescription();
                        } else {
                            desc = Outputs.formatLatLon(evtLoc.getLatitude()).trim()
                                    + "/" + Outputs.formatLatLon(evtLoc.getLongitude()).trim();
                        }
                        dr.setDescription(desc+" to az "+Outputs.formatDistance(getAzimuth()));
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
                    for (Location staLoc : stationList) {
                        DistanceRay staDr = new DistanceRay(dr);
                        staDr.withStationBackAzimuth(staLoc, getBackAzimuth());
                        String desc;
                        if (staLoc.hasDescription()) {
                            desc = staLoc.getDescription();
                        } else {
                            desc = Outputs.formatLatLon(staLoc.getLatitude()).trim()
                                    + "/" + Outputs.formatLatLon(staLoc.getLongitude()).trim();
                        }
                        staDr.setDescription("baz "+Outputs.formatDistance(getAzimuth())+" to "+desc);
                        staOut.add(staDr);
                    }
                }
            }
            out.addAll(staOut);
        } else if (hasEvent && hasStation) {
            // add simple distances
            out.addAll(simpleDistanceList);
            // now add evt-station pairs, already have latlonable
            for (Location evtLoc : quakes) {
                for (Location staLoc : stationList) {
                    DistanceRay dr;
                    if (geodeticArgs.isGeodetic()) {
                        dr = DistanceRay.ofGeodeticEventStation(evtLoc, staLoc, geodeticArgs.getInverseEllipFlattening());
                    } else {
                        dr = DistanceRay.ofEventStation(evtLoc, staLoc);
                    }
                    if (evtLoc.hasDescription() && staLoc.hasDescription()) {
                        dr.setDescription(evtLoc.getDescription()+" to "+staLoc.getDescription());
                    } else if (staLoc.hasDescription()) {
                        String evt_desc = Outputs.formatLatLon(evtLoc.getLatitude()).trim()
                                + "/" + Outputs.formatLatLon(evtLoc.getLongitude()).trim();
                        dr.setDescription(evt_desc+" to "+staLoc.getDescription());
                    } else if (evtLoc.hasDescription()) {
                        String sta_desc = Outputs.formatLatLon(staLoc.getLatitude()).trim()
                                + "/" + Outputs.formatLatLon(staLoc.getLongitude()).trim();
                        dr.setDescription(evtLoc.getDescription()+" to "+sta_desc);
                    }
                    out.add(dr);
                }
            }
        } else {
            // no event or station, so just add simple distances
            out.addAll(simpleDistanceList);
        }
        return out;
    }

    public List<RayParamKmRay> getRayParamKmRays() {
        List<RayParamKmRay> rpList = new ArrayList<>();
        for (Double d : distArgs.shootKmRaypList) {
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                if (geodeticArgs.isGeodetic()) {
                    throw new IllegalArgumentException("geodetic not yet for az from event...");
                } else {
                    for (Location evt : latLonArgs.eventList) {
                        RayParamKmRay evtDr = new RayParamKmRay(d);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                if (geodeticArgs.isGeodetic()) {
                    throw new IllegalArgumentException("geodetic not yet for baz from station...");
                } else {
                    for (Location sta : latLonArgs.stationList) {
                        RayParamKmRay staDr = new RayParamKmRay(d);
                        staDr.withStationBackAzimuth(sta, getBackAzimuth());
                        rpList.add(staDr);
                    }
                }
            } else {
                rpList.add(new RayParamKmRay(d));
            }
        }
        return rpList;
    }

    public List<RayParamRay> getRayParamDegRays() {
        List<RayParamRay> rpList = new ArrayList<>();
        for (Double d : distArgs.shootRaypList) {
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                if (geodeticArgs.isGeodetic()) {
                    throw new IllegalArgumentException("geodetic not yet...");
                } else {
                    for (Location evt : latLonArgs.eventList) {
                        RayParamRay evtDr = RayParamRay.ofRayParamSDegree(d);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                if (geodeticArgs.isGeodetic()) {
                    throw new IllegalArgumentException("geodetic not yet for baz from station...");
                } else {
                    for (Location sta : latLonArgs.stationList) {
                        RayParamRay staDr = RayParamRay.ofRayParamSDegree(d);
                        staDr.withStationBackAzimuth(sta, getBackAzimuth());
                        rpList.add(staDr);
                    }
                }
            } else {
                rpList.add(RayParamRay.ofRayParamSDegree(d));
            }
        }
        return rpList;
    }

    public List<RayParamRay> getRayParamRadianRays() {
        List<RayParamRay> rpList = new ArrayList<>();
        for (Double d : distArgs.shootRadianRaypList) {
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                if (geodeticArgs.isGeodetic()) {
                    throw new IllegalArgumentException("geodetic not yet...");
                } else {
                    for (Location evt : latLonArgs.eventList) {
                        RayParamRay evtDr = RayParamRay.ofRayParamSRadian(d);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                if (geodeticArgs.isGeodetic()) {
                    throw new IllegalArgumentException("geodetic not yet for baz from station...");
                } else {
                    for (Location sta : latLonArgs.stationList) {
                        RayParamRay staDr = RayParamRay.ofRayParamSRadian(d);
                        staDr.withStationBackAzimuth(sta, getBackAzimuth());
                        rpList.add(staDr);
                    }
                }
            } else {
                rpList.add(RayParamRay.ofRayParamSRadian(d));
            }
        }
        return rpList;
    }


    public List<RayParamIndexRay> getRayParamIndexRays() {
        List<RayParamIndexRay> rpList = new ArrayList<>();
        for (Integer d : distArgs.shootIndexRaypList) {
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                if (geodeticArgs.isGeodetic()) {
                    throw new IllegalArgumentException("geodetic not yet...");
                } else {
                    for (Location evt : latLonArgs.eventList) {
                        RayParamIndexRay evtDr = new RayParamIndexRay(d);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                if (geodeticArgs.isGeodetic()) {
                    throw new IllegalArgumentException("geodetic not yet...");
                } else {
                    for (Location sta : latLonArgs.stationList) {
                        RayParamIndexRay staDr = new RayParamIndexRay(d);
                        staDr.withStationBackAzimuth(sta, getBackAzimuth());
                        rpList.add(staDr);
                    }
                }
            } else {
                rpList.add(new RayParamIndexRay(d));
            }
        }
        return rpList;
    }

    public List<TakeoffAngleRay> getTakeoffAngleRays() {
        List<TakeoffAngleRay> rpList = new ArrayList<>();
        List<Double> takeoffInputList = new ArrayList<>();
        takeoffInputList.addAll(distArgs.takeoffAngle);
        if (!distArgs.takeoffRange.isEmpty()) {
            takeoffInputList.addAll(createListFromRange(distArgs.takeoffRange));
        }
        for (Double d : takeoffInputList) {
            if (d < 0 || d > 180) {
                throw new IllegalArgumentException("Takeoff angle should be between 0 and 180 degrees: "+d);
            }
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                if (geodeticArgs.isGeodetic()) {
                    throw new IllegalArgumentException("geodetic not yet...");
                } else {
                    for (Location evt : latLonArgs.eventList) {
                        TakeoffAngleRay evtDr = new TakeoffAngleRay(d);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                if (geodeticArgs.isGeodetic()) {
                    throw new IllegalArgumentException("geodetic not yet...");
                } else {
                    for (Location sta : latLonArgs.stationList) {
                        TakeoffAngleRay staDr = new TakeoffAngleRay(d);
                        staDr.withStationBackAzimuth(sta, getBackAzimuth());
                        rpList.add(staDr);
                    }
                }
            } else {
                rpList.add(TakeoffAngleRay.ofTakeoffAngle(d));
            }
        }
        return rpList;
    }

    public List<IncidentAngleRay> getIncidentAngleRays() {
        List<IncidentAngleRay> rpList = new ArrayList<>();
        List<Double> incidentAngleInputList = new ArrayList<>();
        incidentAngleInputList.addAll(distArgs.incidentAngle);
        if (!distArgs.incidentRange.isEmpty()) {
            incidentAngleInputList.addAll(createListFromRangeDeg(distArgs.incidentRange));
        }
        for (Double d : incidentAngleInputList) {
            if (d < 0 || d > 180) {
                throw new IllegalArgumentException("Incident angle should be between 0 and 180 degrees: "+d);
            }
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                if (geodeticArgs.isGeodetic()) {
                    throw new IllegalArgumentException("geodetic not yet...");
                } else {
                    for (Location evt : latLonArgs.eventList) {
                        IncidentAngleRay evtDr = new IncidentAngleRay(d);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                if (geodeticArgs.isGeodetic()) {
                    throw new IllegalArgumentException("geodetic not yet...");
                } else {
                    for (Location sta : latLonArgs.stationList) {
                        IncidentAngleRay staDr = new IncidentAngleRay(d);
                        staDr.withStationBackAzimuth(sta, getBackAzimuth());
                        rpList.add(staDr);
                    }
                }
            } else {
                rpList.add(IncidentAngleRay.ofIncidentAngle(d));
            }
        }
        return rpList;
    }

    public static List<Double> createListFromRange(List<Double> minMaxStep) {
        double step = 10;
        double start = 0;
        double stop = 180;
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
                stop = minMaxStep.get(0);
                break;
            case 0:
                break;
            default:
                throw new IllegalArgumentException("range length should be 1-3 but was "+minMaxStep.size());
        }
        if (step == 0.0) {
            throw new IllegalArgumentException("Step cannot be zero");
        }
        double d = start;
        List<Double> out = new ArrayList<>();
        while (d<= stop) {
            out.add(d);
            d+=step;
        }
        return out;
    }

    public List<RayCalculateable> getRayCalculatables() throws TauPException {
        return getRayCalculatables(null);
    }

    public List<RayCalculateable> getRayCalculatables(SeismicSourceArgs sourceArgs) throws TauPException {
        List<RayCalculateable> out = new ArrayList<>();
        out.addAll(getDistances());
        out.addAll(getRayParamDegRays());
        out.addAll(getRayParamKmRays());
        out.addAll(getRayParamRadianRays());
        out.addAll(getTakeoffAngleRays());
        out.addAll(getIncidentAngleRays());
        out.addAll(getRayParamIndexRays());
        if (hasAzimuth()) {
            for (RayCalculateable rc : out) {
                if (!rc.hasAzimuth()) {
                    rc.setAzimuth(getAzimuth());
                }
            }
        }
        if (sourceArgs != null) {
            for (RayCalculateable rc : out) {
                if (!rc.hasSourceArgs()) {
                    rc.setSourceArgs(sourceArgs);
                }
            }
        }
        return out;
    }

    public Double getAzimuth() {
        return latLonArgs.azimuth;
    }
    public void setAzimuth(double val) { latLonArgs.azimuth = val;}
    public boolean hasAzimuth() {return latLonArgs.hasAzimuth();}

    public Double getBackAzimuth() {
        return latLonArgs.backAzimuth;
    }
    public boolean hasBackAzimuth() {return latLonArgs.hasBackAzimuth();}
    public void setBackAzimuth(double val) { latLonArgs.backAzimuth = val;}

    public boolean hasEventLatLon() {
        return ! latLonArgs.eventList.isEmpty();
    }

    public boolean hasStationLatLon() {
        return ! latLonArgs.stationList.isEmpty();
    }

    public void validateArguments() {
        if (distArgs.allEmpty()
                && ( (latLonArgs.eventList.isEmpty() && !qmlStaxmlArgs.hasQml() )
                    || (latLonArgs.stationList.isEmpty() && ! qmlStaxmlArgs.hasStationXML()) )
        ) {
            throw new IllegalArgumentException("Must specify at least one distance or station, event.");
        }
        for (Double d : distArgs.takeoffAngle) {
            if (d < 0 || d > 180) {
                throw new IllegalArgumentException("Takeoff angle should be between 0 and 180 degrees: " + d);
            }
        }
        if (!latLonArgs.eventList.isEmpty() && !latLonArgs.stationList.isEmpty()
                && (hasAzimuth() || hasBackAzimuth())) {
            throw new IllegalArgumentException("Cannot specify azimuth or back azimuth when both station and event are given");
        }
        if ((hasAzimuth() && hasBackAzimuth())) {
            throw new IllegalArgumentException("Cannot specify both azimuth and back azimuth");
        }
        latLonArgs.validateArguments();
        geodeticArgs.validateArguments();
    }

    public boolean isAllIndexRays() {
        return distArgs.allIndexRays;
    }

    @ArgGroup(exclusive = false, multiplicity = "0..*", heading = "Distance is given by:%n")
    DistanceRayArgs distArgs = new DistanceRayArgs();

    @ArgGroup(validate = false, heading = "Lat,Lon influenced by:%n")
    LatLonAzBazArgs latLonArgs = new LatLonAzBazArgs();

    @CommandLine.Mixin
    GeodeticArgs geodeticArgs = new GeodeticArgs();

    @CommandLine.Mixin
    QmlStaxmlArgs qmlStaxmlArgs = new QmlStaxmlArgs();

    public List<Location> getStationList() throws TauPException {
        List<Location> staList = new ArrayList<>();
        staList.addAll(latLonArgs.getStationLocations());
        staList.addAll(qmlStaxmlArgs.getStationLocations());
        return staList;
    }

    public void clearStationLatLon() {
        latLonArgs.stationList.clear();
    }

    public List<Location> getEventList() throws TauPException {
        List<Location> eventLocs = new ArrayList<>();
        eventLocs.addAll(latLonArgs.getEventLocations());
        eventLocs.addAll(qmlStaxmlArgs.getEventLocations());
        return eventLocs;
    }
    public void clearEventLatLon() {
        latLonArgs.eventList.clear();
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
        latLonArgs.stationList.clear();
        latLonArgs.eventList.clear();
        distArgs.takeoffAngle.clear();
        distArgs.incidentAngle.clear();
        distArgs.degreesList.clear();
        distArgs.distKilometersList.clear();
        distArgs.shootRaypList.clear();
        distArgs.shootKmRaypList.clear();
        distArgs.shootRadianRaypList.clear();
        distArgs.shootIndexRaypList.clear();
    }


}
