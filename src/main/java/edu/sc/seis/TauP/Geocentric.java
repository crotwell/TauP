package edu.sc.seis.TauP;

import net.sf.geographiclib.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.sc.seis.TauP.SphericalCoords.rtod;

/**
 * GeoCentric, modified from geocentric.cpp
 *
 * \file Geocentric.cpp
 * \brief Implementation for GeographicLib::Geocentric class
 *
 * Copyright (c) Charles Karney (2008-2022) &lt;karney@alum.mit.edu&gt; and licensed
 * under the MIT/X11 License.  For more information, see
 * https://geographiclib.sourceforge.io/
 **********************************************************************/

public class Geocentric {
  double _a;
  double _f;
  double _e2;
  double _e2m;
  double _e2a;
  double _e4a;
  double _maxrad;

  public Geocentric(Geodesic geodesic) {
    this(geodesic.EquatorialRadius(), geodesic.Flattening());
  }

  public Geocentric(double a, double f) {
    this._a = a;
    this._f = f;
    this._e2 = this._f * (2 - this._f);
    this._e2m = GeoMath.sq(1 - this._f);   // 1 - _e2
    this._e2a = Math.abs(this._e2);
    this._e4a = GeoMath.sq(this._e2);
    this._maxrad = (2 * this._a / Double.MAX_VALUE);

    if (!(Double.isFinite(this._a) && this._a > 0))
      throw new GeographicErr("Equatorial radius is not positive");
    if (!(Double.isFinite(this._f) && this._f < 1))
      throw new GeographicErr("Polar semiaxis is not positive");
  }

  public static final Geocentric WGS84() {
    Geocentric wgs84 = new Geocentric(Constants.WGS84_a, Constants.WGS84_f);
    return wgs84;
  }

  public static final Geocentric SPHERE(double radius) {
    return new Geocentric(radius, 0);
  }

  public List<Double> IntForward(double lat, double lon, double h,
                                 boolean M) {
    double sphi, cphi, slam, clam;
    lat = GeoMath.LatFix(lat);
    Pair plat = new Pair();
    GeoMath.sincosd(plat, GeoMath.AngRound(lat));
    sphi = plat.first;
    cphi = plat.second;
    Pair plon = new Pair();
    GeoMath.sincosd(plon, GeoMath.AngRound(lon));
    slam = plon.first;
    clam = plon.second;
    double n = this._a /Math.sqrt(1 - this._e2 * GeoMath.sq(sphi));
    double Z = (this._e2m * n + h) * sphi;
    double X = (n + h) * cphi;
    double Y = X * slam;
    X *= clam;
    List<Double> ans = List.of(X, Y, Z);
    if (M) {
      ans = Stream.concat(ans.stream(), Rotation(sphi, cphi, slam, clam).stream())
              .collect(Collectors.toList());
    } else {

    }

    return ans;
  }

