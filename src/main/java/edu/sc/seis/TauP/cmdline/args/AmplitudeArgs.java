package edu.sc.seis.TauP.cmdline.args;

import picocli.CommandLine;

public class AmplitudeArgs extends SeismicSourceArgs {

    @CommandLine.Option(names = "--amp", description = "show amplitude factor for each phase")
    public boolean withAmplitude = false;

    public boolean isWithAmplitude() {
        return withAmplitude;
    }

    public static final String AMPLITUDE_WARNING = "    WARNING: \n"+
            "      Amplitudes are an experimental feature and may not generate correct\n" +
            "      results. They are provided in the hope that they are helpful and to\n" +
            "      allow feedback from the community, but testing of their correctness\n" +
            "      is ongoing.";

}
