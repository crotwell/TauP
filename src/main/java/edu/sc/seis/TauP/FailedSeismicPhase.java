package edu.sc.seis.TauP;

import java.util.List;

/**
 * Represents a seismic phase that doesn't exist in the model, either malformed or not compatible with the broad
 * structure of the model.
 */
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
        this.proto.isFail = true;
    }

    public static FailedSeismicPhase failForReason(String phaseName, TauModel tMod, double receiverDepth, String failReason) {
        ProtoSeismicPhase proto = ProtoSeismicPhase.failNewPhase(tMod, true, true,
                receiverDepth, phaseName, failReason);
        FailedSeismicPhase fail = new FailedSeismicPhase(proto);
        return fail;
    }

    @Override
    public boolean phasesExistsInModel() {
        return false;
    }

    @Override
    public boolean hasArrivals() {
        return false;
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
        return getName()+":\n  Failed phase:\n"
                +"  "+proto.failReason
                +"\n"+ SeismicPhase.segmentDescribe(this);
    }

    @Override
    public double calcEnergyReflTranPSV(Arrival arrival) throws VelocityModelException, SlownessModelException {
        return 0;
    }

    @Override
    public double calcEnergyReflTranSH(Arrival arrival) throws VelocityModelException, SlownessModelException {
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
