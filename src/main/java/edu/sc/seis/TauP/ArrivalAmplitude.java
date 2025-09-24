package edu.sc.seis.TauP;

public class ArrivalAmplitude {

    public static final float DEFAULT_ATTENUATION_FREQUENCY = 1.0f;
    public static final float DEFAULT_MW = 4.0f;
    public static final String DEFAULT_MW_STR = ""+DEFAULT_MW;
    public static final int DEFAULT_NUM_FREQUENCIES = 64;
    float factorpsv;
    float factorsh;
    float geospread;
    float attenuation;
    float freeFactor;
    float[] radiationPattern;
    float radiationTerm;
    float mgtokg;
    float refltranpsv;
    float refltransh;
    SeismicSource source;

    public ArrivalAmplitude(Arrival arr) throws TauModelException, SlownessModelException {
        geospread = (float)arr.getAmplitudeGeometricSpreadingFactor();
        source = arr.getRayCalculateable().seismicSource;
        factorpsv = (float) arr.getAmplitudeFactorPSV();
        factorsh = (float) arr.getAmplitudeFactorSH();
        attenuation = (float) arr.calcAttenuation();
        freeFactor = (float) arr.calcFreeFactor();
        radiationPattern = new float[3];
        RadiationAmplitude drp = arr.calcRadiationPattern();
        radiationPattern[0] = (float)drp.getRadialAmplitude();
        radiationPattern[1] = (float)drp.getPhiAmplitude();
        radiationPattern[2] = (float)drp.getThetaAmplitude();

        radiationTerm = (float) arr.calcRadiationTerm();
        mgtokg = (float) 1e-3;
        refltranpsv = (float) arr.getEnergyFluxFactorReflTransPSV();
        refltransh = (float) arr.getEnergyFluxFactorReflTransSH();
    }
}