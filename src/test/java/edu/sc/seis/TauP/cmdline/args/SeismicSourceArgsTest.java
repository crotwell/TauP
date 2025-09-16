package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.SphericalCoordinate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SeismicSourceArgsTest {

    @Test
    public void testSimpleFaultVectors() {
        float strike = 0;
        float dip = 90;
        float rake = 0;
        SeismicSourceArgs sourceArgs = new SeismicSourceArgs();
        sourceArgs.setStrikeDipRake(List.of(strike, dip, rake));
        double[] n = sourceArgs.faultNormal();
        assertEquals(0, n[2], 1e-9);
        assertEquals(1, n[0], 1e-9);
        assertEquals(0, n[1], 1e-9);
        double[] d = sourceArgs.faultSlip();
        assertEquals(0, d[2], 1e-9);
        assertEquals(0, d[0], 1e-9);
        assertEquals(1, d[1], 1e-9);
        double[] b = sourceArgs.nullAxis();
    }

    @Test
    public void testFaultVectors() {
        float strike = 238;
        float dip = 22;
        float rake = 108;
        SeismicSourceArgs sourceArgs = new SeismicSourceArgs();
        sourceArgs.setStrikeDipRake(List.of(strike, dip, rake));
        double[] n = sourceArgs.faultNormal();
        double[] d = sourceArgs.faultSlip();
        double[] b = sourceArgs.nullAxis();
        double[] nCrossD = SphericalCoordinate.crossProduct(n,d);
        assertEquals(b[0], nCrossD[0], 1e-9);
        assertEquals(b[1], nCrossD[1], 1e-9);
        assertEquals(b[2], nCrossD[2], 1e-9);

    }
}
