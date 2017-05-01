package edu.sc.seis.TauP;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Before;
import org.junit.Test;

public class VelocityModelTest {

    public static String[] modelNames = new String[] {"crustless.nd",
                                                      MoonTest.lunarModelFile,
                                                      "cn01.tvel",
                                                      "constant.tvel"};

    @Before
    public void setUp() throws Exception {}

    public static VelocityModel loadTestVelMod(String name) throws IOException, VelocityModelException {
        BufferedReader in = new BufferedReader(new InputStreamReader(VelocityModelTest.class.getClassLoader()
                .getResourceAsStream("edu/sc/seis/TauP/" + name)));
        VelocityModel vmod;
        if (name.endsWith(".tvel")) {
            vmod = VelocityModel.readTVelFile(in, name);
        } else {
            vmod = VelocityModel.readNDFile(in, name);
        }
        return vmod;
    }

    @Test
    public void testDisconDepths() throws IOException, VelocityModelException {
        for (String modelName : modelNames) {
            VelocityModel vMod = loadTestVelMod(modelName);
            assertNotEquals(modelName + " Moho != CMB", vMod.getMohoDepth(), vMod.getCmbDepth());
            if (vMod.getCmbDepth() == vMod.getIocbDepth()) {
                assertEquals(modelName+" cmb=iocb means both at center", vMod.getRadiusOfEarth(), vMod.getCmbDepth(), 0.00000001);
            }
        }
    }

    @Test
    public void testCN01() throws IOException, VelocityModelException {
        String modelName = "cn01.tvel";
        VelocityModel vMod = loadTestVelMod(modelName);

        assertEquals("cmb=moho, moho at 36", 36, vMod.getMohoDepth(), 0.00000001);
        assertEquals("cmb=moho, cmb at center", vMod.getRadiusOfEarth(), vMod.getCmbDepth(), 0.00000001);
        assertEquals("cmb=moho, iocb at center", vMod.getRadiusOfEarth(), vMod.getIocbDepth(), 0.00000001);
        
    }

    @Test
    public void testAllCore() throws IOException, VelocityModelException {
        try {
        String modelName = "allCore.nd";
        VelocityModel vMod = loadTestVelMod(modelName);
        fail("should throw VelocityModelException");
        } catch (VelocityModelException e) {
            // this is supposed to happen
        }
    }

    @Test
    public void testAllInnerCoreCore() throws IOException, VelocityModelException {
        try {
        String modelName = "allInnerCore.nd";
        VelocityModel vMod = loadTestVelMod(modelName);
        fail("should throw VelocityModelException");
        } catch (VelocityModelException e) {
            // this is supposed to happen
        }
    }

    @Test
    public void testNoOuterCore() throws IOException, VelocityModelException {
        String modelName = "noOuterCore.nd";
        VelocityModel vMod = loadTestVelMod(modelName);

        assertEquals(modelName+" moho ", 35, vMod.getMohoDepth(), 0.00000001);
        assertEquals(modelName+"cmb ", 2889, vMod.getCmbDepth(), 0.00000001);
        assertEquals(modelName+"cmb=iocb, iocb ", vMod.getCmbDepth(), vMod.getIocbDepth(), 0.00000001);
        
    }

    @Test
    public void testNDWithoutLabels() throws IOException, VelocityModelException {
        String modelName = "NDNoLabels.nd";
        VelocityModel vMod = loadTestVelMod(modelName);

        assertEquals(modelName+" moho ", 36, vMod.getMohoDepth(), 0.00000001);
        assertEquals(modelName+"cmb ", 2890, vMod.getCmbDepth(), 0.00000001);
        assertEquals(modelName+"iocb", 5154.9, vMod.getIocbDepth(), 0.00000001);
        
    }
}
