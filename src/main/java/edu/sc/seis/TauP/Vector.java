package edu.sc.seis.TauP;

public class Vector {

    public Vector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public SphericalCoordinate toSpherical() {
        double r = magnitude();
        double phi = Math.acos((z/r));
        double theta = Math.atan2(y, x);
        return new SphericalCoordinate(phi, theta, r);
    }

    public Vector plus(Vector b) {
        return new Vector(x+b.x, y+b.y, z+b.z);
    }

    public Vector minus(Vector b) {
        return new Vector(x-b.x, y-b.y, z-b.z);
    }

    public double magnitude() {
        return Math.sqrt(x*x+y*y+z*z);
    }

    public Vector normalize() {
        double mag = magnitude();
        return new Vector(x/mag, y/mag, z/mag);
    }

    public Vector negate() {
        return new Vector(-1*x, -1*y, -1*z);
    }

    public static Vector dotProduct(Vector a, Vector b) {
        return new Vector(
                a.x*b.x,
                a.y*b.y,
                a.z*b.z
        );
    }

    public static Vector crossProduct(Vector a, Vector b) {
        return new Vector(
                a.y*b.z - a.z*b.y,
                a.z*b.x - a.x*b.z,
                a.x*b.y - a.y*b.x
        );
    }

    double x;
    double y;
    double z;

}
