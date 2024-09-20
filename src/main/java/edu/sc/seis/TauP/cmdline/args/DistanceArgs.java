package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.*;
import edu.sc.seis.seisFile.Location;
import edu.sc.seis.seisFile.fdsnws.quakeml.Event;
import edu.sc.seis.seisFile.fdsnws.quakeml.Magnitude;
import edu.sc.seis.seisFile.fdsnws.quakeml.Origin;
import edu.sc.seis.seisFile.fdsnws.stationxml.Channel;
import edu.sc.seis.seisFile.fdsnws.stationxml.Network;
import edu.sc.seis.seisFile.fdsnws.stationxml.Station;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.sc.seis.seisFile.TimeUtils.TZ_UTC;

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
        for (Double d : distArgs.distKilometersList) {
            simpleDistanceList.add(DistanceRay.ofKilometers(d));
        }
        for (Double d : distArgs.exactDistKilometersList) {
            simpleDistanceList.add(ExactDistanceRay.ofKilometers(d));
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


        if (hasEvent && getAzimuth() != null) {
            List<DistanceRay> evtOut = new ArrayList<>();
            for (DistanceRay dr : simpleDistanceList) {
                if (dr.isLatLonable()) {
                    // already enough info, so just add
                    evtOut.add(dr);
                } else {
                    for (Location evtLoc : quakes) {
                        DistanceRay evtDr = new DistanceRay(dr);
                        evtDr.withEventAzimuth(evtLoc, getAzimuth());
                        if (evtLoc.hasDescription()) {
                            dr.setDescription(dr.getDescription());
                        }
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
                        if (staLoc.hasDescription()) {
                            staDr.setDescription(staLoc.getDescription());
                        }
                        staOut.add(staDr);
                    }
                }
            }
            out.addAll(staOut);
        } else if (hasEvent && hasStation) {
            // now add evt-station pairs, already have latlonable
            for (Location evtLoc : quakes) {
                for (Location staLoc : stationList) {
                    DistanceRay dr;
                    if (geodeticArgs.isGeodetic()) {
                        dr = DistanceRay.ofGeodeticStationEvent(staLoc, evtLoc, geodeticArgs.getEllipFlattening());
                    } else {
                        dr = DistanceRay.ofStationEvent(staLoc, evtLoc);
                    }
                    if (staLoc.hasDescription()) {
                        dr.setDescription(staLoc.getDescription());
                    }
                    if (evtLoc.hasDescription()) {
                        dr.setDescription(dr.getDescription()+" "+evtLoc.getDescription());
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
        for (Double d : distArgs.takeoffAngle) {
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

    public List<RayCalculateable> getRayCalculatables() throws TauPException {
        List<RayCalculateable> out = new ArrayList<>();
        out.addAll(getDistances());
        out.addAll(getRayParamDegRays());
        out.addAll(getRayParamKmRays());
        out.addAll(getRayParamRadianRays());
        out.addAll(getTakeoffAngleRays());
        out.addAll(getRayParamIndexRays());
        return out;
    }

    public Double getAzimuth() {
        return latLonArgs.azimuth;
    }
    public void setAzimuth(double val) { latLonArgs.azimuth = val;}
    public boolean hasAzimuth() {return latLonArgs.azimuth != null;}

    public Double getBackAzimuth() {
        return latLonArgs.backAzimuth;
    }
    public boolean hasBackAzimuth() {return latLonArgs.backAzimuth != null;}
    public void setBackAzimuth(double val) { latLonArgs.backAzimuth = val;}

    public boolean hasEventLatLon() {
        return ! latLonArgs.eventList.isEmpty();
    }

    public boolean hasStationLatLon() {
        return ! latLonArgs.stationList.isEmpty();
    }

    public void validateArguments() {
        if (distArgs.allEmpty()
                && (latLonArgs.eventList.isEmpty() || latLonArgs.stationList.isEmpty())
                && ! ( qmlStaxmlArgs.hasQml() && qmlStaxmlArgs.hasStationXML())
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
    }

    @ArgGroup(exclusive = false, multiplicity = "0..*", heading = "Distance is given by:%n")
    DistanceArgsInner distArgs = new DistanceArgsInner();

    @ArgGroup(validate = false, heading = "Lat,Lon influenced by:%n")
    LatLonInner latLonArgs = new LatLonInner();

    @CommandLine.Mixin
    GeodeticArgs geodeticArgs = new GeodeticArgs();

    @CommandLine.Mixin
    QmlStaxmlArgs qmlStaxmlArgs = new QmlStaxmlArgs();

    public List<Location> getStationList() throws TauPException {
        List<Location> staList = new ArrayList<>();
        staList.addAll(latLonArgs.stationList);
        Map<Network, List<Station>> networks = qmlStaxmlArgs.loadStationXML();
        for (Network net : networks.keySet()) {
            for (Station sta : networks.get(net)) {
                List<Location> allChans = new ArrayList<>();
                for (Channel chan : sta.getChannelList()) {
                    Location cLoc = new Location(chan);
                    boolean found = false;
                    for (Location prev : allChans) {
                        if (prev.equals(cLoc)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        cLoc.setDescription(sta.getNetworkCode()+"."+sta.getStationCode());
                        allChans.add(cLoc);
                    }
                }
                staList.addAll(allChans);
            }
        }
        return staList;
    }

    public void clearStationLatLon() {
        latLonArgs.stationList.clear();
    }

    public List<Location> getEventList() throws TauPException {
        List<Location> eventLocs = new ArrayList<>();
        eventLocs.addAll(latLonArgs.eventList);
        List<Event> quakes = qmlStaxmlArgs.loadQuakeML();
        DateTimeFormatter dformat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(TZ_UTC);
        for (Event evt : quakes) {
            Location evtLoc = new Location(evt);
            Origin origin = evt.getPreferredOrigin();
            Magnitude mag = evt.getPreferredMagnitude();
            String desc = "";
            if (origin != null) {
                desc = dformat.format(origin.getTime().asInstant());
            }
            if (mag != null) {
                if (!desc.isEmpty()) {desc += " ";}
                desc += mag.getType()+" "+mag.getMag().getValue();
            }
            if ( ! desc.isEmpty()) {
                evtLoc.setDescription(desc);
            }
            eventLocs.add(evtLoc);
        }
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
        distArgs.degreesList.clear();
        distArgs.distKilometersList.clear();
        distArgs.shootRaypList.clear();
        distArgs.shootKmRaypList.clear();
        distArgs.shootRadianRaypList.clear();
        distArgs.shootIndexRaypList.clear();
    }


    static class DistanceArgsInner {

        @CommandLine.Option(names={"--deg", "--degree"},
                paramLabel="d",
                description="distance in degrees", split=",")
        protected List<Double> degreesList = new ArrayList<>();


        @CommandLine.Option(names={"--exactdegree"},
                paramLabel="d",
                description="exact distance traveled in degrees, not 360-d", split=",")
        protected List<Double> exactDegreesList = new ArrayList<>();

        /**
         * For when command line args uses --km for distance. Have to wait until
         * after the model is read in to get radius of earth.
         */
        @CommandLine.Option(names={"--km", "--kilometer"},
                paramLabel = "km",
                description="distance in kilometers", split=",")
        protected List<Double> distKilometersList = new ArrayList<>();

        /**
         * Exact km, no mod 360
         */
        @CommandLine.Option(names={ "--exactkilometer"},
                paramLabel = "km",
                description="exact distance traveled in kilometers, not 360-k", split=",")
        protected List<Double> exactDistKilometersList = new ArrayList<>();


        @Option(names="--takeoff",
                split=",",
                paramLabel = "deg",
                description="takeoff angle in degrees from the source zero is down, 90 horizontal, 180 is up")
        protected List<Double> takeoffAngle = new ArrayList<>();

        @CommandLine.Option(names={"--rayparamrad"},
                paramLabel = "s/rad",
                description="ray parameter from the source in s/rad, up or down is determined by the phase",
                split=",")
        protected List<Double> shootRadianRaypList = new ArrayList<>();

        @CommandLine.Option(names={"--rayparamdeg"},
                paramLabel = "s/deg",
                description="ray parameter from the source in s/deg, up or down is determined by the phase",
                split=",")
        protected List<Double> shootRaypList = new ArrayList<>();

        @CommandLine.Option(names={"--rayparamkm"},
                paramLabel = "s/km",
                description="ray parameter from the source in s/km, up or down is determined by the phase",
                split=",")
        protected List<Double> shootKmRaypList = new ArrayList<>();

        @CommandLine.Option(names={"--rayparamidx"},
                paramLabel = "i",
                description="ray parameter from the source as index into model sampling, up or down is determined by the phase",
                split=",")
        protected List<Integer> shootIndexRaypList = new ArrayList<>();

        public boolean allEmpty() {
            return degreesList.isEmpty()
                    && distKilometersList.isEmpty()
                    && exactDegreesList.isEmpty()
                    && exactDistKilometersList.isEmpty()
                    && shootIndexRaypList.isEmpty()
                    && shootKmRaypList.isEmpty()
                    && shootRadianRaypList.isEmpty()
                    && shootRaypList.isEmpty()
                    && takeoffAngle.isEmpty();
        }
    }

    static class LatLonInner {

        @Option(names={"--sta", "--station"},
                arity="2",
                paramLabel = "l",
                description="station latitude and longitude. Creates a distance if event is also given."
        )
        void setStationLatLon(List<Double> stationLatLon) {
            if (stationLatLon.isEmpty()) {
                stationList.clear();
            }
            if (stationLatLon.size() % 2 != 0) {
                throw new IllegalArgumentException("Station lat lon must have even number of items: "+stationLatLon.size());
            }
            for (int i = 0; i < stationLatLon.size()/2; i+=2) {
                stationList.add(new Location(stationLatLon.get(i), stationLatLon.get(i+1)));
            }
        }

        protected List<Location> stationList = new ArrayList<>();

        @Option(names={"--evt", "--event"},
                paramLabel = "l",
                arity="2",
                description="event latitude and longitude.  Creates a distance if station is also given.")
        void setEventLatLon(List<Double> eventLatLon) {
            if (eventLatLon.isEmpty()) {
                eventList.clear();
            }
            if (eventLatLon.size() % 2 != 0) {
                throw new IllegalArgumentException("Event lat lon must have even number of items: "+eventLatLon.size());
            }
            for (int i = 0; i < eventLatLon.size()/2; i+=2) {
                eventList.add(new Location(eventLatLon.get(i), eventLatLon.get(i+1)));
            }
        }

        protected List<Location> eventList = new ArrayList<>();

        @Option(names="--az", description="azimuth in degrees")
        protected Double azimuth = null;


        @Option(names="--baz", description="backazimuth in degrees")
        protected Double backAzimuth = null;

    }
}
