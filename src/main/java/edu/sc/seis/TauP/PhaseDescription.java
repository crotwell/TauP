package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

public class PhaseDescription {

    public PhaseDescription(SeismicPhase phase) {
        this.name = phase.getName();
        this.puristname = phase.getPuristName();
        if (phase.isFail()) {
            this.fail = phase.failReason();
            this.minexists = null;
            this.maxexists = null;
        } else {
            this.minexists = new PhaseRange();
            this.maxexists = new PhaseRange();

            this.minexists.dist = (float) phase.getMinDistanceDeg();
            this.minexists.modulodist = (float) SeismicPhase.distanceTrim180(phase.getMinDistanceDeg());
            this.minexists.rayparameter = (float) (phase.getMinRayParam() / SphericalCoords.RtoD);
            this.minexists.time = (float) phase.getMinTime();

            this.minexists.dist = (float) phase.getMaxDistanceDeg();
            this.minexists.modulodist = (float) SeismicPhase.distanceTrim180(phase.getMaxDistanceDeg());
            this.minexists.rayparameter = (float) (phase.getMaxRayParam() / SphericalCoords.RtoD);
            this.minexists.time = (float) phase.getMaxTime();
            this.shadow = phase.getShadowZones();
            this.segments = new ArrayList<>();
            for (List<SeismicPhaseSegment> segList : phase.getListPhaseSegments()) {
                SegmentDescription segDesc = new SegmentDescription();
                segDesc.minrayparam = (float) segList.get(segList.size()-1).getMinRayParam();
                segDesc.maxrayparam = (float) segList.get(segList.size()-1).getMaxRayParam();
                for (SeismicPhaseSegment segment : segList) {
                    BranchDescription b = segment.describe();
                    segDesc.branchseq.add(b);
                }
            }
        }

    }
    String name;
    String puristname;
    String fail;
    PhaseRange minexists;
    PhaseRange maxexists;
    List<ShadowZone> shadow = new ArrayList<>();
    List<SegmentDescription> segments = new ArrayList<>();
}

class PhaseRange {
    float dist;
    float modulodist;
    float rayparameter;
    float time;
}

class SegmentDescription {
    float maxrayparam;
    float minrayparam;
    List<BranchDescription> branchseq = new ArrayList<>();
}

