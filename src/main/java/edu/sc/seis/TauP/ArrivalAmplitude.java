package edu.sc.seis.TauP;

import static edu.sc.seis.TauP.SphericalCoords.RtoD;

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
    float phaseFactorpsv;
    String hilbertpsv;
    float phaseFactorsh;
    String hilbertsh;
    int internalCaustic = 0;
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
        internalCaustic = arr.getInternalCausticCount();
        SeismicPhaseReflTransHolder psvRT = arr.getReflTransPSV();
        if (factorpsv < 0) {
            factorpsv *= -1;
            psvRT = psvRT.negative();
        }
        phaseFactorpsv = (float) psvRT.getPhase();
        hilbertpsv = getHilbertSymbolPSv();
        SeismicPhaseReflTransHolder shRT = arr.getReflTransPSV();
        if (factorsh < 0) {
            factorsh *= -1;
            shRT = shRT.negative();
        }
        phaseFactorsh = (float) shRT.getPhase();
        hilbertsh = getHilbertSymbolSh();
    }

    public double getPhasePSvDeg() {
        return phaseFactorpsv*RtoD;
    }
    public double getPhaseShDeg() {
        return phaseFactorsh*RtoD;
    }
    public String getHilbertSymbolPSv() {
        return getHilbertSymbol(phaseFactorpsv);
    }
    public String getHilbertSymbolSh() {
        return getHilbertSymbol(phaseFactorsh);
    }
    public static String getHilbertSymbol(double phaseFactor) {
        int rotatePhaseNum = (int) Math.round(phaseFactor/(Math.PI/4));
        switch (rotatePhaseNum) {
            case -4:
            case 4:
                return "-1";
            case -3:
                return "-h";
            case -2:
                return "-H";
            case -1:
                return "-h";
            case 0:
                return "1";
            case 1:
                return "h";
            case 2:
                return "H";
            case 3:
                return "h";
            default:
                return "oops "+rotatePhaseNum;
        }
    }
}