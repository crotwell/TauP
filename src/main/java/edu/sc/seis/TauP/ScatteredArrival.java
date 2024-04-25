package edu.sc.seis.TauP;

public class ScatteredArrival extends Arrival {

    private final Arrival inboundArrival;
    private final Arrival scatteredArrival;
    private final boolean isBackscatter;

    public static String formScatterPhaseName(String inboundName, String scatterName, boolean isBackscatter) {
        if (isBackscatter) {
            return inboundName+ PhaseSymbols.BACKSCATTER_CODE+scatterName;
        } else {
            return inboundName+ PhaseSymbols.SCATTER_CODE+scatterName;
        }
    }
    public ScatteredArrival(ScatteredSeismicPhase phase, RayCalculateable searchVal, Arrival inboundArrival, Arrival scatteredArrival, boolean isBackscatter) {
        super(phase,
                inboundArrival.getTime()+scatteredArrival.getTime(),
                inboundArrival.getDist()+scatteredArrival.getDist(),
                scatteredArrival.getRayParam(),
                scatteredArrival.getRayParamIndex(),
                searchVal,
                formScatterPhaseName(inboundArrival.getName(), scatteredArrival.getName(), isBackscatter),
                formScatterPhaseName(inboundArrival.getPuristName(), scatteredArrival.getPuristName(), isBackscatter),
                inboundArrival.getPhase().getSourceDepth(),
                scatteredArrival.getPhase().getReceiverDepth(),
                inboundArrival.getPhase().calcTakeoffAngle(inboundArrival.getRayParam()),
                scatteredArrival.getPhase().calcIncidentAngle(scatteredArrival.getRayParam()),
                scatteredArrival.getDRayParamDDelta());
        this.inboundArrival = inboundArrival;
        this.scatteredArrival = scatteredArrival;
        this.isBackscatter = isBackscatter;
    }

    public ScatteredSeismicPhase getScatteredSeismicPhase() {
        return (ScatteredSeismicPhase)getPhase();
    }

    public Arrival getInboundArrival() {
        return inboundArrival;
    }
    public Arrival getScatteredArrival() {
        return scatteredArrival;
    }
    public boolean isBackscatter() {
        return isBackscatter;
    }

    @Override
    public boolean isLongWayAround() {
        // scattered phases are not symmetric, so never long way around
        return false;
    }

    public boolean isInboundNegativeDirection() {
        return getScatteredSeismicPhase().getScattererDistanceDeg() < 0;
    }
    public boolean isScatterNegativeDirection() {
        double scatDist = getScatteredSeismicPhase().getScattererDistanceDeg();
        return (scatDist >= 0 && isBackscatter()) || (scatDist < 0 && !isBackscatter());
    }
}
