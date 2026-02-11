package edu.sc.seis.TauP.cmdline;

import com.google.gson.Gson;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.*;
import edu.sc.seis.TauP.gson.GsonUtil;
import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.Geodesic;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

        List<LatLonLocatable> eventLocs = new ArrayList<>();
        eventLocs.addAll(geodeticArgs.getEventLocations());
        eventLocs.addAll(qmlStaxmlArgs.getEventLocations());

        List<LatLonLocatable> staList = new ArrayList<>();
        staList.addAll(geodeticArgs.getStationLocations());
        staList.addAll(qmlStaxmlArgs.getStationLocations());

        List<DistanceRay> distList = new ArrayList<>();

        Geodesic geodesic = null;
        if (geodeticArgs.isGeodetic()) {
            geodesic = geodeticArgs.getGeodesic();
        }
        DistanceArgs distanceArgs = distArgs.createDistanceArgs(geodeticArgs, qmlStaxmlArgs);
        List<RayCalculateable> rayList  = distanceArgs.getRayCalculatables(radiusArgs.getRadiusOfEarth());
        for (RayCalculateable ray : rayList) {
            if (ray instanceof DistanceRay) {
                distList.add((DistanceRay) ray);
            } else {
                throw new TauPException("Only DistanceRay allowed in TauP_DistAz: "+ray);
            }
        }

        distList.sort(Comparator.comparingDouble(DistanceRay::getDegrees));
        List<Daz> dazList = new ArrayList<>();
        double radius;
        if (geodeticArgs.isGeodetic()) {
            radius = DistAzKarney.averageRadiusKm(geodeticArgs.getGeodesic());
        } else {
            radius = radiusArgs.getRadiusOfEarth() != null ? radiusArgs.getRadiusOfEarth() : 6371.0;
        }
        for (DistanceRay ray : distList) {
            dazList.add(new Daz(ray));
        }
        PrintWriter  out = outputTypeArgs.createWriter(spec.commandLine().getOut());
        if (outputTypeArgs.isText()) {
            String geoditic = geodeticArgs.isGeodetic() ? "Geodetic "+geodeticArgs.getInverseEllipFlattening() : "Spherical";
            out.println("Degrees      Km     Azimuth  BackAz    Source    Receiver      Description   ("+geoditic+")  ");
            out.println("---------------------------------------------------------------------------------------------");
            for (Daz dr : dazList) {
                out.println(Outputs.formatDistance(dr.getDegrees())
                        +" "+Outputs.formatKilometer (dr.getKilometers())
                        +" "+Outputs.formatDistance(dr.getNormalizedAzimuth())
                        +"  "+Outputs.formatDistance(dr.getNormalizedBackAzimuth())
                        +" "+dr.getSource().getLocationDescription()
                        +" "+dr.getReceiver().getLocationDescription()
                        +"      "+(dr.hasDescription() ? dr.getDescription() : "")
                );
            }
        } else if (outputTypeArgs.isHTML()) {
            String geoditic = geodeticArgs.isGeodetic() ? "Geodetic "+geodeticArgs.getInverseEllipFlattening() : "Spherical";
            List<String> head = List.of("Degrees","Km","Azimuth","BackAzimuth","Source","Receiver","Description   ("+geoditic+")  ");
            List<List<String>> values = new ArrayList<>();
            for (Daz dr : dazList) {
                List<String> row = List.of(Outputs.formatDistance(dr.getDegrees()),
                        Outputs.formatKilometer (dr.getKilometers()),
                        Outputs.formatDistance(dr.getNormalizedAzimuth()),
                        Outputs.formatDistance(dr.getNormalizedBackAzimuth()),
                        dr.getSource().getLocationDescription(),
                        dr.getReceiver().getLocationDescription(),
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
            result.sources = eventLocs.stream().map(LatLonLocatable::asLocation).collect(Collectors.toList());
            result.receivers = staList.stream().map(LatLonLocatable::asLocation).collect(Collectors.toList());
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
        if (!distArgs.allEmpty() && ! (geodeticArgs.hasAzimuth() || geodeticArgs.hasBackAzimuth())) {
            throw new IllegalArgumentException("Distance only used with azimuth or backazimuth");
        }
        if (distArgs.allEmpty() && (geodeticArgs.hasAzimuth() || geodeticArgs.hasBackAzimuth())) {
            throw new IllegalArgumentException("Azimuth and backazimuth require distance in deg or km");
        }
        if ( (geodeticArgs.getEventLocations().isEmpty() && ! qmlStaxmlArgs.hasQml()) && !geodeticArgs.hasBackAzimuth()) {
            throw new IllegalArgumentException("Either back azimuth, event lat,lon or QuakeML file must be given");
        }
        if ( (geodeticArgs.getStationLocations().isEmpty() && ! qmlStaxmlArgs.hasStationXML()) && !geodeticArgs.hasAzimuth()) {
            throw new IllegalArgumentException("Either azimuth, station lat,lon or StationXML file must be given");
        }
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
        double r;
        if (geodeticArgs.isGeodetic()) {
            r = DistAzKarney.averageRadiusKm(geodeticArgs.getGeodesic());
        } else {
            r = radiusArgs.getRadiusOfEarth() != null ? radiusArgs.getRadiusOfEarth() : 6371.0;
        }
        return  180.0 / Math.PI / r; // default radius
    }

    @CommandLine.Mixin
    TextOutputTypeArgs outputTypeArgs;

    @CommandLine.Mixin
    QmlStaxmlArgs qmlStaxmlArgs = new QmlStaxmlArgs();

    @CommandLine.Mixin
    GeodeticArgs geodeticArgs = new GeodeticArgs();

    @CommandLine.ArgGroup(exclusive = false, multiplicity = "0..*", heading = "Optional distance is given by:%n")
    DistanceLengthArgs distArgs = new DistanceLengthArgs();

    @CommandLine.ArgGroup()
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