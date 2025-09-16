package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VectorTest {

    @Test
    public void crossProduct() {
        Vector a = new Vector(1, 0, 0);
        Vector b = new Vector(0, 1, 0);
        Vector c = Vector.crossProduct(a,b);
        assertEquals(0, c.x, 1e-9);
        assertEquals(0, c.y, 1e-9);
        assertEquals(1, c.z, 1e-9);
        assertEquals(1, c.magnitude(), 1e-9);
        Vector v = Vector.crossProduct(b,c);
        assertEquals(a.x, v.x, 1e-9);
        assertEquals(a.y, v.y, 1e-9);
        assertEquals(a.z, v.z, 1e-9);
        assertEquals(1, v.magnitude(), 1e-9);
        v = Vector.crossProduct(c,a);
        assertEquals(b.x, v.x, 1e-9);
        assertEquals(b.y, v.y, 1e-9);
        assertEquals(b.z, v.z, 1e-9);
        assertEquals(1, v.magnitude(), 1e-9);
    }
}
