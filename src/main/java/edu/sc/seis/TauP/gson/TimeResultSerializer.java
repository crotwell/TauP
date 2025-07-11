package edu.sc.seis.TauP.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.sc.seis.TauP.AbstractPhaseResult;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.TimeResult;

import java.lang.reflect.Type;

public class TimeResultSerializer implements JsonSerializer<TimeResult> {

    public static JsonObject baseSerialize(AbstractPhaseResult src, JsonSerializationContext context) {
        JsonObject out = new JsonObject();
        out.addProperty(JSONLabels.MODEL, src.getModel());
        out.add(JSONLabels.SOURCEDEPTH_LIST, context.serialize(src.getSourcedepthlist()));
        out.add(JSONLabels.RECEIVERDEPTH_LIST, context.serialize(src.getReceiverdepthlist()));
        out.add(JSONLabels.PHASE_LIST, context.serialize(src.getPhases()));
        out.add(JSONLabels.SCATTER, context.serialize(src.getScatter()));
        if (src.getSourceArg() != null) {
            out.add(JSONLabels.SOURCE, context.serialize(src.getSourceArg()));
        }
        return out;
    }

    @Override
    public JsonElement serialize(TimeResult src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = baseSerialize(src, context);
        out.add(JSONLabels.ARRIVAL_LIST, context.serialize(src.getArrivals()));
        return out;
    }
}
