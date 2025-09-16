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

    public double[] faultNormal() {
        double s = strikeDipRake.get(0)*DtoR;
        double d = strikeDipRake.get(1)*DtoR;
        double r = strikeDipRake.get(2)*DtoR;
        return new double[] {
                Math.sin(d) * Math.cos(s),
                -1 * Math.sin(d) * Math.sin(s),
                Math.cos(d)
        };
    }

    public double[] faultSlip() {
        return faultVector(strikeDipRake.get(2));
    }

    public double[] faultVector(double rake) {
        double s = strikeDipRake.get(0)*DtoR;
        double d = strikeDipRake.get(1)*DtoR;
        double r = rake*DtoR;
        return new double[] {
                Math.cos(r)*Math.sin(s)-Math.sin(r) * Math.cos(d) * Math.cos(s),
                Math.sin(r) * Math.cos(d) * Math.sin(s)+Math.cos(r)*Math.cos(s),
                Math.sin(r)*Math.sin(d)
        };
    }

    public SeismicSourceArgs auxPlane() {
        double[] normal = faultSlip();
        double[] slip = faultNormal();
        double dip = Math.acos(normal[2]);
        double sinDip = Math.sin(dip);
        double strike = Math.atan2(-1*normal[1]/sinDip, normal[0]/sinDip);
        double rake = Math.asin(slip[2]/sinDip);
        SeismicSourceArgs auxPlane = new SeismicSourceArgs();
        auxPlane.setStrikeDipRake(List.of((float)(strike*RtoD), (float)(dip*RtoD), (float)(rake*RtoD)));
        auxPlane.setMw(getMw());
        auxPlane.attenuationFreq = attenuationFreq;
        auxPlane.numFrequencies = numFrequencies;
        return auxPlane;
    }

    public double[] nullAxis() {
        double s = strikeDipRake.get(0)*DtoR;
        double d = strikeDipRake.get(1)*DtoR;
        double r = strikeDipRake.get(2)*DtoR;
        return new double[] {
                -1*Math.sin(r)*Math.cos(s) + Math.cos(r)*Math.cos(d)*Math.sin(s),
                Math.sin(r)*Math.sin(s) + Math.cos(r)*Math.cos(d)*Math.cos(s),
                Math.cos(r)*Math.sin(d)
        };
    }

    /**
     * Calculate radiation pattern terms, Fp, Fsv, Fsh for the given fault orientation and az,takeoff.
     *
     * @param azimuth azimuth to receiver in degrees
     * @param takeoffDeg takeoff angle in degrees
     * @return  Fp, Fsv, Fsh
     */
    public double[] calcRadiationPat(double azimuth, double takeoffDeg) {
        if (hasStrikeDipRake()) {
            return calcRadiationPatRadian(
                    strikeDipRake.get(0)*DtoR,
                    strikeDipRake.get(1)*DtoR,
                    strikeDipRake.get(2)*DtoR,
                    azimuth*DtoR,
                    takeoffDeg*DtoR
                    );
        }
        return new double[] { 1, 1, 1};
    }

    public static double[] calcRadiationPatDeg(double strike, double dip, double rake, double azimuth, double takeoff) {
        return calcRadiationPatRadian(strike*DtoR, dip*DtoR, rake*DtoR, azimuth*DtoR, takeoff*DtoR);
    }

    /**
     * Calculate radiation pattern terms, Fp, Fsv, Fsh for the given fault orientation and az,takeoff.
     * ALl in radians.
     * @param strike fault strike in radian
     * @param dip fault dip in radian
     * @param rake fault rake in radian
     * @param azimuth azimuth to receiver in radian
     * @param takeoff takeoff angle in radian
     * @return  Fp, Fsv, Fsh
     */
    public static double[] calcRadiationPatRadian(double strike, double dip, double rake, double azimuth, double takeoff) {
        double ih = takeoff;
        double phi_f = strike;
        double phi_r = azimuth;
        double phi_r_f = phi_r - phi_f;
        double theta = dip;
        double lam = rake;
        double Fp = (Math.cos(lam)*Math.sin(theta)*Math.sin(2*phi_r_f)
            - Math.sin(lam)*Math.sin(2*theta)*Math.sin(phi_r_f)*Math.sin(phi_r_f)
              )*Math.sin(ih)*Math.sin(ih)
            + (Math.sin(lam)*Math.cos(2*theta)*Math.sin(phi_r_f)
                - Math.cos(lam)*Math.cos(theta)*Math.cos(phi_r_f)
              )*Math.sin(2*ih)
            + Math.sin(lam)*Math.sin(2*theta)*Math.cos(ih)*Math.cos(ih);

        double Fsv = (Math.sin(lam)*Math.cos(2*theta)*Math.sin(phi_r_f)
            - Math.cos(lam)*Math.cos(theta)*Math.cos(phi_r_f)) * Math.cos(2*ih)
            + 1.0/2*Math.cos(lam)*Math.sin(theta)*Math.sin(2*phi_r_f)*Math.sin(2*ih)
            - 1.0/2*Math.sin(lam)*Math.sin(2*theta)*(1 + Math.sin(phi_r_f)*Math.sin(phi_r_f));

        double Fsh = (Math.cos(lam)*Math.cos(theta)*Math.sin(phi_r_f)
        + Math.sin(lam)*Math.cos(2*theta)*Math.cos(phi_r_f))*Math.cos(ih)
        +(Math.cos(lam)*Math.sin(theta)*Math.cos(2*phi_r_f)
        - 1.0/2*Math.sin(lam)*Math.sin(2*theta)*Math.sin(2*phi_r_f))*Math.sin(ih);
        return new double[] {Fp, Fsv, Fsh};
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
        if (getStrikeDipRake() != null) {
            for (RayCalculateable rc : rayList) {
                if (!rc.hasAzimuth()) {
                    throw new IllegalArgumentException("Amplitude with Strike,Dip,Rake requires azimuth: "+rc);
                }
            }
        }
    }

    public static final float DEFAULT_MW = 4.0f;
    public static final String DEFAULT_MW_STR = ""+DEFAULT_MW;

    public static final int DEFAULT_NUM_FREQUENCIES = 64;
}
