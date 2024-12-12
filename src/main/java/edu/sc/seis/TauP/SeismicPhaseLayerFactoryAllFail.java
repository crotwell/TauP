package edu.sc.seis.TauP;

public class SeismicPhaseLayerFactoryAllFail extends SeismicPhaseLayerFactory {

    public SeismicPhaseLayerFactoryAllFail(SeismicPhaseFactory baseFactory, String layerName, int topBranchNum, int botBranchNum, String reason) {
        super(baseFactory, layerName, topBranchNum, botBranchNum);
        this.reason = reason;
    }
    public ProtoSeismicPhase parse(ProtoSeismicPhase proto,
                                   String prevLeg, String currLeg, String nextLeg, String nextNextLeg,
                                   boolean prevIsPWave, boolean isPWave, boolean nextIsPWave, int legNum) throws TauModelException {
        // see if can pass on to below or above as we are degenerate layer
        SeismicPhaseLayerFactory above = belowLayerFactory;
        if (above != null && above instanceof SeismicPhaseLayerFactoryAllFail) {
            above = above.aboveLayerFactory;
        }
        SeismicPhaseLayerFactory below = belowLayerFactory;
        if (below != null && below instanceof SeismicPhaseLayerFactoryAllFail) {
            below = below.belowLayerFactory;
        }
        if (below != null && below.isLayerLeg(currLeg)) {
            return below.parse(proto, prevLeg, currLeg, nextLeg, nextNextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);
        } else if (above != null && above.isLayerLeg(currLeg)) {
            return aboveLayerFactory.parse(proto, prevLeg, currLeg, nextLeg, nextNextLeg, prevIsPWave, isPWave, nextIsPWave, legNum);

        }
        proto.failNext(reason);
        return proto;
    }

    String reason;
}
