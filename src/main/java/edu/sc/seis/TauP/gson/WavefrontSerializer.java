package edu.sc.seis.TauP.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.WavefrontResult;

import java.lang.reflect.Type;

public class WavefrontSerializer implements JsonSerializer<WavefrontResult> {
    @Override
    public JsonElement serialize(WavefrontResult src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = TimeResultSerializer.baseSerialize(src, context);
        out.add(JSONLabels.TIMESTEPS, context.serialize(src.getTimesteps()));
        out.add(JSONLabels.ISOCHRON, context.serialize(src.getIsochrons()));

        return out;
    }
}
