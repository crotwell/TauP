package edu.sc.seis.TauP.cli;

import picocli.CommandLine;

public class SeismicSourceArgs {


    @CommandLine.Option(names = "--amp", description = "amplitude factor for each phase")
    public boolean withAmplitude = false;

    @CommandLine.Option(names = "--mw", description = "scale by source moment magnitude")
    Double mw = null;

    public boolean isWithAmplitude() {
        return withAmplitude;
    }

    public double getMw() {
        return mw;
    }

    public double getMoment() {
        if (mw != null) {
            return mw_to_N_m(getMw());
        }
        return 1.0;
    }

    /**
     *
     * Mw to Mo conversion from Lay and Wallace p. 384.
     *
     * @return
     */
    public static double mw_to_N_m(double Mw) {
        double scalar_moment_N_m = Math.pow(10, (Mw + 10.73) * 1.5 - 7.0);
        return scalar_moment_N_m;
    }

    public static double MAG3_MOMENT = mw_to_N_m(3);
}
