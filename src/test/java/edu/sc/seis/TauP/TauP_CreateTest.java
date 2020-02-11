package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import java.io.IOException;



public class TauP_CreateTest {

    @Test
    public void testGetCmbDepth() throws IOException, VelocityModelException, SlownessModelException, TauModelException {
        TauP_Create taupCreate = new TauP_Create();
        for (String modelName : VelocityModelTest.modelNames) {
            VelocityModel vMod = VelocityModelTest.loadTestVelMod(modelName);
            TauModel tMod = taupCreate.createTauModel(vMod);
            assertEquals( vMod.getMohoDepth(), tMod.getMohoDepth(), 0.000001);
            assertEquals( vMod.getCmbDepth(), tMod.getCmbDepth(), 0.000001);
            assertEquals( vMod.getIocbDepth(), tMod.getIocbDepth(), 0.000001);
        }
    }
}
