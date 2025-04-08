package edu.sc.seis.TauP;

import java.util.List;

public class Isochron {
    public Isochron(double timeval, List<WavefrontPathSegment> segmentList) {
        this.timeval = timeval;
        this.segmentList = segmentList;
    }

    public double getTimeval() {
        return timeval;
    }

    public List<WavefrontPathSegment> getSegmentList() {
        return segmentList;
    }

    double timeval;
    List<WavefrontPathSegment> segmentList;

}
