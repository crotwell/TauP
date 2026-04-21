package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.JSONLabels;
import net.sf.geographiclib.Geodesic;

import java.lang.reflect.Type;

public class GeodesicSerializer  implements JsonSerializer<Geodesic> {
    @Override
    public JsonElement serialize(Geodesic geodesic, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject out = new JsonObject();
        if (geodesic.Flattening() != 0) {
            out.add(JSONLabels.INVFLATTENING, new JsonPrimitive((float)(1 / geodesic.Flattening())));
        }
        out.add(JSONLabels.EQUITORIALRADIUS, new JsonPrimitive((float)(geodesic.EquatorialRadius()/1000)));
        return out;
    }
}
