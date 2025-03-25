package edu.sc.seis.TauP.cmdline.args;

import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

public class DistanceLengthArgs {

    @CommandLine.Option(names = {"--deg", "--degree"},
            paramLabel = "d",
            description = "distance in degrees", split = ",")
    public List<Double> degreesList = new ArrayList<>();

    @CommandLine.Option(names = {"--degreerange"},
            paramLabel = "d",
            arity = "1..3",
            description = "regular distance range in degrees, one of step; min,max or min,max,step. "
                    + "Default min is 0, max is 180 and step is 10.",
            split = ",")
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
            paramLabel = "k",
            arity = "1..3",
            description = "regular distance range in kilometers, one of step; min,max or min,max,step. "
                    + "Default min is 0, max is 1000 and step is 100.",
            split = ",")
    public List<Double> kilometerRange = new ArrayList<>();

    public boolean allEmpty() {
        return degreesList.isEmpty()
                && distKilometersList.isEmpty()
                && degreeRange.isEmpty()
                && kilometerRange.isEmpty();
    }
}
