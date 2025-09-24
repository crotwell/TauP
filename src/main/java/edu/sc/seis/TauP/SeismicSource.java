package edu.sc.seis.TauP;

public class SeismicSource {

    public SeismicSource() {

    }

    public SeismicSource(float Mw) {
        this.Mw = Mw;
    }

    public SeismicSource(float Mw, FaultPlane nodalPlane) {
        this(Mw);
        this.nodalPlane = nodalPlane;
    }

    public boolean hasSource() {
        return this.Mw != null || this.nodalPlane != null;
    }

    public boolean hasMw() {
        return Mw != null;
    }

    public Float getMw() {
        return Mw;
    }

    public boolean hasNodalPlane() {
        return this.nodalPlane != null;
    }

    public FaultPlane getNodalPlane1() {
        return nodalPlane;
    }

    public FaultPlane getNodalPlane2() {
        return nodalPlane.auxPlane();
    }

    public float getAttenuationFrequency() {
        return attenuationFrequency;
    }

    public int getNumFrequencies() {
        return numFrequencies;
    }

    Float Mw = null;

    FaultPlane nodalPlane = null;

    float attenuationFrequency = ArrivalAmplitude.DEFAULT_ATTENUATION_FREQUENCY;

    int numFrequencies = ArrivalAmplitude.DEFAULT_NUM_FREQUENCIES;

}
