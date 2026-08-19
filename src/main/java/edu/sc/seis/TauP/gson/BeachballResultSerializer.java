package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.*;

import java.lang.reflect.Type;

import static edu.sc.seis.TauP.gson.TimeResultSerializer.baseSerialize;

public class BeachballResultSerializer  implements JsonSerializer<BeachballResult> {

    @Override
    public JsonElement serialize(BeachballResult src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = baseSerialize(src, context);


        out.add(JSONLabels.ARRIVAL_LIST, context.serialize(src.getArrivals()));
        JsonArray bbArr = new JsonArray();
        out.add(JSONLabels.BEACHBALLS, context.serialize(src.getBeachBalls()));
        return out;
    }
}
