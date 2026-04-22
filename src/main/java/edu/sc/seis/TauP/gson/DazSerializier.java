package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.Daz;
import edu.sc.seis.TauP.DistanceCalcSpherical;
import edu.sc.seis.TauP.GeoDistType;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.seisFile.Location;

import java.lang.reflect.Type;

public class DazSerializier implements JsonSerializer<Daz> {

    /**
     * Gson invokes this call-back method during serialization when it encounters a field of the
     * specified type.
     *
     * <p>In the implementation of this call-back method, you should consider invoking {@link
     * JsonSerializationContext#serialize(Object, Type )} method to create JsonElements for any
     * non-trivial field of the {@code src} object. However, you should never invoke it on the {@code
     * src} object itself since that will cause an infinite loop (Gson will call your call-back method
     * again).
     *
     * @param src       the object that needs to be converted to Json.
     * @param typeOfSrc the actual type (fully genericized version) of the source object.
     * @param context the serialization context
     * @return a JsonElement corresponding to the specified object.
     */
    @Override
    public JsonElement serialize(Daz src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = new JsonObject();
        out.add(JSONLabels.DEG, new JsonPrimitive((float)src.getDegrees()));
        out.add(JSONLabels.KM, new JsonPrimitive(src.getKilometers().floatValue()));

        out.add(JSONLabels.AZ, new JsonPrimitive(src.getAzimuth().floatValue()));

        out.add(JSONLabels.BAZ, new JsonPrimitive(src.getBackAzimuth().floatValue()));
        JsonObject distCalc = new JsonObject();
        out.add(JSONLabels.DISTTYPE, distCalc);
        distCalc.add("type",  new JsonPrimitive(src.getDistCalc().getCalcType()));
        distCalc.addProperty(JSONLabels.RADIUS, src.getDistCalc().getCalcType());
        if ( ! (src.getDistCalc() instanceof DistanceCalcSpherical)) {
            distCalc.add(JSONLabels.INVFLATTENING, new JsonPrimitive(1.0/src.getDistCalc().getGeodesic().Flattening()));
            distCalc.add(JSONLabels.EQUITORIALRADIUS, new JsonPrimitive((float)(src.getEquitorialRadius()/1000.0)));
        }
        if (src.hasSource()) {
            out.add(JSONLabels.SOURCE, locSerial.serialize(src.getSource(), Location.class, context));
        }
        if (src.hasReceiver()) {
            out.add(JSONLabels.RECEIVER, locSerial.serialize(src.getReceiver(), Location.class, context));
        }
        return out;
    }
    LocationSerializer locSerial = new LocationSerializer();
}
