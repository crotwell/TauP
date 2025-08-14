package edu.sc.seis.TauP;

import java.util.List;

public class PhaseIsochron {

    public PhaseIsochron(double time, SeismicPhase phase, List<WavefrontPathSegment> wavefront) {
        this.time = time;
        this.phase = phase;
        this.wavefront = wavefront;
    }

    public double getTime() {
        return time;
    }

    public SeismicPhase getPhase() {
        return phase;
    }

    public java.util.List<WavefrontPathSegment> getWavefront() {
        return wavefront;
    }

    double time;
    SeismicPhase phase;
    List<WavefrontPathSegment> wavefront;

}
