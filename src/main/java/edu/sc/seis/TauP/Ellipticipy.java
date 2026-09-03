package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
# Copyright (C) 2022 Stuart Russell
"""
This module contains functions to support the calculation of ellipticity
corrections. These functions are called by the functions in the main module.

Copied from https://github.com/StuartJRussell/EllipticiPy, 2026-08-24
git commit 1bac64e158e0bc6300fc12793cea06dd385a93d3
"""


        import numpy as np
    from scipy.integrate import cumtrapz

    from obspy.taup import TauPyModel
    from obspy.taup.tau import TauModel
    from obspy.taup.seismic_phase import SeismicPhase
    from obspy.taup.helper_classes import Arrival
    from obspy.taup.utils import parse_phase_list
    from obspy.geodetics.base import gps2dist_azimuth*/

public class Ellipticipy {

//# Constants
    public static final double EARTH_LOD = 86164.0905;  // s, length of day of Earth
    public static final double G = 6.67408e-11;  // m^3 kg^-1 s^-2, universal gravitational constant


    public static Object model_epsilon(TauModel model) {
        return model_epsilon(model, EARTH_LOD);
    }
    public static Object model_epsilon(TauModel model, double lod) {
            /*
    Calculates a profile of ellipticity of figure through a planetary model.

    :param model: The tau model object
    :type model: :class:`obspy.taup.tau_model.TauModel`
    :param lod: length of day in seconds. Defaults to Earth value
    :type lod: float
    :returns: Adds arrays of epsilon (ellipticity of figure)at top and
        bottom of each velocity layer as attributes
        model.s_mod.v_mod.top_epsilon and model.s_mod.v_mod.bot_epsilon
    */

        // Angular velocity of planet
        double omega = 2 * Math.PI / lod;  // s^-1

        // Radius of planet in m
        double a = model.radiusOfEarth * 1e3;

        // Depth and density information of velocity layers
        VelocityModel v_mod = model.getVelocityModel();  // velocity_model
        double[] volume = new double[v_mod.getLayers().length+1 ];
        double total_mass = 0;
        for (VelocityLayer vLay : v_mod.getLayers()) {
            double top_depth = vLay.getTopDepth() *1e3;  // in m
            double bot_depth = vLay.getBotDepth() *1e3;  // in m
            double top_density = vLay.getTopDensity() *1e3;  // in kg m^-3
            double bot_density = vLay.getBotDensity() *1e3; // in kg m^-3
            double top_radius = a - top_depth;
            double bot_radius = a - bot_depth;

            // Mass within each spherical shell by trapezoidal rule
            double top_volume = (4.0 / 3.0) * Math.PI * Math.pow(top_radius, 3);
            double bot_volume = (4.0 / 3.0) * Math.PI * Math.pow(bot_radius, 3);
            double d_volume = top_volume-bot_volume;
            double d_mass = 0.5 * (bot_density + top_density) * d_volume;
            double mass = np.cumsum(d_mass);

            total_mass +=  mass;

            // Moment of inertia of each spherical shell by trapezoidal rule
            double j_top = (8.0 / 15.0) * Math.PI * Math.pow(top_radius, 5);
            double[] j = np.zeros(len(top_depth) + 1);
            j[1:] =j_top;
            double[] d_j = j[1:]-j[:-1];
            double d_inertia = 0.5 * (bot_density + top_density) * d_j;

            double moment_of_inertia = np.cumsum(d_inertia);

            // Calculate y (moment of inertia factor) for surfaces within the body
            double y = moment_of_inertia / (mass * Math.pow(top_radius, 2));

            // Calculate Radau's parameter
            double radau = 6.25 * Math.pow(1 - 3 * y / 2, 2) - 1;

            // Calculate h, ratio of centrifugal force and gravity for a particle
            // on the equator at the surface
            double ha = (Math.pow(a, 3) * Math.pow(omega, 2)) / (G * total_mass);

            // epsilon at surface
            double epsilona = (5 * ha) / (2 * radau[-1] + 4);

            // Solve the differential equation
            double epsilon = np.exp(cumtrapz(radau / top_radius, x = top_radius, initial = 0.0));
            epsilon = epsilona * epsilon / epsilon[-1];
        }
        // Output as model attributes
        double[] r = np.zeros(len(top_radius) + 1);
        r[1:] =top_radius;
        epsilon = np.insert(epsilon, 0, epsilon[0]);  // add a centre of planet value
        v_mod.top_epsilon = epsilon[::-1][:-1];
        v_mod.bot_epsilon = epsilon[::-1][1:];
        v_mod.lod = lod;
    }

