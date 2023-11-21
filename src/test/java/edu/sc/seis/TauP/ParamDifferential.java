package edu.sc.seis.TauP;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test if the params for model sampling are too tight, ie if relaxing one can still generate results with same
 * ans within tolerance.
 */
public class ParamDifferential {
    
    public static void main(String[] args) throws Exception {
        ParamDifferential pd = new ParamDifferential();
        System.out.println("ak135");
        VelocityModel vmod = pd.getAK135VelModel();
        pd.dotestcalc("P", vmod, new AK135CorrectTime());
        pd.dotestcalc("S", vmod, new AK135CorrectTime());
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
    
    @Test
    @Disabled
    public void testConstVelModel() throws Exception {
        VelocityModel vmod = getConstVelModel();
        dotestcalc("P", vmod, new ConstCorrectTime(vmod));
    }
    
    @Test
    @Disabled
    public void testAK135() throws Exception {
        System.err.println("testAK135: "+getAK135VelModel().getModelName());
        dotestcalc("P", getAK135VelModel(), new AK135CorrectTime());
        dotestcalc("S", getAK135VelModel(), new AK135CorrectTime());
    }
    
    public void dotestcalc(String phaseName, VelocityModel vMod, CorrectTime correctTime) throws Exception {
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
        SimpleSeismicPhase[] pPhase = new SimpleSeismicPhase[deltaTMod.length];
        for (int i = 0; i < pPhase.length; i++) {
            pPhase[i] = SeismicPhaseFactory.createPhase(phaseName, deltaTMod[i]);
            System.err.println("tmod size="+deltaTMod[i].getRayParams().length);
        }
        float deltaDeg = 1.0f;
        String outName = "/tmp/"+deltaTMod[0].getVelocityModel().getModelName()+"_"+phaseName+".deltaTau";
        System.err.println("woking on "+outName);
        Arrival[] maxImprove = new Arrival[deltaTMod.length];
        double[] maxImproveTime = new double[deltaTMod.length];
        for (int t = 0; t < deltaTMod.length; t++) {
            maxImproveTime[t] = Double.NEGATIVE_INFINITY;
        }
        double maxError = 0;
        double errorDistance = -1;
        int errorIdx = -1;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outName)));
        for (float d = deltaDeg; d < 99; d+=deltaDeg) {
            double corTime = correctTime.getCorrectTime(phaseName, d);
            corTime = pPhase[0].getEarliestArrival(d).getTime();
            String distResult = d+" "+ corTime+" ";
            for (int t = 0; t < deltaTMod.length; t++) {
                Arrival a = pPhase[t].getEarliestArrival(d);
                if (a != null) {
                    distResult += nf.format((a.getTime()-corTime)) +" ";
                    if (Math.abs(a.getTime()-corTime) > maxImproveTime[t]) {
                        maxImproveTime[t] = Math.abs(a.getTime()-corTime);
                        maxImprove[t] = a;
                    }
                    if (Math.abs(a.getTime()-corTime) > maxError) {
                        maxError = Math.abs(a.getTime()-corTime);
                        errorDistance = d;
                        errorIdx = t;
                    }
                } else {
                    distResult += " ?? ";
                    maxError= Double.POSITIVE_INFINITY;
                }
                
            }
            out.println(distResult);
        }
        out.println("Max Difference: "+maxError+" at "+errorDistance+" for "+errorIdx);

        for (int t = 0; t < deltaTMod.length; t++) {
            out.println(t+" "+maxImproveTime[t]+"  "+maxImprove[t].getDistDeg());
            assertTrue(maxImproveTime[t] < 0.0005, t+ "Max Difference: "+maxImproveTime[t]+" at "+maxImprove[t].getDistDeg()+" for "+t);
        }
        out.close();
        assertTrue(maxError < 0.0005, "Max Difference: "+maxError+" at "+errorDistance+" for "+errorIdx);
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
        SlownessModel smod = new SphericalSModel(vMod,
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

abstract class CorrectTime {
    
    public abstract double getCorrectTime(String phase, double dist);
}

class ConstCorrectTime extends CorrectTime {

    ConstCorrectTime(VelocityModel vMod) {
        this.vMod = vMod;
    }
    VelocityModel vMod;
    
    @Override
    public double getCorrectTime(String phase, double dist) {
        double v;
        if (phase.equals("P") || phase.equals("PKP") || phase.equals("PKIKP")) {
            v = vMod.getVelocityLayer(0).getTopPVelocity();
        } else if (phase.equals("S")) {
            v = vMod.getVelocityLayer(0).getTopSVelocity();
        } else {
            throw new RuntimeException("Unknown phase: "+phase);
        }
        return 2*vMod.getRadiusOfEarth()*Math.sin(dist/2.0*Math.PI/180)/v;
    }
    
}

class AK135CorrectTime extends CorrectTime {

    AK135CorrectTime() throws Exception {
        AK135Test ak135Test = new AK135Test();
        ak135Test.loadTable();
        table = ak135Test.getTable();
    }
    HashMap<String, HashMap<Float, List<TimeDist>>> table;
    
    
    @Override
    public double getCorrectTime(String phase, double dist) {
        List<TimeDist> zeroDepth = table.get(phase).get(Float.valueOf(0));
        TimeDist prev = zeroDepth.get(0);
        for (TimeDist td : zeroDepth) {
            if (td.getDistDeg() == dist) {
                System.out.println("Match: "+td.getDistDeg()+" "+td.getTime());
                return td.getTime();
            } else if (td.getDistDeg() > dist) {
                System.out.println("interp "+td.getDistDeg()+" "+prev.getDistDeg()+"  "+dist+"    "+td.getTime()+"  "+prev.getTime());
                return (td.getTime()-prev.getTime())/(td.getDistDeg()-prev.getDistDeg())*(dist-prev.getDistDeg()) + prev.getTime();
            }
            prev=td;
        }
        System.out.println("Miss "+dist);
        return -999999;
    }
    
}
