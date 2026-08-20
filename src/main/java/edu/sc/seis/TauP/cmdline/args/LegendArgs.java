package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.LegendLocation;
import picocli.CommandLine;

public class LegendArgs {

    public boolean isLegend() {
        return legend;
    }

    public LegendLocation getLegendLocation() {
        return legendLocation;
    }

    public void setLegend(boolean legend) {
        this.legend = legend;
    }

    public void setLegendLocation(LegendLocation legendLocation) {
        this.legendLocation = legendLocation;
    }

    @CommandLine.Option(names = "--legend", description = "create a legend")
    boolean legend = false;

    @CommandLine.Option(names = "--legendloc",
            defaultValue = "TOP_LEFT",
            description = "position legend, one of ${COMPLETION-CANDIDATES}, default is ${DEFAULT-VALUE}")
    LegendLocation legendLocation = LegendLocation.TOP_LEFT;

}
