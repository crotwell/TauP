package edu.sc.seis.TauP;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ellipticipy {

    public static final double EARTH_LOD = 86164.0905; // s
    public static final double G = 6.67408e-11; // m^3 kg^-1 s^-2

    private final double lod;
    private final TauModel model;
    VelocityModel vMod;
    private HashMap<VelocityLayer, Double> top_epsilon = new HashMap<>();
    private HashMap<VelocityLayer, Double> bot_epsilon = new HashMap<>();

    public Ellipticipy(TauModel tMod) {
        this(tMod, EARTH_LOD);
    }
    public Ellipticipy(TauModel tMod, double lod) {
        this.lod = lod;
        this.model = tMod;
        vMod = model.getVelocityModel();
        modelEpsilon();
    }

    public void modelEpsilon() {
        double omega = 2.0 * Math.PI / lod;
        double a = model.radiusOfEarth * 1e3;

        double[] topRadiusMeter = new double[vMod.getNumLayers()];
        double[] radau = new double[vMod.getNumLayers()];
        double cumulativeMass = 0.0;
        double cumulativeInertia = 0.0;
        for (int i = vMod.getNumLayers(); i >= 0; i--) {
            VelocityLayer vLayer = vMod.getVelocityLayer(i);
            topRadiusMeter[i] = vLayer.getTopRadius(vMod.getRadiusOfEarth()) * 1e3;
            cumulativeMass += vLayer.calcMass(vMod.getRadiusOfEarth());
            double shellInertia = vLayer.calcMomentOfInertia(vMod.getRadiusOfEarth());
            cumulativeInertia += shellInertia;
            double y = cumulativeInertia / (cumulativeMass * Math.pow(vLayer.getTopRadius(vMod.getRadiusOfEarth()), 2));
            radau[i] = 6.25 * Math.pow(1.0 - 3.0 * y / 2.0, 2) - 1.0;
        }
        double totalMass = cumulativeMass;

        double ha = (Math.pow(a, 3) * Math.pow(omega, 2)) / (G * totalMass);
        double epsilona = (5.0 * ha) / (2.0 * radau[radau.length - 1] + 4.0);

        double[] integrand = new double[vMod.getNumLayers()];
        for (int i = vMod.getNumLayers(); i >= 0; i--) {
            integrand[i] = radau[i] / topRadiusMeter[i];
        }

        double[] epsilon = cumtrapz(integrand, topRadiusMeter);
        for (int i = 0; i < epsilon.length; i++) {
            epsilon[i] = epsilona * epsilon[i] / epsilon[epsilon.length - 1];
        }

        double[] epsilonWithCenter = new double[epsilon.length + 1];
        epsilonWithCenter[0] = epsilon[0];
        System.arraycopy(epsilon, 0, epsilonWithCenter, 1, epsilon.length);

        for (int i = vMod.getNumLayers(); i >= 0; i--) {
            VelocityLayer vLayer = vMod.getVelocityLayer(i);
            this.bot_epsilon.put(vLayer, epsilonWithCenter[i]);
            this.top_epsilon.put(vLayer, epsilonWithCenter[i+1]);
        }

    }

    public double[] getEpsilon(TauModel model, double[] depth) throws NoSuchLayerException {
        double[] result = new double[depth.length];
        for (int i = 0; i < depth.length; i++) {
            int layerIdx = 0;
            if (depth[i] > 0.0) {
                layerIdx = vMod.layerNumberAbove(depth[i]);
            }
            VelocityLayer vLayer = vMod.getVelocityLayer(layerIdx);
            double botEps = bot_epsilon.get(vLayer);
            double topEps = top_epsilon.get(vLayer);
            double slope = (botEps - topEps) / vLayer.getThickness();
            result[i] = slope * (depth[i] - vLayer.getTopDepth()) + topEps;
        }
        return result;
    }

    public double getEpsilon(TauModel model, double depth) throws NoSuchLayerException {
        return getEpsilon(model, new double[] { depth })[0];
    }

    public static double weightedAlp2(int m, double theta) {
        int kronecker0m = (m == 0) ? 1 : 0;
        double norm = Math.sqrt((2 - kronecker0m) * (factorial(2 - m) / factorial(2 + m)));

        if (m == 0) {
            return norm * 0.5 * (3.0 * Math.cos(theta) * Math.cos(theta) - 1.0);
        }
        if (m == 1) {
            return norm * 3.0 * Math.cos(theta) * Math.sin(theta);
        }
        if (m == 2) {
            return norm * 3.0 * Math.sin(theta) * Math.sin(theta);
        }
        throw new IllegalArgumentException("Invalid value of m");
    }

    private static double factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative");
        }
        double result = 1.0;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    public List<Double> ellipticityCoefficients(Arrival arrival, double lod) throws NoSuchLayerException {
        return individualEllipticityCoefficients(arrival, lod);
    }

    public List<List<Double>> ellipticityCoefficients(List<Arrival> arrivals, double lod) throws NoSuchLayerException {
        List<List<Double>> result = new ArrayList<>();
        for (Arrival arr : arrivals) {
            result.add(individualEllipticityCoefficients(arr, lod));
        }
        return result;
    }

    public List<Double> individualEllipticityCoefficients(Arrival arrival, double lod) throws NoSuchLayerException {
        double[] raySigma = integralCoefficients(arrival);
        double[] discSigma = discontinuityCoefficients(arrival);

        List<Double> sigma = new ArrayList<>();
        for (int m = 0; m < 3; m++) {
            sigma.add(raySigma[m] + discSigma[m]);
        }
        return sigma;
    }

    public double expectedDelayTime(double rayParam, double depth0, double depth1, VelocityModelMaterial wave) throws NoSuchLayerException {
        double radius0 = vMod.radiusOfEarth - depth0;
        double radius1 = vMod.radiusOfEarth - depth1;

        double v0;
        double v1;

        if (depth1 >= depth0) {
            v0 = vMod.evaluateBelow(depth0, wave);
            v1 = vMod.evaluateAbove(depth1, wave);
        } else {
            v0 = vMod.evaluateAbove(depth0, wave);
            v1 = vMod.evaluateBelow(depth1, wave);
        }

        if (v0 > 0.0) {
            double eta0 = radius0 / v0;
            double eta1 = radius1 / v1;

            double n0 = verticalSlowness(eta0, rayParam);
            double n1 = verticalSlowness(eta1, rayParam);

            if (rayParam == 0.0) {
                return 0.5 * ((1.0 / v0) + (1.0 / v1)) * Math.abs(radius1 - radius0);
            }

            return 0.5 * (n0 + n1) * Math.abs(Math.log(radius1 / radius0));
        }

        return 0.0;
    }

    private static double verticalSlowness(double eta, double p) {
        double y = Math.pow(eta, 2) - Math.pow(p, 2);
        return Math.sqrt(Math.max(y, 0.0));
    }

    public String classifyPath(List<TimeDist> path, TauModel model) throws NoSuchLayerException {
        TimeDist point0;
        TimeDist point1;

        if (path.get(0).getDepth() < path.get(path.size() - 1).getDepth()) {
            point0 = path.get(0);
            point1 = path.get(1);
        } else {
            point0 = path.get(path.size() - 2);
            point1 = path.get(path.size() - 1);
        }

        double rayParam = point0.getP();
        double depth0 = point0.getDepth();
        double depth1 = point1.getDepth();

        if (depth0 == depth1) {
            return "diff";
        }

        double travelTime = point1.getTime() - point0.getTime();
        double distance = Math.abs(point0.getDistRadian() - point1.getDistRadian());
        double delayTime = travelTime - rayParam * distance;

        double delayP = expectedDelayTime(rayParam, depth0, depth1, VelocityModelMaterial.P_VELOCITY);
        double delayS = expectedDelayTime(rayParam, depth0, depth1, VelocityModelMaterial.S_VELOCITY);

        double errorP = (delayP / delayTime) - 1.0;
        double errorS = (delayS / delayTime) - 1.0;

        if (Math.abs(errorP) < Math.abs(errorS)) {
            return "p";
        }
        return "s";
    }

    public double[] integralCoefficients(Arrival arrival) throws NoSuchLayerException {
        TauModel model = arrival.getTauModel();

        double[] total = new double[3];
        for (ArrivalPathSegment seg : arrival.getPathSegments()) {
            List<TimeDist> path = seg.getPath();
            double[] depth = new double[path.size()];
            for (int i = 0; i < path.size(); i++) {
                depth[i] = path.get(i).getDepth();
            }

            double maxDepth = max(depth);
            double[] radius = new double[depth.length];
            for (int i = 0; i < depth.length; i++) {
                radius[i] = model.radiusOfEarth - depth[i];
            }

            VelocityModel vMod = model.getVelocityModel();
            double[] v = new double[depth.length];
            VelocityModelMaterial wave = seg.isPWave() ? VelocityModelMaterial.P_VELOCITY : VelocityModelMaterial.S_VELOCITY;

            for (int i = 0; i < depth.length; i++) {
                if (depth[i] != maxDepth) {
                    v[i] = vMod.evaluateBelow(depth[i], wave);
                } else {
                    v[i] = vMod.evaluateAbove(maxDepth, wave);
                }
            }

            double[] eta = new double[depth.length];
            for (int i = 0; i < depth.length; i++) {
                eta[i] = radius[i] / v[i];
            }

            double[] epsilon = getEpsilon(model, depth);
            double[] distArr = new double[path.size()];
            for (int i = 0; i < path.size(); i++) {
                distArr[i] = path.get(i).getDistRadian();
            }

            double[][] lam = new double[3][depth.length];
            for (int m = 0; m < 3; m++) {
                for (int i = 0; i < depth.length; i++) {
                    lam[m][i] = -(2.0 / 3.0) * weightedAlp2(m, distArr[i]);
                }
            }

            double[] y = new double[eta.length];
            for (int i = 0; i < eta.length; i++) {
                y[i] = Math.pow(eta[i], 2) - Math.pow(arrival.getRayParam(), 2);
            }

            double[] verticalSlowness = new double[depth.length];
            for (int i = 0; i < depth.length; i++) {
                verticalSlowness[i] = Math.sqrt(Math.max(y[i], 0.0));
            }

            int minIdx = indexOfMin(radius);
            if (arrival.getRayParam() > 0.0 && minIdx != 0 && minIdx != (radius.length - 1)) {
                eta[minIdx] = arrival.getRayParam();
                v[minIdx] = radius[minIdx] / eta[minIdx];
                verticalSlowness[minIdx] = 0.0;
            }

            double[] rTop = new double[radius.length - 1];
            double[] rBot = new double[radius.length - 1];
            double[] vTop = new double[v.length - 1];
            double[] vBot = new double[v.length - 1];

            for (int i = 1; i < radius.length; i++) {
                rTop[i - 1] = radius[i];
                rBot[i - 1] = radius[i - 1];
                vTop[i - 1] = v[i];
                vBot[i - 1] = v[i - 1];
            }

            double[] dlogr = new double[rTop.length];
            double[] dlogv = new double[rTop.length];
            double[] dlogrDlogeta = new double[rTop.length];

            for (int i = 0; i < rTop.length; i++) {
                dlogr[i] = Math.log(rTop[i]) - Math.log(rBot[i]);
                dlogv[i] = Math.log(vTop[i]) - Math.log(vBot[i]);

                if (Math.abs(rTop[i] - rBot[i]) < 1e-12) {
                    dlogrDlogeta[i] = 1.0;
                } else {
                    dlogrDlogeta[i] = 1.0 / (1.0 - dlogv[i] / dlogr[i]);
                }
            }

            for (int m = 0; m < 3; m++) {
                double sum = 0.0;
                for (int i = 0; i < rTop.length; i++) {
                    double top = epsilon[i + 1] * lam[m][i + 1];
                    double bot = epsilon[i] * lam[m][i];
                    double delta = Math.abs(verticalSlowness[i + 1] - verticalSlowness[i]);

                    sum += 0.5 * (top + bot) * (dlogrDlogeta[i] - 1.0) * delta;
                }
                total[m] += sum;
            }

        }
        return total;
    }

    private static int indexOfMin(double[] arr) {
        int minIndex = 0;
        double minValue = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < minValue) {
                minValue = arr[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

    private static double max(double[] values) {
        double max = values[0];
        for (int i = 1; i < values.length; i++) {
            max = Math.max(max, values[i]);
        }
        return max;
    }

    public double[] discontinuityContribution(List<TimeDist> points, VelocityModelMaterial wavetype) throws NoSuchLayerException {
        if (points.size() < 2) {
            return new double[] { 0.0, 0.0, 0.0 };
        }

        TimeDist discPoint = points.get(0);
        TimeDist neighbourPoint = points.get(1);

        double rayParam = discPoint.getP();
        double distance = discPoint.getDistRadian();
        double depth = discPoint.getDepth();
        double radius = model.radiusOfEarth - depth;
        double neighbourDepth = neighbourPoint.getDepth();

        double v;
        if (neighbourDepth >= depth) {
            v = model.getVelocityModel().evaluateBelow(depth, wavetype);
        } else {
            v = model.getVelocityModel().evaluateAbove(depth, wavetype);
        }

        double eta = radius / v;
        double y = Math.pow(eta, 2) - Math.pow(rayParam, 2);
        double verticalSlowness = Math.sqrt(Math.max(y, 0.0));

        if (neighbourDepth == depth) {
            verticalSlowness = 0.0;
        }

        double sign = Math.signum(depth - neighbourDepth);
        double epsilon = getEpsilon(model, depth);

        double[] lam = new double[3];
        for (int m = 0; m < 3; m++) {
            lam[m] = -(2.0 / 3.0) * weightedAlp2(m, distance);
        }

        double[] sigma = new double[3];
        for (int m = 0; m < 3; m++) {
            sigma[m] = -sign * verticalSlowness * epsilon * lam[m];
        }

        return sigma;
    }

    public double[] discontinuityCoefficients(Arrival arrival) throws NoSuchLayerException {
        double[] total = new double[3];

        for (ArrivalPathSegment seg : arrival.getPathSegments()) {
            List<TimeDist> path = seg.getPath();
            if (path.size() < 2) {
                continue;
            }

            List<TimeDist> startPoints = new ArrayList<>();
            startPoints.add(path.get(0));
            startPoints.add(path.get(1));

            List<TimeDist> endPoints = new ArrayList<>();
            endPoints.add(path.get(path.size() - 1));
            endPoints.add(path.get(path.size() - 2));

            VelocityModelMaterial wave = seg.isPWave()?VelocityModelMaterial.P_VELOCITY:VelocityModelMaterial.S_VELOCITY;
            double[] start = discontinuityContribution(startPoints, wave);
            double[] end = discontinuityContribution(endPoints, wave);

            for (int m = 0; m < 3; m++) {
                total[m] += start[m] + end[m];
            }
        }

        return total;
    }

    public static double correctionFromCoefficients(double[] coefficients, double azimuthDegrees, double sourceLatitude) {
        if (!(sourceLatitude >= -90.0 && sourceLatitude <= 90.0)) {
            throw new IllegalArgumentException("Source latitude must be in range -90 to 90 degrees");
        }
        if (!(azimuthDegrees >= 0.0 && azimuthDegrees <= 360.0)) {
            throw new IllegalArgumentException("Azimuth must be in range 0 to 360 degrees");
        }

        double colatitude = Math.toRadians(90.0 - sourceLatitude);
        double azimuth = Math.toRadians(azimuthDegrees);

        double sum = 0.0;
        for (int m = 0; m < 3; m++) {
            sum += coefficients[m] * weightedAlp2(m, colatitude) * Math.cos(m * azimuth);
        }
        return sum;
    }

    public static Map<String, Table> tableEllipticityCoefficients(
            List<String> phaseList,
            TauModel model,
            double sourceDepthInKm,
            double receiverDepthInKm,
            double lod
    ) throws TauModelException {
        TauModel depthCorrectedModel = model.depthCorrect(sourceDepthInKm);
        Map<String, Table> tables = new HashMap<>();

        for (String phaseName : phaseList) {
            SeismicPhase phase = SeismicPhaseFactory.createPhase(phaseName, depthCorrectedModel, sourceDepthInKm, receiverDepthInKm);

            double[][] ellipCoeffs = new double[phase.getRayParams().length][3];
            double[] degrees = new double[ellipCoeffs.length];
            double[] time = new double[ellipCoeffs.length];
            for (int idx = 0; idx < phase.getRayParams().length; idx++) {
                Arrival arrival = phase.createArrivalAtIndex(idx);
                arrival.getPath();
                degrees[idx] =arrival.getDistDeg();
                time[idx] = arrival.getTime();
                Ellipticipy ellipticipy2 = new Ellipticipy(model, lod);

                List<Double> ellip = ellipticipy2.individualEllipticityCoefficients(arrival, lod);
                for (int j = 0; j < 3; j++) {
                    ellipCoeffs[idx][j] = ellip.get(j);
                }
            }

            tables.put(phaseName, new Table(phase.getRayParams(), degrees, degrees, time, ellipCoeffs));
        }

        return tables;
    }

    private static double[] cumtrapz(double[] y, double[] x) {
        double[] out = new double[y.length];
        out[0] = 0.0;
        for (int i = 1; i < y.length; i++) {
            out[i] = out[i - 1] + 0.5 * (y[i] + y[i - 1]) * (x[i] - x[i - 1]);
        }
        return out;
    }

    public static class Table {
        public final double[] rayParam;
        public final double[] degrees;
        public final double[] dist;
        public final double[] time;
        public final double[][] ellipCoeffs;

        public Table(double[] rayParam, double[] degrees, double[] dist, double[] time, double[][] ellipCoeffs) {
            this.rayParam = rayParam;
            this.degrees = degrees;
            this.dist = dist;
            this.time = time;
            this.ellipCoeffs = ellipCoeffs;
        }
    }


    public static class EllipCoef {
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
}
