package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.seisFile.Location;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

public class LatLonAzBazArgs extends LatLonArgs {

    @CommandLine.Option(names = "--az", description = "azimuth in degrees")
    protected Double azimuth = null;


    @CommandLine.Option(names = "--baz", description = "backazimuth in degrees")
    protected Double backAzimuth = null;

}
