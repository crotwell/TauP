package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.DistanceRay;
import edu.sc.seis.TauP.Outputs;
import edu.sc.seis.TauP.SphericalCoords;
import edu.sc.seis.TauP.TauPException;
import edu.sc.seis.TauP.cmdline.args.*;
import edu.sc.seis.seisFile.Location;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
        double kmToDeg = 180.0 / Math.PI * radiusOfEarth;

        List<Location> eventLocs = new ArrayList<>();
        eventLocs.addAll(latLonArgs.getEventLocations());
        eventLocs.addAll(qmlStaxmlArgs.getEventLocations());

        List<Location> staList = new ArrayList<>();
        staList.addAll(latLonArgs.getStationLocations());
        staList.addAll(qmlStaxmlArgs.getStationLocations());

        List<DistanceRay> distList = new ArrayList<>();

        if (latLonArgs.hasAzimuth()) {
            for (Location evtLoc : eventLocs) {
                String evtDesc = QmlStaxmlArgs.createDescription(evtLoc);

                for (Double d : createDistDegreeList()) {
                    double lat = SphericalCoords.latFor(evtLoc, d, latLonArgs.getAzimuth());
                    double lon = SphericalCoords.lonFor(evtLoc, d, latLonArgs.getAzimuth());
                    Location loc = new Location(lat, lon);
                    DistanceRay dr = DistanceRay.ofEventStation(evtLoc, loc);
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
                    DistanceRay dr = DistanceRay.ofEventStation(loc, staLoc);
                    //dr.setBackAzimuth(latLonArgs.getBackAzimuth());
                    dr.setDescription(QmlStaxmlArgs.createDescription(loc)+" to "+staDesc);
                    distList.add(dr);
                }
            }
        }
        for (Location evtLoc : eventLocs) {
            for (Location staLoc : staList) {
                DistanceRay dr;
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
        distList.sort((lhs, rhs) -> {
            // radus only used if DistanceRay created with km, so doesn't
            // matter in this case
            return Double.compare(rhs.getDegrees(1), lhs.getDegrees(1));
        });
        PrintWriter  out = outputTypeArgs.createWriter(spec.commandLine().getOut());
        if (outputTypeArgs.isText()) {
            String geoditic = geodeticArgs.isGeodetic() ? "Geodetic "+geodeticArgs.getInverseEllipFlattening() : "Spherical";
            out.println("Degrees      Km     Azimuth  BackAzimuth  Description   ("+geoditic+")  ");
            out.println("----------------------------------------------------------------------");
            for (DistanceRay dr : distList) {
                out.println(Outputs.formatDistance(dr.getDegrees(1))
                        +" "+Outputs.formatKilometer (dr.getKilometers(radiusOfEarth))
                        +" "+Outputs.formatDistance(dr.getNormalizedAzimuth())
                        +"  "+Outputs.formatDistance(dr.getNormalizedBackAzimuth())
                        +"      "+(dr.hasDescription() ? dr.getDescription() : "")
                );
            }
        } else {
            out.println("{");
            out.println("  \"calctype\": \""+(geodeticArgs.isGeodetic()?"geodetic":"spherical")+"\",");
            out.println("  \"invflattening\": "+(geodeticArgs.isGeodetic()?geodeticArgs.getInverseEllipFlattening():"0")+",");
            out.println("  \"events\": [");
            boolean isFirst = true;
            for (Location evtLoc : eventLocs) {
                if ( ! isFirst) {
                    out.println("    },");
                }
                isFirst = false;
                out.println("    {");
                out.println("      \"lat\": "+Outputs.formatLatLon(evtLoc.getLatitude()).trim()+",");
                out.println("      \"lon\": "+Outputs.formatLatLon(evtLoc.getLongitude()).trim()+",");
                String evtDesc = evtLoc.getDescription()==null?"":evtLoc.getDescription().trim();
                out.println("      \"desc\": \""+evtDesc+"\"");
            }
            out.println("    }");
            out.println("  ],");
            out.println("  \"stations\": [");
            isFirst = true;
            for (Location staLoc : staList) {
                if ( ! isFirst) {
                    out.println("    },");
                }
                isFirst = false;
                out.println("    {");
                out.println("      \"lat\": "+Outputs.formatLatLon(staLoc.getLatitude()).trim()+",");
                out.println("      \"lon\": "+Outputs.formatLatLon(staLoc.getLongitude()).trim()+",");
                String staDesc = staLoc.getDescription()==null?"":staLoc.getDescription().trim();
                out.println("      \"desc\": \""+staDesc+"\"");
            }
            out.println("    }");
            out.println("  ],");
            out.println("  \"distances\": [");
            isFirst = true;
            for (DistanceRay dr : distList) {
                if ( ! isFirst) {
                    out.println("    },");
                }
                isFirst = false;
                out.println("    {");
                out.println("      \"deg\": "+Outputs.formatDistance(dr.getDegrees(1))+",");
                out.println("      \"az\": "+Outputs.formatDistance(dr.getAzimuth())+",");
                out.println("      \"baz\": "+Outputs.formatDistance(dr.getBackAzimuth())+",");
                out.println("      \"desc\": \""+(dr.hasDescription() ? dr.getDescription() : "")+"\"");
            }
            out.println("    }");
            out.println("  ]");

            out.println("}");
        }
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
        double r = radiusOfEarth != null ? radiusOfEarth : 6371.0;
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


    @CommandLine.Option(names = "--radius",
            defaultValue = "6371",
            description = "radius of earth in km, used when distance given in km")
    protected Double radiusOfEarth = null;

}
