package edu.sc.seis.TauP;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import junit.framework.TestCase;

public class ParamDifferential extends TestCase {

    public ParamDifferential() {}
    
    public static void main(String[] args) throws Exception {
        ParamDifferential pd = new ParamDifferential();
        //pd.testcalc(pd.getConstVelModel());
        System.out.println("ak135");
        pd.testcalc(pd.getAK135VelModel());
    }

    public VelocityModel getConstVelModel() {
        return ConstantModelTest.createVelMod(vp, vs);
    }

    public VelocityModel getAK135VelModel() {
        try {
            return new TauP_Time("ak135").getTauModel().getVelocityModel();
        } catch(TauModelException e) {
            //shouldn't happen
            throw new RuntimeException(e);
        }
    }
    
    public void testcalc(VelocityModel vMod) throws Exception {
        double minDeltaP = 0.1;
        double maxDeltaP = 11.0;
        double maxDepthInterval = 115.0;
        double maxRangeInterval = 2.5 * Math.PI / 180;
        double maxInterpError = 0.01;
        double delta = .5;
        TauModel[] deltaTMod = new TauModel[6];
        deltaTMod[0] = create(vMod,
                               minDeltaP,
                               maxDeltaP,
                               maxDepthInterval,
                               maxRangeInterval,
                               maxInterpError,
                               true,
                               SlownessModel.DEFAULT_SLOWNESS_TOLERANCE);
        deltaTMod[1] = create(vMod,
                              minDeltaP * delta,
                              maxDeltaP,
                              maxDepthInterval,
                              maxRangeInterval,
                              maxInterpError,
                              true,
                              SlownessModel.DEFAULT_SLOWNESS_TOLERANCE);
        deltaTMod[2] = create(vMod,
                              minDeltaP,
                              maxDeltaP * delta,
                              maxDepthInterval,
                              maxRangeInterval,
                              maxInterpError,
                              true,
                              SlownessModel.DEFAULT_SLOWNESS_TOLERANCE);
        deltaTMod[3] = create(vMod,
                              minDeltaP,
                              maxDeltaP,
                              maxDepthInterval * delta,
                              maxRangeInterval,
                              maxInterpError,
                              true,
                              SlownessModel.DEFAULT_SLOWNESS_TOLERANCE);
        deltaTMod[4] = create(vMod,
                              minDeltaP,
                              maxDeltaP,
                              maxDepthInterval,
                              maxRangeInterval * delta,
                              maxInterpError,
                              true,
                              SlownessModel.DEFAULT_SLOWNESS_TOLERANCE);
        deltaTMod[5] = create(vMod,
                              minDeltaP,
                              maxDeltaP,
                              maxDepthInterval,
                              maxRangeInterval,
                              maxInterpError * delta,
                              true,
                              SlownessModel.DEFAULT_SLOWNESS_TOLERANCE);
        SeismicPhase[] pPhase = new SeismicPhase[deltaTMod.length];
        for (int i = 0; i < pPhase.length; i++) {
            pPhase[i] = new SeismicPhase("P", deltaTMod[i]);
            System.err.println("tmod size="+deltaTMod[i].getRayParams().length);
        }
        double[][] errors = new double[deltaTMod.length][180];
        double R = 6371;
        float deltaDeg = .02f;
        for (float d = deltaDeg; d < 113; d+=deltaDeg) {
            String distResult = d+" ";
            for (int t = 0; t < errors.length; t++) {
                pPhase[t].calcTime(d);
                Arrival[] arrivals = pPhase[t].getArrivals();
                Arrival a = arrivals[0];
                double correct = 2*R*Math.sin(d/2.0*Math.PI/180)/vp;
                double error = (correct - a.getTime() ) ; 
                //errors[t][d] = (correct - a.getTime() ) / correct *100; 
                distResult += nf.format(error) +" ";
            }
            System.out.println(distResult);
        }
    }
    
    NumberFormat nf = new DecimalFormat("0.0000000");
    
    static double vp = 5.8;
    static double vs = 3.5;

    public static TauModel create(VelocityModel vMod,
                                  double minDeltaP,
                                  double maxDeltaP,
                                  double maxDepthInterval,
                                  double maxRangeInterval,
                                  double maxInterpError,
                                  boolean allowInnerCoreS,
                                  double slownessTolerance) throws NoSuchMatPropException, NoSuchLayerException,
            SlownessModelException, TauModelException {
        VelocityModel vmod = ConstantModelTest.createVelMod(vp, vs);
        SlownessModel smod = new SphericalSModel(vmod,
                                                 minDeltaP,
                                                 maxDeltaP,
                                                 maxDepthInterval,
                                                 maxRangeInterval,
                                                 maxInterpError,
                                                 true,
                                                 SlownessModel.DEFAULT_SLOWNESS_TOLERANCE);
        return new TauModel(smod);
    }
}
