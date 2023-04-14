package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScatteredSeismicPhase implements SeismicPhase {

    private final Arrival inboundArrival;
    private final SeismicPhase scatteredPhase;
    private final double scatterDepth;
    private final double scatterDistance;

    public ScatteredSeismicPhase(Arrival inboundArrival, SeismicPhase scatteredPhase, double scatterDepth, double scatterDistance) {
        this.inboundArrival = inboundArrival;
        this.scatteredPhase = scatteredPhase;
        this.scatterDepth = scatterDepth;
        this.scatterDistance = scatterDistance;
    }

    @Override
    public boolean phasesExistsInModel() {
        return inboundArrival != null && scatteredPhase.phasesExistsInModel();
    }

    @Override
    public Arrival getEarliestArrival(double degrees) {
        return Arrival.getEarliestArrival(calcTime(degrees));
    }

    @Override
    public TauModel getTauModel() {
        return scatteredPhase.getTauModel();
    }

    @Override
    public double getMinDistanceDeg() {
        return inboundArrival.getDistDeg()+scatteredPhase.getMinDistanceDeg();
    }

    @Override
    public double getMinDistance() {
        return inboundArrival.getDist()+scatteredPhase.getMinDistance();
    }

    @Override
    public double getMaxDistanceDeg() {
        return inboundArrival.getDistDeg()+scatteredPhase.getMaxDistanceDeg();
    }

    @Override
    public double getMaxDistance() {
        return inboundArrival.getDist()+scatteredPhase.getMaxDistance();
    }

    @Override
    public double getMaxRayParam() {
        return scatteredPhase.getMaxRayParam();
    }

    @Override
    public double getMinRayParam() {
        return scatteredPhase.getMinRayParam();
    }

    @Override
    public int getMaxRayParamIndex() {
        return scatteredPhase.getMaxRayParamIndex();
    }

    @Override
    public int getMinRayParamIndex() {
        return scatteredPhase.getMinRayParamIndex();
    }

    @Override
    public double getSourceDepth() {
        return inboundArrival.getSourceDepth();
    }

    @Override
    public double getReceiverDepth() {
        return scatteredPhase.getReceiverDepth();
    }

    @Override
    public String getName() {
        return inboundArrival.getName()+LegPuller.SCATTER_CODE+scatteredPhase.getName();
    }

    @Override
    public String getPuristName() {
        return inboundArrival.getPuristName()+"."+scatteredPhase.getPuristName();
    }

    @Override
    public List<String> getLegs() {
        List<String> out = new ArrayList<>();
        out.addAll(inboundArrival.getPhase().getLegs());
        out.addAll(scatteredPhase.getLegs());
        return out;
    }

    @Override
    public List<SeismicPhaseSegment> getPhaseSegments() {
        List<SeismicPhaseSegment> out = new ArrayList<>();
        out.addAll(inboundArrival.getPhase().getPhaseSegments());
        out.addAll(scatteredPhase.getPhaseSegments());
        return out;
    }

    @Override
    public double getRayParams(int i) {
        return scatteredPhase.getRayParams(i);
    }

    @Override
    public double[] getRayParams() {
        return scatteredPhase.getRayParams();
    }

    @Override
    public double getDist(int i) {
        return inboundArrival.getDist()+scatteredPhase.getDist(i);
    }

    @Override
    public double[] getDist() {
        double[] scatDist = scatteredPhase.getDist();
        double[] out = new double[scatDist.length];
        for (int i = 0; i < scatDist.length; i++) {
            out[i] = inboundArrival.getDist()+scatDist[i];
        }
        return out;
    }

    @Override
    public double getTime(int i) {
        return inboundArrival.getTime()+scatteredPhase.getTime(i);
    }

    @Override
    public double[] getTime() {
        double[] scatTime = scatteredPhase.getTime();
        double[] out = new double[scatTime.length];
        for (int i = 0; i < scatTime.length; i++) {
            out[i] = inboundArrival.getTime()+scatTime[i];
        }
        return out;
    }

    @Override
    public double getTau(int i) {
        return 0;//scatteredPhase.getTau(i);
    }

    @Override
    public double[] getTau() {
        return new double[0];
    }

    @Override
    public boolean[] getDownGoing() {
        boolean[] inDowngoing = inboundArrival.getPhase().getDownGoing();
        boolean[] scatDownGoing = scatteredPhase.getDownGoing();
        boolean[] out = new boolean[inDowngoing.length+scatDownGoing.length];
        System.arraycopy(inDowngoing, 0, out, 0, inDowngoing.length);
        System.arraycopy(scatDownGoing, 0, out, inDowngoing.length, scatDownGoing.length);
        return out;
    }

    @Override
    public boolean[] getWaveType() {
        boolean[] in = inboundArrival.getPhase().getWaveType();
        boolean[] scat = scatteredPhase.getWaveType();
        boolean[] out = new boolean[in.length+scat.length];
        System.arraycopy(in, 0, out, 0, in.length);
        System.arraycopy(scat, 0, out, in.length, scat.length);
        return out;
    }

    @Override
    public int[] getLegAction() {
        int[] in = inboundArrival.getPhase().getLegAction();
        int[] scat = scatteredPhase.getLegAction();
        int[] out = new int[in.length+scat.length];
        System.arraycopy(in, 0, out, 0, in.length);
        System.arraycopy(scat, 0, out, in.length, scat.length);
        return out;
    }

    @Override
    public boolean hasArrivals() {
        return inboundArrival!= null && scatteredPhase.hasArrivals();
    }

    @Override
    public List<Arrival> calcTime(double deg) {
        List<Arrival> out = new ArrayList<>();
        List<Arrival> scat = scatteredPhase.calcTime(deg-inboundArrival.getDistDeg());
        for (Arrival a : scat) {
            Arrival b = new Arrival(this,
                    inboundArrival.getTime()+a.getTime(),
                    inboundArrival.getDist()+a.getDist(),
                    a.getRayParam(),
                    a.getRayParamIndex());
            out.add(b);
        }
        return out;
    }

    @Override
    public Arrival shootRay(double rayParam) throws SlownessModelException, NoSuchLayerException {
        return null;
    }

    @Override
    public double calcRayParamForTakeoffAngle(double takeoffDegree) {
        return inboundArrival.getRayParam();
    }

    @Override
    public double calcTakeoffAngle(double arrivalRayParam) {
        return inboundArrival.getTakeoffAngle();
    }

    @Override
    public double calcIncidentAngle(double arrivalRayParam) {
        return scatteredPhase.calcIncidentAngle(arrivalRayParam);
    }

    @Override
    public String describe() {
        return inboundArrival+"\nScatter at "+scatterDepth+", "+scatterDistance+"\n"+scatteredPhase.describe();
    }

    @Override
    public void dump() {

    }

    @Override
    public List<TimeDist> calcPierceTimeDist(Arrival arrival) {
        List<TimeDist> out = new ArrayList<>();
        out.addAll(Arrays.asList(inboundArrival.getPierce()));
        List<TimeDist> scatPierce = scatteredPhase.calcPierceTimeDist(arrival);
        // first TimeDist is just the zero distance starting point, which repeats the end of the inbound
        scatPierce = scatPierce.subList(1,scatPierce.size());
        out.addAll(scatPierce);
        return out;
    }

    @Override
    public List<TimeDist> calcPathTimeDist(Arrival arrival) {
        List<TimeDist> out = new ArrayList<>();
        out.addAll(Arrays.asList(inboundArrival.getPath()));
        List<TimeDist> scatPath = scatteredPhase.calcPathTimeDist(arrival);
        for (TimeDist td : scatPath) {
            // shift over by scatter distance
            // weird, why is pierce increment but path is cummulative dist?
            out.add(new TimeDist(td.getP(),
                    td.getTime(),
                    scatterDistance*Math.PI/180 +td.getDistRadian(),
                    td.getDepth()));
        }
        return out;
    }
}