    public static Object get_epsilon(TauModel model, double[] depth) {
            /*
        Gets ellipticity of figure for a model at a specified depth.

        :param model: The tau model object
        :type model: :class:`obspy.taup.tau_model.TauModel`
        :param depth: depth(s) in km
        :type depth: float or :class:`~numpy.ndarray`
        :returns: values of epsilon, ellipticity of figure
        :rtype: :class:`~numpy.ndarray`
        */



        // Velocity model from TauModel
        VelocityModel v_mod = model.getVelocityModel();

        // Closest index to depth
        int top_layer_idx = v_mod.layer_number_below(0.0)[0];
        int[] layer_idx = top_layer_idx * np.ones(len(depth), dtype = int);
        boolean[] cond = depth > 0.0;
        if cond.any():
        layer_idx[cond] = v_mod.layer_number_above(depth[cond]);

        // Interpolate to get epsilon value
        VelocityLayer layer = v_mod.layers[layer_idx];
        double thick = layer["bot_depth"] - layer["top_depth"];
        double bot_eps = v_mod.bot_epsilon[layer_idx];
        double top_eps = v_mod.top_epsilon[layer_idx];
        double slope = (bot_eps - top_eps) / thick;

        return slope * (depth - layer["top_depth"]) + top_eps;
    }

    public static Object weighted_alp2(int m, double theta) {
            /*
    The weighted degree 2 associated Legendre polynomial.

    :param m: order of polynomial (0, 1, or 2)
    :type m: int
    :param theta: angle
    :type theta: float
    :returns: value of weighted associated Legendre polynomial of degree 2
        and order m at x = cos(theta)
    :rtype: float
    */

        // Kronecker delta
        double kronecker_0m = m==0 ? 1 : 0;

        // Pre-factor for polynomial - Schmidt semi-normalisation
        double norm = Math.sqrt(
                (2 - kronecker_0m) * (np.math.factorial(2 - m) / np.math.factorial(2 + m))
        );

        // Return polynomial of degree 2 and order m
        switch(m) {
            case 0:
                return norm * 0.5 * (3.0 * np.cos(theta) * * 2 - 1.0);
            case 1:
                return norm * 3.0 * np.cos(theta) * np.sin(theta);
            case 2:
                return norm * 3.0 * np.sin(theta) * * 2;
            default:
                throw new IllegalArgumentException("Invalid value of m: "+m);
        }
    }

    public static Object ellipticity_coefficients(List<Arrival> arrivals) {
        return ellipticity_coefficients(arrivals, EARTH_LOD);
    }

    public static Object ellipticity_coefficients(List<Arrival> arrivals, double lod) {
            /*
    Ellipticity coefficients for a set of arrivals.

    :param arrivals: TauP Arrival or Arrivals object with ray paths calculated.
    :type arrivals: :class:`obspy.taup.tau.Arrivals` or
                    :class:`obspy.taup.tau.Arrival`
    :param lod: optional, length of day in seconds. Defaults to Earth value
    :type lod: float
    :returns: list of lists of three floats, ellipticity coefficients
    :rtype: list[list] for Arrivals, list for Arrival

    Usage:

    >>> from obspy.taup import TauPyModel
    >>> from ellipticipy.tools import ellipticity_coefficients
    >>> model = TauPyModel('prem')
    >>> arrivals = model.get_ray_paths(source_depth_in_km = 124,
        distance_in_degree = 65, phase_list = ['pPKiKP'])
    >>> ellipticity_coefficients(arrivals)
    [[-0.9293229194820186, -0.6859308201378412, -0.8799047487163734]]
    */
        if isinstance(arrivals, Arrival):
        return individual_ellipticity_coefficients(arrivals, lod);
        return [individual_ellipticity_coefficients(arr, lod) for arr in arrivals];
    }

