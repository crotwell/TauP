package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.*;

import java.lang.reflect.Type;

import static edu.sc.seis.TauP.gson.TimeResultSerializer.baseSerialize;

public class BeachballResultSerializer  implements JsonSerializer<BeachballResult> {

    @Override
    public JsonElement serialize(BeachballResult src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = baseSerialize(src, context);

        JsonObject nptAxis = new JsonObject();
        FaultPlane fp1 = src.getSourceArg().getNodalPlane1();
        nptAxis.add(JSONLabels.N_AXIS, asAzTakeoff(fp1.nullAxis()));
        nptAxis.add(JSONLabels.P_AXIS, asAzTakeoff(fp1.pAxis()));
        nptAxis.add(JSONLabels.T_AXIS, asAzTakeoff(fp1.tAxis()));
        out.add(JSONLabels.NPT_AXIS, nptAxis);

        out.add(JSONLabels.ARRIVAL_LIST, context.serialize(src.getArrivals()));
        JsonArray radArr = new JsonArray();
        for (RadiationAmplitude radAmp : src.getRadiationPattern()) {
            JsonArray radPoint = new JsonArray();
            radPoint.add(radAmp.getCoord().getTakeoffAngleDegree());
            radPoint.add(radAmp.getCoord().getAzimuthDegree());
            radPoint.add(radAmp.getRadialAmplitude());
            radPoint.add(radAmp.getPhiAmplitude());
            radPoint.add(radAmp.getThetaAmplitude());
            radArr.add(radPoint);
        }
        out.add(JSONLabels.RADIATION_PATTERN, radArr);
        return out;
    }

    JsonObject asAzTakeoff(Vector v) {
        SphericalCoordinate n = v.toSpherical();
        JsonObject azto = new JsonObject();
        azto.addProperty(JSONLabels.AZ, n.getAzimuthDegree());
        azto.addProperty(JSONLabels.TAKEOFF, n.getTakeoffAngleDegree());
        return azto;
    }
}
