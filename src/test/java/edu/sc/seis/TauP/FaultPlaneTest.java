package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FaultPlaneTest {

    @Test
    public void testSimpleFaultVectors() {
        float strike = 0;
        float dip = 90;
        float rake = 0;
        FaultPlane faultPlane = new FaultPlane(strike, dip, rake);
        FaultPlane auxPlane = faultPlane.auxPlane();
        assertEquals(90, auxPlane.dip, 0.5);
        assertEquals(0, auxPlane.rake, 0.5);
        assertEquals(-90, auxPlane.strike, 0.5);
        Vector n = faultPlane.faultNormal();
        assertEquals(0, n.z, 1e-9);
        assertEquals(1, n.x, 1e-9);
        assertEquals(0, n.y, 1e-9);
        Vector d = faultPlane.faultSlip();
        assertEquals(0, d.z, 1e-9);
        assertEquals(0, d.x, 1e-9);
        assertEquals(1, d.y, 1e-9);
        Vector b = faultPlane.nullAxis();
    }

    @Test
    public void testFaultVectors() {
        float strike = 238;
        float dip = 22;
        float rake = 108;
        FaultPlane faultPlane = new FaultPlane(strike, dip, rake);
        FaultPlane auxPlane = faultPlane.auxPlane();
        assertEquals(69, auxPlane.dip, 0.5);
        assertEquals(83, auxPlane.rake, 0.5);
        assertEquals(39, auxPlane.strike, 0.5);
        Vector n = faultPlane.faultNormal();
        assertEquals(1, n.magnitude(), 1e-9);
        Vector d = faultPlane.faultSlip();
        assertEquals(0, Vector.dotProduct(n, d), 1e-9);
        assertEquals(1, d.magnitude(), 1e-9);
        Vector b = faultPlane.nullAxis();
        assertEquals(1, b.magnitude(), 1e-9);
        Vector nCrossD = Vector.crossProduct(n,d);
        assertEquals(1, nCrossD.magnitude(), 1e-9);
        assertEquals(b.magnitude(), nCrossD.magnitude(), 1e-9);
        assertEquals(b.x, nCrossD.x, 1e-9);
        assertEquals(b.y, nCrossD.y, 1e-9);
        assertEquals(b.z, nCrossD.z, 1e-9);
        Vector p = faultPlane.pAxis();
        Vector t = faultPlane.tAxis();
        assertEquals(0, Vector.dotProduct(t, b), 1e-9);
        assertEquals(0, Vector.dotProduct(p, b), 1e-9);
        assertEquals(0, Vector.dotProduct(p, t), 1e-9);

        Vector auxN = auxPlane.faultNormal();
        assertEquals(0, Vector.dotProduct(n, auxN), 1e-9, "n: "+n+" auxN: "+auxN);
    }
}