    public static double[] individual_ellipticity_coefficients(Arrival arrival, double lod) {
            /*
    Ellipticity coefficients for a single ray path.

    :param arrival: TauP Arrival object with ray path calculated.
    :type arrival: :class:`obspy.taup.helper_classes.Arrival`
    :param lod: optional, length of day in seconds. Defaults to Earth value
    :type lod: float
    :returns: list of three floats, ellipticity coefficients
    :rtype: list
    */

        // Calculate epsilon values if they don't already exist
        TauModel model = arrival.phase.tau_model;
        if not hasattr (model.s_mod.v_mod, "top_epsilon")or model.s_mod.v_mod.lod != lod:
        model_epsilon(model, lod);

        // Coefficients from continuous ray path
        double ray_sigma = integral_coefficients(arrival);

        // Coefficients from discontinuities
        double disc_sigma = discontinuity_coefficients(arrival);

        // Sum the contribution from the ray path and the discontinuities
        // to get final coefficients
        double sigma = [ray_sigma[m] + disc_sigma[m] for m in[ 0, 1, 2]];

        return sigma;
    }

    public static Object expected_delay_time(double ray_param, double depth0, double depth1, boolean wave, TauModel model) {
            /*
    Expected delay time between two depths for a given wave type (p or s).
    */

        // Convert depths to radii
        double radius0 = model.getRadiusOfEarth() - depth0;
        double radius1 = model.getRadiusOfEarth() - depth1;

        // Velocity model from TauModel
        VelocityModel v_mod = model.getVelocityModel();

        // Get velocities
        double v0;
        double v1;
        if (depth1 >= depth0) {
            v0 = v_mod.evaluate_below(depth0, wave)[0];
            v1 = v_mod.evaluate_above(depth1, wave)[0];
        } else {
            v0 = v_mod.evaluate_above(depth0, wave)[0];
            v1 = v_mod.evaluate_below(depth1, wave)[0];
        }
        // Calculate time for segment if velocity non-zero
        // - if velocity zero then return zero time
        if (v0 > 0.0) {

            double eta0 = radius0 / v0;
            double eta1 = radius1 / v1;

            double n0 = vertical_slowness(eta0, ray_param);
            double n1 = vertical_slowness(eta1, ray_param);

            if (ray_param == 0.0) {
                return 0.5 * ((1.0 / v0) + (1.0 / v1)) * Math.abs(radius1 - radius0);
            }
            return 0.5 * (n0 + n1) * Math.abs(Math.log(radius1 / radius0));

        }
        return 0.0;

    }

    public static double vertical_slowness(double eta, double p) {
        double y = Math.pow(eta, 2) - Math.pow(p, 2);
        return (y>0)?Math.sqrt(y) : 0.0;  // in s
    }

    public static Object integral_coefficients(Arrival arrival) {
            /*
    Ellipticity coefficients due to integral along ray path.
    */
        TauModel model = arrival.phase.tau_model;

        // Split the ray path
        List<ArrivalPathSegment> segmentList = arrival.getPathSegments();

        // Loop through path segments
        List<Double> sigmas = new ArrayList<>();
        for (ArrivalPathSegment seg : segmentList) {
            for (TimeDist td : seg.getPath()) {

                // Depth in km
                double depth = path["depth"];
                double max_depth = np.max(depth);

                // Radius in km
                double radius = model.getRadiusOfEarth() - depth;

                // Velocity in km/s
                VelocityModel v_mod = model.getVelocityModel();
                boolean cond = depth != max_depth;
                double[] v = np.zeros_like(depth);
                v[cond] = v_mod.evaluate_below(depth[cond], wave);
                v[~cond] = v_mod.evaluate_above(max_depth, wave);

                // eta in s
                double eta = radius / v;

                // epsilon
                double epsilon = get_epsilon(model, depth);

                // Epicentral distance in radians
                double distance = path["dist"];

                // lambda
                lam = [-(2.0 / 3.0) * weighted_alp2(m, distance) for m in[ 0, 1, 2]];

                // Vertical slowness
                y = eta * * 2 - arrival.ray_param * * 2;
                vertical_slowness = np.sqrt(y * (y > 0));  // in s

                // Make velocities for bottoming rays consistent
                min_idx = np.argmin(radius);
                if arrival.ray_param > 0.0 and min_idx !=0 and min_idx !=(len(radius) - 1):
                // We have a bottoming ray
                eta[min_idx] = arrival.ray_param;
                v[min_idx] = radius[min_idx] / eta[min_idx];
                vertical_slowness[min_idx] = 0.0;

                // the Bullen (1963) quantity d log(r)/d log(eta)
                r_top = radius[1:];
                r_bot = radius[:-1];

                v_top = v[1:];
                v_bot = v[:-1];

                with np.errstate(divide = "ignore", invalid = "ignore"):
                // centre of planet log(0.0) will evaluate as -np.inf, which is ok, don't warn
                dlogr = np.log(r_top) - np.log(r_bot);
                dlogv = np.log(v_top) - np.log(v_bot);
                dlogr_dlogeta = 1.0 / (1.0 - dlogv / dlogr);

                // remove nans caused by a zero thickness layer
                dlogr_dlogeta[r_top == r_bot] = 1.0;

                sigmas.append([integration(m) for m in[ 0, 1, 2]]);
            }
        }
        // Sum coefficients for each segment to get total ray path contribution
        return [np.sum([s[m] for s in sigmas])for m in[ 0, 1, 2]]
    }

