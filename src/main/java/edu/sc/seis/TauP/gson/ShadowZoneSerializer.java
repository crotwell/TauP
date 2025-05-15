package edu.sc.seis.TauP.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.ShadowZone;

import java.lang.reflect.Type;

public class ShadowZoneSerializer implements JsonSerializer<ShadowZone> {
    public JsonElement serialize(ShadowZone shad, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = new JsonObject();
        out.addProperty(JSONLabels.RAYPARAM, shad.getRayParam());
        out.addProperty(JSONLabels.TOP_DEPTH, shad.getTopDepth());
        out.addProperty(JSONLabels.BOT_DEPTH, shad.getBotDepth());
        out.add(JSONLabels.SHADOW_PRE_ARRIVAL, context.serialize(shad.getPreArrival()));
        out.add(JSONLabels.SHADOW_POST_ARRIVAL, context.serialize(shad.getPostArrival()));
        return out;
    }
}
