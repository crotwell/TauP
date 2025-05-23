package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.seisFile.Location;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

public class LatLonAzBazArgs extends LatLonArgs {

    @CommandLine.Option(names = "--az", description = "azimuth in degrees, source to receiver")
    protected Double azimuth = null;

    public boolean hasAzimuth() {
        return azimuth != null;
    }
    public Double getAzimuth() {
        return azimuth;
    }

    @CommandLine.Option(names = "--baz", description = "backazimuth in degrees, receiver to source")
    protected Double backAzimuth = null;

    public boolean hasBackAzimuth() {
        return backAzimuth != null;
    }

    public Double getBackAzimuth() {
        return backAzimuth;
    }
}
