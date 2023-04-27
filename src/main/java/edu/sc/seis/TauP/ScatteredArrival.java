package edu.sc.seis.TauP;

public class ScatteredArrival extends Arrival {

    private final Arrival inboundArrival;
    private final Arrival scatteredArrival;
    private final boolean isBackscatter;

    public static String formScatterPhaseName(String inboundName, String scatterName, boolean isBackscatter) {
        if (isBackscatter) {
            return inboundName+LegPuller.BACKSCATTER_CODE+scatterName;
        } else {
            return inboundName+LegPuller.SCATTER_CODE+scatterName;
        }
    }
    public ScatteredArrival(ScatteredSeismicPhase phase, double deg, Arrival inboundArrival, Arrival scatteredArrival, boolean isBackscatter) {
        super(phase,
                inboundArrival.getTime()+scatteredArrival.getTime(),
                deg*Arrival.DtoR,
                scatteredArrival.getRayParam(),
                scatteredArrival.getRayParamIndex(),
                formScatterPhaseName(inboundArrival.getName(), scatteredArrival.getName(), isBackscatter),
                formScatterPhaseName(inboundArrival.getPuristName(), scatteredArrival.getPuristName(), isBackscatter),
                inboundArrival.getPhase().getSourceDepth(),
                scatteredArrival.getPhase().getReceiverDepth(),
                inboundArrival.getPhase().calcTakeoffAngle(inboundArrival.getRayParam()),
                scatteredArrival.getPhase().calcIncidentAngle(scatteredArrival.getRayParam()));
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

    public boolean isInboundNegativeDirection() {
        return (getScatteredSeismicPhase().getScattererDistanceDeg()<0) == isBackscatter();
    }
    public boolean isScatterNegativeDirection() {
        return (getScatteredSeismicPhase().getScattererDistanceDeg()<0) != isBackscatter();
    }
}
