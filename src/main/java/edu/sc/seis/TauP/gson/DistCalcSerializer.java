package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.DistAzKarney;
import edu.sc.seis.TauP.DistanceCalc;
import edu.sc.seis.TauP.DistanceCalcSpherical;
import edu.sc.seis.TauP.JSONLabels;
import net.sf.geographiclib.Geodesic;

import java.lang.reflect.Type;

public class DistCalcSerializer   implements JsonSerializer<DistanceCalc> {
    @Override
    public JsonElement serialize(DistanceCalc distanceCalc, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject out = new JsonObject();
        Geodesic geodesic = distanceCalc.getGeodesic();
        out.add(JSONLabels.TYPE,  new JsonPrimitive(distanceCalc.getCalcType()));
        out.add(JSONLabels.RADIUS, new JsonPrimitive((float) DistAzKarney.averageRadiusKm(geodesic)));
        if ( ! (distanceCalc instanceof DistanceCalcSpherical)) {
            out.add(JSONLabels.INVFLATTENING, new JsonPrimitive(1.0/geodesic.Flattening()));
            out.add(JSONLabels.EQUITORIALRADIUS, new JsonPrimitive((float)(geodesic.EquatorialRadius()/1000.0)));
        }
        return out;
    }
}
