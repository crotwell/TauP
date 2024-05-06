package edu.sc.seis.TauP.cli;

import picocli.CommandLine;

public class SeismicSourceArgs {


    @CommandLine.Option(names = "--amp", description = "amplitude factor for each phase")
    public boolean withAmplitude = false;

    @CommandLine.Option(names = "--mw", defaultValue = "4.0", description = "scale by source moment magnitude")
    Float mw = null;

    public boolean isWithAmplitude() {
        return withAmplitude;
    }

    public float getMw() {
        if (mw == null) {
            return MAG4;
        }
        return mw;
    }

    public double getMoment() {
        return mw_to_N_m(getMw());
    }

    /**
     *
     * Mw to Mo conversion FMGS eq 7.24, p 210
     * Math.pow(10, (1.5*Mw + 16.1 ));
     * 1 N-m = 1e7 dyne cm, so minus 7
     */
    public static double mw_to_N_m(double Mw) {
        double scalar_moment_N_m = Math.pow(10, (1.5*Mw + 9.1 ));
        return scalar_moment_N_m;
    }

    public static float MAG4 = 4.0f;
    public static double MAG4_MOMENT = mw_to_N_m(MAG4);
}
