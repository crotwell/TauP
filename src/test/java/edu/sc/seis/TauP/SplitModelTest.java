package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;


public class SplitModelTest {
    
    @Test
    public void testSplit() throws Exception {
        double depth = 110;
        TauModel tMod = TauModelLoader.load("iasp91");
        TauModel splitTMod = tMod.splitBranch(depth);
        assertEquals( tMod.getNumBranches()+1, splitTMod.getNumBranches());
        assertEquals( tMod.getRayParams().length+2, splitTMod.getRayParams().length);

        int numBranches = tMod.getNumBranches();
        int splitBranch = tMod.findBranch(depth);
        
        double newPRP = splitTMod.getSlownessModel().getSlownessLayer(splitTMod.getSlownessModel().layerNumberAbove(depth, true), true).getBotP();
        double newSRP = splitTMod.getSlownessModel().getSlownessLayer(splitTMod.getSlownessModel().layerNumberAbove(depth, false), false).getBotP();
        
        int sIndex = splitTMod.getRayParams().length;
        int pIndex = sIndex;
        double[] rayParams = splitTMod.getRayParams();
        for (int j = 0; j < rayParams.length; j++) {
            if (newPRP == rayParams[j]) {pIndex = j;}
            if (newSRP == rayParams[j]) {sIndex = j;}
        }
        assertTrue( pIndex == splitTMod.getRayParams().length || sIndex < pIndex, "index "+pIndex+" "+sIndex+" "+splitTMod.getRayParams().length);
        
        
        for (int b=0;b<numBranches; b++) {
            TauBranch orig = tMod.getTauBranch(b, true);
            TauBranch depthBranch;
            if (b < splitBranch) {
                depthBranch = splitTMod.getTauBranch(b, true);
                assertTrue(depthBranch.getDist(pIndex)>0, "p dist above split");
                assertTrue(depthBranch.getTime(pIndex)>0, "p time above split");
            } else if ( b > splitBranch) {
                depthBranch = splitTMod.getTauBranch(b+1, true);
                assertEquals( depthBranch.getDist(pIndex),0,0.00000001, "p dist below split");
                assertEquals( depthBranch.getTime(pIndex),0,0.00000001, "p time below split");
            } else {
                // the split one
                continue;
            }
            System.out.println("Add ray params at index "+sIndex+" and "+pIndex);
            arrayEqualsSubrange("branch "+b+" dist", orig.getDist(), 0, depthBranch.getDist(), 0, sIndex);
            arrayEqualsSubrange("branch "+b+" time", orig.getTime(), 0, depthBranch.getTime(), 0, sIndex);
            int len = orig.getDist().length;
            if (sIndex < len) {
                assertEquals( len+2, depthBranch.getDist().length);
                arrayEqualsSubrange("branch "+b+" dist", orig.getDist(), sIndex, depthBranch.getDist(), sIndex+1, pIndex-sIndex-1);
                arrayEqualsSubrange("branch "+b+" time", orig.getTime(), sIndex, depthBranch.getTime(), sIndex+1, pIndex-sIndex-1);
                arrayEqualsSubrange("branch "+b+" dist", orig.getDist(), pIndex, depthBranch.getDist(), pIndex+2, len-pIndex-sIndex-2);
                arrayEqualsSubrange("branch "+b+" time", orig.getTime(), pIndex, depthBranch.getTime(), pIndex+2, len-pIndex-sIndex-2);
                
            }
        }
        // now check branch split
        TauBranch orig = tMod.getTauBranch(splitBranch, true);
        TauBranch above = splitTMod.getTauBranch(splitBranch, true);
        TauBranch below = splitTMod.getTauBranch(splitBranch+1, true);
        assertEquals( above.getMinRayParam(), below.getMaxRayParam(), 0.00000001, "min/max ray param");
        for (int i = 0; i < above.getDist().length; i++) {
            if (i < sIndex) {
                assertEquals(orig.getDist(i), above.getDist(i), 0.000000001, "above");
            } else if (i == sIndex) {
                // new value should be close to average of values to either side
                assertEquals( (orig.getDist(i-1)+orig.getDist(i))/2, above.getDist(i), 0.00001, "sIndex");
            } else if (i > sIndex && i < pIndex) {
                assertEquals( orig.getDist(i-1), above.getDist(i)+below.getDist(i), 0.000000001, "above");
            } else if (i == pIndex) {
                // new value should be close to average of values to either side
                System.out.println(i);
                System.out.println((i-3)+" "+orig.getDist(i-3)+"  "+tMod.getRayParams()[i-3]);
                System.out.println((i-2)+" "+orig.getDist(i-2)+"  "+tMod.getRayParams()[i-2]);
                System.out.println((i-1)+" "+orig.getDist(i-1)+"  "+tMod.getRayParams()[i-1]);
                System.out.println((i)+" "+orig.getDist(i)+"  "+tMod.getRayParams()[i]);
                System.out.println((i+1)+" "+orig.getDist(i+1)+"  "+tMod.getRayParams()[i+1]);
                System.out.println();
                System.out.println((i-1)+"  above"+above.getDist(i-1)+" below"+below.getDist(i-1)+"  "+rayParams[i-1]);
                System.out.println((i)+"  above"+above.getDist(i)+" below"+below.getDist(i)+"  "+rayParams[i]);
                System.out.println((i+1)+"  above"+above.getDist(i+1)+" below"+below.getDist(i+1)+"  "+rayParams[i+1]);
                System.out.println((i+2)+"  above"+above.getDist(i+2)+" below"+below.getDist(i+2)+"  "+rayParams[i+2]);
                System.out.println("above tb: "+above);
                System.out.println("below tb: "+below);
                assertEquals( (orig.getDist(i-2)+orig.getDist(i-1))/2, above.getDist(i)+below.getDist(i), 0.0001,
                		     "pIndex "+orig.getDist(i-2)+" "+orig.getDist(i-1)+"  above"+above.getDist(i)+" "+below.getDist(i));
            }else {
                assertEquals( orig.getDist(i-2), above.getDist(i)+below.getDist(i), 0.000000001, "above");
            }
        }
    }
    
/*
    @Test
    public void testDepthInModel() throws Exception {
        double depth = 120; // 120 is in model
        testDepth(depth);
    }
*/
    //@Test
    public void testDepthNotInModel() throws Exception {
        double depth = 119; // 120 is in model
        testDepth(depth);
    }

