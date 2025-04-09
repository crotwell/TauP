package edu.sc.seis.TauP.gson;


import com.google.gson.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.sc.seis.TauP.*;

import java.lang.reflect.Type;

public class ArrivalSerializer implements JsonSerializer<Arrival> {
    boolean withPierce;
    boolean withPath;
    boolean withAmplitude;

    public ArrivalSerializer(boolean withPierce,
                             boolean withPath,
                             boolean withAmplitude) {
        this.withPierce = withPierce;
        this.withPath = withPath;
        this.withAmplitude = withAmplitude;
    }

    /**
     * Gson invokes this call-back method during serialization when it encounters a field of the
     * specified type.
     *
     * <p>In the implementation of this call-back method, you should consider invoking {@link
     * JsonSerializationContext#serialize(Object, Type)} method to create JsonElements for any
     * non-trivial field of the {@code src} object. However, you should never invoke it on the {@code
     * src} object itself since that will cause an infinite loop (Gson will call your call-back method
     * again).
     *
     * @param arr       the object that needs to be converted to Json.
     * @param typeOfSrc the actual type (fully genericized version) of the source object.
     * @param context
     * @return a JsonElement corresponding to the specified object.
     */
    @Override
    public JsonElement serialize(Arrival arr, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject a = new JsonObject();
        a.addProperty(JSONLabels.SOURCEDEPTH, (float)arr.getSourceDepth());
        a.addProperty(JSONLabels.RECEIVERDEPTH, (float)arr.getReceiverDepth());
        a.addProperty(JSONLabels.DISTDEG_LABEL, (float)arr.getModuloDistDeg());
        a.addProperty(JSONLabels.PHASE, arr.getName());
        a.addProperty(JSONLabels.TIME, (float)arr.getTime());
        a.addProperty(JSONLabels.RAYPARAM, (float)(Math.PI / 180.0 * arr.getRayParam()));
        a.addProperty(JSONLabels.TAKEOFF, (float) arr.getTakeoffAngleDegree());
        a.addProperty(JSONLabels.INCIDENT, (float) arr.getIncidentAngleDegree());
        a.addProperty(JSONLabels.PURISTDIST, (float)arr.getDistDeg());
        a.addProperty(JSONLabels.PURISTNAME, arr.getPuristName());
        if (arr.getRayCalculateable().hasDescription()) {
            a.addProperty(JSONLabels.DESC, arr.getRayCalculateable().getDescription());
        }
        if (withAmplitude) {
            try {
                a.add(JSONLabels.AMP, context.serialize(new ArrivalAmplitude(arr)));
            } catch (TauModelException e) {
                throw new RuntimeException(e);
            } catch (SlownessModelException e) {
                throw new RuntimeException(e);
            }
        }
        if (arr.getPhase() instanceof ScatteredSeismicPhase) {
            ScatteredSeismicPhase scatPhase = (ScatteredSeismicPhase)arr.getPhase();
            a.add(JSONLabels.SCATTER, context.serialize(scatPhase.getScatterer()));
        }
        if (arr.isRelativeToArrival()) {
            Arrival relArrival = arr.getRelativeToArrival();
            JsonObject relA = new JsonObject();
            a.add(JSONLabels.RELATIVE, relA);
            relA.addProperty(JSONLabels.DIFFERENCE, (float)(arr.getTime()-relArrival.getTime()));
            relA.add(JSONLabels.ARRIVAL, context.serialize(relArrival));
        }
        if (withPierce) {
            JsonArray points = new JsonArray();
            a.add(JSONLabels.PIERCE, points);
            TimeDist[] tdArray = arr.getPierce();
            for (TimeDist td : tdArray) {
                JsonArray tdItems = new JsonArray();
                points.add(tdItems);
                tdItems.add((float)td.getDistDeg());
                tdItems.add((float)td.getDepth());
                tdItems.add((float)td.getTime());
                if (arr.isLatLonable()) {
                    double[] latlon = arr.getLatLonable().calcLatLon(td.getDistDeg(), arr.getDistDeg());
                    tdItems.add((float)latlon[0]);
                    tdItems.add((float)latlon[1]);
                }
            }
        }
        if (withPath) {
            a.addProperty(JSONLabels.PATHLENGTH, (float)arr.calcPathLength());
            JsonArray points = new JsonArray();
            a.add(JSONLabels.PATH, points);
            for (ArrivalPathSegment seg : arr.getPathSegments()) {
                points.add(seg.asJsonObject());
            }
        }
        return a;
    }
}
