package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;

public class ArrivalAmplitude {

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
    SeismicSourceArgs source;

    public ArrivalAmplitude(Arrival arr) throws TauModelException, SlownessModelException {
        geospread = (float)arr.getAmplitudeGeometricSpreadingFactor();
        source = arr.getRayCalculateable().sourceArgs;
        factorpsv = (float) arr.getAmplitudeFactorPSV();
        factorsh = (float) arr.getAmplitudeFactorSH();
        geospread = (float) geospread;
        attenuation = (float) arr.calcAttenuation();
        freeFactor = (float) arr.calcFreeFactor();
        radiationPattern = new float[3];
        double[] drp = arr.calcRadiationPattern();
        radiationPattern[0] = (float)drp[0];
        radiationPattern[1] = (float)drp[1];
        radiationPattern[2] = (float)drp[2];

        radiationTerm = (float) arr.calcRadiationTerm();
        mgtokg = (float) 1e-3;
        refltranpsv = (float) arr.getEnergyFluxFactorReflTransPSV();
        refltransh = (float) arr.getEnergyFluxFactorReflTransSH();
    }
}