  public List<Double> IntReverse(double X, double Y, double Z,
                          boolean M) {
    double
      R = Math.hypot(X, Y),
      slam = R != 0 ? Y / R : 0,
      clam = R != 0 ? X / R : 1;
    double h = Math.hypot(R, Z);      // Distance to center of earth
    double sphi, cphi;
    if (h > this._maxrad) {
      // We really far away (> 12 million light years); treat the earth as a
      // point and h, above, is an acceptable approximation to the height.
      // This avoids overflow, e.g., in the computation of disc below.  It's
      // possible that h has overflowed to inf; but that's OK.
      //
      // Treat the case X, Y finite, but R overflows to +inf by scaling by 2.
      R = Math.hypot(X/2, Y/2);
      slam = R != 0 ? (Y/2) / R : 0;
      clam = R != 0 ? (X/2) / R : 1;
      double H = Math.hypot(Z/2, R);
      sphi = (Z/2) / H;
      cphi = R / H;
    } else if (this._e4a == 0) {
      // Treat the spherical case.  Dealing with underflow in the general case
      // with this.e2 = 0 is difficult.  Origin maps to N pole same as with
      // ellipsoid.
      double H = Math.hypot(h == 0 ? 1 : Z, R);
      sphi = (h == 0 ? 1 : Z) / H;
      cphi = R / H;
      h -= this._a;
    } else {
      // Treat prolate spheroids by swapping R and Z here and by switching
      // the arguments to phi = atan2(...) at the end.
      double
        p = GeoMath.sq(R / this._a),
        q = this._e2m * GeoMath.sq(Z / this._a),
        r = (p + q - this._e4a) / 6;
      if (this._f < 0) {
        //swap(p, q);
        double tmp = p;
        p = q;
        q = tmp;
      }
      if ( !(this._e4a * q == 0 && r <= 0) ) {
        double
          // Avoid possible division by zero when r = 0 by multiplying
          // equations for s and t by r^3 and r, resp.
          S = this._e4a * p * q / 4, // S = r^3 * s
          r2 = GeoMath.sq(r),
          r3 = r * r2,
          disc = S * (2 * r3 + S);
        double u = r;
        if (disc >= 0) {
          double T3 = S + r3;
          // Pick the sign on the sqrt to maximize abs(T3).  This minimizes
          // loss of precision due to cancellation.  The result is unchanged
          // because of the way the T is used in definition of u.
          T3 += T3 < 0 ? -Math.sqrt(disc) : Math.sqrt(disc); // T3 = (r * t)^3
          // N.B. cbrt always returns the real root.  cbrt(-8) = -2.
          double T = Math.pow(T3, 1/3.0); // T = r * t
          // T can be zero; but then r2 / T -> 0.
          u += T + (T != 0 ? r2 / T : 0);
        } else {
          // T is complex, but the way u is defined the result is real.
          double ang = Math.atan2(Math.sqrt(-disc), -(S + r3));
          // There are three possible cube roots.  We choose the root which
          // avoids cancellation.  Note that disc < 0 implies that r < 0.
          u += 2 * r * Math.cos(ang / 3);
        }
        double
          v = Math.sqrt(GeoMath.sq(u) + this._e4a * q), // guaranteed positive
          // Avoid loss of accuracy when u < 0.  Underflow doesn't occur in
          // e4 * q / (v - u) because u ~ e^4 when q is small and u < 0.
          uv = u < 0 ? this._e4a * q / (v - u) : u + v, // u+v, guaranteed positive
          // Need to guard against w going negative due to roundoff in uv - q.
          w = Math.max(0.0, this._e2a * (uv - q) / (2 * v)),
          // Rearrange expression for k to avoid loss of accuracy due to
          // subtraction.  Division by 0 not possible because uv > 0, w >= 0.
          k = uv / (Math.sqrt(uv + GeoMath.sq(w)) + w),
          k1 = this._f >= 0 ? k : k - this._e2,
          k2 = this._f >= 0 ? k + this._e2 : k,
          d = k1 * R / k2,
          H = Math.hypot(Z/k1, R/k2);
        sphi = (Z/k1) / H;
        cphi = (R/k2) / H;
        h = (1 - this._e2m /k1) * Math.hypot(d, Z);
      } else {                  // e4 * q == 0 && r <= 0
        // This leads to k = 0 (oblate, equatorial plane) and k + e^2 = 0
        // (prolate, rotation axis) and the generation of 0/0 in the general
        // formulas for phi and h.  using the general formula and division by 0
        // in formula for h.  So handle this case by taking the limits:
        // f > 0: z -> 0, k      ->   e2 * sqrt(q)/sqrt(e4 - p)
        // f < 0: R -> 0, k + e2 -> - e2 * sqrt(q)/sqrt(e4 - p)
        double
          zz = Math.sqrt((this._f >= 0 ? this._e4a - p : p) / this._e2m),
          xx = Math.sqrt( this._f <  0 ? this._e4a - p : p        ),
          H = Math.hypot(zz, xx);
        sphi = zz / H;
        cphi = xx / H;
        if (Z < 0) sphi = -sphi; // for tiny negative Z (not for prolate)
        h = - this._a * (this._f >= 0 ? this._e2m : 1) * H / this._e2a;
      }
    }
    double lat = Math.atan2(sphi, cphi);
    double lon = Math.atan2(slam, clam);
    List<Double> ans = List.of(lat, lon, h);
    if (M )
      ans = Stream.concat(ans.stream(), Rotation(sphi, cphi, slam, clam).stream())
              .collect(Collectors.toList());
    return ans;
  }

