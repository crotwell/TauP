package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.seisFile.Location;

import java.lang.reflect.Type;

public class LocationSerializer implements JsonSerializer<Location> {
    public JsonElement serialize(Location loc, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = new JsonObject();
        out.add(JSONLabels.LAT, new JsonPrimitive((float)loc.getLatitude()));
        out.add(JSONLabels.LON, new JsonPrimitive((float)loc.getLongitude()));
        if (loc.hasDepth()) {
            out.add(JSONLabels.DEPTH, new JsonPrimitive(loc.getDepthKm().floatValue()));
        }
        if (loc.hasDescription()) {
            out.add(JSONLabels.DESC, new JsonPrimitive(loc.getDescription()));
        }
        return out;
    }
}
