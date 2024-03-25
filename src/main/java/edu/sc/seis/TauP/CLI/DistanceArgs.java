package edu.sc.seis.TauP.CLI;

import edu.sc.seis.TauP.*;
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
        for (Double d : distArgs.distKilometersList) {
            out.add(DistanceRay.ofKilometers(d));
        }
        return out;
    }

    public List<RayCalculateable> getShootRays() {
        List<RayCalculateable> out = new ArrayList<>();
        out.addAll(getDistances());
        for (Double d : distArgs.shootRaypList) {
            out.add(RayParamRay.ofRayParam(d));
        }
        for (Double d : distArgs.takeoffAngle) {
            out.add(TakeoffAngleRay.ofTakeoffAngle(d));
        }
        LatLonable latLonable = getLatLonable();
        if (latLonable != null ) {
            for (RayCalculateable shoot : out) {
                shoot.setLatLonable(latLonable);
            }
        }
        return out;
    }

    public LatLonable getLatLonable() {
        LatLonable latLonable = null;
        if (distArgs.geodetic) {
            throw new RuntimeException("geodetic not yet...");
        } else {
            if (hasEventLatLon() && hasStationLatLon()) {
                latLonable = new EventStation(distArgs.eventLat, distArgs.eventLon, distArgs.stationLat, distArgs.stationLon);
            } else if (getAzimuth() != null && hasEventLatLon()) {
                latLonable = new EventAzimuth(distArgs.eventLat, distArgs.eventLon, getAzimuth());
            } else if (getBackAzimuth() != null && hasStationLatLon()) {
                latLonable = new StationBackAzimuth(distArgs.stationLat, distArgs.stationLon, getBackAzimuth());
            }
        }
        return latLonable;
    }

    public Double getAzimuth() {
        return distArgs.azimuth;
    }
    public void setAzimuth(double val) { distArgs.azimuth = val;}
    public Double getBackAzimuth() {
        return distArgs.backAzimuth;
    }
    public void setBackAzimuth(double val) { distArgs.backAzimuth = val;}
    public boolean hasEventLatLon() {
        return distArgs.eventLat != null && distArgs.eventLon != null;
    }
    public boolean hasStationLatLon() {
        return distArgs.stationLat != null && distArgs.stationLon != null;
    }

    @ArgGroup(validate = false, heading = "Distance is given by:%n")
    DistanceArgsInner distArgs = new DistanceArgsInner();

    public void setStationLatLon(double stationLat, double stationLon) {
        distArgs.stationLat = stationLat;
        distArgs.stationLon = stationLon;
    }
    public void unsetStationLatLon() {
        distArgs.stationLat = null;
        distArgs.stationLon = null;
    }

    public void setEventLatLon(double evLat, double evLon) {
        distArgs.eventLat = evLat;
        distArgs.eventLon = evLon;
    }
    public void unsetEventLatLon() {
        distArgs.eventLat = null;
        distArgs.eventLon = null;
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

    public void setStationList(List<Double[]> stlatlonList) {
        if (stlatlonList.size()>1) {
            throw new RuntimeException("multiple sta lat/lon not yet impl.");
        }
        distArgs.stationLat = stlatlonList.get(0)[0];
        distArgs.stationLon = stlatlonList.get(0)[1];
    }

    public void clear() {
        distArgs.stationLat = null;
        distArgs.stationLon = null;
        distArgs.eventLat = null;
        distArgs.eventLon = null;
        distArgs.takeoffAngle.clear();
        distArgs.degreesList.clear();
        distArgs.distKilometersList.clear();
        distArgs.shootRaypList.clear();
    }


    static class DistanceArgsInner {

        @CommandLine.Option(names={"--deg", "--degrees"}, paramLabel="d", description="distance in degrees", split=",")
        protected List<Double> degreesList = new ArrayList<Double>();

        /**
         * For when command line args uses --km for distance. Have to wait until
         * after the model is read in to get radius of earth.
         */
        @CommandLine.Option(names={"--km", "--kilometers"}, paramLabel = "km", description="distance in kilometers", split=",")
        protected List<Double> distKilometersList = new ArrayList<Double>();

        @Option(names="-az", description="azimuth in degrees")
        protected double azimuth = Double.MAX_VALUE;


        @Option(names="-baz", description="backazimuth in degrees")
        protected double backAzimuth = Double.MAX_VALUE;


        @Option(names="--takeoff",
                description="takeoff angle in degrees from the source zero is down, 90 horizontal, 180 is up")
        protected List<Double> takeoffAngle = new ArrayList<Double>();

        @CommandLine.Option(names={"--shoot", "--shootray"},
                description="ray parameter from the source in s/deg up or down is determined by the phase",
                split=",")
        protected List<Double> shootRaypList = new ArrayList<Double>();

        @Option(names={"-sta", "--station"}, arity="2", description="station latitude and longitude"
                )
        void setStationLatLon(double stationLat, double stationLon) {
            this.stationLat = stationLat;
            this.stationLon = stationLon;
        }

        protected Double stationLat = null;

        protected Double stationLon = null;

        @Option(names={"-evt", "--event"}, arity="2", description="event latitude and longitude")
        void setEventLatLon(double eventLat, double eventLon) {
            this.eventLat = eventLat;
            this.eventLon = eventLon;
        }

        protected Double eventLat = null;

        protected Double eventLon = null;

        @Option(names = "--geodetic",
                description = "use geodetic latitude for distance calculations, which implies an ellipticity. Default is spherical. Note this only affects calculation of distance from lat/lon, all travel time calculations are done in a purely spherical model.")
        protected boolean geodetic = false;

        @Option(names= "--ellipflattening", description = "Elliptical flattening for distance calculations when --geodetic, defaults to WGS84 ~ 1/298.257")
        protected double ellipflattening = DistAz.wgs85_flattening;
    }
}
