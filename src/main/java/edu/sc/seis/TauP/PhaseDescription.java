package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.SphericalCoords.RtoD;

public class PhaseDescription {

    public PhaseDescription(SeismicPhase phase) {
        this.name = phase.getName();
        this.puristname = phase.getPuristName();
        this.sourcedepth = (float) phase.getSourceDepth();
        this.receiverdepth = (float) phase.getReceiverDepth();
        if (phase.isFail()) {
            this.fail = phase.failReason();
            this.minexists = null;
            this.maxexists = null;
        } else {
            this.minexists = new PhaseRange();
            this.maxexists = new PhaseRange();

            this.minexists.dist = (float) phase.getMinDistanceDeg();
            this.minexists.modulodist = (float) SeismicPhase.distanceTrim180(phase.getMinDistanceDeg());
            this.minexists.rayparameter = (float) (phase.getMinRayParam() / RtoD);
            this.minexists.time = (float) phase.getMinTime();

            this.maxexists.dist = (float) phase.getMaxDistanceDeg();
            this.maxexists.modulodist = (float) SeismicPhase.distanceTrim180(phase.getMaxDistanceDeg());
            this.maxexists.rayparameter = (float) (phase.getMaxRayParam() / RtoD);
            this.maxexists.time = (float) phase.getMaxTime();
            this.shadow = phase.getShadowZones();
            this.segments = new ArrayList<>();
            for (List<SeismicPhaseSegment> segList : phase.getListPhaseSegments()) {
                SegmentDescription segDesc = new SegmentDescription();
                segDesc.minrayparam = (float) (segList.get(segList.size()-1).getMinRayParam()/RtoD);
                segDesc.maxrayparam = (float) (segList.get(segList.size()-1).getMaxRayParam()/RtoD);
                for (SeismicPhaseSegment segment : segList) {
                    BranchDescription b = segment.describe();
                    segDesc.branchseq.add(b);
                }
                segments.add(segDesc);
            }
        }

    }
    String name;
    String puristname;
    float sourcedepth;
    float receiverdepth;
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

