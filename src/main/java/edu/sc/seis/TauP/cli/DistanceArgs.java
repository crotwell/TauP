package edu.sc.seis.TauP.cli;

import edu.sc.seis.TauP.*;
import edu.sc.seis.seisFile.Location;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;

public class DistanceArgs {

    public List<DistanceRay> getDistances() {
        List<DistanceRay> out = new ArrayList<>();
        for (Double d : distArgs.degreesList) {
            out.add(DistanceRay.ofDegrees(d));
        }
        for (Double d : distArgs.exactDegreesList) {
            out.add(ExactDistanceRay.ofDegrees(d));
        }
        for (Double d : distArgs.distKilometersList) {
            out.add(DistanceRay.ofKilometers(d));
        }
        for (Double d : distArgs.exactDistKilometersList) {
            out.add(ExactDistanceRay.ofKilometers(d));
        }

        if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
            List<DistanceRay> evtOut = new ArrayList<>();
            for (DistanceRay dr : out) {
                if (dr.isLatLonable()) {
                    // already enough info, so just add
                    evtOut.add(dr);
                }
                for (Location evt : latLonArgs.eventList) {
                    DistanceRay evtDr = new DistanceRay(dr);
                    evtDr.withEventAzimuth(evt, getAzimuth());
                    evtOut.add(evtDr);
                }
            }
            out = evtOut;
        } else if ( ! hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
            List<DistanceRay> staOut = new ArrayList<>();
            for (DistanceRay dr : out) {
                if (dr.isLatLonable()) {
                    // already enough info, so just add
                    staOut.add(dr);
                }
                for (Location sta : latLonArgs.stationList) {
                    DistanceRay staDr = new DistanceRay(dr);
                    staDr.withStationBackAzimuth(sta, getBackAzimuth());
                    staOut.add(staDr);
                }
            }
            out = staOut;
        } else if (hasEventLatLon() && hasStationLatLon()) {
            // now add evt-station pairs, already have latlonable
            for (Location evtLoc : latLonArgs.eventList) {
                for (Location staLoc : latLonArgs.stationList) {
                    if (latLonArgs.geodetic) {
                        throw new RuntimeException("geodetic not yet...");
                    } else {
                        out.add(DistanceRay.ofStationEvent(staLoc, evtLoc));
                    }
                }
            }
        }
        return out;
    }

    public List<RayParamKmRay> getRayParamKmRays() {
        List<RayParamKmRay> rpList = new ArrayList<>();
        for (Double d : distArgs.shootKmRaypList) {
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                if (latLonArgs.geodetic) {
                    throw new RuntimeException("geodetic not yet...");
                } else {
                    for (Location evt : latLonArgs.eventList) {
                        RayParamKmRay evtDr = new RayParamKmRay(d);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                if (latLonArgs.geodetic) {
                    throw new RuntimeException("geodetic not yet...");
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
                if (latLonArgs.geodetic) {
                    throw new RuntimeException("geodetic not yet...");
                } else {
                    for (Location evt : latLonArgs.eventList) {
                        RayParamRay evtDr = RayParamRay.ofRayParamSDegree(d);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                if (latLonArgs.geodetic) {
                    throw new RuntimeException("geodetic not yet...");
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
                if (latLonArgs.geodetic) {
                    throw new RuntimeException("geodetic not yet...");
                } else {
                    for (Location evt : latLonArgs.eventList) {
                        RayParamRay evtDr = RayParamRay.ofRayParamSRadian(d);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                if (latLonArgs.geodetic) {
                    throw new RuntimeException("geodetic not yet...");
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
                if (latLonArgs.geodetic) {
                    throw new RuntimeException("geodetic not yet...");
                } else {
                    for (Location evt : latLonArgs.eventList) {
                        RayParamIndexRay evtDr = new RayParamIndexRay(d);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                if (latLonArgs.geodetic) {
                    throw new RuntimeException("geodetic not yet...");
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
            if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
                if (latLonArgs.geodetic) {
                    throw new RuntimeException("geodetic not yet...");
                } else {
                    for (Location evt : latLonArgs.eventList) {
                        TakeoffAngleRay evtDr = new TakeoffAngleRay(d);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        rpList.add(evtDr);
                    }
                }
            } else if (!hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
                if (latLonArgs.geodetic) {
                    throw new RuntimeException("geodetic not yet...");
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

    public List<RayCalculateable> getRayCalculatables() {
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

    @ArgGroup(exclusive = false, heading = "Distance is given by:%n")
    DistanceArgsInner distArgs = new DistanceArgsInner();

    @ArgGroup(validate = false, heading = "Lat,Lon influenced by:%n")
    LatLonInner latLonArgs = new LatLonInner();

    public List<Location> getStationList() {
        return latLonArgs.stationList;
    }
    public void clearStationLatLon() {
        latLonArgs.stationList.clear();
    }

    public List<Location> getEventList() {
        return latLonArgs.eventList;
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
        protected Double azimuth = Double.MAX_VALUE;


        @Option(names="--baz", description="backazimuth in degrees")
        protected Double backAzimuth = Double.MAX_VALUE;

        @Option(names = "--geodetic",
                description = "use geodetic latitude for distance calculations, which implies an ellipticity. "
                        +"Default is spherical. Note this only affects calculation of distance from lat/lon, "
                        +"all travel time calculations are done in a purely spherical model.")
        protected boolean geodetic = false;

        @Option(names= "--ellipflattening",
                paramLabel = "f",
                description = "Elliptical flattening for distance calculations when --geodetic, defaults to WGS84 ~ 1/298.257")
        protected double ellipflattening = DistAz.wgs85_flattening;
    }
}
