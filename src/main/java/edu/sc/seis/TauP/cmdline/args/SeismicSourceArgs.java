package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.*;
import picocli.CommandLine;

import java.util.List;

import static edu.sc.seis.TauP.SphericalCoords.DtoR;
import static edu.sc.seis.TauP.SphericalCoords.RtoD;

public class SeismicSourceArgs {



    @CommandLine.Option(names = "--mw",
            defaultValue = DEFAULT_MW_STR,
            description = "scale amplitude by source moment magnitude, default is ${DEFAULT-VALUE}")
    public void setMw(float mw) {
        this.mw = mw;
    }
    Float mw = null;

    public float getMw() {
        if (mw == null) {
            return MomentMagnitude.MAG4;
        }
        return mw;
    }

    public double getMoment() {
        return MomentMagnitude.mw_to_N_m(getMw());
    }

    @CommandLine.Option(names="--attenuationfreq",
            defaultValue=""+Arrival.DEFAULT_ATTENUATION_FREQUENCY,
            description = "attenuation frequency for amplitude calculations, default is ${DEFAULT-VALUE}")
    Float attenuationFreq = null;

    public float getAttenuationFrequency() {
        if (attenuationFreq == null) {
            return Arrival.DEFAULT_ATTENUATION_FREQUENCY;
        } else {
            return attenuationFreq;
        }
    }

    @CommandLine.Option(names="--numattenuationfreq",
            defaultValue=""+DEFAULT_NUM_FREQUENCIES,
            description = " number attenuation frequencies for amplitude calculations, default is ${DEFAULT-VALUE}")
    int numFrequencies = DEFAULT_NUM_FREQUENCIES;

    public int getNumFrequencies() {
        return numFrequencies;
    }

    List<Float> strikeDipRake = null;

    @CommandLine.Option(names="--strikediprake",
            paramLabel =  "strike dip rake",
            hideParamSyntax = true,
            arity = "3",
            description = "fault strike, dip and rake for amplitude calculations. If not given radiation pattern is unity in all directions.")
    public void setStrikeDipRake(List<Float> sdr) {
        if (sdr.isEmpty()) {
            // unset by picocli, as no default value
            this.strikeDipRake = null;
        } else if (sdr.size() != 3) {
            String valStr="";
            for (Float f : sdr) {
                valStr+=" "+f;
            }
            throw new IllegalArgumentException(
                    String.format("Invalid number of params '%d' for option '--strikediprake': " +
                            "must give 3 values: %s", sdr.size(), valStr));
        }
        this.strikeDipRake = sdr;
    }

    public boolean hasStrikeDipRake() {
        return strikeDipRake != null;
    }

    public List<Float>  getStrikeDipRake() {
        return strikeDipRake;
    }

    public FaultPlane getFaultPlane() {
        return new FaultPlane(strikeDipRake.get(0), strikeDipRake.get(1), strikeDipRake.get(2));
    }

    public void validateArguments() {
        if (strikeDipRake != null && strikeDipRake.size() != 3) {
            throw new IllegalArgumentException("StrikeDipRake must have 3 values, but was: "+strikeDipRake.size());
        }
    }

    public void validateArgumentsForAmplitude(ModelArgs modelArgs,  List<RayCalculateable> rayList) throws TauPException {
        if (modelArgs.getTauModel().getVelocityModel().densityIsDefault()) {
            throw new TauModelException("model "+modelArgs.getModelName()+" does not include density, but amplitude requires density.");
        }
        if (modelArgs.getTauModel().getVelocityModel().QIsDefault()) {
            throw new TauModelException("model "+modelArgs.getModelName()
                    +" does not include Q, but amplitude requires Q. Please choose a different model.");
        }
        if (hasStrikeDipRake() ) {
            for (RayCalculateable rc : rayList) {
                if (!rc.hasAzimuth()) {
                    throw new IllegalArgumentException("Amplitude with Strike,Dip,Rake requires azimuth: "+rc);
                }
            }
        }
    }

    public String toString() {
        return getMw()+" Mw, strike: "+strikeDipRake.get(0)+", dip: "+strikeDipRake.get(1)+", rake: "+strikeDipRake.get(2);
    }

    public static final float DEFAULT_MW = 4.0f;
    public static final String DEFAULT_MW_STR = ""+DEFAULT_MW;

    public static final int DEFAULT_NUM_FREQUENCIES = 64;
}