  List<Double> Rotation(double sphi, double cphi, double slam, double clam) {
    // This rotation matrix is given by the following quaternion operations
    // qrot(lam, [0,0,1]) * qrot(phi, [0,-1,0]) * [1,1,1,1]/2
    // or
    // qrot(pi/2 + lam, [0,0,1]) * qrot(-pi/2 + phi , [-1,0,0])
    // where
    // qrot(t,v) = [cos(t/2), sin(t/2)*v[1], sin(t/2)*v[2], sin(t/2)*v[3]]
    Double[] M = new Double[9];
    // Local X axis (east) in geocentric coords
    M[0] = -slam;        M[3] =  clam;        M[6] = 0.0;
    // Local Y axis (north) in geocentric coords
    M[1] = -clam * sphi; M[4] = -slam * sphi; M[7] = cphi;
    // Local Z axis (up) in geocentric coords
    M[2] =  clam * cphi; M[5] =  slam * cphi; M[8] = sphi;
    return Arrays.asList(M);
  }

  public double angleBetweenRadian(double[] v1, double[] v2) {
    double len1 = Math.sqrt(v1[0]*v1[0]+v1[1]*v1[1]+v1[2]*v1[2]);
    double len2 = Math.sqrt(v2[0]*v2[0]+v2[1]*v2[1]+v2[2]*v2[2]);
    double dp = v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2];
    return Math.acos((dp)/(len1*len2));
  }

  public double angleBetweenRadian(double latA, double lonA, double hA, double latB, double lonB, double hB) {
    List<Double> vAList = IntForward(latA, lonA, hA, false);
    double[] vA = listToArray(vAList);
    List<Double> vBList = IntForward(latB, lonB, hB, false);
    double[] vB = listToArray(vBList);
    return angleBetweenRadian(vA, vB);
  }
  public double angleBetweenDeg(double latA, double lonA, double hA, double latB, double lonB, double hB) {
    return rtod*angleBetweenRadian( latA,  lonA,  hA,  latB,  lonB,  hB);
  }

    /**
     * Azimuth from point A to point B. Converts to geocentric xyz taking ellipticity into account, then finds azimuth
     * as if on a sphere.
     * @param latA
     * @param lonA
     * @param hA
     * @param latB
     * @param lonB
     * @param hB
     * @return
     */
  public double azimuth(double latA, double lonA, double hA, double latB, double lonB, double hB) {
    double[] vA = listToArray(IntForward(latA, lonA, hA, false));
    double[] spLatLonA = SphericalCoords.latLonFromXYZ(vA);
    double sphlatA = spLatLonA[0];
    double sphlonA = spLatLonA[1];

    double[] vB = listToArray(IntForward(latB, lonB, hB, false));
    double[] spLatLonB = SphericalCoords.latLonFromXYZ(vB);
    double sphlatB = spLatLonB[0];
    double sphlonB = spLatLonB[1];
    return SphericalCoords.azimuth(sphlatA, sphlonA, sphlatB, sphlonB);
  }

  public double[] latLonForAzimuth(double lat, double lon, double hMeters, double azimuth, double distdeg, double pointDepthM) {
    double[] vA = listToArray(IntForward(lat, lon, hMeters, false));
    double[] spLatLon = SphericalCoords.latLonFromXYZ(vA);
    double sphLat = SphericalCoords.latFor(spLatLon[0], spLatLon[1], azimuth, distdeg);
    double sphLon = SphericalCoords.lonFor(spLatLon[0], spLatLon[1], azimuth, distdeg);
    double radius = DistAzKarney.averageRadiusMeter(new Geodesic(_a, _f));
    double[] xyz = SphericalCoords.xyzFromLatLonRadius(sphLat, sphLon, radius-pointDepthM);
    List<Double> point = IntReverse(xyz[0], xyz[1], xyz[2], false);
    return new double[] {point.get(0), point.get(1), point.get(2)};
  }

  public double length(double[] vec) {
    return Math.sqrt(vec[0]*vec[0]+vec[1]*vec[1]+vec[2]*vec[2]);
  }

  public double[] listToArray(List<Double> doubleList) {
    double[] out = new double[doubleList.size()];
    for (int i = 0; i < out.length; i++) {
      out[i] = doubleList.get(i);
    }
    return out;
  }

} // namespace GeographicLib