    // Do the integration by trapezoidal rule
    public static Object integration(m) {
        integrand = epsilon * lam[m];
        top = integrand[1:];
        bot = integrand[:-1];

        delta = abs(vertical_slowness[1:]-vertical_slowness[:-1]);

        return np.sum(0.5 * (top + bot) * (dlogr_dlogeta - 1.0) * delta);
    }

    public static double[] discontinuity_contribution(List<TimeDist> points, boolean phase, TauModel model) {
            /*
    Ellipticity coefficients due to an individual discontinuity.
    */

        // Use closest points to the boundary
        TimeDist disc_point = points[0];
        TimeDist neighbour_point = points[1];

        // Ray parameter
        double ray_param = disc_point["p"];

        // Distance in radians
        double distance = disc_point["dist"];

        // Radius in km
        double depth = disc_point["depth"];
        double radius = model.radius_of_earth - depth;
        double neighbour_depth = neighbour_point["depth"];

        // Get velocity on appropriate side of the boundary
        double v;
        if (neighbour_depth >= depth) {
            v = model.s_mod.v_mod.evaluate_below(depth, phase)[0];
        } else {
            v = model.s_mod.v_mod.evaluate_above(depth, phase)[0];
        }
        // Vertical slowness
        double eta = radius / v;
        double y = Math.pow(eta, 2) - Math.pow(ray_param, 2);
        double vertical_slowness = y>0 ? Math.sqrt(y) : 0.0;

        // If ray does not change depth then this should have no contribution
        if (neighbour_depth == depth) {
            vertical_slowness = 0.0;
        }

        // Above/below sign, positive if above
        int sign = np.sign(depth - neighbour_depth);

        // epsilon at this depth
        double epsilon = get_epsilon(model, depth);

        // lambda at this distance
        double lam = [-(2.0 / 3.0) * weighted_alp2(m, distance) for m in[ 0, 1, 2]];

        // Coefficients for this discontinuity
        double[] sigma = np.array([-sign * vertical_slowness * epsilon * lam[m] for m in[ 0, 1, 2]])

        return sigma;
    }

    public static Object discontinuity_coefficients(Arrival arrival) {
            /*
    Ellipticity coefficients due to all discontinuities.
    */
        TauModel model = arrival.phase.tau_model;

        // Split the ray path
        paths, waves = split_ray_path(arrival);

        // Loop through path segments
        sigmas = [];
        for path, wave in zip (paths, waves):
        start_points = (path[0], path[1]);
        end_points = (path[-1], path[-2]);

        // Contributions from each end of the ray path segment
        start = discontinuity_contribution(start_points, wave, model);
        end = discontinuity_contribution(end_points, wave, model);

        // Overall contribution
        sigmas.append(start + end);

        // Sum the coefficients from all discontinuities
        disc_sigma = [np.sum([s[m] for s in sigmas])for m in[ 0, 1, 2]];

        return disc_sigma;
    }

