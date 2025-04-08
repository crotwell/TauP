package edu.sc.seis.TauP.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.NamedVelocityDiscon;

import java.lang.reflect.Type;

public class NamedVelocityDisconSerializer implements JsonSerializer<NamedVelocityDiscon> {

    @Override
    public JsonElement serialize(NamedVelocityDiscon src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        if (src.getName() != null) {
            json.addProperty(JSONLabels.NAME, src.getName());
        }
        if (src.getPreferredName() != null) {
            json.addProperty(JSONLabels.PREFNAME, src.getPreferredName());
        }
        json.addProperty(JSONLabels.DEPTH, (float)src.getDepth());
        return json;
    }
}
