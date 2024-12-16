package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.DistAz;
import edu.sc.seis.TauP.DistanceRay;
import edu.sc.seis.TauP.Outputs;
import edu.sc.seis.TauP.TauPException;
import edu.sc.seis.TauP.cmdline.args.*;
import edu.sc.seis.seisFile.Location;
import edu.sc.seis.seisFile.fdsnws.quakeml.Event;
import edu.sc.seis.seisFile.fdsnws.quakeml.Magnitude;
import edu.sc.seis.seisFile.fdsnws.quakeml.Origin;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.ABREV_SYNOPSIS;
import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;
import static edu.sc.seis.seisFile.TimeUtils.TZ_UTC;

/**
 * Calc distance, az and baz for event lat,lon and station lat,lon pairs.
 */
@CommandLine.Command(name = "distaz",
        description = "Calc distance, az and baz for event lat,lon and station lat,lon pairs.",
        optionListHeading = OPTIONS_HEADING,
        abbreviateSynopsis = ABREV_SYNOPSIS,
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

        List<DistanceRay> distList = new ArrayList<>();
        for (Location evtLoc : eventLocs) {
            for (Location staLoc : staList) {
                DistanceRay dr;
                if (geodeticArgs.isGeodetic()) {
                    dr = DistanceRay.ofGeodeticStationEvent(staLoc, evtLoc, geodeticArgs.getInverseEllipFlattening());
                } else {
                    dr = DistanceRay.ofStationEvent(staLoc, evtLoc);
                }
                String staDesc = "";
                if (staLoc.hasDescription()) {
                    staDesc = staLoc.getDescription();
                } else {
                    staDesc = Outputs.formatLatLon(staLoc.getLatitude()).trim()
                            +"/"+Outputs.formatLatLon(staLoc.getLongitude()).trim();
                }
                String evtDesc = "";
                if (evtLoc.hasDescription()) {
                    evtDesc = evtLoc.getDescription();
                } else {
                    evtDesc = Outputs.formatLatLon(evtLoc.getLatitude()).trim()
                            +"/"+Outputs.formatLatLon(evtLoc.getLongitude()).trim();
                }
                dr.setDescription(staDesc+"  "+evtDesc);
                distList.add(dr);
            }
        }
        Collections.sort(distList, new Comparator<DistanceRay>() {
            @Override
            public int compare(DistanceRay lhs, DistanceRay rhs) {
                // radus only used if DistanceRay created with km, so doesn't
                // matter in this case
                if (lhs.getDegrees(1) > rhs.getDegrees(1)) {
                    return -1;
                } else if (lhs.getDegrees(1) < rhs.getDegrees(1)) {
                    return 1;
                } else{
                    return 0;
                }
            }
        });
        PrintWriter  out = outputTypeArgs.createWriter(spec.commandLine().getOut());
        if (outputTypeArgs.isText()) {
            String geoditic = geodeticArgs.isGeodetic() ? "Geodetic "+geodeticArgs.getInverseEllipFlattening() : "Spherical";
            out.println("Degrees   Azimuth  BackAzimuth  ("+geoditic+")");
            out.println("-------------------------------");
            for (DistanceRay dr : distList) {
                out.println(Outputs.formatDistance(dr.getDegrees(1))
                        +" "+Outputs.formatDistance(dr.getAzimuth())
                        +" "+Outputs.formatDistance(dr.getBackAzimuth())
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
        if ( (latLonArgs.getEventLocations().isEmpty() && ! qmlStaxmlArgs.hasQml())) {
            throw new IllegalArgumentException("Either event lat,lon or QuakeML file must be given");
        }
        if ( (latLonArgs.getStationLocations().isEmpty() && ! qmlStaxmlArgs.hasStationXML())) {
            throw new IllegalArgumentException("Either station lat,lon or StationXML file must be given");
        }
    }


    @CommandLine.Mixin
    TextOutputTypeArgs outputTypeArgs;

    @CommandLine.Mixin
    LatLonAzBazArgs latLonArgs = new LatLonAzBazArgs();

    @CommandLine.Mixin
    QmlStaxmlArgs qmlStaxmlArgs = new QmlStaxmlArgs();

    @CommandLine.Mixin
    GeodeticArgs geodeticArgs = new GeodeticArgs();
}
