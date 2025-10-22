package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.BeachballResult;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.RadiationAmplitude;
import edu.sc.seis.TauP.TimeResult;

import java.lang.reflect.Type;

import static edu.sc.seis.TauP.gson.TimeResultSerializer.baseSerialize;

public class BeachballResultSerializer  implements JsonSerializer<BeachballResult> {

    @Override
    public JsonElement serialize(BeachballResult src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = baseSerialize(src, context);
        out.add(JSONLabels.ARRIVAL_LIST, context.serialize(src.getArrivals()));
        JsonArray radArr = new JsonArray();
        for (RadiationAmplitude radAmp : src.getRadiationPattern()) {
            JsonArray radPoint = new JsonArray();
            radPoint.add(radAmp.getCoord().getPhi());
            radPoint.add(radAmp.getCoord().getTheta());
            radPoint.add(radAmp.getRadialAmplitude());
            radPoint.add(radAmp.getPhiAmplitude());
            radPoint.add(radAmp.getThetaAmplitude());
            radArr.add(radPoint);
        }
        out.add(JSONLabels.RADIATION_PATTERN, radArr);
        return out;
    }
}
