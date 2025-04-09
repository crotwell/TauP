package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;
import edu.sc.seis.seisFile.Location;

import java.lang.reflect.Type;

public class GsonUtil {

    public static String toJson(Object obj) {
        return createGsonBuilder().create().toJson(obj);
    }

    public static GsonBuilder createGsonBuilder() {
        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        GsonUtil.registerSerializers(builder);
        return builder;
    }

    public static void registerSerializers(GsonBuilder gson) {
        gson.registerTypeAdapter(Location.class, new LocationSerializer());
        gson.registerTypeAdapter(DistanceAngleRay.class, new DistanceAngleRaySerializier());
        gson.registerTypeAdapter(SeismicSourceArgs.class, new SourceArgsSerializer());
        gson.registerTypeAdapter(Scatterer.class, new ScattererSerializer());
        gson.registerTypeAdapter(ShadowZone.class, new ShadowZoneSerializer());
        gson.registerTypeAdapter(NamedVelocityDiscon.class, new NamedVelocityDisconSerializer());
        gson.registerTypeAdapter(VelocityLayer.class, new VelocityLayerSerializer());
        gson.registerTypeAdapter(VelocityModel.class, new VelocityModelSerializer());
        gson.registerTypeAdapter(Isochron.class, new IsochronSerializer());
        XYPlotSerializers.registerSerializers(gson);
        gson.registerTypeAdapter(Arrival.class, new ArrivalSerializer(false, false, false));
        gson.registerTypeAdapter(ScatteredArrival.class, new ScatteredArrivalSerializer(false, false, false));
        gson.registerTypeAdapter(TimeResult.class, new TimeResultSerializer());
    }

}

class LocationSerializer implements JsonSerializer<Location> {
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

class SourceArgsSerializer implements JsonSerializer<SeismicSourceArgs> {

    @Override
    public JsonElement serialize(SeismicSourceArgs src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty(JSONLabels.MW, src.getMw());
        json.addProperty(JSONLabels.ATTEN_FREQ, src.getAttenuationFrequency());
        if (src.hasStrikeDipRake()) {
            JsonObject jsonSDR = new JsonObject();
            json.add(JSONLabels.FAULT, jsonSDR);
            jsonSDR.addProperty(JSONLabels.STRIKE, src.getStrikeDipRake().get(0));
            jsonSDR.addProperty(JSONLabels.DIP, src.getStrikeDipRake().get(1));
            jsonSDR.addProperty(JSONLabels.RAKE, src.getStrikeDipRake().get(2));

        }
        return json;
    }
}

class DistanceAngleRaySerializier implements JsonSerializer<DistanceAngleRay> {

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
     * @param src       the object that needs to be converted to Json.
     * @param typeOfSrc the actual type (fully genericized version) of the source object.
     * @param context
     * @return a JsonElement corresponding to the specified object.
     */
    @Override
    public JsonElement serialize(DistanceAngleRay src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = new JsonObject();
        if (src.isDegrees()) {
            out.add(JSONLabels.DEG, new JsonPrimitive((float)src.getDegrees()));
        } else {
            out.add(JSONLabels.RADIAN, new JsonPrimitive((float)src.getRadians()));
        }
        if (src.hasAzimuth()) {
            out.add(JSONLabels.AZ, new JsonPrimitive(src.getAzimuth().floatValue()));
        }
        if (src.hasBackAzimuth()) {
            out.add(JSONLabels.BAZ, new JsonPrimitive(src.getBackAzimuth().floatValue()));
        }
        if (src.hasSource()) {
            out.add(JSONLabels.SOURCE, locSerial.serialize(src.getSource(), Location.class, context));
        }
        if (src.hasReceiver()) {
            out.add(JSONLabels.RECEIVER, locSerial.serialize(src.getSource(), Location.class, context));
        }
        return out;
    }
    LocationSerializer locSerial = new LocationSerializer();
}

class VelocityModelSerializer implements JsonSerializer<VelocityModel> {

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
     * @param vmod       the object that needs to be converted to Json.
     * @param typeOfSrc the actual type (fully genericized version) of the source object.
     * @param context
     * @return a JsonElement corresponding to the specified object.
     */
    @Override
    public JsonElement serialize(VelocityModel vmod, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("modelName", vmod.getModelName());
        json.addProperty("radiusOfEarth", vmod.getRadiusOfEarth());
        JsonArray ndArr = new JsonArray(vmod.getNamedDiscons().size());
        json.add("nameddisons", ndArr);
        for (NamedVelocityDiscon nd : vmod.getNamedDiscons()) {
            ndArr.add(nd.asJSON());
        }
        json.addProperty("minRadius", vmod.getMinRadius());
        json.addProperty("maxRadius", vmod.getMaxRadius());
        json.addProperty("spherical", vmod.getSpherical());
        JsonArray layers = new JsonArray();
        json.add("layers", layers);
        for (VelocityLayer vl : vmod.getLayers()) {
            layers.add(context.serialize(vl));
        }
        return json;
    }
}

class VelocityLayerSerializer implements JsonSerializer<VelocityLayer> {

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
     * @param src       the object that needs to be converted to Json.
     * @param typeOfSrc the actual type (fully genericized version) of the source object.
     * @param context
     * @return a JsonElement corresponding to the specified object.
     */
    @Override
    public JsonElement serialize(VelocityLayer src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("num", src.getLayerNum());
        JsonObject top = new JsonObject();
        top.addProperty("depth", src.getTopDepth());
        top.addProperty("vp", src.getTopPVelocity());
        top.addProperty("vs", src.getTopSVelocity());
        top.addProperty("rho", src.getTopDensity());
        json.add("top", top);
        JsonObject bot = new JsonObject();
        bot.addProperty("depth", src.getBotDepth());
        bot.addProperty("vp", src.getBotPVelocity());
        bot.addProperty("vs", src.getBotSVelocity());
        bot.addProperty("rho", src.getBotDensity());
        json.add("bot", bot);
        return json;
    }
}