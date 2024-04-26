package edu.sc.seis.TauP;

import java.util.List;

public class WavefrontPathSegment extends  AbstractPathSegment {

    public WavefrontPathSegment(List<TimeDist> path, boolean isPWave, String segmentName, TimeDist prevEnd,
                                int segmentIndex, int totalNumSegments, SeismicPhase phase, double timeVal) {
        super(path, isPWave, segmentName, prevEnd, segmentIndex, totalNumSegments, phase);
        this.timeVal = timeVal;
        this.pathCssClass = "wavefront";
    }

    @Override
    public String description() {
        return "seg "+segmentIndex+"/"+totalNumSegments+" "+segmentName+" of "+phase.getName()+" at "+Outputs.formatTimeNoPad(timeVal);

    }

    public String getCssClasses() {
        return super.getCssClasses()+ " "+SvgUtil.formatTimeForCss(timeVal);
    }

    public WavefrontPathSegment asNegativeDistance() {
        return new WavefrontPathSegment(negativeDistance(), isPWave, segmentName, prevEnd,
                segmentIndex, totalNumSegments, phase, timeVal);
    }

    double timeVal;
}
