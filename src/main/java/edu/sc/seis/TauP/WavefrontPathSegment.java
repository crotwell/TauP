package edu.sc.seis.TauP;

import java.util.List;

public class WavefrontPathSegment extends  AbstractPathSegment {

    public WavefrontPathSegment(List<TimeDist> path, boolean isPWave, String segmentName, TimeDist prevEnd, int segmentIndex, SeismicPhase phase, double timeVal) {
        super(path, isPWave, segmentName, prevEnd, segmentIndex, phase);
    }

    @Override
    public String description() {
        return "seg "+segmentIndex+" "+segmentName+" of "+phase.getName();

    }

    float timeVal;
}
