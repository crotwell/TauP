package edu.sc.seis.TauP;

import java.util.List;

public class Isochron {
    public Isochron(double time, List<WavefrontPathSegment> wavefront) {
        this.time = time;
        this.wavefront = wavefront;
    }

    public double getTime() {
        return time;
    }

    public List<WavefrontPathSegment> getWavefront() {
        return wavefront;
    }

    double time;
    List<WavefrontPathSegment> wavefront;

}
