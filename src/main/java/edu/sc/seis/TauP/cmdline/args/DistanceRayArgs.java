package edu.sc.seis.TauP.cmdline.args;

import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

class DistanceRayArgs extends DistanceLengthArgs {

    @CommandLine.Option(names = {"--exactdegree"},
            paramLabel = "d",
            description = "exact distance traveled in degrees, not 360-d", split = ",")
    public List<Double> exactDegreesList = new ArrayList<>();

    /**
     * Exact km, no mod 360
     */
    @CommandLine.Option(names = {"--exactkilometer"},
            paramLabel = "km",
            description = "exact distance traveled in kilometers, not 360-k", split = ",")
    public List<Double> exactDistKilometersList = new ArrayList<>();


    @CommandLine.Option(names = "--takeoff",
            split = ",",
            paramLabel = "deg",
            description = "takeoff angle in degrees from the source, zero is down, 90 horizontal, 180 is up.")
    protected List<Double> takeoffAngle = new ArrayList<>();


    @CommandLine.Option(names = {"--takeoffrange"},
            paramLabel = "k",
            arity = "1..3",
            description = "regular range in takeoff angle in degrees, one of max min,max or min,max,step. "
                    + "Default min is 0 and step is 10.",
            split = ",")
    protected List<Double> takeoffRange = new ArrayList<>();

    @CommandLine.Option(names = {"--rayparamrad"},
            paramLabel = "s/rad",
            description = "ray parameter from the source in s/rad, up or down is determined by the phase",
            split = ",")
    protected List<Double> shootRadianRaypList = new ArrayList<>();

    @CommandLine.Option(names = {"--rayparamdeg"},
            paramLabel = "s/deg",
            description = "ray parameter from the source in s/deg, up or down is determined by the phase",
            split = ",")
    protected List<Double> shootRaypList = new ArrayList<>();

    @CommandLine.Option(names = {"--rayparamkm"},
            paramLabel = "s/km",
            description = "ray parameter from the source in s/km, up or down is determined by the phase",
            split = ",")
    protected List<Double> shootKmRaypList = new ArrayList<>();

    @CommandLine.Option(names = {"--rayparamidx"},
            paramLabel = "i",
            description = "ray parameter from the source as index into model sampling, up or down is determined by the phase",
            split = ",")
    protected List<Integer> shootIndexRaypList = new ArrayList<>();

    @Override
    public boolean allEmpty() {
        return super.allEmpty()
                && exactDegreesList.isEmpty()
                && exactDistKilometersList.isEmpty()
                && shootIndexRaypList.isEmpty()
                && shootKmRaypList.isEmpty()
                && shootRadianRaypList.isEmpty()
                && shootRaypList.isEmpty()
                && takeoffAngle.isEmpty()
                && takeoffRange.isEmpty();
    }
}
