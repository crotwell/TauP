package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.DistanceCalc;
import edu.sc.seis.TauP.JSONLabels;
import net.sf.geographiclib.Geodesic;

import java.lang.reflect.Type;

public class DistCalcSerializer   implements JsonSerializer<DistanceCalc> {
    @Override
    public JsonElement serialize(DistanceCalc distanceCalc, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject out = new JsonObject();
        out.addProperty(JSONLabels.DISTTYPE, distanceCalc.getCalcType());
        Geodesic geodesic = distanceCalc.getGeodesic();
        if (geodesic.Flattening() != 0) {
            out.add(JSONLabels.INVFLATTENING, new JsonPrimitive((float)(1 / geodesic.Flattening())));
        }
        out.add(JSONLabels.EQUITORIALRADIUS, new JsonPrimitive((float)(geodesic.EquatorialRadius()/1000)));
        return out;
    }
}
