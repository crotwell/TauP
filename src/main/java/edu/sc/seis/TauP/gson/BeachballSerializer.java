package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.*;

import java.lang.reflect.Type;

public class BeachballSerializer implements JsonSerializer<BeachBall> {
    @Override
    public JsonElement serialize(BeachBall beachball, Type type, JsonSerializationContext context) {
        JsonObject bballObj = new JsonObject();
        bballObj.add(JSONLabels.FAULT, context.serialize(beachball.getFaultPlane()));
        bballObj.addProperty(JSONLabels.BBTYPE, beachball.getBbType().toString());
        bballObj.addProperty(JSONLabels.HEMISPHERE, beachball.getHemisphereType().toString());
        bballObj.addProperty(JSONLabels.BBTYPE, beachball.getBbType().toString());

        JsonObject nptAxis = new JsonObject();
        FaultPlane fp1 = beachball.getFaultPlane();
        nptAxis.add(JSONLabels.N_AXIS, asAzTakeoff(fp1.nullAxis()));
        nptAxis.add(JSONLabels.P_AXIS, asAzTakeoff(fp1.pAxis()));
        nptAxis.add(JSONLabels.T_AXIS, asAzTakeoff(fp1.tAxis()));
        bballObj.add(JSONLabels.NPT_AXIS, nptAxis);
        bballObj.add(JSONLabels.ARRIVAL_LIST, context.serialize(beachball.getArrivals()));
        JsonArray radArr = new JsonArray();
        for (RadiationAmplitude radAmp: beachball.getRadiationAmplitudeList()) {
            JsonArray radPoint = new JsonArray();
            radPoint.add(radAmp.getCoord().getTakeoffAngleDegree());
            radPoint.add(radAmp.getCoord().getAzimuthDegree());
            radPoint.add(radAmp.getRadialAmplitude());
            radPoint.add(radAmp.getPhiAmplitude());
            radPoint.add(radAmp.getThetaAmplitude());
            radArr.add(radPoint);
        }
        bballObj.add(JSONLabels.RADIATION_PATTERN, radArr);
        return bballObj;
    }

    public static JsonObject asAzTakeoff(Vector v) {
        SphericalCoordinate n = v.toSpherical();
        JsonObject azto = new JsonObject();
        azto.addProperty(JSONLabels.AZ, n.getAzimuthDegree());
        azto.addProperty(JSONLabels.TAKEOFF, n.getTakeoffAngleDegree());
        return azto;
    }
}
