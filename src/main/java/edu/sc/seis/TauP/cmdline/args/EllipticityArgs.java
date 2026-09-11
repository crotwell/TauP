package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.Ellipticipy;
import edu.sc.seis.TauP.RayCalculateable;
import edu.sc.seis.TauP.TauPException;
import picocli.CommandLine;

public class EllipticityArgs {

    @CommandLine.Option(
            names = {"--ellipticity"},
            description = "Apply ellipticity correction to time, as implemented by ellipticipy",
            defaultValue = "false"
    )
    boolean isEllipticity = false;

    public boolean isEllipticity() {
        return isEllipticity;
    }

    @CommandLine.Option(
            names = {"--siderealDay"},
            description = "Sidereal length of day, in seconds, defaults to Earth value of "+ Ellipticipy.EARTH_LOD
    )
    Double siderealDay = null;

    public boolean hasSiderealDay() {
        return siderealDay != null;
    }
    public Double getSiderealDay() {
        return siderealDay;
    }

    public double bestSiderealDay(DistanceArgs distanceArgs) {
        double siderealDay = Ellipticipy.EARTH_LOD;
        if (hasSiderealDay()) {
            siderealDay = getSiderealDay();
        } else if (distanceArgs.getGeodeticArgs().hasPlanet()) {
            siderealDay = distanceArgs.getGeodeticArgs().getPlanet().getSiderealDay();
        }
        return siderealDay;
    }

    public void validateArguments(CommandLine.Model.CommandSpec spec,
                                  DistanceArgs distanceArgs,
                                  SeismicSourceArgs sourceArgs) throws TauPException {
        if (isEllipticity()) {
            for (RayCalculateable rc : distanceArgs.getRayCalculatables(sourceArgs)) {
                if (!rc.isLatLonable()) {
                    throw new CommandLine.ParameterException(spec.commandLine(),
                            "Using --ellipticity requires source latiude and azimuth, "
                                    +rc.getDescription()+" does not include geographical location,"
                                    +"use some combination of event, station, az, or baz to calculate.");
                }
            }
        }
    }
}
