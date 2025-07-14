package edu.sc.seis.TauP.cmdline;

import com.google.gson.Gson;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.*;
import edu.sc.seis.TauP.gson.GsonUtil;
import edu.sc.seis.seisFile.Location;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;

/**
 * Calc distance, az and baz for event lat,lon and station lat,lon pairs.
 */
@CommandLine.Command(name = "distaz",
        description = "Calc distance, az and baz for event lat,lon and station lat,lon pairs.",
        optionListHeading = OPTIONS_HEADING,
        usageHelpAutoWidth = true)
public class TauP_DistAz extends TauP_Tool {

    public TauP_DistAz() {
        super(new TextOutputTypeArgs(OutputTypes.TEXT, AbstractOutputTypeArgs.STDOUT_FILENAME));
        outputTypeArgs = (TextOutputTypeArgs)abstractOutputTypeArgs;
    }

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOutputFormat();
    }

    /**
     * preforms intialization of the tool. Properties are queried for the
     * default model to load, source depth to use, phases to use, etc.
     */
    @Override
    public void init() throws TauPException {
    }

    @Override
    public void start() throws IOException, TauPException {

        List<Location> eventLocs = new ArrayList<>();
        eventLocs.addAll(latLonArgs.getEventLocations());
        eventLocs.addAll(qmlStaxmlArgs.getEventLocations());

        List<Location> staList = new ArrayList<>();
        staList.addAll(latLonArgs.getStationLocations());
        staList.addAll(qmlStaxmlArgs.getStationLocations());

        List<DistanceAngleRay> distList = new ArrayList<>();

        if (latLonArgs.hasAzimuth()) {
            for (Location evtLoc : eventLocs) {
                String evtDesc = QmlStaxmlArgs.createDescription(evtLoc);

                for (Double d : createDistDegreeList()) {
                    double lat = SphericalCoords.latFor(evtLoc, d, latLonArgs.getAzimuth());
                    double lon = SphericalCoords.lonFor(evtLoc, d, latLonArgs.getAzimuth());
                    Location loc = new Location(lat, lon);
                    DistanceAngleRay dr = DistanceRay.ofEventStation(evtLoc, loc);
                    //dr.setAzimuth(latLonArgs.getAzimuth());
                    dr.setDescription(evtDesc+" to "+QmlStaxmlArgs.createDescription(loc));
                    distList.add(dr);
                }

            }
        }
        if (latLonArgs.hasBackAzimuth()) {
            for (Location staLoc : staList) {
                String staDesc = QmlStaxmlArgs.createDescription(staLoc);
                for (Double d : createDistDegreeList()) {
                    double lat = SphericalCoords.latFor(staLoc, d, latLonArgs.getBackAzimuth());
                    double lon = SphericalCoords.lonFor(staLoc, d, latLonArgs.getBackAzimuth());
                    Location loc = new Location(lat, lon);
                    DistanceAngleRay dr = DistanceRay.ofEventStation(loc, staLoc);
                    //dr.setBackAzimuth(latLonArgs.getBackAzimuth());
                    dr.setDescription(QmlStaxmlArgs.createDescription(loc)+" to "+staDesc);
                    distList.add(dr);
                }
            }
        }
        for (Location evtLoc : eventLocs) {
            for (Location staLoc : staList) {
                DistanceAngleRay dr;
                if (geodeticArgs.isGeodetic()) {
                    dr = DistanceRay.ofGeodeticEventStation(evtLoc, staLoc, geodeticArgs.getInverseEllipFlattening());
                } else {
                    dr = DistanceRay.ofEventStation(evtLoc, staLoc);
                }
                String staDesc = QmlStaxmlArgs.createDescription(staLoc);
                String evtDesc = QmlStaxmlArgs.createDescription(evtLoc);

                dr.setDescription(evtDesc+" to "+staDesc);
                distList.add(dr);
            }
        }
        distList.sort(Comparator.comparingDouble(DistanceAngleRay::getDegrees));
        List<Daz> dazList = new ArrayList<>();
        double radius = radiusArgs.getRadiusOfEarth() != null ? radiusArgs.getRadiusOfEarth() : 6371.0;
        for (DistanceAngleRay ray : distList) {
            dazList.add(new Daz(ray, radius));
        }
        PrintWriter  out = outputTypeArgs.createWriter(spec.commandLine().getOut());
        if (outputTypeArgs.isText()) {
            String geoditic = geodeticArgs.isGeodetic() ? "Geodetic "+geodeticArgs.getInverseEllipFlattening() : "Spherical";
            out.println("Degrees      Km     Azimuth  BackAzimuth  Description   ("+geoditic+")  ");
            out.println("----------------------------------------------------------------------");
            for (Daz dr : dazList) {
                out.println(Outputs.formatDistance(dr.getDegrees())
                        +" "+Outputs.formatKilometer (dr.getKilometers())
                        +" "+Outputs.formatDistance(dr.getNormalizedAzimuth())
                        +"  "+Outputs.formatDistance(dr.getNormalizedBackAzimuth())
                        +"      "+(dr.hasDescription() ? dr.getDescription() : "")
                );
            }
        } else if (outputTypeArgs.isHTML()) {
            String geoditic = geodeticArgs.isGeodetic() ? "Geodetic "+geodeticArgs.getInverseEllipFlattening() : "Spherical";
            List<String> head = List.of("Degrees","Km","Azimuth","BackAzimuth","Description   ("+geoditic+")  ");
            List<List<String>> values = new ArrayList<>();
            for (Daz dr : dazList) {
                List<String> row = List.of(Outputs.formatDistance(dr.getDegrees()),
                        Outputs.formatKilometer (dr.getKilometers()),
                        Outputs.formatDistance(dr.getNormalizedAzimuth()),
                        Outputs.formatDistance(dr.getNormalizedBackAzimuth()),
                        (dr.hasDescription() ? dr.getDescription() : "")
                );
                values.add(row);
            }
            HTMLUtil.createHtmlStart(out, "TauP Distaz", HTMLUtil.createTableCSS(), false);
            out.println(HTMLUtil.createBasicTable(head, values));
            HTMLUtil.addSortTableJS(out);
            out.println(HTMLUtil.createHtmlEnding());
        } else if (outputTypeArgs.isJSON()){
            Result result = new Result();
            result.calctype = geodeticArgs.getCalcType();
            if (geodeticArgs.isGeodetic()) {
                result.invflattening = geodeticArgs.getInverseEllipFlattening();
            }
            result.radius = radius;
            if (radiusArgs.modelName != null) {
                result.model = radiusArgs.getModelName();
            }
            result.sources = eventLocs;
            result.receivers = staList;
            result.distances = dazList;
            Gson gson = GsonUtil.createGsonBuilder().create();
            out.println(gson.toJson(result));
        }
        out.flush();
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void validateArguments() throws TauPException {
        if (!distArgs.allEmpty() && ! (latLonArgs.hasAzimuth() || latLonArgs.hasBackAzimuth())) {
            throw new IllegalArgumentException("Distance only used with azimuth or backazimuth");
        }
        if (distArgs.allEmpty() && (latLonArgs.hasAzimuth() || latLonArgs.hasBackAzimuth())) {
            throw new IllegalArgumentException("Azimuth and backazimuth require distance in deg or km");
        }
        if (geodeticArgs.isGeodetic() && (latLonArgs.hasBackAzimuth() || latLonArgs.hasAzimuth())) {
            throw new IllegalArgumentException("Unable to project for az,baz with geodetic, only for spherical");
        }
        if ( (latLonArgs.getEventLocations().isEmpty() && ! qmlStaxmlArgs.hasQml()) && !latLonArgs.hasBackAzimuth()) {
            throw new IllegalArgumentException("Either back azimuth, event lat,lon or QuakeML file must be given");
        }
        if ( (latLonArgs.getStationLocations().isEmpty() && ! qmlStaxmlArgs.hasStationXML()) && !latLonArgs.hasAzimuth()) {
            throw new IllegalArgumentException("Either azimuth, station lat,lon or StationXML file must be given");
        }
        latLonArgs.validateArguments();
        geodeticArgs.validateArguments();
    }

    public List<Double> createDistDegreeList() {
        List<Double> allDeg = new ArrayList<>(distArgs.degreesList);
        if (!distArgs.degreeRange.isEmpty()) {
            allDeg.addAll(DistanceArgs.createListFromRange(distArgs.degreeRange, 10, 180, 10));
        }
        List<Double> allKm = new ArrayList<>(distArgs.distKilometersList);
        if (!distArgs.kilometerRange.isEmpty()) {
            allKm.addAll(DistanceArgs.createListFromRange(distArgs.kilometerRange, 100, 1000, 100));
        }
        if (!allKm.isEmpty()) {
            double kmToDeg = kmToDeg();
            for (Double km : allKm) {
                allDeg.add(km * kmToDeg);
            }
        }
        return allDeg;
    }

    public Double kmToDeg() {
        double r = radiusArgs.getRadiusOfEarth() != null ? radiusArgs.getRadiusOfEarth() : 6371.0;
        return  180.0 / Math.PI / r; // default radius
    }

    @CommandLine.Mixin
    TextOutputTypeArgs outputTypeArgs;

    @CommandLine.Mixin
    LatLonAzBazArgs latLonArgs = new LatLonAzBazArgs();

    @CommandLine.Mixin
    QmlStaxmlArgs qmlStaxmlArgs = new QmlStaxmlArgs();

    @CommandLine.Mixin
    GeodeticArgs geodeticArgs = new GeodeticArgs();

    @CommandLine.ArgGroup(exclusive = false, multiplicity = "0..*", heading = "Optional distance is given by:%n")
    DistanceLengthArgs distArgs = new DistanceLengthArgs();

    @CommandLine.ArgGroup(exclusive = true)
    ModelOrRadius radiusArgs = new ModelOrRadius();

}

class ModelOrRadius {

    public Double getRadiusOfEarth() {
        return radiusOfEarth;
    }

    @CommandLine.Option(names = "--radius",
            defaultValue = "6371",
            description = "radius of earth in km, used when distance given in km")
    protected Double radiusOfEarth = null;

    @CommandLine.Option(names={"--mod", "--model"},
            description = {"use velocity model \"modelName\" for radius, used when distance given in km. "}
    )
    public void setModelName(String modelName) throws TauModelException {
        if (modelName != null) {
            TauModel tMod = TauModelLoader.load(modelName);
            this.radiusOfEarth = tMod.getRadiusOfEarth();
        }
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }

    String modelName = null;

    static Properties toolProps = TauP_Tool.configDefaults();
}

class Result {
    double radius;
    String model = null;
    String calctype;
    Double invflattening = null;
    List<Location> sources;
    List<Location> receivers;
    List<Daz> distances;
}