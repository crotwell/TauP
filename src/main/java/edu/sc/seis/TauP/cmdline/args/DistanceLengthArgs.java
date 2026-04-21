package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.*;
import net.sf.geographiclib.Geodesic;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static edu.sc.seis.TauP.cmdline.args.DistanceArgs.createListFromRangeDeg;
import static edu.sc.seis.TauP.cmdline.args.DistanceArgs.createListFromRangeKm;

public class DistanceLengthArgs {

    public DistanceArgs createDistanceArgs(GeodeticArgs geodeticArgs, QmlStaxmlArgs qmlStaxmlArgs, List<Double> sourceDepths, List<Double> receiverDepths) {
        ModelArgs tmpModelArgs = new ModelArgs();
        tmpModelArgs.setSourceDepths(sourceDepths);
        tmpModelArgs.setReceiverDepths(receiverDepths);

        DistanceArgs out = new DistanceArgs(tmpModelArgs);
        out.distArgs = new DistanceRayArgs();
        out.distArgs.degreeRange = degreeRange;
        out.distArgs.degreesList = degreesList;
        out.distArgs.distKilometersList = distKilometersList;
        out.distArgs.kilometerRange = kilometerRange;
        out.geodeticArgs = geodeticArgs;
        out.qmlStaxmlArgs = qmlStaxmlArgs;
        return out;
    }

    public List<RayCalculateable> getRayCalculatables(SeismicSourceArgs sourceArgs, GeodeticArgs geodeticArgs, ModelArgs modelArgs) throws TauModelException {
        List<RayCalculateable> out = new ArrayList<>();
        Map<GeoDistType, Geodesic> geodesicMap = geodeticArgs.createGeodesics(modelArgs.getTauModel().getVelocityModel());
        out.addAll(getLengthDistances(geodesicMap));
        if (sourceArgs != null) {
            SeismicSource ss = new SeismicSource(sourceArgs.getMw(), sourceArgs.getFaultPlane());
            for (RayCalculateable rc : out) {
                if (!rc.hasSeismicSource()) {
                    rc.setSeismicSource(ss);
                }
            }
        }
        return out;
    }


    public List<DistanceRay> getLengthDistances(Map<GeoDistType, Geodesic> geodesicMap) {
        List<DistanceRay> simpleDistanceList = new ArrayList<>();
        for (Double d : degreesList) {
            for (GeoDistType geoDistType : geodesicMap.keySet()) {
                simpleDistanceList.add(DistanceRay.ofDegrees(d, geoDistType, geodesicMap.get(geoDistType)));
            }
        }

        if (!degreeRange.isEmpty()) {
            for (Double d : createListFromRangeDeg(degreeRange)) {
                for (GeoDistType geoDistType : geodesicMap.keySet()) {
                    simpleDistanceList.add(DistanceRay.ofDegrees(d, geoDistType, geodesicMap.get(geoDistType)));
                }
            }
        }
        for (Double d : distKilometersList) {
            for (GeoDistType geoDistType : geodesicMap.keySet()) {
                simpleDistanceList.add(DistanceRay.ofKilometers(d, geoDistType, geodesicMap.get(geoDistType)));
            }
        }

        if (!kilometerRange.isEmpty()) {
            for (Double d : createListFromRangeKm(kilometerRange)) {
                for (GeoDistType geoDistType : geodesicMap.keySet()) {
                    simpleDistanceList.add(DistanceRay.ofKilometers(d, geoDistType, geodesicMap.get(geoDistType)));
                }
            }
        }
        return simpleDistanceList;
    }

    @CommandLine.Option(names = {"--deg", "--degree"},
            paramLabel = "d",
            description = "distance in degrees", split = ",")
    public List<Double> degreesList = new ArrayList<>();

    @CommandLine.Option(names = {"--degreerange"},
            arity = "1..3",
            paramLabel =  "[step][min max][min max step]",
            hideParamSyntax = true,
            description = "regular distance range in degrees, one of step; min max or min max step. "
                    + "Default min is 0, max is 180 and step is 10.")
    public List<Double> degreeRange = new ArrayList<>();

    /**
     * For when command line args uses --km for distance. Have to wait until
     * after the model is read in to get radius of earth.
     */
    @CommandLine.Option(names = {"--km", "--kilometer"},
            paramLabel = "km",
            description = "distance in kilometers along surface.", split = ",")
    public List<Double> distKilometersList = new ArrayList<>();

    @CommandLine.Option(names = {"--kilometerrange"},
            arity = "1..3",
            paramLabel =  "[step][min max][min max step]",
            hideParamSyntax = true,
            description = "regular distance range in kilometers, one of step; min max or min max step. "
                    + "Default min is 0, max is 1000 and step is 100.")
    public List<Double> kilometerRange = new ArrayList<>();

    public boolean allEmpty() {
        return degreesList.isEmpty()
                && distKilometersList.isEmpty()
                && degreeRange.isEmpty()
                && kilometerRange.isEmpty();
    }
}
