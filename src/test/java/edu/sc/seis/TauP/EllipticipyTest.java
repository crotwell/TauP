package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

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
        SeismicPhase P = SeismicPhaseFactory.createPhase("P", tMod);
        SeismicPhase PKiKP = SeismicPhaseFactory.createPhase("PKiKP", tMod);
        SeismicPhase PcP = SeismicPhaseFactory.createPhase("PcP", tMod);

        Arrival P_arr = P.getEarliestArrival(degree);
        double[] sigma = ellipticipy.ellipticityCoefficients(P_arr);
        double correction = Ellipticipy.correctionFromCoefficients(sigma, az, stalat);
        assertEquals(-0.45, correction, 0.01);

        Arrival PcP_arr = PcP.getEarliestArrival(degree);
        double[] PcP_sigma = ellipticipy.ellipticityCoefficients(PcP_arr);
        double PcP_correction = Ellipticipy.correctionFromCoefficients(PcP_sigma, az, stalat);
        assertEquals(-0.48, PcP_correction, 0.01);

        Arrival PKiKP_arr = PKiKP.getEarliestArrival(degree);
        double[] PKiKP_sigma = ellipticipy.ellipticityCoefficients(PKiKP_arr);
        double PKiKP_correction = Ellipticipy.correctionFromCoefficients(PKiKP_sigma, az, stalat);
        assertEquals(-0.75, PKiKP_correction, 0.01);


        SeismicPhase SP = SeismicPhaseFactory.createPhase("SP", tMod);
        Arrival SP_arr = SP.getEarliestArrival(degree);
        double[] SP_sigma = ellipticipy.ellipticityCoefficients(SP_arr);
        double SP_correction = Ellipticipy.correctionFromCoefficients(SP_sigma, az, stalat);
        assertEquals(-1.02, SP_correction, 0.01);

        SeismicPhase PS = SeismicPhaseFactory.createPhase("PS", tMod);
        Arrival PS_arr = PS.getEarliestArrival(degree);
        double[] PS_sigma = ellipticipy.ellipticityCoefficients(PS_arr);
        double PS_correction = Ellipticipy.correctionFromCoefficients(PS_sigma, az, stalat);
        assertEquals(-0.86, PS_correction, 0.01);

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
        assertEquals(earthMass, ellipticipy.totalMass, 1e10);
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
        assertEquals(radau_top, ellipticipy.radau[126], 1e-15);

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
        assertEquals(top_epsilon_zero, ellipticipy.top_epsilon.get(centerLayer), 1.0e-9);
        assertEquals(top_epsilon_top, ellipticipy.top_epsilon.get(vMod.getVelocityLayer(0)), 1.0e-9);
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
}
