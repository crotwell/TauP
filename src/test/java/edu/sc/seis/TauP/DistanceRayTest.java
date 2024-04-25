package edu.sc.seis.TauP;

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
        ExactDistanceRay edr = ExactDistanceRay.ofDegrees(210);
        List<Double> edistList = edr.calcRadiansInRange(0, 4*Math.PI, 6371, true);
        assertEquals(1, edistList.size());

        FixedHemisphereDistanceRay fdr = FixedHemisphereDistanceRay.ofDegrees(210);
        List<Double> fdistList = fdr.calcRadiansInRange(0, 4*Math.PI, 6371, true);
        assertEquals(2, fdistList.size());

    }

}
