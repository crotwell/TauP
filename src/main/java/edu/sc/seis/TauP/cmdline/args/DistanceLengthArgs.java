package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.DistanceRay;
import edu.sc.seis.TauP.RayCalculateable;
import edu.sc.seis.TauP.SeismicSource;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.cmdline.args.DistanceArgs.createListFromRangeDeg;
import static edu.sc.seis.TauP.cmdline.args.DistanceArgs.createListFromRangeKm;

public class DistanceLengthArgs {

    public List<RayCalculateable> getRayCalculatables(SeismicSourceArgs sourceArgs) {
        List<RayCalculateable> out = new ArrayList<>();
        out.addAll(getLengthDistances());
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


    public List<DistanceRay> getLengthDistances() {
        List<DistanceRay> simpleDistanceList = new ArrayList<>();
        for (Double d : degreesList) {
            simpleDistanceList.add(DistanceRay.ofDegrees(d));
        }

        if (!degreeRange.isEmpty()) {
            for (Double d : createListFromRangeDeg(degreeRange)) {
                simpleDistanceList.add(DistanceRay.ofDegrees(d));
            }
        }
        for (Double d : distKilometersList) {
            simpleDistanceList.add(DistanceRay.ofKilometers(d));
        }

        if (!kilometerRange.isEmpty()) {
            for (Double d : createListFromRangeKm(kilometerRange)) {
                simpleDistanceList.add(DistanceRay.ofKilometers(d));
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
