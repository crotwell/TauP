package edu.sc.seis.TauP;

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
        for (VelocityLayer vLay : v_mod.getLayers()) {
            double top_depth = v_mod.layers["top_depth"][::-1] *1e3  // in m
            double top_density = v_mod.layers["top_density"][::-1] *1e3  // in kg m^-3
            double bot_density = v_mod.layers["bot_density"][::-1] *1e3  // in kg m^-3
            double top_radius = a - top_depth

            // Mass within each spherical shell by trapezoidal rule
            double top_volume = (4.0 / 3.0) * np.pi * top_radius * * 3
            volume = np.zeros(len(top_depth) + 1)
            volume[1:] =top_volume
                    d_volume = volume[1:]-volume[:-1]
            d_mass = 0.5 * (bot_density + top_density) * d_volume
            mass = np.cumsum(d_mass)

            double total_mass = mass[-1]

            // Moment of inertia of each spherical shell by trapezoidal rule
            double j_top = (8.0 / 15.0) * np.pi * top_radius * * 5
            double[] j = np.zeros(len(top_depth) + 1)
            j[1:] =j_top
                    d_j = j[1:]-j[:-1]
            d_inertia = 0.5 * (bot_density + top_density) * d_j

            double moment_of_inertia = np.cumsum(d_inertia)

            // Calculate y (moment of inertia factor) for surfaces within the body
            y = moment_of_inertia / (mass * top_radius * * 2)

            // Calculate Radau's parameter
            radau = 6.25 * (1 - 3 * y / 2) * * 2 - 1

            // Calculate h, ratio of centrifugal force and gravity for a particle
            // on the equator at the surface
            ha = (a * * 3 * omega * * 2) / (G * total_mass)

            // epsilon at surface
            epsilona = (5 * ha) / (2 * radau[-1] + 4)

            // Solve the differential equation
            epsilon = np.exp(cumtrapz(radau / top_radius, x = top_radius, initial = 0.0))
            epsilon = epsilona * epsilon / epsilon[-1]
        }
        // Output as model attributes
        r = np.zeros(len(top_radius) + 1)
        r[1:] =top_radius
                epsilon = np.insert(epsilon, 0, epsilon[0])  // add a centre of planet value
        v_mod.top_epsilon = epsilon[::-1][:-1]
        v_mod.bot_epsilon = epsilon[::-1][1:]
        v_mod.lod = lod
    }

    public static Object get_epsilon(model, depth) {
            /*
    Gets ellipticity of figure for a model at a specified depth.

    :param model: The tau model object
    :type model: :class:`obspy.taup.tau_model.TauModel`
    :param depth: depth(s) in km
    :type depth: float or :class:`~numpy.ndarray`
    :returns: values of epsilon, ellipticity of figure
    :rtype: :class:`~numpy.ndarray`
    */

        if isinstance(depth, float):
        depth = np.array([depth])

        // Velocity model from TauModel
        v_mod = model.s_mod.v_mod

        // Closest index to depth
        top_layer_idx = v_mod.layer_number_below(0.0)[0]
        layer_idx = top_layer_idx * np.ones(len(depth), dtype = int)
        cond = depth > 0.0
        if cond.any():
        layer_idx[cond] = v_mod.layer_number_above(depth[cond])

        // Interpolate to get epsilon value
        layer = v_mod.layers[layer_idx]
        thick = layer["bot_depth"] - layer["top_depth"]
        bot_eps = v_mod.bot_epsilon[layer_idx]
        top_eps = v_mod.top_epsilon[layer_idx]
        slope = (bot_eps - top_eps) / thick

        return slope * (depth - layer["top_depth"]) + top_eps
    }

    public static Object weighted_alp2(m, theta) {
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
        kronecker_0m = 1 if m == 0 else 0

        // Pre-factor for polynomial - Schmidt semi-normalisation
        norm = np.sqrt(
                (2 - kronecker_0m) * (np.math.factorial(2 - m) / np.math.factorial(2 + m))
        )

        // Return polynomial of degree 2 and order m
        if m == 0:
        return norm * 0.5 * (3.0 * np.cos(theta) * * 2 - 1.0)
        if m == 1:
        return norm * 3.0 * np.cos(theta) * np.sin(theta)
        if m == 2:
        return norm * 3.0 * np.sin(theta) * * 2
        raise ValueError ("Invalid value of m")
    }

    public static Object ellipticity_coefficients(arrivals, lod=EARTH_LOD) {
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
        return individual_ellipticity_coefficients(arrivals, lod = lod)
        return [individual_ellipticity_coefficients(arr, lod = lod) for arr in arrivals]
    }

    public static Object individual_ellipticity_coefficients(arrival, lod=EARTH_LOD) {
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
        model = arrival.phase.tau_model
        if not hasattr (model.s_mod.v_mod, "top_epsilon")or model.s_mod.v_mod.lod != lod:
        model_epsilon(model, lod)

        // Coefficients from continuous ray path
        ray_sigma = integral_coefficients(arrival)

        // Coefficients from discontinuities
        disc_sigma = discontinuity_coefficients(arrival)

        // Sum the contribution from the ray path and the discontinuities
        // to get final coefficients
        sigma = [ray_sigma[m] + disc_sigma[m] for m in[ 0, 1, 2]]

        return sigma
    }

    public static Object split_ray_path(arrival) {
            /*
    Split and label ray path according to type of wave.
    */
        model = arrival.phase.tau_model

        // Get discontinuity depths in the model in km
        discs = model.s_mod.v_mod.get_discontinuity_depths()[:-1]

        // Split path at discontinuity depths
        full_path = arrival.path
        depths = full_path["depth"]
        is_disc = np.isin(depths, discs)
        is_disc[0] = False  // Don't split on first point
        idx = np.where(is_disc)[0]

        // Split ray paths, including start and end points
        splitted = np.split(full_path, idx)
        dpaths = [np.append(s, splitted[i + 1][0]) for i, s in enumerate (splitted[:-1])]

        // Classify the waves - P or S
        dwaves = [classify_path(path, model) for path in dpaths]

        // Construct final path list by removing diffracted segments
        paths = [p for p, w in zip (dpaths, dwaves)if w != "diff"]
        waves = [w for p, w in zip (dpaths, dwaves)if w != "diff"]

        // Enforce that paths and waves are the same length
        // Something has gone wrong if not
        assert len(paths) == len(waves)

        return paths,waves
    }

    public static Object expected_delay_time(ray_param, depth0, depth1, wave, model) {
            /*
    Expected delay time between two depths for a given wave type (p or s).
    */

        // Convert depths to radii
        radius0 = model.radius_of_planet - depth0
        radius1 = model.radius_of_planet - depth1

        // Velocity model from TauModel
        v_mod = model.s_mod.v_mod

        // Get velocities
        if depth1 >= depth0:
        v0 = v_mod.evaluate_below(depth0, wave)[0]
        v1 = v_mod.evaluate_above(depth1, wave)[0]
            else:
        v0 = v_mod.evaluate_above(depth0, wave)[0]
        v1 = v_mod.evaluate_below(depth1, wave)[0]

        // Calculate time for segment if velocity non-zero
        // - if velocity zero then return zero time
        if v0 > 0.0:

        eta0 = radius0 / v0
        eta1 = radius1 / v1
    }

    public static Object vertical_slowness (eta, p){
            y = eta * * 2 - p * * 2
            return np.sqrt(y * (y > 0))  // in s

            n0 = vertical_slowness(eta0, ray_param)
            n1 = vertical_slowness(eta1, ray_param)

            if ray_param == 0.0:
            return 0.5 * ((1.0 / v0) + (1.0 / v1)) * abs(radius1 - radius0)
            return 0.5 * (n0 + n1) * abs(np.log(radius1 / radius0))

            return 0.0
        }

        public static Object classify_path (path, model):
            /*
    Determine whether we have a p or s-wave path by comparing delay times.
    */

        // Examine just the first two points near the shallowest part of the path
        if path[0]["depth"] < path[-1]["depth"]:
        point0 = path[0]
        point1 = path[1]
            else:
        point0 = path[-2]
        point1 = path[-1]

        // Ray parameter
        ray_param = point0["p"]

        // Depths
        depth0 = point0["depth"]
        depth1 = point1["depth"]

        // If no change in depth then this is a diffracted/head wave segment
        if depth0 == depth1:
        return "diff"

        // Delay time for this segment from ObsPy ray path
        travel_time = point1["time"] - point0["time"]
        distance = abs(point0["dist"] - point1["dist"])
        delay_time = travel_time - ray_param * distance

        // Get the expected delay time for each wave type
        delay_p = expected_delay_time(ray_param, depth0, depth1, "p", model)
        delay_s = expected_delay_time(ray_param, depth0, depth1, "s", model)

        // Difference between predictions and given delay times
        error_p = (delay_p / delay_time) - 1.0
        error_s = (delay_s / delay_time) - 1.0

        // Check which wave type matches the given delay time the best
        if abs(error_p) < abs(error_s):
        return "p"
        return "s"
    }

    public static Object integral_coefficients(arrival) {
            /*
    Ellipticity coefficients due to integral along ray path.
    */
        model = arrival.phase.tau_model

        // Split the ray path
        paths, waves = split_ray_path(arrival)

        // Loop through path segments
        sigmas = []
        for path, wave in zip (paths, waves):

        // Depth in km
        depth = path["depth"]
        max_depth = np.max(depth)

        // Radius in km
        radius = model.radius_of_planet - depth

        // Velocity in km/s
        v_mod = model.s_mod.v_mod
        cond = depth != max_depth
        v = np.zeros_like(depth)
        v[cond] = v_mod.evaluate_below(depth[cond], wave)
        v[~cond] = v_mod.evaluate_above(max_depth, wave)

        // eta in s
        eta = radius / v

        // epsilon
        epsilon = get_epsilon(model, depth)

        // Epicentral distance in radians
        distance = path["dist"]

        // lambda
        lam = [-(2.0 / 3.0) * weighted_alp2(m, distance) for m in[ 0, 1, 2]]

        // Vertical slowness
        y = eta * * 2 - arrival.ray_param * * 2
        vertical_slowness = np.sqrt(y * (y > 0))  // in s

        // Make velocities for bottoming rays consistent
        min_idx = np.argmin(radius)
        if arrival.ray_param > 0.0 and min_idx !=0 and min_idx !=(len(radius) - 1):
        // We have a bottoming ray
        eta[min_idx] = arrival.ray_param
        v[min_idx] = radius[min_idx] / eta[min_idx]
        vertical_slowness[min_idx] = 0.0

        // the Bullen (1963) quantity d log(r)/d log(eta)
        r_top = radius[1:]
        r_bot = radius[:-1]

        v_top = v[1:]
        v_bot = v[:-1]

        with np.errstate(divide = "ignore", invalid = "ignore"):
        // centre of planet log(0.0) will evaluate as -np.inf, which is ok, don't warn
        dlogr = np.log(r_top) - np.log(r_bot)
        dlogv = np.log(v_top) - np.log(v_bot)
        dlogr_dlogeta = 1.0 / (1.0 - dlogv / dlogr)

        // remove nans caused by a zero thickness layer
        dlogr_dlogeta[r_top == r_bot] = 1.0
    }
            // Do the integration by trapezoidal rule
    public static Object integration(m) {
        integrand = epsilon * lam[m]
        top = integrand[1:]
        bot = integrand[:-1]

        delta = abs(vertical_slowness[1:]-vertical_slowness[:-1])

        return np.sum(0.5 * (top + bot) * (dlogr_dlogeta - 1.0) * delta)

        sigmas.append([integration(m) for m in[ 0, 1, 2]])

        // Sum coefficients for each segment to get total ray path contribution
        return [np.sum([s[m] for s in sigmas])for m in[ 0, 1, 2]]
    }

    public static Object discontinuity_contribution(points, phase, model) {
            /*
    Ellipticity coefficients due to an individual discontinuity.
    */

        // Use closest points to the boundary
        disc_point = points[0]
        neighbour_point = points[1]

        // Ray parameter
        ray_param = disc_point["p"]

        // Distance in radians
        distance = disc_point["dist"]

        // Radius in km
        depth = disc_point["depth"]
        radius = model.radius_of_planet - depth
        neighbour_depth = neighbour_point["depth"]

        // Get velocity on appropriate side of the boundary
        if neighbour_depth >= depth:
        v = model.s_mod.v_mod.evaluate_below(depth, phase)[0]
            else:
        v = model.s_mod.v_mod.evaluate_above(depth, phase)[0]

        // Vertical slowness
        eta = radius / v
        y = eta * * 2 - ray_param * * 2
        vertical_slowness = np.sqrt(y * (y > 0))

        // If ray does not change depth then this should have no contribution
        if neighbour_depth == depth:
        vertical_slowness = 0.0

        // Above/below sign, positive if above
        sign = np.sign(depth - neighbour_depth)

        // epsilon at this depth
        epsilon = get_epsilon(model, depth)

        // lambda at this distance
        lam = [-(2.0 / 3.0) * weighted_alp2(m, distance) for m in[ 0, 1, 2]]

        // Coefficients for this discontinuity
        sigma = np.array([-sign * vertical_slowness * epsilon * lam[m] for m in[ 0, 1, 2]])

        return sigma
    }

    public static Object discontinuity_coefficients(arrival) {
            /*
    Ellipticity coefficients due to all discontinuities.
    */
        model = arrival.phase.tau_model

        // Split the ray path
        paths, waves = split_ray_path(arrival)

        // Loop through path segments
        sigmas = []
        for path, wave in zip (paths, waves):
        start_points = (path[0], path[1])
        end_points = (path[-1], path[-2])

        // Contributions from each end of the ray path segment
        start = discontinuity_contribution(start_points, wave, model)
        end = discontinuity_contribution(end_points, wave, model)

        // Overall contribution
        sigmas.append(start + end)

        // Sum the coefficients from all discontinuities
        disc_sigma = [np.sum([s[m] for s in sigmas])for m in[ 0, 1, 2]]

        return disc_sigma
    }

    public static Object correction_from_coefficients(coefficients, azimuth, source_latitude) {
            /*
    Ellipticity correction given the ellipticity coefficients.
    */
        // Enforce that event latitude must be in range -90 to 90 degrees
        if not - 90 <= source_latitude <= 90:
        raise ValueError ("Source latitude must be in range -90 to 90 degrees")

        // Enforce that azimuth must be in range 0 to 360 degrees
        if not 0 <= azimuth <= 360:
        raise ValueError ("Azimuth must be in range 0 to 360 degrees")

        // Convert latitude to colatitude
        colatitude = np.radians(90 - source_latitude)

        // Convert azimuth to radians
        azimuth = np.radians(azimuth)

        return sum(
                coefficients[m] * weighted_alp2(m, colatitude) * np.cos(m * azimuth)
        for m in[ 0, 1, 2]
            )
    }

    public static Object azimuth_source_latitude_from_geo_arrival(arrival) {
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
        return azimuth,source_latitude
    }

    public static Object table_ellipticity_coefficients(
            phase_list, model, source_depth_in_km=0.0, receiver_depth_in_km=0.0, lod=EARTH_LOD
    ) {
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

        if isinstance(model, TauPyModel):
        model = model.model

        if isinstance(phase_list, str):
        phase_names = sorted(parse_phase_list([phase_list]))
            else:
        phase_names = sorted(parse_phase_list(phase_list))

        // correct TauModel for source depth
        depth_corrected_model = model.depth_correct(source_depth_in_km)

        tables = {}
        for phase_name in phase_names:
        ph = SeismicPhase(phase_name, depth_corrected_model, receiver_depth_in_km)

        ellip_coeffs = np.zeros((len(ph.ray_param), 3))
        for idx, p in enumerate (ph.ray_param):
        arrival = Arrival(
                ph,
                0.0,
                ph.time[idx],
                ph.dist[idx],
                ph.ray_param[idx],
                idx,
                ph.name,
                ph.purist_name,
                source_depth_in_km,
                receiver_depth_in_km,
                )
        arrival = ph.calc_path_from_arrival(arrival)
        ellip = individual_ellipticity_coefficients(arrival, lod = lod)
        ellip_coeffs[idx, :] =ellip

        tables[phase_name] = {
                "ray_param":ph.ray_param,
                "degrees":(180.0 / np.pi) * ph.dist,
                "dist":ph.dist,
                "time":ph.time,
                "ellip_coeffs":ellip_coeffs,
    }

        if isinstance(phase_list, str) and len (tables) == 1:
        return tables[phase_name]

        return tables
    }
}
