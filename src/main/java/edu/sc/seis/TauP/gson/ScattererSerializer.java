package edu.sc.seis.TauP.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.Scatterer;

import java.lang.reflect.Type;

public class ScattererSerializer implements JsonSerializer<Scatterer> {

    @Override
    public JsonElement serialize(Scatterer src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty(JSONLabels.DEPTH, (float)src.depth);
        obj.addProperty(JSONLabels.DISTDEG_LABEL, (float)src.getDistanceDegree());
        return obj;    }
}
