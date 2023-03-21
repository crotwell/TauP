package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class VelocityModelTest {

    public static String[] modelNames = new String[] {"crustless.nd",
                                                      MoonTest.lunarModelFile,
                                                      "cn01.tvel",
                                                      "constant.tvel"};

    @BeforeEach
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
            assertNotEquals( vMod.getMohoDepth(), vMod.getCmbDepth(), modelName + " Moho != CMB");
            if (vMod.getCmbDepth() == vMod.getIocbDepth()) {
                assertEquals( vMod.getRadiusOfEarth(), vMod.getCmbDepth(), 0.00000001, modelName+" cmb=iocb means both at center");
            }
        }
    }

    @Test
    public void testCN01() throws IOException, VelocityModelException {
        String modelName = "cn01.tvel";
        VelocityModel vMod = loadTestVelMod(modelName);

        assertEquals( 36, vMod.getMohoDepth(), 0.00000001, "cmb=moho, moho at 36");
        assertEquals( vMod.getRadiusOfEarth(), vMod.getCmbDepth(), 0.00000001, "cmb=moho, cmb at center");
        assertEquals( vMod.getRadiusOfEarth(), vMod.getIocbDepth(), 0.00000001, "cmb=moho, iocb at center");
        
    }


    @Test
    public void testNoOuterCore() throws IOException, VelocityModelException {
        String modelName = "noOuterCore.nd";
        VelocityModel vMod = loadTestVelMod(modelName);

        assertEquals( 35, vMod.getMohoDepth(), 0.00000001, modelName+" moho ");
        assertEquals( 2889, vMod.getCmbDepth(), 0.00000001, modelName+"cmb ");
        assertEquals( vMod.getCmbDepth(), vMod.getIocbDepth(), 0.00000001, modelName+"cmb=iocb, iocb ");
        
    }

    @Test
    public void testNoInnerCore() throws IOException, VelocityModelException {
        String modelName = "noInnerCore.nd";
        VelocityModel vMod = loadTestVelMod(modelName);

        assertEquals( 35, vMod.getMohoDepth(), 0.00000001, modelName+" moho ");
        assertEquals( 2889, vMod.getCmbDepth(), 0.00000001, modelName+"cmb ");
        assertEquals( vMod.getRadiusOfEarth(), vMod.getIocbDepth(), 0.00000001, modelName+"iocb=center of earth, iocb ");
        
    }

    @Test
    public void testNDWithoutLabels() throws IOException, VelocityModelException {
        String modelName = "NDNoLabels.nd";
        VelocityModel vMod = loadTestVelMod(modelName);

        assertEquals( 36, vMod.getMohoDepth(), 0.00000001, modelName+" moho ");
        assertEquals(2890, vMod.getCmbDepth(), 0.00000001, modelName+"cmb ");
        assertEquals( 5154.9, vMod.getIocbDepth(), 0.00000001, modelName+"iocb");
        
    }

    @Test
    public void testMergeMyCrust() throws IOException, VelocityModelException, TauModelException {
        boolean smoothTop = false;
        boolean smoothBot = true;
        String crustModelName = "mycrust.nd";
        VelocityModel crustVMod = loadTestVelMod(crustModelName);
        TauModel tMod = TauModelLoader.load("ak135");
        SlownessModel sMod = tMod.getSlownessModel();
        VelocityModel baseVMod = sMod.getVelocityModel();
        VelocityModel outVMod = baseVMod.replaceLayers(crustVMod.getLayers(), crustModelName, smoothTop, smoothBot);

        assertEquals( 39, outVMod.getMohoDepth(), 0.00000001, crustModelName+" moho ");
        assertEquals( baseVMod.getCmbDepth(), outVMod.getCmbDepth(), 0.00000001, crustModelName+" cmb ");
        assertEquals( baseVMod.getIocbDepth(), outVMod.getIocbDepth(), 0.00000001, crustModelName+" iocb ");
    }
}