    public void testDepth(double depth) throws Exception {
        TauModel tMod = TauModelLoader.load("iasp91");
        TauModel tModDepth = tMod.depthCorrect(depth);
        double[] tModRP = tMod.getRayParams();
        double[] tModDepthRP = tModDepth.getRayParams();
        SlownessLayer psl = tMod.getSlownessModel().getSlownessLayer(tMod.getSlownessModel().layerNumberAbove(depth,
                                                                                                              true),
                                                                     true);
        SlownessLayer ssl = tMod.getSlownessModel().getSlownessLayer(tMod.getSlownessModel().layerNumberAbove(depth,
                                                                                                              false),
                                                                     false);
        System.out.println("P Slowness Layer: " + psl);
        System.out.println("S Slowness Layer: " + ssl);
        boolean depthInModel = psl.getBotDepth() == depth;
        System.out.println("depthInModel: "+depthInModel);
        assertEquals( tModRP.length + (depthInModel?0:2), tModDepthRP.length, "ray param length");
        int i = 0;
        double newPRP = psl.evaluateAt_bullen(depth, tMod.getRadiusOfEarth());
        double newSRP = ssl.evaluateAt_bullen(depth, tMod.getRadiusOfEarth());
        int sIndex = tModRP.length;
        int pIndex = sIndex;
        while (i < tModRP.length && (depthInModel || (tModRP[i] > newPRP && tModRP[i] > newSRP))) {
            assertEquals( tModRP[i], tModDepthRP[i], 0.00000001, "rayParay " + i);
            i++;
        }
        if (!depthInModel) {
            sIndex = i;
            assertEquals(newSRP, tModDepthRP[i], 0.000000001, "new S RP");
            while (i < tModRP.length && tModRP[i] > newPRP) {
                assertEquals( tModRP[i], tModDepthRP[i + 1], 0.00000001, "rayParay " + i);
                i++;
            }
            pIndex = i;
            assertEquals( newPRP, tModDepthRP[i + 1], 0.000000001, "new P RP");
            while (i < tModRP.length) {
                assertEquals( tModRP[i], tModDepthRP[i + 2], 0.00000001, "rayParay " + i);
                i++;
            }
        }
        
        assertEquals(  tMod.getNumBranches() + (depthInModel?0:1), tModDepth.getNumBranches());
        int numBranches = tMod.getNumBranches();
        int splitBranch = tMod.findBranch(depth);
        for (int b=0;b<numBranches; b++) {
            TauBranch orig = tMod.getTauBranch(b, true);
            TauBranch depthBranch;
            if (b < splitBranch) {
                depthBranch = tModDepth.getTauBranch(b, true);
            } else if ( b > splitBranch) {
                depthBranch = tModDepth.getTauBranch(b+1, true);
            } else {
                // the split one
                continue;
            }
            System.out.println("Add ray params at index "+sIndex+" and "+pIndex);
            arrayEqualsSubrange("branch "+b+" dist", orig.getDist(), 0, depthBranch.getDist(), 0, sIndex);
            arrayEqualsSubrange("branch "+b+" time", orig.getTime(), 0, depthBranch.getTime(), 0, sIndex);
            int len = orig.getDist().length;
            if (sIndex < len) {
                arrayEqualsSubrange("branch "+b+" dist", orig.getDist(), sIndex, depthBranch.getDist(), sIndex+1, pIndex-sIndex);
                arrayEqualsSubrange("branch "+b+" time", orig.getTime(), sIndex, depthBranch.getTime(), sIndex+1, pIndex-sIndex);
                arrayEqualsSubrange("branch "+b+" dist", orig.getDist(), pIndex, depthBranch.getDist(), pIndex+2, len-pIndex-sIndex);
                arrayEqualsSubrange("branch "+b+" time", orig.getTime(), pIndex, depthBranch.getTime(), pIndex+2, len-pIndex-sIndex);
                
            }
        }
        
    }
    
    public static void arrayEqualsSubrange(String msg, double[] expected, int startExpected, double[] actual, int startActual, int length)  {   
        assertTrue( startExpected + length <= expected.length, msg+" expected length");
        assertTrue( startActual + length <= actual.length, msg+" actual length");
        for (int i = 0; i < length; i++) {
            assertEquals(expected[startExpected+i], actual[startActual+i], 0.00000001,
                         msg+" actual["+startActual+"+"+i+"] != expected["+startActual+"+"+i+"]" );
            
        }
        
    }
}
