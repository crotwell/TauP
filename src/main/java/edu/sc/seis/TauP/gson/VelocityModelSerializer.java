package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.NamedVelocityDiscon;
import edu.sc.seis.TauP.VelocityLayer;
import edu.sc.seis.TauP.VelocityModel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.JSONLabels.*;

public class VelocityModelSerializer
        implements JsonSerializer<VelocityModel>, JsonDeserializer<VelocityModel> {

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
     * @param vmod       the object that needs to be converted to Json.
     * @param typeOfSrc the actual type (fully genericized version) of the source object.
     * @param context the serialization context
     * @return a JsonElement corresponding to the specified object.
     */
    @Override
    public JsonElement serialize(VelocityModel vmod, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty(MODEL_NAME, vmod.getModelName());
        json.addProperty(RADIUS_OF_EARTH, vmod.getRadiusOfEarth());
        json.addProperty(MODEL_MIN_RADIUS, vmod.getMinRadius());
        json.addProperty(MODEL_MAX_RADIUS, vmod.getMaxRadius());
        json.addProperty(MODEL_SPHERICAL, vmod.getSpherical());
        JsonArray ndArr = new JsonArray(vmod.getNamedDiscons().size());
        json.add(NAMED_DISCONS, ndArr);
        for (NamedVelocityDiscon nd : vmod.getNamedDiscons()) {
            ndArr.add(nd.asJSON());
        }
        JsonArray layers = new JsonArray();
        json.add(MODEL_LAYERS, layers);
        for (VelocityLayer vl : vmod.getLayers()) {
            layers.add(context.serialize(vl));
        }
        return json;
    }

    @Override
    public VelocityModel deserialize(JsonElement jsonElement,
                                     Type type,
                                     JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement instanceof JsonObject) {
            JsonObject jObj = (JsonObject)jsonElement;
            List<NamedVelocityDiscon> namedDiscon = new ArrayList<>();
            JsonArray ndArray = jObj.getAsJsonArray(NAMED_DISCONS);
            for (JsonElement ndEl : ndArray) {
                namedDiscon.add(jsonDeserializationContext.deserialize(ndEl, NamedVelocityDiscon.class));
            }
            List<VelocityLayer> vLayerList = new ArrayList<>();
            JsonArray layerArray = jObj.getAsJsonArray(MODEL_LAYERS);
            for (JsonElement ndEl : layerArray) {
                vLayerList.add(jsonDeserializationContext.deserialize(ndEl, VelocityLayer.class));
            }
            VelocityModel vMod = new VelocityModel(
                    jObj.getAsJsonPrimitive(MODEL_NAME).getAsString(),
                    jObj.getAsJsonPrimitive(RADIUS_OF_EARTH).getAsDouble(),
                    namedDiscon,
                    jObj.getAsJsonPrimitive(MODEL_MIN_RADIUS).getAsDouble(),
                    jObj.getAsJsonPrimitive(MODEL_MAX_RADIUS).getAsDouble(),
                    jObj.getAsJsonPrimitive(MODEL_SPHERICAL).getAsBoolean(),
                    vLayerList
                    );
            return vMod;
        }
        throw new JsonParseException("Expected an Object for VelocityModel");
    }
}
