package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DistanceRayTest {

    @Test
    public void calcRadiansInRangeTest() {
        DistanceRay pi4dr = DistanceRay.ofRadians(Math.PI/4);
        List<Double> pi4distList = pi4dr.calcRadiansInRange(0, Math.PI/2, 6371, true);
        assertEquals(1, pi4distList.size());
        assertEquals(Math.PI/4, pi4distList.get(0));

        DistanceRay dr = DistanceRay.ofDegrees(10);
        List<Double> distList = dr.calcRadiansInRange(0, 4*Math.PI, 6371, true);
        assertEquals(4, distList.size());
        assertEquals(10*Math.PI/180, distList.get(0));
        assertEquals(350*Math.PI/180, distList.get(1), 1e-6);
        assertEquals(2*Math.PI+10*Math.PI/180, distList.get(2), 1e-6);
        assertEquals(2*Math.PI+350*Math.PI/180, distList.get(3), 1e-6);
        ExactDistanceRay edr = DistanceRay.ofExactDegrees(210);
        List<Double> edistList = edr.calcRadiansInRange(0, 4*Math.PI, 6371, true);
        assertEquals(1, edistList.size());

        FixedHemisphereDistanceRay fdr = DistanceRay.ofFixedHemisphereDegrees(210);
        List<Double> fdistList = fdr.calcRadiansInRange(0, 4*Math.PI, 6371, true);
        assertEquals(2, fdistList.size());

    }

    @Test
    public void testCalcAzBaz() {
        Location staLoc = new Location(10, 0);
        Location evtLoc = new Location(0, 0);
        DistanceRay dr = DistanceRay.ofEventStation(evtLoc, staLoc);
        assertEquals(0, dr.getNormalizedAzimuth(), 0.01);
        assertEquals(180, dr.getNormalizedBackAzimuth(), 0.01);

        DistanceRay gdr = DistanceRay.ofGeodeticEventStation(evtLoc, staLoc, DistAz.wgs85_invflattening);
        assertEquals(dr.getNormalizedAzimuth(), gdr.getNormalizedAzimuth(), 0.1);
        assertEquals(dr.getNormalizedBackAzimuth(), gdr.getNormalizedBackAzimuth(), 0.1);


        Location staLocE = new Location(10, 10);
        DistanceRay drE = DistanceRay.ofEventStation(evtLoc, staLocE);
        assertEquals(45, drE.getNormalizedAzimuth(), 1);
        assertEquals(-135, drE.getNormalizedBackAzimuth(), 1);
        DistanceRay gdrE = DistanceRay.ofGeodeticEventStation(evtLoc, staLocE, DistAz.wgs85_invflattening);
        assertEquals(drE.getNormalizedAzimuth(), gdrE.getNormalizedAzimuth(), 0.5, "az");
        assertEquals(drE.getNormalizedBackAzimuth(), gdrE.getNormalizedBackAzimuth(), 0.5, "baz");

        Location evtLocW = new Location(0, -10);
        DistanceRay drW = DistanceRay.ofEventStation(evtLocW, staLoc);
        assertEquals(45, drW.getNormalizedAzimuth(), 1);
        assertEquals(-135, drW.getNormalizedBackAzimuth(), 1);
        DistanceRay gdrW = DistanceRay.ofGeodeticEventStation(evtLocW, staLoc, DistAz.wgs85_invflattening);
        assertEquals(drW.getNormalizedAzimuth(), gdrW.getNormalizedAzimuth(), 0.5);
        assertEquals(drW.getNormalizedBackAzimuth(), gdrW.getNormalizedBackAzimuth(), 0.5);
    }

}
