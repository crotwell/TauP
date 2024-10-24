package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.MomentMagnitude;
import picocli.CommandLine;

public class SeismicSourceArgs {


    @CommandLine.Option(names = "--mw", defaultValue = "4.0", description = "scale amplitude by source moment magnitude")
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

}