    public static Object correction_from_coefficients(double[][] coefficients, double azimuth, double source_latitude) {
            /*
    Ellipticity correction given the ellipticity coefficients.
    */
        // Enforce that event latitude must be in range -90 to 90 degrees
        if (source_latitude < - 90 || 90 < source_latitude) {
            throw new IllegalArgumentException("Source latitude must be in range -90 to 90 degrees: "+source_latitude);
        }
        // Enforce that azimuth must be in range 0 to 360 degrees
        if (azimuth < 0 || 360 < azimuth) {
            throw new IllegalArgumentException("Azimuth must be in range 0 to 360 degrees");
        }
        // Convert latitude to colatitude
        double colatitude = np.radians(90 - source_latitude);

        // Convert azimuth to radians
        double azimuth = np.radians(azimuth);

        return sum(
                coefficients[m] * weighted_alp2(m, colatitude) * np.cos(m * azimuth)
        for m in[ 0, 1, 2]
            )
    }

    public static Object azimuth_source_latitude_from_geo_arrival(Arrival arrival) {
            /*
    For an arrival with a taup.TimeDistGeo path calculate azimuth and source latitude.
    */
        if "lat" not in arrival.path.dtype.names:
        raise ValueError ("Unable to determine source latitude and azimuth from Arrival")

        source_latitude = arrival.path["lat"][0]
        source_longitude = arrival.path["lon"][0]
        receiver_latitude = arrival.path["lat"][-1]
        receiver_longitude = arrival.path["lon"][-1]

        azimuth = gps2dist_azimuth(
                source_latitude, source_longitude, receiver_latitude, receiver_longitude
        )[1]
        return azimuth,source_latitude;
    }

    public static Object table_ellipticity_coefficients(
            List<String> phase_list, TauModel model
    ) {
        return table_ellipticity_coefficients(phase_list, model, 0, 0, EARTH_LOD);
    }

    public static Object table_ellipticity_coefficients(
            List<String> phase_list, TauModel model, double source_depth_in_km, double receiver_depth_in_km, double lod
    ) throws TauModelException {
            /*
    Produce a table of ellipticity coefficients for a given phase.

    :param phase_list: Phase name, e.g. "PKP" or list of phase names
    :type phase_list: str or list
    :param model: The model object
    :type model: :class:`obspy.taup.tau.TauPyModel` or
                 :class:`obspy.taup.tau_model.TauModel`
    :param source_depth_in_km: Source depth in km
    :type source_depth_in_km: float
    :param receiver_depth_in_km: Receiver depth in km
    :type receiver_depth_in_km: float
    :param lod: length of day in seconds. Defaults to Earth value
    :type lod: float

    :returns: Table of ellipticity coefficients
    :rtype: dict
    */

        List<String> phase_names = sorted(parse_phase_list(phase_list));

        // correct TauModel for source depth
        TauModel depth_corrected_model = model.depthCorrect(source_depth_in_km);

        HashMap<String, EllipCoef> tables = new HashMap();
        for (String phase_name : phase_names) {
            SeismicPhase ph = SeismicPhaseFactory.createPhase(phase_name, depth_corrected_model, receiver_depth_in_km);

            double[] ellip_coeffs = np.zeros((len(ph.ray_param), 3));
            for (int idx = 0; idx < ph.getRayParams().length; idx++) {

                Arrival arrival = ph.createArrivalAtIndex(idx);
                arrival.getPath();
                double[] ellip = individual_ellipticity_coefficients(arrival, lod);
                ellip_coeffs[idx, :] =ellip;
            }
            tables[phase_name] = {
                    "ray_param":ph.ray_param,
                    "degrees":(180.0 / np.pi) * ph.dist,
                    "dist":ph.dist,
                    "time":ph.time,
                    "ellip_coeffs":ellip_coeffs,
    }
        }

        return tables;
    }
}

class EllipCoef {
    public EllipCoef(double rayParam, double degrees, double dist, double time, double[] ellip_coeffs) {
        this.rayParam = rayParam;
        this.degrees = degrees;
        this.dist = dist;
        this.time = time;
        this.ellip_coeffs = ellip_coeffs;
    }

    double rayParam;
    double degrees;
    double dist;
    double time;
    double[] ellip_coeffs;
}