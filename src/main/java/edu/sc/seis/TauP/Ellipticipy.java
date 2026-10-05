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
    private final VelocityModel vMod;
    private final HashMap<VelocityLayer, Double> top_epsilon = new HashMap<>();
    private final HashMap<VelocityLayer, Double> bot_epsilon = new HashMap<>();
    private final HashMap<VelocityLayer, Double> radauParameter = new HashMap<>();

    private double totalMass;
    double[] cumMass;
    double[] cumMomInteria;
    double ha;
    double epsilona;
    double[] epsilon_pre_a;
    double[] radau;
    double[] y;

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
        cumMass = new double[vMod.getNumLayers()];
        cumMomInteria = new double[vMod.getNumLayers()];
        this.y = new double[vMod.getNumLayers()];
        radau = new double[vMod.getNumLayers()];
        double cumulativeMass = 0.0;
        double cumulativeInertia = 0.0;
        for (int i = 0; i < vMod.getNumLayers(); i++) {
            VelocityLayer vLayer = vMod.getVelocityLayer(vMod.getNumLayers()-1-i);
            topRadiusMeter[i] = vLayer.getTopRadius(vMod.getRadiusOfEarth()) * 1e3;
            cumulativeMass += vLayer.calcMass(vMod.getRadiusOfEarth());
            cumMass[i] = cumulativeMass;
            double shellInertia = vLayer.calcMomentOfInertia(vMod.getRadiusOfEarth());
            cumulativeInertia += shellInertia;
            cumMomInteria[i] = cumulativeInertia;
            double y = cumulativeInertia / (cumulativeMass * Math.pow(topRadiusMeter[i], 2));
            this.y[i] = y;
            radau[i] = 6.25 * Math.pow(1.0 - 3.0 * y / 2.0, 2) - 1.0;
            radauParameter.put(vLayer, radau[i]);
        }
        totalMass = cumulativeMass;

        double ha = (Math.pow(a, 3) * Math.pow(omega, 2)) / (G * totalMass);
        double epsilona = (5.0 * ha) / (2.0 * radau[radau.length - 1] + 4.0);

        this.ha = ha;
        this.epsilona = epsilona;

        double[] integrand = new double[vMod.getNumLayers()];
        for (int i = 0; i < vMod.getNumLayers(); i++) {
            integrand[i] = radau[i] / topRadiusMeter[i];
        }

        double[] epsilon = cumtrapz(integrand, topRadiusMeter);
        epsilon_pre_a = new double[epsilon.length];
        for (int i = 0; i < epsilon.length; i++) {
            epsilon[i] = Math.exp(epsilon[i]);
            epsilon_pre_a[i] = epsilon[i];
        }
        for (int i = 0; i < epsilon.length; i++) {
            epsilon[i] = epsilona * epsilon[i] / epsilon[epsilon.length - 1];
        }

        double[] epsilonWithCenter = new double[epsilon.length + 1];
        epsilonWithCenter[0] = epsilon[0];
        System.arraycopy(epsilon, 0, epsilonWithCenter, 1, epsilon.length);

        for (int i = 0; i < vMod.getNumLayers(); i++) {
            VelocityLayer vLayer = vMod.getVelocityLayer(vMod.getNumLayers()-1-i);
            this.bot_epsilon.put(vLayer, epsilonWithCenter[i]);
            this.top_epsilon.put(vLayer, epsilonWithCenter[i+1]);
        }

    }

    public double getEpsilon(double depth) throws NoSuchLayerException {
        int layerIdx = 0;
        if (depth > 0.0) {
            layerIdx = vMod.layerNumberAbove(depth);
        }
        VelocityLayer vLayer = vMod.getVelocityLayer(layerIdx);
        double botEps = bot_epsilon.get(vLayer);
        double topEps = top_epsilon.get(vLayer);
        double slope = (botEps - topEps) / vLayer.getThickness();
        return slope * (depth - vLayer.getTopDepth()) + topEps;
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

    public double[] ellipticityCoefficients(Arrival arrival) throws NoSuchLayerException {
        double[] raySigma = integralCoefficients(arrival);
        double[] discSigma = discontinuityCoefficients(arrival);

        double[] sigma = new double[3];
        for (int m = 0; m < 3; m++) {
            sigma[m] = (raySigma[m] + discSigma[m]);
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

    public double[] integralCoefficients(Arrival arrival) throws NoSuchLayerException {
        TauModel model = arrival.getTauModel();

        double[] total = new double[3];
        for (ArrivalPathSegment seg : arrival.getPathSegments()) {
            LayerPropogationType layerPropogationType = seg.getPhaseSegment().getLayerPropogationType();
            if (seg.getPhaseSegment().getIsFlat()) {
                continue;
            }
            boolean downgoing = layerPropogationType == LayerPropogationType.DOWN;
            List<TimeDist> path = seg.getPath();


            VelocityModel vMod = model.getVelocityModel();
            VelocityModelMaterial wave = seg.isPWave() ? VelocityModelMaterial.P_VELOCITY : VelocityModelMaterial.S_VELOCITY;
            double[][] lam = new double[3][path.size()];
            double[] verticalSlowness = new double[path.size()];
            double[] epsilon = new double[path.size()];

            TimeDist prevTd = null;
            double vel;
            double prevVel = 0;
            double eta;
            for (int i = 0; i < path.size(); i++) {
                TimeDist td = path.get(i);
                if ((i==0 && downgoing) || (!downgoing && i==path.size()-1)) {
                    vel = vMod.evaluateBelow(td.getDepth(), wave);
                } else {
                    vel = vMod.evaluateAbove(td.getDepth(), wave);
                }
                eta = (model.radiusOfEarth - td.getDepth()) / vel;
                epsilon[i] = getEpsilon(td.getDepth());

                for (int m = 0; m < 3; m++) {
                    lam[m][i] = -(2.0 / 3.0) * weightedAlp2(m, td.getDistRadian());
                }
                double y = Math.pow(eta, 2) - Math.pow(arrival.getRayParam(), 2);
                verticalSlowness[i] = Math.sqrt(Math.max(y, 0.0));
                // Make velocities for bottoming rays consistent
                if (arrival.getRayParam() != 0.0
                        && ((seg.getPhaseSegment().getPrevEndAction()==PhaseInteraction.TURN && i == 0)
                        || (seg.getPhaseSegment().getEndAction()==PhaseInteraction.TURN && i == path.size() - 1)) ) {
                    eta = arrival.getRayParam();
                    vel = (model.radiusOfEarth - td.getDepth()) / eta;
                    verticalSlowness[i] = 0.0;

                }

                if (prevTd!= null) {
                    // i > 0

                    double dlogr = Math.log(model.radiusOfEarth - td.getDepth()) - Math.log(model.radiusOfEarth - prevTd.getDepth());
                    double dlogv = Math.log(vel) - Math.log(prevVel);

                    double dlogrDlogeta;
                    if (Math.abs(td.getDepth()-prevTd.getDepth()) < 1e-12) {
                        dlogrDlogeta = 1.0;
                    } else {
                        dlogrDlogeta = 1.0 / (1.0 - dlogv / dlogr);
                    }

                    for (int m = 0; m < 3; m++) {
                        double sum = 0.0;
                        double top = epsilon[i] * lam[m][i ];
                        double bot = epsilon[i-1] * lam[m][i-1];
                        double delta = Math.abs(verticalSlowness[i ] - verticalSlowness[i-1]);

                        sum += 0.5 * (top + bot) * (dlogrDlogeta - 1.0) * delta;
                        total[m] += sum;
                    }
                }
                prevVel = vel;
                prevTd = td;
            }


        }
        return total;
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
        double epsilon = getEpsilon(depth);

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
            throw new IllegalArgumentException("Source latitude must be in range -90 to 90 degrees: "+sourceLatitude);
        }
        while (azimuthDegrees < 0 ) {
            azimuthDegrees += 360.0;
        }
        if (azimuthDegrees > 360) {
            azimuthDegrees = azimuthDegrees % 360.0;
        }
        if (!(azimuthDegrees >= 0.0 && azimuthDegrees <= 360.0)) {
            throw new IllegalArgumentException("Azimuth must be in range 0 to 360 degrees: "+azimuthDegrees);
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

                double[] ellip = ellipticipy2.ellipticityCoefficients(arrival);
                for (int j = 0; j < 3; j++) {
                    ellipCoeffs[idx][j] = ellip[j];
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

    public double getLod() {
        return lod;
    }

    public TauModel getTauModel() {
        return model;
    }

    public VelocityModel getVelocityMod() {
        return vMod;
    }

    public Double topEpsilon(VelocityLayer layer) {
        return top_epsilon.get(layer);
    }

    public Double botEpsilon(VelocityLayer layer) {
        return bot_epsilon.get(layer);
    }

    public double getTotalMass() {
        return totalMass;
    }

    public double getRadauParameter(VelocityLayer layer) {
        return radauParameter.get(layer);
    }
}
