package edu.sc.seis.TauP.CLI;

import picocli.CommandLine;

public class ScattererArgs {


    @CommandLine.ArgGroup(validate = false, heading = "Scatterer is given by:%n")
    ScattererArgs.ScattererArgsInner distArgs = new ScattererArgs.ScattererArgsInner();

    static class ScattererArgsInner {

        @CommandLine.Parameters(index = "0", paramLabel = "depth")
        public double depth;
        @CommandLine.Parameters(index = "1", paramLabel = "dist")
        public double dist;

    }
}
