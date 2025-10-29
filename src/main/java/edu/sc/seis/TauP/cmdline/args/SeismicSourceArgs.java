package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.*;
import picocli.CommandLine;

import java.util.List;

public class SeismicSourceArgs {



    @CommandLine.Option(names = "--mw",
            defaultValue = ArrivalAmplitude.DEFAULT_MW_STR,
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
            defaultValue=""+ ArrivalAmplitude.DEFAULT_ATTENUATION_FREQUENCY,
            description = "attenuation frequency for amplitude calculations, default is ${DEFAULT-VALUE}")
    Float attenuationFreq = null;

    public float getAttenuationFrequency() {
        if (attenuationFreq == null) {
            return ArrivalAmplitude.DEFAULT_ATTENUATION_FREQUENCY;
        } else {
            return attenuationFreq;
        }
    }

    @CommandLine.Option(names="--numattenuationfreq",
            defaultValue=""+ ArrivalAmplitude.DEFAULT_NUM_FREQUENCIES,
            description = " number attenuation frequencies for amplitude calculations, default is ${DEFAULT-VALUE}")
    int numFrequencies = ArrivalAmplitude.DEFAULT_NUM_FREQUENCIES;

    public int getNumFrequencies() {
        return numFrequencies;
    }

    List<Float> strikeDipRake = null;

    @CommandLine.Option(names= {"--strikediprake"},
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
        if (strikeDipRake != null) {
            return new FaultPlane(strikeDipRake.get(0), strikeDipRake.get(1), strikeDipRake.get(2));
        }
        return null;
    }

    public void validateArguments() {
        if (strikeDipRake != null) {
            if (strikeDipRake.size() != 3) {
                throw new IllegalArgumentException("StrikeDipRake must have 3 values, but was: " + strikeDipRake.size());
            }
            if (strikeDipRake.get(0) < -180 || 360 < strikeDipRake.get(0) ) {
                throw new IllegalArgumentException("Strike should be -180 to 360, but was "+strikeDipRake.get(0));
            }
            if (strikeDipRake.get(1) < -90 || 90 < strikeDipRake.get(1) ) {
                throw new IllegalArgumentException("Dip should be -90 to 90, but was "+strikeDipRake.get(1));
            }
            if (strikeDipRake.get(2) < -180 || 180 < strikeDipRake.get(2) ) {
                throw new IllegalArgumentException("Rake should be -180 to 180, but was "+strikeDipRake.get(2));
            }
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

    public SeismicSource getSeismicSource() {
        return new SeismicSource(getMw(), hasStrikeDipRake()?getFaultPlane():null);
    }
}
