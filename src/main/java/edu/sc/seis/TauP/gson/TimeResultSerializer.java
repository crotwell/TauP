package edu.sc.seis.TauP.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.sc.seis.TauP.JSONLabels;

import java.lang.reflect.Type;

public class TimeResultSerializer implements JsonSerializer<TimeResult> {
    @Override
    public JsonElement serialize(TimeResult src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = new JsonObject();
        out.addProperty(JSONLabels.MODEL, src.model);
        out.add(JSONLabels.SOURCEDEPTH_LIST, context.serialize(src.sourcedepthlist));
        out.add(JSONLabels.RECEIVERDEPTH_LIST, context.serialize(src.receiverdepthlist));
        out.add(JSONLabels.PHASE_LIST, context.serialize(src.phases));
        out.add(JSONLabels.SCATTER, context.serialize(src.scatter));
        //out.add(JSONLabels.SCATTERER, context.serialize(src.scatter));
        out.add(JSONLabels.SOURCEDEPTH_LIST, context.serialize(src.sourcedepthlist));
        if (src.sourceArg != null) {
            out.add(JSONLabels.SOURCE, context.serialize(src.sourceArg));

        }
        out.add(JSONLabels.ARRIVAL_LIST, context.serialize(src.arrivals));
        return out;
    }
}
