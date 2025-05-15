package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.NamedVelocityDiscon;
import edu.sc.seis.TauP.VelocityLayer;
import edu.sc.seis.TauP.VelocityModel;

import java.lang.reflect.Type;

public class VelocityModelSerializer implements JsonSerializer<VelocityModel> {

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
