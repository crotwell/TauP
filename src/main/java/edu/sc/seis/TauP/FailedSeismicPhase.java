package edu.sc.seis.TauP;

import java.util.List;

public class FailedSeismicPhase extends SimpleSeismicPhase {

    public FailedSeismicPhase(ProtoSeismicPhase proto) {
        super(proto,
        new double[0] ,
                new double[0] ,
                new double[0] ,
        -1,
        -1,
        -1,
        -1,
        0,
        0,
        false);
    }

    @Override
    public boolean phasesExistsInModel() {
        return false;
    }

    @Override
    public boolean hasArrivals() {
        return false;
    }

    @Override
    public List<Arrival> calcTime(double deg) {
        return List.of();
    }

    /**
     * Creates an Arrival for a sampled ray parameter from the model. No interpolation between rays as this is a sample.
     *
     * @param rayNum index in ray parameters
     */
    @Override
    public Arrival createArrivalAtIndex(int rayNum) {
        return null;
    }

    @Override
    public Arrival shootRay(double rayParam) throws SlownessModelException, NoSuchLayerException {
        return null;
    }

    @Override
    public List<ArrivalPathSegment> calcSegmentPaths(Arrival currArrival) {
        return List.of();
    }

    @Override
    public String describe() {
        return "Failed phase "+getName()+": "+proto.failReason+"\n"
                +super.describe();
    }

    @Override
    public String describeJson() {
        return "";
    }


    @Override
    public double calcReflTranPSV(Arrival arrival) throws VelocityModelException, SlownessModelException {
        return 0;
    }

    @Override
    public double calcReflTranSH(Arrival arrival) throws VelocityModelException, SlownessModelException {
        return 0;
    }

    @Override
    public List<TimeDist> calcPierceTimeDist(Arrival arrival) {
        return List.of();
    }

    @Override
    public int getNumRays() {
        return 0;
    }
}
