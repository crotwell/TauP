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
                for (Location evt : distArgs.eventList) {
                    DistanceRay evtDr = new DistanceRay(dr);
                    evtDr.withEventAzimuth(evt, getAzimuth());
                    evtOut.add(dr);
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
                for (Location evt : distArgs.eventList) {
                    DistanceRay evtDr = new DistanceRay(dr);
                    evtDr.withEventAzimuth(evt, getAzimuth());
                    staOut.add(dr);
                }
            }
            out = staOut;
        } else if (hasEventLatLon() && hasStationLatLon()) {
            // now add evt-station pairs, already have latlonable
            for (Location evtLoc : distArgs.eventList) {
                for (Location staLoc : distArgs.stationList) {
                    if (distArgs.geodetic) {
                        throw new RuntimeException("geodetic not yet...");
                    } else {
                        out.add(DistanceRay.ofStationEvent(staLoc, evtLoc));
                    }
                }
            }
        }
        return out;
    }

    public List<RayParamRay> getRayParamRays() {
        List<RayParamRay> rpList = new ArrayList<>();
        for (Double d : distArgs.shootRaypList) {
            rpList.add(RayParamRay.ofRayParamSDegree(d));
        }
        if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
            if (distArgs.geodetic) {
                throw new RuntimeException("geodetic not yet...");
            } else {
                List<RayParamRay> evtOut = new ArrayList<>();
                for (RayParamRay dr : rpList) {
                    for (Location evt : distArgs.eventList) {
                        RayParamRay evtDr = new RayParamRay(dr);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        evtOut.add(dr);
                    }
                }
                rpList = evtOut;
            }

        } else if ( ! hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
            List<RayParamRay> staOut = new ArrayList<>();
            for (RayParamRay dr : rpList) {
                if (dr.isLatLonable()) {
                    // already enough info, so just add
                    staOut.add(dr);
                }
                for (Location evt : distArgs.eventList) {
                    RayParamRay evtDr = new RayParamRay(dr);
                    evtDr.withEventAzimuth(evt, getAzimuth());
                    staOut.add(dr);
                }

            }
            rpList = staOut;
        }
        return rpList;
    }

    public List<TakeoffAngleRay> getTakeoffAngleRays() {
        List<TakeoffAngleRay> rpList = new ArrayList<>();
        for (Double d : distArgs.takeoffAngle) {
            rpList.add(TakeoffAngleRay.ofTakeoffAngle(d));
        }
        if (hasEventLatLon() && !hasStationLatLon() && getAzimuth() != null) {
            if (distArgs.geodetic) {
                throw new RuntimeException("geodetic not yet...");
            } else {
                List<TakeoffAngleRay> evtOut = new ArrayList<>();
                for (TakeoffAngleRay dr : rpList) {
                    for (Location evt : distArgs.eventList) {
                        TakeoffAngleRay evtDr = new TakeoffAngleRay(dr);
                        evtDr.withEventAzimuth(evt, getAzimuth());
                        evtOut.add(dr);
                    }
                }
                rpList = evtOut;
            }
        } else if ( ! hasEventLatLon() && hasStationLatLon() && getBackAzimuth() != null) {
            List<TakeoffAngleRay> staOut = new ArrayList<>();
            for (TakeoffAngleRay dr : rpList) {
                if (dr.isLatLong()) {
                    // already enough info, so just add
                    staOut.add(dr);
                }
                for (Location evt : distArgs.eventList) {
                    TakeoffAngleRay evtDr = new TakeoffAngleRay(dr);
                    evtDr.withEventAzimuth(evt, getAzimuth());
                    staOut.add(dr);
                }

            }
            rpList = staOut;
        }
        return rpList;
    }
    public List<RayCalculateable> getRayCalculatables() {
        List<RayCalculateable> out = new ArrayList<>();
        out.addAll(getDistances());
        out.addAll(getRayParamRays());
        out.addAll(getTakeoffAngleRays());
        return out;
    }

    public Double getAzimuth() {
        return distArgs.azimuth;
    }
    public void setAzimuth(double val) { distArgs.azimuth = val;}
    public boolean hasAzimuth() {return distArgs.azimuth != null;}

    public Double getBackAzimuth() {
        return distArgs.backAzimuth;
    }
    public void setBackAzimuth(double val) { distArgs.backAzimuth = val;}

    public boolean hasEventLatLon() {
        return ! distArgs.eventList.isEmpty();
    }

    public boolean hasStationLatLon() {
        return ! distArgs.stationList.isEmpty();
    }

    @ArgGroup(validate = false, heading = "Distance is given by:%n")
    DistanceArgsInner distArgs = new DistanceArgsInner();

    public List<Location> getStationList() {
        return distArgs.stationList;
    }
    public void clearStationLatLon() {
        distArgs.stationList.clear();
    }

    public List<Location> getEventList() {
        return distArgs.eventList;
    }
    public void clearEventLatLon() {
        distArgs.eventList.clear();
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

    public void clear() {
        distArgs.stationList.clear();
        distArgs.eventList.clear();
        distArgs.takeoffAngle.clear();
        distArgs.degreesList.clear();
        distArgs.distKilometersList.clear();
        distArgs.shootRaypList.clear();
    }


    static class DistanceArgsInner {

        @CommandLine.Option(names={"--deg", "--degree"}, paramLabel="d", description="distance in degrees", split=",")
        protected List<Double> degreesList = new ArrayList<Double>();


        @CommandLine.Option(names={"--exactdegree"}, paramLabel="d",
                description="exact distance traveled in degrees, not 360-d", split=",")
        protected List<Double> exactDegreesList = new ArrayList<Double>();

        /**
         * For when command line args uses --km for distance. Have to wait until
         * after the model is read in to get radius of earth.
         */
        @CommandLine.Option(names={"--km", "--kilometer"}, paramLabel = "km",
                description="distance in kilometers", split=",")
        protected List<Double> distKilometersList = new ArrayList<Double>();

        /**
         * Exact km, no mod 360
         */
        @CommandLine.Option(names={ "--exactkilometer"}, paramLabel = "km",
                description="exact distance traveled in kilometers, not 360-k", split=",")
        protected List<Double> exactDistKilometersList = new ArrayList<Double>();

        @Option(names="--az", description="azimuth in degrees")
        protected Double azimuth = Double.MAX_VALUE;


        @Option(names="--baz", description="backazimuth in degrees")
        protected Double backAzimuth = Double.MAX_VALUE;


        @Option(names="--takeoff",
                description="takeoff angle in degrees from the source zero is down, 90 horizontal, 180 is up")
        protected List<Double> takeoffAngle = new ArrayList<Double>();

        @CommandLine.Option(names={"--shoot", "--shootray"},
                description="ray parameter from the source in s/deg up or down is determined by the phase",
                split=",")
        protected List<Double> shootRaypList = new ArrayList<Double>();

        @Option(names={"--sta", "--station"},
                arity="2",
                description="station latitude and longitude"
                )
        void setStationLatLon(List<Double> stationLatLon) {
            if (stationLatLon.size() == 0) {
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

        @Option(names={"--evt", "--event"}, arity="2", description="event latitude and longitude")
        void setEventLatLon(List<Double> eventLatLon) {
            if (eventLatLon.size() == 0) {
                eventList.clear();
            }
            if (eventLatLon.size() % 2 != 0) {
                throw new IllegalArgumentException("Station lat lon must have even number of items: "+eventLatLon.size());
            }
            for (int i = 0; i < eventLatLon.size()/2; i+=2) {
                eventList.add(new Location(eventLatLon.get(i), eventLatLon.get(i+1)));
            }
        }

        protected List<Location> eventList = new ArrayList<>();

        @Option(names = "--geodetic",
                description = "use geodetic latitude for distance calculations, which implies an ellipticity. "
                        +"Default is spherical. Note this only affects calculation of distance from lat/lon, "
                        +"all travel time calculations are done in a purely spherical model.")
        protected boolean geodetic = false;

        @Option(names= "--ellipflattening",
                description = "Elliptical flattening for distance calculations when --geodetic, defaults to WGS84 ~ 1/298.257")
        protected double ellipflattening = DistAz.wgs85_flattening;
    }
}
