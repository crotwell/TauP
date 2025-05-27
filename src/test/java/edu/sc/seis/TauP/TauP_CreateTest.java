package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.io.IOException;



public class TauP_CreateTest {

    @Test
    public void testGetCmbDepth() throws IOException, VelocityModelException, SlownessModelException, TauModelException {
        for (String modelName : VelocityModelTest.modelNames) {
            VelocityModel vMod = VelocityModelTest.loadTestVelMod(modelName);
            TauModel tMod = TauModelLoader.createTauModel(vMod);
            assertEquals( vMod.getMohoDepth(), tMod.getMohoDepth(), 0.000001);
            assertEquals( vMod.getCmbDepth(), tMod.getCmbDepth(), 0.000001);
            assertEquals( vMod.getIocbDepth(), tMod.getIocbDepth(), 0.000001);
        }
    }
}
