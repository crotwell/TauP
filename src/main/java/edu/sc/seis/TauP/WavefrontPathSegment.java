package edu.sc.seis.TauP;

import java.util.List;

/**
 * Segment of the path of wavefront of a seismic phase at a time step.
 * Usually a segment the full wavefront between discontinuities in the model, or turning point.
 */
public class WavefrontPathSegment extends  AbstractPathSegment {

    public WavefrontPathSegment(List<TimeDist> path, boolean isPWave, String segmentName, TimeDist prevEnd,
                                int segmentIndex, int totalNumSegments, SeismicPhase phase, double timeVal) {
        super(path, isPWave, segmentName, prevEnd, segmentIndex, totalNumSegments, phase);
        this.timeVal = timeVal;
        this.pathCssClass = "wavefront";
    }

    public static WavefrontPathSegment degenerateSegment(TimeDist pathPoint,
                                                         boolean isPWave, String segmentName, TimeDist prevEnd,
                                                         int segmentIndex, int totalNumSegments,
                                                         SeismicPhase phase, double timeVal) {
        List<TimeDist> path = List.of(pathPoint);
        return new WavefrontPathSegment(path, isPWave, segmentName, prevEnd, segmentIndex, totalNumSegments, phase, timeVal);
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

    public double getTimeVal() {
        return timeVal;
    }

    double timeVal;
}
