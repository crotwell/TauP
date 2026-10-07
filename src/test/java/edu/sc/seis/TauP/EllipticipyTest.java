package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.sc.seis.seisFile.LatLonSimple;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EllipticipyTest {

    @Test
    public void testPyExample() throws TauModelException {
        /*
        ellipticipy ver 1.0.1, Sept 2026
        ellip -d 134 -deg 64 -az 15 -sl 23 -ph P,PKiKP,PcP,SP,PS -mod ak135

        Model: ak135
        Distance   Depth   Phase        Ray Param   Spherical   Ellipticity   Elliptical
          (deg)     (km)   Name         p (s/deg)   Travel      Correction    Travel
                                                    Time (s)        (s)       Time (s)
        --------------------------------------------------------------------------------
           64.00   134.0   P                6.536     619.05        -0.45       618.60
           64.00   134.0   PcP              4.110     653.31        -0.48       652.83
           64.00   134.0   PKiKP            1.307    1020.55        -0.75      1019.80
           64.00   134.0   SP              13.673    1140.79        -1.02      1139.77
           64.00   134.0   PS              13.401    1163.00        -0.86      1162.14
         */
        String modelName = "ak135";
        double depth = 134.0;
        double degree = 64.0;
        double az = 15.0;
        double stalat = 23;

        TauModel tMod = TauModelLoader.load(modelName).depthCorrect(depth);
        Ellipticipy ellipticipy = new Ellipticipy(tMod);
        checkPhase("P", -0.45, degree, az, stalat, ellipticipy );
        checkPhase("PcP", -0.48, degree, az, stalat, ellipticipy );
        checkPhase("PKiKP", -0.75, degree, az, stalat, ellipticipy );
        checkPhase("SP", -1.02, degree, az, stalat, ellipticipy );
        checkPhase("PS", -0.86, degree, az, stalat, ellipticipy );
    }

    public void checkPhase(String phaseName,
                           double ellipticipyCorrection,
                           double degree, double az, double staLat,
                           Ellipticipy ellipticipy) throws TauModelException {
        checkPhase(phaseName, List.of(ellipticipyCorrection),
                degree, az, staLat, ellipticipy);
    }

    public void checkPhase(String phaseName,
                           List<Double> ellipticipyCorrection,
                           double degree, double az, double staLat,
                           Ellipticipy ellipticipy) throws TauModelException {
        SeismicPhase phase = SeismicPhaseFactory.createPhase(phaseName, ellipticipy.getTauModel());
        DistanceRay dr = DistanceAngleRay.ofDegrees(degree);
        List<Arrival> arrivalList = dr.calculate(phase);
        assertEquals(ellipticipyCorrection.size(), arrivalList.size());
        for (int i = 0; i < ellipticipyCorrection.size(); i++) {
            Arrival arr = arrivalList.get(i);
            double[] sigma = ellipticipy.ellipticityCoefficients(arr);
            double arrAz = az;
            if (arr.isLongWayAround()) {
                arrAz = (az + 180) % 360;
            }
            double correction = Ellipticipy.correctionFromCoefficients(sigma, arrAz, staLat);
            assertEquals(ellipticipyCorrection.get(i), correction, 0.01, phaseName+" "+i);
        }
    }

    @Test
    public void check_ttall() throws TauModelException {
        /*
        ellip -d 134 -deg 64 -az 15 -sl 23 -ph ttall -mod ak135

        Model: ak135
        Distance   Depth   Phase        Ray Param   Spherical   Ellipticity   Elliptical
          (deg)     (km)   Name         p (s/deg)   Travel      Correction    Travel
                                                    Time (s)        (s)       Time (s)
        --------------------------------------------------------------------------------
           64.00   134.0   P                6.536     619.05        -0.45       618.60
           64.00   134.0   pP               6.623     651.33        -0.42       650.91
           64.00   134.0   PcP              4.110     653.31        -0.48       652.83
           64.00   134.0   sP               6.603     665.88        -0.41       665.47
           64.00   134.0   PP               8.768     761.36        -0.72       760.64
           64.00   134.0   PKiKP            1.307    1020.55        -0.75      1019.80
           64.00   134.0   pPKiKP           1.303    1056.52        -0.72      1055.80
           64.00   134.0   sPKiKP           1.304    1070.21        -0.71      1069.50
           64.00   134.0   S               12.338    1124.58        -0.82      1123.76
           64.00   134.0   SP              13.673    1140.79        -1.02      1139.77
           64.00   134.0   pS              12.715    1161.37        -0.80      1160.57
           64.00   134.0   PS              13.401    1163.00        -0.86      1162.14
           64.00   134.0   sS              12.491    1180.23        -0.77      1179.46
           64.00   134.0   SKS              7.590    1199.97        -0.89      1199.08
           64.00   134.0   SKKS             7.591    1199.97        -0.89      1199.08
           64.00   134.0   ScS              7.656    1200.02        -0.89      1199.13
           64.00   134.0   SKiKP            1.362    1221.08        -0.57      1220.50
           64.00   134.0   pSKS             7.591    1245.74        -0.86      1244.88
           64.00   134.0   sSKS             7.591    1260.62        -0.84      1259.78
           64.00   134.0   SS              15.582    1375.57        -1.30      1374.28
          296.00   134.0   PKIKKIKP         1.355    1850.70         0.80      1851.50
          296.00   134.0   SKIKKIKP         1.296    2051.20         0.97      2052.17
          296.00   134.0   PKIKKIKS         1.296    2064.89         0.80      2065.69
          296.00   134.0   SKIKKIKS         1.241    2265.20         0.97      2266.18
          296.00   134.0   PKIKPPKIKP       1.645    2350.47         1.53      2352.00
          296.00   134.0   PKPPKP           2.645    2356.82         1.45      2358.27
          296.00   134.0   PKPPKP           4.005    2362.51         1.16      2363.68
          296.00   134.0   SKIKSSKIKS       1.373    3196.14         2.28      3198.42
         */
        String modelName = "ak135";
        double depth = 134.0;
        double degree = 64.0;
        double az = 15.0;
        double stalat = 23;

        TauModel tMod = TauModelLoader.load(modelName).depthCorrect(depth);
        Ellipticipy ellipticipy = new Ellipticipy(tMod);
        checkPhase("P",  -0.4504, degree, az, stalat, ellipticipy );
        checkPhase("pP",  -0.4236, degree, az, stalat, ellipticipy );
        checkPhase("PcP",  -0.4847, degree, az, stalat, ellipticipy );
        checkPhase("sP",  -0.4089, degree, az, stalat, ellipticipy );
        checkPhase("PP",  -0.7172, degree, az, stalat, ellipticipy );
        checkPhase("PKiKP",  -0.7507, degree, az, stalat, ellipticipy );
        checkPhase("pPKiKP",  -0.7211, degree, az, stalat, ellipticipy );
        checkPhase("sPKiKP",  -0.7072, degree, az, stalat, ellipticipy );
        checkPhase("S",    -0.82, degree, az, stalat, ellipticipy );
        checkPhase("SP",    -1.02, degree, az, stalat, ellipticipy );
        checkPhase("pS",  -0.8044, degree, az, stalat, ellipticipy );
        checkPhase("PS",  -0.8634, degree, az, stalat, ellipticipy );
        checkPhase("sS",   -0.774, degree, az, stalat, ellipticipy );
        checkPhase("SKS",  -0.8865, degree, az, stalat, ellipticipy );
        checkPhase("SKKS",  -0.8865, degree, az, stalat, ellipticipy );
        checkPhase("ScS",  -0.8864, degree, az, stalat, ellipticipy );
        checkPhase("SKiKP",   -0.573, degree, az, stalat, ellipticipy );
        checkPhase("pSKS",    -0.86, degree, az, stalat, ellipticipy );
        checkPhase("sSKS",  -0.8402, degree, az, stalat, ellipticipy );
        checkPhase("SS",   -1.297, degree, az, stalat, ellipticipy );
        checkPhase("PKIKKIKP",   0.7998, degree, az, stalat, ellipticipy );
        checkPhase("SKIKKIKP",   0.9734, degree, az, stalat, ellipticipy );
        checkPhase("PKIKKIKS",   0.8019, degree, az, stalat, ellipticipy );
        checkPhase("SKIKKIKS",   0.9743, degree, az, stalat, ellipticipy );
        checkPhase("PKIKPPKIKP",    1.528, degree, az, stalat, ellipticipy );
        // PKPPKP has two arrivals
        checkPhase("PKPPKP",    List.of(1.451, 1.163), degree, az, stalat, ellipticipy );
        checkPhase("SKIKSSKIKS",     2.28, degree, az, stalat, ellipticipy );
    }

    @Test
    public void diffPhase()  throws TauModelException {
        /*
        ellip  -d 134 -deg 105 -az 15 -sl 23 -ph Pdiff,Sdiff -mod ak135

        Model: ak135
        Distance   Depth   Phase        Ray Param   Spherical   Ellipticity   Elliptical
          (deg)     (km)   Name         p (s/deg)   Travel      Correction    Travel
                                                    Time (s)        (s)       Time (s)
        --------------------------------------------------------------------------------
          105.00   134.0   Pdiff            4.446     832.04         0.04       832.08
          105.00   134.0   Sdiff            8.341    1534.51         0.06      1534.58

         */
        String modelName = "ak135";
        double depth = 134.0;
        double degree = 105.0;
        double az = 15.0;
        double stalat = 23;

        TauModel tMod = TauModelLoader.load(modelName).depthCorrect(depth);
        Ellipticipy ellipticipy = new Ellipticipy(tMod);
        checkPhase("Pdiff",   0.04, degree, az, stalat, ellipticipy );
        checkPhase("Sdiff",   0.06, degree, az, stalat, ellipticipy );

        degree = 115;
        checkPhase("Pdiff",   0.29, degree, az, stalat, ellipticipy );
        checkPhase("Sdiff",   0.52, degree, az, stalat, ellipticipy );
    }

    @Test
    public void calcMass() throws TauModelException {
        String modelName = "ak135";
        TauModel tMod = TauModelLoader.load(modelName);
        VelocityModel vMod = tMod.getVelocityModel();
        double cumulativeMass = 0;
        for (VelocityLayer vLayer : vMod.getLayers()) {
            cumulativeMass += vLayer.calcMass(vMod.getRadiusOfEarth());
        }
        assertEquals(5.971739162213734e+24, cumulativeMass, 1e10);
    }

    @Test
    public void testEpsilon() throws TauModelException {
        String modelName = "ak135";
        TauModel tMod = TauModelLoader.load(modelName);
        VelocityModel vMod = tMod.getVelocityModel();
        VelocityLayer centerLayer = vMod.getVelocityLayer(vMod.getNumLayers()-1);

        Ellipticipy ellipticipy = new Ellipticipy(tMod);
        double zero_vol = 546222318024212.25;
        double calc_zero_vol = centerLayer.calcVolumeKm(vMod.getRadiusOfEarth());
        calc_zero_vol *= 1e9;
        assertEquals(zero_vol, calc_zero_vol, 1e2);
        double zero_cumMass = 7.107417491015149e+18;
        double calc_zero_mass = centerLayer.calcMass(vMod.getRadiusOfEarth());
        assertEquals(zero_cumMass, calc_zero_mass, 1e10);
        assertEquals(zero_cumMass, ellipticipy.cumMass[0], 1e10);

        double earthMass = 5.971739162213734e+24;
        assertEquals(earthMass, ellipticipy.getTotalMass(), 1e10);
        double zeroY = 0.4;
        double oneY = 0.3999968303319719;
        double topY = 0.3309551570346571;
        assertEquals(zeroY, ellipticipy.y[0], 1e-9);
        assertEquals(oneY, ellipticipy.y[1], 1e-9);
        assertEquals(topY, ellipticipy.y[126], 1e-9);

        double radau_zero = -4.440892098500626e-16;
        double radau_one = 2.3772651493558783e-05;
        double radau_top = 0.5848749363978527;
        assertEquals(radau_zero, ellipticipy.radau[0]);
        assertEquals(radau_one, ellipticipy.radau[1], 1e-14, ""+(radau_one - ellipticipy.radau[1]));
        assertEquals(radau_top, ellipticipy.radau[126], 1e-14);

        double ha= 0.003450147778619671;
        double epsilona= 0.0033368614183590033;
        assertEquals(ha, ellipticipy.ha, 1e-9, "ha");
        assertEquals(epsilona, ellipticipy.epsilona, 1e-9, "epsilona");

        double zero_ep_pre_a = 1.0;
        double one_ep_pre_a =1.0000059437664746;
        assertEquals(zero_ep_pre_a, ellipticipy.epsilon_pre_a[0], 1e-9);
        assertEquals(one_ep_pre_a, ellipticipy.epsilon_pre_a[1], 1e-9);

        double top_epsilon_zero = 0.002419280267333141;

        double top_epsilon_top = 0.0033368614183590037;
        assertEquals(top_epsilon_zero, ellipticipy.topEpsilon(centerLayer), 1.0e-9);
        assertEquals(top_epsilon_top, ellipticipy.topEpsilon(vMod.getVelocityLayer(0)), 1.0e-9);
    }

    @Test
    public void momentOfInertia() throws TauModelException {
        String modelName = "ak135";

        double zerothMofI = 7.31070129e+27;
        double topMofI = 7.46141565e+35;

        TauModel tMod = TauModelLoader.load(modelName);
        VelocityModel vMod = tMod.getVelocityModel();
        assertEquals(zerothMofI, vMod.getVelocityLayer(vMod.getNumLayers()-1).calcMomentOfInertia(vMod.getRadiusOfEarth()), 1e20);
        assertEquals(topMofI, vMod.getVelocityLayer(0).calcMomentOfInertia(vMod.getRadiusOfEarth()), 1e29);


    }

    /**
     * Modified from https://github.com/StuartJRussell/EllipticiPy/blob/master/src/test/test_epsilon.py
     */
    @Test
    public void test_epsilon() throws TauModelException {
        //"""Test the numerical integration for ellipticity (epsilon)"""
        TauModel model = TauModelLoader.load("prem");

        Ellipticipy ellipticipy = new Ellipticipy(model);

        //Calculate ellipticity at the core mantle boundary(CMB)
        double cmb_depth = 2891.0;  //km
        double calculated_epsilon = ellipticipy.getEpsilon(cmb_depth);

        //Expected ellipticity for PREM at the CMB, see e.g.Huang et al GJI (2001) 146 p130
        double expected_epsilon = 0.002547;

        System.err.println("epsilon_CMB"+ expected_epsilon+" "+ calculated_epsilon);
        assertEquals(expected_epsilon, calculated_epsilon, 2e-6);
    }

    /**
     * Modified from https://github.com/StuartJRussell/EllipticiPy/blob/master/src/test/test_ellipticity_coefficients.py
     * @throws TauModelException
     */
    @Test
    public void test_ellipticity_coefficients() throws TauModelException {
        TauModel model = TauModelLoader.load("ak135");

        // Expected values from ak135 reference tables (or for phases where these are wrong from our prior calculations)
        // list is: source_depth_in_km, distance_in_degree, expected_sigma
        HashMap<String, List<Double>> test_data = new HashMap<>();
        test_data.put("Pdiff", List.of(100.0, 140.0, -1.42, 0.94, -0.29));
        test_data.put("Sdiff", List.of(200.0, 110.0, -1.16, 1.34, -1.32));
        test_data.put("PcP", List.of(700.0, 10.0, -1.19, -0.24, -0.03));
        test_data.put("ScP", List.of(0.0, 45.0, -1.49, -0.53, -0.45));
        test_data.put("PP", List.of(300.0, 70.0, -0.69, -0.96, -0.72));
        test_data.put("SS", List.of(0.0, 130.0, -1.11, -0.40, -2.59));
        test_data.put("SKiKP", List.of(500.0, 0.0, -2.60, 0.00, 0.00));
        test_data.put("PKIKP", List.of(0.0, 180.0, -2.70, 0.00, 0.00));
        test_data.put("p", List.of(200.0, 0.0, -0.09, 0.00, 0.00));
        test_data.put("s", List.of(500.0, 5.0, -0.41, -0.33, -0.02));

        //Allow a tolerence of 2e-2
        double tol = 3e-2;
        for (String phaseName : test_data.keySet()) {
            List<Double> values = test_data.get(phaseName);
            double source_depth_in_km = values.get(0);
            double distance_in_degree = values.get(1);
            TauModel tModDepth = model.depthCorrect(source_depth_in_km);
            Ellipticipy ellipticipy = new Ellipticipy(model);

            SeismicPhase phase = SeismicPhaseFactory.createPhase(phaseName, tModDepth);
            DistanceRay dr = DistanceAngleRay.ofDegrees(distance_in_degree);
            List<Arrival> arrivalList = dr.calculate(phase);
            Arrival arr = arrivalList.get(0);
            List<Double> sigma_expected = values.subList(2,5);
            double[] calculated_sigma = ellipticipy.ellipticityCoefficients(arr);
            assertEquals(sigma_expected.get(0), calculated_sigma[0], tol, phaseName);
            assertEquals(sigma_expected.get(1), calculated_sigma[1], tol, phaseName);
            assertEquals(sigma_expected.get(2), calculated_sigma[2], tol, phaseName);
        }
    }

    /**
     * Modified from https://github.com/StuartJRussell/EllipticiPy/blob/master/src/test/test_ellipticity_coefficients.py
     * @throws TauModelException
     */
    @Test
    public void test_table_ellipticity_coefficients() throws TauModelException {
        TauModel model = TauModelLoader.load("ak135");
        Ellipticipy ellipticipy = new Ellipticipy(model);
        double source_depth_in_km = 200.0;
        double receiverDepthInKm = 0.0;
        String phaseName = "SKKS";
        Map<String, Ellipticipy.Table> table = Ellipticipy.tableEllipticityCoefficients(List.of(phaseName),
                model,
                source_depth_in_km,
                receiverDepthInKm,
                Ellipticipy.EARTH_LOD);
        double[] test_coeff = table.get(phaseName).ellipCoeffs[table.get(phaseName).ellipCoeffs.length-1];
        double[] expected_coeff = new double[] {-1.26, 1.57, -2.08};
        double atol = 2e-2;
        for (int i = 0; i < expected_coeff.length; i++) {
            assertEquals(expected_coeff[i], test_coeff[i], atol);
        }
    }

    /**
     * Modified from https://github.com/StuartJRussell/EllipticiPy/blob/master/src/test/test_ellipticity_coefficients.py
     * @throws TauModelException
     */
    @Test
    public void test_correction() throws TauPException {

        // Expected corrections are from prior calculation
        HashMap<String, HashMap<String, List<Double>>> test_data = new HashMap<>();
        HashMap<String, List<Double>> test_data_ak135 = new HashMap<>();
        test_data.put("ak135", test_data_ak135);
        test_data_ak135.put("P", List.of(124.0, 65.0, 45.0, 39.0, -0.37));
        test_data_ak135.put("S", List.of(124.0, 65.0, 45.0, 39.0, -0.70));
        test_data_ak135.put("ScS", List.of(240.0, 76.0, 30.0, 15.0, -0.83));

        HashMap<String, List<Double>> test_data_prem = new HashMap<>();
        test_data.put("prem", test_data_prem);
        test_data_prem.put("PKKP", List.of(320.0, 90.0, 10.0, 15.0, -0.55));
        test_data_prem.put("PcS", List.of(10.0, 40.0, -50.0, 260.0, -0.41));

        HashMap<String, List<Double>> test_data_iasp91 = new HashMap<>();
        test_data.put("iasp91", test_data_iasp91);
        test_data_iasp91.put("sPKiKP", List.of(540.0, 75.0, -80.0, 210.0, -1.16));
        test_data_iasp91.put("SKSSKS", List.of(400.0, 260.0, 0.0, 80.0, 3.48));
        double tol = 1e-2;
        double source_longitude = 0; // doesn't matter
        for (String modelName : test_data.keySet()) {
            for (String phaseName : test_data.get(modelName).keySet()) {

                List<Double> values = test_data.get(modelName).get(phaseName);
                double source_depth_in_km = values.get(0);
                double distance_in_degree = values.get(1);
                double source_latitude = values.get(2);
                double azimuth = values.get(3);
                double expected_correction = values.get(4);
                TauModel model = TauModelLoader.load(modelName);
                TauModel tModDepth = model.depthCorrect(source_depth_in_km);
                Ellipticipy ellipticipy = new Ellipticipy(tModDepth);

                SeismicPhase phase = SeismicPhaseFactory.createPhase(phaseName, ellipticipy.getTauModel());
                DistanceRay dr = DistanceAngleRay.ofDegrees(distance_in_degree);
                dr.evtLatLon = new LatLonSimple(source_latitude, source_longitude, source_depth_in_km);
                dr.azimuth = azimuth;
                List<Arrival> arrivalList = dr.calculate(phase);
                Arrival arr = arrivalList.get(0);
                arr.applyEllipticityCorrection(ellipticipy);
                assertEquals(expected_correction, arr.getEllipticityCorrection(), tol);
            }
        }
    }

    @Test
    public void testVsEllipticipyData() throws TauPException {
        /*
         * data for test generated by:  <br>
             python generateElliptipyTestData.py<br>
           in src/test/resources/edu/sc/seis/TauP.
         */
        String filename = "elliptipy_data.json";
        BufferedReader in = new BufferedReader(new InputStreamReader(this.getClass()
                .getClassLoader()
                .getResourceAsStream("edu/sc/seis/TauP/" + filename)));
        JSONTokener inJson = new JSONTokener(in);
        JSONObject ellipObj = new JSONObject(inJson);
        double source_depth_in_km = ellipObj.getDouble("source_depth_in_km");
        double source_longitude = 0;
        TauModel model = TauModelLoader.load(ellipObj.getString("model_name"));
        TauModel tModDepth = model.depthCorrect(source_depth_in_km);
        Ellipticipy ellipticipy = new Ellipticipy(tModDepth);
        HashMap<String, SeismicPhase> phases = new HashMap<>();
        double tol = 1e-2;
        for (int i = 0; i < ellipObj.getJSONArray("phaseList").length(); i++) {
            String phaseName = ellipObj.getJSONArray("phaseList").getString(i);
            phases.put(phaseName, SeismicPhaseFactory.createPhase(phaseName, ellipticipy.getTauModel()));
        }
        for (int i = 0; i < ellipObj.getJSONArray("data").length(); i++) {
            JSONObject dataObj = ellipObj.getJSONArray("data").getJSONObject(i);
            SeismicPhase sp = phases.get(dataObj.getString("phase"));
            DistanceRay dr = DistanceAngleRay.ofDegrees(dataObj.getDouble("deg"));
            dr.evtLatLon = new LatLonSimple(dataObj.getDouble("lat"), source_longitude, source_depth_in_km);
            dr.azimuth = dataObj.getDouble("az");

            List<Arrival> arrivalList = dr.calculate(sp);
            Arrival arr = arrivalList.get(0);
            arr.applyEllipticityCorrection(ellipticipy);
            assertEquals(dataObj.getDouble("ellip"), arr.getEllipticityCorrection(), tol, sp.getName()+" deg: "+dr.getDegrees()+" az: "+dr.azimuth+" lat: "+dr.evtLatLon.asLocation().getLatitude());
        }
    }
}
