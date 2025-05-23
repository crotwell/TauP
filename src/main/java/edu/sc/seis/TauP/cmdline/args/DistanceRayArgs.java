package edu.sc.seis.TauP.cmdline.args;

import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

public class DistanceRayArgs extends DistanceLengthArgs {

    @CommandLine.Option(names = {"--exactdegree"},
            paramLabel = "d",
            description = "exact distance traveled in degrees, not 360-d", split = ",")
    public List<Double> exactDegreesList = new ArrayList<>();


    @CommandLine.Option(names = {"--exactdegreerange"},
            arity = "1..3",
            paramLabel =  "[step][min max][min max step]",
            hideParamSyntax = true,
            description = "regular distance range in exact degrees, not 360-deg, one of step; min max or min max step. "
                    + "Default min is 0, max is 180 and step is 10.")
    public List<Double> exactDegreeRange = new ArrayList<>();

    /**
     * Exact km, no mod 360
     */
    @CommandLine.Option(names = {"--exactkilometer"},
            paramLabel = "km",
            description = "exact distance traveled in kilometers, not 360-k", split = ",")
    public List<Double> exactDistKilometersList = new ArrayList<>();

    @CommandLine.Option(names = {"--exactkilometerrange"},
            arity = "1..3",
            paramLabel =  "[step][min max][min max step]",
            hideParamSyntax = true,

            description = "regular distance range in kilometers, not 360-k, one of step; min max or min max step. "
                    + "Default min is 0, max is 1000 and step is 100.")
    public List<Double> exactKilometerRange = new ArrayList<>();


    @CommandLine.Option(names = "--takeoff",
            split = ",",
            paramLabel = "deg",
            description = "takeoff angle in degrees from the source, zero is down, 90 horizontal, 180 is up.")
    public List<Double> takeoffAngle = new ArrayList<>();


    @CommandLine.Option(names = {"--takeoffrange"},
            arity = "1..3",
            paramLabel =  "[step][min max][min max step]",
            hideParamSyntax = true,
            description = "regular range in takeoff angle in degrees, one of step; min,max or min,max,step. "
                    + "Default min is 0 and step is 10.")
    public List<Double> takeoffRange = new ArrayList<>();

    @CommandLine.Option(names = "--incident",
            split = ",",
            paramLabel = "deg",
            description = "incident angle in degrees at the receiver, zero is down, 90 horizontal, 180 is up.")
    public List<Double> incidentAngle = new ArrayList<>();


    @CommandLine.Option(names = {"--incidentrange"},
            arity = "1..3",
            paramLabel =  "[step][min max][min max step]",
            hideParamSyntax = true,
            description = "regular range in incident angle in degrees, one of step; min max or min max step. "
                    + "Default min is 0 and step is 10."
            )
    public List<Double> incidentRange = new ArrayList<>();

    @CommandLine.Option(names = {"--rayparamrad"},
            paramLabel = "s/rad",
            description = "ray parameter from the source in s/rad, up or down is determined by the phase",
            split = ",")
    public List<Double> shootRadianRaypList = new ArrayList<>();

    @CommandLine.Option(names = {"--rayparamdeg"},
            paramLabel = "s/deg",
            description = "ray parameter from the source in s/deg, up or down is determined by the phase",
            split = ",")
    public List<Double> shootRaypList = new ArrayList<>();

    @CommandLine.Option(names = {"--rayparamkm"},
            paramLabel = "s/km",
            description = "ray parameter from the source in s/km, up or down is determined by the phase",
            split = ",")
    public List<Double> shootKmRaypList = new ArrayList<>();

    @CommandLine.Option(names = {"--rayparamidx"},
            paramLabel = "i",
            description = "ray parameter from the source as index into model sampling, up or down is determined by the phase",
            split = ",")
    public List<Integer> shootIndexRaypList = new ArrayList<>();

    @CommandLine.Option(names = {"--allindex"},
            description = "all arrivals at sampling of model"
    )
    public boolean allIndexRays = false;

    @Override
    public boolean allEmpty() {
        return super.allEmpty()
                && exactDegreesList.isEmpty()
                && exactDegreeRange.isEmpty()
                && exactDistKilometersList.isEmpty()
                && exactKilometerRange.isEmpty()
                && shootIndexRaypList.isEmpty()
                && shootKmRaypList.isEmpty()
                && shootRadianRaypList.isEmpty()
                && shootRaypList.isEmpty()
                && takeoffAngle.isEmpty()
                && takeoffRange.isEmpty()
                && incidentAngle.isEmpty()
                && incidentRange.isEmpty();
    }
}
